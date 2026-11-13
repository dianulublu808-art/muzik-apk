package app.muzik.data.remote

import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Минимальный HTTP-клиент на HttpURLConnection: без сторонних зависимостей,
 * только то, что нужно источникам «Волны».
 */
object Http {

    private const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122 Mobile Safari/537.36"

    suspend fun getText(
        url: String,
        headers: Map<String, String> = emptyMap(),
        timeoutMs: Int = 15_000,
    ): String = withContext(Dispatchers.IO) {
        request(url, method = "GET", params = null, headers = headers, timeoutMs = timeoutMs)
    }

    suspend fun getJsonObject(
        url: String,
        headers: Map<String, String> = emptyMap(),
        timeoutMs: Int = 15_000,
    ): JSONObject = JSONObject(getText(url, headers, timeoutMs))

    suspend fun getJsonArray(
        url: String,
        headers: Map<String, String> = emptyMap(),
        timeoutMs: Int = 15_000,
    ): JSONArray = JSONArray(getText(url, headers, timeoutMs))

    suspend fun postForm(
        url: String,
        params: Map<String, String>,
        headers: Map<String, String> = emptyMap(),
        timeoutMs: Int = 20_000,
    ): String = withContext(Dispatchers.IO) {
        request(url, method = "POST", params = params, headers = headers, timeoutMs = timeoutMs)
    }

    fun enc(value: String): String = URLEncoder.encode(value, "UTF-8")

    private fun request(
        url: String,
        method: String,
        params: Map<String, String>?,
        headers: Map<String, String>,
        timeoutMs: Int,
    ): String {
        val body = params
            ?.entries
            ?.joinToString("&") { "${enc(it.key)}=${enc(it.value)}" }

        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = timeoutMs
            readTimeout = timeoutMs
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", USER_AGENT)
            setRequestProperty("Accept", "application/json, text/html;q=0.9, */*;q=0.8")
            headers.forEach { (key, value) -> setRequestProperty(key, value) }
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
            }
        }

        try {
            if (body != null) {
                connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader()?.use(BufferedReader::readText).orEmpty()
            if (code !in 200..299) {
                throw WaveException("HTTP $code: ${text.compact().take(180)}")
            }
            if (text.isBlank()) throw WaveException("Пустой ответ сервера")
            return text
        } finally {
            connection.disconnect()
        }
    }

    private fun String.compact(): String = replace(Regex("\\s+"), " ").trim()
}
