package com.example.alzeihmersapp

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.ProgressBar
import androidx.activity.ComponentActivity
import com.example.alzeihmersapp.adapter.RecordingsAdapter
import com.example.alzeihmersapp.speech.AudioTranscriber
import java.io.File

class RecordingsActivity : ComponentActivity() {

    private enum class PlaybackState {
        IDLE, PLAYING, PAUSED, COMPLETED
    }

    private var playbackState: PlaybackState = PlaybackState.IDLE
    private var mediaPlayer: MediaPlayer? = null
    private var currentFile: File? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private lateinit var progressRunnable: Runnable

    // Periodic refresh so "Processing..." buttons update when transcription completes
    private lateinit var refreshRunnable: Runnable
    private var adapter: RecordingsAdapter? = null

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
        val transcriptsDir = File(filesDir, "transcripts")
        val files = recordingsDir.listFiles()?.sortedBy { it.lastModified() } ?: emptyList()

        if (files.isEmpty()) {
            listView.adapter = ArrayAdapter(
                this,
                android.R.layout.simple_list_item_1,
                listOf("No recordings yet")
            )
        } else {
            adapter = RecordingsAdapter(this, files, transcriptsDir) { recordingFile ->
                val intent = Intent(this, TranscriptViewActivity::class.java)
                intent.putExtra(TranscriptViewActivity.EXTRA_RECORDING_PATH, recordingFile.absolutePath)
                startActivity(intent)
            }
            listView.adapter = adapter

            // Transcribe any old recordings that are missing transcripts
            transcribeMissing(files, transcriptsDir)
        }

        backButton.setOnClickListener { finish() }

        // Progress bar updater for audio playback
        progressRunnable = Runnable {
            val player = mediaPlayer
            val dur = player?.duration ?: 0
            val pos = player?.currentPosition ?: 0
            if (dur > 0) {
                progressBar.progress = ((pos * 100) / dur).coerceIn(0, 100)
            }
            if (playbackState == PlaybackState.PLAYING) {
                mainHandler.postDelayed(progressRunnable, 100L)
            }
        }

        // Refresh adapter every 2 seconds to pick up completed transcriptions
        refreshRunnable = Runnable {
            adapter?.notifyDataSetChanged()
            mainHandler.postDelayed(refreshRunnable, 2000L)
        }

        fun updateControls() {
            when (playbackState) {
                PlaybackState.IDLE -> {
                    playPauseReplayButton.text = "Play"
                    playPauseReplayButton.isEnabled = currentFile != null
                }
                PlaybackState.PLAYING -> {
                    playPauseReplayButton.text = "Pause"
                    playPauseReplayButton.isEnabled = true
                }
                PlaybackState.PAUSED -> {
                    playPauseReplayButton.text = "Play"
                    playPauseReplayButton.isEnabled = true
                }
                PlaybackState.COMPLETED -> {
                    playPauseReplayButton.text = "Replay"
                    playPauseReplayButton.isEnabled = true
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
                playbackState = PlaybackState.IDLE
                currentFile = null
                progressBar.progress = 0
                updateControls()
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
            if (fromBeginning) player.seekTo(0)
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
            updateControls()
            return
        }

        // Tap a recording row to load it for playback
        listView.setOnItemClickListener { _, _, position, _ ->
            val file = files.getOrNull(position) ?: return@setOnItemClickListener
            loadPlayerForFile(file)
        }
    }

    override fun onResume() {
        super.onResume()
        // Start periodic refresh to detect completed transcriptions
        adapter?.notifyDataSetChanged()
        mainHandler.postDelayed(refreshRunnable, 2000L)
    }

    override fun onPause() {
        super.onPause()
        mainHandler.removeCallbacks(refreshRunnable)
    }

    private fun transcribeMissing(recordings: List<File>, transcriptsDir: File) {
        if (!AudioTranscriber.isReady) return
        transcriptsDir.mkdirs()

        val missing = recordings.filter { rec ->
            val txt = File(transcriptsDir, rec.nameWithoutExtension + ".txt")
            !txt.exists()
        }
        if (missing.isEmpty()) return

        Log.i("RecordingsActivity", "Transcribing ${missing.size} old recording(s) in background")
        Thread {
            for (rec in missing) {
                val text = AudioTranscriber.transcribeFile(rec)
                if (text != null) {
                    File(transcriptsDir, rec.nameWithoutExtension + ".txt").writeText(text)
                    Log.i("RecordingsActivity", "Transcribed: ${rec.name}")
                } else {
                    Log.w("RecordingsActivity", "No speech detected in: ${rec.name}")
                }
            }
        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        mainHandler.removeCallbacks(progressRunnable)
        mainHandler.removeCallbacks(refreshRunnable)
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
