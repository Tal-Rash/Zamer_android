package ru.depo.zamerykp.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import ru.depo.zamerykp.domain.MeasurementSource
import ru.depo.zamerykp.domain.MeasurementStatus
import ru.depo.zamerykp.domain.SentStatus
import ru.depo.zamerykp.domain.WheelSide

    @Dao
    interface LocomotiveDao {
    @Query("SELECT * FROM locomotives ORDER BY id")
    suspend fun getAll(): List<LocomotiveEntity>

    @Query("SELECT * FROM locomotives WHERE deletedAt = 0 ORDER BY series, number")
    fun observeAll(): Flow<List<LocomotiveEntity>>

    @Query("SELECT * FROM locomotives WHERE id = :id")
    suspend fun getById(id: Long): LocomotiveEntity?

    @Query("SELECT * FROM locomotives WHERE series = :series AND number = :number LIMIT 1")
    suspend fun find(series: String, number: String): LocomotiveEntity?

    @Query("SELECT * FROM locomotives WHERE number = :number LIMIT 1")
    suspend fun findByNumber(number: String): LocomotiveEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: LocomotiveEntity): Long

    @Query(
        """
        UPDATE locomotives
        SET series = :series,
            number = :number,
            wheelPairCount = :wheelPairCount,
            comment = :comment,
            createdOnPhone = :createdOnPhone,
            createdAt = :createdAt,
            updatedAt = :updatedAt,
            deletedAt = :deletedAt
        WHERE id = :id
        """
    )
    suspend fun update(
        id: Long,
        series: String,
        number: String,
        wheelPairCount: Int,
        comment: String,
        createdOnPhone: Boolean,
        createdAt: Long,
        updatedAt: Long,
        deletedAt: Long,
    )

    @Query(
        """
        UPDATE locomotives
        SET deletedAt = :deletedAt,
            updatedAt = :updatedAt
        WHERE id = :id
        """
    )
    suspend fun delete(id: Long, deletedAt: Long, updatedAt: Long)

    @Query(
        """
        DELETE FROM locomotives
        WHERE createdOnPhone = 0
          AND id NOT IN (SELECT DISTINCT locomotiveId FROM measurement_sessions)
        """
    )
    suspend fun deleteOrphanImportedLocomotives()
}

@Dao
interface WheelPairProfileDao {
    @Query("SELECT * FROM wheel_pair_profiles ORDER BY locomotiveId, number")
    suspend fun getAll(): List<WheelPairProfileEntity>

    @Query("SELECT * FROM wheel_pair_profiles WHERE locomotiveId = :locomotiveId ORDER BY number")
    fun observeForLocomotive(locomotiveId: Long): Flow<List<WheelPairProfileEntity>>

    @Query("SELECT * FROM wheel_pair_profiles WHERE locomotiveId = :locomotiveId ORDER BY number")
    suspend fun getForLocomotive(locomotiveId: Long): List<WheelPairProfileEntity>

    @Query("DELETE FROM wheel_pair_profiles WHERE locomotiveId = :locomotiveId")
    suspend fun deleteForLocomotive(locomotiveId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<WheelPairProfileEntity>)
}

@Dao
interface MeasurementDao {
    @Query("SELECT * FROM measurement_sessions ORDER BY createdAt")
    suspend fun getAllSessions(): List<MeasurementSessionEntity>

    @Query("SELECT * FROM wheel_side_measurements ORDER BY sessionId, wheelPairNumber, side")
    suspend fun getAllSideMeasurements(): List<WheelSideMeasurementEntity>

    @Query("SELECT * FROM measurement_sessions WHERE id = :id")
    suspend fun getSession(id: String): MeasurementSessionEntity?

    @Query("SELECT * FROM measurement_sessions WHERE status = 'DRAFT' ORDER BY updatedAt DESC LIMIT 1")
    suspend fun getLatestDraftSession(): MeasurementSessionEntity?

    @Query(
        """
        SELECT s.id
        FROM measurement_sessions s
        INNER JOIN locomotives l ON l.id = s.locomotiveId
        WHERE s.status = 'FINISHED'
          AND (
              SELECT COUNT(*) FROM (
                  SELECT wheelPairNumber
                  FROM wheel_side_measurements
                  WHERE sessionId = s.id
                  GROUP BY wheelPairNumber
                  HAVING SUM(CASE WHEN flangeThickness IS NOT NULL
                       AND flangeWear IS NOT NULL
                       AND flangeSteepness IS NOT NULL
                       AND bandageThickness IS NOT NULL THEN 1 ELSE 0 END) = 2
              ) AS completed_pairs
          ) = l.wheelPairCount
        ORDER BY s.measurementDate DESC, s.updatedAt DESC
        """
    )
    suspend fun getFinishedSessionIds(): List<String>

    @Query("SELECT * FROM wheel_side_measurements WHERE sessionId = :sessionId ORDER BY wheelPairNumber, side")
    fun observeSides(sessionId: String): Flow<List<WheelSideMeasurementEntity>>

    @Query("SELECT * FROM wheel_side_measurements WHERE sessionId = :sessionId ORDER BY wheelPairNumber, side")
    suspend fun getSides(sessionId: String): List<WheelSideMeasurementEntity>

    @Query(
        """
        SELECT s.id AS measurementId, s.measurementDate, s.repairType, s.sentStatus,
               s.source AS source,
               l.series, l.number, l.wheelPairCount,
               (
                   SELECT COUNT(*) FROM (
                       SELECT wheelPairNumber
                       FROM wheel_side_measurements
                       WHERE sessionId = s.id
                       GROUP BY wheelPairNumber
                       HAVING SUM(CASE WHEN flangeThickness IS NOT NULL
                            AND flangeWear IS NOT NULL
                            AND flangeSteepness IS NOT NULL
                            AND bandageThickness IS NOT NULL THEN 1 ELSE 0 END) = 2
                   ) AS completed_pairs
               ) AS filledWheelPairs
        FROM measurement_sessions s
        INNER JOIN locomotives l ON l.id = s.locomotiveId
        WHERE s.status = 'FINISHED'
          AND (
              SELECT COUNT(*) FROM (
                  SELECT wheelPairNumber
                  FROM wheel_side_measurements
                  WHERE sessionId = s.id
                  GROUP BY wheelPairNumber
                  HAVING SUM(CASE WHEN flangeThickness IS NOT NULL
                       AND flangeWear IS NOT NULL
                       AND flangeSteepness IS NOT NULL
                       AND bandageThickness IS NOT NULL THEN 1 ELSE 0 END) = 2
              ) AS completed_pairs
          ) = l.wheelPairCount
        ORDER BY s.measurementDate DESC, s.updatedAt DESC
        """
    )
    fun observeArchive(): Flow<List<SessionWithLocomotiveRow>>

    @Query(
        """
        SELECT s.id AS measurementId,
               s.measurementDate,
               s.repairType,
               s.status AS status,
               s.sentStatus AS sentStatus,
               l.series AS series,
               l.number AS number
        FROM measurement_sessions s
        INNER JOIN locomotives l ON l.id = s.locomotiveId
        WHERE s.source = 'PHONE'
          AND s.sentStatus != 'EXPORTED'
        ORDER BY s.updatedAt DESC
        """
    )
    fun observePendingMeasurements(): Flow<List<PendingMeasurementRow>>

    @Query(
        """
        SELECT s.id AS measurementId,
               s.measurementDate,
               s.repairType,
               s.status AS status,
               s.sentStatus AS sentStatus,
               l.series AS series,
               l.number AS number
        FROM measurement_sessions s
        INNER JOIN locomotives l ON l.id = s.locomotiveId
        WHERE s.source = 'PHONE'
          AND s.sentStatus != 'EXPORTED'
        ORDER BY s.updatedAt DESC
        """
    )
    suspend fun getPendingMeasurements(): List<PendingMeasurementRow>

    @Query(
        """
        SELECT COUNT(*)
        FROM measurement_sessions
        WHERE source = 'PHONE'
          AND sentStatus != 'EXPORTED'
        """
    )
    suspend fun countPendingMeasurements(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSession(entity: MeasurementSessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSide(entity: WheelSideMeasurementEntity)

    @Query("DELETE FROM wheel_side_measurements WHERE sessionId = :sessionId AND wheelPairNumber = :wheelPairNumber")
    suspend fun deleteWheelPairSides(sessionId: String, wheelPairNumber: Int)

    @Query("UPDATE measurement_sessions SET status = :status, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateStatus(id: String, status: MeasurementStatus, updatedAt: Long)

    @Query("UPDATE measurement_sessions SET sentStatus = :sentStatus, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateSentStatus(id: String, sentStatus: SentStatus, updatedAt: Long)

    @Query("DELETE FROM measurement_sessions WHERE id = :id")
    suspend fun deleteSession(id: String)

    @Query("DELETE FROM measurement_sessions WHERE archivePayload = 1")
    suspend fun deleteImportedArchiveSessions()

    @Transaction
    suspend fun createSessionWithEmptySides(session: MeasurementSessionEntity, wheelPairCount: Int) {
        upsertSession(session)
        for (number in 1..wheelPairCount.coerceAtLeast(1)) {
            upsertSide(WheelSideMeasurementEntity(session.id, number, WheelSide.LEFT, updatedAt = session.updatedAt))
            upsertSide(WheelSideMeasurementEntity(session.id, number, WheelSide.RIGHT, updatedAt = session.updatedAt))
        }
    }
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM app_settings WHERE id = 1")
    fun observe(): Flow<AppSettingsEntity?>

    @Query("SELECT * FROM app_settings WHERE id = 1")
    suspend fun get(): AppSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: AppSettingsEntity)
}
