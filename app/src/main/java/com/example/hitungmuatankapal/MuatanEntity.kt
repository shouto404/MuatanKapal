package com.example.hitungmuatankapal

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "muatan")
data class MuatanEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val rute: String,              // ✅ NEW
    val tanggal: String,
    val kendaraan: String,
    val golongan: String,
    val deck: String,
    val ton: Int,
    val harga: Int
)