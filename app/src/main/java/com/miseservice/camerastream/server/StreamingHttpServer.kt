package com.miseservice.camerastream.server

import android.graphics.Rect
import android.graphics.YuvImage
import android.util.Size
import kotlinx.coroutines.flow.StateFlow
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.net.ServerSocket
import java.net.SocketException
import java.util.concurrent.atomic.AtomicBoolean

class StreamingHttpServer(
    private val port: Int = 8080,
    private val frameDataFlow: StateFlow<ByteArray?>,
    private val frameSize: Size = Size(1280, 720)
) {

    private val isRunning = AtomicBoolean(false)
    private var serverSocket: ServerSocket? = null
    private var serverThread: Thread? = null
    private var lastFrameData: ByteArray? = null

    fun start() {
        if (isRunning.getAndSet(true)) return

        serverThread = Thread {
            try {
                serverSocket = ServerSocket(port)
                while (isRunning.get()) {
                    try {
                        val socket = serverSocket?.accept()
                        if (socket != null) {
                            Thread {
                                handleClient(socket)
                            }.start()
                        }
                    } catch (e: SocketException) {
                        if (isRunning.get()) {
                            e.printStackTrace()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                try {
                    serverSocket?.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }.apply {
            isDaemon = true
            start()
        }
    }

    private fun handleClient(socket: java.net.Socket) {
        try {
            val input = socket.inputStream.bufferedReader()
            val output = socket.outputStream

            // Read HTTP request
            val requestLine = input.readLine() ?: return

            // Parse path from request
            val parts = requestLine.split(" ")
            val path = if (parts.size > 1) parts[1] else "/"

            when {
                path == "/stream" -> streamMjpeg(output)
                path == "/status" -> sendStatus(output)
                else -> sendNotFound(output)
            }

            input.close()
            output.close()
            socket.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun streamMjpeg(output: OutputStream) {
        try {
            // Send HTTP response headers
            val headers = """
                HTTP/1.1 200 OK
                Connection: keep-alive
                Content-Type: multipart/x-mixed-replace; boundary=--boundary
                Cache-Control: no-cache
                Pragma: no-cache
                
            """.trimIndent().replace("\n", "\r\n")
            output.write(headers.toByteArray())
            output.flush()

            val boundary = "--boundary".toByteArray()
            val crlf = "\r\n".toByteArray()
            val header = "Content-Type: image/jpeg\r\nContent-Length: ".toByteArray()

            var lastTime = System.currentTimeMillis()
            val frameInterval = 33 // ~30 FPS

            while (isRunning.get()) {
                try {
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastTime < frameInterval) {
                        Thread.sleep(frameInterval - (currentTime - lastTime))
                    }
                    lastTime = System.currentTimeMillis()

                    val frameData = frameDataFlow.value
                    if (frameData != null && frameData !== lastFrameData) {
                        lastFrameData = frameData
                        val jpeg = yuv420ToJpeg(frameData, frameSize.width, frameSize.height, 85)

                        output.write(boundary)
                        output.write(crlf)
                        output.write(header)
                        output.write(jpeg.size.toString().toByteArray())
                        output.write(crlf)
                        output.write(crlf)
                        output.write(jpeg)
                        output.write(crlf)
                        output.flush()
                    }
                } catch (e: SocketException) {
                    break
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun sendStatus(output: OutputStream) {
        try {
            val status = if (isRunning.get()) "streaming" else "stopped"
            val body = """{"status":"$status"}"""
            val response = """
                HTTP/1.1 200 OK
                Content-Type: application/json
                Content-Length: ${body.length}
                
                $body
            """.trimIndent().replace("\n", "\r\n")
            output.write(response.toByteArray())
            output.flush()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun sendNotFound(output: OutputStream) {
        try {
            val response = "HTTP/1.1 404 Not Found\r\nContent-Type: text/plain\r\nContent-Length: 9\r\n\r\nNot Found"
            output.write(response.toByteArray())
            output.flush()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun yuv420ToJpeg(
        nv21: ByteArray,
        width: Int,
        height: Int,
        quality: Int = 85
    ): ByteArray {
        return try {
            val yuvImage = YuvImage(nv21, android.graphics.ImageFormat.NV21, width, height, null)
            val out = ByteArrayOutputStream()
            yuvImage.compressToJpeg(Rect(0, 0, width, height), quality, out)
            out.toByteArray()
        } catch (e: Exception) {
            e.printStackTrace()
            ByteArray(0)
        }
    }

    fun stop() {
        isRunning.set(false)
        try {
            serverSocket?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            serverThread?.join(1000)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        serverThread = null
        serverSocket = null
    }
}




