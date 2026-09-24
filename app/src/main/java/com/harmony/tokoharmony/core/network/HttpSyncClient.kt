package com.harmony.tokoharmony.core.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

data class HttpResponse(
    val statusCode: Int,
    val body: String,
    val isSuccessful: Boolean = statusCode in 200..299
)

@Singleton
class HttpSyncClient @Inject constructor(
    private val syncConfig: SyncConfig
) {
    companion object {
        private const val CONNECT_TIMEOUT_MS = 25_000
        private const val READ_TIMEOUT_MS = 35_000
        private const val MAX_REDIRECTS = 5
    }

    suspend fun postJson(urlString: String, jsonBody: String): HttpResponse = withContext(Dispatchers.IO) {
        executeRequestWithRedirects(urlString = urlString, method = "POST", jsonBody = jsonBody)
    }

    suspend fun getJson(urlString: String): HttpResponse = withContext(Dispatchers.IO) {
        executeRequestWithRedirects(urlString = urlString, method = "GET", jsonBody = null)
    }

    private fun executeRequestWithRedirects(
        urlString: String,
        method: String,
        jsonBody: String?,
        redirectCount: Int = 0
    ): HttpResponse {
        if (redirectCount > MAX_REDIRECTS) {
            return HttpResponse(
                statusCode = -1,
                body = "{\"status\":\"ERROR\",\"error\":\"Too many redirects ($redirectCount)\"}",
                isSuccessful = false
            )
        }

        var connection: HttpURLConnection? = null
        try {
            val url = URL(urlString)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                instanceFollowRedirects = false // Manually handle redirects for Google Apps Script
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "TokoHarmony-Android-POS/1.0")

                val token = syncConfig.syncToken
                if (token.isNotBlank()) {
                    setRequestProperty("X-Sync-Token", token)
                }

                if (jsonBody != null && (method == "POST" || method == "PUT")) {
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                    OutputStreamWriter(outputStream, Charsets.UTF_8).use { writer ->
                        writer.write(jsonBody)
                        writer.flush()
                    }
                }
            }

            val responseCode = connection.responseCode

            // Google Apps Script redirects with 302 to script.googleusercontent.com
            // Following 302/303 from Google Apps Script requires a GET request to the redirect URL
            if (responseCode in listOf(HttpURLConnection.HTTP_MOVED_PERM, HttpURLConnection.HTTP_MOVED_TEMP, HttpURLConnection.HTTP_SEE_OTHER, 307, 308)) {
                val newUrl = connection.getHeaderField("Location")
                connection.disconnect()

                if (!newUrl.isNullOrBlank()) {
                    // Google Apps Script doPost redirects to a temporary GET URL serving the response
                    val nextMethod = if (responseCode == HttpURLConnection.HTTP_MOVED_TEMP || responseCode == HttpURLConnection.HTTP_SEE_OTHER) {
                        "GET"
                    } else {
                        method
                    }
                    return executeRequestWithRedirects(
                        urlString = newUrl,
                        method = nextMethod,
                        jsonBody = if (nextMethod == "GET") null else jsonBody,
                        redirectCount = redirectCount + 1
                    )
                }
            }

            val inputStream = if (responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream ?: connection.inputStream
            }

            val responseText = inputStream?.use { stream ->
                BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { reader ->
                    reader.readText()
                }
            } ?: ""

            return HttpResponse(
                statusCode = responseCode,
                body = responseText,
                isSuccessful = responseCode in 200..299
            )
        } catch (e: Exception) {
            return HttpResponse(
                statusCode = -1,
                body = "{\"status\":\"ERROR\",\"error\":\"${e.localizedMessage ?: e.javaClass.simpleName}\"}",
                isSuccessful = false
            )
        } finally {
            connection?.disconnect()
        }
    }
}
