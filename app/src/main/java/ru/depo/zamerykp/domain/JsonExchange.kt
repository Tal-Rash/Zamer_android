package ru.depo.zamerykp.domain

import kotlinx.serialization.Serializable

@Serializable
data class MeasurementExportDto(
    val formatVersion: Int = 1,
    val createdAt: String,
    val measurementId: String,
    val locomotive: LocomotiveExportDto,
    val repairType: String,
    val measurementDate: String,
    val wheelPairs: List<WheelPairExportDto>,
)

sealed interface ImportPayload {
    data class Measurement(val dto: MeasurementExportDto) : ImportPayload
    data class ReferenceData(val dto: ReferenceDataExportDto) : ImportPayload
    data class ArchiveData(val dto: ArchiveDataExportDto) : ImportPayload
}

data class ImportEnvelope(
    val measurement: MeasurementExportDto? = null,
    val referenceData: ReferenceDataExportDto? = null,
    val archiveData: ArchiveDataExportDto? = null,
)

@Serializable
data class ReferenceDataExportDto(
    val formatVersion: Int = 2,
    val exportType: String = "referenceData",
    val exportedAt: String,
    val locomotives: List<ReferenceLocomotiveExportDto>,
)

@Serializable
data class ArchiveDataExportDto(
    val formatVersion: Int = 1,
    val exportType: String = "archiveData",
    val exportedAt: String,
    val archive: List<MeasurementExportDto>,
)

@Serializable
data class FullBackupExportDto(
    val formatVersion: Int = 1,
    val exportType: String = "fullBackup",
    val exportedAt: String,
    val locomotives: List<BackupLocomotiveDto>,
    val wheelPairProfiles: List<BackupWheelPairProfileDto>,
    val measurementSessions: List<BackupMeasurementSessionDto>,
    val wheelSideMeasurements: List<BackupWheelSideMeasurementDto>,
    val settings: BackupSettingsDto,
)

fun FullBackupExportDto.suggestedFileName(): String =
    "zamery_kp_backup_${exportedAt.take(10)}.json"

fun ArchiveDataExportDto.suggestedFileName(): String =
    "zamery_kp_archive_${exportedAt.take(10)}.json"

@Serializable
data class LocomotiveExportDto(
    val series: String,
    val number: String,
    val wheelPairCount: Int,
    val comment: String = "",
    val isNew: Boolean = false,
    val deletedAt: Long = 0L,
)

@Serializable
data class ReferenceLocomotiveExportDto(
    val series: String,
    val number: String,
    val wheelPairCount: Int,
    val sortOrder: Long = 0L,
    val updatedAt: Long = 0L,
    val deletedAt: Long = 0L,
    val wheelPairs: List<ReferenceWheelPairExportDto> = emptyList(),
)

@Serializable
data class ReferenceWheelPairExportDto(
    val number: Int,
    val axisNumber: Int = number,
    val diameterLeft: Double? = null,
    val diameterRight: Double? = null,
)

@Serializable
data class WheelPairExportDto(
    val number: Int,
    val left: SideExportDto = SideExportDto(),
    val right: SideExportDto = SideExportDto(),
)

@Serializable
data class SideExportDto(
    val flangeThickness: Double? = null,
    val flangeWear: Double? = null,
    val flangeSteepness: Double? = null,
    val bandageThickness: Double? = null,
    val bandageDiameter: Double? = null,
)

@Serializable
data class BackupLocomotiveDto(
    val id: Long,
    val series: String,
    val number: String,
    val wheelPairCount: Int,
    val comment: String = "",
    val createdOnPhone: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long = 0L,
    val sortOrder: Long = 0L,
)

@Serializable
data class BackupWheelPairProfileDto(
    val locomotiveId: Long,
    val number: Int,
    val axisNumber: Int,
    val kcDiameterLeft: Double? = null,
    val kcDiameterRight: Double? = null,
)

@Serializable
data class BackupMeasurementSessionDto(
    val id: String,
    val locomotiveId: Long,
    val measurementDate: String,
    val repairType: String,
    val status: String,
    val source: String,
    val archivePayload: Boolean = false,
    val sentStatus: String,
    val createdAt: Long,
    val updatedAt: Long,
)

@Serializable
data class BackupWheelSideMeasurementDto(
    val sessionId: String,
    val wheelPairNumber: Int,
    val side: String,
    val flangeThickness: Double? = null,
    val flangeWear: Double? = null,
    val flangeSteepness: Double? = null,
    val bandageThickness: Double? = null,
    val bandageDiameter: Double? = null,
    val updatedAt: Long,
)

@Serializable
data class BackupSettingsDto(
    val defaultEmail: String = "",
    val voiceConfirmLowConfidence: Boolean = true,
    val keepVoiceServiceEnabled: Boolean = true,
    val keepScreenOn: Boolean = true,
)

fun MeasurementExportDto.suggestedFileName(): String {
    val safeSeries = locomotive.series.toFilePart()
    val safeNumber = locomotive.number.toFilePart()
    val shortId = measurementId.take(8)
    return "zamery_kp_${safeSeries}_${safeNumber}_${measurementDate}_$shortId.json"
}

private fun String.toFilePart(): String =
    trim().ifBlank { "unknown" }.replace(Regex("[^A-Za-zА-Яа-я0-9_-]+"), "_")
