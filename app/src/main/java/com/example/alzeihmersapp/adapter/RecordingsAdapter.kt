package com.example.alzeihmersapp.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.TextView
import com.example.alzeihmersapp.R
import java.io.File

class RecordingsAdapter(
    context: Context,
    private val recordings: List<File>,
    private val transcriptsDir: File,
    private val onViewTranscript: (File) -> Unit
) : ArrayAdapter<File>(context, 0, recordings) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_recording, parent, false)

        val recording = recordings[position]
        val nameText: TextView = view.findViewById(R.id.text_recording_name)
        val transcriptButton: Button = view.findViewById(R.id.button_view_transcript)

        nameText.text = "Recording ${position + 1}"

        val transcriptFile = File(transcriptsDir, recording.nameWithoutExtension + ".txt")
        val hasTranscript = transcriptFile.exists()

        transcriptButton.isEnabled = hasTranscript
        transcriptButton.alpha = if (hasTranscript) 1.0f else 0.4f

        transcriptButton.setOnClickListener {
            if (hasTranscript) {
                onViewTranscript(transcriptFile)
            }
        }

        return view
    }
}
