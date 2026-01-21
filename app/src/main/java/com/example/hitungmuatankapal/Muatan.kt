package com.example.hitungmuatankapal

data class Muatan(
    val tanggal: String,
    val kendaraan: String,
    val golongan: String,
    val deck: String,      // BARU
    val ton: Int,          // BARU
    val harga: Int
)