package com.example.transportapp.data.transport.documents

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.transportapp.pdf.AndroidPdfRenderer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The PDF port (Implementation.md §9.8): HTML string plus job name in, byte array out.
 * An EMPTY byte array means failure — the implementation retries three times with a short
 * delay and only then reports, so a transient WebView hiccup never reaches the user.
 */
interface PdfPort {
    suspend fun render(html: String, jobName: String): ByteArray
}

@Singleton
class AndroidPdfPort @Inject constructor(
    @ApplicationContext private val appContext: Context,
) : PdfPort {

    override suspend fun render(html: String, jobName: String): ByteArray {
        repeat(3) { attempt ->
            val bytes = AndroidPdfRenderer.exportPdf(appContext, html, jobName)
            if (bytes.isNotEmpty()) return bytes
            Log.w("DocumentRepository", "PDF export attempt ${attempt + 1} produced no bytes")
            if (attempt < 2) delay(250L)
        }
        return ByteArray(0)
    }
}

/** Share and print actions over already-rendered bytes (§9.11's distribution stage). */
interface PdfActions {
    fun share(pdfBytes: ByteArray, fileName: String, chooserTitle: String)
    fun print(pdfBytes: ByteArray, jobName: String)
    fun saveToDownloads(pdfBytes: ByteArray, fileName: String): Uri?
}

@Singleton
class AndroidPdfActions @Inject constructor(
    @ApplicationContext private val appContext: Context,
) : PdfActions {

    override fun share(pdfBytes: ByteArray, fileName: String, chooserTitle: String) =
        AndroidPdfRenderer.sharePdf(appContext, pdfBytes, fileName, chooserTitle)

    override fun print(pdfBytes: ByteArray, jobName: String) =
        AndroidPdfRenderer.printBytes(appContext, pdfBytes, jobName)

    override fun saveToDownloads(pdfBytes: ByteArray, fileName: String): Uri? =
        AndroidPdfRenderer.saveToDownloads(appContext, pdfBytes, fileName)
}
