package com.example.meachei

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast

class PermissaoBateriaActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        val btnPermissao = Button(this)
        btnPermissao.text = "Permitir execução em segundo plano"
        btnPermissao.contentDescription = "Botão para permitir que o aplicativo funcione com a tela desligada, ignorando economia de bateria"

        btnPermissao.setOnClickListener {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            val packageName = packageName

            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                intent.data = android.net.Uri.parse("package:$packageName")
                startActivity(intent)
            } else {
                Toast.makeText(this, "Permissão já concedida", Toast.LENGTH_SHORT).show()
            }
        }

        layout.addView(btnPermissao)
        setContentView(layout)
    }
}
