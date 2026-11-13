package app.muzik.data.remote

/**
 * Источники для «Волны».
 *
 * ВАЖНО: SoundCloud, YouTube и VK не предоставляют публичного API для такого
 * сценария. Приложение использует открытые (недокументированные) точки входа
 * этих сервисов: они могут измениться в любой момент, а условия использования
 * площадок могут запрещать подобное воспроизведение. Источники отключаются
 * в настройках, ошибки каждого источника показываются в интерфейсе.
 */
enum class WaveSource(val label: String, val badge: String) {
    SOUNDCLOUD("SoundCloud", "SC"),
    YOUTUBE("YouTube", "YT"),
    VK("VK", "VK"),
    LOCAL("Локальная медиатека", "LOCAL"),
    ;

    companion object {
        val remote: List<WaveSource> = listOf(SOUNDCLOUD, YOUTUBE, VK)

        fun fromName(value: String?): WaveSource? =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
    }
}

/**
 * Трек, который умеет играть «Волна»: либо локальный файл, либо поток из сети.
 */
data class WaveTrack(
    val uid: String,
    val source: WaveSource,
    val title: String,
    val artist: String,
    val durationMs: Long = 0L,
    val artworkUrl: String? = null,
    val streamUrl: String? = null,
    val permalink: String? = null,
    val meta: Map<String, String> = emptyMap(),
) {
    val displayArtist: String get() = artist.ifBlank { source.label }

    val isPlayable: Boolean get() = !streamUrl.isNullOrBlank()

    fun withStream(url: String): WaveTrack = copy(streamUrl = url)
}

/** Ошибка источника: текст показывается пользователю как есть. */
class WaveException(message: String) : Exception(message)

/** Результат подбора треков: сами треки и сообщения об ошибках источников. */
data class WaveBatch(
    val tracks: List<WaveTrack>,
    val errors: List<String>,
)

interface WaveSourceClient {
    val source: WaveSource

    suspend fun search(query: String, limit: Int): List<WaveTrack>

    /** Достаёт прямую ссылку на аудиопоток. null — если не получилось. */
    suspend fun resolve(track: WaveTrack): String?
}
