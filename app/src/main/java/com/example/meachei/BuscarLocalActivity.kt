package com.example.meachei

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.View
import android.widget.*
import java.util.*

class BuscarLocalActivity : Activity() {

    private lateinit var edtBusca: EditText
    private lateinit var btnVoz: Button
    private lateinit var btnBuscar: Button
    private lateinit var listaResultados: LinearLayout
    private lateinit var db: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_buscar_local)

        edtBusca = findViewById(R.id.edtBusca)
        btnVoz = findViewById(R.id.btnVoz)
        btnBuscar = findViewById(R.id.btnBuscar)
        listaResultados = findViewById(R.id.listaResultados)
        db = DatabaseHelper(this)

        btnVoz.setOnClickListener {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            startActivityForResult(intent, 1)
        }

        btnBuscar.setOnClickListener {
            val termo = edtBusca.text.toString()
            if (termo.isNotEmpty()) {
                mostrarResultadosSimulados(termo)
            }
        }
    }

    private fun mostrarResultadosSimulados(termo: String) {
        listaResultados.removeAllViews()

        val simulados = listOf(
            Triple("$termo - Centro", -22.9000, -47.0600),
            Triple("$termo - Jardim Paulista", -22.9100, -47.0500),
            Triple("$termo - Vila Industrial", -22.9200, -47.0700)
        )

        for ((nome, lat, lon) in simulados) {
            val itemLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(8, 8, 8, 8)
            }

            val txt = TextView(this)
            txt.text = nome
            txt.contentDescription = "Resultado encontrado: $nome"

            val btnSalvar = Button(this)
            btnSalvar.text = "Salvar"
            btnSalvar.contentDescription = "Botão para salvar o local $nome"
            btnSalvar.setOnClickListener {
                db.inserirLocal(nome, lat, lon)
                Toast.makeText(this, "$nome salvo com sucesso", Toast.LENGTH_SHORT).show()
            }

            val btnMonitorar = Button(this)
            btnMonitorar.text = "Monitorar"
            btnMonitorar.contentDescription = "Botão para iniciar o monitoramento do local $nome"
            btnMonitorar.setOnClickListener {
                val intent = Intent(this, ProximityService::class.java)
                intent.putExtra("destino_lat", lat)
                intent.putExtra("destino_lon", lon)
                intent.putExtra("destino_nome", nome)
                startService(intent)
                Toast.makeText(this, "Monitorando $nome", Toast.LENGTH_SHORT).show()
            }

            itemLayout.addView(txt)
            itemLayout.addView(btnSalvar)
            itemLayout.addView(btnMonitorar)
            listaResultados.addView(itemLayout)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == RESULT_OK && data != null) {
            val resultado = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            edtBusca.setText(resultado?.get(0) ?: "")
        }
    }
}
