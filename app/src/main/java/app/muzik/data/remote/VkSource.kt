package app.muzik.data.remote

import org.json.JSONArray
import org.json.JSONObject

/**
 * VK Audio.
 *
 * Публичного доступа к музыке VK больше не выдаёт: нужен access token
 * пользователя с правами на аудио. Токен вводится в настройках «Волны» и
 * хранится только на устройстве. Если VK отвечает ошибкой — её текст
 * показывается в интерфейсе как есть.
 */
class VkSource(
    private val tokenProvider: () -> String,
) : WaveSourceClient {

    override val source: WaveSource = WaveSource.VK

    override suspend fun search(query: String, limit: Int): List<WaveTrack> {
        val token = requireToken()
        val items = execute(
            method = "audio.search",
            params = mapOf(
                "q" to query,
                "count" to limit.coerceIn(1, 50).toString(),
                "sort" to "2",
            ),
            token = token,
        )
        return parseItems(items)
    }

    override suspend fun resolve(track: WaveTrack): String? {
        val direct = track.streamUrl?.takeIf { it.isNotBlank() }
        if (direct != null) return direct
        val audioId = track.meta["audioId"] ?: return null
        val token = requireToken()
        val items = execute(
            method = "audio.getById",
            params = mapOf("audios" to audioId),
            token = token,
        )
        return parseItems(items).firstOrNull()?.streamUrl
    }

    private suspend fun execute(
        method: String,
        params: Map<String, String>,
        token: String,
    ): JSONArray {
        val body = Http.postForm(
            url = "https://api.vk.com/method/$method",
            params = params + mapOf("access_token" to token, "v" to API_VERSION),
        )
        val json = runCatching { JSONObject(body) }
            .getOrElse { throw WaveException("VK: некорректный ответ") }
        json.optJSONObject("error")?.let { error ->
            val code = error.optInt("error_code")
            val message = error.optString("error_msg").ifBlank { "ошибка $code" }
            throw WaveException("VK: $message")
        }
        return when (val response = json.opt("response")) {
            is JSONArray -> response
            is JSONObject -> response.optJSONArray("items") ?: JSONArray()
            else -> throw WaveException("VK: пустой ответ")
        }
    }

    private fun parseItems(items: JSONArray?): List<WaveTrack> {
        if (items == null) return emptyList()
        val tracks = mutableListOf<WaveTrack>()
        for (index in 0 until items.length()) {
            val item = items.optJSONObject(index) ?: continue
            val audioId = item.optLong("id")
            val ownerId = item.optLong("owner_id")
            if (audioId == 0L) continue
            val url = item.optString("url").takeIf { it.isNotBlank() && it != "null" }
            tracks += WaveTrack(
                uid = "vk:${ownerId}_$audioId",
                source = source,
                title = item.optString("title").ifBlank { "Без названия" },
                artist = item.optString("artist"),
                durationMs = item.optLong("duration") * 1000L,
                artworkUrl = item.optString("thumb").takeIf { it.isNotBlank() },
                streamUrl = url,
                permalink = "https://vk.com/audio${ownerId}_$audioId",
                meta = mapOf("audioId" to "${ownerId}_$audioId"),
            )
        }
        return tracks
    }

    private fun requireToken(): String {
        val token = tokenProvider().trim()
        if (token.isBlank()) {
            throw WaveException("VK: вставьте access token в настройках «Волны»")
        }
        return token
    }

    private companion object {
        const val API_VERSION = "5.199"
    }
}
