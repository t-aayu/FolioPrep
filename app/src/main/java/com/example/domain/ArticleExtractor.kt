package com.example.domain

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.util.concurrent.TimeUnit

data class ExtractedArticle(
    val title: String,
    val cleanText: String,
    val rawHtmlLength: Int,
    val cleanTextLength: Int
) {
    val reductionPercent: Int
        get() {
            if (rawHtmlLength <= 0) return 0
            val saved = ((rawHtmlLength - cleanTextLength).coerceAtLeast(0) * 100.0 / rawHtmlLength).toInt()
            return saved.coerceIn(0, 99)
        }
}

class ArticleExtractor {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    suspend fun extractFromUrl(rawUrl: String): Result<ExtractedArticle> = withContext(Dispatchers.IO) {
        runCatching {
            val normalizedUrl = normalizeUrl(rawUrl)
            val request = Request.Builder()
                .url(normalizedUrl)
                .header(
                    "User-Agent",
                    "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"
                )
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.9")
                .build()

            val html = client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IllegalStateException("HTTP ${response.code}: Unable to fetch article.")
                }
                response.body?.string()
                    ?: throw IllegalStateException("The page returned an empty response.")
            }

            val doc = Jsoup.parse(html, normalizedUrl)
            val pageTitle = doc.selectFirst("meta[property=og:title]")?.attr("content")?.trim()
                ?.takeIf { it.isNotBlank() }
                ?: doc.selectFirst("h1")?.text()?.trim()?.takeIf { it.isNotBlank() }
                ?: doc.title().trim()

            // Remove noisy non-article elements locally to save 80-90% tokens
            doc.select(
                "script, style, noscript, iframe, svg, canvas, nav, header, footer, aside, form, " +
                    "button, input, select, textarea, [role=navigation], [role=banner], [role=complementary], " +
                    "[aria-hidden=true], .ad, .ads, .advertisement, .promo, .newsletter, .subscribe, " +
                    ".cookie, .cookies, .share, .social, .related, .comments, #comments, .sidebar, .menu"
            ).remove()

            val rootCandidate: Element = findBestArticleContainer(doc.body())
                ?: doc.body()
                ?: throw IllegalStateException("No readable article body found on this page.")

            val blocks = rootCandidate
                .select("h1, h2, h3, h4, p, li, blockquote")
                .map { it.text().replace(Regex("\\s+"), " ").trim() }
                .filter { line ->
                    line.length >= 25 &&
                        !line.startsWith("Read more", ignoreCase = true) &&
                        !line.startsWith("Also read", ignoreCase = true) &&
                        !line.startsWith("Subscribe", ignoreCase = true)
                }
                .distinct()

            val bodyText = if (blocks.isNotEmpty()) {
                blocks.joinToString("\n\n")
            } else {
                rootCandidate.text().replace(Regex("\\s+"), " ").trim()
            }

            if (bodyText.length < 60) {
                throw IllegalStateException(
                    "Extracted text is too short (the page may be paywalled or require JavaScript). Try copying the text directly."
                )
            }

            val finalText = if (pageTitle.isNotBlank() && !bodyText.startsWith(pageTitle)) {
                "$pageTitle\n\n$bodyText"
            } else {
                bodyText
            }

            ExtractedArticle(
                title = pageTitle.ifBlank { "Web Article" },
                cleanText = finalText,
                rawHtmlLength = html.length,
                cleanTextLength = finalText.length
            )
        }
    }

    private fun normalizeUrl(input: String): String {
        val trimmed = input.trim()
        require(trimmed.isNotEmpty()) { "Please enter a valid article URL." }
        return if (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true)
        ) {
            trimmed
        } else {
            "https://$trimmed"
        }
    }

    private fun findBestArticleContainer(body: Element?): Element? {
        if (body == null) return null
        val semanticCandidates = body.select("article, main, [role=main], .article-body, .story-content, .post-content, .entry-content")
        val bestSemantic = semanticCandidates.maxByOrNull { candidate ->
            candidate.select("p").sumOf { it.text().length }
        }
        if (bestSemantic != null && bestSemantic.select("p").sumOf { it.text().length } > 250) {
            return bestSemantic
        }

        val divCandidates = body.select("section, div")
        return divCandidates.maxByOrNull { container ->
            val directParagraphChars = container.children()
                .filter { it.tagName() in setOf("p", "blockquote", "h2", "h3") }
                .sumOf { it.text().length }
            directParagraphChars
        }?.takeIf { it.text().length > 200 } ?: body
    }
}
