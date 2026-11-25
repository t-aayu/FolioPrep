package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentPasteGo
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.ExternalAiLauncher
import com.example.domain.InstalledAiApp
import com.example.domain.QuickAskQuestionType
import com.example.ui.theme.ChatGptPillBg
import com.example.ui.theme.ChatGptPillBorder
import com.example.ui.theme.ChatGptPillContent
import com.example.ui.theme.GeminiPillBg
import com.example.ui.theme.GeminiPillBorder
import com.example.ui.theme.GeminiPillContent
import com.example.viewmodel.QuickAskUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickAskAiCard(
    state: QuickAskUiState,
    installedAiApps: List<InstalledAiApp>,
    onKeywordsChange: (String) -> Unit,
    onSelectQuestionType: (QuickAskQuestionType) -> Unit,
    onAdjustCount: (Int) -> Unit,
    onPreparePrompt: () -> String?,
    onPasteClick: () -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val chatGptInstalled = remember(installedAiApps) {
        installedAiApps.any { it.packageName == "com.openai.chatgpt" }
    }
    val geminiInstalled = remember(installedAiApps) {
        installedAiApps.any { it.packageName == "com.google.android.apps.bard" }
    }
    val otherInstalledApps = remember(installedAiApps) {
        installedAiApps.filterNot {
            it.packageName == "com.openai.chatgpt" ||
                it.packageName == "com.google.android.apps.bard"
        }
    }
    val maxCount = if (state.questionType == QuickAskQuestionType.MCQ) 20 else 10

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("quick_ask_ai_card"),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AutoAwesome,
                            contentDescription = "Quick Ask AI",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Quick Ask AI",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Keywords → ChatGPT or Gemini → Paste reply",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = state.keywords,
                onValueChange = onKeywordsChange,
                label = { Text("Topic Keywords") },
                placeholder = { Text("e.g. Article 356, Ramsar wetlands, MPC inflation...") },
                minLines = 1,
                maxLines = 3,
                trailingIcon = {
                    if (state.keywords.isNotEmpty()) {
                        IconButton(
                            onClick = { onKeywordsChange("") },
                            modifier = Modifier.testTag("quick_ask_clear_keywords")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "Clear keywords"
                            )
                        }
                    }
                },
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("quick_ask_keywords_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("quick_ask_type_selector")
            ) {
                QuickAskQuestionType.entries.forEachIndexed { index, type ->
                    SegmentedButton(
                        selected = state.questionType == type,
                        onClick = { onSelectQuestionType(type) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = QuickAskQuestionType.entries.size
                        ),
                        icon = {},
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("quick_ask_type_${type.name.lowercase()}")
                    ) {
                        Text(
                            text = type.label,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Number of Questions",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = state.questionType.helper,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FilledTonalIconButton(
                            onClick = { onAdjustCount(-1) },
                            enabled = state.questionCount > 1,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("quick_ask_count_dec")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Remove,
                                contentDescription = "Decrease question count"
                            )
                        }
                        Text(
                            text = state.questionCount.toString(),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .padding(horizontal = 14.dp)
                                .testTag("quick_ask_count_value")
                        )
                        FilledTonalIconButton(
                            onClick = { onAdjustCount(1) },
                            enabled = state.questionCount < maxCount,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("quick_ask_count_inc")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Add,
                                contentDescription = "Increase question count"
                            )
                        }
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
                QuickAskAiPillButton(
                    label = "ChatGPT",
                    containerColor = ChatGptPillBg,
                    borderColor = ChatGptPillBorder,
                    contentColor = ChatGptPillContent,
                    testTag = "quick_ask_chatgpt_button",
                    onClick = {
                        val prompt = onPreparePrompt() ?: return@QuickAskAiPillButton
                        if (chatGptInstalled) {
                            ExternalAiLauncher.sharePromptToSpecificApp(
                                context = context,
                                packageName = "com.openai.chatgpt",
                                prompt = prompt
                            )
                        } else {
                            onShowMessage("Opening share menu for ChatGPT")
                            ExternalAiLauncher.sharePromptViaChooser(context, prompt)
                        }
                    },
                    modifier = Modifier.weight(1f)
                )

                QuickAskAiPillButton(
                    label = "Gemini",
                    containerColor = GeminiPillBg,
                    borderColor = GeminiPillBorder,
                    contentColor = GeminiPillContent,
                    testTag = "quick_ask_gemini_button",
                    onClick = {
                        val prompt = onPreparePrompt() ?: return@QuickAskAiPillButton
                        if (geminiInstalled) {
                            ExternalAiLauncher.sharePromptToSpecificApp(
                                context = context,
                                packageName = "com.google.android.apps.bard",
                                prompt = prompt
                            )
                        } else {
                            onShowMessage("Opening share menu for Gemini")
                            ExternalAiLauncher.sharePromptViaChooser(context, prompt)
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            if (otherInstalledApps.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    otherInstalledApps.forEach { app ->
                        ElevatedFilterChip(
                            selected = false,
                            shape = CircleShape,
                            onClick = {
                                val prompt = onPreparePrompt() ?: return@ElevatedFilterChip
                                ExternalAiLauncher.sharePromptToSpecificApp(
                                    context = context,
                                    packageName = app.packageName,
                                    prompt = prompt
                                )
                            },
                            label = { Text("Send to ${app.name}") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            modifier = Modifier.testTag("quick_ask_ai_${app.shortBadge.lowercase()}")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val prompt = onPreparePrompt() ?: return@OutlinedButton
                        ExternalAiLauncher.sharePromptViaChooser(context, prompt)
                    },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("quick_ask_share_any_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Send,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Other AI", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }

                FilledTonalButton(
                    onClick = onPasteClick,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("quick_ask_paste_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ContentPasteGo,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Paste Reply", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }

            AnimatedVisibility(visible = state.awaitingReply) {
                Row(
                    modifier = Modifier.padding(top = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Share,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "Waiting for reply — copy the generated questions and tap Paste Reply or return here.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickAskAiPillButton(
    label: String,
    containerColor: Color,
    borderColor: Color,
    contentColor: Color,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .minimumInteractiveComponentSize()
            .height(48.dp)
            .testTag(testTag),
        shape = CircleShape,
        color = containerColor,
        contentColor = contentColor,
        border = BorderStroke(1.5.dp, borderColor),
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = contentColor,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                contentDescription = "Send to $label",
                tint = contentColor,
                modifier = Modifier.size(17.dp)
            )
        }
    }
}
