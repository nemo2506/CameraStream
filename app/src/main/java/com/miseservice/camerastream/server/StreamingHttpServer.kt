package com.miseservice.camerastream.server

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import com.miseservice.camerastream.camera.CameraFrame
import kotlinx.coroutines.flow.StateFlow
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.net.ServerSocket
import java.net.SocketException
import java.util.concurrent.atomic.AtomicBoolean

class StreamingHttpServer(
    private val port: Int = 8080,
    private val frameDataFlow: StateFlow<CameraFrame?>
) {

    private val isRunning = AtomicBoolean(false)
    private var serverSocket: ServerSocket? = null
    private var serverThread: Thread? = null
    @Volatile
    private var cachedFrameRef: CameraFrame? = null

    @Volatile
    private var cachedJpeg: ByteArray? = null

    private val encodeLock = Any()

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
            var clientLastFrame: CameraFrame? = null

            while (isRunning.get()) {
                try {
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastTime < frameInterval) {
                        Thread.sleep(frameInterval - (currentTime - lastTime))
                    }
                    lastTime = System.currentTimeMillis()

                    val frameData = frameDataFlow.value
                    if (frameData != null && frameData !== clientLastFrame) {
                        clientLastFrame = frameData
                        val jpeg = getOrEncodeJpeg(frameData)
                        if (jpeg.isEmpty()) {
                            continue
                        }

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
                } catch (_: SocketException) {
                    break
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getOrEncodeJpeg(frame: CameraFrame): ByteArray {
        val snapshotFrame = cachedFrameRef
        val snapshotJpeg = cachedJpeg
        if (snapshotFrame === frame && snapshotJpeg != null) {
            return snapshotJpeg
        }

        synchronized(encodeLock) {
            if (cachedFrameRef === frame && cachedJpeg != null) {
                return cachedJpeg ?: ByteArray(0)
            }

            val encoded = yuv420ToJpeg(
                nv21 = frame.nv21,
                width = frame.width,
                height = frame.height,
                rotationDegrees = frame.rotationDegrees
            )

            cachedFrameRef = frame
            cachedJpeg = encoded
            return encoded
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
        rotationDegrees: Int = 0
    ): ByteArray {
        return try {
            val yuvImage = YuvImage(nv21, android.graphics.ImageFormat.NV21, width, height, null)
            val out = ByteArrayOutputStream()
            yuvImage.compressToJpeg(Rect(0, 0, width, height), 85, out)
            rotateJpegIfNeeded(out.toByteArray(), rotationDegrees)
        } catch (e: Exception) {
            e.printStackTrace()
            ByteArray(0)
        }
    }

    private fun rotateJpegIfNeeded(jpeg: ByteArray, rotationDegrees: Int): ByteArray {
        val normalizedRotation = ((rotationDegrees % 360) + 360) % 360
        if (normalizedRotation == 0) return jpeg

        return try {
            val sourceBitmap = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size) ?: return jpeg
            val matrix = Matrix().apply { postRotate(normalizedRotation.toFloat()) }
            val rotatedBitmap = Bitmap.createBitmap(
                sourceBitmap,
                0,
                0,
                sourceBitmap.width,
                sourceBitmap.height,
                matrix,
                true
            )

            val output = ByteArrayOutputStream()
            rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 85, output)

            if (rotatedBitmap !== sourceBitmap) {
                rotatedBitmap.recycle()
            }
            sourceBitmap.recycle()

            output.toByteArray()
        } catch (e: Exception) {
            e.printStackTrace()
            jpeg
        }
    }

    fun stop() {
        isRunning.set(false)
        cachedFrameRef = null
        cachedJpeg = null
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




