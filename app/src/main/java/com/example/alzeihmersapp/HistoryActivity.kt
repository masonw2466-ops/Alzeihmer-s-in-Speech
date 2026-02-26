package com.example.alzeihmersapp

import android.content.Intent
import android.os.Bundle
import android.widget.CheckBox
import android.widget.TextView
import androidx.activity.ComponentActivity

class HistoryActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        restoreAndBindCheckboxes()
        setupBottomNav()
    }

    private fun restoreAndBindCheckboxes() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

        val checkBoxIds = listOf(
            R.id.check_hypertension,
            R.id.check_heart_disease,
            R.id.check_stroke,
            R.id.check_heart_attack,
            R.id.check_atrial_fibrillation,
            R.id.check_coronary_artery_disease,
            R.id.check_head_injury,
            R.id.check_seizures,
            R.id.check_parkinsons,
            R.id.check_multiple_sclerosis,
            R.id.check_diabetes,
            R.id.check_obesity,
            R.id.check_high_cholesterol,
            R.id.check_metabolic_syndrome,
            R.id.check_depression,
            R.id.check_anxiety,
            R.id.check_bipolar,
            R.id.check_schizophrenia,
            R.id.check_ptsd,
            R.id.check_sleep_apnea,
            R.id.check_insomnia,
            R.id.check_sleep_disorders,
            R.id.check_thyroid_disease,
            R.id.check_kidney_disease,
            R.id.check_liver_disease,
            R.id.check_cancer,
            R.id.check_arthritis,
            R.id.check_osteoporosis,
            R.id.check_vision_problems,
            R.id.check_hearing_loss,
            R.id.check_family_alzheimers,
            R.id.check_family_dementia,
            R.id.check_family_diabetes,
            R.id.check_family_heart_disease,
            R.id.check_family_stroke,
            R.id.check_smoking,
            R.id.check_alcohol_abuse,
            R.id.check_drug_abuse,
            R.id.check_sedentary_lifestyle,
            R.id.check_down_syndrome,
            R.id.check_mild_cognitive_impairment,
            R.id.check_vascular_dementia,
            R.id.check_other
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

        navHistory.isSelected = true

        navMain.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }

        navMedical.setOnClickListener {
            startActivity(Intent(this, MedicalActivity::class.java))
        }

        navHistory.setOnClickListener {
            // Already on history page
        }

        navSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    companion object {
        private const val PREFS_NAME = "alzeihmers_prefs"
    }
}
