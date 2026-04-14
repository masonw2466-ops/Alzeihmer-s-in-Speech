package com.example.alzeihmersapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.example.alzeihmersapp.speech.AudioTranscriber
import com.example.alzeihmersapp.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        // Load Vosk model in the background so it's ready when recording stops
        AudioTranscriber.initModel(this,
            onReady = { /* model loaded — transcription will work */ },
            onError = { e ->
                Log.e("MainActivity", "Vosk model init failed", e)
                runOnUiThread {
                    Toast.makeText(this,
                        "Model error: ${e.javaClass.simpleName}: ${e.message}",
                        Toast.LENGTH_LONG).show()
                }
            }
        )

        val recordButton: Button = findViewById(R.id.button_record)
        val viewRecordingsButton: Button = findViewById(R.id.button_view_recordings)

        viewModel.isRecording.observe(this) { isRecording ->
            updateRecordingUi(recordButton, isRecording)
        }

        recordButton.setOnClickListener {
            if (viewModel.isRecording.value != true) {
                if (hasRecordPermission()) {
                    viewModel.startRecording()
                } else {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.RECORD_AUDIO),
                        REQUEST_RECORD_AUDIO_PERMISSION
                    )
                }
            } else {
                viewModel.stopRecording()
            }
        }

        viewRecordingsButton.setOnClickListener {
            startActivity(Intent(this, RecordingsActivity::class.java))
        }

        updateRecordingInfo()
        setupBottomNav()
    }

    override fun onResume() {
        super.onResume()
        updateRecordingInfo()
    }

    private fun updateRecordingInfo() {
        val countText: TextView = findViewById(R.id.text_recording_count)
        val lastText: TextView = findViewById(R.id.text_last_recording)

        val recordingsDir = File(filesDir, "recordings")
        val files = recordingsDir.listFiles()?.sortedByDescending { it.lastModified() }
            ?: emptyList()

        countText.text = "${files.size} recording${if (files.size != 1) "s" else ""}"

        if (files.isNotEmpty()) {
            val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            lastText.text = "Last: ${dateFormat.format(Date(files[0].lastModified()))}"
        } else {
            lastText.text = "No recordings yet"
        }
    }

    private fun updateRecordingUi(button: Button, isRecording: Boolean) {
        if (isRecording) {
            button.isSelected = true
            button.text = "Recording..."
        } else {
            button.isSelected = false
            button.text = "Record"
        }
    }

    private fun hasRecordPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                viewModel.startRecording()
            } else {
                Toast.makeText(this, "Microphone permission is required to record audio.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        viewModel.stopIfRecording()
    }

    private fun setupBottomNav() {
        val navMain: TextView = findViewById(R.id.nav_main)
        val navMedical: TextView = findViewById(R.id.nav_medical)
        val navHistory: TextView = findViewById(R.id.nav_history)
        val navSettings: TextView = findViewById(R.id.nav_settings)

        navMain.isSelected = true

        navMain.setOnClickListener {
            // Already on main page
        }

        navMedical.setOnClickListener {
            startActivity(Intent(this, MedicalActivity::class.java))
        }

        navHistory.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }

        navSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    companion object {
        private const val REQUEST_RECORD_AUDIO_PERMISSION = 1001
    }
}
