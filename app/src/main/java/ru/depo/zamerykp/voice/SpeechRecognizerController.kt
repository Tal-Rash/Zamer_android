package ru.depo.zamerykp.voice

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import android.os.Handler
import android.os.Looper
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import ru.depo.zamerykp.domain.VoiceCommand
import ru.depo.zamerykp.domain.VoiceCommandParser
import java.io.File

class SpeechRecognizerController(
    private val onTextRecognized: (text: String, confidence: Float) -> Unit,
    private val onErrorMessage: (String) -> Unit,
    private val onRecognitionText: (text: String) -> Unit = {},
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val commandParser = VoiceCommandParser()
    private val mainHandler = Handler(Looper.getMainLooper())
    private var model: Model? = null
    private var recognizer: Recognizer? = null
    private var speechService: SpeechService? = null
    private val phraseBuffer = StringBuilder()
    @Volatile private var phraseStartedAt = 0L
    @Volatile private var flushScheduled = false
    @Volatile private var lastEmittedPhrase: String = ""
    @Volatile private var active = false

    suspend fun start(modelPath: String) {
        if (active) return
        if (modelPath.isBlank()) {
            onErrorMessage("Не выбрана папка модели Vosk")
            return
        }

        stop()
        runCatching {
            withContext(Dispatchers.IO) {
                val modelFile = File(modelPath)
                require(modelFile.exists()) { "Папка модели не найдена" }
                val loadedModel = Model(modelFile.absolutePath)
                val loadedRecognizer = Recognizer(loadedModel, SAMPLE_RATE)
                val loadedService = SpeechService(loadedRecognizer, SAMPLE_RATE)

                model = loadedModel
                recognizer = loadedRecognizer
                speechService = loadedService
                active = true

                loadedService.startListening(object : RecognitionListener {
                    override fun onPartialResult(hypothesis: String?) {
                        queuePartialHypothesis(hypothesis)
                    }

                    override fun onResult(hypothesis: String?) {
                        queueHypothesis(hypothesis)
                    }

                    override fun onFinalResult(hypothesis: String?) {
                        queueHypothesis(hypothesis)
                    }

                    override fun onError(exception: Exception?) {
                        if (!active) return
                        onErrorMessage(exception?.message ?: "Ошибка офлайн-распознавания")
                    }

                    override fun onTimeout() = Unit
                })
            }
        }.onFailure {
            onErrorMessage(it.message ?: "Не удалось запустить офлайн-распознавание")
            stop()
        }
    }

    fun stop() {
        active = false
        flushScheduled = false
        lastEmittedPhrase = ""
        mainHandler.removeCallbacksAndMessages(null)
        synchronized(phraseBuffer) { phraseBuffer.clear() }
        phraseStartedAt = 0L
        runCatching { speechService?.stop() }
        runCatching { (speechService as? AutoCloseable)?.close() }
        runCatching { (recognizer as? AutoCloseable)?.close() }
        runCatching { (model as? AutoCloseable)?.close() }
        speechService = null
        recognizer = null
        model = null
    }

    fun isActive(): Boolean = active

    fun destroy() {
        stop()
    }

    private fun extractText(hypothesis: String?): String? {
        if (hypothesis.isNullOrBlank()) return null
        return runCatching {
            val jsonObject = json.parseToJsonElement(hypothesis).jsonObject
            val text = jsonObject["text"]?.jsonPrimitive?.content?.trim().orEmpty()
            text.ifBlank {
                jsonObject["partial"]?.jsonPrimitive?.content?.trim()
            }
        }.getOrNull()
    }

    private fun queueHypothesis(hypothesis: String?) {
        if (!active) return
        val text = extractText(hypothesis).orEmpty().trim()
        if (text.isBlank()) return
        onRecognitionText(text)
        synchronized(phraseBuffer) {
            if (phraseBuffer.isBlank()) {
                phraseStartedAt = System.currentTimeMillis()
            }
            if (phraseBuffer.isNotBlank()) {
                phraseBuffer.append(' ')
            }
            phraseBuffer.append(text)
        }
        scheduleFlush()
    }

    private fun queuePartialHypothesis(hypothesis: String?) {
        if (!active) return
        val text = extractText(hypothesis).orEmpty().trim()
        if (text.isBlank()) return
        onRecognitionText(text)
        val normalized = text.lowercase().replace('ё', 'е').trim()
        if (!isStartWithSideCommand(normalized)) return
        if (normalized == lastEmittedPhrase) return
        lastEmittedPhrase = normalized
        synchronized(phraseBuffer) {
            phraseBuffer.clear()
            phraseStartedAt = 0L
        }
        mainHandler.removeCallbacksAndMessages(null)
        flushScheduled = false
        onTextRecognized(text, 1f)
    }

    private fun scheduleFlush() {
        if (flushScheduled || !active) return
        flushScheduled = true
        val delayMs = synchronized(phraseBuffer) {
            val value = phraseBuffer.toString().trim()
            when {
                value.isBlank() -> PHRASE_GAP_MS
                isCompleteValueCommand(value) -> VALUE_COMMAND_GAP_MS
                isStartVoiceCommand(value) -> START_COMMAND_GAP_MS
                isQuickVoiceCommand(value) -> QUICK_COMMAND_GAP_MS
                else -> PHRASE_GAP_MS
            }
        }
        mainHandler.postDelayed({
            flushScheduled = false
            val (phrase, ready) = synchronized(phraseBuffer) {
                val value = phraseBuffer.toString().trim()
                if (value.isBlank()) {
                    "" to false
                } else {
                    val numericCount = countNumericTokens(value)
                    val elapsed = System.currentTimeMillis() - phraseStartedAt
                    val isReady = numericCount >= MIN_NUMERIC_TOKENS ||
                        isCompleteValueCommand(value) ||
                        isQuickVoiceCommand(value) ||
                        elapsed >= MAX_BUFFER_MS
                    if (isReady) {
                        phraseBuffer.clear()
                        phraseStartedAt = 0L
                    }
                    value to isReady
                }
            }
            if (phrase.isNotBlank() && ready) {
                lastEmittedPhrase = phrase.lowercase().replace('ё', 'е').trim()
                onTextRecognized(phrase, 1f)
            } else if (phrase.isNotBlank()) {
                scheduleFlush()
            }
        }, delayMs)
    }

    private fun countNumericTokens(text: String): Int =
        text
            .split(Regex("\\s+"))
            .count { token -> token.any(Char::isDigit) }

    private fun isQuickVoiceCommand(text: String): Boolean {
        val value = text.lowercase().replace('ё', 'е')
        return value.contains("нов") ||
            value.contains("начать") ||
            value.contains("левая") ||
            value.contains("правая") ||
            value.contains("лево") ||
            value.contains("право") ||
            value.contains("следующ") ||
            value.contains("дальше") ||
            value.contains("закончить") ||
            value.contains("замеры окончены") ||
            value.startsWith("гребень") ||
            value.startsWith("прокат") ||
            value.startsWith("крутизна") ||
            value.startsWith("бандаж") ||
            Regex("(?<!\\S)\\d+\\s*(?:п[эе]|пэ|пе|э[лль]|эл|ел)\\b").containsMatchIn(value)
    }

    private fun isStartVoiceCommand(text: String): Boolean {
        val value = text.lowercase().replace('ё', 'е').trim()
        return value == "замер" || value.startsWith("замер ")
    }

    private fun isStartWithSideCommand(text: String): Boolean {
        val value = text.lowercase().replace('ё', 'е').trim()
        return value.startsWith("замер ") && (value.contains(" лев") || value.contains(" пра"))
    }

    private fun isCompleteValueCommand(text: String): Boolean {
        return when (commandParser.parse(text).command) {
            is VoiceCommand.FillCurrentSide,
            is VoiceCommand.FillSide -> true
            else -> false
        }
    }

    private companion object {
        const val SAMPLE_RATE = 16_000.0f
        const val START_COMMAND_GAP_MS = 90L
        const val VALUE_COMMAND_GAP_MS = 250L
        const val QUICK_COMMAND_GAP_MS = 220L
        const val PHRASE_GAP_MS = 700L
        const val MIN_NUMERIC_TOKENS = 4
        const val MAX_BUFFER_MS = 5_000L
    }
}
