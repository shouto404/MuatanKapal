package com.example.hitungmuatankapal

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "kendaraan_tarif",
    indices = [Index(value = ["rute", "kendaraan", "golongan"], unique = true)]
)
data class KendaraanTarifEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val rute: String,              // ✅ NEW
    val urutan: Int,
    val kendaraan: String,
    val golongan: String,
    val ton: Int,
    val deck: String,
    val harga: Int
) {
    fun labelSpinner(): String = "$kendaraan - $golongan"
}