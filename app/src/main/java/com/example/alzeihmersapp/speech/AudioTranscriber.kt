package com.example.alzeihmersapp.speech

import android.content.Context
import android.content.res.AssetManager
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.Log
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

object AudioTranscriber {

    private const val TAG = "AudioTranscriber"
    private const val VOSK_SAMPLE_RATE = 16000f
    private const val ASSET_SOURCE = "model-en-us"
    private const val LOCAL_TARGET = "vosk-model"

    private var model: Model? = null
    private var isInitializing = false

    val isReady: Boolean get() = model != null

    fun initModel(context: Context, onReady: () -> Unit, onError: (Exception) -> Unit) {
        if (model != null) {
            onReady()
            return
        }
        if (isInitializing) return
        isInitializing = true

        Thread {
            try {
                val targetDir = File(context.filesDir, LOCAL_TARGET)

                // Step 1: Copy model from assets to internal storage
                Log.i(TAG, "Step 1: Copying model from assets/$ASSET_SOURCE to ${targetDir.absolutePath}")
                copyAssetDir(context.assets, ASSET_SOURCE, targetDir)

                // Step 2: Verify files were copied
                val copiedFiles = targetDir.walkTopDown().filter { it.isFile }.toList()
                Log.i(TAG, "Step 2: Copied ${copiedFiles.size} files:")
                copiedFiles.forEach { Log.i(TAG, "  ${it.relativeTo(targetDir)}  (${it.length()} bytes)") }

                if (copiedFiles.isEmpty()) {
                    throw IOException("No model files were copied — assets/$ASSET_SOURCE may be empty or unreadable")
                }

                // Step 3: Load the Vosk Model
                Log.i(TAG, "Step 3: Creating Vosk Model from ${targetDir.absolutePath}")
                val loadedModel = Model(targetDir.absolutePath)

                model = loadedModel
                isInitializing = false
                Log.i(TAG, "Vosk model loaded successfully")
                onReady()
            } catch (e: Exception) {
                isInitializing = false
                Log.e(TAG, "Failed to load Vosk model", e)
                onError(e)
            }
        }.start()
    }

    /**
     * Recursively copy an asset directory to a local directory.
     * If the local directory already has the files, it skips them.
     */
    private fun copyAssetDir(assets: AssetManager, assetPath: String, targetDir: File) {
        // Try to list contents — if it returns items, it's a directory
        val items = assets.list(assetPath)
        Log.d(TAG, "assets.list(\"$assetPath\") returned: ${items?.toList()}")

        if (items != null && items.isNotEmpty()) {
            // It's a directory — recurse into each child
            targetDir.mkdirs()
            for (item in items) {
                copyAssetDir(assets, "$assetPath/$item", File(targetDir, item))
            }
        } else {
            // It's a file — copy it
            if (targetDir.exists() && targetDir.length() > 0) {
                Log.d(TAG, "Skipping (already exists): ${targetDir.absolutePath}")
                return
            }
            targetDir.parentFile?.mkdirs()
            try {
                val input: InputStream = assets.open(assetPath)
                targetDir.outputStream().use { output ->
                    input.copyTo(output)
                }
                input.close()
                Log.d(TAG, "Copied: $assetPath -> ${targetDir.absolutePath} (${targetDir.length()} bytes)")
            } catch (e: IOException) {
                Log.e(TAG, "Failed to copy asset: $assetPath", e)
                throw e
            }
        }
    }

    /**
     * Transcribe an .m4a audio file to text using Vosk.
     * This is a blocking call — run it on a background thread.
     *
     * @return the transcript text, or null if transcription failed
     */
    fun transcribeFile(audioFile: File): String? {
        val mdl = model ?: run {
            Log.w(TAG, "Model not initialized")
            return null
        }

        return try {
            val pcmData = decodeM4aToPcm(audioFile)
            if (pcmData.isEmpty()) {
                Log.w(TAG, "No PCM data decoded from ${audioFile.name}")
                return null
            }

            val recognizer = Recognizer(mdl, VOSK_SAMPLE_RATE)
            val chunkSize = 4000
            var offset = 0
            while (offset < pcmData.size) {
                val end = minOf(offset + chunkSize, pcmData.size)
                val chunk = pcmData.copyOfRange(offset, end)
                recognizer.acceptWaveForm(chunk, chunk.size)
                offset = end
            }
            val resultJson = recognizer.finalResult
            recognizer.close()

            val text = JSONObject(resultJson).optString("text", "").trim()
            if (text.isEmpty()) null else text
        } catch (e: Exception) {
            Log.e(TAG, "Transcription failed for ${audioFile.name}", e)
            null
        }
    }

    // ── M4A → 16 kHz mono PCM decoding ──────────────────────────────

    private fun decodeM4aToPcm(file: File): ByteArray {
        val extractor = MediaExtractor()
        extractor.setDataSource(file.absolutePath)

        // Find the audio track
        var audioTrackIndex = -1
        var audioFormat: MediaFormat? = null
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
            if (mime.startsWith("audio/")) {
                audioTrackIndex = i
                audioFormat = format
                break
            }
        }
        if (audioTrackIndex < 0 || audioFormat == null) {
            extractor.release()
            throw IOException("No audio track found in ${file.name}")
        }

        extractor.selectTrack(audioTrackIndex)
        val sampleRate = audioFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        val channelCount = audioFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        val mime = audioFormat.getString(MediaFormat.KEY_MIME)!!

        // Create decoder
        val codec = MediaCodec.createDecoderByType(mime)
        codec.configure(audioFormat, null, null, 0)
        codec.start()

        val rawPcm = drainDecoder(codec, extractor)

        codec.stop()
        codec.release()
        extractor.release()

        return resampleTo16kMono(rawPcm, sampleRate, channelCount)
    }

    private fun drainDecoder(codec: MediaCodec, extractor: MediaExtractor): ByteArray {
        val output = ByteArrayOutputStream()
        val bufferInfo = MediaCodec.BufferInfo()
        var inputDone = false

        while (true) {
            // Feed input buffers
            if (!inputDone) {
                val inputIndex = codec.dequeueInputBuffer(10_000)
                if (inputIndex >= 0) {
                    val inputBuffer = codec.getInputBuffer(inputIndex)!!
                    val sampleSize = extractor.readSampleData(inputBuffer, 0)
                    if (sampleSize < 0) {
                        codec.queueInputBuffer(inputIndex, 0, 0, 0,
                            MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        inputDone = true
                    } else {
                        codec.queueInputBuffer(inputIndex, 0, sampleSize,
                            extractor.sampleTime, 0)
                        extractor.advance()
                    }
                }
            }

            // Drain output buffers
            val outputIndex = codec.dequeueOutputBuffer(bufferInfo, 10_000)
            if (outputIndex >= 0) {
                val outputBuffer = codec.getOutputBuffer(outputIndex)!!
                val chunk = ByteArray(bufferInfo.size)
                outputBuffer.get(chunk)
                output.write(chunk)
                codec.releaseOutputBuffer(outputIndex, false)

                if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) break
            }
        }

        return output.toByteArray()
    }

    /**
     * Converts raw PCM (16-bit signed LE, any sample rate, any channel count)
     * to 16 kHz mono 16-bit signed LE — what Vosk expects.
     */
    private fun resampleTo16kMono(pcm: ByteArray, sampleRate: Int, channels: Int): ByteArray {
        val bytesPerSample = 2
        val frameSize = bytesPerSample * channels
        val numFrames = pcm.size / frameSize

        // Step 1: mix down to mono
        val monoSamples = ShortArray(numFrames)
        val buf = ByteBuffer.wrap(pcm).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until numFrames) {
            var sum = 0L
            for (ch in 0 until channels) {
                sum += buf.short
            }
            monoSamples[i] = (sum / channels).toInt().toShort()
        }

        // Step 2: resample to 16 000 Hz
        val targetRate = 16000
        val resampledSamples: ShortArray
        if (sampleRate == targetRate) {
            resampledSamples = monoSamples
        } else {
            val ratio = sampleRate.toDouble() / targetRate
            val newLength = (numFrames / ratio).toInt()
            resampledSamples = ShortArray(newLength)
            for (i in resampledSamples.indices) {
                val srcIndex = (i * ratio).toInt().coerceIn(0, monoSamples.size - 1)
                resampledSamples[i] = monoSamples[srcIndex]
            }
        }

        // Step 3: convert back to byte array
        val result = ByteArray(resampledSamples.size * 2)
        ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
            .put(resampledSamples)
        return result
    }
}
