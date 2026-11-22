package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.data.local.QuizAttemptEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DeckDetailScreen(
    bundle: DeckDetailBundle?,
    onBack: () -> Unit,
    onStartPractice: (Long) -> Unit,
    onStartTimedExam: (deckId: Long, durationMinutes: Int, negativeFactor: Double) -> Unit,
    onStartSubjectiveFlashcards: (Long) -> Unit
) {
    var showTimedConfigDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Study Dossier",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("deck_detail_back")
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back to Library")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        if (bundle == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("Loading offline study deck...", style = MaterialTheme.typography.bodyLarge)
            }
            return@Scaffold
        }

        val deck = bundle.deck
        val masteredCount = bundle.subjectiveCards.count { it.isMastered }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 720.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item(key = "dossier_header") {
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                text = "${deck.examScope} · Prelims ${deck.prelimsYear} · Mains ${deck.mainsYear}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = deck.title,
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            val tags = deck.tagList()
                            if (tags.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    tags.forEach { tag ->
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant
                                        ) {
                                            Text(
                                                text = tag,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item(key = "study_modes_title") {
                    Text(
                        text = "Offline Study Modes",
                        style = MaterialTheme.typography.titleLarge
                    )
                }

                if (bundle.mcqs.isNotEmpty()) {
                    item(key = "practice_mode") {
                        StudyModeActionCard(
                            icon = Icons.Outlined.Quiz,
                            title = "Prelims Practice Mode",
                            subtitle = "${bundle.mcqs.size} MCQs · Instant answer reveal, detailed UPSC explanations & score tracking",
                            badge = "UNTIMED",
                            accentContainer = MaterialTheme.colorScheme.primaryContainer,
                            accentContent = MaterialTheme.colorScheme.onPrimaryContainer,
                            testTag = "launch_practice_mode",
                            onClick = { onStartPractice(deck.id) }
                        )
                    }

                    item(key = "timed_exam_mode") {
                        StudyModeActionCard(
                            icon = Icons.Outlined.Timer,
                            title = "Timed Prelims Exam Mode",
                            subtitle = "${bundle.mcqs.size} MCQs · Live countdown, question palette & optional UPSC -1/3 negative marking",
                            badge = "SIMULATION",
                            accentContainer = MaterialTheme.colorScheme.tertiaryContainer,
                            accentContent = MaterialTheme.colorScheme.onTertiaryContainer,
                            testTag = "launch_timed_exam_mode",
                            onClick = { showTimedConfigDialog = true }
                        )
                    }
                }

                if (bundle.subjectiveCards.isNotEmpty()) {
                    item(key = "mains_flashcards_mode") {
                        StudyModeActionCard(
                            icon = Icons.Outlined.Style,
                            title = "Mains 10/15-Mark Flashcards",
                            subtitle = "${bundle.subjectiveCards.size} cards ($masteredCount mastered) · 3D flip showing ideal Intro, Key Points & Conclusion",
                            badge = "3D FLIP",
                            accentContainer = MaterialTheme.colorScheme.secondaryContainer,
                            accentContent = MaterialTheme.colorScheme.onSecondaryContainer,
                            testTag = "launch_mains_flashcards",
                            onClick = { onStartSubjectiveFlashcards(deck.id) }
                        )
                    }
                }

                if (bundle.attempts.isNotEmpty()) {
                    item(key = "history_title") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 6.dp)
                        ) {
                            Icon(
                                Icons.Outlined.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Score History (${bundle.attempts.size})",
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                    }

                    items(items = bundle.attempts, key = { it.id }) { attempt ->
                        AttemptHistoryRow(attempt = attempt)
                    }
                }

                item(key = "detail_spacer") {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }

        if (showTimedConfigDialog) {
            TimedExamConfigDialog(
                questionCount = bundle.mcqs.size,
                onDismiss = { showTimedConfigDialog = false },
                onStart = { minutes, negFactor ->
                    showTimedConfigDialog = false
                    onStartTimedExam(deck.id, minutes, negFactor)
                }
            )
        }
    }
}

@Composable
private fun StudyModeActionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    badge: String,
    accentContainer: androidx.compose.ui.graphics.Color,
    accentContent: androidx.compose.ui.graphics.Color,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = accentContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentContent,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = badge,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Outlined.PlayArrow,
                contentDescription = "Start $title",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun AttemptHistoryRow(attempt: QuizAttemptEntity) {
    val accuracy = if (attempt.totalQuestions > 0) {
        (attempt.correctCount * 100) / attempt.totalQuestions
    } else 0
    val formattedDate = remember(attempt.completedAt) {
        SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(attempt.completedAt))
    }
    val modeLabel = if (attempt.mode == "TIMED_EXAM") "Timed Exam" else "Practice"

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$modeLabel · $formattedDate",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${attempt.correctCount} right · ${attempt.wrongCount} wrong · ${attempt.skippedCount} skipped",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = String.format(Locale.US, "%.2f / %.0f", attempt.netScore, attempt.maxScore),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "$accuracy% accuracy",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TimedExamConfigDialog(
    questionCount: Int,
    onDismiss: () -> Unit,
    onStart: (Int, Double) -> Unit
) {
    var minutes by remember(questionCount) {
        mutableIntStateOf((questionCount * 1.2).toInt().coerceIn(2, 120))
    }
    var negativeFactor by remember { mutableDoubleStateOf(0.3333) }

    val penaltyOptions = listOf(
        0.0 to "None (0)",
        0.25 to "-1/4 (-0.25)",
        0.3333 to "UPSC -1/3 (-0.66/2)",
        0.5 to "-1/2 (-0.50)",
        1.0 to "-1.0"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Timed Prelims Setup") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Each correct answer awards +2.0 marks (UPSC Prelims GS-I standard).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "Duration (Minutes)",
                    style = MaterialTheme.typography.labelLarge
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilledTonalIconButton(
                        onClick = { minutes = (minutes - 1).coerceAtLeast(1) }
                    ) {
                        Icon(Icons.Outlined.Remove, contentDescription = "Decrease minutes")
                    }
                    Text(
                        text = "$minutes min",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(horizontal = 18.dp)
                    )
                    FilledTonalIconButton(
                        onClick = { minutes = (minutes + 1).coerceAtMost(180) }
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = "Increase minutes")
                    }
                }

                Text(
                    text = "Negative Marking",
                    style = MaterialTheme.typography.labelLarge
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    penaltyOptions.forEach { (factor, label) ->
                        FilterChip(
                            selected = kotlin.math.abs(negativeFactor - factor) < 0.01,
                            onClick = { negativeFactor = factor },
                            label = { Text(label) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onStart(minutes, negativeFactor) },
                modifier = Modifier.testTag("start_timed_exam_confirm")
            ) {
                Text("Begin Exam")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
