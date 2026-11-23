package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.EditCalendar
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.ExamYearConfig

@Composable
fun ExamYearHeaderBar(
    examYearConfig: ExamYearConfig,
    onUpdatePrelimsYear: (Int) -> Unit,
    onUpdateMainsYear: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var editingTarget by remember { mutableStateOf<ExamYearTarget?>(null) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AccountBalance,
                            contentDescription = "UPSC CSE Scope",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "UPSC CSE Pattern",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Saved · Applied automatically to new question sets",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                YearSelectorPill(
                    label = "PRELIMS LATEST",
                    year = examYearConfig.latestPrelimsYear,
                    subtitle = "Up to ${examYearConfig.latestPrelimsYear} & PYQs",
                    testTag = "prelims_year_selector",
                    onClick = { editingTarget = ExamYearTarget.PRELIMS },
                    modifier = Modifier.weight(1f)
                )
                YearSelectorPill(
                    label = "MAINS LATEST",
                    year = examYearConfig.latestMainsYear,
                    subtitle = "Up to ${examYearConfig.latestMainsYear} & PYQs",
                    testTag = "mains_year_selector",
                    onClick = { editingTarget = ExamYearTarget.MAINS },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    editingTarget?.let { target ->
        val initialYear = when (target) {
            ExamYearTarget.PRELIMS -> examYearConfig.latestPrelimsYear
            ExamYearTarget.MAINS -> examYearConfig.latestMainsYear
        }
        ExamYearPickerDialog(
            target = target,
            currentYear = initialYear,
            onDismiss = { editingTarget = null },
            onConfirm = { newYear ->
                when (target) {
                    ExamYearTarget.PRELIMS -> onUpdatePrelimsYear(newYear)
                    ExamYearTarget.MAINS -> onUpdateMainsYear(newYear)
                }
                editingTarget = null
            }
        )
    }
}

private enum class ExamYearTarget(val title: String, val helper: String) {
    PRELIMS(
        title = "Latest UPSC Prelims Year",
        helper = "Prompts will calibrate MCQs to the UPSC CSE Prelims pattern of this year and all previous years."
    ),
    MAINS(
        title = "Latest UPSC Mains Year",
        helper = "Prompts will calibrate 10 & 15-mark subjective questions to the UPSC CSE Mains pattern of this year and all previous years."
    )
}

@Composable
private fun YearSelectorPill(
    label: String,
    year: Int,
    subtitle: String,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = year.toString(),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Outlined.EditCalendar,
                contentDescription = "Edit $label year",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExamYearPickerDialog(
    target: ExamYearTarget,
    currentYear: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var selectedYear by remember(currentYear) { mutableIntStateOf(currentYear) }
    var manualInput by remember(currentYear) { mutableStateOf(currentYear.toString()) }
    val quickYears = remember { listOf(2023, 2024, 2025, 2026, 2027, 2028) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Outlined.CalendarMonth,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = target.title,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = target.helper,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    FilledTonalIconButton(
                        onClick = {
                            val next = (selectedYear - 1).coerceAtLeast(ExamYearConfig.MIN_EXAM_YEAR)
                            selectedYear = next
                            manualInput = next.toString()
                        },
                        modifier = Modifier.testTag("decrement_exam_year")
                    ) {
                        Icon(Icons.Outlined.Remove, contentDescription = "Previous year")
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    OutlinedTextField(
                        value = manualInput,
                        onValueChange = { raw ->
                            val digits = raw.filter { it.isDigit() }.take(4)
                            manualInput = digits
                            digits.toIntOrNull()?.let { parsed ->
                                if (parsed in ExamYearConfig.MIN_EXAM_YEAR..ExamYearConfig.MAX_EXAM_YEAR) {
                                    selectedYear = parsed
                                }
                            }
                        },
                        label = { Text("Year") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .width(116.dp)
                            .testTag("exam_year_text_input")
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    FilledTonalIconButton(
                        onClick = {
                            val next = (selectedYear + 1).coerceAtMost(ExamYearConfig.MAX_EXAM_YEAR)
                            selectedYear = next
                            manualInput = next.toString()
                        },
                        modifier = Modifier.testTag("increment_exam_year")
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = "Next year")
                    }
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    quickYears.forEach { yr ->
                        FilterChip(
                            selected = selectedYear == yr,
                            onClick = {
                                selectedYear = yr
                                manualInput = yr.toString()
                            },
                            label = { Text(yr.toString()) },
                            modifier = Modifier.testTag("quick_year_$yr")
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalYear = manualInput.toIntOrNull()
                        ?.coerceIn(ExamYearConfig.MIN_EXAM_YEAR, ExamYearConfig.MAX_EXAM_YEAR)
                        ?: selectedYear
                    onConfirm(finalYear)
                },
                modifier = Modifier.testTag("confirm_exam_year_button")
            ) {
                Text("Save Year")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
