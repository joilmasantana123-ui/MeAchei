package com.example.meachei

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.SystemClock

class VolumeButtonReceiver : BroadcastReceiver() {

    companion object {
        private var ultimoToque: Long = 0
        private var contadorToques = 0
        // Tempo máximo entre cliques para considerar uma sequência (ex: 2 cliques)
        private const val JANELA_TEMPO_CLIQUES = 800L 
    }

    override fun onReceive(context: Context, intent: Intent) {
        // Verifica se a função está ativada nas configurações
        val prefs = context.getSharedPreferences("config", Context.MODE_PRIVATE)
        val ativo = prefs.getBoolean("volume_key_enabled", false)
        if (!ativo) return

        // Filtra apenas alterações de volume (ignora outros broadcasts se houver)
        if (intent.action != "android.media.VOLUME_CHANGED_ACTION") return

        val eventTime = SystemClock.elapsedRealtime()

        // Se passou muito tempo desde o último toque, reseta o contador
        if (eventTime - ultimoToque > JANELA_TEMPO_CLIQUES) {
            contadorToques = 0
        }

        contadorToques++
        ultimoToque = eventTime

        // Prepara o Intent para o serviço que vai falar
        val serviceIntent = Intent(context, VolumeActionService::class.java)
        serviceIntent.putExtra("toques", contadorToques)

        try {
            // Tenta iniciar o serviço. 
            // Em Android 8+, startService pode falhar se o app estiver em background,
            // mas como o ProximityService (Foreground) deve estar rodando quando o usuário
            // está navegando, isso geralmente funciona.
            context.startService(serviceIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}