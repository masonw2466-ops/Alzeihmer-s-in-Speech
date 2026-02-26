package com.example.alzeihmersapp

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.activity.ComponentActivity

class MedicalActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_medical)

        setupBottomNav()
    }

    private fun setupBottomNav() {
        val navMain: TextView = findViewById(R.id.nav_main)
        val navMedical: TextView = findViewById(R.id.nav_medical)
        val navHistory: TextView = findViewById(R.id.nav_history)
        val navSettings: TextView = findViewById(R.id.nav_settings)

        navMedical.isSelected = true

        navMain.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }

        navMedical.setOnClickListener {
            // Already on medical page
        }

        navHistory.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }

        navSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }
}
