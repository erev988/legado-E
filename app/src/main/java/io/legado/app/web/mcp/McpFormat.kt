package io.legado.app.web.mcp

import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import io.legado.app.data.entities.BookSource
import io.legado.app.data.entities.BookSourcePart

object McpFormat {

    const val TRUNCATE_LIMIT = 100_000

    private val prettyGson = GsonBuilder()
        .setPrettyPrinting()
        .disableHtmlEscaping()
        .create()

    fun detectFormat(source: String): String {
        val first = source.firstOrNull { !it.isWhitespace() && it != '\uFEFF' }
        return if (first == '{' || first == '[') "json" else "js"
    }

    fun summarizeSources(sources: List<BookSource>, search: String?): List<Map<String, Any>> {
        val summaries = sources.map { source ->
            mapOf(
                "bookSourceName" to source.bookSourceName,
                "bookSourceUrl" to source.bookSourceUrl,
                "bookSourceGroup" to source.bookSourceGroup.orEmpty(),
                "enabled" to source.enabled,
                "bookSourceType" to source.bookSourceType,
            )
        }
        if (search.isNullOrEmpty()) return summaries
        return summaries.filter { summary ->
            (summary["bookSourceName"] as String).contains(search, ignoreCase = true) ||
                (summary["bookSourceUrl"] as String).contains(search, ignoreCase = true)
        }
    }

    fun toPrettyJson(value: Any): String = prettyGson.toJson(value)

    fun prettyJson(json: String): String = prettyGson.toJson(JsonParser.parseString(json))

    fun truncate(text: String, limit: Int = TRUNCATE_LIMIT): String {
        if (text.length <= limit) return text
        return text.take(limit) + "\n…[已截断,原文 ${text.length} 字符]"
    }

    fun renderCheckSummary(
        requestedSources: List<BookSourcePart>,
        messages: Map<String, String>,
        isChecking: Boolean,
    ): String {
        val failed = mutableListOf<String>()
        val passed = mutableListOf<String>()
        val pending = mutableListOf<String>()
        requestedSources.forEach { requested ->
            val message = messages[requested.bookSourceUrl].orEmpty()
            val label = "${requested.bookSourceName}(${requested.bookSourceUrl})"
            when {
                message.contains("失败") || message.contains("错误") ->
                    failed += "[失败] $label:${message}"
                message.contains("校验成功") ->
                    passed += "[通过] $label:${message}"
                else ->
                    pending += "[未完成] $label${message.takeIf { it.isNotEmpty() }?.let { ":$it" }.orEmpty()}"
            }
        }
        return buildString {
            appendLine("失败 ${failed.size}/${requestedSources.size}:")
            appendLinesOrEmpty(failed)
            appendLine()
            appendLine("通过 ${passed.size}/${requestedSources.size}:")
            appendLinesOrEmpty(passed)
            appendLine()
            appendLine("未完成 ${pending.size}/${requestedSources.size}:")
            appendLinesOrEmpty(pending)
            if (isChecking) appendLine("\n（校验仍在进行中，以上为当前快照）")
        }.trimEnd()
    }

    private fun StringBuilder.appendLinesOrEmpty(lines: List<String>) {
        if (lines.isEmpty()) appendLine("(无)") else lines.forEach { appendLine(it) }
    }
}
