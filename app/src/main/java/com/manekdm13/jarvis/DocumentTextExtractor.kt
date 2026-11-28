package com.manekdm13.jarvis

import android.content.Context
import android.net.Uri
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.TimeUnit

/**
 * On-device OCR for an image explicitly selected by the user.
 *
 * The extracted text stays in the app until the user reviews it and presses Send.
 * This class does not upload images or extracted text to any service.
 */
internal object DocumentTextExtractor {
    fun extractImageText(context: Context, imageUri: Uri): String {
        val image = InputImage.fromFilePath(context, imageUri)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        return try {
            val result = Tasks.await(recognizer.process(image), 45, TimeUnit.SECONDS)
            result.text
                .replace(Regex("[\\t ]+"), " ")
                .replace(Regex("\\n{3,}"), "\\n\\n")
                .trim()
                .take(MAX_EXTRACTED_CHARS)
        } finally {
            recognizer.close()
        }
    }

    private const val MAX_EXTRACTED_CHARS = 18_000
}
