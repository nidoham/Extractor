package com.nidoham.extractor

import android.util.Log
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Modern OkHttp-based downloader for NewPipe Extractor
 * Optimized for Jetpack Compose YouTube-like apps with caching and retry logic
 */
class NewPipeDownloaderImpl(
    cacheDir: File? = null,
    cacheSize: Long = 10 * 1024 * 1024 // 10 MB default cache
) : Downloader() {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .apply {
            // Add cache if directory provided
            cacheDir?.let { dir ->
                val cacheDirectory = File(dir, "newpipe_http_cache")
                cache(Cache(cacheDirectory, cacheSize))
            }
        }
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    @Throws(IOException::class, ReCaptchaException::class)
    override fun execute(request: Request): Response {
        val httpMethod = request.httpMethod()
        val url = request.url()
        val headers = request.headers()
        val dataToSend = request.dataToSend()

        // Build OkHttp request
        val requestBuilder = okhttp3.Request.Builder()
            .method(httpMethod, dataToSend?.toRequestBody())
            .url(url)
            .addHeader("User-Agent", USER_AGENT)

        // Add custom headers
        for ((headerKey, headerValues) in headers) {
            requestBuilder.removeHeader(headerKey)
            for (headerValue in headerValues) {
                requestBuilder.addHeader(headerKey, headerValue)
            }
        }

        // Execute request
        val response = try {
            client.newCall(requestBuilder.build()).execute()
        } catch (e: IOException) {
            Log.e(TAG, "Network request failed: ${e.message}", e)
            throw IOException("Failed to execute request to $url", e)
        }

        // Handle response codes
        return when (response.code) {
            429 -> {
                response.close()
                Log.w(TAG, "Rate limit hit for URL: $url")
                throw ReCaptchaException("reCaptcha Challenge requested", url)
            }

            in 500..599 -> {
                val errorBody = response.body?.string()
                response.close()
                Log.e(TAG, "Server error ${response.code} for URL: $url")
                throw IOException("Server error: ${response.code} ${response.message}")
            }

            in 400..499 -> {
                // Client errors - still return response for NewPipe to handle
                val responseBody = response.body?.string()
                Log.w(TAG, "Client error ${response.code} for URL: $url")
                Response(
                    response.code,
                    response.message,
                    response.headers.toMultimap(),
                    responseBody,
                    response.request.url.toString()
                )
            }

            else -> {
                // Success responses (200-299, 300-399)
                val responseBody = response.body?.string()
                Response(
                    response.code,
                    response.message,
                    response.headers.toMultimap(),
                    responseBody,
                    response.request.url.toString()
                )
            }
        }
    }

    /**
     * Clean up resources
     */
    fun shutdown() {
        client.dispatcher.executorService.shutdown()
        client.connectionPool.evictAll()
        client.cache?.close()
    }

    companion object {
        private const val TAG = "NewPipeDownloader"

        // Updated User-Agent for better compatibility
        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:135.0) Gecko/20100101 Firefox/135.0"
    }
}