package com.example.meachei

import android.content.*
import android.media.AudioManager
import android.os.*
import android.speech.tts.TextToSpeech
import android.widget.Toast
import android.location.Location
import android.location.LocationManager
import java.util.*

class VolumeKeyReceiver : BroadcastReceiver() {

    private var lastPressTime = 0L
    private var pressCount = 0
    private var handler: Handler? = null
    private var contextRef: Context? = null

    override fun onReceive(context: Context, intent: Intent) {
        contextRef = context

        val prefs = context.getSharedPreferences("config", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("volume_key_enabled", false)) return

        val keyAction = intent.getIntExtra("key_action", -1)
        val keyCode = intent.getIntExtra("key_code", -1)

        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP && keyAction == KeyEvent.ACTION_DOWN) {
            val power = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            if (power.isInteractive) return  // só executa se a tela estiver desligada

            val now = System.currentTimeMillis()
            if (now - lastPressTime > 1000) {
                pressCount = 0
            }
            pressCount++
            lastPressTime = now

            handler?.removeCallbacksAndMessages(null)
            handler = Handler(Looper.getMainLooper())
            handler?.postDelayed({
                if (pressCount == 1) {
                    informarLocalizacao()
                } else if (pressCount == 2) {
                    informarDistancia()
                }
                pressCount = 0
            }, 700)
        }
    }

    private fun informarLocalizacao() {
        val context = contextRef ?: return
        val tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val locManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                val providers = locManager.getProviders(true)
                var location: Location? = null

                for (provider in providers) {
                    location = locManager.getLastKnownLocation(provider)
                    if (location != null) break
                }

                if (location != null) {
                    val geocoder = Geocoder(context, Locale.getDefault())
                    val result = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                    if (!result.isNullOrEmpty()) {
                        val endereco = result[0]
                        val texto = "Você está na rua ${endereco.thoroughfare}, número ${endereco.subThoroughfare}, bairro ${endereco.subLocality}, ${endereco.locality}"
                        tts.language = Locale("pt", "BR")
                        tts.speak(texto, TextToSpeech.QUEUE_FLUSH, null, null)
                    }
                } else {
                    tts.speak("Localização não disponível", TextToSpeech.QUEUE_FLUSH, null, null)
                }
            }
        }
    }

    private fun informarDistancia() {
        val context = contextRef ?: return
        val prefs = context.getSharedPreferences("monitoramento", Context.MODE_PRIVATE)
        val lat = prefs.getFloat("dest_lat", 0f).toDouble()
        val lon = prefs.getFloat("dest_lon", 0f).toDouble()
        val nome = prefs.getString("dest_nome", "")

        val tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                if (lat == 0.0 && lon == 0.0) {
                    tts.speak("O serviço de monitoramento não está ativo", TextToSpeech.QUEUE_FLUSH, null, null)
                } else {
                    val locManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                    val providers = locManager.getProviders(true)
                    var location: Location? = null
                    for (provider in providers) {
                        location = locManager.getLastKnownLocation(provider)
                        if (location != null) break
                    }

                    if (location != null) {
                        val resultado = FloatArray(1)
                        Location.distanceBetween(location.latitude, location.longitude, lat, lon, resultado)
                        val km = resultado[0] / 1000
                        val texto = "Faltam %.1f quilômetros para o destino: %s".format(km, nome)
                        tts.language = Locale("pt", "BR")
                        tts.speak(texto, TextToSpeech.QUEUE_FLUSH, null, null)
                    } else {
                        tts.speak("Localização não disponível", TextToSpeech.QUEUE_FLUSH, null, null)
                    }
                }
            }
        }
    }
}
