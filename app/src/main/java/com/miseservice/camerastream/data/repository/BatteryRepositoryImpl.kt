package com.miseservice.camerastream.data.repository

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.miseservice.camerastream.domain.model.BatteryInfo
import com.miseservice.camerastream.domain.repository.BatteryRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BatteryRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : BatteryRepository {

    private val _batteryInfo = MutableStateFlow<BatteryInfo?>(null)
    override val batteryInfo: StateFlow<BatteryInfo?> = _batteryInfo

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            _batteryInfo.value = parseBatteryIntent(intent)
        }
    }

    init {
        val stickyIntent = context.registerReceiver(
            receiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        _batteryInfo.value = parseBatteryIntent(stickyIntent)
    }

    override fun getCurrentBatteryInfo(): BatteryInfo? = _batteryInfo.value

    private fun parseBatteryIntent(intent: Intent?): BatteryInfo? {
        intent ?: return null
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        if (level < 0 || scale <= 0) return null

        val percent = ((level * 100f) / scale.toFloat()).toInt().coerceIn(0, 100)
        val statusCode = intent.getIntExtra(
            BatteryManager.EXTRA_STATUS,
            BatteryManager.BATTERY_STATUS_UNKNOWN
        )
        val isCharging = statusCode == BatteryManager.BATTERY_STATUS_CHARGING ||
            statusCode == BatteryManager.BATTERY_STATUS_FULL
        val statusLabel = when (statusCode) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "En charge"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "Décharge"
            BatteryManager.BATTERY_STATUS_FULL -> "Pleine"
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Branchée"
            else -> "Indisponible"
        }
        val rawTemperature = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
        val temperatureC = rawTemperature.takeIf { it != Int.MIN_VALUE }?.let { it / 10f }

        return BatteryInfo(
            levelPercent = percent,
            isCharging = isCharging,
            status = statusLabel,
            temperatureC = temperatureC,
            timestampMs = System.currentTimeMillis()
        )
    }
}

