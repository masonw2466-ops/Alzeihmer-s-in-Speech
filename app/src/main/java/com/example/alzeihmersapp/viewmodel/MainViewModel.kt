package com.example.alzeihmersapp.viewmodel

import android.app.Application
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.alzeihmersapp.speech.AudioTranscriber
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val _isRecording = MutableLiveData(false)
    val isRecording: LiveData<Boolean> = _isRecording

    private var mediaRecorder: MediaRecorder? = null
    private var currentOutputFile: File? = null

    private val recordingsDir: File by lazy {
        File(getApplication<Application>().filesDir, "recordings").apply {
            if (!exists()) mkdirs()
        }
    }

    private val transcriptsDir: File by lazy {
        File(getApplication<Application>().filesDir, "transcripts").apply {
            if (!exists()) mkdirs()
        }
    }

    fun startRecording() {
        if (_isRecording.value == true) return

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val outputFile = File(recordingsDir, "recording_$timestamp.m4a")

        val ctx = getApplication<Application>()
        val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(ctx)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

        mediaRecorder = recorder.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(128_000)
            setAudioSamplingRate(44_100)
            setOutputFile(outputFile.absolutePath)
            prepare()
            start()
        }
        currentOutputFile = outputFile
        _isRecording.value = true
    }

    fun stopRecording() {
        if (_isRecording.value != true) return

        val outputFile = currentOutputFile
        val recorder = mediaRecorder ?: return
        var recordingSaved = true
        try {
            recorder.stop()
        } catch (e: RuntimeException) {
            currentOutputFile?.delete()
            recordingSaved = false
        } finally {
            recorder.reset()
            recorder.release()
            mediaRecorder = null
            currentOutputFile = null
        }

        _isRecording.value = false

        // Kick off background transcription if the recording was saved
        if (recordingSaved && outputFile != null && outputFile.exists()) {
            transcribeInBackground(outputFile)
        }
    }

    private fun transcribeInBackground(audioFile: File) {
        Thread {
            Log.i(TAG, "Starting transcription for ${audioFile.name}")
            val text = AudioTranscriber.transcribeFile(audioFile)
            if (text != null) {
                val transcriptFile = File(transcriptsDir,
                    audioFile.nameWithoutExtension + ".txt")
                transcriptFile.writeText(text)
                Log.i(TAG, "Transcript saved: ${transcriptFile.name}")
            } else {
                Log.w(TAG, "Transcription returned no text for ${audioFile.name}")
            }
        }.start()
    }

    fun stopIfRecording() {
        if (_isRecording.value == true) {
            stopRecording()
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopIfRecording()
    }

    companion object {
        private const val TAG = "MainViewModel"
    }
}
