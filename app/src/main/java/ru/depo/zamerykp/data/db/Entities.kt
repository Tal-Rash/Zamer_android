package ru.depo.zamerykp.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import ru.depo.zamerykp.domain.MeasurementSource
import ru.depo.zamerykp.domain.MeasurementStatus
import ru.depo.zamerykp.domain.SentStatus
import ru.depo.zamerykp.domain.WheelSide

@Entity(
    tableName = "locomotives",
    indices = [Index(value = ["series", "number"], unique = true)]
)
data class LocomotiveEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val series: String,
    val number: String,
    val wheelPairCount: Int,
    val comment: String = "",
    val createdOnPhone: Boolean = true,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long = 0L,
    val sortOrder: Long = 0L,
)

@Entity(
    tableName = "wheel_pair_profiles",
    primaryKeys = ["locomotiveId", "number"],
    foreignKeys = [
        ForeignKey(
            entity = LocomotiveEntity::class,
            parentColumns = ["id"],
            childColumns = ["locomotiveId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("locomotiveId")]
)
data class WheelPairProfileEntity(
    val locomotiveId: Long,
    val number: Int,
    val axisNumber: Int,
    val kcDiameterLeft: Double? = null,
    val kcDiameterRight: Double? = null,
)

@Entity(
    tableName = "measurement_sessions",
    foreignKeys = [
        ForeignKey(
            entity = LocomotiveEntity::class,
            parentColumns = ["id"],
            childColumns = ["locomotiveId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("locomotiveId")]
)
data class MeasurementSessionEntity(
    @PrimaryKey
    val id: String,
    val locomotiveId: Long,
    val measurementDate: String,
    val repairType: String,
    val status: MeasurementStatus = MeasurementStatus.DRAFT,
    val source: MeasurementSource = MeasurementSource.PHONE,
    val archivePayload: Boolean = false,
    val sentStatus: SentStatus = SentStatus.NOT_SENT,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "wheel_side_measurements",
    primaryKeys = ["sessionId", "wheelPairNumber", "side"],
    foreignKeys = [
        ForeignKey(
            entity = MeasurementSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("sessionId")]
)
data class WheelSideMeasurementEntity(
    val sessionId: String,
    val wheelPairNumber: Int,
    val side: WheelSide,
    val flangeThickness: Double? = null,
    val flangeWear: Double? = null,
    val flangeSteepness: Double? = null,
    val bandageThickness: Double? = null,
    val bandageDiameter: Double? = null,
    val updatedAt: Long,
)

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey
    val id: Int = 1,
    val defaultEmail: String = "",
    val voiceConfirmLowConfidence: Boolean = true,
    val keepVoiceServiceEnabled: Boolean = true,
    val keepScreenOn: Boolean = true,
    val voskModelUri: String = "",
    val syncServerUrl: String = "",
    val syncPassword: String = "",
)
