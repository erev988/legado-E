package io.legado.app.web.mcp

import io.legado.app.data.appDb
import io.legado.app.data.entities.BookSource
import io.legado.app.help.ConcurrentRateLimiter.Companion.concurrentRecordMap
import io.legado.app.help.config.SourceConfig
import io.legado.app.help.source.clearExploreKindsCache
import io.legado.app.model.SharedJsScope
import io.legado.app.utils.GSON
import io.legado.app.utils.fromJsonObject

internal object McpSourceStore {

    suspend fun saveDeclarative(text: String): BookSource {
        val source = parseDeclarative(text)
        val old = appDb.bookSourceDao.getBookSource(source.bookSourceUrl)
        if (old != null) {
            // 保留启用、排序和权重；分组为空时保留已有分组
            if (source.bookSourceGroup.isNullOrBlank()) {
                source.bookSourceGroup = old.bookSourceGroup
            }
            source.customOrder = old.customOrder
            source.weight = old.weight
            source.enabled = old.enabled
            if (old.exploreUrl != source.exploreUrl) {
                old.clearExploreKindsCache()
            }
            if (old.jsLib != source.jsLib) {
                SharedJsScope.remove(old.jsLib)
            }
            appDb.bookSourceDao.delete(old)
            SourceConfig.removeSource(old.bookSourceUrl)
        }
        appDb.bookSourceDao.insert(source)
        concurrentRecordMap.remove(source.bookSourceUrl)
        return source
    }

    internal fun parseDeclarative(text: String): BookSource {
        require(text.toByteArray(Charsets.UTF_8).size <= MAX_SOURCE_BYTES) {
            "书源 JSON 不能超过 1 MiB"
        }
        val json = text.dropWhile { it.isWhitespace() || it == '\uFEFF' }
        val source = GSON.fromJsonObject<BookSource>(json).getOrThrow()
        require(source.bookSourceName.isNotBlank() && source.bookSourceUrl.isNotBlank()) {
            "源名称和 URL 不能为空"
        }
        return source
    }

    private const val MAX_SOURCE_BYTES = 1 * 1024 * 1024
}
