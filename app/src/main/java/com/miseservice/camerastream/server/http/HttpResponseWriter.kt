package com.miseservice.camerastream.server.http

import java.io.OutputStream

object HttpResponseWriter {
    fun write(output: OutputStream, response: HttpResponse) {
        val body = response.body.toByteArray(Charsets.UTF_8)
        val extraHeaders = response.extraHeaders.entries.joinToString(separator = "") {
            "${it.key}: ${it.value}\r\n"
        }

        val headers = (
            "HTTP/1.1 ${response.statusCode} ${response.statusText}\r\n" +
                "Content-Type: ${response.contentType}; charset=utf-8\r\n" +
                extraHeaders +
                "Content-Length: ${body.size}\r\n" +
                "Connection: close\r\n\r\n"
            ).toByteArray(Charsets.UTF_8)

        output.write(headers)
        output.write(body)
        output.flush()
    }
}

