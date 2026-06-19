package com.example.stopscrolling_android.data.remote

sealed class ApiException(message: String) : Exception(message) {
    class InvalidEndpoint : ApiException("API base URL is invalid.")
    class InvalidResponse : ApiException("Unexpected response from the server.")
    class HttpError(val code: Int, body: String) :
        ApiException(ApiErrorParser.messageFromBody(body).ifEmpty { "Request failed with HTTP $code." })
    class NetworkError(message: String) : ApiException(message)
    class MissingAccessToken : ApiException("Sign in on the Account screen to sync events.")
}
