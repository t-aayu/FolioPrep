package com.example.domain

enum class QuestionMode(val label: String) {
    MCQ("Prelims MCQ"),
    SUBJECTIVE("Mains 10/15M"),
    BOTH("Both")
}

enum class QuestionDifficulty(
    val label: String,
    val jsonValue: String,
    val description: String
) {
    EASY("Easy", "Easy", "Foundational NCERT & direct conceptual clarity"),
    MEDIUM("Medium", "Medium", "Standard UPSC CSE analytical & multi-statement level"),
    HARD("Hard", "Hard", "Elimination-resistant high-order UPSC CSE difficulty");

    companion object {
        fun fromRaw(raw: String?): QuestionDifficulty {
            val normalized = raw?.trim()?.lowercase().orEmpty()
            return when {
                normalized.startsWith("easy") || normalized.startsWith("basic") || normalized == "low" -> EASY
                normalized.startsWith("hard") || normalized.startsWith("tough") || normalized.startsWith("difficult") || normalized == "high" -> HARD
                else -> MEDIUM
            }
        }
    }
}

object UpscSubjects {
    val mcqTags: List<String> = listOf(
        "Polity",
        "Economy",
        "Environment",
        "History",
        "Geography",
        "Sci-Tech",
        "IR",
        "Government Schemes",
        "Current Affairs"
    )

    val mainsTags: List<String> = listOf(
        "Geography",
        "Indian History",
        "World History",
        "Indian Society",
        "Polity and Governance",
        "IR",
        "Social Justice",
        "Economy",
        "Internal Security",
        "Sci-Tech",
        "Environment Biodiversity",
        "Disaster Management",
        "Ethics integrity and aptitude"
    )

    val bothTags: List<String> = listOf(
        "History",
        "Geography",
        "Polity",
        "IR",
        "Economy",
        "Sci-tech",
        "Environment"
    )

    fun tagsForMode(mode: QuestionMode): List<String> = when (mode) {
        QuestionMode.MCQ -> mcqTags
        QuestionMode.SUBJECTIVE -> mainsTags
        QuestionMode.BOTH -> bothTags
    }

    val allTags: List<String> = (mcqTags + mainsTags + bothTags).distinctBy { it.lowercase() }
}

object UpscPromptBuilder {

    const val DEFAULT_SKILL_PROMPT: String =
        "Act strictly as a senior UPSC Civil Services Examination (CSE) Question Setter — a seasoned university professor who drafts official UPSC CSE question papers."

    fun buildPrompt(
        sourceText: String,
        latestPrelimsYear: Int,
        latestMainsYear: Int,
        questionMode: QuestionMode,
        mcqCount: Int,
        mains10Count: Int,
        mains15Count: Int,
        subjectTag: String?,
        difficulty: QuestionDifficulty = QuestionDifficulty.MEDIUM,
        useCustomSkill: Boolean = false,
        customSkillText: String = ""
    ): String {
        val includeMcq = (questionMode == QuestionMode.MCQ || questionMode == QuestionMode.BOTH) && mcqCount > 0
        val includeMains10 = (questionMode == QuestionMode.SUBJECTIVE || questionMode == QuestionMode.BOTH) && mains10Count > 0
        val includeMains15 = (questionMode == QuestionMode.SUBJECTIVE || questionMode == QuestionMode.BOTH) && mains15Count > 0
        val includeSubjective = includeMains10 || includeMains15

        val taskParts = mutableListOf<String>()
        if (includeMcq) {
            taskParts.add("$mcqCount UPSC Prelims MCQ(s)")
        }
        if (includeMains10) {
            taskParts.add("$mains10Count UPSC Mains 10-mark (150 words) subjective question(s)")
        }
        if (includeMains15) {
            taskParts.add("$mains15Count UPSC Mains 15-mark (250 words) subjective question(s)")
        }

        val taskSummary = if (taskParts.isEmpty()) {
            "0 questions (please set MCQ, 10-mark, or 15-mark count above 0 before sharing)"
        } else {
            taskParts.joinToString(" and ")
        }

        val subjectLine = if (!subjectTag.isNullOrBlank()) {
            "SUBJECT / PAPER FOCUS: $subjectTag (UPSC CSE)."
        } else {
            "SUBJECT / PAPER FOCUS: Infer the most appropriate UPSC CSE GS subject tags from the source text."
        }

        val patternRules = buildString {
            if (includeMcq) {
                appendLine(
                    "- PRELIMS PATTERN CALIBRATION (Up to $latestPrelimsYear): Strictly emulate the official UPSC CSE Prelims $latestPrelimsYear pattern AND all previous UPSC CSE Prelims years up to $latestPrelimsYear. Incorporate authentic recent UPSC question styles including multi-statement counting ('How many of the statements given above are correct?'), Statement-I / Statement-II assertion-reasoning, pair-matching, subtle conceptual distinctions, and elimination-resistant framing."
                )
            }
            if (includeSubjective) {
                appendLine(
                    "- MAINS PATTERN CALIBRATION (Up to $latestMainsYear): Strictly emulate the official UPSC CSE Mains $latestMainsYear pattern AND all previous UPSC CSE Mains years up to $latestMainsYear. Use authentic UPSC directive words (Critically examine, Discuss, Elucidate, Analyze, Comment), calibrate depth for 10-mark (150 words) and 15-mark (250 words) questions, and structure every model answer into a crisp Introduction, multi-dimensional Key Arguments, and balanced Conclusion/Way Forward."
                )
            }
            if (!includeMcq && !includeSubjective) {
                appendLine(
                    "- PATTERN CALIBRATION: Strictly align with UPSC CSE Prelims (up to $latestPrelimsYear) and Mains (up to $latestMainsYear) patterns and all previous years."
                )
            }
        }.trimEnd()

        val schemaBlock = when {
            includeMcq && includeSubjective -> """
{
  "title": "Concise UPSC Topic Title",
  "tags": [
    "Polity"
  ],
  "mcqs": [
    {
      "q": "Consider the following statements:\n1. First statement here.\n2. Second statement here.\n\nHow many of the statements given above are correct?",
      "opts": [
        "Only one",
        "Only two",
        "All of the above",
        "None"
      ],
      "ans": 0,
      "difficulty": "${difficulty.jsonValue}",
      "exp": "Statement 1 is correct because ...\nStatement 2 is incorrect because ..."
    }
  ],
  "subjective": [
    {
      "q": "Mains question with authentic UPSC directive (150/250 words)",
      "marks": 10,
      "intro": "2-3 sentence contextual introduction",
      "points": [
        "Key dimension 1 with facts/articles",
        "Key dimension 2",
        "Key dimension 3",
        "Key dimension 4"
      ],
      "conclusion": "Balanced way forward / constitutional synthesis"
    }
  ]
}
            """.trimIndent()

            includeSubjective -> """
{
  "title": "Concise UPSC Topic Title",
  "tags": [
    "Environment"
  ],
  "subjective": [
    {
      "q": "Mains question with authentic UPSC directive (150/250 words)",
      "marks": 10,
      "intro": "2-3 sentence contextual introduction",
      "points": [
        "Key dimension 1 with facts/articles",
        "Key dimension 2",
        "Key dimension 3",
        "Key dimension 4"
      ],
      "conclusion": "Balanced way forward / constitutional synthesis"
    }
  ]
}
            """.trimIndent()

            else -> """
{
  "title": "Concise UPSC Topic Title",
  "tags": [
    "Polity"
  ],
  "mcqs": [
    {
      "q": "Consider the following statements:\n1. First statement here.\n2. Second statement here.\n\nHow many of the statements given above are correct?",
      "opts": [
        "Only one",
        "Only two",
        "All of the above",
        "None"
      ],
      "ans": 0,
      "difficulty": "${difficulty.jsonValue}",
      "exp": "Statement 1 is correct because ...\nStatement 2 is incorrect because ..."
    }
  ]
}
            """.trimIndent()
        }

        val cleanSource = sourceText.trim().ifEmpty { "[Paste text, extract a link, or scan notes with OCR above]" }
        val activeSkillLine = if (useCustomSkill && customSkillText.isNotBlank()) {
            customSkillText.trim()
        } else {
            DEFAULT_SKILL_PROMPT
        }

        return buildString {
            appendLine(activeSkillLine)
            appendLine("Using the source text below as a topic/current-affairs anchor, generate $taskSummary aligned with the UPSC CSE syllabus and Previous Year Question (PYQ) trends.")
            appendLine(subjectLine)
            appendLine("TARGET HARDNESS / DIFFICULTY LEVEL: ${difficulty.label} — ${difficulty.description}.")
            appendLine()
            appendLine("EXAM PATTERN & SYLLABUS RULES:")
            appendLine(patternRules)
            appendLine("- SYLLABUS & PYQ CORRELATION: Do NOT restrict questions solely to facts mentioned in the source text. Treat the passage as a current-affairs or conceptual trigger, and freely integrate correlated static UPSC CSE syllabus knowledge (constitutional articles, acts, institutions, ecological linkages, economic mechanisms, and recurring PYQ themes) just like real UPSC papers.")
            if (includeMcq) {
                appendLine("- For every MCQ: provide exactly 4 options in \"opts\", set \"ans\" to the 0-based index (0, 1, 2, or 3) of the single correct option, and include \"difficulty\" set to \"Easy\", \"Medium\", or \"Hard\" reflecting the question's actual UPSC CSE hardness level (calibrated primarily around ${difficulty.label}).")
            }
            if (includeMains10 || includeMains15) {
                appendLine("- For every subjective item: set \"marks\" to 10 for 10-mark questions ($mains10Count) and 15 for 15-mark questions ($mains15Count).")
            }
            appendLine("- OUTPUT FORMAT: Return ONLY a single fenced JSON code snippet block (```json ... ```) formatted cleanly with proper spacing and indentation, with no greetings, commentary, or extra text outside the code block so it is easy to read and copy in one tap.")
            appendLine("- Use appropriate \\n line breaks inside multi-statement UPSC question stems (e.g., separating numbered statements 1., 2., 3. and the concluding question) and explanations for clear readability.")
            appendLine("- Follow this exact JSON schema inside the ```json code snippet:")
            appendLine("```json")
            appendLine(schemaBlock)
            appendLine("```")
            appendLine()
            appendLine("SOURCE TEXT:")
            append(cleanSource)
        }
    }
}
