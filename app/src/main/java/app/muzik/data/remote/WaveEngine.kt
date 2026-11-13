package app.muzik.data.remote

import app.muzik.data.repository.MusicRepository
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first

/**
 * «Волна»: бесконечный поток треков из выбранных источников.
 *
 * Движок сам не управляет плеером — он только подбирает треки и достаёт
 * прямые ссылки. Очерёдностью занимается WaveViewModel.
 */
class WaveEngine(
    private val musicRepository: MusicRepository,
    youtubeInstanceProvider: () -> String,
    vkTokenProvider: () -> String,
) {

    private val soundCloud = SoundCloudSource()
    private val youTube = YouTubeSource(youtubeInstanceProvider)
    private val vk = VkSource(vkTokenProvider)

    private val streamCache = ConcurrentHashMap<String, String>()

    suspend fun nextBatch(
        sources: Set<WaveSource>,
        seeds: List<String>,
        disliked: Set<String>,
        perSource: Int = 8,
    ): WaveBatch {
        val clients = buildList {
            if (WaveSource.SOUNDCLOUD in sources) add(soundCloud)
            if (WaveSource.YOUTUBE in sources) add(youTube)
            if (WaveSource.VK in sources) add(vk)
        }
        if (clients.isEmpty()) {
            return WaveBatch(emptyList(), listOf("Включите хотя бы один источник в настройках «Волны»"))
        }

        val queries = seeds.ifEmpty { DEFAULT_SEEDS }.shuffled()
        val errors = mutableListOf<String>()
        val perClient = mutableListOf<List<WaveTrack>>()

        coroutineScope {
            val jobs = clients.mapIndexed { index, client ->
                client to async {
                    val query = queries[index % queries.size]
                    runCatching { client.search(query, perSource) }
                }
            }
            jobs.forEach { (client, job) ->
                job.await()
                    .onSuccess { tracks -> perClient += tracks }
                    .onFailure { error ->
                        errors += (error.message?.takeIf { it.isNotBlank() }
                            ?: "${client.source.label}: не удалось получить треки")
                    }
            }
        }

        val merged = interleave(perClient)
        val seen = HashSet<String>()
        val filtered = merged.filter { track ->
            track.uid !in disliked && seen.add(track.uid)
        }
        return WaveBatch(filtered, errors.distinct())
    }

    suspend fun localTracks(limit: Int): List<WaveTrack> {
        val tracks = runCatching { musicRepository.tracks.first() }.getOrDefault(emptyList())
        return tracks
            .shuffled()
            .take(limit)
            .map { track ->
                WaveTrack(
                    uid = "local:${track.id}",
                    source = WaveSource.LOCAL,
                    title = track.title,
                    artist = track.artist,
                    durationMs = track.durationMs,
                    artworkUrl = track.artworkUri?.toString(),
                    streamUrl = track.uri.toString(),
                    permalink = track.uri.toString(),
                )
            }
    }

    /** Достаёт прямую ссылку на поток. Возвращает null, если источник не смог. */
    suspend fun resolve(track: WaveTrack): WaveTrack {
        if (track.isPlayable) return track
        streamCache[track.uid]?.let { return track.withStream(it) }

        val client = when (track.source) {
            WaveSource.SOUNDCLOUD -> soundCloud
            WaveSource.YOUTUBE -> youTube
            WaveSource.VK -> vk
            WaveSource.LOCAL -> null
        } ?: return track

        val url = runCatching { client.resolve(track) }.getOrNull()
        if (!url.isNullOrBlank()) {
            streamCache[track.uid] = url
            return track.withStream(url)
        }
        return track
    }

    suspend fun resolveAll(tracks: List<WaveTrack>): List<WaveTrack> = coroutineScope {
        tracks
            .map { track -> async { resolve(track) } }
            .map { deferred -> deferred.await() }
            .filter { it.isPlayable }
    }

    private fun interleave(lists: List<List<WaveTrack>>): List<WaveTrack> {
        if (lists.isEmpty()) return emptyList()
        val maxSize = lists.maxOf { it.size }
        val result = mutableListOf<WaveTrack>()
        for (index in 0 until maxSize) {
            lists.forEach { list ->
                list.getOrNull(index)?.let(result::add)
            }
        }
        return result
    }

    companion object {
        val DEFAULT_SEEDS = listOf(
            "lofi hip hop",
            "chill electronic",
            "synthwave",
            "indie rock",
            "phonk",
            "jazz beats",
            "russian rap",
            "ambient",
        )
    }
}
