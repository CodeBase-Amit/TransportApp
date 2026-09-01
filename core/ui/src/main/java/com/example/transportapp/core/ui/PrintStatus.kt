package com.example.transportapp.core.ui

/**
 * The document print/share lifecycle (Phase 3 S13): rendering a document takes a beat, so
 * the screens show progress; errors carry the typed copy from ErrorCopy and are dismissible.
 */
sealed interface PrintStatus {
    data object Idle : PrintStatus
    data class Rendering(val message: String) : PrintStatus
    data class Error(val message: String) : PrintStatus
}
