package com.miseservice.camerastream.server

import com.miseservice.camerastream.webrtc.WebRtcEngine
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import java.io.BufferedReader
import java.io.OutputStream
import java.net.ServerSocket
import java.net.SocketException
import java.util.concurrent.atomic.AtomicBoolean

class WebRtcHttpServer(
    private val port: Int = 8080,
    private val webRtcEngine: WebRtcEngine
) {
    private val isRunning = AtomicBoolean(false)
    private var serverSocket: ServerSocket? = null
    private var serverThread: Thread? = null

    fun start() {
        if (isRunning.getAndSet(true)) return

        serverThread = Thread {
            try {
                serverSocket = ServerSocket(port)
                while (isRunning.get()) {
                    try {
                        val socket = serverSocket?.accept() ?: continue
                        Thread { handleClient(socket.inputStream.bufferedReader(), socket.outputStream) }
                            .start()
                    } catch (_: SocketException) {
                    }
                }
            } catch (_: Exception) {
            } finally {
                try {
                    serverSocket?.close()
                } catch (_: Exception) {
                }
            }
        }.apply {
            isDaemon = true
            start()
        }
    }

    fun stop() {
        isRunning.set(false)
        try {
            serverSocket?.close()
        } catch (_: Exception) {
        }
        try {
            serverThread?.join(1000)
        } catch (_: Exception) {
        }
        serverThread = null
        serverSocket = null
    }

    private fun handleClient(input: BufferedReader, output: OutputStream) {
        try {
            val requestLine = input.readLine() ?: return
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

            val body = readBody(method, input, headers)

            when {
                method == "GET" && (path == "/" || path == "/viewer") -> sendHtml(output, VIEWER_HTML)
                method == "POST" && path == "/api/webrtc/offer" -> handleOffer(output, body)
                method == "GET" && path == "/status" -> sendJson(output, JSONObject().put("status", "streaming").put("protocol", "webrtc").toString())
                else -> sendText(output, 404, "Not Found")
            }
        } catch (_: Exception) {
            sendText(output, 500, "Internal Server Error")
        } finally {
            try {
                input.close()
            } catch (_: Exception) {
            }
            try {
                output.close()
            } catch (_: Exception) {
            }
        }
    }

    private fun readBody(method: String, input: BufferedReader, headers: Map<String, String>): String {
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

    private fun handleOffer(output: OutputStream, body: String) {
        try {
            val json = JSONObject(body)
            val offerSdp = json.optString("offerSdp", "")
            if (offerSdp.isBlank()) {
                sendJson(output, JSONObject().put("error", "offerSdp manquant").toString(), statusCode = 400)
                return
            }

            val answerSdp = runBlocking {
                webRtcEngine.createAnswer(offerSdp)
            }

            val response = JSONObject()
                .put("answerType", "answer")
                .put("answerSdp", answerSdp)
                .toString()

            sendJson(output, response)
        } catch (e: Exception) {
            val errorJson = JSONObject().put("error", e.message ?: "Erreur WebRTC").toString()
            sendJson(output, errorJson, statusCode = 500)
        }
    }

    private fun sendHtml(output: OutputStream, html: String) {
        val body = html.toByteArray(Charsets.UTF_8)
        val headers = (
            "HTTP/1.1 200 OK\r\n" +
                "Content-Type: text/html; charset=utf-8\r\n" +
                "Content-Length: ${body.size}\r\n" +
                "Cache-Control: no-cache\r\n" +
                "Connection: close\r\n\r\n"
            ).toByteArray(Charsets.UTF_8)
        output.write(headers)
        output.write(body)
        output.flush()
    }

    private fun sendJson(output: OutputStream, json: String, statusCode: Int = 200) {
        val statusText = if (statusCode == 200) "OK" else "Error"
        val body = json.toByteArray(Charsets.UTF_8)
        val headers = (
            "HTTP/1.1 $statusCode $statusText\r\n" +
                "Content-Type: application/json; charset=utf-8\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Content-Length: ${body.size}\r\n" +
                "Connection: close\r\n\r\n"
            ).toByteArray(Charsets.UTF_8)
        output.write(headers)
        output.write(body)
        output.flush()
    }

    private fun sendText(output: OutputStream, statusCode: Int, text: String) {
        val body = text.toByteArray(Charsets.UTF_8)
        val headers = (
            "HTTP/1.1 $statusCode Error\r\n" +
                "Content-Type: text/plain; charset=utf-8\r\n" +
                "Content-Length: ${body.size}\r\n" +
                "Connection: close\r\n\r\n"
            ).toByteArray(Charsets.UTF_8)
        output.write(headers)
        output.write(body)
        output.flush()
    }

    companion object {
        private val VIEWER_HTML = """
            <!doctype html>
            <html lang="fr">
            <head>
              <meta charset="utf-8" />
              <meta name="viewport" content="width=device-width,initial-scale=1" />
              <title>CameraStream WebRTC</title>
              <style>
                html,body { margin: 0; background: #0f1115; color: #e8eaf0; font-family: Arial, sans-serif; }
                .wrap { padding: 12px; }
                video { width: 100%; max-height: 80vh; background: black; border-radius: 12px; }
                .state { margin-top: 8px; font-size: 14px; opacity: .9; }
              </style>
            </head>
            <body>
              <div class="wrap">
                <h2>Flux camera WebRTC</h2>
                <video id="v" autoplay playsinline muted></video>
                <div class="state" id="state">Connexion...</div>
              </div>
              <script>
                (async () => {
                  const stateEl = document.getElementById('state');
                  const videoEl = document.getElementById('v');
                  const pc = new RTCPeerConnection({ iceServers: [{ urls: 'stun:stun.l.google.com:19302' }] });

                  pc.ontrack = (event) => {
                    if (event.streams && event.streams[0]) {
                      videoEl.srcObject = event.streams[0];
                    }
                  };

                  pc.onconnectionstatechange = () => {
                    stateEl.textContent = 'Etat WebRTC: ' + pc.connectionState;
                  };

                  try {
                    const offer = await pc.createOffer({ offerToReceiveVideo: true, offerToReceiveAudio: false });
                    await pc.setLocalDescription(offer);

                    const response = await fetch('/api/webrtc/offer', {
                      method: 'POST',
                      headers: { 'Content-Type': 'application/json' },
                      body: JSON.stringify({ offerSdp: offer.sdp })
                    });

                    const data = await response.json();
                    if (!response.ok || !data.answerSdp) {
                      throw new Error(data.error || 'Reponse SDP invalide');
                    }

                    await pc.setRemoteDescription({ type: data.answerType || 'answer', sdp: data.answerSdp });
                    stateEl.textContent = 'Connecte';
                  } catch (err) {
                    stateEl.textContent = 'Erreur: ' + (err.message || err);
                  }
                })();
              </script>
            </body>
            </html>
        """.trimIndent()
    }
}

