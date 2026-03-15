package com.miseservice.camerastream.domain.usecase

import com.miseservice.camerastream.domain.model.BatteryInfo
import com.miseservice.camerastream.domain.repository.BatteryRepository
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class FetchBatteryInfoUseCase @Inject constructor(
    private val batteryRepository: BatteryRepository
) {
    val batteryInfoFlow: StateFlow<BatteryInfo?> get() = batteryRepository.batteryInfo
    operator fun invoke(): BatteryInfo? = batteryRepository.getCurrentBatteryInfo()
}

