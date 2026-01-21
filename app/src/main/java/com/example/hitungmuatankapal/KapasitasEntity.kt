package com.example.hitungmuatankapal

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "kapasitas")
data class KapasitasEntity(
    @PrimaryKey val key: String = "PNK_PTBN", // bisa dibedakan per rute
    val upper: Int,
    val lower: Int,
    val sisaUpper: Int,
    val sisaLower: Int,
    val isLocked: Boolean
)