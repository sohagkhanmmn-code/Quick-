package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.sin

class JarvisSoundEffects {

    private val sampleRate = 44100
    private val scope = CoroutineScope(Dispatchers.Default)
    var isMuted: Boolean = false

    fun playWakeChime() {
        if (isMuted) return
        scope.launch {
            val tone1 = generateSineTone(freq = 880.0, durationMs = 70, volume = 0.35f)
            val tone2 = generateSineTone(freq = 1320.0, durationMs = 120, volume = 0.45f)
            val combined = ByteArray(tone1.size + tone2.size)
            System.arraycopy(tone1, 0, combined, 0, tone1.size)
            System.arraycopy(tone2, 0, combined, tone1.size, tone2.size)
            playPcm(combined)
        }
    }

    fun playListeningStart() {
        if (isMuted) return
        scope.launch {
            val sweep = generateToneSweep(startFreq = 520.0, endFreq = 1040.0, durationMs = 110, volume = 0.3f)
            playPcm(sweep)
        }
    }

    fun playActionSuccess() {
        if (isMuted) return
        scope.launch {
            val tone1 = generateSineTone(freq = 1040.0, durationMs = 50, volume = 0.35f)
            val tone2 = generateSineTone(freq = 1560.0, durationMs = 90, volume = 0.45f)
            val combined = ByteArray(tone1.size + tone2.size)
            System.arraycopy(tone1, 0, combined, 0, tone1.size)
            System.arraycopy(tone2, 0, combined, tone1.size, tone2.size)
            playPcm(combined)
        }
    }

    fun playErrorTone() {
        if (isMuted) return
        scope.launch {
            val tone = generateToneSweep(startFreq = 650.0, endFreq = 260.0, durationMs = 140, volume = 0.35f)
            playPcm(tone)
        }
    }

    private fun generateSineTone(freq: Double, durationMs: Int, volume: Float): ByteArray {
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val pcm = ByteArray(numSamples * 2)
        var idx = 0
        for (i in 0 until numSamples) {
            val angle = 2.0 * Math.PI * i * freq / sampleRate
            // Envelope smoothing: fade in and fade out
            val envelope = when {
                i < numSamples * 0.1 -> (i / (numSamples * 0.1)).toFloat()
                i > numSamples * 0.8 -> ((numSamples - i) / (numSamples * 0.2)).toFloat()
                else -> 1.0f
            }
            val sample = (sin(angle) * 32767 * volume * envelope).toInt().coerceIn(-32768, 32767)
            pcm[idx++] = (sample and 0xFF).toByte()
            pcm[idx++] = ((sample shr 8) and 0xFF).toByte()
        }
        return pcm
    }

    private fun generateToneSweep(startFreq: Double, endFreq: Double, durationMs: Int, volume: Float): ByteArray {
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val pcm = ByteArray(numSamples * 2)
        var idx = 0
        var phase = 0.0
        for (i in 0 until numSamples) {
            val progress = i.toDouble() / numSamples
            val currentFreq = startFreq + (endFreq - startFreq) * progress
            phase += 2.0 * Math.PI * currentFreq / sampleRate
            val envelope = when {
                i < numSamples * 0.15 -> (i / (numSamples * 0.15)).toFloat()
                i > numSamples * 0.85 -> ((numSamples - i) / (numSamples * 0.15)).toFloat()
                else -> 1.0f
            }
            val sample = (sin(phase) * 32767 * volume * envelope).toInt().coerceIn(-32768, 32767)
            pcm[idx++] = (sample and 0xFF).toByte()
            pcm[idx++] = ((sample shr 8) and 0xFF).toByte()
        }
        return pcm
    }

    private fun playPcm(pcmData: ByteArray) {
        var track: AudioTrack? = null
        try {
            val bufferSize = pcmData.size
            track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(pcmData, 0, pcmData.size)
            track.play()
            // Allow track to finish playing
            Thread.sleep((pcmData.size / (sampleRate * 2.0) * 1000).toLong() + 20)
        } catch (_: Exception) {
        } finally {
            try {
                track?.stop()
                track?.release()
            } catch (_: Exception) {
            }
        }
    }
}
