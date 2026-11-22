package com.example.domain

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import com.example.data.local.McqQuestionEntity
import java.io.File
import java.io.FileOutputStream

object QuestionShareHelper {

    fun formatQuestionWithOptions(
        questionNumber: Int,
        question: McqQuestionEntity
    ): String {
        val opts = question.optionsList()
        return buildString {
            appendLine("Q$questionNumber. ${question.question.trim()}")
            appendLine()
            opts.forEachIndexed { idx, opt ->
                val letter = 'A' + idx
                append("$letter. ${opt.trim()}")
                if (idx < opts.lastIndex) appendLine()
            }
        }
    }

    fun copyQuestionToClipboard(
        context: Context,
        questionNumber: Int,
        question: McqQuestionEntity
    ) {
        val formatted = formatQuestionWithOptions(questionNumber, question)
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(
            ClipData.newPlainText("FolioPrep UPSC Question", formatted)
        )
    }

    fun shareQuestionScreenshot(
        context: Context,
        deckTitle: String,
        questionNumber: Int,
        question: McqQuestionEntity,
        isDarkTheme: Boolean
    ): Result<Unit> = runCatching {
        val file = renderQuestionScreenshotPng(
            context = context,
            deckTitle = deckTitle,
            questionNumber = questionNumber,
            question = question,
            isDarkTheme = isDarkTheme
        )
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "UPSC CSE Question #$questionNumber")
            clipData = ClipData.newRawUri("UPSC Question Screenshot", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(shareIntent, "Share Question Screenshot").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(chooser)
    }

    fun formatExplanationText(
        questionNumber: Int,
        question: McqQuestionEntity
    ): String {
        val opts = question.optionsList()
        val correctLetter = 'A' + question.correctIndex.coerceIn(0, 3)
        val correctOptionText = opts.getOrElse(question.correctIndex) { "" }.trim()
        return buildString {
            appendLine("Q$questionNumber Explanation")
            if (correctOptionText.isNotEmpty()) {
                appendLine("Correct Answer: $correctLetter. $correctOptionText")
            }
            appendLine()
            append(question.explanation.trim())
        }
    }

    fun copyExplanationToClipboard(
        context: Context,
        questionNumber: Int,
        question: McqQuestionEntity
    ) {
        val formatted = formatExplanationText(questionNumber, question)
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(
            ClipData.newPlainText("FolioPrep UPSC Explanation", formatted)
        )
    }

    fun shareExplanationScreenshot(
        context: Context,
        deckTitle: String,
        questionNumber: Int,
        question: McqQuestionEntity,
        isDarkTheme: Boolean
    ): Result<Unit> = runCatching {
        val file = renderExplanationScreenshotPng(
            context = context,
            deckTitle = deckTitle,
            questionNumber = questionNumber,
            question = question,
            isDarkTheme = isDarkTheme
        )
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "UPSC CSE Question #$questionNumber Explanation")
            clipData = ClipData.newRawUri("UPSC Explanation Screenshot", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(shareIntent, "Share Explanation Screenshot").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(chooser)
    }

    private fun renderQuestionScreenshotPng(
        context: Context,
        deckTitle: String,
        questionNumber: Int,
        question: McqQuestionEntity,
        isDarkTheme: Boolean
    ): File {
        val width = 1080
        val outerMargin = 48f
        val cardPadding = 44f
        val contentWidth = (width - (outerMargin * 2) - (cardPadding * 2)).toInt()

        // Palette aligned with FolioPrep Warm Editorial Paper / Warm Ink
        val bgColor = if (isDarkTheme) Color.parseColor("#161513") else Color.parseColor("#FAF7F2")
        val cardColor = if (isDarkTheme) Color.parseColor("#211F1C") else Color.parseColor("#FFFDF9")
        val optionBgColor = if (isDarkTheme) Color.parseColor("#2B2824") else Color.parseColor("#F4EFE6")
        val borderColor = if (isDarkTheme) Color.parseColor("#3E3A33") else Color.parseColor("#DFD8CB")
        val accentColor = if (isDarkTheme) Color.parseColor("#8CC7A8") else Color.parseColor("#2A5942")
        val accentContainerColor = if (isDarkTheme) Color.parseColor("#1E3E2E") else Color.parseColor("#DCEAE2")
        val accentOnContainerColor = if (isDarkTheme) Color.parseColor("#DCEAE2") else Color.parseColor("#153425")
        val titleTextColor = if (isDarkTheme) Color.parseColor("#F2EDE4") else Color.parseColor("#1C1B18")
        val mutedTextColor = if (isDarkTheme) Color.parseColor("#C5BEB2") else Color.parseColor("#5B574F")

        val badgePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentOnContainerColor
            textSize = 28f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val deckTitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = mutedTextColor
            textSize = 30f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        val questionPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = titleTextColor
            textSize = 40f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        }
        val optionBadgePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentOnContainerColor
            textSize = 30f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val optionTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = titleTextColor
            textSize = 34f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        val footerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = mutedTextColor
            textSize = 26f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }

        val deckLayout = if (deckTitle.isNotBlank()) {
            buildLayout(deckTitle.trim(), deckTitlePaint, contentWidth)
        } else null
        val questionLayout = buildLayout(question.question.trim(), questionPaint, contentWidth)

        val optionBoxHorizontalPad = 24f
        val optionBoxVerticalPad = 24f
        val badgeSize = 54f
        val badgeGap = 22f
        val optionTextWidth = (contentWidth - (optionBoxHorizontalPad * 2) - badgeSize - badgeGap).toInt()
        val optionLayouts = question.optionsList().map { opt ->
            buildLayout(opt.trim(), optionTextPaint, optionTextWidth)
        }

        val badgeHeight = 56f
        val optionSpacing = 20f
        var calculatedCardHeight = cardPadding + badgeHeight + 20f
        if (deckLayout != null) {
            calculatedCardHeight += deckLayout.height + 24f
        }
        calculatedCardHeight += questionLayout.height + 36f
        optionLayouts.forEach { layout ->
            val rowHeight = maxOf(layout.height.toFloat(), badgeSize) + (optionBoxVerticalPad * 2)
            calculatedCardHeight += rowHeight + optionSpacing
        }
        calculatedCardHeight += 24f + 36f + cardPadding

        val totalHeight = (calculatedCardHeight + (outerMargin * 2)).toInt().coerceAtLeast(600)
        val bitmap = Bitmap.createBitmap(width, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Background
        canvas.drawColor(bgColor)

        // Outer Card
        val cardRect = RectF(
            outerMargin,
            outerMargin,
            width - outerMargin,
            totalHeight - outerMargin
        )
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = cardColor
        }
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = borderColor
            strokeWidth = 2.5f
        }
        canvas.drawRoundRect(cardRect, 36f, 36f, fillPaint)
        canvas.drawRoundRect(cardRect, 36f, 36f, strokePaint)

        val startX = outerMargin + cardPadding
        var cursorY = outerMargin + cardPadding

        // Header Pill ("UPSC CSE · Q1 · MEDIUM")
        val diffLabel = QuestionDifficulty.fromRaw(question.difficulty).label.uppercase()
        val badgeText = "UPSC CSE  ·  Q$questionNumber  ·  $diffLabel"
        val badgeWidth = badgePaint.measureText(badgeText) + 44f
        val pillRect = RectF(startX, cursorY, startX + badgeWidth, cursorY + badgeHeight)
        fillPaint.color = accentContainerColor
        canvas.drawRoundRect(pillRect, 16f, 16f, fillPaint)
        val badgeBaseline = pillRect.centerY() - ((badgePaint.descent() + badgePaint.ascent()) / 2f)
        canvas.drawText(badgeText, startX + 22f, badgeBaseline, badgePaint)
        cursorY += badgeHeight + 20f

        // Deck Title
        if (deckLayout != null) {
            canvas.save()
            canvas.translate(startX, cursorY)
            deckLayout.draw(canvas)
            canvas.restore()
            cursorY += deckLayout.height + 24f
        }

        // Divider line
        val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = borderColor
            strokeWidth = 2f
        }
        canvas.drawLine(startX, cursorY - 10f, startX + contentWidth, cursorY - 10f, dividerPaint)

        // Question Stem
        canvas.save()
        canvas.translate(startX, cursorY)
        questionLayout.draw(canvas)
        canvas.restore()
        cursorY += questionLayout.height + 36f

        // Options A, B, C, D
        optionLayouts.forEachIndexed { idx, optLayout ->
            val rowHeight = maxOf(optLayout.height.toFloat(), badgeSize) + (optionBoxVerticalPad * 2)
            val optRect = RectF(startX, cursorY, startX + contentWidth, cursorY + rowHeight)
            fillPaint.color = optionBgColor
            canvas.drawRoundRect(optRect, 22f, 22f, fillPaint)
            canvas.drawRoundRect(optRect, 22f, 22f, strokePaint)

            // Letter badge (A / B / C / D)
            val badgeLeft = startX + optionBoxHorizontalPad
            val badgeTop = cursorY + (rowHeight - badgeSize) / 2f
            val letterRect = RectF(badgeLeft, badgeTop, badgeLeft + badgeSize, badgeTop + badgeSize)
            fillPaint.color = accentContainerColor
            canvas.drawRoundRect(letterRect, 14f, 14f, fillPaint)
            val letterBaseline = letterRect.centerY() - ((optionBadgePaint.descent() + optionBadgePaint.ascent()) / 2f)
            canvas.drawText("${'A' + idx}", letterRect.centerX(), letterBaseline, optionBadgePaint)

            // Option text
            val textLeft = badgeLeft + badgeSize + badgeGap
            val textTop = cursorY + (rowHeight - optLayout.height) / 2f
            canvas.save()
            canvas.translate(textLeft, textTop)
            optLayout.draw(canvas)
            canvas.restore()

            cursorY += rowHeight + optionSpacing
        }

        // Subtle Footer
        cursorY += 16f
        canvas.drawLine(startX, cursorY, startX + contentWidth, cursorY, dividerPaint)
        cursorY += 34f
        val accentDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accentColor }
        canvas.drawCircle(startX + 8f, cursorY - 8f, 7f, accentDotPaint)
        canvas.drawText("FolioPrep · UPSC CSE Study Studio", startX + 26f, cursorY, footerPaint)

        val dir = File(context.cacheDir, "shared_questions").apply { mkdirs() }
        // Clean previous temporary screenshots so cache stays light
        dir.listFiles()?.forEach { oldFile ->
            if (oldFile.isFile && System.currentTimeMillis() - oldFile.lastModified() > 60_000L) {
                oldFile.delete()
            }
        }
        val outFile = File(dir, "upsc_q${questionNumber}_${System.currentTimeMillis()}.png")
        FileOutputStream(outFile).use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        }
        bitmap.recycle()
        return outFile
    }

    private fun renderExplanationScreenshotPng(
        context: Context,
        deckTitle: String,
        questionNumber: Int,
        question: McqQuestionEntity,
        isDarkTheme: Boolean
    ): File {
        val width = 1080
        val outerMargin = 48f
        val cardPadding = 44f
        val contentWidth = (width - (outerMargin * 2) - (cardPadding * 2)).toInt()

        val bgColor = if (isDarkTheme) Color.parseColor("#161513") else Color.parseColor("#FAF7F2")
        val cardColor = if (isDarkTheme) Color.parseColor("#211F1C") else Color.parseColor("#FFFDF9")
        val answerBgColor = if (isDarkTheme) Color.parseColor("#1E3E2E") else Color.parseColor("#DCEAE2")
        val explanationBgColor = if (isDarkTheme) Color.parseColor("#2B2824") else Color.parseColor("#F4EFE6")
        val borderColor = if (isDarkTheme) Color.parseColor("#3E3A33") else Color.parseColor("#DFD8CB")
        val accentColor = if (isDarkTheme) Color.parseColor("#8CC7A8") else Color.parseColor("#2A5942")
        val accentOnContainerColor = if (isDarkTheme) Color.parseColor("#DCEAE2") else Color.parseColor("#153425")
        val titleTextColor = if (isDarkTheme) Color.parseColor("#F2EDE4") else Color.parseColor("#1C1B18")
        val mutedTextColor = if (isDarkTheme) Color.parseColor("#C5BEB2") else Color.parseColor("#5B574F")

        val badgePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentOnContainerColor
            textSize = 27f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val deckTitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = mutedTextColor
            textSize = 30f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        val answerLabelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentOnContainerColor
            textSize = 28f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val answerTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentOnContainerColor
            textSize = 34f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val sectionLabelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor
            textSize = 30f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val explanationPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = titleTextColor
            textSize = 34f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        val footerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = mutedTextColor
            textSize = 26f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }

        val opts = question.optionsList()
        val correctLetter = 'A' + question.correctIndex.coerceIn(0, 3)
        val correctOptionText = opts.getOrElse(question.correctIndex) { "" }.trim()
        val answerLine = "$correctLetter. $correctOptionText"

        val innerBoxPad = 28f
        val innerBoxTextWidth = (contentWidth - (innerBoxPad * 2)).toInt()

        val deckLayout = if (deckTitle.isNotBlank()) {
            buildLayout(deckTitle.trim(), deckTitlePaint, contentWidth)
        } else null
        val answerLayout = buildLayout(answerLine, answerTextPaint, innerBoxTextWidth)
        val explanationLayout = buildLayout(
            question.explanation.trim().ifEmpty { "No explanation provided." },
            explanationPaint,
            innerBoxTextWidth
        )

        val badgeHeight = 56f
        val answerBoxHeight = innerBoxPad + 36f + 10f + answerLayout.height + innerBoxPad
        val explanationBoxHeight = innerBoxPad + 42f + 16f + explanationLayout.height + innerBoxPad

        var calculatedCardHeight = cardPadding + badgeHeight + 20f
        if (deckLayout != null) {
            calculatedCardHeight += deckLayout.height + 24f
        }
        calculatedCardHeight += 14f + answerBoxHeight + 24f + explanationBoxHeight + 28f + 36f + cardPadding

        val totalHeight = (calculatedCardHeight + (outerMargin * 2)).toInt().coerceAtLeast(620)
        val bitmap = Bitmap.createBitmap(width, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        canvas.drawColor(bgColor)

        val cardRect = RectF(
            outerMargin,
            outerMargin,
            width - outerMargin,
            totalHeight - outerMargin
        )
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = cardColor
        }
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = borderColor
            strokeWidth = 2.5f
        }
        canvas.drawRoundRect(cardRect, 36f, 36f, fillPaint)
        canvas.drawRoundRect(cardRect, 36f, 36f, strokePaint)

        val startX = outerMargin + cardPadding
        var cursorY = outerMargin + cardPadding

        // Header Pill ("UPSC CSE · Q1 · HARD · EXPLANATION")
        val diffLabel = QuestionDifficulty.fromRaw(question.difficulty).label.uppercase()
        val badgeText = "UPSC CSE  ·  Q$questionNumber  ·  $diffLabel  ·  EXPLANATION"
        val badgeWidth = (badgePaint.measureText(badgeText) + 44f).coerceAtMost(contentWidth.toFloat())
        val pillRect = RectF(startX, cursorY, startX + badgeWidth, cursorY + badgeHeight)
        fillPaint.color = answerBgColor
        canvas.drawRoundRect(pillRect, 16f, 16f, fillPaint)
        val badgeBaseline = pillRect.centerY() - ((badgePaint.descent() + badgePaint.ascent()) / 2f)
        canvas.drawText(badgeText, startX + 22f, badgeBaseline, badgePaint)
        cursorY += badgeHeight + 20f

        if (deckLayout != null) {
            canvas.save()
            canvas.translate(startX, cursorY)
            deckLayout.draw(canvas)
            canvas.restore()
            cursorY += deckLayout.height + 24f
        }

        val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = borderColor
            strokeWidth = 2f
        }
        canvas.drawLine(startX, cursorY - 10f, startX + contentWidth, cursorY - 10f, dividerPaint)
        cursorY += 14f

        // Correct Answer Box
        val answerRect = RectF(startX, cursorY, startX + contentWidth, cursorY + answerBoxHeight)
        fillPaint.color = answerBgColor
        canvas.drawRoundRect(answerRect, 24f, 24f, fillPaint)
        val answerLabelY = cursorY + innerBoxPad + 28f
        canvas.drawText("CORRECT ANSWER", startX + innerBoxPad, answerLabelY, answerLabelPaint)
        canvas.save()
        canvas.translate(startX + innerBoxPad, cursorY + innerBoxPad + 46f)
        answerLayout.draw(canvas)
        canvas.restore()
        cursorY += answerBoxHeight + 24f

        // Explanation Box
        val expRect = RectF(startX, cursorY, startX + contentWidth, cursorY + explanationBoxHeight)
        fillPaint.color = explanationBgColor
        canvas.drawRoundRect(expRect, 24f, 24f, fillPaint)
        canvas.drawRoundRect(expRect, 24f, 24f, strokePaint)
        val expHeaderY = cursorY + innerBoxPad + 30f
        canvas.drawText("UPSC Question Setter Explanation", startX + innerBoxPad, expHeaderY, sectionLabelPaint)
        canvas.save()
        canvas.translate(startX + innerBoxPad, cursorY + innerBoxPad + 58f)
        explanationLayout.draw(canvas)
        canvas.restore()
        cursorY += explanationBoxHeight + 28f

        // Subtle Footer
        canvas.drawLine(startX, cursorY, startX + contentWidth, cursorY, dividerPaint)
        cursorY += 34f
        val accentDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accentColor }
        canvas.drawCircle(startX + 8f, cursorY - 8f, 7f, accentDotPaint)
        canvas.drawText("FolioPrep · UPSC CSE Study Studio", startX + 26f, cursorY, footerPaint)

        val dir = File(context.cacheDir, "shared_questions").apply { mkdirs() }
        dir.listFiles()?.forEach { oldFile ->
            if (oldFile.isFile && System.currentTimeMillis() - oldFile.lastModified() > 60_000L) {
                oldFile.delete()
            }
        }
        val outFile = File(dir, "upsc_q${questionNumber}_explanation_${System.currentTimeMillis()}.png")
        FileOutputStream(outFile).use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        }
        bitmap.recycle()
        return outFile
    }

    private fun buildLayout(
        text: String,
        paint: TextPaint,
        widthPx: Int
    ): StaticLayout {
        return StaticLayout.Builder
            .obtain(text, 0, text.length, paint, widthPx.coerceAtLeast(100))
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0f, 1.25f)
            .setIncludePad(false)
            .build()
    }
}
