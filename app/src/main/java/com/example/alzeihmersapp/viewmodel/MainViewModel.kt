package com.example.alzeihmersapp.viewmodel

import android.app.Application
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.alzeihmersapp.speech.SpeechTranscriptionManager
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val _isRecording = MutableLiveData(false)
    val isRecording: LiveData<Boolean> = _isRecording

    private var mediaRecorder: MediaRecorder? = null
    private var currentOutputFile: File? = null

    private var transcriptionManager: SpeechTranscriptionManager? = null
    private val transcriptBuilder = StringBuilder()

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

    fun setTranscriptionManager(manager: SpeechTranscriptionManager) {
        transcriptionManager = manager
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

        transcriptBuilder.clear()
        transcriptionManager?.start(object : SpeechTranscriptionManager.TranscriptionCallback {
            override fun onFinalResult(text: String) {
                if (transcriptBuilder.isNotEmpty()) {
                    transcriptBuilder.append(" ")
                }
                transcriptBuilder.append(text)
            }

            override fun onTranscriptionError(errorCode: Int) {
                Log.w(TAG, "Transcription error: $errorCode")
            }
        })
    }

    fun stopRecording() {
        if (_isRecording.value != true) return

        transcriptionManager?.stop()

        val transcriptText = transcriptBuilder.toString().trim()
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

        if (recordingSaved && transcriptText.isNotEmpty() && outputFile != null) {
            val transcriptFileName = outputFile.nameWithoutExtension + ".txt"
            File(transcriptsDir, transcriptFileName).writeText(transcriptText)
        }

        _isRecording.value = false
    }

    fun stopIfRecording() {
        if (_isRecording.value == true) {
            stopRecording()
        }
    }

    override fun onCleared() {
        super.onCleared()
        transcriptionManager?.stop()
        stopIfRecording()
    }

    companion object {
        private const val TAG = "MainViewModel"
    }
}
