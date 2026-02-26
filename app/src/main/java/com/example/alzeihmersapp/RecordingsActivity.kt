package com.example.alzeihmersapp

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import androidx.activity.ComponentActivity
import java.io.File

class RecordingsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recordings)

        val listView: ListView = findViewById(R.id.recordings_list)
        val backButton: Button = findViewById(R.id.button_back_to_main)

        val recordingsDir = File(filesDir, "recordings")
        val files = recordingsDir.listFiles()?.sortedBy { it.lastModified() } ?: emptyList()

        val items = if (files.isEmpty()) {
            listOf("No recordings yet")
        } else {
            files.mapIndexed { index, _ -> "Recording ${index + 1}" }
        }

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            items
        )
        listView.adapter = adapter

        backButton.setOnClickListener {
            finish()
        }
    }
}

