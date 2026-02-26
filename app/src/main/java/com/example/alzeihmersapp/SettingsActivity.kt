package com.example.alzeihmersapp

import android.content.Intent
import android.os.Bundle
import android.widget.CheckBox
import android.widget.TextView
import androidx.activity.ComponentActivity

class SettingsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        restoreAndBindCheckboxes()
        setupBottomNav()
    }

    private fun restoreAndBindCheckboxes() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val checkBoxIds = listOf(
            R.id.check_monitor_phone_calls,
            R.id.check_continuous_listening,
            R.id.check_smart_accessories,
            R.id.check_outgoing_texts
        )

        checkBoxIds.forEach { id ->
            val checkBox: CheckBox = findViewById(id)
            val key = "checkbox_" + resources.getResourceEntryName(id)
            checkBox.isChecked = prefs.getBoolean(key, false)
            checkBox.setOnCheckedChangeListener { _, isChecked ->
                prefs.edit().putBoolean(key, isChecked).apply()
            }
        }
    }

    private fun setupBottomNav() {
        val navMain: TextView = findViewById(R.id.nav_main)
        val navMedical: TextView = findViewById(R.id.nav_medical)
        val navHistory: TextView = findViewById(R.id.nav_history)
        val navSettings: TextView = findViewById(R.id.nav_settings)

        navSettings.isSelected = true

        navMain.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }

        navMedical.setOnClickListener {
            startActivity(Intent(this, MedicalActivity::class.java))
        }

        navHistory.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }

        navSettings.setOnClickListener {
            // Already on settings page
        }
    }

    companion object {
        private const val PREFS_NAME = "alzeihmers_prefs"
    }
}
