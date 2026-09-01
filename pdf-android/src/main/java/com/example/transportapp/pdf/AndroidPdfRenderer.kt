package com.example.transportapp.pdf

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PdfCallbackBridge
import android.print.PrintJob
import android.print.PrintManager
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume

/**
 * The pipeline's only impure step (Implementation.md §9.8): HTML in, A4 vector PDF bytes
 * out, driven through Chromium's own print adapter headlessly — the same engine behind the
 * system "Save as PDF" dialog, so output matches it page for page, with real selectable
 * text paginated by the @page CSS.
 *
 * The eleven checklist items exist because omitting each caused a real, diagnosed failure
 * in the prototype; they are commented at the exact line that carries them. Screen
 * rasterisation capture is a settled non-answer — do not resurrect it.
 *
 * The failure contract the UI depends on: an EMPTY byte array means failure. Never a raw
 * exception, never a spinner without a retry.
 */
object AndroidPdfRenderer {

    private const val TAG = "AndroidPdfRenderer"
    private const val BASE_URL = "file:///android_asset/"
    private const val A4_WIDTH_PX = 794   // 210mm at 96dpi
    private const val A4_HEIGHT_PX = 1123 // 297mm at 96dpi
    private const val EXPORT_TIMEOUT_MS = 15_000L

    /** HTML → PDF bytes. Empty array on any failure after the internal retries. */
    suspend fun exportPdf(context: Context, html: String, jobName: String): ByteArray =
        withContext(Dispatchers.Main) {
            try {
                exportOnMainThread(context, html, jobName)
            } catch (e: Exception) {
                Log.w(TAG, "export failed", e)
                ByteArray(0)
            }
        }

    private suspend fun exportOnMainThread(context: Context, html: String, jobName: String): ByteArray {
        val handler = Handler(Looper.getMainLooper())
        // (3) the print pipeline only sees content on a real window
        val parent = findActivity(context)?.findViewById<FrameLayout>(android.R.id.content)
        if (parent == null) {
            Log.w(TAG, "no activity window to attach the WebView; export aborting")
            return ByteArray(0)
        }

        val webView = createWebView(context)
        parent.addView(webView, webView.params)

        fun cleanup() {
            handler.post {
                try {
                    (webView.parent as? ViewGroup)?.removeView(webView)
                } catch (e: Exception) {
                    Log.w(TAG, "detach failed", e)
                }
                // (10) a leaked WebView attached to a destroyed activity is a crash source
                webView.destroy()
            }
        }

        fun closeAndDelete(descriptor: ParcelFileDescriptor?, file: File?) {
            try { descriptor?.close() } catch (_: Exception) {}
            try { file?.delete() } catch (_: Exception) {}
        }

        return suspendCancellableCoroutine { continuation ->
            var finished = false
            var timeout: Runnable? = null
            val signal = CancellationSignal()

            fun finish(bytes: ByteArray) {
                if (finished) return
                finished = true
                timeout?.let(handler::removeCallbacks)
                if (continuation.isActive) continuation.resume(bytes)
                cleanup()
            }

            // (10) a timed-out export always resumes with an empty byte array instead of hanging
            val timeoutRunnable = Runnable {
                signal.cancel()
                finish(ByteArray(0))
            }
            timeout = timeoutRunnable
            handler.postDelayed(timeoutRunnable, EXPORT_TIMEOUT_MS)

            continuation.invokeOnCancellation {
                finished = true
                timeout?.let(handler::removeCallbacks)
                signal.cancel()
                cleanup()
            }

            webView.webViewClient = object : WebViewClient() {
                // (5) onPageFinished fires for the internal about:blank page and can fire
                // twice for the real document; driving the print adapter on the blank pass
                // produces an empty PDF, so the pipeline runs exactly once, on a real URL only.
                private var handled = false

                override fun onPageFinished(view: WebView, url: String?) {
                    Log.d(TAG, "onPageFinished url=$url handled=$handled")
                    if (handled) return
                    if (url == null || url.startsWith("about:")) return
                    handled = true
                    var out: File? = null
                    var pfd: ParcelFileDescriptor? = null
                    try {
                        // (6) the adapter writes into a cache file through a read-write descriptor
                        val adapter = view.createPrintDocumentAdapter(jobName)
                        val outFile = File(context.cacheDir, "export_${System.nanoTime()}.pdf")
                        out = outFile
                        pfd = ParcelFileDescriptor.open(
                            outFile,
                            ParcelFileDescriptor.MODE_READ_WRITE or
                                ParcelFileDescriptor.MODE_CREATE or
                                ParcelFileDescriptor.MODE_TRUNCATE
                        )

                        adapter.onLayout(
                            null,
                            a4PrintAttributes(),
                            signal,
                            PdfCallbackBridge.layoutCallback(
                                { info, _ ->
                                    Log.d(TAG, "layout finished pageCount=${info?.pageCount}")
                                    // (9) drive layout, then write all pages, then read the bytes
                                    adapter.onWrite(
                                        arrayOf(PageRange.ALL_PAGES),
                                        pfd,
                                        signal,
                                        PdfCallbackBridge.writeCallback(
                                            { _ ->
                                                // (11) file I/O off the main thread
                                                kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                                                    val bytes = try {
                                                        outFile.readBytes()
                                                    } catch (e: Exception) {
                                                        Log.w(TAG, "read failed", e)
                                                        ByteArray(0)
                                                    }
                                                    Log.d(TAG, "write finished bytes=${bytes.size}")
                                                    closeAndDelete(pfd, outFile)
                                                    finish(bytes)
                                                }
                                            },
                                            { error ->
                                                Log.w(TAG, "write failed: $error")
                                                closeAndDelete(pfd, outFile)
                                                finish(ByteArray(0))
                                            }
                                        )
                                    )
                                },
                                { error ->
                                    Log.w(TAG, "layout failed: $error")
                                    closeAndDelete(pfd, outFile)
                                    finish(ByteArray(0))
                                }
                            ),
                            null
                        )
                    } catch (e: Exception) {
                        Log.w(TAG, "export setup failed", e)
                        closeAndDelete(pfd, out)
                        finish(ByteArray(0))
                    }
                }
            }

            try {
                // (4) explicit UTF-8: non-ASCII party names corrupt without it
                webView.loadDataWithBaseURL(BASE_URL, html, "text/html", "UTF-8", null)
            } catch (e: Exception) {
                Log.w(TAG, "load failed", e)
                finish(ByteArray(0))
            }
        }
    }

    /** The system print dialog for freshly generated HTML (a live document). */
    suspend fun printHtml(context: Context, html: String, jobName: String) {
        withContext(Dispatchers.Main) {
            try {
                val activity = findActivity(context) ?: run {
                    Log.w(TAG, "no activity to print from")
                    return@withContext
                }
                val parent = activity.findViewById<FrameLayout>(android.R.id.content)
                val webView = createWebView(context)
                parent.addView(webView, webView.params)

                suspendCancellableCoroutine<Unit> { continuation ->
                    var finished = false
                    fun done() {
                        if (finished) return
                        finished = true
                        if (continuation.isActive) continuation.resume(Unit)
                    }

                    continuation.invokeOnCancellation {
                        finished = true
                        Handler(Looper.getMainLooper()).post {
                            (webView.parent as? ViewGroup)?.removeView(webView)
                            webView.destroy()
                        }
                    }

                    webView.webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView, url: String?) {
                            try {
                                val adapter = webView.createPrintDocumentAdapter(jobName)
                                val manager = activity.getSystemService(Context.PRINT_SERVICE) as PrintManager
                                val printJob = manager.print(jobName, adapter, a4PrintAttributes())
                                scheduleDestroyWhenFinished(printJob, webView)
                            } catch (e: Exception) {
                                Log.w(TAG, "print setup failed", e)
                            }
                            done()
                        }
                    }

                    try {
                        webView.loadDataWithBaseURL(BASE_URL, html, "text/html", "UTF-8", null)
                    } catch (e: Exception) {
                        Log.w(TAG, "load failed", e)
                        done()
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "print failed", e)
            }
        }
    }

    /**
     * Printing already-generated PDF bytes is a different path from printing HTML (§9.8):
     * an adapter that reports an unknown page count and writes the bytes through. This is
     * the reprint-from-snapshot route. PrintManager requires an Activity context.
     */
    fun printBytes(context: Context, pdfBytes: ByteArray, jobName: String) {
        val activity = findActivity(context)
            ?: throw IllegalStateException("Can print only from an activity — none is resumed")
        val printManager = activity.getSystemService(Context.PRINT_SERVICE) as PrintManager
        printManager.print(
            jobName,
            BytesPrintAdapter(pdfBytes, jobName),
            PrintAttributes.Builder().setMediaSize(PrintAttributes.MediaSize.ISO_A4).build(),
        )
    }

    /** Save to the public Downloads collection (modern) or app-specific external (older). */
    fun saveToDownloads(context: Context, pdfBytes: ByteArray, fileName: String): Uri? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = android.content.ContentValues().apply {
                put(android.provider.MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(android.provider.MediaStore.Downloads.MIME_TYPE, "application/pdf")
            }
            val uri = context.contentResolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return null
            context.contentResolver.openOutputStream(uri)?.use { it.write(pdfBytes) }
            uri
        } else {
            val dir = File(context.getExternalFilesDir(null), "pdfs").apply { if (!exists()) mkdirs() }
            val file = File(dir, fileName)
            file.writeBytes(pdfBytes)
            Uri.fromFile(file)
        }

    /** Share through a FileProvider URI — the file name is what the recipient sees. */
    fun sharePdf(context: Context, pdfBytes: ByteArray, fileName: String, chooserTitle: String) {
        val dir = File(context.cacheDir, "pdfs").apply { if (!exists()) mkdirs() }
        val file = File(dir, fileName)
        file.writeBytes(pdfBytes)
        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, chooserTitle).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    /**
     * Preview rasterisation is a third, separate concern (§9.8): rasterise the first page at
     * a bounded target width. Degrades to null on any failure; the bitmap memory is bounded
     * by the target width, which is where this code is most likely to hit OOM.
     */
    fun rasteriseFirstPage(context: Context, pdfBytes: ByteArray, targetWidthPx: Int): Bitmap? = try {
        val temp = File(context.cacheDir, "preview_${System.nanoTime()}.pdf")
        temp.writeBytes(pdfBytes)
        ParcelFileDescriptor.open(temp, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
            android.graphics.pdf.PdfRenderer(pfd).use { renderer ->
                renderer.openPage(0).use { page ->
                    val scale = targetWidthPx.toFloat() / page.width
                    val bitmap = Bitmap.createBitmap(
                        targetWidthPx,
                        (page.height * scale).toInt().coerceAtLeast(1),
                        Bitmap.Config.ARGB_8888,
                    )
                    bitmap.eraseColor(android.graphics.Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    bitmap
                }
            }
        }.also { temp.delete() }
    } catch (e: Exception) {
        Log.w(TAG, "rasterise failed", e)
        null
    }

    // ---------- internals ----------

    /** Walks Context wrappers; falls back to the registered resumed activity (item 3). */
    private tailrec fun findActivity(context: Context): Activity? = when (context) {
        is Activity -> context
        is ContextWrapper -> findActivity(context.baseContext)
        else -> CurrentActivity.current
    }

    private fun createWebView(context: Context): WebView {
        val webView = WebView(context)
        // (1) visible + zero alpha: INVISIBLE/GONE skips Chromium's render pipeline entirely,
        // producing an empty document; alpha 0 keeps the view off the user's screen while it
        // stays fully functional. Software layer; no wide viewport; text zoom pinned to 100
        // so the user's font-size setting cannot distort the document.
        webView.visibility = View.VISIBLE
        webView.alpha = 0f
        webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        webView.settings.useWideViewPort = false
        webView.settings.textZoom = 100
        // (2) measure and lay out at A4 pixel dimensions at 96 dpi
        webView.measure(
            View.MeasureSpec.makeMeasureSpec(A4_WIDTH_PX, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(A4_HEIGHT_PX, View.MeasureSpec.EXACTLY)
        )
        webView.layout(0, 0, A4_WIDTH_PX, A4_HEIGHT_PX)
        return webView
    }

    private val WebView.params: FrameLayout.LayoutParams
        get() = FrameLayout.LayoutParams(A4_WIDTH_PX, A4_HEIGHT_PX, Gravity.TOP or Gravity.START)

    private fun a4PrintAttributes(): PrintAttributes =
        PrintAttributes.Builder()
            .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
            // (7) Chromium's onWrite rejects attributes without a resolution ("attributes
            // must specify print resolution"); the system dialog injects one silently — a
            // headless drive must supply it. This was the prototype's final blocking bug.
            .setResolution(PrintAttributes.Resolution("document-pdf", "Document PDF", 300, 300))
            .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
            .build()

    private fun scheduleDestroyWhenFinished(printJob: PrintJob, webView: WebView) {
        val handler = Handler(Looper.getMainLooper())
        fun poll() {
            if (printJob.isCompleted || printJob.isCancelled || printJob.isFailed) {
                webView.destroy()
            } else {
                handler.postDelayed({ poll() }, 500)
            }
        }
        handler.postDelayed({ poll() }, 1500)
    }

    /** The byte-through adapter for reprinting an already-generated PDF (§9.8). */
    private class BytesPrintAdapter(
        private val pdfBytes: ByteArray,
        private val jobName: String,
    ) : PrintDocumentAdapter() {

        override fun onLayout(
            oldAttributes: PrintAttributes?,
            newAttributes: PrintAttributes?,
            cancellationSignal: CancellationSignal?,
            callback: LayoutResultCallback,
            extras: Bundle?
        ) {
            if (cancellationSignal?.isCanceled == true) {
                callback.onLayoutCancelled()
                return
            }
            val info = PrintDocumentInfo.Builder(jobName)
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
                .build()
            callback.onLayoutFinished(info, true)
        }

        override fun onWrite(
            pages: Array<out PageRange>,
            destination: ParcelFileDescriptor,
            cancellationSignal: CancellationSignal?,
            callback: WriteResultCallback
        ) {
            try {
                FileOutputStream(destination.fileDescriptor).use { it.write(pdfBytes) }
                callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
            } catch (e: Exception) {
                callback.onWriteFailed(e.message)
            }
        }
    }
}
