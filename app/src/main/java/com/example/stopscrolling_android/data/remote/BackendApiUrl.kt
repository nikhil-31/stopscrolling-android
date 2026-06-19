package com.example.stopscrolling_android.data.remote

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

object BackendApiUrl {
    const val DEFAULT_API_BASE_URL = "http://10.0.2.2"

    fun normalizeBaseUrl(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return DEFAULT_API_BASE_URL
        return if (trimmed.contains("://")) trimmed else "http://$trimmed"
    }

    fun resolve(baseUrl: String, path: String): String? {
        val normalized = normalizeBaseUrl(baseUrl)
        val base = normalized.toHttpUrlOrNull() ?: return null
        val trimmedPath = path.trim('/')
        return base.newBuilder()
            .addPathSegments(trimmedPath)
            .addPathSegment("")
            .build()
            .toString()
    }

    fun connectionErrorMessage(message: String, endpoint: String?): String {
        return "Cannot connect to ${endpoint ?: "the server"}. " +
            "On the Android emulator use http://10.0.2.2 (maps to host localhost). " +
            "With Docker Compose use http://localhost on desktop or http://10.0.2.2 on emulator. " +
            "Check Settings → Backend → API base URL. ($message)"
    }
}
