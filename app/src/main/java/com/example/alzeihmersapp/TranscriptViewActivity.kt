package com.example.alzeihmersapp

import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import java.io.File
import java.io.IOException

class TranscriptViewActivity : ComponentActivity() {

    private var mediaPlayer: MediaPlayer? = null
    private var isAudioPlaying = false

    private lateinit var playPauseButton: Button
    private lateinit var seekBar: SeekBar
    private lateinit var progressText: TextView
    private lateinit var toggleTranscriptButton: Button
    private lateinit var transcriptContent: TextView

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var updateSeekBarRunnable: Runnable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transcript_view)

        playPauseButton = findViewById(R.id.button_play_pause)
        seekBar = findViewById(R.id.seek_bar_audio)
        progressText = findViewById(R.id.text_audio_progress)
        toggleTranscriptButton = findViewById(R.id.button_toggle_transcript)
        transcriptContent = findViewById(R.id.transcript_content)
        val backButton: Button = findViewById(R.id.button_back)

        // Accept the recording file path and derive the transcript path from it
        val recordingPath = intent.getStringExtra(EXTRA_RECORDING_PATH)

        var audioFilePath: String? = null
        var transcriptText: String? = null

        if (recordingPath != null) {
            val recordingFile = File(recordingPath)
            if (recordingFile.exists()) {
                audioFilePath = recordingPath
            }

            // Derive transcript path: recordings/foo.m4a -> transcripts/foo.txt
            val transcriptPath = recordingPath
                .replace("recordings", "transcripts")
                .replace(".m4a", ".txt")
            val transcriptFile = File(transcriptPath)
            if (transcriptFile.exists()) {
                transcriptText = transcriptFile.readText()
            }
        }

        // Load transcript if available, otherwise show a message
        if (transcriptText.isNullOrBlank()) {
            transcriptContent.text = getString(R.string.transcript_not_found)
        } else {
            transcriptContent.text = transcriptText
        }

        // Setup Audio Player
        if (audioFilePath != null) {
            setupAudioPlayer(audioFilePath)
        } else {
            playPauseButton.isEnabled = false
            seekBar.isEnabled = false
            Toast.makeText(this, "Audio file not found.", Toast.LENGTH_SHORT).show()
        }

        playPauseButton.setOnClickListener { togglePlayback() }

        toggleTranscriptButton.setOnClickListener {
            if (transcriptContent.visibility == View.GONE) {
                transcriptContent.visibility = View.VISIBLE
                toggleTranscriptButton.text = "Hide Transcription"
            } else {
                transcriptContent.visibility = View.GONE
                toggleTranscriptButton.text = "Show Transcription"
            }
        }

        backButton.setOnClickListener { finish() }
    }

    private fun setupAudioPlayer(audioPath: String) {
        mediaPlayer = MediaPlayer().apply {
            try {
                setDataSource(audioPath)
                prepare()

                seekBar.max = duration
                updateProgressText(0, duration)

                setOnCompletionListener {
                    playPauseButton.text = "Play"
                    isAudioPlaying = false
                    seekBar.progress = 0
                    seekTo(0)
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    mediaPlayer?.seekTo(progress)
                    mediaPlayer?.duration?.let { updateProgressText(progress, it) }
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        updateSeekBarRunnable = object : Runnable {
            override fun run() {
                mediaPlayer?.let { player ->
                    if (player.isPlaying) {
                        val currentPosition = player.currentPosition
                        seekBar.progress = currentPosition
                        updateProgressText(currentPosition, player.duration)
                        handler.postDelayed(this, 100)
                    }
                }
            }
        }
    }

    private fun togglePlayback() {
        mediaPlayer?.let { player ->
            if (isAudioPlaying) {
                player.pause()
                playPauseButton.text = "Play"
                isAudioPlaying = false
                handler.removeCallbacks(updateSeekBarRunnable)
            } else {
                player.start()
                playPauseButton.text = "Pause"
                isAudioPlaying  = true
                handler.postDelayed(updateSeekBarRunnable, 0)
            }
        }
    }

    private fun updateProgressText(currentPosition: Int, totalDuration: Int) {
        val currentStr = createTimeLabel(currentPosition)
        val totalStr = createTimeLabel(totalDuration)
        progressText.text = "$currentStr / $totalStr"
    }

    private fun createTimeLabel(time: Int): String {
        val min = time / 1000 / 60
        val sec = time / 1000 % 60
        return String.format("%02d:%02d", min, sec)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        mediaPlayer?.release()
        mediaPlayer = null
    }

    companion object {
        const val EXTRA_RECORDING_PATH = "recording_path"
    }
}
