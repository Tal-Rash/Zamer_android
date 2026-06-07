package ru.depo.zamerykp.data.repository

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonArray
import ru.depo.zamerykp.data.db.LocomotiveDao
import ru.depo.zamerykp.data.db.MeasurementDao
import ru.depo.zamerykp.data.db.WheelPairProfileDao
import ru.depo.zamerykp.domain.ImportEnvelope
import ru.depo.zamerykp.domain.ArchiveDataExportDto
import ru.depo.zamerykp.domain.LocomotiveExportDto
import ru.depo.zamerykp.domain.ImportPayload
import ru.depo.zamerykp.domain.ReferenceDataExportDto
import ru.depo.zamerykp.domain.ReferenceLocomotiveExportDto
import ru.depo.zamerykp.domain.ReferenceWheelPairExportDto
import ru.depo.zamerykp.domain.MeasurementExportDto
import ru.depo.zamerykp.domain.SideExportDto
import ru.depo.zamerykp.domain.WheelPairExportDto
import ru.depo.zamerykp.domain.WheelSide
import java.time.OffsetDateTime
import kotlin.math.roundToInt

class ExportRepository(
    private val measurementDao: MeasurementDao,
    private val locomotiveDao: LocomotiveDao,
    private val profileDao: WheelPairProfileDao,
) {
    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        explicitNulls = true
        ignoreUnknownKeys = true
    }

    suspend fun buildExport(sessionId: String): MeasurementExportDto {
        val session = requireNotNull(measurementDao.getSession(sessionId)) { "Замер не найден" }
        val locomotive = requireNotNull(locomotiveDao.getById(session.locomotiveId)) { "Локомотив не найден" }
        val sides = measurementDao.getSides(sessionId)
        val sideMap = sides.associateBy { it.wheelPairNumber to it.side }
        val profiles = profileDao.getForLocomotive(locomotive.id)
        val pairNumbers = (profiles.map { it.number } + sides.map { it.wheelPairNumber })
            .distinct()
            .ifEmpty { (1..locomotive.wheelPairCount).toList() }
            .sorted()

        return MeasurementExportDto(
            createdAt = OffsetDateTime.now().toString(),
            measurementId = session.id,
            locomotive = LocomotiveExportDto(
                series = locomotive.series,
                number = locomotive.number,
                wheelPairCount = locomotive.wheelPairCount,
                comment = locomotive.comment,
                isNew = locomotive.createdOnPhone,
            ),
            repairType = session.repairType,
            measurementDate = session.measurementDate,
            wheelPairs = pairNumbers.map { number ->
                WheelPairExportDto(
                    number = number,
                    left = sideMap[number to WheelSide.LEFT].toDto(),
                    right = sideMap[number to WheelSide.RIGHT].toDto(),
                )
            },
        )
    }

    suspend fun exportJson(sessionId: String): String = json.encodeToString(buildExport(sessionId))

    suspend fun buildArchiveExport(): ArchiveDataExportDto =
        ArchiveDataExportDto(
            exportedAt = OffsetDateTime.now().toString(),
            archive = measurementDao.getFinishedSessionIds().map { buildExport(it) }
        )

    suspend fun exportArchiveJson(): String = json.encodeToString(buildArchiveExport())

    suspend fun buildReferenceExport(): ReferenceDataExportDto {
        val locomotives = locomotiveDao.getAll()
        val profiles = profileDao.getAll().groupBy { it.locomotiveId }
        return ReferenceDataExportDto(
            exportedAt = OffsetDateTime.now().toString(),
            locomotives = locomotives.map { locomotive ->
                val wheelPairs = profiles[locomotive.id].orEmpty()
                    .sortedBy { it.number }
                    .map { profile ->
                        ReferenceWheelPairExportDto(
                            number = profile.number,
                            axisNumber = profile.axisNumber,
                            diameterLeft = profile.kcDiameterLeft,
                            diameterRight = profile.kcDiameterRight,
                        )
                    }
                ReferenceLocomotiveExportDto(
                    series = locomotive.series,
                    number = locomotive.number,
                    wheelPairCount = locomotive.wheelPairCount,
                    wheelPairs = wheelPairs,
                )
            },
        )
    }

    suspend fun exportReferenceJson(): String = json.encodeToString(buildReferenceExport())

    fun parseExport(text: String): MeasurementExportDto {
        return parseImportEnvelope(text).measurement
            ?: runCatching { json.decodeFromString(MeasurementExportDto.serializer(), text) }
                .getOrElse { throw IllegalArgumentException("Не найден объект замера в JSON") }
    }

    fun parseImportPayload(text: String): ImportPayload {
        val envelope = parseImportEnvelope(text)
        return when {
            envelope.archiveData != null -> ImportPayload.ArchiveData(envelope.archiveData)
            envelope.referenceData != null && envelope.measurement == null -> ImportPayload.ReferenceData(envelope.referenceData)
            envelope.measurement != null -> ImportPayload.Measurement(envelope.measurement)
            else -> throw IllegalArgumentException("Не найден объект замера или справочника в JSON")
        }
    }

    fun parseImportEnvelope(text: String): ImportEnvelope {
        val element = json.parseToJsonElement(text)
        val objectElement = element.jsonObject
        val measurementNode = objectElement["measurementImport"]
            ?: objectElement["measurement"]
            ?: objectElement["measurementExport"]
        val referenceNode = objectElement["referenceExport"]
            ?: objectElement["referenceData"]
        val archiveNode = objectElement["archiveData"]

        val measurementDto = when {
            measurementNode != null -> decodeMeasurementSection(measurementNode)
            objectElement["formatVersion"]?.jsonPrimitive?.contentOrNull == "1" &&
                objectElement["exportType"]?.jsonPrimitive?.contentOrNull != "archiveData" -> decodeMeasurementSection(element)
            else -> null
        }
        val referenceDto = when {
            referenceNode != null -> decodeReferenceSection(referenceNode)
            objectElement["exportType"]?.jsonPrimitive?.contentOrNull == "referenceData" -> decodeReferenceSection(element)
            else -> null
        }
        val archiveDto = when {
            objectElement["exportType"]?.jsonPrimitive?.contentOrNull == "archiveData" -> decodeArchiveSection(element)
            archiveNode is JsonObject -> decodeArchiveSection(archiveNode)
            else -> null
        }
        return ImportEnvelope(measurement = measurementDto, referenceData = referenceDto, archiveData = archiveDto)
    }

    private fun decodeMeasurementSection(element: JsonElement): MeasurementExportDto? {
        return runCatching { json.decodeFromJsonElement(MeasurementExportDto.serializer(), element) }
            .getOrNull()
    }

    private fun decodeReferenceSection(element: JsonElement): ReferenceDataExportDto? {
        return runCatching { json.decodeFromJsonElement(ReferenceDataExportDto.serializer(), element) }
            .getOrNull()
    }

    private fun decodeArchiveSection(element: JsonElement): ArchiveDataExportDto? {
        val objectElement = element.jsonObject
        val archiveNode = objectElement["archiveData"]?.jsonObject ?: objectElement
        val exportedAt = archiveNode["exportedAt"]?.jsonPrimitive?.contentOrNull.orEmpty()
        val archiveItems = archiveNode["archive"]
            ?.jsonArray
            ?.mapNotNull { item -> decodeMeasurementExportItem(item) }
            ?: emptyList()
        return ArchiveDataExportDto(
            exportedAt = exportedAt,
            archive = archiveItems,
        )
    }

    private fun decodeMeasurementExportItem(element: JsonElement): MeasurementExportDto? {
        val obj = element.jsonObject
        val locomotive = obj["locomotive"]?.jsonObject ?: return null
        val wheelPairs = obj["wheelPairs"]?.jsonArray?.mapNotNull { pairElement ->
            decodeWheelPairExport(pairElement)
        }.orEmpty()
        return MeasurementExportDto(
            createdAt = obj["createdAt"]?.jsonPrimitive?.contentOrNull.orEmpty(),
            measurementId = obj["measurementId"]?.jsonPrimitive?.contentOrNull.orEmpty(),
            locomotive = LocomotiveExportDto(
                series = locomotive["series"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                number = locomotive["number"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                wheelPairCount = locomotive["wheelPairCount"]?.jsonPrimitive?.intOrNull ?: wheelPairs.size,
                comment = locomotive["comment"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                isNew = locomotive["isNew"]?.jsonPrimitive?.booleanOrNull ?: false,
            ),
            repairType = obj["repairType"]?.jsonPrimitive?.contentOrNull.orEmpty(),
            measurementDate = obj["measurementDate"]?.jsonPrimitive?.contentOrNull.orEmpty(),
            wheelPairs = wheelPairs,
        )
    }

    private fun decodeWheelPairExport(element: JsonElement): WheelPairExportDto? {
        val obj = element.jsonObject
        val number = obj["number"]?.jsonPrimitive?.intOrNull ?: return null
        return WheelPairExportDto(
            number = number,
            left = decodeSideExport(obj["left"]),
            right = decodeSideExport(obj["right"]),
        )
    }

    private fun decodeSideExport(element: JsonElement?): SideExportDto {
        val obj = element?.jsonObject ?: return SideExportDto()
        return SideExportDto(
            flangeThickness = obj["flangeThickness"]?.jsonPrimitive?.doubleOrNull,
            flangeWear = obj["flangeWear"]?.jsonPrimitive?.doubleOrNull,
            flangeSteepness = obj["flangeSteepness"]?.jsonPrimitive?.doubleOrNull,
            bandageThickness = obj["bandageThickness"]?.jsonPrimitive?.doubleOrNull,
            bandageDiameter = obj["bandageDiameter"]?.jsonPrimitive?.doubleOrNull,
        )
    }
}

private fun ru.depo.zamerykp.data.db.WheelSideMeasurementEntity?.toDto(): SideExportDto =
    SideExportDto(
        flangeThickness = this?.flangeThickness,
        flangeWear = this?.flangeWear,
        flangeSteepness = this?.flangeSteepness,
        bandageThickness = this?.bandageThickness,
        bandageDiameter = this?.bandageDiameter?.roundToInt()?.toDouble(),
    )
