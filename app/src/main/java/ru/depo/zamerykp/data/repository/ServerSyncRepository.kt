package ru.depo.zamerykp.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import ru.depo.zamerykp.domain.ImportPayload
import ru.depo.zamerykp.domain.ReferenceDataExportDto
import ru.depo.zamerykp.domain.ReferenceLocomotiveExportDto
import ru.depo.zamerykp.domain.MeasurementStatus
import java.time.LocalDate
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class ServerSyncResult(
    val referencePushed: Int = 0,
    val archivePushed: Int = 0,
    val pendingPushed: Int = 0,
    val referencePulled: Int = 0,
    val archivePulled: Int = 0,
    val referenceConflicts: List<ReferenceSyncConflict> = emptyList(),
)

data class ReferenceSyncConflict(
    val series: String,
    val number: String,
    val localUpdatedAt: Long,
    val serverUpdatedAt: Long,
    val reason: String,
)

data class LocomotiveServerInfo(
    val inventoryNumber: String,
    val eightDigitNumber: String,
    val manufactureYear: String,
    val serviceLife: String,
)

class ServerSyncRepository(
    private val exportRepository: ExportRepository,
    private val measurementRepository: MeasurementRepository,
) {
    private val json = Json {
        encodeDefaults = true
        explicitNulls = true
    }

    suspend fun sync(
        serverBaseUrl: String,
        password: String,
    ): ServerSyncResult = withContext(Dispatchers.IO) {
        val baseUrl = normalizeBaseUrl(serverBaseUrl)
        require(baseUrl.isNotBlank()) { "Введите адрес сервера." }
        require(password.isNotBlank()) { "Введите пароль веб-входа." }

        val cookie = login(baseUrl, password)
        val pulledServerReference = pullReferenceDto(baseUrl, cookie)
        val serverReference = pulledServerReference.copy(
            locomotives = pulledServerReference.locomotives.collapseReferenceLocomotives(),
        )
        val builtLocalReference = exportRepository.buildReferenceExport()
        val localReference = builtLocalReference.copy(
            locomotives = builtLocalReference.locomotives.collapseReferenceLocomotives(),
        )
        val localMap = localReference.locomotives.associateBy { it.referenceKey() }
        val serverMap = serverReference.locomotives.associateBy { it.referenceKey() }

        val referenceConflicts = mutableListOf<ReferenceSyncConflict>()
        val referenceToUpload = mutableListOf<ReferenceLocomotiveExportDto>()
        var referencePulled = 0
        for (serverLocomotive in serverReference.locomotives) {
            val key = serverLocomotive.referenceKey()
            val localLocomotive = localMap[key]
            if (localLocomotive == null) {
                measurementRepository.importReferenceData(
                    ReferenceDataExportDto(
                        exportedAt = serverReference.exportedAt,
                        locomotives = listOf(serverLocomotive),
                    ),
                    importLocomotives = true,
                    importWheelPairs = true,
                )
                referencePulled += 1
                continue
            }
            val equal = serverLocomotive.referenceEquals(localLocomotive)
            when {
                serverLocomotive.deletedAt > 0L && localLocomotive.deletedAt <= 0L && serverLocomotive.updatedAt >= localLocomotive.updatedAt -> {
                    measurementRepository.importReferenceData(
                        ReferenceDataExportDto(
                            exportedAt = serverReference.exportedAt,
                            locomotives = listOf(serverLocomotive),
                        ),
                        importLocomotives = true,
                        importWheelPairs = true,
                    )
                    referencePulled += 1
                }
                serverLocomotive.deletedAt > 0L && localLocomotive.deletedAt > 0L -> {
                    if (serverLocomotive.updatedAt > localLocomotive.updatedAt) {
                        measurementRepository.importReferenceData(
                            ReferenceDataExportDto(
                                exportedAt = serverReference.exportedAt,
                                locomotives = listOf(serverLocomotive),
                            ),
                            importLocomotives = true,
                            importWheelPairs = true,
                        )
                        referencePulled += 1
                    }
                }
                localLocomotive.deletedAt > 0L && serverLocomotive.deletedAt <= 0L && localLocomotive.updatedAt >= serverLocomotive.updatedAt -> {
                    referenceToUpload += localLocomotive
                }
                serverLocomotive.sortOrder != localLocomotive.sortOrder && serverLocomotive.referenceEqualsIgnoringSortOrder(localLocomotive) -> {
                    if (serverLocomotive.updatedAt >= localLocomotive.updatedAt) {
                        measurementRepository.importReferenceData(
                            ReferenceDataExportDto(
                                exportedAt = serverReference.exportedAt,
                                locomotives = listOf(serverLocomotive),
                            ),
                            importLocomotives = true,
                            importWheelPairs = true,
                        )
                        referencePulled += 1
                    } else {
                        referenceToUpload += localLocomotive
                    }
                }
                serverLocomotive.updatedAt > localLocomotive.updatedAt -> {
                    measurementRepository.importReferenceData(
                        ReferenceDataExportDto(
                            exportedAt = serverReference.exportedAt,
                            locomotives = listOf(serverLocomotive),
                        ),
                        importLocomotives = true,
                        importWheelPairs = true,
                    )
                    referencePulled += 1
                }
                localLocomotive.updatedAt > serverLocomotive.updatedAt -> {
                    referenceToUpload += localLocomotive
                }
                !equal -> {
                    referenceConflicts += ReferenceSyncConflict(
                        series = serverLocomotive.series,
                        number = serverLocomotive.number,
                        localUpdatedAt = localLocomotive.updatedAt,
                        serverUpdatedAt = serverLocomotive.updatedAt,
                        reason = "Изменения есть и на сервере, и локально.",
                    )
                }
            }
        }
        for (localLocomotive in localReference.locomotives) {
            if (localLocomotive.deletedAt > 0L) {
                continue
            }
            if (serverMap[localLocomotive.referenceKey()] == null) {
                referenceToUpload += localLocomotive
            }
        }
        val referencePushed = if (referenceToUpload.isNotEmpty()) {
            val payload = localReference.copy(
                exportedAt = serverReference.exportedAt,
                locomotives = referenceToUpload.collapseReferenceLocomotives(),
            )
            postJson("$baseUrl/zamer-kp/api/phone-import", cookie, json.encodeToString(payload))
            payload.locomotives.size
        } else {
            0
        }
        val pendingPushed = 0

        val syncableMeasurements = measurementRepository.getSyncableMeasurements()
        var archivePushed = 0
        for (item in syncableMeasurements) {
            val measurementDto = exportRepository.buildExport(item.measurementId)
            postJson("$baseUrl/zamer-kp/api/phone-import", cookie, json.encodeToString(measurementDto))
            measurementRepository.markSent(item.measurementId)
            archivePushed += 1
        }

        val pulledArchive = pullArchiveData(
            baseUrl,
            cookie,
            serverReference.locomotives.map { it.referenceKey() }.toSet(),
        )

        ServerSyncResult(
            referencePushed = referencePushed,
            archivePushed = archivePushed,
            pendingPushed = pendingPushed,
            referencePulled = referencePulled,
            archivePulled = pulledArchive,
            referenceConflicts = referenceConflicts,
        )
    }

    suspend fun pushLocalReferenceSnapshot(
        serverBaseUrl: String,
        password: String,
    ): Int = withContext(Dispatchers.IO) {
        val baseUrl = normalizeBaseUrl(serverBaseUrl)
        require(baseUrl.isNotBlank()) { "Введите адрес сервера." }
        require(password.isNotBlank()) { "Введите пароль веб-входа." }
        val cookie = login(baseUrl, password)
        val builtReferenceDto = exportRepository.buildReferenceExport()
        val referenceDto = builtReferenceDto.copy(
            locomotives = builtReferenceDto.locomotives.collapseReferenceLocomotives(),
        )
        if (referenceDto.locomotives.isEmpty()) return@withContext 0
        postJson("$baseUrl/zamer-kp/api/phone-import", cookie, json.encodeToString(referenceDto))
        referenceDto.locomotives.size
    }

    suspend fun fetchLocomotiveInfo(
        serverBaseUrl: String,
        password: String,
        series: String,
        number: String,
    ): LocomotiveServerInfo? = withContext(Dispatchers.IO) {
        val baseUrl = normalizeBaseUrl(serverBaseUrl)
        require(baseUrl.isNotBlank()) { "В настройках не указан адрес сервера." }
        require(password.isNotBlank()) { "В настройках не указан пароль сервера." }

        val cookie = login(baseUrl, password)
        val text = getText("$baseUrl/zamer-kp/api/phone-export?kind=reference&format=json", cookie)
        val root = json.parseToJsonElement(text).jsonObject
        val locomotives = root["locomotives"]?.jsonArray.orEmpty()
        val targetSeries = series.trim().uppercase()
        val targetNumber = number.trim()
        val item = locomotives
            .mapNotNull { runCatching { it.jsonObject }.getOrNull() }
            .firstOrNull { locomotive ->
                locomotive.valueFor("series").uppercase() == targetSeries &&
                    locomotive.valueFor("number") == targetNumber
            }
            ?: return@withContext null

        LocomotiveServerInfo(
            inventoryNumber = item.valueFor(
                "inventoryNumber", "inventory_number", "invNumber", "inv_number",
                "inventory", "инвНомер", "инв_номер",
            ),
            eightDigitNumber = item.valueFor(
                "eightDigitNumber", "eight_digit_number", "eightNumber", "eight_number",
                "uicNumber", "uic_number", "восьмизначныйНомер", "восьмизначный_номер",
            ),
            manufactureYear = item.valueFor(
                "manufactureYear", "manufacture_year", "yearBuilt", "buildYear", "constructionYear", "годПостройки",
            ),
            serviceLife = item.valueFor(
                "serviceLife", "service_life", "serviceLifetime", "service_lifetime", "serviceTerm", "service_term", "срокСлужбы", "срок_службы",
            ),
        )
    }

    suspend fun fetchRepairScheduleDates(
        serverBaseUrl: String,
        password: String,
        series: String,
        number: String,
        year: Int = LocalDate.now().year,
    ): Map<String, String> = withContext(Dispatchers.IO) {
        val baseUrl = normalizeBaseUrl(serverBaseUrl)
        if (baseUrl.isBlank() || password.isBlank()) return@withContext emptyMap()
        runCatching {
            val cookie = login(baseUrl, password)
            val text = getText("$baseUrl/grafik-ppr/api/state?year=$year", cookie)
            parseRepairScheduleDates(text, series, number)
        }.getOrDefault(emptyMap())
    }

    private fun normalizeBaseUrl(value: String): String =
        value.trim()
            .trimEnd('/')
            .removeSuffix("/zamer-kp")

    private fun login(baseUrl: String, password: String): String {
        var loginUrl = URL("$baseUrl/login")
        repeat(4) { _ ->
            val connection = (loginUrl.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                instanceFollowRedirects = false
                doOutput = true
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=utf-8")
                connectTimeout = 15_000
                readTimeout = 15_000
            }
            connection.outputStream.use { output ->
                OutputStreamWriter(output, Charsets.UTF_8).use { writer ->
                    writer.write("password=${URLEncoder.encode(password, Charsets.UTF_8.name())}")
                }
            }
            val responseCode = connection.responseCode
            val cookie = connection.headerFields["Set-Cookie"]
                ?.firstOrNull { it.contains("rtps_session=") || it.contains("grafik_ppr_session=") }
                ?.substringBefore(';')
            if (!cookie.isNullOrBlank()) {
                runCatching { connection.inputStream.close() }
                connection.disconnect()
                return cookie
            }
            if (responseCode in 300..399) {
                val location = connection.getHeaderField("Location")
                    ?: throw IllegalStateException("Сервер вернул редирект без адреса входа (код $responseCode)")
                runCatching { connection.inputStream.close() }
                connection.disconnect()
                loginUrl = URL(loginUrl, location)
                return@repeat
            }
            val error = connection.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            runCatching { connection.inputStream.close() }
            connection.disconnect()
            throw IllegalStateException(error.ifBlank { "Не удалось получить веб-сессию (код $responseCode)" })
        }
        throw IllegalStateException("Не удалось получить веб-сессию после редиректов")
    }

    private suspend fun pullReferenceDto(baseUrl: String, cookie: String): ReferenceDataExportDto {
        val text = getText("$baseUrl/zamer-kp/api/phone-export?kind=reference&format=json", cookie)
        val payload = exportRepository.parseImportPayload(text)
        return when (payload) {
            is ImportPayload.ReferenceData -> payload.dto
            is ImportPayload.Measurement, is ImportPayload.ArchiveData -> throw IllegalStateException("Сервер вернул не справочник.")
        }
    }

    private suspend fun pullArchiveData(
        baseUrl: String,
        cookie: String,
        allowedReferenceLocomotives: Set<String>,
    ): Int {
        val text = getText("$baseUrl/zamer-kp/api/phone-export?kind=archive&format=json", cookie)
        val payload = exportRepository.parseImportPayload(text)
        return when (payload) {
            is ImportPayload.ArchiveData -> {
                measurementRepository.replaceImportedArchive()
                var imported = 0
                payload.dto.archive.forEach { measurement ->
                    measurementRepository.importMeasurement(
                        measurement,
                        importLocomotive = true,
                        importWheelPairs = true,
                        importArchive = true,
                        archivePayload = true,
                        allowedArchiveLocomotives = allowedReferenceLocomotives,
                    )
                    imported += 1
                }
                imported
            }
            is ImportPayload.Measurement -> 0
            is ImportPayload.ReferenceData -> 0
        }
    }

    private fun postJson(url: String, cookie: String, body: String) {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Cookie", cookie)
        }
        connection.outputStream.use { output ->
            OutputStreamWriter(output, Charsets.UTF_8).use { writer ->
                writer.write(body)
            }
        }
        val code = connection.responseCode
        if (code !in 200..299) {
            val error = connection.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            connection.disconnect()
            throw IllegalStateException(error.ifBlank { "Сервер вернул код $code" })
        }
        runCatching { connection.inputStream.close() }
        connection.disconnect()
    }

    private fun getText(url: String, cookie: String): String {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("Cookie", cookie)
            setRequestProperty("Accept", "application/json")
            connectTimeout = 15_000
            readTimeout = 30_000
        }
        val code = connection.responseCode
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
        connection.disconnect()
        if (code !in 200..299) {
            throw IllegalStateException(text.ifBlank { "Сервер вернул код $code" })
        }
        return text
    }

    private fun parseRepairScheduleDates(
        text: String,
        series: String,
        number: String,
    ): Map<String, String> {
        val root = json.parseToJsonElement(text).jsonObject
        val schedule = root["repair_schedule"]?.jsonObject ?: return emptyMap()
        val objects = schedule["objects"]?.jsonArray ?: return emptyMap()
        val targetSeries = series.trim().uppercase()
        val targetNumber = number.trim()
        val columns = schedule["columns"]?.jsonArray.orEmpty()
        val row = objects.mapNotNull { item ->
            val obj = item.jsonObject
            val objSeries = obj["series"].stringOrNull()?.trim()?.uppercase()
            val objNumber = obj["number"].stringOrNull()?.trim()
            if (objSeries == targetSeries && objNumber == targetNumber) obj else null
        }.maxByOrNull { candidate -> candidate.latestRepairDateMillis(columns) } ?: return emptyMap()

        val result = linkedMapOf<String, String>()
        fun putIfBlank(type: String, value: String?) {
            val textValue = value?.trim().orEmpty()
            if (textValue.isNotBlank() && result[type].isNullOrBlank()) {
                result[type] = textValue
            }
        }

        fun pickGraphDate(cellValue: String?): String? =
            extractLatestDate(cellValue)

        val fact = row["fact"]?.jsonArray.orEmpty()
        columns.forEachIndexed { index, column ->
            val code = column.jsonObject["code"].stringOrNull().orEmpty().trim().uppercase()
            if (code.isBlank()) return@forEachIndexed
            val repairType = when (code) {
                "ТР1" -> "ТР1"
                "ТР2" -> "ТР2"
                "ТР3" -> "ТР3"
                "СР" -> "СР"
                "КР" -> "КР"
                else -> code
            }
            val factValue = fact.getOrNull(index).stringOrNull()
            putIfBlank(repairType, pickGraphDate(factValue))
        }

        val krObject = row["kr"]?.jsonObject
        if (krObject != null) {
            pickGraphDate(krObject["fact"].stringOrNull())?.let { result["КР"] = it }
        }
        return result
    }
}

private fun kotlinx.serialization.json.JsonArray?.orEmpty(): kotlinx.serialization.json.JsonArray =
    this ?: kotlinx.serialization.json.JsonArray(emptyList())

private fun kotlinx.serialization.json.JsonElement?.stringOrNull(): String? =
    runCatching { this?.jsonPrimitive?.content }.getOrNull()

private fun kotlinx.serialization.json.JsonObject.valueFor(vararg keys: String): String =
    keys.firstNotNullOfOrNull { key -> this[key].stringOrNull()?.trim()?.takeIf(String::isNotBlank) }.orEmpty()

private fun extractLatestDate(value: String?): String? {
    val text = value?.trim().orEmpty()
    if (text.isBlank()) return null
    val matches = DATE_PATTERN.findAll(text).map { it.value }.toList()
    return when {
        matches.isNotEmpty() -> matches.last()
        else -> text
    }
}

private fun kotlinx.serialization.json.JsonObject.latestRepairDateMillis(columns: kotlinx.serialization.json.JsonArray): Long {
    fun rowValueAt(index: Int): String? {
        val fact = this["fact"]?.jsonArray?.getOrNull(index).stringOrNull()
        return extractLatestDate(fact)
    }

    var best = Long.MIN_VALUE
    columns.forEachIndexed { index, column ->
        val code = column.jsonObject["code"].stringOrNull().orEmpty().trim().uppercase()
        if (code.isBlank()) return@forEachIndexed
        val candidate = rowValueAt(index)?.toRepairDateMillis() ?: return@forEachIndexed
        if (candidate > best) best = candidate
    }
    val krCandidate = this["kr"]?.jsonObject?.let { kr ->
        extractLatestDate(kr["fact"].stringOrNull())
    }?.toRepairDateMillis()
    if (krCandidate != null && krCandidate > best) best = krCandidate
    return best
}

private fun String.toRepairDateMillis(): Long? =
    runCatching {
        LocalDate.parse(this, java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy"))
            .atStartOfDay(java.time.ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }.getOrNull()

private val DATE_PATTERN = Regex("""\d{2}\.\d{2}\.\d{4}""")

private fun ReferenceLocomotiveExportDto.referenceKey(): String =
    "${series.trim().uppercase()}|${number.trim()}"

private fun ReferenceLocomotiveExportDto.referenceEquals(other: ReferenceLocomotiveExportDto): Boolean {
    if (!referenceEqualsIgnoringSortOrder(other)) return false
    if (sortOrder != other.sortOrder) return false
    return true
}

private fun ReferenceLocomotiveExportDto.referenceEqualsIgnoringSortOrder(other: ReferenceLocomotiveExportDto): Boolean {
    if (series.trim().uppercase() != other.series.trim().uppercase()) return false
    if (number.trim() != other.number.trim()) return false
    if (wheelPairCount != other.wheelPairCount) return false
    if (deletedAt > 0L && other.deletedAt > 0L) return true
    if (deletedAt != other.deletedAt) return false
    if (wheelPairs.size != other.wheelPairs.size) return false
    return wheelPairs.zip(other.wheelPairs).all { (left, right) ->
        left.number == right.number &&
            left.axisNumber == right.axisNumber &&
            left.diameterLeft == right.diameterLeft &&
            left.diameterRight == right.diameterRight
    }
}

private fun List<ReferenceLocomotiveExportDto>.collapseReferenceLocomotives(): List<ReferenceLocomotiveExportDto> {
    return this
        .groupBy { it.referenceKey() }
        .values
        .mapNotNull { group ->
            group.maxWithOrNull(
                compareBy<ReferenceLocomotiveExportDto> { it.updatedAt }
                    .thenBy { it.deletedAt }
                    .thenBy { it.sortOrder },
            )
        }
        .sortedWith(
            compareBy<ReferenceLocomotiveExportDto> { it.sortOrder }
                .thenByDescending { it.updatedAt }
                .thenByDescending { it.deletedAt }
                .thenBy { it.series.trim().uppercase() }
                .thenBy { it.number.trim() },
        )
}
