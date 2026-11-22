package com.example.domain

data class TextChunk(
    val index: Int,
    val text: String,
    val wordCount: Int,
    val charCount: Int
)

object TextChunker {
    const val MAX_WORDS_PER_CHUNK = 2500
    const val MAX_CHARS_PER_CHUNK = 14500

    fun countWords(text: String): Int {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return 0
        return trimmed.split(Regex("\\s+")).size
    }

    fun chunkText(rawText: String): List<TextChunk> {
        val normalized = rawText
            .replace("\r\n", "\n")
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()
        if (normalized.isEmpty()) return emptyList()

        val totalWords = countWords(normalized)
        if (totalWords <= MAX_WORDS_PER_CHUNK && normalized.length <= MAX_CHARS_PER_CHUNK) {
            return listOf(
                TextChunk(
                    index = 0,
                    text = normalized,
                    wordCount = totalWords,
                    charCount = normalized.length
                )
            )
        }

        val paragraphs = normalized.split(Regex("\n\n+")).map { it.trim() }.filter { it.isNotEmpty() }
        val chunks = mutableListOf<TextChunk>()
        val currentBuilder = StringBuilder()
        var currentWords = 0

        fun flushChunk() {
            val built = currentBuilder.toString().trim()
            if (built.isNotEmpty()) {
                chunks.add(
                    TextChunk(
                        index = chunks.size,
                        text = built,
                        wordCount = countWords(built),
                        charCount = built.length
                    )
                )
            }
            currentBuilder.clear()
            currentWords = 0
        }

        for (paragraph in paragraphs) {
            val pWords = countWords(paragraph)
            if (pWords > MAX_WORDS_PER_CHUNK || paragraph.length > MAX_CHARS_PER_CHUNK) {
                // Split oversized paragraph by words safely
                if (currentBuilder.isNotEmpty()) flushChunk()
                val words = paragraph.split(Regex("\\s+"))
                var start = 0
                while (start < words.size) {
                    val subBuilder = StringBuilder()
                    var count = 0
                    while (start < words.size && count < MAX_WORDS_PER_CHUNK &&
                        subBuilder.length + words[start].length + 1 <= MAX_CHARS_PER_CHUNK
                    ) {
                        if (subBuilder.isNotEmpty()) subBuilder.append(' ')
                        subBuilder.append(words[start])
                        start++
                        count++
                    }
                    val piece = subBuilder.toString().trim()
                    if (piece.isNotEmpty()) {
                        chunks.add(
                            TextChunk(
                                index = chunks.size,
                                text = piece,
                                wordCount = countWords(piece),
                                charCount = piece.length
                            )
                        )
                    }
                }
                continue
            }

            val wouldExceedWords = currentWords + pWords > MAX_WORDS_PER_CHUNK
            val wouldExceedChars = currentBuilder.length + paragraph.length + 2 > MAX_CHARS_PER_CHUNK
            if (currentBuilder.isNotEmpty() && (wouldExceedWords || wouldExceedChars)) {
                flushChunk()
            }

            if (currentBuilder.isNotEmpty()) currentBuilder.append("\n\n")
            currentBuilder.append(paragraph)
            currentWords += pWords
        }

        if (currentBuilder.isNotEmpty()) flushChunk()
        return chunks
    }
}
