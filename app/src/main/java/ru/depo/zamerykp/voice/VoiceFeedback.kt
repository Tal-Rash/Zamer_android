package ru.depo.zamerykp.voice

import android.media.AudioManager
import android.media.ToneGenerator

class VoiceFeedback {
    private val toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 85)

    fun signalRecognized() {
        toneGenerator.startTone(ToneGenerator.TONE_PROP_ACK, 100)
    }

    fun signalNeedsConfirmation() {
        toneGenerator.startTone(ToneGenerator.TONE_PROP_NACK, 120)
    }

    fun release() {
        toneGenerator.release()
    }
}
