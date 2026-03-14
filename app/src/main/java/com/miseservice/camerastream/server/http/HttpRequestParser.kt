package com.miseservice.camerastream.server.http

import java.io.BufferedReader

object HttpRequestParser {
    fun parse(input: BufferedReader): HttpRequest? {
        val requestLine = input.readLine() ?: return null
        val requestParts = requestLine.split(" ")
        val method = requestParts.getOrNull(0) ?: "GET"
        val path = requestParts.getOrNull(1) ?: "/"

        val headers = mutableMapOf<String, String>()
        while (true) {
            val line = input.readLine() ?: break
            if (line.isBlank()) break
            val split = line.split(":", limit = 2)
            if (split.size == 2) {
                headers[split[0].trim().lowercase()] = split[1].trim()
            }
        }

        val body = readBody(method, headers, input)
        return HttpRequest(method = method, path = path, headers = headers, body = body)
    }

    private fun readBody(method: String, headers: Map<String, String>, input: BufferedReader): String {
        if (method != "POST") return ""
        val length = headers["content-length"]?.toIntOrNull() ?: 0
        if (length <= 0) return ""

        val chars = CharArray(length)
        var totalRead = 0
        while (totalRead < length) {
            val read = input.read(chars, totalRead, length - totalRead)
            if (read == -1) break
            totalRead += read
        }

        return String(chars, 0, totalRead)
    }
}

