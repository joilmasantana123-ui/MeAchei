package com.example.meachei

import android.content.Context
import java.io.File

class LocalSharingHelper {

    fun exportarLocais(context: Context, locais: List<LocalSalvo>): File {
        val file = File(context.getExternalFilesDir(null), "locais.meachei")
        file.writeText(locais.joinToString("\n") { "\${it.nome},\${it.latitude},\${it.longitude}" })
        return file
    }

    fun importarLocais(conteudo: String, existentes: List<LocalSalvo>): List<LocalSalvo> {
        val novos = mutableListOf<LocalSalvo>()
        val existentesCoords = existentes.map { Pair(it.latitude, it.longitude) }.toSet()

        for (linha in conteudo.lines()) {
            val partes = linha.split(",")
            if (partes.size == 3) {
                val nome = partes[0]
                val lat = partes[1].toDoubleOrNull()
                val lon = partes[2].toDoubleOrNull()
                if (lat != null && lon != null) {
                    val coord = Pair(lat, lon)
                    if (!existentesCoords.contains(coord)) {
                        novos.add(LocalSalvo(0, nome, lat, lon))
                    }
                }
            }
        }
        return novos
    }
}
