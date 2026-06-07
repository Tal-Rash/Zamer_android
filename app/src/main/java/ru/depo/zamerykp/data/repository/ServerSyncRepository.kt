package ru.depo.zamerykp.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ru.depo.zamerykp.domain.ImportPayload
import ru.depo.zamerykp.domain.ReferenceDataExportDto
import ru.depo.zamerykp.domain.ReferenceLocomotiveExportDto
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
        val serverReference = pullReferenceDto(baseUrl, cookie)
        val localReference = exportRepository.buildReferenceExport()
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
                localLocomotive.deletedAt > 0L && serverLocomotive.deletedAt <= 0L && localLocomotive.updatedAt >= serverLocomotive.updatedAt -> {
                    referenceToUpload += localLocomotive
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
            if (serverMap[localLocomotive.referenceKey()] == null) {
                referenceToUpload += localLocomotive
            }
        }
        val referencePushed = if (referenceToUpload.isNotEmpty()) {
            val payload = localReference.copy(
                exportedAt = serverReference.exportedAt,
                locomotives = referenceToUpload.distinctBy { it.referenceKey() },
            )
            postJson("$baseUrl/zamer-kp/api/phone-import", cookie, json.encodeToString(payload))
            payload.locomotives.size
        } else {
            0
        }
        val pending = measurementRepository.getPendingMeasurements()
        var pendingPushed = 0
        for (item in pending) {
            val measurementDto = exportRepository.buildExport(item.measurementId)
            postJson("$baseUrl/zamer-kp/api/phone-import", cookie, json.encodeToString(measurementDto))
            pendingPushed += 1
        }

        val pulledArchive = pullArchiveData(
            baseUrl,
            cookie,
            serverReference.locomotives.map { it.referenceKey() }.toSet(),
        )

        ServerSyncResult(
            referencePushed = referencePushed,
            archivePushed = 0,
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
        val referenceDto = exportRepository.buildReferenceExport()
        if (referenceDto.locomotives.isEmpty()) return@withContext 0
        postJson("$baseUrl/zamer-kp/api/phone-import", cookie, json.encodeToString(referenceDto))
        referenceDto.locomotives.size
    }

    private fun normalizeBaseUrl(value: String): String =
        value.trim()
            .trimEnd('/')
            .removeSuffix("/zamer-kp")

    private fun login(baseUrl: String, password: String): String {
        val connection = (URL("$baseUrl/login").openConnection() as HttpURLConnection).apply {
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
            ?.firstOrNull { it.contains("grafik_ppr_session=") }
            ?.substringBefore(';')
            ?: throw IllegalStateException("Не удалось получить веб-сессию (код $responseCode)")
        runCatching { connection.inputStream.close() }
        connection.disconnect()
        return cookie
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
}

private fun ReferenceLocomotiveExportDto.referenceKey(): String =
    "${series.trim().uppercase()}|${number.trim()}"

private fun ReferenceLocomotiveExportDto.referenceEquals(other: ReferenceLocomotiveExportDto): Boolean {
    if (series.trim().uppercase() != other.series.trim().uppercase()) return false
    if (number.trim() != other.number.trim()) return false
    if (wheelPairCount != other.wheelPairCount) return false
    if (deletedAt != other.deletedAt) return false
    if (wheelPairs.size != other.wheelPairs.size) return false
    return wheelPairs.zip(other.wheelPairs).all { (left, right) ->
        left.number == right.number &&
            left.axisNumber == right.axisNumber &&
            left.diameterLeft == right.diameterLeft &&
            left.diameterRight == right.diameterRight
    }
}
