package ru.depo.zamerykp.data.db

import androidx.room.TypeConverter
import ru.depo.zamerykp.domain.MeasurementSource
import ru.depo.zamerykp.domain.MeasurementStatus
import ru.depo.zamerykp.domain.SentStatus
import ru.depo.zamerykp.domain.WheelSide

class Converters {
    @TypeConverter
    fun toWheelSide(value: String): WheelSide = enumValueOf(value)

    @TypeConverter
    fun fromWheelSide(value: WheelSide): String = value.name

    @TypeConverter
    fun toMeasurementStatus(value: String): MeasurementStatus = enumValueOf(value)

    @TypeConverter
    fun fromMeasurementStatus(value: MeasurementStatus): String = value.name

    @TypeConverter
    fun toMeasurementSource(value: String): MeasurementSource = enumValueOf(value)

    @TypeConverter
    fun fromMeasurementSource(value: MeasurementSource): String = value.name

    @TypeConverter
    fun toSentStatus(value: String): SentStatus = enumValueOf(value)

    @TypeConverter
    fun fromSentStatus(value: SentStatus): String = value.name
}
