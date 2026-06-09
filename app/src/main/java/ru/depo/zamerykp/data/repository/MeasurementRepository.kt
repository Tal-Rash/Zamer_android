package ru.depo.zamerykp.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.depo.zamerykp.data.db.LocomotiveDao
import ru.depo.zamerykp.data.db.MeasurementDao
import ru.depo.zamerykp.data.db.PendingMeasurementRow
import ru.depo.zamerykp.data.db.WheelPairProfileDao
import ru.depo.zamerykp.data.db.MeasurementSessionEntity
import ru.depo.zamerykp.data.db.WheelSideMeasurementEntity
import ru.depo.zamerykp.domain.ArchiveItem
import ru.depo.zamerykp.domain.ReferenceDataExportDto
import ru.depo.zamerykp.domain.ReferenceLocomotiveExportDto
import ru.depo.zamerykp.domain.MeasurementStatus
import ru.depo.zamerykp.domain.MeasurementSource
import ru.depo.zamerykp.domain.SentStatus
import ru.depo.zamerykp.domain.WheelSide
import ru.depo.zamerykp.domain.MeasurementExportDto
import ru.depo.zamerykp.domain.SideExportDto
import java.time.LocalDate
import java.util.UUID

class MeasurementRepository(
    private val measurementDao: MeasurementDao,
    private val locomotiveDao: LocomotiveDao,
    private val profileDao: WheelPairProfileDao,
) {
    fun observeArchive(): Flow<List<ArchiveItem>> =
        measurementDao.observeArchive().map { rows ->
            rows.map {
                ArchiveItem(
                    measurementId = it.measurementId,
                    measurementDate = it.measurementDate,
                    locomotiveTitle = "${it.series} ${it.number}",
                    repairType = it.repairType,
                    filledWheelPairs = it.filledWheelPairs,
                    sentStatus = it.sentStatus,
                    source = it.source,
                    canDelete = it.source == MeasurementSource.PHONE,
                )
            }
        }

    fun observePendingMeasurements(): Flow<List<PendingMeasurementRow>> =
        measurementDao.observePendingMeasurements()

    suspend fun getPendingMeasurements(): List<PendingMeasurementRow> =
        measurementDao.getPendingMeasurements()

    suspend fun getSyncableMeasurements(): List<PendingMeasurementRow> =
        measurementDao.getSyncableMeasurements()

    suspend fun hasPendingMeasurements(): Boolean =
        measurementDao.countPendingMeasurements() > 0

    fun observeSides(sessionId: String): Flow<List<WheelSideMeasurementEntity>> =
        measurementDao.observeSides(sessionId)

    suspend fun getLatestDraftSession(): MeasurementSessionEntity? = measurementDao.getLatestDraftSession()

    suspend fun startSession(
        locomotiveId: Long,
        measurementDate: String = LocalDate.now().toString(),
        repairType: String,
    ): String {
        val locomotive = requireNotNull(locomotiveDao.getById(locomotiveId)) { "Локомотив не найден" }
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        measurementDao.createSessionWithEmptySides(
            MeasurementSessionEntity(
                id = id,
                locomotiveId = locomotiveId,
                measurementDate = measurementDate,
                repairType = repairType.trim(),
                source = MeasurementSource.PHONE,
                createdAt = now,
                updatedAt = now,
            ),
            locomotive.wheelPairCount,
        )
        return id
    }

    suspend fun getSession(id: String): MeasurementSessionEntity? = measurementDao.getSession(id)

    suspend fun saveSideValue(
        sessionId: String,
        wheelPairNumber: Int,
        side: WheelSide,
        flangeThickness: Double?,
        flangeWear: Double?,
        flangeSteepness: Double?,
        bandageThickness: Double?,
        bandageDiameter: Double?,
    ) {
        measurementDao.upsertSide(
            WheelSideMeasurementEntity(
                sessionId = sessionId,
                wheelPairNumber = wheelPairNumber,
                side = side,
                flangeThickness = flangeThickness,
                flangeWear = flangeWear,
                flangeSteepness = flangeSteepness,
                bandageThickness = bandageThickness,
                bandageDiameter = bandageDiameter,
                updatedAt = System.currentTimeMillis(),
            )
        )
    }

    suspend fun clearWheelPair(sessionId: String, wheelPairNumber: Int) {
        measurementDao.deleteWheelPairSides(sessionId, wheelPairNumber)
    }

    suspend fun cleanupImportedLocomotives() {
        locomotiveDao.deleteOrphanImportedLocomotives()
    }

    suspend fun finishSession(id: String) {
        measurementDao.updateStatus(id, MeasurementStatus.FINISHED, System.currentTimeMillis())
    }

    suspend fun markExported(id: String) {
        measurementDao.updateSentStatus(id, SentStatus.EXPORTED, System.currentTimeMillis())
    }

    suspend fun markSent(id: String) {
        measurementDao.updateSentStatus(id, SentStatus.SENT, System.currentTimeMillis())
    }

    suspend fun deleteMeasurement(id: String): Boolean {
        val session = measurementDao.getSession(id) ?: return false
        if (session.source != MeasurementSource.PHONE) {
            return false
        }
        measurementDao.deleteSession(id)
        return true
    }

    suspend fun replaceImportedArchive() {
        measurementDao.deleteImportedArchiveSessions()
    }

    suspend fun importReferenceData(dto: ReferenceDataExportDto, importLocomotives: Boolean, importWheelPairs: Boolean): Int {
        var imported = 0
        dto.locomotives.forEach { locomotive ->
            if (importLocomotives || importWheelPairs) {
                importReferenceLocomotive(locomotive, importLocomotives, importWheelPairs)
                imported += 1
            }
        }
        return imported
    }

    suspend fun importMeasurement(
        dto: MeasurementExportDto,
        importLocomotive: Boolean,
        importWheelPairs: Boolean,
        importArchive: Boolean,
        archivePayload: Boolean = false,
        allowedArchiveLocomotives: Set<String>? = null,
    ): String {
        val locomotive = if (archivePayload) {
            ensureArchiveLocomotive(
                series = dto.locomotive.series,
                number = dto.locomotive.number,
                wheelPairCount = dto.locomotive.wheelPairCount,
                comment = dto.locomotive.comment,
                deletedAt = dto.locomotive.deletedAt,
                allowedArchiveLocomotives = allowedArchiveLocomotives,
                preserveExistingWheelPairCount = true,
            )
        } else {
            ensureImportedLocomotive(
                series = dto.locomotive.series,
                number = dto.locomotive.number,
                wheelPairCount = dto.locomotive.wheelPairCount,
                comment = dto.locomotive.comment,
                updatedAt = System.currentTimeMillis(),
                deletedAt = dto.locomotive.deletedAt,
                createdOnPhone = dto.locomotive.isNew,
                createIfMissing = importLocomotive || importArchive,
                preserveExistingWheelPairCount = true,
            )
        } ?: return dto.measurementId

        if (importWheelPairs) {
            if (archivePayload) {
                upsertArchiveProfiles(locomotive.id, dto)
            } else {
                replaceProfiles(
                    locomotive.id,
                    (1..dto.locomotive.wheelPairCount.coerceAtLeast(1)).map { number ->
                        ru.depo.zamerykp.data.db.WheelPairProfileEntity(
                            locomotiveId = locomotive.id,
                            number = number,
                            axisNumber = number,
                        )
                    }
                )
            }
        } else if (!archivePayload && (importLocomotive || importArchive)) {
            ensureBaseProfiles(locomotive.id, dto.locomotive.wheelPairCount)
        }

        if (importArchive) {
            val now = System.currentTimeMillis()
            measurementDao.upsertSession(
                MeasurementSessionEntity(
                    id = dto.measurementId,
                    locomotiveId = locomotive.id,
                    measurementDate = dto.measurementDate,
                    repairType = dto.repairType.trim(),
                    source = MeasurementSource.IMPORTED,
                    archivePayload = archivePayload,
                    status = MeasurementStatus.FINISHED,
                    sentStatus = SentStatus.NOT_SENT,
                    createdAt = now,
                    updatedAt = now,
                )
            )

            dto.wheelPairs.forEach { pair ->
                measurementDao.upsertSide(
                    pair.left.toEntity(dto.measurementId, pair.number, WheelSide.LEFT)
                )
                measurementDao.upsertSide(
                    pair.right.toEntity(dto.measurementId, pair.number, WheelSide.RIGHT)
                )
            }
        }
        return dto.measurementId
    }

    private suspend fun importReferenceLocomotive(
        dto: ReferenceLocomotiveExportDto,
        importLocomotives: Boolean,
        importWheelPairs: Boolean,
    ) {
        val locomotive = ensureImportedLocomotive(
            series = dto.series,
            number = dto.number,
            wheelPairCount = dto.wheelPairCount,
            comment = "",
            updatedAt = dto.updatedAt,
            deletedAt = dto.deletedAt,
            sortOrder = dto.sortOrder,
            createdOnPhone = false,
            createIfMissing = dto.deletedAt > 0L || importLocomotives || importWheelPairs || locomotiveDao.find(dto.series.normalizeSeries(), dto.number.normalizeNumber()) != null || locomotiveDao.findByNumber(dto.number.normalizeNumber()) != null,
            preserveExistingWheelPairCount = false,
        ) ?: return
        if (!importWheelPairs) {
            if (importLocomotives) {
                ensureBaseProfiles(locomotive.id, dto.wheelPairCount)
            }
            return
        }
        replaceProfiles(
            locomotive.id,
            (dto.wheelPairs.ifEmpty {
                (1..dto.wheelPairCount.coerceAtLeast(1)).map { number ->
                    ru.depo.zamerykp.domain.ReferenceWheelPairExportDto(number = number)
                }
            }).map { pair ->
                ru.depo.zamerykp.data.db.WheelPairProfileEntity(
                    locomotiveId = locomotive.id,
                    number = pair.number,
                    axisNumber = pair.axisNumber,
                    kcDiameterLeft = pair.diameterLeft,
                    kcDiameterRight = pair.diameterRight,
                )
            }
        )
    }

    private suspend fun ensureImportedLocomotive(
        series: String,
        number: String,
        wheelPairCount: Int,
        comment: String,
        updatedAt: Long,
        deletedAt: Long,
        sortOrder: Long = 0L,
        createdOnPhone: Boolean,
        createIfMissing: Boolean,
        preserveExistingWheelPairCount: Boolean = false,
    ): ru.depo.zamerykp.data.db.LocomotiveEntity? {
        val now = System.currentTimeMillis()
        val normalizedSeries = series.normalizeSeries()
        val normalizedNumber = number.normalizeNumber()
        val existing = locomotiveDao.find(normalizedSeries, normalizedNumber)
            ?: locomotiveDao.findByNumber(normalizedNumber)
        if (existing == null && !createIfMissing) return null
        val resolvedWheelPairCount = if (existing != null && preserveExistingWheelPairCount) {
            existing.wheelPairCount
        } else {
            wheelPairCount.coerceAtLeast(1)
        }
        val entity = ru.depo.zamerykp.data.db.LocomotiveEntity(
            id = existing?.id ?: 0L,
            series = normalizedSeries,
            number = normalizedNumber,
            wheelPairCount = resolvedWheelPairCount,
            comment = if (comment.isBlank()) existing?.comment.orEmpty() else comment.trim(),
            createdOnPhone = existing?.createdOnPhone ?: createdOnPhone,
            createdAt = existing?.createdAt ?: now,
            updatedAt = if (updatedAt > 0L) maxOf(existing?.updatedAt ?: 0L, updatedAt) else now,
            deletedAt = deletedAt,
            sortOrder = if (sortOrder > 0L) sortOrder else existing?.sortOrder ?: 0L,
        )
        if (existing == null) {
            locomotiveDao.upsert(entity)
        } else {
            locomotiveDao.update(
                id = entity.id,
                series = entity.series,
                number = entity.number,
                wheelPairCount = entity.wheelPairCount,
                comment = entity.comment,
                createdOnPhone = entity.createdOnPhone,
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt,
                deletedAt = entity.deletedAt,
                sortOrder = entity.sortOrder,
            )
        }
        val savedId = existing?.id ?: 0L
        val locomotiveId = if (savedId == -1L || savedId == 0L) {
            requireNotNull(locomotiveDao.find(normalizedSeries, normalizedNumber) ?: locomotiveDao.findByNumber(normalizedNumber)).id
        } else {
            savedId
        }
        return requireNotNull(locomotiveDao.getById(locomotiveId))
    }

    private suspend fun ensureArchiveLocomotive(
        series: String,
        number: String,
        wheelPairCount: Int,
        comment: String,
        deletedAt: Long,
        allowedArchiveLocomotives: Set<String>? = null,
        preserveExistingWheelPairCount: Boolean = false,
    ): ru.depo.zamerykp.data.db.LocomotiveEntity? {
        val now = System.currentTimeMillis()
        val normalizedSeries = series.normalizeSeries()
        val normalizedNumber = number.normalizeNumber()
        val key = archiveLocomotiveKey(normalizedSeries, normalizedNumber)
        if (allowedArchiveLocomotives != null && key !in allowedArchiveLocomotives) {
            return locomotiveDao.find(normalizedSeries, normalizedNumber)
                ?: locomotiveDao.findByNumber(normalizedNumber)
        }
        val existing = locomotiveDao.find(normalizedSeries, normalizedNumber)
            ?: locomotiveDao.findByNumber(normalizedNumber)
        val entity = ru.depo.zamerykp.data.db.LocomotiveEntity(
            id = existing?.id ?: 0L,
            series = if (existing?.series.isNullOrBlank()) normalizedSeries else existing.series.normalizeSeries(),
            number = normalizedNumber,
            wheelPairCount = if (existing != null && preserveExistingWheelPairCount) {
                existing.wheelPairCount
            } else {
                maxOf(existing?.wheelPairCount ?: 1, wheelPairCount.coerceAtLeast(1))
            },
            comment = if (comment.isBlank()) existing?.comment.orEmpty() else comment.trim(),
            createdOnPhone = existing?.createdOnPhone ?: false,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
            deletedAt = maxOf(existing?.deletedAt ?: 0L, deletedAt),
            sortOrder = existing?.sortOrder ?: 0L,
        )
        if (existing == null) {
            locomotiveDao.upsert(entity)
        } else {
            locomotiveDao.update(
                id = entity.id,
                series = entity.series,
                number = entity.number,
                wheelPairCount = entity.wheelPairCount,
                comment = entity.comment,
                createdOnPhone = entity.createdOnPhone,
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt,
                deletedAt = entity.deletedAt,
                sortOrder = entity.sortOrder,
            )
        }
        val savedId = existing?.id ?: 0L
        val locomotiveId = if (savedId == -1L || savedId == 0L) {
            requireNotNull(locomotiveDao.findByNumber(normalizedNumber) ?: locomotiveDao.find(normalizedSeries, normalizedNumber)).id
        } else {
            savedId
        }
        return requireNotNull(locomotiveDao.getById(locomotiveId))
    }

    private suspend fun replaceProfiles(
        locomotiveId: Long,
        profiles: List<ru.depo.zamerykp.data.db.WheelPairProfileEntity>,
    ) {
        profileDao.deleteForLocomotive(locomotiveId)
        profileDao.upsertAll(profiles)
    }

    private suspend fun ensureBaseProfiles(
        locomotiveId: Long,
        wheelPairCount: Int,
    ) {
        val existing = profileDao.getForLocomotive(locomotiveId).associateBy { it.number }
        val profiles = (1..wheelPairCount.coerceAtLeast(1)).map { number ->
            existing[number] ?: ru.depo.zamerykp.data.db.WheelPairProfileEntity(
                locomotiveId = locomotiveId,
                number = number,
                axisNumber = number,
            )
        }
        profileDao.upsertAll(profiles)
    }

    private suspend fun upsertArchiveProfiles(
        locomotiveId: Long,
        dto: MeasurementExportDto,
    ) {
        val existing = profileDao.getForLocomotive(locomotiveId).associateBy { it.number }
        val profiles = dto.wheelPairs
            .distinctBy { it.number }
            .map { pair ->
                existing[pair.number] ?: ru.depo.zamerykp.data.db.WheelPairProfileEntity(
                    locomotiveId = locomotiveId,
                    number = pair.number,
                    axisNumber = pair.number,
                )
            }
        if (profiles.isNotEmpty()) {
            profileDao.upsertAll(profiles)
        }
    }
}

private fun String.normalizeSeries(): String = trim().uppercase()

private fun String.normalizeNumber(): String = trim()

private fun archiveLocomotiveKey(series: String, number: String): String =
    "${series.trim().uppercase()}|${number.trim()}"

private fun SideExportDto.toEntity(
    sessionId: String,
    wheelPairNumber: Int,
    side: WheelSide,
) = WheelSideMeasurementEntity(
    sessionId = sessionId,
    wheelPairNumber = wheelPairNumber,
    side = side,
    flangeThickness = flangeThickness,
    flangeWear = flangeWear,
    flangeSteepness = flangeSteepness,
    bandageThickness = bandageThickness,
    bandageDiameter = bandageDiameter,
    updatedAt = System.currentTimeMillis(),
)
