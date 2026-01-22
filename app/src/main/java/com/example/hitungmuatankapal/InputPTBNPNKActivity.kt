package com.example.hitungmuatankapal

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import android.content.ContentValues
import android.os.Environment
import android.provider.MediaStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class InputPTBNPNKActivity : AppCompatActivity() {

    private var sisaUpperDeck = 0
    private var sisaLowerDeck = 0
    private var kapasitasDiSet = false

    private val listMuatan = mutableListOf<MuatanEntity>()

    private lateinit var db: AppDatabase
    private lateinit var dao: AppDao

    private lateinit var etTanggal: EditText
    private lateinit var spKendaraan: Spinner
    private lateinit var btnTambah: Button
    private lateinit var etUpper: EditText
    private lateinit var etLower: EditText
    private lateinit var btnSetKapasitas: Button
    private lateinit var btnHapusMuatan: Button
    private lateinit var btnResetKapasitas: Button
    private lateinit var btnTambahKendaraan: Button
    private lateinit var btnExportCsv: Button
    private val keyRute = "PTBN_PNK"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_input_ptbn_pnk)

        db = AppDatabase.getInstance(this)
        dao = db.dao()

        etTanggal = findViewById(R.id.etTanggal)
        spKendaraan = findViewById(R.id.spKendaraan)
        btnTambah = findViewById(R.id.btnTambah)
        etUpper = findViewById(R.id.etUpperDeck)
        etLower = findViewById(R.id.etLowerDeck)
        btnSetKapasitas = findViewById(R.id.btnSetKapasitas)
        btnHapusMuatan = findViewById(R.id.btnHapusMuatan)
        btnResetKapasitas = findViewById(R.id.btnResetKapasitas)
        btnTambahKendaraan = findViewById(R.id.btnTambahKendaraan)
        btnExportCsv = findViewById<Button>(R.id.btnExportCsv)

        etTanggal.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(
                this,
                { _, y, m, d -> etTanggal.setText("$d/${m + 1}/$y") },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        lifecycleScope.launch {
            seedTarifJikaKosong_PTBN_PNK()
            loadTarifKeSpinner()
            loadKapasitas()
            loadMuatan()
            updateTampilanKapasitas()
        }

        btnSetKapasitas.setOnClickListener {
            if (etLower.text.isNullOrBlank()) {
                Toast.makeText(this, "Lower Deck wajib diisi", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val lower = etLower.text.toString().toInt()
            val upper = if (etUpper.text.isNullOrBlank()) 0 else etUpper.text.toString().toInt()

            sisaLowerDeck = lower
            sisaUpperDeck = upper
            kapasitasDiSet = true

            etUpper.isEnabled = false
            etLower.isEnabled = false
            btnSetKapasitas.isEnabled = false

            updateTampilanKapasitas()

            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    dao.upsertKapasitas(
                        KapasitasEntity(
                            key = keyRute,
                            upper = upper,
                            lower = lower,
                            sisaUpper = sisaUpperDeck,
                            sisaLower = sisaLowerDeck,
                            isLocked = true
                        )
                    )
                }
            }

            Toast.makeText(this, "Kapasitas diset: Lower $lower ton, Upper $upper ton", Toast.LENGTH_SHORT).show()
        }

        btnHapusMuatan.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Hapus Data Tabel")
                .setMessage("Yakin ingin menghapus semua data muatan di tabel rute ini?")
                .setPositiveButton("YA") { _, _ ->
                    lifecycleScope.launch {
                        withContext(Dispatchers.IO) {
                            dao.clearMuatanByRute(keyRute)
                            // kalau belum punya per rute: dao.clearMuatan()
                        }
                        listMuatan.clear()
                        tampilkanTabel()
                        Toast.makeText(this@InputPTBNPNKActivity, "Data tabel muatan dihapus", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("BATAL", null)
                .show()
        }

        btnExportCsv.setOnClickListener {
            lifecycleScope.launch {
                val data = withContext(Dispatchers.IO) {
                    dao.getAllMuatanByRute(keyRute)
                }

                if (data.isEmpty()) {
                    Toast.makeText(this@InputPTBNPNKActivity, "Data masih kosong", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val success = withContext(Dispatchers.IO) {
                    exportMuatanToCsv(
                        rute = keyRute,
                        muatan = data
                    )
                }

                if (success) {
                    Toast.makeText(this@InputPTBNPNKActivity, "CSV tersimpan di Downloads", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@InputPTBNPNKActivity, "Gagal export CSV", Toast.LENGTH_SHORT).show()
                }
            }
        }

        btnResetKapasitas.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Reset Kapasitas")
                .setMessage("Reset kapasitas dan buka kembali input kapasitas?")
                .setPositiveButton("YA") { _, _ ->
                    lifecycleScope.launch {
                        kapasitasDiSet = false
                        sisaUpperDeck = 0
                        sisaLowerDeck = 0

                        etUpper.isEnabled = true
                        etLower.isEnabled = true
                        btnSetKapasitas.isEnabled = true

                        etUpper.setText("")
                        etLower.setText("")

                        updateTampilanKapasitas()

                        withContext(Dispatchers.IO) {
                            dao.upsertKapasitas(
                                KapasitasEntity(
                                    key = keyRute,
                                    upper = 0,
                                    lower = 0,
                                    sisaUpper = 0,
                                    sisaLower = 0,
                                    isLocked = false
                                )
                            )
                        }

                        Toast.makeText(this@InputPTBNPNKActivity, "Kapasitas di-reset. Silakan set lagi.", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("BATAL", null)
                .show()
        }

        btnTambahKendaraan.setOnClickListener { showDialogTambahKendaraan() }

        btnTambah.setOnClickListener {
            val tanggal = etTanggal.text.toString().trim()

            if (!kapasitasDiSet) {
                Toast.makeText(this, "Set kapasitas kapal terlebih dahulu", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (tanggal.isBlank()) {
                Toast.makeText(this, "Tanggal harus dipilih", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val label = spKendaraan.selectedItem?.toString() ?: ""
            if (label.isBlank()) return@setOnClickListener

            val parts = label.split(" - ")
            if (parts.size != 2) {
                Toast.makeText(this, "Format pilihan tidak valid", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val kendaraan = parts[0].trim()
            val golongan = parts[1].trim()

            lifecycleScope.launch {
                val tarif = withContext(Dispatchers.IO) {
                    dao.getTarifByRuteKendaraanGol(keyRute, kendaraan, golongan)
                }

                if (tarif == null) {
                    Toast.makeText(this@InputPTBNPNKActivity, "Tarif tidak ditemukan untuk rute ini", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val deckTerpilih = pilihDeckSesuaiTarif(tarif.deck, tarif.ton)
                if (deckTerpilih == null) {
                    Toast.makeText(this@InputPTBNPNKActivity, "Kapasitas tidak cukup (Lower & Upper penuh)", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                if (deckTerpilih == "Lower Deck") sisaLowerDeck -= tarif.ton else sisaUpperDeck -= tarif.ton

                val muatan = MuatanEntity(
                    rute = keyRute,
                    tanggal = tanggal,
                    kendaraan = tarif.kendaraan,
                    golongan = tarif.golongan,
                    deck = deckTerpilih,
                    ton = tarif.ton,
                    harga = tarif.harga
                )

                val upperCap = if (etUpper.text.isNullOrBlank()) 0 else etUpper.text.toString().toInt()
                val lowerCap = if (etLower.text.isNullOrBlank()) 0 else etLower.text.toString().toInt()

                withContext(Dispatchers.IO) {
                    dao.insertMuatan(muatan)
                    dao.upsertKapasitas(
                        KapasitasEntity(
                            key = keyRute,
                            upper = upperCap,
                            lower = lowerCap,
                            sisaUpper = sisaUpperDeck,
                            sisaLower = sisaLowerDeck,
                            isLocked = true
                        )
                    )
                }

                listMuatan.add(0, muatan)
                tampilkanTabel()
                updateTampilanKapasitas()
            }
        }
    }

    private fun pilihDeckSesuaiTarif(deckPrioritas: String, ton: Int): String? {
        return when (deckPrioritas) {
            "Lower Deck" -> when {
                sisaLowerDeck >= ton -> "Lower Deck"
                sisaUpperDeck >= ton -> "Upper Deck"
                else -> null
            }
            "Upper Deck" -> when {
                sisaUpperDeck >= ton -> "Upper Deck"
                sisaLowerDeck >= ton -> "Lower Deck"
                else -> null
            }
            else -> null
        }
    }

    private fun exportMuatanToCsv(
        rute: String,
        muatan: List<MuatanEntity>
    ): Boolean {
        return try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "Muatan_${rute}_$timeStamp.csv"

            fun esc(value: String): String {
                // CSV escape
                val v = value.replace("\"", "\"\"")
                return "\"$v\""
            }

            val csv = buildString {
                // HEADER (sesuai tabel aplikasi)
                append("Tanggal,Kendaraan,Golongan,Deck,Ton,Harga\n")

                // DATA
                for (m in muatan) {
                    append(
                        listOf(
                            esc(m.tanggal),
                            esc(m.kendaraan),
                            esc(m.golongan),
                            esc(m.deck),
                            m.ton.toString(),
                            m.harga.toString()
                        ).joinToString(",")
                    )
                    append("\n")
                }
            }

            val resolver = contentResolver
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }

            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: return false

            resolver.openOutputStream(uri).use { out ->
                if (out == null) return false
                out.write(csv.toByteArray(Charsets.UTF_8))
                out.flush()
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun showDialogTambahKendaraan() {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 20, 40, 0)
        }

        val etNama = EditText(this).apply { hint = "Nama Kendaraan" }
        val etGol = EditText(this).apply { hint = "Golongan (contoh: Gol IV A)" }
        val etTon = EditText(this).apply {
            hint = "Ton"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }
        val spDeck = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@InputPTBNPNKActivity,
                android.R.layout.simple_spinner_dropdown_item,
                listOf("Lower Deck", "Upper Deck")
            )
        }
        val etHarga = EditText(this).apply {
            hint = "Harga"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }

        container.addView(etNama)
        container.addView(etGol)
        container.addView(etTon)
        container.addView(spDeck)
        container.addView(etHarga)

        AlertDialog.Builder(this)
            .setTitle("Tambah Kendaraan (Rute $keyRute)")
            .setView(container)
            .setPositiveButton("SIMPAN") { _, _ ->
                val nama = etNama.text.toString().trim()
                val gol = etGol.text.toString().trim()
                val ton = etTon.text.toString().trim().toIntOrNull() ?: 0
                val deck = spDeck.selectedItem.toString()
                val harga = etHarga.text.toString().trim().toIntOrNull() ?: 0

                if (nama.isBlank() || gol.isBlank() || ton <= 0 || harga <= 0) {
                    Toast.makeText(this, "Data belum valid", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                lifecycleScope.launch {
                    val nextUrutan = withContext(Dispatchers.IO) {
                        dao.getAllTarifByRute(keyRute).size + 1
                    }
                    withContext(Dispatchers.IO) {
                        dao.insertTarif(
                            KendaraanTarifEntity(
                                urutan = nextUrutan,
                                rute = keyRute,
                                kendaraan = nama,
                                golongan = gol,
                                ton = ton,
                                deck = deck,
                                harga = harga
                            )
                        )
                    }
                    loadTarifKeSpinner()
                    Toast.makeText(this@InputPTBNPNKActivity, "Kendaraan ditambahkan untuk rute $keyRute", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("BATAL", null)
            .show()
    }

    private suspend fun seedTarifJikaKosong_PTBN_PNK() {
        val existing = withContext(Dispatchers.IO) { dao.getAllTarifByRute(keyRute) }
        if (existing.isNotEmpty()) return

        val seed = listOf(
            KendaraanTarifEntity(
                urutan = 1,
                rute = keyRute,
                kendaraan = "Mobil",
                golongan = "Gol IV A",
                ton = 5,
                deck = "Lower Deck",
                harga = 3409550
            ),
            KendaraanTarifEntity(
                urutan = 2,
                rute = keyRute,
                kendaraan = "Mobil",
                golongan = "Gol IV B",
                ton = 5,
                deck = "Lower Deck", // kalau mau default Upper, ganti "Upper Deck"
                harga = 4454550
            ),
            KendaraanTarifEntity(
                urutan = 3,
                rute = keyRute,
                kendaraan = "TRUK ENGKEL/DUMP/CHASIS/ELF",
                golongan = "Gol V A",
                ton = 10,
                deck = "Lower Deck", // kalau mau default Upper, ganti "Upper Deck"
                harga = 11894475
            ),
            KendaraanTarifEntity(
                urutan = 4,
                rute = keyRute,
                kendaraan = "TRUCK SEDANG BAK BESI/KAYU",
                golongan = "Gol V B",
                ton = 10,
                deck = "Lower Deck", // kalau mau default Upper, ganti "Upper Deck"
                harga = 12236475
            ),
            KendaraanTarifEntity(
                urutan = 5,
                rute = keyRute,
                kendaraan = "TRUK SEDANG/BUS SEDANG (7MUP)",
                golongan = "Gol V C",
                ton = 10,
                deck = "Lower Deck", // kalau mau default Upper, ganti "Upper Deck"
                harga = 12880500
            ),
            KendaraanTarifEntity(
                urutan = 6,
                rute = keyRute,
                kendaraan = "TRUCK BESAR DUMP/TRUCK CHASIS",
                golongan = "Gol VI A",
                ton = 20,
                deck = "Lower Deck", // kalau mau default Upper, ganti "Upper Deck"
                harga = 14245725
            ),
            KendaraanTarifEntity(
                urutan = 7,
                rute = keyRute,
                kendaraan = "TRUCK BESAR BAK BESI/KAYU",
                golongan = "Gol VI B",
                ton = 20,
                deck = "Lower Deck", // kalau mau default Upper, ganti "Upper Deck"
                harga = 15034367
            ),
            KendaraanTarifEntity(
                urutan = 8,
                rute = keyRute,
                kendaraan = "TRUCK/BUS BESAR (10M UP)",
                golongan = "Gol VI C",
                ton = 20,
                deck = "Lower Deck", // kalau mau default Upper, ganti "Upper Deck"
                harga = 17281117
            ),
            KendaraanTarifEntity(
                urutan = 9,
                rute = keyRute,
                kendaraan = "TRUCK TRONTON DUMP/CHASIS R10",
                golongan = "Gol VII A",
                ton = 25,
                deck = "Lower Deck", // kalau mau default Upper, ganti "Upper Deck"
                harga = 19788690
            ),
            KendaraanTarifEntity(
                urutan = 10,
                rute = keyRute,
                kendaraan = "TRUCK TRONTON BAK BESI/KAYU R10",
                golongan = "Gol VII B",
                ton = 25,
                deck = "Lower Deck", // kalau mau default Upper, ganti "Upper Deck"
                harga = 20453690
            ),
            KendaraanTarifEntity(
                urutan = 11,
                rute = keyRute,
                kendaraan = "TRUCK TRONTON/BUS R10",
                golongan = "Gol VIII",
                ton = 25,
                deck = "Lower Deck", // kalau mau default Upper, ganti "Upper Deck"
                harga = 37002690
            ),
            KendaraanTarifEntity(
                urutan = 12,
                rute = keyRute,
                kendaraan = "MUATAN MOBIL",
                golongan = "Gol IV A",
                ton = 10,
                deck = "Lower Deck", // kalau mau default Upper, ganti "Upper Deck"
                harga = 3979550
            ),
            KendaraanTarifEntity(
                urutan = 13,
                rute = keyRute,
                kendaraan = "MUATAN MOBIL",
                golongan = "Gol IV B",
                ton = 10,
                deck = "Lower Deck", // kalau mau default Upper, ganti "Upper Deck"
                harga = 5024550
            ),
            KendaraanTarifEntity(
                urutan = 14,
                rute = keyRute,
                kendaraan = " MUATAN TRUK ENGKEL/DUMP/CHASIS/ELF",
                golongan = "Gol V A",
                ton = 10,
                deck = "Lower Deck", // kalau mau default Upper, ganti "Upper Deck"
                harga = 14048600
            ),
            KendaraanTarifEntity(
                urutan = 15,
                rute = keyRute,
                kendaraan = "MUATAN TRUCK SEDANG BAK BESI/KAYU",
                golongan = "Gol V B",
                ton = 10,
                deck = "Lower Deck", // kalau mau default Upper, ganti "Upper Deck"
                harga = 14048600
            ),
            KendaraanTarifEntity(
                urutan = 16,
                rute = keyRute,
                kendaraan = " MUATAN TRUK SEDANG/BUS SEDANG (7MUP)",
                golongan = "Gol V C",
                ton = 10,
                deck = "Lower Deck", // kalau mau default Upper, ganti "Upper Deck"
                harga = 17900850
            ),
            KendaraanTarifEntity(
                urutan = 17,
                rute = keyRute,
                kendaraan = "MUATAN TRUCK BESAR DUMP/TRUCK CHASIS",
                golongan = "Gol VI A",
                ton = 20,
                deck = "Lower Deck", // kalau mau default Upper, ganti "Upper Deck"
                harga = 17136860
            ),
            KendaraanTarifEntity(
                urutan = 18,
                rute = keyRute,
                kendaraan = "MUATAN TRUCK BESAR BAK BESI/KAYU",
                golongan = "Gol VI B",
                ton = 20,
                deck = "Lower Deck", // kalau mau default Upper, ganti "Upper Deck"
                harga = 18756610
            ),
            KendaraanTarifEntity(
                urutan = 19,
                rute = keyRute,
                kendaraan = "MUATAN TRUCK/BUS BESAR (10M UP)",
                golongan = "Gol VI C",
                ton = 20,
                deck = "Lower Deck", // kalau mau default Upper, ganti "Upper Deck"
                harga = 22377250
            ),
            KendaraanTarifEntity(
                urutan = 20,
                rute = keyRute,
                kendaraan = "MUATAN TRUCK TRONTON DUMP/CHASIS R10",
                golongan = "Gol VII A",
                ton = 25,
                deck = "Lower Deck", // kalau mau default Upper, ganti "Upper Deck"
                harga = 24318860
            ),
            KendaraanTarifEntity(
                urutan = 21,
                rute = keyRute,
                kendaraan = "MUATAN TRUCK TRONTON BAK BESI/KAYU R10",
                golongan = "Gol VII B",
                ton = 25,
                deck = "Lower Deck", // kalau mau default Upper, ganti "Upper Deck"
                harga = 25545000
            ),
            KendaraanTarifEntity(
                urutan = 22,
                rute = keyRute,
                kendaraan = "MUATAN TRUCK TRONTON/BUS R10",
                golongan = "Gol VIII",
                ton = 25,
                deck = "Lower Deck", // kalau mau default Upper, ganti "Upper Deck"
                harga = 38565250
            )
        )

        withContext(Dispatchers.IO) { dao.insertTarifAll(seed) }
    }

    private suspend fun loadTarifKeSpinner() {
        val tarif = withContext(Dispatchers.IO) { dao.getAllTarifByRute(keyRute) }
        val labels = tarif.map { it.labelSpinner() }
        spKendaraan.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, labels)
    }

    private suspend fun loadKapasitas() {
        val kap = withContext(Dispatchers.IO) { dao.getKapasitas(keyRute) }
        if (kap == null) {
            kapasitasDiSet = false
            sisaUpperDeck = 0
            sisaLowerDeck = 0
            etUpper.isEnabled = true
            etLower.isEnabled = true
            btnSetKapasitas.isEnabled = true
            return
        }

        kapasitasDiSet = kap.isLocked
        sisaUpperDeck = kap.sisaUpper
        sisaLowerDeck = kap.sisaLower

        etUpper.setText(kap.upper.toString())
        etLower.setText(kap.lower.toString())

        etUpper.isEnabled = !kap.isLocked
        etLower.isEnabled = !kap.isLocked
        btnSetKapasitas.isEnabled = !kap.isLocked
    }

    private suspend fun loadMuatan() {
        val all = withContext(Dispatchers.IO) {
            dao.getAllMuatanByRute(keyRute)
            // kalau belum punya per rute: dao.getAllMuatan()
        }
        listMuatan.clear()
        listMuatan.addAll(all)
        tampilkanTabel()
    }

    private fun updateTampilanKapasitas() {
        findViewById<TextView>(R.id.tvSisaUpper).text = "Sisa Upper Deck: $sisaUpperDeck ton"
        findViewById<TextView>(R.id.tvSisaLower).text = "Sisa Lower Deck: $sisaLowerDeck ton"
    }

    private fun tampilkanTabel() {
        val tabel = findViewById<TableLayout>(R.id.tableLayout)
        while (tabel.childCount > 1) tabel.removeViewAt(1)

        for (m in listMuatan) {
            val row = TableRow(this)
            row.addView(buatText(m.tanggal))
            row.addView(buatText(m.kendaraan))
            row.addView(buatText(m.golongan))
            row.addView(buatText(m.deck))
            row.addView(buatText("Rp ${m.harga}"))
            tabel.addView(row)
        }
    }

    private fun buatText(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            setPadding(8, 8, 8, 8)
        }
    }
}

