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
        val chooser = Intent.createChooser(sendIntent, "Choose your AI app").apply {
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
}
