package com.example.domain

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build

data class InstalledAiApp(
    val name: String,
    val packageName: String,
    val shortBadge: String
)

data class IncomingSharedText(
    val text: String,
    val title: String = "",
    val isUrlOnly: Boolean = false
)

object ExternalAiLauncher {

    private val knownAiApps = listOf(
        InstalledAiApp(name = "ChatGPT", packageName = "com.openai.chatgpt", shortBadge = "GPT"),
        InstalledAiApp(name = "Gemini", packageName = "com.google.android.apps.bard", shortBadge = "GEM"),
        InstalledAiApp(name = "Claude", packageName = "com.anthropic.claude", shortBadge = "CLD"),
        InstalledAiApp(name = "Perplexity", packageName = "ai.perplexity.app.android", shortBadge = "PPX")
    )

    fun detectInstalledAiApps(context: Context): List<InstalledAiApp> {
        val pm = context.packageManager
        return knownAiApps.filter { app ->
            isPackageInstalled(pm, app.packageName)
        }
    }

    private fun isPackageInstalled(pm: PackageManager, packageName: String): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(packageName, 0)
            }
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun sharePromptToSpecificApp(context: Context, packageName: String, prompt: String): Boolean {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, prompt)
            `package` = packageName
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            sharePromptViaChooser(context, prompt)
            false
        }
    }

    fun sharePromptViaChooser(context: Context, prompt: String) {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, prompt)
        }
        val chooser = Intent.createChooser(sendIntent, "Open with").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    fun copyPromptToClipboard(context: Context, prompt: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("FolioPrep UPSC Prompt", prompt))
    }

    fun readClipboardText(context: Context): String? {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = clipboard.primaryClip ?: return null
        if (clip.itemCount == 0) return null
        return clip.getItemAt(0).coerceToText(context)?.toString()
    }

    fun extractIncomingSharedText(context: Context, intent: Intent?): IncomingSharedText? {
        if (intent == null) return null
        return when (intent.action) {
            Intent.ACTION_PROCESS_TEXT -> {
                val selected = intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)
                    ?.toString()
                    ?.trim()
                    .orEmpty()
                if (selected.isEmpty()) null
                else IncomingSharedText(text = selected, isUrlOnly = false)
            }

            Intent.ACTION_SEND -> {
                val type = intent.type
                if (type != null && !type.startsWith("text/")) return null
                val rawText = intent.getStringExtra(Intent.EXTRA_TEXT)?.trim()
                    ?: intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()?.trim()
                    ?: intent.clipData?.takeIf { it.itemCount > 0 }
                        ?.getItemAt(0)
                        ?.coerceToText(context)
                        ?.toString()
                        ?.trim()
                    ?: return null
                if (rawText.isEmpty()) return null
                val subject = intent.getStringExtra(Intent.EXTRA_SUBJECT)?.trim()
                    ?: intent.getStringExtra(Intent.EXTRA_TITLE)?.trim()
                    ?: ""
                val singleLineUrl = !rawText.contains('\n') &&
                    !rawText.contains(' ') &&
                    (rawText.startsWith("http://", ignoreCase = true) ||
                        rawText.startsWith("https://", ignoreCase = true))
                IncomingSharedText(
                    text = rawText,
                    title = subject,
                    isUrlOnly = singleLineUrl
                )
            }

            else -> null
        }
    }
}
