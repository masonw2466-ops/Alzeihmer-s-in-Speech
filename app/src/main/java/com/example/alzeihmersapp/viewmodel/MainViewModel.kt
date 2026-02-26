package com.example.alzeihmersapp.viewmodel

import android.app.Application
import android.media.MediaRecorder
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
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
            if (!exists()) {
                mkdirs()
            }
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

        val recorder = mediaRecorder ?: return
        try {
            recorder.stop()
        } catch (e: RuntimeException) {
            currentOutputFile?.delete()
        } finally {
            recorder.reset()
            recorder.release()
            mediaRecorder = null
            currentOutputFile = null
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
        stopIfRecording()
    }
}

