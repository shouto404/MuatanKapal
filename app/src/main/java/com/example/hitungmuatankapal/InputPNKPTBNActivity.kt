package com.example.hitungmuatankapal

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.app.DatePickerDialog
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.Calendar

class InputPNKPTBNActivity : AppCompatActivity() {

    private val listMuatan = mutableListOf<Muatan>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_input_pnk_ptbn)

        val etTanggal = findViewById<EditText>(R.id.etTanggal)
        val spKendaraan = findViewById<Spinner>(R.id.spKendaraan)
        val btnTambah = findViewById<Button>(R.id.btnTambah)

        // ===== Spinner Data =====
        val pilihan = listOf(
            "Mobil - Gol IV",
            "Truk - Gol V"
        )

        spKendaraan.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            pilihan
        )

        // ===== Date Picker =====
        etTanggal.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(
                this,
                { _, y, m, d ->
                    etTanggal.setText("$d/${m + 1}/$y")
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        // ===== Tambah Muatan =====
        btnTambah.setOnClickListener {
            val tanggal = etTanggal.text.toString()
            val pilihanKendaraan = spKendaraan.selectedItem.toString()

            val muatan = when (pilihanKendaraan) {
                "Mobil - Gol IV" -> Muatan(tanggal, "Mobil", "Gol IV", 450000)
                "Truk - Gol V" -> Muatan(tanggal, "Truk", "Gol V", 750000)
                else -> null
            }

            if (muatan != null) {
                listMuatan.add(muatan)
                tampilkanTabel()
            }
        }
    }

    private fun tampilkanTabel() {
        val tabel = findViewById<TableLayout>(R.id.tableLayout)
        tabel.removeViews(1, tabel.childCount - 1)

        for (m in listMuatan) {
            val row = TableRow(this)
            row.addView(buatText(m.tanggal))
            row.addView(buatText(m.kendaraan))
            row.addView(buatText(m.golongan))
            row.addView(buatText("Rp ${m.harga}"))
            tabel.addView(row)
        }
    }

    private fun buatText(text: String): TextView {
        val tv = TextView(this)
        tv.text = text
        tv.setPadding(8, 8, 8, 8)
        return tv
    }
}
