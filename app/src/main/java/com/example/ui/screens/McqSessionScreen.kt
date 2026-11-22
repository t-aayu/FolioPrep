package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.DeckDetailBundle
import com.example.data.local.McqQuestionEntity
import kotlinx.coroutines.delay
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun McqSessionScreen(
    bundle: DeckDetailBundle?,
    isTimedExam: Boolean,
    durationMinutes: Int,
    negativeFactor: Double,
    onBack: () -> Unit,
    onCompleteAttempt: (
        deckId: Long,
        mode: String,
        totalQuestions: Int,
        correctCount: Int,
        wrongCount: Int,
        skippedCount: Int,
        negativeFactor: Double,
        netScore: Double,
        maxScore: Double,
        durationSeconds: Int
    ) -> Unit
) {
    val mcqs = bundle?.mcqs.orEmpty()
    val deckId = bundle?.deck?.id ?: -1L
    val marksPerCorrect = 2.0

    var currentIndex by remember(deckId) { mutableIntStateOf(0) }
    val selectedAnswers = remember(deckId) { mutableStateMapOf<Int, Int>() }
    var isFinished by remember(deckId) { mutableStateOf(false) }
    var elapsedSeconds by remember(deckId) { mutableIntStateOf(0) }
    var remainingSeconds by remember(deckId, durationMinutes) {
        mutableIntStateOf((durationMinutes * 60).coerceAtLeast(60))
    }

    fun finalizeSession() {
        if (isFinished || mcqs.isEmpty() || deckId <= 0L) return
        isFinished = true
        var correct = 0
        var wrong = 0
        var skipped = 0
        mcqs.forEachIndexed { index, q ->
            val picked = selectedAnswers[index]
            when {
                picked == null -> skipped++
                picked == q.correctIndex -> correct++
                else -> wrong++
            }
        }
        val penaltyPerWrong = if (isTimedExam) marksPerCorrect * negativeFactor else 0.0
        val netScore = (correct * marksPerCorrect) - (wrong * penaltyPerWrong)
        val maxScore = mcqs.size * marksPerCorrect
        onCompleteAttempt(
            deckId,
            if (isTimedExam) "TIMED_EXAM" else "PRACTICE",
            mcqs.size,
            correct,
            wrong,
            skipped,
            if (isTimedExam) negativeFactor else 0.0,
            netScore,
            maxScore,
            elapsedSeconds
        )
    }

    LaunchedEffect(deckId, isTimedExam, isFinished, mcqs.size) {
        if (mcqs.isEmpty() || isFinished) return@LaunchedEffect
        while (!isFinished) {
            delay(1000L)
            elapsedSeconds++
            if (isTimedExam) {
                remainingSeconds--
                if (remainingSeconds <= 0) {
                    finalizeSession()
                    break
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (isTimedExam) "Timed Prelims Exam" else "Prelims Practice",
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (bundle != null) {
                            Text(
                                text = bundle.deck.title,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("mcq_session_back")
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isTimedExam && !isFinished) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (remainingSeconds <= 60) {
                                MaterialTheme.colorScheme.errorContainer
                            } else {
                                MaterialTheme.colorScheme.primaryContainer
                            },
                            modifier = Modifier.padding(end = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Outlined.Timer,
                                    contentDescription = "Remaining time",
                                    tint = if (remainingSeconds <= 60) {
                                        MaterialTheme.colorScheme.error
                                    } else {
                                        MaterialTheme.colorScheme.primary
                                    },
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = formatSeconds(remainingSeconds),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (remainingSeconds <= 60) {
                                        MaterialTheme.colorScheme.onErrorContainer
                                    } else {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        if (mcqs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("No MCQs found in this deck.", style = MaterialTheme.typography.bodyLarge)
            }
            return@Scaffold
        }

        if (isFinished) {
            ExamSummaryReviewView(
                mcqs = mcqs,
                selectedAnswers = selectedAnswers,
                isTimedExam = isTimedExam,
                negativeFactor = negativeFactor,
                elapsedSeconds = elapsedSeconds,
                onRestart = {
                    selectedAnswers.clear()
                    currentIndex = 0
                    elapsedSeconds = 0
                    remainingSeconds = (durationMinutes * 60).coerceAtLeast(60)
                    isFinished = false
                },
                onDone = onBack,
                modifier = Modifier.padding(innerPadding)
            )
            return@Scaffold
        }

        val currentQuestion = mcqs[currentIndex.coerceIn(0, mcqs.lastIndex)]
        val chosenOption = selectedAnswers[currentIndex]
        val showInstantFeedback = !isTimedExam && chosenOption != null

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 720.dp)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                // Progress Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Question ${currentIndex + 1} of ${mcqs.size}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${selectedAnswers.size} answered" +
                            if (isTimedExam && negativeFactor > 0.0) {
                                " · Penalty -${String.format(Locale.US, "%.2f", marksPerCorrect * negativeFactor)}"
                            } else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { (currentIndex + 1).toFloat() / mcqs.size.toFloat() },
                    modifier = Modifier.fillMaxWidth()
                )

                // Question Palette Strip (ideal for Timed Exam skipping & revisiting)
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    mcqs.indices.forEach { idx ->
                        val answered = selectedAnswers.containsKey(idx)
                        val isCurrent = idx == currentIndex
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = when {
                                isCurrent -> MaterialTheme.colorScheme.primary
                                answered -> MaterialTheme.colorScheme.primaryContainer
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            },
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { currentIndex = idx }
                        ) {
                            Text(
                                text = "${idx + 1}",
                                style = MaterialTheme.typography.labelMedium,
                                color = when {
                                    isCurrent -> MaterialTheme.colorScheme.onPrimary
                                    answered -> MaterialTheme.colorScheme.onPrimaryContainer
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = currentQuestion.question,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(18.dp)
                        )
                    }

                    currentQuestion.optionsList().forEachIndexed { optIndex, optionText ->
                        val isSelected = chosenOption == optIndex
                        val isCorrectOption = optIndex == currentQuestion.correctIndex

                        val containerColor = when {
                            showInstantFeedback && isCorrectOption ->
                                MaterialTheme.colorScheme.primaryContainer
                            showInstantFeedback && isSelected && !isCorrectOption ->
                                MaterialTheme.colorScheme.errorContainer
                            isSelected ->
                                MaterialTheme.colorScheme.secondaryContainer
                            else ->
                                MaterialTheme.colorScheme.surface
                        }

                        val borderColor = when {
                            showInstantFeedback && isCorrectOption ->
                                MaterialTheme.colorScheme.primary
                            showInstantFeedback && isSelected && !isCorrectOption ->
                                MaterialTheme.colorScheme.error
                            isSelected ->
                                MaterialTheme.colorScheme.primary
                            else ->
                                MaterialTheme.colorScheme.outlineVariant
                        }

                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = containerColor,
                            border = BorderStroke(if (isSelected || (showInstantFeedback && isCorrectOption)) 1.5.dp else 1.dp, borderColor),
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .clip(MaterialTheme.shapes.medium)
                                .clickable(enabled = isTimedExam || chosenOption == null) {
                                    if (isTimedExam && chosenOption == optIndex) {
                                        selectedAnswers.remove(currentIndex)
                                    } else {
                                        selectedAnswers[currentIndex] = optIndex
                                    }
                                }
                                .testTag("mcq_option_$optIndex")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    }
                                ) {
                                    Text(
                                        text = "${'A' + optIndex}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.onPrimary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = optionText,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                if (showInstantFeedback && isCorrectOption) {
                                    Icon(
                                        Icons.Outlined.CheckCircle,
                                        contentDescription = "Correct answer",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                } else if (showInstantFeedback && isSelected && !isCorrectOption) {
                                    Icon(
                                        Icons.Outlined.Close,
                                        contentDescription = "Incorrect answer",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }

                    AnimatedVisibility(visible = showInstantFeedback) {
                        Surface(
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Outlined.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "UPSC Question Setter Explanation",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = currentQuestion.explanation,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Bottom Controls
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { currentIndex = (currentIndex - 1).coerceAtLeast(0) },
                        enabled = currentIndex > 0,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Prev")
                    }

                    if (currentIndex < mcqs.lastIndex) {
                        Button(
                            onClick = { currentIndex++ },
                            modifier = Modifier
                                .weight(1.2f)
                                .testTag("mcq_next_button")
                        ) {
                            Text("Next")
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    } else {
                        Button(
                            onClick = { finalizeSession() },
                            modifier = Modifier
                                .weight(1.2f)
                                .testTag("mcq_submit_button")
                        ) {
                            Icon(Icons.Outlined.DoneAll, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isTimedExam) "Submit Exam" else "Finish")
                        }
                    }
                }

                if (isTimedExam && currentIndex < mcqs.lastIndex) {
                    TextButton(
                        onClick = { finalizeSession() },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("Submit Early (${selectedAnswers.size}/${mcqs.size} answered)")
                    }
                }
            }
        }
    }
}

@Composable
private fun ExamSummaryReviewView(
    mcqs: List<McqQuestionEntity>,
    selectedAnswers: Map<Int, Int>,
    isTimedExam: Boolean,
    negativeFactor: Double,
    elapsedSeconds: Int,
    onRestart: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val marksPerCorrect = 2.0
    var correct = 0
    var wrong = 0
    var skipped = 0
    mcqs.forEachIndexed { idx, q ->
        val picked = selectedAnswers[idx]
        when {
            picked == null -> skipped++
            picked == q.correctIndex -> correct++
            else -> wrong++
        }
    }
    val penaltyPerWrong = if (isTimedExam) marksPerCorrect * negativeFactor else 0.0
    val netScore = (correct * marksPerCorrect) - (wrong * penaltyPerWrong)
    val maxScore = mcqs.size * marksPerCorrect
    val accuracy = if (mcqs.isNotEmpty()) (correct * 100) / mcqs.size else 0

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "scorecard") {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = if (isTimedExam) "TIMED EXAM SCORECARD" else "PRACTICE SUMMARY",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = String.format(Locale.US, "%.2f / %.0f Marks", netScore, maxScore),
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Accuracy: $accuracy% · Time taken: ${formatSeconds(elapsedSeconds)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ScoreStatPill("Correct", "$correct", true, Modifier.weight(1f))
                            ScoreStatPill("Wrong", "$wrong", false, Modifier.weight(1f))
                            ScoreStatPill("Skipped", "$skipped", false, Modifier.weight(1f))
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = onRestart,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Retake")
                            }
                            Button(
                                onClick = onDone,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Back to Dossier")
                            }
                        }
                    }
                }
            }

            item(key = "answer_key_title") {
                Text(
                    text = "Detailed Answer Key & Explanations",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            itemsIndexed(items = mcqs, key = { _, q -> q.id }) { idx, q ->
                val picked = selectedAnswers[idx]
                val isCorrect = picked == q.correctIndex
                val statusLabel = when {
                    picked == null -> "Skipped"
                    isCorrect -> "Correct (+2.0)"
                    else -> if (isTimedExam && negativeFactor > 0.0) {
                        "Wrong (-${String.format(Locale.US, "%.2f", penaltyPerWrong)})"
                    } else "Wrong"
                }

                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Q${idx + 1}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = statusLabel,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = when {
                                    picked == null -> MaterialTheme.colorScheme.onSurfaceVariant
                                    isCorrect -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.error
                                }
                            )
                        }
                        Text(
                            text = q.question,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
                        )
                        val opts = q.optionsList()
                        Text(
                            text = "Correct Answer: ${'A' + q.correctIndex}. ${opts.getOrElse(q.correctIndex) { "" }}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (picked != null && picked != q.correctIndex) {
                            Text(
                                text = "Your Answer: ${'A' + picked}. ${opts.getOrElse(picked) { "" }}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        Text(
                            text = q.explanation,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScoreStatPill(
    label: String,
    value: String,
    positive: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = if (positive) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatSeconds(totalSeconds: Int): String {
    val safe = totalSeconds.coerceAtLeast(0)
    val mins = safe / 60
    val secs = safe % 60
    return String.format(Locale.US, "%02d:%02d", mins, secs)
}
