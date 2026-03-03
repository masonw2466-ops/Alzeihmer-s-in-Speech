package com.example.alzeihmersapp.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

class SpeechTranscriptionManager(private val context: Context) {

    interface TranscriptionCallback {
        fun onFinalResult(text: String)
        fun onTranscriptionError(errorCode: Int)
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private var speechRecognizer: SpeechRecognizer? = null
    private var callback: TranscriptionCallback? = null
    private var isActive = false

    private val recognizerIntent: Intent
        get() = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

    fun start(callback: TranscriptionCallback) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            callback.onTranscriptionError(SpeechRecognizer.ERROR_CLIENT)
            return
        }

        this.callback = callback
        isActive = true
        mainHandler.post { createAndStartRecognizer() }
    }

    fun stop() {
        isActive = false
        mainHandler.post { destroyRecognizer() }
    }

    private fun createAndStartRecognizer() {
        if (!isActive) return

        destroyRecognizer()

        val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    callback?.onFinalResult(matches[0])
                }
                restartIfActive()
            }

            override fun onPartialResults(partialResults: Bundle?) {}

            override fun onError(error: Int) {
                if (isNonFatalError(error)) {
                    restartIfActive()
                } else {
                    callback?.onTranscriptionError(error)
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        speechRecognizer = recognizer
        recognizer.startListening(recognizerIntent)
    }

    private fun restartIfActive() {
        if (!isActive) return
        mainHandler.postDelayed({ createAndStartRecognizer() }, RESTART_DELAY_MS)
    }

    private fun destroyRecognizer() {
        speechRecognizer?.apply {
            cancel()
            destroy()
        }
        speechRecognizer = null
    }

    private fun isNonFatalError(error: Int): Boolean {
        return error == SpeechRecognizer.ERROR_NO_MATCH ||
                error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT ||
                error == SpeechRecognizer.ERROR_CLIENT ||
                error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY
    }

    companion object {
        private const val RESTART_DELAY_MS = 300L
    }
}
