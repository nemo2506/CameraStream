package com.miseservice.camerastream.domain.repository

import com.miseservice.camerastream.domain.model.BatteryInfo
import kotlinx.coroutines.flow.StateFlow

interface BatteryRepository {
    val batteryInfo: StateFlow<BatteryInfo?>
    fun getCurrentBatteryInfo(): BatteryInfo?
}

