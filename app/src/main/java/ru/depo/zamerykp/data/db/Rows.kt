package ru.depo.zamerykp.data.db

import ru.depo.zamerykp.domain.MeasurementSource
import ru.depo.zamerykp.domain.MeasurementStatus
import ru.depo.zamerykp.domain.SentStatus

data class SessionWithLocomotiveRow(
    val measurementId: String,
    val measurementDate: String,
    val repairType: String,
    val source: MeasurementSource,
    val sentStatus: SentStatus,
    val series: String,
    val number: String,
    val wheelPairCount: Int,
    val filledWheelPairs: Int,
)

data class PendingMeasurementRow(
    val measurementId: String,
    val measurementDate: String,
    val repairType: String,
    val status: MeasurementStatus,
    val sentStatus: SentStatus,
    val series: String,
    val number: String,
)
