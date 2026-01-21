package com.example.hitungmuatankapal

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        KendaraanTarifEntity::class,
        MuatanEntity::class,
        KapasitasEntity::class
    ],
    version = 2 // ✅ naikkan
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): AppDao

    companion object {
        // ...

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // tambah kolom rute dengan default "PNK_PTBN" biar data lama aman
                db.execSQL("ALTER TABLE kendaraan_tarif ADD COLUMN rute TEXT NOT NULL DEFAULT 'PNK_PTBN'")

                // buat index unik untuk mencegah duplikat
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_kendaraan_tarif_rute_kendaraan_golongan ON kendaraan_tarif(rute, kendaraan, golongan)")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return Room.databaseBuilder(context, AppDatabase::class.java, "app_db")
                .addMigrations(MIGRATION_1_2) // ✅
                .build()
        }
    }
}