package ru.depo.zamerykp.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

class VoiceSpeaker(context: Context) : TextToSpeech.OnInitListener {
    private val appContext = context.applicationContext
    private var textToSpeech: TextToSpeech? = null
    private var ready = false
    private var pendingText: String? = null

    init {
        textToSpeech = TextToSpeech(appContext, this)
    }

    override fun onInit(status: Int) {
        val tts = textToSpeech ?: return
        ready = status == TextToSpeech.SUCCESS
        if (!ready) return
        val localeResult = tts.setLanguage(Locale("ru", "RU"))
        ready = localeResult != TextToSpeech.LANG_MISSING_DATA && localeResult != TextToSpeech.LANG_NOT_SUPPORTED
        if (!ready) return
        tts.setSpeechRate(0.95f)
        pendingText?.let {
            pendingText = null
            speak(it)
        }
    }

    fun speak(text: String) {
        speak(text, onDone = {})
    }

    fun speak(text: String, onDone: () -> Unit) {
        val normalized = text.trim()
        if (normalized.isBlank()) return
        val tts = textToSpeech
        if (!ready || tts == null) {
            pendingText = normalized
            return
        }
        val utteranceId = "voice_feedback_${System.currentTimeMillis()}"
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit
            override fun onDone(utteranceId: String?) = onDone()
            override fun onError(utteranceId: String?) = onDone()
        })
        tts.stop()
        tts.speak(normalized, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun release() {
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
        ready = false
        pendingText = null
    }
}
