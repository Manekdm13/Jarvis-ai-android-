package com.manekdm13.jarvis

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.TimeUnit

/**
 * On-device OCR for images and PDFs explicitly selected by the user.
 *
 * Extracted text stays in the app until the user reviews it and presses Send.
 * This class does not upload source files or extracted text to any service.
 */
internal object DocumentTextExtractor {
    fun extractImageText(context: Context, imageUri: Uri): String {
        val image = InputImage.fromFilePath(context, imageUri)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        return try {
            val result = Tasks.await(recognizer.process(image), OCR_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            normalize(result.text)
        } finally {
            recognizer.close()
        }
    }

    fun extractImagesText(context: Context, imageUris: List<Uri>): String {
        require(imageUris.isNotEmpty()) { "Select at least one image." }
        val selected = imageUris.take(MAX_GALLERY_IMAGES)
        val pages = selected.mapIndexedNotNull { index, uri ->
            val text = extractImageText(context, uri)
            if (text.isBlank()) null else "IMAGE ${index + 1}\n$text"
        }
        val limitNotice = if (imageUris.size > MAX_GALLERY_IMAGES) {
            "\n\n[For safety, only the first $MAX_GALLERY_IMAGES selected images were scanned.]"
        } else ""
        return (pages.joinToString("\n\n") + limitNotice).take(MAX_EXTRACTED_CHARS)
    }

    fun extractPdfText(context: Context, pdfUri: Uri): String {
        val descriptor = context.contentResolver.openFileDescriptor(pdfUri, "r")
            ?: throw IllegalArgumentException("Android could not open the selected PDF.")
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val extractedPages = mutableListOf<String>()
        var totalPages = 0
        var pagesProcessed = 0
        var reachedTextLimit = false
        try {
            PdfRenderer(descriptor).use { renderer ->
                totalPages = renderer.pageCount
                if (totalPages == 0) {
                    throw IllegalArgumentException("The selected PDF has no pages.")
                }
                val pagesToRead = minOf(totalPages, MAX_PDF_PAGES)
                for (pageIndex in 0 until pagesToRead) {
                    pagesProcessed = pageIndex + 1
                    renderer.openPage(pageIndex).use { page ->
                        val scale = minOf(1f, MAX_RENDER_DIMENSION.toFloat() / maxOf(page.width, page.height))
                        val width = (page.width * scale).toInt().coerceAtLeast(1)
                        val height = (page.height * scale).toInt().coerceAtLeast(1)
                        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        try {
                            page.render(bitmap, Rect(0, 0, width, height), null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            val image = InputImage.fromBitmap(bitmap, 0)
                            val text = Tasks.await(
                                recognizer.process(image),
                                OCR_TIMEOUT_SECONDS,
                                TimeUnit.SECONDS
                            ).text
                            if (text.isNotBlank()) {
                                extractedPages += "PAGE ${pageIndex + 1}\n${normalize(text)}"
                            }
                        } finally {
                            bitmap.recycle()
                        }
                    }
                    if (extractedPages.sumOf { it.length } >= MAX_EXTRACTED_CHARS) {
                        reachedTextLimit = true
                        break
                    }
                }
            }
        } finally {
            recognizer.close()
            descriptor.close()
        }
        val suffix = if (pagesProcessed < totalPages || reachedTextLimit) {
            "\n\n[Scanning stopped at the safe page or text limit.]"
        } else ""
        return (extractedPages.joinToString("\n\n") + suffix).take(MAX_EXTRACTED_CHARS)
    }

    private fun normalize(text: String): String = text
        .replace(Regex("[\\t ]+"), " ")
        .replace(Regex("\\n{3,}"), "\\n\\n")
        .trim()
        .take(MAX_EXTRACTED_CHARS)

    private const val MAX_EXTRACTED_CHARS = 18_000
    private const val MAX_PDF_PAGES = 20
    private const val MAX_GALLERY_IMAGES = 10
    private const val MAX_RENDER_DIMENSION = 1800
    private const val OCR_TIMEOUT_SECONDS = 45L
}
