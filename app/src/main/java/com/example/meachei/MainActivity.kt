package com.example.meachei

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    companion object {
        private const val CODIGO_PERMISSAO_GPS = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1. Configura os botões primeiro (para a interface carregar rápido)
        configurarBotoes()

        // 2. Inicia o fluxo de verificação de permissões
        verificarPermissaoLocalizacao()
    }

    private fun configurarBotoes() {
        val btnSalvar = findViewById<Button>(R.id.btnSalvarLocal)
        btnSalvar.setOnClickListener {
            // Abre a tela de editar, mas com ID -1 para indicar que é um NOVO local
            val intent = Intent(this, EditarLocalActivity::class.java)
            intent.putExtra("id", -1)
            intent.putExtra("nome", "") // Nome vazio para o usuário digitar
            startActivity(intent)
        }
        btnSalvar.contentDescription = "Botão Salvar novo local. Abre uma tela para dar nome e gravar sua posição atual."

        val btnListar = findViewById<Button>(R.id.btnLocaisSalvos)
        btnListar.setOnClickListener {
            startActivity(Intent(this, LocaisSalvosActivity::class.java))
        }

        val btnBuscar = findViewById<Button>(R.id.btnBuscarLocal)
        btnBuscar.setOnClickListener {
            startActivity(Intent(this, BuscarLocalActivity::class.java))
        }

        val btnOndeEstou = findViewById<Button>(R.id.btnOndeEstou)
        btnOndeEstou.setOnClickListener {
            startActivity(Intent(this, OndeEstouActivity::class.java))
        }

        val btnMaisOpcoes = findViewById<Button>(R.id.btnMaisOpcoes)
        btnMaisOpcoes.setOnClickListener {
            startActivity(Intent(this, MaisOpcoesActivity::class.java))
        }

        val btnFechar = findViewById<Button>(R.id.btnFechar)
        btnFechar.setOnClickListener {
            finish()
        }
    }

    private fun verificarPermissaoLocalizacao() {
        // Verifica se já temos a permissão ACCESS_FINE_LOCATION
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            // Se não tem, pede ao usuário
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                CODIGO_PERMISSAO_GPS
            )
        } else {
            // Se já tem permissão, segue para verificar a otimização de bateria
            verificarPermissaoBateria()
        }
    }

    // O Android chama esse método depois que o usuário clica em "Permitir" ou "Negar"
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == CODIGO_PERMISSAO_GPS) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permissão concedida! Agora verifica a bateria.
                Toast.makeText(this, "Permissão de localização concedida.", Toast.LENGTH_SHORT).show()
                verificarPermissaoBateria()
            } else {
                // Permissão negada. É crucial explicar para o usuário cego o impacto disso.
                Toast.makeText(
                    this,
                    "O aplicativo precisa de acesso à localização para funcionar. Por favor, reinicie e permita.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun verificarPermissaoBateria() {
        // Lógica original preservada: verifica se já mostramos a tela de ajuda da bateria
        val prefs = getSharedPreferences("config", Context.MODE_PRIVATE)
        val jaMostrouPermissao = prefs.getBoolean("bateria_permissao_exibida", false)

        if (!jaMostrouPermissao) {
            prefs.edit().putBoolean("bateria_permissao_exibida", true).apply()
            val intent = Intent(this, PermissaoBateriaActivity::class.java)
            startActivity(intent)
        }
    }
}