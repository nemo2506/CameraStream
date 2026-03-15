package com.miseservice.camerastream.server.http

import android.os.Build
import com.miseservice.camerastream.domain.model.BatteryInfo
import com.miseservice.camerastream.server.signaling.SignalingDataSource
import kotlinx.coroutines.runBlocking
import org.json.JSONObject

class WebRtcRouteHandler(
    private val signalingDataSource: SignalingDataSource,
    private val batteryProvider: () -> BatteryInfo? = { null }
) {
    fun handle(request: HttpRequest): HttpResponse {
        val method = request.method.uppercase()

        return when {
            method == "OPTIONS" && request.path == "/api/webrtc/offer" -> {
                HttpResponse(
                    statusCode = 204,
                    statusText = "No Content",
                    contentType = "text/plain",
                    body = "",
                    extraHeaders = corsHeaders()
                )
            }

            method == "GET" && (request.path == "/" || request.path == "/viewer") -> {
                HttpResponse(
                    statusCode = 200,
                    statusText = "OK",
                    contentType = "text/html",
                    body = viewerHtml(),
                    extraHeaders = mapOf("Cache-Control" to "no-cache")
                )
            }

            method == "GET" && request.path == "/favicon.ico" -> {
                HttpResponse(
                    statusCode = 200,
                    statusText = "OK",
                    contentType = "image/svg+xml",
                    body = FAVICON_SVG,
                    extraHeaders = mapOf("Cache-Control" to "public, max-age=86400")
                )
            }

            method == "POST" && request.path == "/api/webrtc/offer" -> {
                handleOffer(request.body)
            }

            method == "GET" && request.path == "/status" -> {
                HttpResponse(
                    statusCode = 200,
                    statusText = "OK",
                    contentType = "application/json",
                    body = JSONObject()
                        .put("status", "streaming")
                        .put("protocol", "webrtc")
                        .put("viewers", signalingDataSource.activeSessionCount())
                        .toString(),
                    extraHeaders = corsHeaders()
                )
            }

            method == "GET" && request.path == "/api/battery" -> {
                handleBattery()
            }

            else -> {
                HttpResponse(
                    statusCode = 404,
                    statusText = "Not Found",
                    contentType = "text/plain",
                    body = "Not Found",
                    extraHeaders = corsHeaders()
                )
            }
        }
    }

    private fun viewerHtml(): String {
        val title = escapeHtml(deviceModelTitle())
        return VIEWER_HTML_TEMPLATE.replace("__DEVICE_TITLE__", title)
    }

    private fun deviceModelTitle(): String {
        val manufacturer = Build.MANUFACTURER?.trim().orEmpty()
        val model = Build.MODEL?.trim().orEmpty()
        val combined = when {
            model.isBlank() -> "Android Camera"
            manufacturer.isBlank() -> model
            model.startsWith(manufacturer, ignoreCase = true) -> model
            else -> "$manufacturer $model"
        }
        return combined
            .replace(Regex("\\s+"), " ")
            .trim()
            .ifBlank { "Android Camera" }
    }

    private fun escapeHtml(value: String): String {
        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
    }

    private fun corsHeaders(): Map<String, String> = mapOf(
        "Access-Control-Allow-Origin" to "*",
        "Access-Control-Allow-Methods" to "GET, POST, OPTIONS",
        "Access-Control-Allow-Headers" to "Content-Type, Authorization",
        "Access-Control-Max-Age" to "86400"
    )

    private fun handleBattery(): HttpResponse {
        val battery = batteryProvider()
        if (battery == null) {
            return HttpResponse(
                statusCode = 503,
                statusText = "Service Unavailable",
                contentType = "application/json",
                body = """{"ok":false,"code":503,"message":"battery_unavailable"}""",
                extraHeaders = corsHeaders()
            )
        }
        val tempStr = battery.temperatureC?.toString() ?: "null"
        val statusRaw = battery.status
            .replace("\\", "\\\\").replace("\"", "\\\"")
        return HttpResponse(
            statusCode = 200,
            statusText = "OK",
            contentType = "application/json",
            body = """{"ok":true,"levelPercent":${battery.levelPercent},"isCharging":${battery.isCharging},"status":"$statusRaw","temperatureC":$tempStr,"timestampMs":${battery.timestampMs}}""",
            extraHeaders = corsHeaders()
        )
    }

    private fun handleOffer(body: String): HttpResponse {
        return try {
            val json = JSONObject(body)
            val offerSdp = json.optString("offerSdp", "")
            if (offerSdp.isBlank()) {
                return HttpResponse(
                    statusCode = 400,
                    statusText = "Bad Request",
                    contentType = "application/json",
                    body = JSONObject().put("error", "offerSdp manquant").toString(),
                    extraHeaders = mapOf("Access-Control-Allow-Origin" to "*")
                )
            }

            val answerSdp = runBlocking {
                signalingDataSource.createAnswer(offerSdp)
            }

            HttpResponse(
                statusCode = 200,
                statusText = "OK",
                contentType = "application/json",
                body = JSONObject()
                    .put("answerType", "answer")
                    .put("answerSdp", answerSdp)
                    .toString(),
                extraHeaders = mapOf("Access-Control-Allow-Origin" to "*")
            )
        } catch (e: Exception) {
            HttpResponse(
                statusCode = 500,
                statusText = "Error",
                contentType = "application/json",
                body = JSONObject().put("error", e.message ?: "Erreur WebRTC").toString(),
                extraHeaders = mapOf("Access-Control-Allow-Origin" to "*")
            )
        }
    }

    companion object {
        private val FAVICON_SVG = """
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 64 64">
              <defs>
                <linearGradient id="g" x1="0" y1="0" x2="1" y2="1">
                  <stop offset="0%" stop-color="#4f46e5"/>
                  <stop offset="100%" stop-color="#0ea5e9"/>
                </linearGradient>
              </defs>
              <rect width="64" height="64" rx="14" fill="url(#g)"/>
              <path d="M16 24h10l5-5h10l5 5h2a6 6 0 0 1 6 6v14a6 6 0 0 1-6 6H16a6 6 0 0 1-6-6V30a6 6 0 0 1 6-6z" fill="#ffffff" fill-opacity=".92"/>
              <circle cx="32" cy="37" r="10" fill="#1f2937"/>
              <circle cx="32" cy="37" r="6" fill="#93c5fd"/>
            </svg>
        """.trimIndent()

         private val VIEWER_HTML_TEMPLATE = """
             <!doctype html>
             <html lang="fr">
             <head>
               <meta charset="utf-8"/>
               <meta name="viewport" content="width=device-width,initial-scale=1"/>
               <meta name="theme-color" content="#0f1115"/>
               <title>__DEVICE_TITLE__</title>
               <link rel="icon" type="image/svg+xml" href="/favicon.ico"/>
               <style>
                *,*::before,*::after{box-sizing:border-box;margin:0;padding:0}
                html,body{width:100%;height:100%;background:#000;overflow:hidden;font-family:system-ui,sans-serif}

                /* Vidéo plein écran */
                #v{
                  display:block;width:100%;height:100vh;
                  object-fit:cover;background:#000;
                }
                #v.force-landscape{
                  transform:rotate(90deg);
                  transform-origin:center center;
                  width:100vh;
                  height:100vw;
                }

                /* Barre admin flottante compacte */
                #bar{
                  position:fixed;left:50%;bottom:12px;transform:translateX(-50%);
                  display:flex;align-items:center;gap:8px;flex-wrap:wrap;
                  width:max-content;max-width:calc(100vw - 16px);
                  padding:8px 10px calc(8px + env(safe-area-inset-bottom,0px)) 10px;
                  background:rgba(8,10,16,.72);
                  border:1px solid rgba(255,255,255,.10);
                  border-radius:16px;
                  backdrop-filter:blur(14px);
                  box-shadow:0 10px 28px rgba(0,0,0,.28);
                  font-size:12px;color:#e8eaf0;
                  transition:opacity .25s ease,transform .25s ease;
                }
                #bar.hide{opacity:0;pointer-events:none}

                .pill{
                  display:inline-flex;align-items:center;gap:5px;
                  min-height:32px;
                  background:rgba(255,255,255,.08);
                  border:1px solid rgba(255,255,255,.08);
                  border-radius:999px;padding:4px 10px;white-space:nowrap;
                }
                .dot{
                  width:7px;height:7px;border-radius:50%;flex-shrink:0;
                  background:#aaa;transition:background .3s;
                }
                .dot.ok  {background:#22c55e}
                .dot.warn{background:#f59e0b}
                .dot.err {background:#ef4444}

                .icon{
                  width:14px;height:14px;display:inline-block;flex-shrink:0;
                  color:#dbe2f0;
                }
                .icon svg{
                  width:100%;height:100%;display:block;
                  stroke:currentColor;fill:none;stroke-width:1.8;
                  stroke-linecap:round;stroke-linejoin:round;
                }

                .meta{font-variant-numeric:tabular-nums}

                #btn-mute,#btn-fit,#btn-fs{
                  cursor:pointer;
                  display:inline-flex;align-items:center;justify-content:center;
                  min-width:36px;min-height:32px;
                  background:rgba(255,255,255,.12);border:1px solid rgba(255,255,255,.10);color:#fff;
                  border-radius:10px;padding:4px 8px;font-size:15px;
                }
                #btn-mute{margin-left:auto}
                #btn-mute:hover,#btn-fit:hover,#btn-fs:hover{background:rgba(255,255,255,.22)}
                #btn-mute svg,#btn-fit svg,#btn-fs svg{width:18px;height:18px;display:block}
                #btn-mute[data-muted='true'] .icon-volume-on,
                #btn-mute[data-muted='false'] .icon-volume-off,
                #btn-fit[data-fit='cover'] .icon-fit-contain,
                #btn-fit[data-fit='contain'] .icon-fit-cover{display:none}

                @media (max-width:640px){
                  #bar{gap:6px;padding:7px 8px calc(7px + env(safe-area-inset-bottom,0px)) 8px}
                  .pill{padding:4px 8px}
                }
              </style>
            </head>
            <body>
              <video id="v" autoplay playsinline muted></video>

              <div id="bar">
                <span class="pill"><span class="dot" id="dot"></span><span id="state">Connexion…</span></span>
                <span class="pill">
                  <span class="icon" aria-hidden="true">
                    <svg viewBox="0 0 24 24"><path d="M2 12s3.5-6 10-6 10 6 10 6-3.5 6-10 6-10-6-10-6Z"/><circle cx="12" cy="12" r="3"/></svg>
                  </span>
                  <span id="viewers">—</span>
                </span>
                <span class="pill">
                  <span class="icon" aria-hidden="true">
                    <svg viewBox="0 0 24 24"><path d="M12 4v16"/><path d="M7 8.5a6.5 6.5 0 0 0 0 7"/><path d="M17 8.5a6.5 6.5 0 0 1 0 7"/><path d="M3.5 5.5a10.5 10.5 0 0 0 0 13"/><path d="M20.5 5.5a10.5 10.5 0 0 1 0 13"/></svg>
                  </span>
                  <span>WebRTC</span>
                </span>
                <span class="pill meta">
                  <span class="icon" aria-hidden="true">
                    <svg viewBox="0 0 24 24"><circle cx="12" cy="12" r="8"/><path d="M12 7v5l3 2"/></svg>
                  </span>
                  <span id="clock">--:--</span>
                </span>
                <span class="pill meta">
                  <span class="icon" aria-hidden="true">
                    <svg viewBox="0 0 24 24"><rect x="4" y="5" width="16" height="14" rx="2"/><path d="M9 9h6"/><path d="M9 15h6"/></svg>
                  </span>
                  <span id="resolution">--</span>
                </span>
                <span class="pill meta" id="battery-pill">
                  <span class="icon" aria-hidden="true">
                    <svg viewBox="0 0 24 24"><rect x="2" y="7" width="18" height="10" rx="2"/><path d="M22 11v2"/><rect id="bat-fill" x="4" y="9" width="10" height="6" rx="1"/></svg>
                  </span>
                  <span id="battery-text">--%</span>
                </span>
                <button id="btn-mute" title="Activer le son" data-muted="true" aria-label="Activer le son">
                  <svg class="icon-volume-on" viewBox="0 0 24 24" aria-hidden="true"><path d="M5 9h4l5-4v14l-5-4H5z"/><path d="M17 9a4 4 0 0 1 0 6"/><path d="M19.5 6.5a7.5 7.5 0 0 1 0 11"/></svg>
                  <svg class="icon-volume-off" viewBox="0 0 24 24" aria-hidden="true"><path d="M5 9h4l5-4v14l-5-4H5z"/><path d="M17 9l4 6"/><path d="M21 9l-4 6"/></svg>
                </button>
                <button id="btn-fit" title="Basculer fill/fit" data-fit="cover" aria-label="Passer en fit">
                  <svg class="icon-fit-cover" viewBox="0 0 24 24" aria-hidden="true"><rect x="4" y="5" width="16" height="14" rx="2"/><path d="M9 10h6v4H9z"/></svg>
                  <svg class="icon-fit-contain" viewBox="0 0 24 24" aria-hidden="true"><rect x="4" y="5" width="16" height="14" rx="2"/><path d="M7 9h10v6H7z"/></svg>
                </button>
                <button id="btn-fs" title="Plein écran" aria-label="Plein écran">
                  <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M8 4H4v4"/><path d="M16 4h4v4"/><path d="M20 16v4h-4"/><path d="M4 16v4h4"/></svg>
                </button>
              </div>

              <script>
                const stateEl   = document.getElementById('state');
                const viewersEl = document.getElementById('viewers');
                const videoEl   = document.getElementById('v');
                const dotEl     = document.getElementById('dot');
                const bar       = document.getElementById('bar');
                const clockEl   = document.getElementById('clock');
                const resolutionEl = document.getElementById('resolution');
                const batteryEl = document.getElementById('battery-text');
                const batteryPill = document.getElementById('battery-pill');
                const muteBtn   = document.getElementById('btn-mute');
                const fitBtn    = document.getElementById('btn-fit');
                let pc = null;
                let retryDelay  = 2000;
                let hideTimer   = null;
                let viewersTimer = null;
                let clockTimer  = null;
                let reconnectTimer = null;
                let batteryTimer = null;
                let fitMode = localStorage.getItem('viewer-fit-mode') || 'cover';

                function updateMuteButton() {
                  muteBtn.dataset.muted = String(videoEl.muted);
                  muteBtn.title = videoEl.muted ? 'Activer le son' : 'Couper le son';
                  muteBtn.setAttribute('aria-label', muteBtn.title);
                }

                function applyFitMode() {
                  videoEl.style.objectFit = fitMode;
                  fitBtn.dataset.fit = fitMode;
                  fitBtn.title = fitMode === 'cover' ? 'Passer en fit' : 'Passer en fill';
                  fitBtn.setAttribute('aria-label', fitBtn.title);
                }
                function getVideoOrientationKind() {
                  const width = videoEl.videoWidth || 0;
                  const height = videoEl.videoHeight || 0;
                  if (!width || !height) return 'unknown';
                  return width >= height ? 'landscape' : 'portrait';
                }

                function getScreenOrientationKind() {
                  return window.matchMedia('(orientation: portrait)').matches
                    ? 'portrait'
                    : 'landscape';
                }

                function applyOrientationFallback() {
                  const videoKind = getVideoOrientationKind();
                  const screenKind = getScreenOrientationKind();
                  const forceLandscape = screenKind === 'landscape' && videoKind === 'portrait';
                  videoEl.classList.toggle('force-landscape', forceLandscape);
                }

                function updateClock() {
                  const now = new Date();
                  clockEl.textContent = now.toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' });
                }

                function ensureClockTimer() {
                  if (clockTimer) return;
                  updateClock();
                  clockTimer = setInterval(updateClock, 1000);
                }

                function updateResolution() {
                  const width = videoEl.videoWidth || 0;
                  const height = videoEl.videoHeight || 0;
                  resolutionEl.textContent = width > 0 && height > 0 ? `${'$'}{width}×${'$'}{height}` : '--';
                  applyOrientationFallback();
                }

                function scheduleReconnect() {
                  if (reconnectTimer) return;
                  stateEl.textContent = 'Reconnexion ' + (retryDelay / 1000) + 's…';
                  reconnectTimer = setTimeout(() => {
                    reconnectTimer = null;
                    connect();
                  }, retryDelay);
                  retryDelay = Math.min(retryDelay * 2, 15000);
                }

                fitBtn.addEventListener('click', () => {
                  fitMode = fitMode === 'cover' ? 'contain' : 'cover';
                  localStorage.setItem('viewer-fit-mode', fitMode);
                  applyFitMode();
                  showBar();
                });

                muteBtn.addEventListener('click', () => {
                  videoEl.muted = !videoEl.muted;
                  updateMuteButton();
                  showBar();
                });

                applyFitMode();
                updateMuteButton();
                ensureClockTimer();

                /* ── Plein écran ── */
                document.getElementById('btn-fs').addEventListener('click', () => {
                  if (!document.fullscreenElement) document.documentElement.requestFullscreen().catch(()=>{});
                  else document.exitFullscreen();
                });

                /* ── Auto-hide barre après 4 s d'inactivité ── */
                function showBar() {
                  bar.classList.remove('hide');
                  clearTimeout(hideTimer);
                  hideTimer = setTimeout(() => bar.classList.add('hide'), 4000);
                }
                document.addEventListener('pointermove', showBar);
                document.addEventListener('pointerdown', showBar);
                document.addEventListener('keydown',     showBar);
                videoEl.addEventListener('loadedmetadata', updateResolution);
                videoEl.addEventListener('resize', updateResolution);
                window.addEventListener('orientationchange', applyOrientationFallback);
                window.addEventListener('resize', applyOrientationFallback);
                showBar();

                /* ── Etat dot ── */
                function setDot(state) {
                  dotEl.className = 'dot ' + (state === 'connected' ? 'ok' : state === 'connecting' ? 'warn' : 'err');
                }

                /* ── Viewers ── */
                async function fetchViewers() {
                  try {
                    const d = await (await fetch('/status')).json();
                    if (d.viewers !== undefined) viewersEl.textContent = d.viewers;
                  } catch(_) {}
                }

                /* ── Batterie ── */
                async function fetchBattery() {
                  try {
                    const b = await (await fetch('/api/battery')).json();
                    if (!b || b.ok === false || typeof b.levelPercent !== 'number') {
                      batteryEl.textContent = '--%';
                      batteryPill.style.color = '';
                      return;
                    }
                    const parts = [b.levelPercent + '%'];
                    if (typeof b.temperatureC === 'number') parts.push(Math.round(b.temperatureC) + '°C');
                    if (b.isCharging) parts.push('⚡');
                    batteryEl.textContent = parts.join(' ');
                    const fill = document.getElementById('bat-fill');
                    const w = Math.round((b.levelPercent / 100) * 10);
                    if (fill) fill.setAttribute('width', String(w));
                    if (b.levelPercent < 20) batteryPill.style.color = '#ef4444';
                    else if (b.levelPercent < 50) batteryPill.style.color = '#f59e0b';
                    else batteryPill.style.color = '#22c55e';
                  } catch(_) {
                    batteryEl.textContent = '--%';
                  }
                }

                /* ── Connexion WebRTC ── */
                async function connect() {
                  if (reconnectTimer) {
                    clearTimeout(reconnectTimer);
                    reconnectTimer = null;
                  }
                  if (pc) { try { pc.close(); } catch(_){} }
                  pc = new RTCPeerConnection({ iceServers: [{ urls: 'stun:stun.l.google.com:19302' }] });

                  const waitIce = () => new Promise(resolve => {
                    if (pc.iceGatheringState === 'complete') { resolve(); return; }
                    const fn = () => { if (pc.iceGatheringState === 'complete') { pc.removeEventListener('icegatheringstatechange', fn); resolve(); } };
                    pc.addEventListener('icegatheringstatechange', fn);
                    setTimeout(() => { pc.removeEventListener('icegatheringstatechange', fn); resolve(); }, 3000);
                  });

                  pc.ontrack = e => {
                    if (e.streams?.[0]) {
                      videoEl.srcObject = e.streams[0];
                      setTimeout(applyOrientationFallback, 150);
                    }
                  };

                  pc.onconnectionstatechange = () => {
                    const s = pc.connectionState;
                    setDot(s);
                    if (s === 'connected') {
                      stateEl.textContent = 'Connecté';
                      retryDelay = 2000;
                      fetchViewers();
                      if (!viewersTimer) viewersTimer = setInterval(fetchViewers, 10000);
                      updateResolution();
                    } else if (s === 'connecting') {
                      stateEl.textContent = 'Connexion…';
                    } else if (s === 'failed' || s === 'disconnected' || s === 'closed') {
                      scheduleReconnect();
                    }
                  };

                  try {
                    pc.addTransceiver('video', { direction: 'recvonly' });
                    const offer = await pc.createOffer();
                    await pc.setLocalDescription(offer);
                    await waitIce();

                    const res  = await fetch('/api/webrtc/offer', {
                      method: 'POST',
                      headers: { 'Content-Type': 'application/json' },
                      body: JSON.stringify({ offerSdp: pc.localDescription.sdp })
                    });
                    const data = await res.json();
                    if (!res.ok || !data.answerSdp) throw new Error(data.error || 'SDP invalide');
                    await pc.setRemoteDescription({ type: data.answerType || 'answer', sdp: data.answerSdp });
                  } catch (err) {
                    setDot('err');
                    stateEl.textContent = 'Erreur: ' + (err.message || err);
                    scheduleReconnect();
                  }
                }


                connect();
                fetchBattery();
                if (!batteryTimer) batteryTimer = setInterval(fetchBattery, 5000);
              </script>
            </body>
            </html>
        """.trimIndent()
    }
}
