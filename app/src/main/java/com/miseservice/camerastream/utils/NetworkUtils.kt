package com.miseservice.camerastream.utils

import android.content.Context
import android.net.wifi.WifiManager
import java.net.InetAddress

object NetworkUtils {
    fun getLocalIpAddress(context: Context): String? {
        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val wifiInfo = wifiManager.connectionInfo

            if (wifiInfo == null || !wifiManager.isWifiEnabled) {
                return null
            }

            val ipAddress = wifiInfo.ipAddress
            return InetAddress.getByAddress(
                byteArrayOf(
                    (ipAddress and 0xff).toByte(),
                    ((ipAddress shr 8) and 0xff).toByte(),
                    ((ipAddress shr 16) and 0xff).toByte(),
                    ((ipAddress shr 24) and 0xff).toByte()
                )
            ).hostAddress
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun getStreamingUrl(context: Context, port: Int = 8080): String? {
        val ip = getLocalIpAddress(context) ?: return null
        return "http://$ip:$port/stream"
    }
}

