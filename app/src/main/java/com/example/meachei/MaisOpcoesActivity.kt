package com.example.meachei

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout

class MaisOpcoesActivity : Activity() {

    private lateinit var btnToggleVolumeKey: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("config", Context.MODE_PRIVATE)
        val ativo = prefs.getBoolean("volume_key_enabled", false)

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL

        btnToggleVolumeKey = Button(this)
        atualizarTextoBotao(ativo)

        btnToggleVolumeKey.setOnClickListener {
            val novoEstado = !prefs.getBoolean("volume_key_enabled", false)
            prefs.edit().putBoolean("volume_key_enabled", novoEstado).apply()
            atualizarTextoBotao(novoEstado)
        }

        layout.addView(btnToggleVolumeKey)
        setContentView(layout)
    }

    private fun atualizarTextoBotao(ativo: Boolean) {
        if (ativo) {
            btnToggleVolumeKey.text = "Desativar recurso da tecla de volume com a tela desligada"
            btnToggleVolumeKey.contentDescription = "Botão para desativar o recurso de localização com a tecla de volume com a tela desligada"
        } else {
            btnToggleVolumeKey.text = "Ativar recurso da tecla de volume com a tela desligada"
            btnToggleVolumeKey.contentDescription = "Botão para ativar o recurso de localização com a tecla de volume com a tela desligada"
        }
    }
}
