package com.example.meachei

import android.app.Activity
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.TextView
import java.util.*
import kotlin.concurrent.thread

class OndeEstouActivity : Activity(), TextToSpeech.OnInitListener {

    private lateinit var tts: TextToSpeech
    private lateinit var txtEndereco: TextView
    private var ttsPronto = false
    private var textoPendente: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Configura a UI básica
        txtEndereco = TextView(this)
        txtEndereco.textSize = 24f
        txtEndereco.setPadding(32, 32, 32, 32)
        txtEndereco.text = "Obtendo localização..."
        txtEndereco.contentDescription = "Aguarde, buscando seu endereço atual."
        setContentView(txtEndereco)

        // Inicializa o sistema de voz
        tts = TextToSpeech(this, this)

        // Inicia o processo em segundo plano para não travar a tela
        buscarEndereco()
    }

    private fun buscarEndereco() {
        thread {
            try {
                val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
                var location: Location? = null

                // Tenta pegar a localização de qualquer provedor disponível (GPS ou Rede)
                val providers = lm.getProviders(true)
                for (provider in providers) {
                    try {
                        val l = lm.getLastKnownLocation(provider)
                        if (l != null) {
                            // Se achou uma localização mais recente ou mais precisa, usa ela
                            if (location == null || l.accuracy < location!!.accuracy) {
                                location = l
                            }
                        }
                    } catch (e: SecurityException) {
                        // Permissão não concedida
                    }
                }

                if (location != null) {
                    atualizarUi("Localização encontrada. Buscando nome da rua...")
                    
                    val geocoder = Geocoder(this, Locale.getDefault())
                    // Esse método pode demorar, por isso está dentro da thread
                    val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)

                    if (addresses != null && addresses.isNotEmpty()) {
                        val endereco = addresses[0]
                        // Monta o texto de forma segura (lidando com campos nulos)
                        val rua = endereco.thoroughfare ?: "Rua desconhecida"
                        val numero = endereco.subThoroughfare ?: "S/N"
                        val bairro = endereco.subLocality ?: endereco.locality ?: ""
                        
                        val textoFinal = "Você está na $rua, número $numero, $bairro."
                        
                        atualizarUi(textoFinal)
                        falarEndereco(textoFinal)
                    } else {
                        val msg = "Coordenadas encontradas, mas não foi possível achar o nome da rua."
                        atualizarUi(msg)
                        falarEndereco(msg)
                    }
                } else {
                    val msg = "Não foi possível obter sua localização atual. Verifique se o GPS está ligado."
                    atualizarUi(msg)
                    falarEndereco(msg)
                }

            } catch (e: Exception) {
                e.printStackTrace()
                val msg = "Ocorreu um erro ao buscar o endereço."
                atualizarUi(msg)
                falarEndereco(msg)
            }
        }
    }

    // Método auxiliar para atualizar a tela (sempre na Thread Principal)
    private fun atualizarUi(texto: String) {
        runOnUiThread {
            txtEndereco.text = texto
            txtEndereco.contentDescription = texto
        }
    }

    private fun falarEndereco(texto: String) {
        if (ttsPronto) {
            tts.speak(texto, TextToSpeech.QUEUE_FLUSH, null, null)
        } else {
            // Se o TTS ainda não carregou, guarda o texto para falar depois
            textoPendente = texto
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts.setLanguage(Locale("pt", "BR"))
            if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                ttsPronto = true
                // Se tinha alguma frase esperando, fala agora
                textoPendente?.let {
                    falarEndereco(it)
                    textoPendente = null
                }
            }
        }
    }

    override fun onDestroy() {
        if (ttsPronto) {
            tts.stop()
            tts.shutdown()
        }
        super.onDestroy()
    }
}