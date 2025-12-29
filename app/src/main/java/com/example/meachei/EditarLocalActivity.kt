package com.example.meachei

import android.app.Activity
import android.os.Bundle
import android.widget.*
import android.content.Intent

class EditarLocalActivity : Activity() {

    private lateinit var edtNome: EditText
    private lateinit var btnSalvar: Button
    private lateinit var db: DatabaseHelper
    private var localId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editar_local)

        edtNome = findViewById(R.id.edtNomeEditar)
        btnSalvar = findViewById(R.id.btnSalvarEdicao)
        db = DatabaseHelper(this)

        val nome = intent.getStringExtra("nome")
        localId = intent.getIntExtra("id", -1)

        edtNome.setText(nome)

        btnSalvar.setOnClickListener {
            val novoNome = edtNome.text.toString()
            if (novoNome.isNotEmpty() && localId != -1) {
                db.writableDatabase.execSQL("UPDATE locais SET nome=? WHERE id=?", arrayOf(novoNome, localId))
                Toast.makeText(this, "Local atualizado", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}
