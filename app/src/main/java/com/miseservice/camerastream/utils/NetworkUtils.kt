package com.miseservice.camerastream.utils

import android.content.Context
import android.net.wifi.WifiManager
import java.net.InetAddress
import java.net.NetworkInterface

object NetworkUtils {

    /**
     * Récupère l'adresse IP locale du téléphone via WiFi
     * Essaie plusieurs méthodes pour assurer la compatibilité
     */
    fun getLocalIpAddress(context: Context): String? {
        return try {
            // Méthode 1: Via WifiManager (la plus fiable)
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
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

            // Méthode 2: Via NetworkInterface (fallback)
            getIpAddressFromNetworkInterface()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Détecte l'IP via les interfaces réseau disponibles
     */
    private fun getIpAddressFromNetworkInterface(): String? {
        return try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            for (networkInterface in interfaces) {
                // Ignorer les interfaces boucles et inactives
                if (networkInterface.isLoopback || !networkInterface.isUp) {
                    continue
                }

                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress && address is InetAddress) {
                        val hostAddress = address.hostAddress
                        // Vérifier que c'est une adresse IPv4
                        if (hostAddress != null && !hostAddress.contains(":")) {
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
     */
    fun isWifiConnected(context: Context): Boolean {
        return try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            @Suppress("DEPRECATION")
            val connectionInfo = wifiManager.connectionInfo
            connectionInfo != null && connectionInfo.networkId != -1
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Récupère le nom du réseau WiFi connecté
     */
    fun getWifiNetworkName(context: Context): String? {
        return try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            @Suppress("DEPRECATION")
            val connectionInfo = wifiManager.connectionInfo
            connectionInfo?.ssid?.replace("\"", "")
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Formate l'URL du streaming avec l'IP détectée
     */
    fun getStreamingUrl(context: Context, port: Int = 8080): String? {
        val ipAddress = getLocalIpAddress(context) ?: return null
        return "http://$ipAddress:$port/stream"
    }

    /**
     * Formate l'URL du statut avec l'IP détectée
     */
    fun getStatusUrl(context: Context, port: Int = 8080): String? {
        val ipAddress = getLocalIpAddress(context) ?: return null
        return "http://$ipAddress:$port/status"
    }
}

