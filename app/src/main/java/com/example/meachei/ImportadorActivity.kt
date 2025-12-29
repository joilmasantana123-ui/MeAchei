package com.example.meachei

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.speech.tts.TextToSpeech
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.*

class ImportadorActivity : Activity(), TextToSpeech.OnInitListener {

    private lateinit var tts: TextToSpeech
    private lateinit var db: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)
        db = DatabaseHelper(this)

        val uri: Uri? = intent?.data
        if (uri != null) {
            val inputStream = contentResolver.openInputStream(uri)
            val reader = BufferedReader(InputStreamReader(inputStream))
            val conteudo = reader.readText()
            reader.close()

            val existentes = db.listarLocais()
            val novos = LocalSharingHelper().importarLocais(conteudo, existentes)
            for (local in novos) {
                db.inserirLocal(local.nome, local.latitude, local.longitude)
            }

            val mensagem = if (novos.isEmpty()) {
                "Nenhum local novo foi adicionado."
            } else {
                "${novos.size} locais adicionados com sucesso."
            }

            falar(mensagem)
        } else {
            falar("Nenhum arquivo recebido.")
        }

        finish()
    }

    private fun falar(texto: String) {
        tts.speak(texto, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale("pt", "BR")
        }
    }

    override fun onDestroy() {
        tts.shutdown()
        super.onDestroy()
    }
}
