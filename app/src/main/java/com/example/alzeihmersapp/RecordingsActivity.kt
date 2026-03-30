package com.example.alzeihmersapp

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import androidx.activity.ComponentActivity
import android.widget.ProgressBar
import android.media.MediaPlayer
import java.io.File

class RecordingsActivity : ComponentActivity() {

    private enum class PlaybackState {
        IDLE,
        PLAYING,
        PAUSED,
        COMPLETED
    }

    private var playbackState: PlaybackState = PlaybackState.IDLE

    private var mediaPlayer: MediaPlayer? = null
    private var currentFile: File? = null

    private val mainHandler = Handler(Looper.getMainLooper())
    private lateinit var progressRunnable: Runnable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recordings)

        val listView: ListView = findViewById(R.id.recordings_list)
        val backButton: Button = findViewById(R.id.button_back_to_main)
        val playPauseReplayButton: Button = findViewById(R.id.button_play_pause_replay)
        val progressBar: ProgressBar = findViewById(R.id.recording_progress)

        playPauseReplayButton.isEnabled = false
        progressBar.progress = 0

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

        progressRunnable = Runnable {
            val player = mediaPlayer
            val dur = player?.duration ?: 0
            val pos = player?.currentPosition ?: 0

            if (dur > 0) {
                val percent = ((pos * 100) / dur).coerceIn(0, 100)
                progressBar.progress = percent
            }

            if (playbackState == PlaybackState.PLAYING) {
                mainHandler.postDelayed(progressRunnable, 100L)
            }
        }

        fun updateControls() {
            when (playbackState) {
                PlaybackState.IDLE -> {
                    playPauseReplayButton.text = "Play"
                    playPauseReplayButton.isEnabled = currentFile != null
                }
                PlaybackState.PLAYING -> {
                    playPauseReplayButton.text = "Pause"
                    playPauseReplayButton.isEnabled = currentFile != null
                }
                PlaybackState.PAUSED -> {
                    playPauseReplayButton.text = "Play"
                    playPauseReplayButton.isEnabled = currentFile != null
                }
                PlaybackState.COMPLETED -> {
                    playPauseReplayButton.text = "Replay"
                    playPauseReplayButton.isEnabled = currentFile != null
                    progressBar.progress = 100
                }
            }
        }

        fun releasePlayer() {
            mediaPlayer?.release()
            mediaPlayer = null
        }

        fun loadPlayerForFile(file: File) {
            mainHandler.removeCallbacks(progressRunnable)
            releasePlayer()
            currentFile = file

            val player = MediaPlayer()
            try {
                player.setDataSource(file.absolutePath)
                player.prepare()
            } catch (e: Exception) {
                // If something goes wrong, return to idle.
                playbackState = PlaybackState.IDLE
                currentFile = null
                progressBar.progress = 0
                updateControls()
                releasePlayer()
                return
            }

            player.setOnCompletionListener {
                playbackState = PlaybackState.COMPLETED
                mainHandler.removeCallbacks(progressRunnable)
                updateControls()
            }

            mediaPlayer = player
            playbackState = PlaybackState.IDLE
            progressBar.progress = 0
            updateControls()
        }

        fun startPlayback(fromBeginning: Boolean) {
            val player = mediaPlayer ?: return

            if (fromBeginning) {
                player.seekTo(0)
            }

            player.start()
            playbackState = PlaybackState.PLAYING
            mainHandler.removeCallbacks(progressRunnable)
            mainHandler.post(progressRunnable)
            updateControls()
        }

        fun pausePlayback() {
            val player = mediaPlayer ?: return
            if (playbackState == PlaybackState.PLAYING) {
                player.pause()
                playbackState = PlaybackState.PAUSED
                mainHandler.removeCallbacks(progressRunnable)
                updateControls()
            }
        }

        playPauseReplayButton.setOnClickListener {
            if (currentFile == null) return@setOnClickListener

            when (playbackState) {
                PlaybackState.IDLE -> startPlayback(fromBeginning = true)
                PlaybackState.PLAYING -> pausePlayback()
                PlaybackState.PAUSED -> startPlayback(fromBeginning = false)
                PlaybackState.COMPLETED -> {
                    progressBar.progress = 0
                    startPlayback(fromBeginning = true)
                }
            }
        }

        if (files.isEmpty()) {
            // No recordings: keep disabled.
            updateControls()
            return
        }

        listView.setOnItemClickListener { _, _, position, _ ->
            val file = files.getOrNull(position) ?: return@setOnItemClickListener
            loadPlayerForFile(file)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mainHandler.removeCallbacks(progressRunnable)
        mediaPlayer?.release()
        mediaPlayer = null
    }
}

