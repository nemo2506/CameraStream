package com.miseservice.camerastream.server.http

data class HttpRequest(
    val method: String,
    val path: String,
    val headers: Map<String, String>,
    val body: String
)

data class HttpResponse(
    val statusCode: Int,
    val statusText: String,
    val contentType: String,
    val body: String,
    val extraHeaders: Map<String, String> = emptyMap()
)

