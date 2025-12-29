package com.example.meachei

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Button
import android.widget.Toast

class LocaisSalvosActivity : Activity() {

    private lateinit var lista: LinearLayout
    private lateinit var db: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_locais_salvos)

        db = DatabaseHelper(this)
        lista = findViewById(R.id.listaLocais)
        val locais = db.listarLocais()
        lista.removeAllViews()

        for (local in locais) {
            val item = TextView(this)
            item.text = "${local.nome} (Lat: ${local.latitude}, Lon: ${local.longitude})"
            item.setPadding(16, 16, 16, 16)
            item.contentDescription = "Local salvo: ${local.nome}"

            item.setOnClickListener {
                val intent = Intent(this, ProximityService::class.java)
                intent.putExtra("destino_lat", local.latitude)
                intent.putExtra("destino_lon", local.longitude)
                intent.putExtra("destino_nome", local.nome)
                startService(intent)
                Toast.makeText(this, "Monitorando ${local.nome}", Toast.LENGTH_SHORT).show()
            }

            item.setOnLongClickListener {
                val popup = LinearLayout(this)
                popup.orientation = LinearLayout.VERTICAL

                val editar = Button(this)
                editar.text = "Editar"
                editar.setOnClickListener {
                    val intent = Intent(this, EditarLocalActivity::class.java)
                    intent.putExtra("id", local.id)
                    intent.putExtra("nome", local.nome)
                    startActivity(intent)
                }

                val excluir = Button(this)
                excluir.text = "Excluir"
                excluir.setOnClickListener {
                    db.deletarLocal(local.id)
                    recreate()
                }

                popup.addView(editar)
                popup.addView(excluir)

                val toast = Toast(this)
                toast.view = popup
                toast.duration = Toast.LENGTH_LONG
                toast.show()
                true
            }

            lista.addView(item)
        }

        val btnCompartilhar = Button(this)
        btnCompartilhar.text = "Compartilhar Locais"
        btnCompartilhar.setOnClickListener {
            Toast.makeText(this, "Compartilhamento ainda será finalizado", Toast.LENGTH_SHORT).show()
        }

        val btnVoltar = Button(this)
        btnVoltar.text = "Voltar"
        btnVoltar.setOnClickListener {
            finish()
        }

        lista.addView(btnCompartilhar)
        lista.addView(btnVoltar)
    }
}
