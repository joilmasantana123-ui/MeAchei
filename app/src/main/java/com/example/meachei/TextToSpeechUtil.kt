package com.example.meachei

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.*

class TextToSpeechUtil(context: Context) {
    private val tts = TextToSpeech(context) { status ->
        if (status != TextToSpeech.ERROR) {
            tts.language = Locale("pt", "BR")
        }
    }

    fun speak(text: String) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    fun shutdown() {
        tts.shutdown()
    }
}
