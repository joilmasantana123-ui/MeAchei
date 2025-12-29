package com.example.meachei

import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.IBinder
import android.speech.tts.TextToSpeech
import java.util.*
import kotlin.concurrent.thread

class VolumeActionService : Service(), TextToSpeech.OnInitListener {

    private lateinit var tts: TextToSpeech
    private var ttsPronto = false
    private var acaoPendente: Int? = null

    override fun onCreate() {
        super.onCreate()
        // Inicializa o TTS assim que o serviço é criado
        tts = TextToSpeech(this, this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val toques = intent?.getIntExtra("toques", 0) ?: 0

        if (ttsPronto) {
            // Se o TTS já está pronto, executa a ação imediatamente
            processarComando(toques)
        } else {
            // Se o TTS ainda está carregando, guarda o comando para executar no onInit
            acaoPendente = toques
        }

        // START_NOT_STICKY: Se o sistema matar o serviço, não precisa reiniciar sozinho 
        // (pois depende do clique do botão)
        return START_NOT_STICKY
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts.setLanguage(Locale("pt", "BR"))
            if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                ttsPronto = true
                
                // Verifica se ficou alguma ação pendente enquanto carregava
                acaoPendente?.let {
                    processarComando(it)
                    acaoPendente = null
                }
            }
        }
    }

    private fun processarComando(toques: Int) {
        // Obtém a última localização conhecida
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        var location: Location? = null
        
        try {
            // Tenta obter do GPS ou Rede
            val providers = locationManager.getProviders(true)
            for (provider in providers) {
                val l = locationManager.getLastKnownLocation(provider)
                if (l != null) {
                    if (location == null || l.accuracy < location.accuracy) {
                        location = l
                    }
                }
            }
        } catch (e: SecurityException) {
            falar("Permissão de localização não concedida.")
            return
        }

        if (location == null) {
            falar("Não foi possível obter sua localização atual. Verifique o GPS.")
            return
        }

        // Executa a lógica baseada no número de toques
        if (toques == 1) {
            // 1 Toque: Onde estou? (Requer Geocoder / Internet)
            falarLocalAtual(location)
        } else if (toques >= 2) {
            // 2 Toques ou mais: Qual a distância para o destino? (Cálculo matemático simples)
            falarDistanciaDestino(location)
        }
    }

    private fun falarLocalAtual(location: Location) {
        // Geocoder precisa rodar fora da thread principal para não engasgar o áudio
        thread {
            try {
                val geocoder = Geocoder(this, Locale("pt", "BR"))
                val enderecos = geocoder.getFromLocation(location.latitude, location.longitude, 1)

                if (enderecos != null && enderecos.isNotEmpty()) {
                    val e = enderecos[0]
                    // Texto otimizado para leitor de tela/TTS
                    val texto = "Você está na ${e.thoroughfare ?: "rua desconhecida"}, número ${e.subThoroughfare ?: "sem número"}, bairro ${e.subLocality ?: ""}."
                    falar(texto)
                } else {
                    falar("Endereço não encontrado, mas as coordenadas foram obtidas.")
                }
            } catch (e: Exception) {
                falar("Erro de conexão. Não foi possível buscar o nome da rua.")
            }
        }
    }

    private fun falarDistanciaDestino(location: Location) {
        // Recupera o destino salvo no SharedPreferences (mesmo arquivo usado pelo ProximityService)
        val prefs = getSharedPreferences("monitoramento_prefs", Context.MODE_PRIVATE)
        val destinoLat = prefs.getFloat("lat", 0f).toDouble()
        val destinoLon = prefs.getFloat("lon", 0f).toDouble()
        val nomeDestino = prefs.getString("nome", null)

        if (destinoLat == 0.0 && destinoLon == 0.0) {
            falar("Nenhum destino está sendo monitorado no momento.")
        } else {
            val resultado = FloatArray(1)
            Location.distanceBetween(location.latitude, location.longitude, destinoLat, destinoLon, resultado)
            val metros = resultado[0].toInt()

            val texto = if (metros >= 1000) {
                val km = metros / 1000.0
                String.format("Faltam %.1f quilômetros para %s", km, nomeDestino ?: "o destino")
            } else {
                "Faltam $metros metros para ${nomeDestino ?: "o destino"}."
            }
            falar(texto)
        }
    }

    private fun falar(texto: String) {
        if (ttsPronto) {
            // QUEUE_FLUSH interrompe a fala anterior para dar prioridade à informação nova
            tts.speak(texto, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    override fun onDestroy() {
        if (ttsPronto) {
            tts.stop()
            tts.shutdown()
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}