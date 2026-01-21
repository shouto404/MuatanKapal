package com.example.hitungmuatankapal

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnPNKPTBN = findViewById<Button>(R.id.btnPNKPTBN)
        val btnPTBNPNK = findViewById<Button>(R.id.btnPTBNPNK)

        // ==============================
        // TUJUAN PNK - PTBN
        // ==============================
        btnPNKPTBN.setOnClickListener {
            val intent = Intent(this, InputPNKPTBNActivity::class.java)
            startActivity(intent)
        }

        // ==============================
        // TUJUAN PTBN - PNK
        // (sementara placeholder)
        // ==============================
        btnPTBNPNK.setOnClickListener {
            val intent = Intent(this, InputPTBNPNKActivity::class.java)
            startActivity(intent)
        }
    }
}