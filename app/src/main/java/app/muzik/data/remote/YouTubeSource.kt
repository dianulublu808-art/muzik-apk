package app.muzik.data.remote

import org.json.JSONArray
import org.json.JSONObject

/**
 * YouTube через публичный прокси-сервер (Invidious или Piped).
 *
 * Приложение само не разбирает подписи YouTube: адрес сервера задаётся в
 * настройках, поэтому при недоступности одного инстанса достаточно вписать
 * другой. Поддерживаются оба формата API — запросы пробуются по очереди.
 */
class YouTubeSource(
    private val instanceProvider: () -> String,
) : WaveSourceClient {

    override val source: WaveSource = WaveSource.YOUTUBE

    override suspend fun search(query: String, limit: Int): List<WaveTrack> {
        val base = baseUrl()
        var lastError: String? = null

        try {
            val result = invidiousSearch(base, query, limit)
            if (result.isNotEmpty()) return result
        } catch (t: Throwable) {
            lastError = t.message
        }

        try {
            val result = pipedSearch(base, query, limit)
            if (result.isNotEmpty()) return result
        } catch (t: Throwable) {
            lastError = t.message
        }

        throw WaveException("YouTube (${host(base)}): ${lastError ?: "ничего не найдено"}")
    }

    override suspend fun resolve(track: WaveTrack): String? {
        val base = baseUrl()
        val videoId = track.meta["videoId"] ?: track.uid.substringAfter(':', "")
        if (videoId.isBlank()) return null

        runCatching { return invidiousResolve(base, videoId) }
        runCatching { return pipedResolve(base, videoId) }
        return null
    }

    private suspend fun invidiousSearch(base: String, query: String, limit: Int): List<WaveTrack> {
        val array = Http.getJsonArray(
            "$base/api/v1/search?q=${Http.enc(query)}&type=video&sort_by=relevance",
        )
        val tracks = mutableListOf<WaveTrack>()
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            val videoId = item.optString("videoId")
            if (videoId.isBlank()) continue
            tracks += WaveTrack(
                uid = "yt:$videoId",
                source = source,
                title = item.optString("title").ifBlank { "Без названия" },
                artist = item.optString("author"),
                durationMs = item.optLong("lengthSeconds") * 1000L,
                artworkUrl = item.optJSONArray("videoThumbnails")
                    ?.optJSONObject(0)
                    ?.optString("url")
                    ?.takeIf { it.isNotBlank() },
                permalink = "https://www.youtube.com/watch?v=$videoId",
                meta = mapOf("videoId" to videoId),
            )
            if (tracks.size >= limit) break
        }
        return tracks
    }

    private suspend fun pipedSearch(base: String, query: String, limit: Int): List<WaveTrack> {
        val json = Http.getJsonObject("$base/search?q=${Http.enc(query)}&filter=music_songs")
        val items = json.optJSONArray("items") ?: return emptyList()
        val tracks = mutableListOf<WaveTrack>()
        for (index in 0 until items.length()) {
            val item = items.optJSONObject(index) ?: continue
            val videoId = videoIdFromPipedUrl(item.optString("url"))
            if (videoId.isBlank()) continue
            tracks += WaveTrack(
                uid = "yt:$videoId",
                source = source,
                title = item.optString("title").ifBlank { "Без названия" },
                artist = item.optString("uploaderName"),
                durationMs = item.optLong("duration") * 1000L,
                artworkUrl = item.optString("thumbnail").takeIf { it.isNotBlank() },
                permalink = "https://www.youtube.com/watch?v=$videoId",
                meta = mapOf("videoId" to videoId),
            )
            if (tracks.size >= limit) break
        }
        return tracks
    }

    private suspend fun invidiousResolve(base: String, videoId: String): String {
        val json = Http.getJsonObject("$base/api/v1/videos/$videoId?local=true")
        val formats = json.optJSONArray("adaptiveFormats")
        val audio = bestAudioFormat(formats)
        val url = audio?.optString("url")?.takeIf { it.isNotBlank() && it != "null" }
        if (url != null) return url
        return "$base/latest_version?id=$videoId&itag=140"
    }

    private suspend fun pipedResolve(base: String, videoId: String): String {
        val json = Http.getJsonObject("$base/streams/$videoId")
        val streams = json.optJSONArray("audioStreams") ?: throw WaveException("нет аудиопотоков")
        var best: JSONObject? = null
        for (index in 0 until streams.length()) {
            val stream = streams.optJSONObject(index) ?: continue
            if (stream.optString("url").isBlank()) continue
            if (best == null || stream.optLong("bitrate") > best.optLong("bitrate")) best = stream
        }
        return best?.optString("url")?.takeIf { it.isNotBlank() }
            ?: throw WaveException("нет прямых ссылок")
    }

    private fun bestAudioFormat(formats: JSONArray?): JSONObject? {
        if (formats == null) return null
        var mp4: JSONObject? = null
        var any: JSONObject? = null
        for (index in 0 until formats.length()) {
            val format = formats.optJSONObject(index) ?: continue
            val type = format.optString("type")
            if (!type.startsWith("audio")) continue
            val hasUrl = format.optString("url").isNotBlank() && format.optString("url") != "null"
            if (!hasUrl) continue
            if (any == null) any = format
            if (type.contains("mp4") && mp4 == null) mp4 = format
        }
        return mp4 ?: any
    }

    private fun videoIdFromPipedUrl(url: String): String =
        url.substringAfter("v=", "").substringBefore('&')

    private fun baseUrl(): String {
        val raw = instanceProvider().trim().trimEnd('/')
        if (raw.isBlank()) {
            throw WaveException("YouTube: укажите адрес Invidious или Piped в настройках «Волны»")
        }
        return if (raw.startsWith("http")) raw else "https://$raw"
    }

    private fun host(base: String): String = base.substringAfter("://").substringBefore('/')
}
