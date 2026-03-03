package com.example.alzeihmersapp

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import java.io.File

class TranscriptViewActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transcript_view)

        val contentText: TextView = findViewById(R.id.transcript_content)
        val backButton: Button = findViewById(R.id.button_back)

        val filePath = intent.getStringExtra(EXTRA_TRANSCRIPT_PATH)
        if (filePath != null) {
            val file = File(filePath)
            contentText.text = if (file.exists()) {
                file.readText()
            } else {
                getString(R.string.transcript_not_found)
            }
        } else {
            contentText.text = getString(R.string.transcript_not_found)
        }

        backButton.setOnClickListener { finish() }
    }

    companion object {
        const val EXTRA_TRANSCRIPT_PATH = "transcript_path"
    }
}
