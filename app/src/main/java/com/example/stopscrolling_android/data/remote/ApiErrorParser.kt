package com.example.stopscrolling_android.data.remote

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

object ApiErrorParser {
    private val json = Json { ignoreUnknownKeys = true }

    fun messageFromBody(body: String): String {
        if (body.isBlank()) return "Request failed."
        return try {
            val element = json.parseToJsonElement(body)
            parseJsonError(element) ?: body.trim()
        } catch (_: Exception) {
            body.trim().ifEmpty { "Request failed." }
        }
    }

    private fun parseJsonError(element: JsonElement): String? {
        if (element !is JsonObject) return null

        element["detail"]?.jsonPrimitive?.content?.takeIf { it.isNotEmpty() }?.let { return it }

        element["non_field_errors"]?.let { errors ->
            firstStringFromArray(errors)?.let { return it }
        }

        for ((field, value) in element) {
            firstStringFromArray(value)?.let { return "${humanizeField(field)}: $it" }
        }
        return null
    }

    private fun firstStringFromArray(element: JsonElement): String? {
        if (element !is JsonArray) return null
        return element.firstOrNull()?.jsonPrimitive?.content?.takeIf { it.isNotEmpty() }
    }

    private fun humanizeField(field: String): String {
        return field.replace('_', ' ').replaceFirstChar { it.uppercase() }
    }
}
