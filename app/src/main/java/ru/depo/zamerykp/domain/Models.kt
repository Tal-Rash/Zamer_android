package ru.depo.zamerykp.domain

enum class WheelSide(val label: String) {
    LEFT("левая"),
    RIGHT("правая");

    fun next(): WheelSide = if (this == LEFT) RIGHT else LEFT
}

enum class MeasurementStatus {
    DRAFT,
    FINISHED
}

enum class MeasurementSource(val label: String) {
    PHONE("с телефона"),
    IMPORTED("из ПК")
}

enum class SentStatus(val label: String) {
    NOT_SENT("не отправлено"),
    EXPORTED("файл создан"),
    SENT("отправлено")
}

data class SideMeasurements(
    val flangeThickness: Double? = null,
    val flangeWear: Double? = null,
    val flangeSteepness: Double? = null,
    val bandageThickness: Double? = null,
    val bandageDiameter: Double? = null,
) {
    val isFilled: Boolean
        get() = flangeThickness != null &&
            flangeWear != null &&
            flangeSteepness != null &&
            bandageThickness != null
}

data class WheelPairMeasurements(
    val number: Int,
    val left: SideMeasurements = SideMeasurements(),
    val right: SideMeasurements = SideMeasurements(),
) {
    val isFilled: Boolean
        get() = left.isFilled && right.isFilled
}

data class ArchiveItem(
    val measurementId: String,
    val measurementDate: String,
    val locomotiveTitle: String,
    val repairType: String,
    val filledWheelPairs: Int,
    val sentStatus: SentStatus,
    val source: MeasurementSource,
    val canDelete: Boolean,
)

data class RepairDateItem(
    val repairType: String,
    val date: String? = null,
    val sourceLabel: String? = null,
)
