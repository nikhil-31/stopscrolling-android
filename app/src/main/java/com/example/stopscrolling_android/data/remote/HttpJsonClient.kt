package com.example.stopscrolling_android.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import java.io.IOException
import java.util.concurrent.TimeUnit

class HttpJsonClient(
    val okHttpClient: OkHttpClient = defaultClient()
) {
    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    val jsonMediaType = "application/json".toMediaType()

    suspend inline fun <reified T, reified B> post(
        baseUrl: String,
        path: String,
        body: B,
        accessToken: String? = null
    ): T = request("POST", baseUrl, path, json.encodeToString(body), accessToken, emptyMap())

    suspend inline fun <reified T> get(
        baseUrl: String,
        path: String,
        accessToken: String? = null,
        queryParams: Map<String, String> = emptyMap()
    ): T = request("GET", baseUrl, path, null, accessToken, queryParams)

    suspend inline fun <reified T> request(
        method: String,
        baseUrl: String,
        path: String,
        jsonBody: String?,
        accessToken: String?,
        queryParams: Map<String, String>
    ): T {
        val url = buildUrl(baseUrl, path, queryParams)

        val requestBuilder = Request.Builder().url(url)
        if (accessToken != null) {
            requestBuilder.addHeader("Authorization", "Bearer $accessToken")
        }
        when (method) {
            "GET" -> requestBuilder.get()
            "POST" -> {
                requestBuilder.addHeader("Content-Type", "application/json")
                requestBuilder.post((jsonBody ?: "{}").toRequestBody(jsonMediaType))
            }
            else -> throw IllegalArgumentException("Unsupported method: $method")
        }

        return withContext(Dispatchers.IO) {
            val responseBody: String
            val statusCode: Int
            try {
                okHttpClient.newCall(requestBuilder.build()).execute().use { response ->
                    statusCode = response.code
                    responseBody = response.body.string()
                }
            } catch (e: IOException) {
                throw ApiException.NetworkError(
                    BackendApiUrl.connectionErrorMessage(e.message.orEmpty(), url)
                )
            }

            if (statusCode !in 200..299) {
                throw ApiException.HttpError(statusCode, responseBody)
            }

            try {
                json.decodeFromString<T>(responseBody)
            } catch (_: Exception) {
                throw ApiException.InvalidResponse()
            }
        }
    }

    fun buildUrl(baseUrl: String, path: String, queryParams: Map<String, String>): String {
        val base = BackendApiUrl.resolve(baseUrl, path)?.toHttpUrlOrNull()
            ?: throw ApiException.InvalidEndpoint()
        val builder = base.newBuilder()
        queryParams.forEach { (key, value) -> builder.addQueryParameter(key, value) }
        return builder.build().toString()
    }

    companion object {
        fun defaultClient(): OkHttpClient {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            return OkHttpClient.Builder()
                .addInterceptor(logging)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()
        }
    }
}
