package com.example.meachei

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class LocalSalvo(val id: Int, val nome: String, val latitude: Double, val longitude: Double)

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, "locais.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE locais (id INTEGER PRIMARY KEY AUTOINCREMENT, nome TEXT, latitude REAL, longitude REAL)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS locais")
        onCreate(db)
    }

    fun inserirLocal(nome: String, latitude: Double, longitude: Double): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("nome", nome)
            put("latitude", latitude)
            put("longitude", longitude)
        }
        return db.insert("locais", null, values)
    }

    fun listarLocais(): List<LocalSalvo> {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM locais", null)
        val lista = mutableListOf<LocalSalvo>()
        while (cursor.moveToNext()) {
            val id = cursor.getInt(0)
            val nome = cursor.getString(1)
            val lat = cursor.getDouble(2)
            val lon = cursor.getDouble(3)
            lista.add(LocalSalvo(id, nome, lat, lon))
        }
        cursor.close()
        return lista
    }

    fun deletarLocal(id: Int): Int {
        val db = writableDatabase
        return db.delete("locais", "id=?", arrayOf(id.toString()))
    }
}
