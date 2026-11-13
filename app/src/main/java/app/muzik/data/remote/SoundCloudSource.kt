package app.muzik.data.remote

import org.json.JSONArray
import org.json.JSONObject

/**
 * SoundCloud через открытый api-v2.
 *
 * Ключ доступа (client_id) не хранится в приложении: он ищется в JS-бандле
 * веб-плеера при первом обращении и кэшируется в памяти до перезапуска.
 */
class SoundCloudSource : WaveSourceClient {

    override val source: WaveSource = WaveSource.SOUNDCLOUD

    @Volatile
    private var cachedClientId: String? = null

    override suspend fun search(query: String, limit: Int): List<WaveTrack> {
        val clientId = clientId()
        val url = buildString {
            append("https://api-v2.soundcloud.com/search/tracks")
            append("?q=").append(Http.enc(query))
            append("&client_id=").append(clientId)
            append("&limit=").append(limit.coerceIn(1, 50))
            append("&offset=0&linked_partitioning=1&app_locale=ru")
        }
        val json = Http.getJsonObject(url)
        val collection = json.optJSONArray("collection") ?: return emptyList()
        return parseCollection(collection)
    }

    override suspend fun resolve(track: WaveTrack): String? {
        val hint = track.meta["hint"] ?: return null
        val clientId = clientId()
        val url = if (hint.contains("client_id=")) hint else "$hint?client_id=$clientId"
        val json = Http.getJsonObject(url)
        return json.optString("url").takeIf { it.isNotBlank() && it != "null" }
    }

    private fun parseCollection(collection: JSONArray): List<WaveTrack> {
        val tracks = mutableListOf<WaveTrack>()
        for (index in 0 until collection.length()) {
            val item = collection.optJSONObject(index) ?: continue
            val trackId = item.optLong("id")
            if (trackId == 0L) continue
            val title = item.optString("title").ifBlank { "Без названия" }
            val artist = item.optJSONObject("user")?.optString("username").orEmpty()
            val artwork = item.optString("artwork_url")
                .takeIf { it.isNotBlank() && it != "null" }
                ?.replace("-large", "-t500x500")
            val duration = item.optLong("duration")
            val permalink = item.optString("permalink_url").takeIf { it.isNotBlank() }

            val transcodings = item.optJSONObject("media")?.optJSONArray("transcodings")
            val chosen = pickTranscoding(transcodings)
            val hint = chosen?.optString("url")?.takeIf { it.isNotBlank() }
            val protocol = chosen?.optString("protocol").orEmpty()

            tracks += WaveTrack(
                uid = "sc:$trackId",
                source = source,
                title = title,
                artist = artist,
                durationMs = duration,
                artworkUrl = artwork,
                permalink = permalink,
                meta = buildMap {
                    if (hint != null) put("hint", hint)
                    if (protocol.isNotBlank()) put("protocol", protocol)
                },
            )
        }
        return tracks
    }

    private fun pickTranscoding(transcodings: JSONArray?): JSONObject? {
        if (transcodings == null) return null
        var progressive: JSONObject? = null
        var hls: JSONObject? = null
        for (index in 0 until transcodings.length()) {
            val transcoding = transcodings.optJSONObject(index) ?: continue
            when (transcoding.optString("protocol")) {
                "progressive" -> if (progressive == null) progressive = transcoding
                "hls" -> if (hls == null) hls = transcoding
            }
        }
        return progressive ?: hls
    }

    private suspend fun clientId(): String {
        cachedClientId?.let { return it }
        val page = runCatching { Http.getText(PAGE_URL, mapOf("Accept" to "text/html"), 20_000) }
            .recoverCatching { Http.getText(MOBILE_PAGE_URL, mapOf("Accept" to "text/html"), 20_000) }
            .getOrElse { throw WaveException("SoundCloud недоступен: ${it.message}") }

        val scripts = SCRIPT_REGEX.findAll(page)
            .map { it.value }
            .distinct()
            .take(8)
            .toList()

        for (script in scripts) {
            val js = runCatching { Http.getText(script, timeoutMs = 20_000) }.getOrNull() ?: continue
            val found = CLIENT_ID_REGEX.find(js)?.groupValues?.getOrNull(1)
            if (!found.isNullOrBlank()) {
                cachedClientId = found
                return found
            }
        }
        throw WaveException("SoundCloud не отдал ключ доступа (сервис изменился)")
    }

    private companion object {
        const val PAGE_URL = "https://soundcloud.com/"
        const val MOBILE_PAGE_URL = "https://m.soundcloud.com/"
        val SCRIPT_REGEX = Regex("https://a-v2\\.sndcdn\\.com/assets/[A-Za-z0-9._-]+\\.js")
        val CLIENT_ID_REGEX = Regex("client_id\\s*[:=]\\s*\"([A-Za-z0-9]{32})\"")
    }
}
