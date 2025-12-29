package com.example.meachei

import android.app.*
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.*
import android.speech.tts.TextToSpeech
import androidx.core.app.NotificationCompat
import java.util.*

class ProximityService : Service(), LocationListener, TextToSpeech.OnInitListener {

    private var locationManager: LocationManager? = null
    private lateinit var tts: TextToSpeech
    private lateinit var vibrator: Vibrator
    private var ttsPronto = false

    // Dados do destino atual
    private var destinoLat = 0.0
    private var destinoLon = 0.0
    private var nomeDestino = ""
    
    // Controle de alertas para não repetir fala
    private var alertado = mutableSetOf<Int>()
    private val locaisSalvos = mutableListOf<LocalSalvo>()

    companion object {
        private const val CHANNEL_ID = "monitoramento_channel"
        private const val NOTIFICATION_ID = 123
        private const val PREFS_NAME = "monitoramento_prefs"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        // Inicializa TTS
        tts = TextToSpeech(this, this)
        
        vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
        
        // Carrega locais do banco para verificar se passamos perto de algum
        val db = DatabaseHelper(this)
        locaisSalvos.addAll(db.listarLocais())
        
        iniciarNotificacaoForeground()
    }

    // Callback do TTS
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts.setLanguage(Locale("pt", "BR"))
            ttsPronto = !(result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Se intent vier com dados, é um novo comando. Se vier nulo, é o Android reiniciando o serviço.
        if (intent != null && intent.hasExtra("destino_lat")) {
            destinoLat = intent.getDoubleExtra("destino_lat", 0.0)
            destinoLon = intent.getDoubleExtra("destino_lon", 0.0)
            nomeDestino = intent.getStringExtra("destino_nome") ?: "destino desconhecido"
            
            // Salva no SharedPreferences para persistência em caso de reinício
            salvarEstadoMonitoramento()
            
            // Reseta alertas anteriores pois é um novo destino
            alertado.clear()
            falar("Iniciando monitoramento para $nomeDestino")
        } else {
            // Recupera dados salvos se o serviço foi reiniciado pelo sistema
            recuperarEstadoMonitoramento()
        }

        iniciarLocalizacao()
        
        // START_STICKY diz ao Android para recriar o serviço se ele for morto por falta de memória
        return START_STICKY
    }

    private fun iniciarLocalizacao() {
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        try {
            // Verifica permissão (embora a Activity já deva ter pedido, é bom garantir no fluxo)
            locationManager?.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 2f, this)
        } catch (ex: SecurityException) {
            falar("Erro de permissão de GPS.")
            stopSelf()
        } catch (ex: Exception) {
            falar("Erro ao acessar GPS.")
        }
    }

    private fun salvarEstadoMonitoramento() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        with(prefs.edit()) {
            putFloat("lat", destinoLat.toFloat())
            putFloat("lon", destinoLon.toFloat())
            putString("nome", nomeDestino)
            apply()
        }
    }

    private fun recuperarEstadoMonitoramento() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        destinoLat = prefs.getFloat("lat", 0f).toDouble()
        destinoLon = prefs.getFloat("lon", 0f).toDouble()
        nomeDestino = prefs.getString("nome", "") ?: ""
    }

    private fun iniciarNotificacaoForeground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(
                CHANNEL_ID,
                "Monitoramento de Proximidade",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(canal)
        }

        // Intent para abrir o app ao clicar na notificação
        val intentApp = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intentApp, PendingIntent.FLAG_IMMUTABLE)

        val notificacao = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Monitorando Localização")
            .setContentText("Destino: $nomeDestino")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation) // Ícone padrão, substitua pelo seu se tiver
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        startForeground(NOTIFICATION_ID, notificacao)
    }

    override fun onLocationChanged(location: Location) {
        // Se não houver destino configurado, ignora
        if (destinoLat == 0.0 && destinoLon == 0.0) return

        val resultado = FloatArray(1)
        Location.distanceBetween(location.latitude, location.longitude, destinoLat, destinoLon, resultado)
        val distanciaMetros = resultado[0].toInt()

        // Lógica de avisos de distância para o destino
        verificarProximidadeDestino(distanciaMetros)

        // Lógica de "Passando perto de..."
        verificarLocaisSalvos(location)
    }

    private fun verificarProximidadeDestino(distancia: Int) {
        // Chegou (menos de 30m)
        if (distancia <= 30 && !alertado.contains(30)) {
            vibrar()
            falar("Você chegou ao destino: $nomeDestino.")
            // Removemos as atualizações e paramos o serviço
            locationManager?.removeUpdates(this)
            stopSelf()
            return
        }
        
        // Avisos escalonados
        val limites = listOf(100, 300, 500, 1000)
        for (limite in limites) {
            if (distancia <= limite && !alertado.contains(limite)) {
                // Se é o aviso de 100m, vibra também
                if (limite == 100) vibrar()
                
                val texto = if (limite >= 1000) {
                     "Falta 1 quilômetro." // simplificado
                } else {
                     "Faltam $limite metros."
                }
                
                falar(texto)
                alertado.add(limite)
                
                // Adiciona limites maiores ao set para não falar "Faltam 500m" se já estivermos em 300m e pulamos o aviso
                alertado.addAll(limites.filter { it > limite })
                break 
            }
        }
    }

    private fun verificarLocaisSalvos(locationAtual: Location) {
        for (local in locaisSalvos) {
            // Não avisa se o local salvo for o próprio destino
            if (local.latitude == destinoLat && local.longitude == destinoLon) continue
            
            val res = FloatArray(1)
            Location.distanceBetween(locationAtual.latitude, locationAtual.longitude, local.latitude, local.longitude, res)
            val dist = res[0].toInt()

            // Se passar entre 50m e 150m de um local salvo
            if (dist in 50..150) {
                // Usamos um ID alto para diferenciar dos alertas de distância (ex: 10001)
                val chaveAlerta = local.id + 100000 
                if (!alertado.contains(chaveAlerta)) {
                    vibrar()
                    falar("Passando perto de ${local.nome}, à sua ${calcularDirecao(locationAtual, local)}.")
                    alertado.add(chaveAlerta)
                }
            } else {
                // Se afastar, remove o alerta para poder avisar de novo se voltar
                val chaveAlerta = local.id + 100000
                if (dist > 200) {
                    alertado.remove(chaveAlerta)
                }
            }
        }
    }
    
    // Extra: tenta dizer a direção aproximada (Opcional, mas útil)
    private fun calcularDirecao(origem: Location, destino: LocalSalvo): String {
        val bearing = origem.bearingTo(Location("").apply { 
            latitude = destino.latitude 
            longitude = destino.longitude 
        })
        // Simplificação: apenas direita/esquerda/frente não é preciso sem bússola do dispositivo,
        // então retornamos vazio ou apenas "próximo" para não confundir, ou mantemos simples.
        return "volta" // Placeholder. Pode ser removido ou aprimorado.
    }

    private fun falar(texto: String) {
        if (ttsPronto) {
            // QUEUE_ADD para não cortar frases anteriores se houver muitos avisos
            tts.speak(texto, TextToSpeech.QUEUE_ADD, null, null)
        }
    }

    private fun vibrar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(1000, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(1000)
        }
    }

    override fun onDestroy() {
        locationManager?.removeUpdates(this)
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
        super.onDestroy()
    }

    // Métodos obrigatórios da interface LocationListener que não usaremos agora
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}
}