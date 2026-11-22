package com.example.domain

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class OcrExtractor(private val context: Context) {

    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    suspend fun extractTextFromUris(uris: List<Uri>): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            require(uris.isNotEmpty()) { "Select at least one image of notes, slides, or textbook pages." }
            val pageOutputs = mutableListOf<String>()

            uris.forEachIndexed { index, uri ->
                val image = InputImage.fromFilePath(context, uri)
                val recognized = recognizeImage(image)
                val cleaned = recognized
                    .replace(Regex("[ \\t]+"), " ")
                    .replace(Regex("\n{3,}"), "\n\n")
                    .trim()
                if (cleaned.isNotEmpty()) {
                    if (uris.size > 1) {
                        pageOutputs.add("--- Page ${index + 1} ---\n$cleaned")
                    } else {
                        pageOutputs.add(cleaned)
                    }
                }
            }

            val combined = pageOutputs.joinToString("\n\n").trim()
            if (combined.isEmpty()) {
                throw IllegalStateException("No readable text was detected in the selected image(s). Try a sharper or better-lit photo.")
            }
            combined
        }
    }

    private suspend fun recognizeImage(image: InputImage): String =
        suspendCancellableCoroutine { continuation ->
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val blockText = visionText.textBlocks.joinToString("\n\n") { block ->
                        block.lines.joinToString(" ") { line -> line.text.trim() }
                    }
                    if (continuation.isActive) {
                        continuation.resume(blockText.ifBlank { visionText.text })
                    }
                }
                .addOnFailureListener { error ->
                    if (continuation.isActive) {
                        continuation.resumeWithException(error)
                    }
                }
        }
}
