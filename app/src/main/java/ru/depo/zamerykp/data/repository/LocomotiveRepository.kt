package ru.depo.zamerykp.data.repository

import kotlinx.coroutines.flow.Flow
import ru.depo.zamerykp.data.db.LocomotiveDao
import ru.depo.zamerykp.data.db.LocomotiveEntity
import ru.depo.zamerykp.data.db.WheelPairProfileDao
import ru.depo.zamerykp.data.db.WheelPairProfileEntity

class LocomotiveRepository(
    private val locomotiveDao: LocomotiveDao,
    private val profileDao: WheelPairProfileDao,
) {
    fun observeLocomotives(): Flow<List<LocomotiveEntity>> = locomotiveDao.observeAll()

    fun observeProfiles(locomotiveId: Long): Flow<List<WheelPairProfileEntity>> =
        profileDao.observeForLocomotive(locomotiveId)

    suspend fun getLocomotive(id: Long): LocomotiveEntity? = locomotiveDao.getById(id)

    suspend fun saveLocomotive(
        id: Long = 0,
        series: String,
        number: String,
        wheelPairCount: Int,
        comment: String,
        createdOnPhone: Boolean = true,
    ): Long {
        val now = System.currentTimeMillis()
        val normalizedSeries = series.normalizeSeries()
        val normalizedNumber = number.normalizeNumber()
        val old = if (id != 0L) {
            locomotiveDao.getById(id)
        } else {
            locomotiveDao.find(normalizedSeries, normalizedNumber) ?: locomotiveDao.findByNumber(normalizedNumber)
        }
        val savedId = locomotiveDao.upsert(
            LocomotiveEntity(
                id = old?.id ?: id,
                series = normalizedSeries,
                number = normalizedNumber,
                wheelPairCount = wheelPairCount.coerceAtLeast(1),
                comment = comment.trim(),
                createdOnPhone = old?.createdOnPhone ?: createdOnPhone,
                createdAt = old?.createdAt ?: now,
                updatedAt = now,
            )
        )
        val locomotiveId = if (savedId == -1L || savedId == 0L) (old?.id ?: id) else savedId
        ensureProfiles(locomotiveId, wheelPairCount)
        return locomotiveId
    }

    suspend fun deleteLocomotive(id: Long) {
        locomotiveDao.delete(id)
    }

    suspend fun saveProfiles(locomotiveId: Long, profiles: List<WheelPairProfileEntity>) {
        profileDao.upsertAll(profiles)
    }

    suspend fun ensureProfiles(locomotiveId: Long, wheelPairCount: Int) {
        val existing = profileDao.getForLocomotive(locomotiveId).associateBy { it.number }
        val profiles = (1..wheelPairCount.coerceAtLeast(1)).map { number ->
            existing[number] ?: WheelPairProfileEntity(
                locomotiveId = locomotiveId,
                number = number,
                axisNumber = number,
            )
        }
        profileDao.upsertAll(profiles)
    }
}

private fun String.normalizeSeries(): String = trim().uppercase()

private fun String.normalizeNumber(): String = trim()
