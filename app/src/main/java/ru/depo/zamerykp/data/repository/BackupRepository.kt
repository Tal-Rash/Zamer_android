package ru.depo.zamerykp.data.repository

import androidx.room.withTransaction
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ru.depo.zamerykp.data.db.AppDatabase
import ru.depo.zamerykp.data.db.AppSettingsEntity
import ru.depo.zamerykp.data.db.LocomotiveEntity
import ru.depo.zamerykp.data.db.MeasurementSessionEntity
import ru.depo.zamerykp.data.db.WheelPairProfileEntity
import ru.depo.zamerykp.data.db.WheelSideMeasurementEntity
import ru.depo.zamerykp.domain.BackupLocomotiveDto
import ru.depo.zamerykp.domain.BackupMeasurementSessionDto
import ru.depo.zamerykp.domain.BackupSettingsDto
import ru.depo.zamerykp.domain.BackupWheelPairProfileDto
import ru.depo.zamerykp.domain.BackupWheelSideMeasurementDto
import ru.depo.zamerykp.domain.FullBackupExportDto
import ru.depo.zamerykp.domain.MeasurementSource
import ru.depo.zamerykp.domain.MeasurementStatus
import ru.depo.zamerykp.domain.SentStatus
import ru.depo.zamerykp.domain.WheelSide
import java.time.OffsetDateTime

class BackupRepository(private val db: AppDatabase) {
    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    suspend fun exportBackupJson(): String = json.encodeToString(buildBackup())

    suspend fun buildBackup(): FullBackupExportDto {
        val locomotives = db.locomotiveDao().getAll().map { it.toDto() }
        val wheelPairProfiles = db.wheelPairProfileDao().getAll().map { it.toDto() }
        val sessions = db.measurementDao().getAllSessions().map { it.toDto() }
        val sides = db.measurementDao().getAllSideMeasurements().map { it.toDto() }
        val settings = (db.settingsDao().get() ?: AppSettingsEntity()).toDto()
        return FullBackupExportDto(
            exportedAt = OffsetDateTime.now().toString(),
            locomotives = locomotives,
            wheelPairProfiles = wheelPairProfiles,
            measurementSessions = sessions,
            wheelSideMeasurements = sides,
            settings = settings,
        )
    }

    suspend fun restoreBackupJson(text: String): FullBackupExportDto {
        val backup = json.decodeFromString<FullBackupExportDto>(text)
        require(backup.exportType == "fullBackup") { "Выбран не файл резервной копии" }
        db.withTransaction {
            db.clearAllTables()
            backup.locomotives.forEach { db.locomotiveDao().upsert(it.toEntity()) }
            if (backup.wheelPairProfiles.isNotEmpty()) {
                db.wheelPairProfileDao().upsertAll(backup.wheelPairProfiles.map { it.toEntity() })
            }
            backup.measurementSessions.forEach { db.measurementDao().upsertSession(it.toEntity()) }
            backup.wheelSideMeasurements.forEach { db.measurementDao().upsertSide(it.toEntity()) }
            db.settingsDao().upsert(backup.settings.toEntity())
        }
        return backup
    }
}

private fun LocomotiveEntity.toDto() = BackupLocomotiveDto(
    id = id,
    series = series,
    number = number,
    wheelPairCount = wheelPairCount,
    comment = comment,
    createdOnPhone = createdOnPhone,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
)

private fun WheelPairProfileEntity.toDto() = BackupWheelPairProfileDto(
    locomotiveId = locomotiveId,
    number = number,
    axisNumber = axisNumber,
    kcDiameterLeft = kcDiameterLeft,
    kcDiameterRight = kcDiameterRight,
)

private fun MeasurementSessionEntity.toDto() = BackupMeasurementSessionDto(
    id = id,
    locomotiveId = locomotiveId,
    measurementDate = measurementDate,
    repairType = repairType,
    status = status.name,
    source = source.name,
    archivePayload = archivePayload,
    sentStatus = sentStatus.name,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

private fun WheelSideMeasurementEntity.toDto() = BackupWheelSideMeasurementDto(
    sessionId = sessionId,
    wheelPairNumber = wheelPairNumber,
    side = side.name,
    flangeThickness = flangeThickness,
    flangeWear = flangeWear,
    flangeSteepness = flangeSteepness,
    bandageThickness = bandageThickness,
    bandageDiameter = bandageDiameter,
    updatedAt = updatedAt,
)

private fun AppSettingsEntity.toDto() = BackupSettingsDto(
    defaultEmail = defaultEmail,
    voiceConfirmLowConfidence = voiceConfirmLowConfidence,
    keepVoiceServiceEnabled = keepVoiceServiceEnabled,
    keepScreenOn = keepScreenOn,
)

private fun BackupLocomotiveDto.toEntity() = LocomotiveEntity(
    id = id,
    series = series,
    number = number,
    wheelPairCount = wheelPairCount,
    comment = comment,
    createdOnPhone = createdOnPhone,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
)

private fun BackupWheelPairProfileDto.toEntity() = WheelPairProfileEntity(
    locomotiveId = locomotiveId,
    number = number,
    axisNumber = axisNumber,
    kcDiameterLeft = kcDiameterLeft,
    kcDiameterRight = kcDiameterRight,
)

private fun BackupMeasurementSessionDto.toEntity() = MeasurementSessionEntity(
    id = id,
    locomotiveId = locomotiveId,
    measurementDate = measurementDate,
    repairType = repairType,
    status = MeasurementStatus.valueOf(status),
    source = MeasurementSource.valueOf(source),
    archivePayload = archivePayload,
    sentStatus = SentStatus.valueOf(sentStatus),
    createdAt = createdAt,
    updatedAt = updatedAt,
)

private fun BackupWheelSideMeasurementDto.toEntity() = WheelSideMeasurementEntity(
    sessionId = sessionId,
    wheelPairNumber = wheelPairNumber,
    side = WheelSide.valueOf(side),
    flangeThickness = flangeThickness,
    flangeWear = flangeWear,
    flangeSteepness = flangeSteepness,
    bandageThickness = bandageThickness,
    bandageDiameter = bandageDiameter,
    updatedAt = updatedAt,
)

private fun BackupSettingsDto.toEntity() = AppSettingsEntity(
    defaultEmail = defaultEmail,
    voiceConfirmLowConfidence = voiceConfirmLowConfidence,
    keepVoiceServiceEnabled = keepVoiceServiceEnabled,
    keepScreenOn = keepScreenOn,
)
