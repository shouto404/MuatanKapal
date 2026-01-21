package com.example.hitungmuatankapal

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface AppDao {

    // ===== Master Tarif =====
    @Query("SELECT * FROM kendaraan_tarif ORDER BY id ASC")
    suspend fun getAllTarif(): List<KendaraanTarifEntity>

    @Query("SELECT * FROM kendaraan_tarif WHERE kendaraan = :kendaraan AND golongan = :golongan LIMIT 1")
    suspend fun getTarifByKendaraanGol(kendaraan: String, golongan: String): KendaraanTarifEntity?

    // ===== Muatan =====
    @Query("SELECT * FROM muatan ORDER BY id DESC")
    suspend fun getAllMuatan(): List<MuatanEntity>

    @Insert
    suspend fun insertMuatan(m: MuatanEntity)

    @Query("DELETE FROM muatan")
    suspend fun clearMuatan()

    // ===== Kapasitas =====
    @Query("SELECT * FROM kapasitas WHERE `key` = :key LIMIT 1")
    suspend fun getKapasitas(key: String): KapasitasEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertKapasitas(k: KapasitasEntity)

    @Query("DELETE FROM kendaraan_tarif")
    suspend fun clearTarif()

    @Query("SELECT * FROM kendaraan_tarif WHERE rute = :rute ORDER BY urutan ASC")
    suspend fun getAllTarifByRute(rute: String): List<KendaraanTarifEntity>

    @Query("SELECT * FROM kendaraan_tarif WHERE rute = :rute AND kendaraan = :kendaraan AND golongan = :golongan LIMIT 1")
    suspend fun getTarifByRuteKendaraanGol(rute: String, kendaraan: String, golongan: String): KendaraanTarifEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTarifAll(list: List<KendaraanTarifEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTarif(t: KendaraanTarifEntity)

    @Query("DELETE FROM kendaraan_tarif WHERE rute = :rute")
    suspend fun clearTarifByRute(rute: String)

    @Query("SELECT * FROM muatan WHERE rute = :rute ORDER BY id DESC")
    suspend fun getAllMuatanByRute(rute: String): List<MuatanEntity>

    @Query("DELETE FROM muatan WHERE rute = :rute")
    suspend fun clearMuatanByRute(rute: String)

}