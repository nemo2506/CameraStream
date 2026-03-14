package com.miseservice.camerastream.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import java.net.InetAddress
import java.net.NetworkInterface

object NetworkUtils {

    /**
     * Récupère l'adresse IP locale du téléphone via WiFi
     * Essaie plusieurs méthodes pour assurer la compatibilité
     */
    fun getLocalIpAddress(context: Context): String? {
        return try {
            // Méthode 1: Via NetworkInterface (plus robuste)
            val ip = getIpAddressFromNetworkInterface()
            if (ip != null) {
                return ip
            }

            // Méthode 2: Via WifiManager (fallback)
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            if (wifiManager != null) {
                @Suppress("DEPRECATION")
                val connectionInfo = wifiManager.connectionInfo
                if (connectionInfo != null && connectionInfo.ipAddress != 0) {
                    val ipAddress = connectionInfo.ipAddress
                    return String.format(
                        "%d.%d.%d.%d",
                        ipAddress and 0xff,
                        (ipAddress shr 8) and 0xff,
                        (ipAddress shr 16) and 0xff,
                        (ipAddress shr 24) and 0xff
                    )
                }
            }

            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Détecte l'IP via les interfaces réseau disponibles
     * Méthode la plus robuste, fonctionne même si WifiManager échoue
     */
    private fun getIpAddressFromNetworkInterface(): String? {
        return try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            for (networkInterface in interfaces) {
                // Chercher wlan0 (interface WiFi)
                if (!networkInterface.isUp) {
                    continue
                }

                // Préférer les interfaces wlan ou eth
                val name = networkInterface.name.lowercase()
                if (!name.startsWith("wlan") && !name.startsWith("eth") && !name.startsWith("wifi")) {
                    continue
                }

                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress && address is InetAddress) {
                        val hostAddress = address.hostAddress
                        // Vérifier que c'est une adresse IPv4
                        if (hostAddress != null && !hostAddress.contains(":") && hostAddress.contains(".")) {
                            return hostAddress
                        }
                    }
                }
            }
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Vérifie si le téléphone est connecté à un réseau WiFi
     * Utilise ConnectivityManager pour vérification plus robuste
     */
    fun isWifiConnected(context: Context): Boolean {
        return try {
            // Méthode moderne (API 29+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                if (connectivityManager != null) {
                    val network = connectivityManager.activeNetwork
                    if (network != null) {
                        val capabilities = connectivityManager.getNetworkCapabilities(network)
                        if (capabilities != null) {
                            return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                        }
                    }
                }
            }

            // Fallback pour API < 29
            @Suppress("DEPRECATION")
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            if (connectivityManager != null) {
                @Suppress("DEPRECATION")
                val networkInfo = connectivityManager.activeNetworkInfo
                if (networkInfo != null && networkInfo.isConnected) {
                    @Suppress("DEPRECATION")
                    return networkInfo.type == ConnectivityManager.TYPE_WIFI
                }
            }

            // Fallback final: vérifier via WifiManager
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            if (wifiManager != null) {
                @Suppress("DEPRECATION")
                val connectionInfo = wifiManager.connectionInfo
                return connectionInfo != null && connectionInfo.networkId != -1 && wifiManager.isWifiEnabled
            }

            false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Nettoie un SSID brut (retire les guillemets, <unknown ssid>, etc.)
     */
    private fun cleanSsid(raw: String?): String? {
        if (raw.isNullOrEmpty()) return null
        val cleaned = raw.trim()
            .replace("\"", "")
            .replace("<unknown ssid>", "")
            .trim()
        return if (cleaned.isNotEmpty()) cleaned else null
    }

    /**
     * Récupère le nom du réseau WiFi connecté.
     * - Android 10+ (API 29+) : utilise ConnectivityManager + WifiInfo.getSSID()
     *   (requiert ACCESS_FINE_LOCATION ET que les services de localisation soient activés)
     * - Fallback : WifiManager.getConnectionInfo() (déprécié API 31+)
     */
    fun getWifiNetworkName(context: Context): String? {
        return try {
            // ── Méthode moderne (API 29+) ──────────────────────────────────────────
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val connectivityManager =
                    context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                if (connectivityManager != null) {
                    val network = connectivityManager.activeNetwork
                    if (network != null) {
                        val capabilities = connectivityManager.getNetworkCapabilities(network)
                        if (capabilities != null &&
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                        ) {
                            val wifiInfo =
                                capabilities.transportInfo as? android.net.wifi.WifiInfo
                            val ssid = cleanSsid(wifiInfo?.ssid)
                            if (ssid != null) return ssid
                        }
                    }
                }
            }

            // ── Fallback WifiManager (API < 29 ou méthode moderne indisponible) ───
            val wifiManager =
                context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            if (wifiManager != null) {
                @Suppress("DEPRECATION")
                val connectionInfo = wifiManager.connectionInfo
                val ssid = cleanSsid(connectionInfo?.ssid)
                if (ssid != null) return ssid
            }

            android.util.Log.w(
                "NetworkUtils",
                "SSID indisponible – vérifiez que les services de localisation sont activés"
            )
            null
        } catch (e: Exception) {
            android.util.Log.e("NetworkUtils", "Erreur récupération SSID: ${e.message}", e)
            null
        }
    }

    /**
     * Formate l'URL du streaming avec l'IP détectée
     */
    fun getStreamingUrl(context: Context, port: Int = 8080): String? {
        val ipAddress = getLocalIpAddress(context) ?: return null
        return "http://$ipAddress:$port/viewer"
    }

    /**
     * Formate l'URL du statut avec l'IP détectée
     */
    fun getStatusUrl(context: Context, port: Int = 8080): String? {
        val ipAddress = getLocalIpAddress(context) ?: return null
        return "http://$ipAddress:$port/status"
    }
}

