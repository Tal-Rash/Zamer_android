package ru.depo.zamerykp.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ru.depo.zamerykp.domain.ImportPayload
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
        val referenceDto = exportRepository.buildReferenceExport()
        postJson("$baseUrl/zamer-kp/api/phone-import", cookie, json.encodeToString(referenceDto))

        val archiveDto = exportRepository.buildArchiveExport()
        postJson("$baseUrl/zamer-kp/api/phone-import", cookie, json.encodeToString(archiveDto))

        val pending = measurementRepository.getPendingMeasurements()
        var pendingPushed = 0
        for (item in pending) {
            val measurementDto = exportRepository.buildExport(item.measurementId)
            postJson("$baseUrl/zamer-kp/api/phone-import", cookie, json.encodeToString(measurementDto))
            pendingPushed += 1
        }

        val pulledReference = pullReferenceData(baseUrl, cookie)
        val pulledArchive = pullArchiveData(baseUrl, cookie)

        ServerSyncResult(
            referencePushed = referenceDto.locomotives.size,
            archivePushed = archiveDto.archive.size,
            pendingPushed = pendingPushed,
            referencePulled = pulledReference,
            archivePulled = pulledArchive,
        )
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

    private suspend fun pullReferenceData(baseUrl: String, cookie: String): Int {
        val text = getText("$baseUrl/zamer-kp/api/phone-export?kind=reference&format=json", cookie)
        val payload = exportRepository.parseImportPayload(text)
        return when (payload) {
            is ImportPayload.ReferenceData -> {
                measurementRepository.importReferenceData(payload.dto, importLocomotives = true, importWheelPairs = true)
            }
            is ImportPayload.Measurement, is ImportPayload.ArchiveData -> 0
        }
    }

    private suspend fun pullArchiveData(baseUrl: String, cookie: String): Int {
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
