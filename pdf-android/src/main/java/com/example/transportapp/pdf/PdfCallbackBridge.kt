package android.print

import android.print.PrintDocumentAdapter.LayoutResultCallback
import android.print.PrintDocumentAdapter.WriteResultCallback

/**
 * Bridges the package-private print callback types so application code can drive a
 * [PrintDocumentAdapter] headlessly (onLayout -> onWrite) without a PrintManager session.
 * Declared inside the `android.print` package because the callback constructors are
 * inaccessible from any other package.
 *
 * Ported verbatim from the BillTemplatePrototype (Implementation.md §9.8, checklist item 8).
 */
object PdfCallbackBridge {

    fun layoutCallback(
        onLayoutFinished: (info: PrintDocumentInfo?, changed: Boolean) -> Unit,
        onLayoutFailed: (error: CharSequence?) -> Unit
    ): LayoutResultCallback =
        object : LayoutResultCallback() {
            override fun onLayoutFinished(info: PrintDocumentInfo, changed: Boolean) {
                onLayoutFinished(info, changed)
            }

            override fun onLayoutFailed(error: CharSequence?) {
                onLayoutFailed(error)
            }
        }

    fun writeCallback(
        onWriteFinished: (pages: Array<out PageRange>) -> Unit,
        onWriteFailed: (error: CharSequence?) -> Unit
    ): WriteResultCallback =
        object : WriteResultCallback() {
            override fun onWriteFinished(pages: Array<out PageRange>) {
                onWriteFinished(pages)
            }

            override fun onWriteFailed(error: CharSequence?) {
                onWriteFailed(error)
            }
        }
}
