package com.example.alzeihmersapp

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import androidx.activity.ComponentActivity
import com.example.alzeihmersapp.adapter.RecordingsAdapter
import java.io.File

class RecordingsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recordings)

        val listView: ListView = findViewById(R.id.recordings_list)
        val backButton: Button = findViewById(R.id.button_back_to_main)

        val recordingsDir = File(filesDir, "recordings")
        val transcriptsDir = File(filesDir, "transcripts")
        val files = recordingsDir.listFiles()?.sortedBy { it.lastModified() } ?: emptyList()

        if (files.isEmpty()) {
            listView.adapter = ArrayAdapter(
                this,
                android.R.layout.simple_list_item_1,
                listOf("No recordings yet")
            )
        } else {
            listView.adapter = RecordingsAdapter(this, files, transcriptsDir) { transcriptFile ->
                val intent = Intent(this, TranscriptViewActivity::class.java)
                intent.putExtra(TranscriptViewActivity.EXTRA_TRANSCRIPT_PATH, transcriptFile.absolutePath)
                startActivity(intent)
            }
        }

        backButton.setOnClickListener {
            finish()
        }
    }
}
