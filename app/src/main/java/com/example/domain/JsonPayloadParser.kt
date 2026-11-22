package com.example.domain

import org.json.JSONArray
import org.json.JSONObject

data class DraftMcq(
    val tempId: String,
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String,
    val difficulty: String = QuestionDifficulty.MEDIUM.label
)

data class DraftSubjective(
    val tempId: String,
    val question: String,
    val marks: Int,
    val intro: String,
    val keyPoints: List<String>,
    val conclusion: String
)

data class ParsedStudyPayload(
    val title: String,
    val tags: List<String>,
    val mcqs: List<DraftMcq>,
    val subjective: List<DraftSubjective>,
    val warnings: List<String> = emptyList()
) {
    val totalCount: Int get() = mcqs.size + subjective.size
}

object JsonPayloadParser {

    fun looksLikeStudyPayload(rawClipboard: String): Boolean {
        if (rawClipboard.length < 25) return false
        val jsonCandidate = extractJsonObject(rawClipboard) ?: return false
        val hasExpectedKeys = jsonCandidate.contains("\"mcqs\"") ||
            jsonCandidate.contains("\"subjective\"") ||
            jsonCandidate.contains("\"questions\"")
        if (!hasExpectedKeys) return false
        return parse(rawClipboard).getOrNull()?.let { it.totalCount > 0 } == true
    }

    fun extractJsonObject(rawClipboard: String): String? {
        // Prefer content inside a fenced ```json ... ``` or ``` ... ``` code snippet if present
        val fencedRegex = Regex("```(?:json)?\\s*([\\s\\S]*?)```", RegexOption.IGNORE_CASE)
        val fencedMatch = fencedRegex.find(rawClipboard)
        var cleaned = (fencedMatch?.groupValues?.getOrNull(1) ?: rawClipboard)
            .replace(Regex("```json\\s*", RegexOption.IGNORE_CASE), "")
            .replace("```", "")
            .replace('\u201C', '"')
            .replace('\u201D', '"')
            .replace("\r\n", "\n")
            // Repair invalid markdown-escaped punctuation copied from mobile AI apps (\[ \] \{ \} \& \- \_ \*)
            .replace(Regex("""\\([\[\]{}&\-_*])"""), "$1")
            .trim()

        val start = cleaned.indexOf('{')
        val end = cleaned.lastIndexOf('}')
        if (start == -1 || end == -1 || end <= start) return null

        cleaned = cleaned.substring(start, end + 1)
        // Preserve pretty-printed newlines/tabs outside strings, and safely escape raw newlines/tabs inside strings
        cleaned = sanitizeControlCharsInJsonStrings(cleaned)
        // Strip common trailing commas before closing brackets/braces while preserving surrounding indentation/newlines
        cleaned = cleaned.replace(Regex(",(\\s*[}\\]])"), "$1")
        return cleaned
    }

    private fun sanitizeControlCharsInJsonStrings(input: String): String {
        val sb = StringBuilder(input.length + 16)
        var inString = false
        var escaped = false
        var i = 0
        while (i < input.length) {
            val ch = input[i]
            if (!inString) {
                if (ch == '"') {
                    inString = true
                }
                sb.append(ch)
            } else {
                if (escaped) {
                    sb.append(ch)
                    escaped = false
                } else {
                    when (ch) {
                        '\\' -> {
                            sb.append(ch)
                            escaped = true
                        }
                        '"' -> {
                            inString = false
                            sb.append(ch)
                        }
                        '\n' -> sb.append("\\n")
                        '\r' -> {
                            // Handled with \n if CRLF, otherwise escape as \n
                            if (i + 1 >= input.length || input[i + 1] != '\n') {
                                sb.append("\\n")
                            }
                        }
                        '\t' -> sb.append("\\t")
                        else -> sb.append(ch)
                    }
                }
            }
            i++
        }
        return sb.toString()
    }

    private fun cleanDisplayText(raw: String): String {
        return raw
            .replace("\\r\\n", "\n")
            .replace("\\n", "\n")
            .replace("\\t", "\t")
            .replace("\r\n", "\n")
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()
    }

    fun parse(
        rawInput: String,
        fallbackTitle: String = "UPSC CSE Study Set",
        defaultDifficulty: QuestionDifficulty = QuestionDifficulty.MEDIUM
    ): Result<ParsedStudyPayload> {
        return runCatching {
            val jsonText = extractJsonObject(rawInput)
                ?: throw IllegalArgumentException(
                    "Could not locate a JSON object ({ ... }) in the pasted text. Copy the AI's JSON code snippet and try again."
                )

            val root = JSONObject(jsonText)
            val warnings = mutableListOf<String>()

            val title = cleanDisplayText(root.optString("title", "")).ifEmpty { fallbackTitle }
            val tags = parseStringList(root.optJSONArray("tags"))

            val mcqs = mutableListOf<DraftMcq>()
            val mcqArray = root.optJSONArray("mcqs") ?: root.optJSONArray("questions")
            if (mcqArray != null) {
                for (i in 0 until mcqArray.length()) {
                    val obj = mcqArray.optJSONObject(i) ?: continue
                    val q = cleanDisplayText(obj.optString("q", obj.optString("question", "")))
                    if (q.isEmpty()) {
                        warnings.add("Skipped MCQ #${i + 1}: missing question text.")
                        continue
                    }

                    val rawOpts = parseStringList(obj.optJSONArray("opts") ?: obj.optJSONArray("options"))
                    val normalizedOpts = when {
                        rawOpts.size >= 4 -> rawOpts.take(4)
                        rawOpts.isNotEmpty() -> rawOpts + List(4 - rawOpts.size) { "Option ${('A' + rawOpts.size + it)}" }
                        else -> {
                            warnings.add("Skipped MCQ #${i + 1}: missing options.")
                            continue
                        }
                    }

                    val ansRaw = obj.opt("ans") ?: obj.opt("answer") ?: obj.opt("correctIndex")
                    val ansIndex = normalizeAnswerIndex(ansRaw)
                    val exp = cleanDisplayText(obj.optString("exp", obj.optString("explanation", "")))
                    val rawDifficulty = obj.optString(
                        "difficulty",
                        obj.optString("hardness", obj.optString("level", ""))
                    ).trim()
                    val normalizedDifficulty = if (rawDifficulty.isNotEmpty()) {
                        QuestionDifficulty.fromRaw(rawDifficulty).label
                    } else {
                        defaultDifficulty.label
                    }

                    mcqs.add(
                        DraftMcq(
                            tempId = "mcq_${System.nanoTime()}_$i",
                            question = q,
                            options = normalizedOpts,
                            correctIndex = ansIndex,
                            explanation = exp.ifEmpty { "Review the correlated UPSC CSE syllabus concept and source context." },
                            difficulty = normalizedDifficulty
                        )
                    )
                }
            }

            val subjectiveList = mutableListOf<DraftSubjective>()
            val subjArray = root.optJSONArray("subjective")
                ?: root.optJSONArray("mains")
                ?: root.optJSONArray("subjective_questions")
            if (subjArray != null) {
                for (i in 0 until subjArray.length()) {
                    val obj = subjArray.optJSONObject(i) ?: continue
                    val q = cleanDisplayText(obj.optString("q", obj.optString("question", "")))
                    if (q.isEmpty()) {
                        warnings.add("Skipped Subjective #${i + 1}: missing question text.")
                        continue
                    }

                    val rawMarks = obj.optInt("marks", 10)
                    val marks = if (rawMarks >= 13) 15 else 10
                    val intro = cleanDisplayText(obj.optString("intro", obj.optString("introduction", "")))
                    val points = parseStringList(
                        obj.optJSONArray("points")
                            ?: obj.optJSONArray("keyPoints")
                            ?: obj.optJSONArray("body")
                    )
                    val conclusion = cleanDisplayText(obj.optString("conclusion", obj.optString("wayForward", "")))

                    subjectiveList.add(
                        DraftSubjective(
                            tempId = "subj_${System.nanoTime()}_$i",
                            question = q,
                            marks = marks,
                            intro = intro.ifEmpty { "Define the core concept and contextualize with current relevance." },
                            keyPoints = points.ifEmpty { listOf("Outline key arguments, constitutional/policy dimensions, and PYQ-relevant linkages.") },
                            conclusion = conclusion.ifEmpty { "Conclude with a balanced, forward-looking administrative synthesis." }
                        )
                    )
                }
            }

            if (mcqs.isEmpty() && subjectiveList.isEmpty()) {
                throw IllegalArgumentException(
                    "JSON was valid, but contained neither 'mcqs' nor 'subjective' questions."
                )
            }

            ParsedStudyPayload(
                title = title,
                tags = tags,
                mcqs = mcqs,
                subjective = subjectiveList,
                warnings = warnings
            )
        }
    }

    private fun parseStringList(array: JSONArray?): List<String> {
        if (array == null) return emptyList()
        return buildList {
            for (i in 0 until array.length()) {
                val value = cleanDisplayText(array.optString(i, ""))
                if (value.isNotEmpty()) add(value)
            }
        }
    }

    private fun normalizeAnswerIndex(raw: Any?): Int {
        return when (raw) {
            is Number -> {
                val v = raw.toInt()
                when {
                    v in 0..3 -> v
                    v in 1..4 -> v - 1
                    else -> 0
                }
            }
            is String -> {
                val t = raw.trim().uppercase()
                when {
                    t.startsWith("A") || t == "0" -> 0
                    t.startsWith("B") || t == "1" -> 1
                    t.startsWith("C") || t == "2" -> 2
                    t.startsWith("D") || t == "3" -> 3
                    t == "4" -> 3
                    else -> 0
                }
            }
            else -> 0
        }
    }
}
