package com.miseservice.camerastream.server

import android.util.Log
import com.miseservice.camerastream.server.http.HttpRequestParser
import com.miseservice.camerastream.server.http.HttpResponse
import com.miseservice.camerastream.server.http.HttpResponseWriter
import com.miseservice.camerastream.server.http.WebRtcRouteHandler
import com.miseservice.camerastream.server.signaling.WebRtcSignalingDataSource
import com.miseservice.camerastream.webrtc.WebRtcSessionGateway
import java.io.OutputStream
import java.net.ServerSocket
import java.net.SocketException
import java.util.concurrent.atomic.AtomicBoolean

class WebRtcHttpServer(
    private val port: Int = 8080,
    private val webRtcSessionGateway: WebRtcSessionGateway
) {
    private val routeHandler = WebRtcRouteHandler(WebRtcSignalingDataSource(webRtcSessionGateway))
    private val isRunning = AtomicBoolean(false)
    private var serverSocket: ServerSocket? = null
    private var serverThread: Thread? = null

    companion object {
        private const val TAG = "WebRtcHttpServer"
    }

    fun start() {
        if (isRunning.getAndSet(true)) return

        serverThread = Thread {
            try {
                serverSocket = ServerSocket(port).also { it.reuseAddress = true }
                Log.i(TAG, "HTTP server listening on port $port")
                while (isRunning.get()) {
                    try {
                        val socket = serverSocket?.accept() ?: continue
                        Log.d(TAG, "New connection from ${socket.inetAddress}")
                        Thread {
                            val input = socket.inputStream.bufferedReader()
                            val output = socket.outputStream
                            try {
                                val request = HttpRequestParser.parse(input)
                                Log.d(TAG, "Request: ${request?.method} ${request?.path}")
                                handleClient(output, request)
                            } finally {
                                try { input.close() } catch (_: Exception) {}
                                try { output.close() } catch (_: Exception) {}
                                try { socket.close() } catch (_: Exception) {}
                            }
                        }.start()
                    } catch (_: SocketException) {
                        // ServerSocket fermé volontairement lors du stop()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Fatal error — server did NOT start on port $port: ${e.message}", e)
            } finally {
                try { serverSocket?.close() } catch (_: Exception) {}
                Log.i(TAG, "HTTP server stopped")
            }
        }.apply {
            name = "WebRtcHttpServer-$port"
            isDaemon = true
            start()
        }
    }

    fun stop() {
        isRunning.set(false)
        try { serverSocket?.close() } catch (_: Exception) {}
        try { serverThread?.join(1000) } catch (_: Exception) {}
        serverThread = null
        serverSocket = null
        Log.i(TAG, "stop() called")
    }

    private fun handleClient(output: OutputStream, request: com.miseservice.camerastream.server.http.HttpRequest?) {
        try {
            val response = if (request == null) {
                HttpResponse(
                    statusCode = 400,
                    statusText = "Bad Request",
                    contentType = "text/plain",
                    body = "Bad Request"
                )
            } else {
                routeHandler.handle(request)
            }
            HttpResponseWriter.write(output, response)
        } catch (_: Exception) {
            HttpResponseWriter.write(
                output,
                HttpResponse(
                    statusCode = 500,
                    statusText = "Error",
                    contentType = "text/plain",
                    body = "Internal Server Error"
                )
            )
        }
    }
}

