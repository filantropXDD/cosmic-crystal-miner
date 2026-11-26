package com.cosmicminer.app.util

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

object SoundEffects {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private const val SAMPLE_RATE = 22050

    fun playTap(isCritical: Boolean) {
        scope.launch {
            if (isCritical) {
                playChime(startFreq = 660.0, endFreq = 1320.0, durationMs = 170, volume = 0.25)
            } else {
                val randomPitch = 460.0 + (Math.random() * 70.0)
                playChime(startFreq = randomPitch, endFreq = randomPitch * 1.32, durationMs = 65, volume = 0.14)
            }
        }
    }

    fun playPurchase() {
        scope.launch {
            playChime(startFreq = 420.0, endFreq = 840.0, durationMs = 110, volume = 0.18)
        }
    }

    fun playComet() {
        scope.launch {
            playChime(startFreq = 520.0, endFreq = 1560.0, durationMs = 220, volume = 0.26)
        }
    }

    fun playPrestige() {
        scope.launch {
            playChime(startFreq = 220.0, endFreq = 880.0, durationMs = 360, volume = 0.28)
        }
    }

    private fun playChime(
        startFreq: Double,
        endFreq: Double,
        durationMs: Int,
        volume: Double
    ) {
        try {
            val numSamples = (SAMPLE_RATE * durationMs) / 1000
            val samples = ShortArray(numSamples)
            var phase = 0.0

            for (i in 0 until numSamples) {
                val t = i.toDouble() / numSamples
                val freq = startFreq + (endFreq - startFreq) * t
                val envelope = exp(-4.5 * t) * (1.0 - t)
                phase += (2.0 * PI * freq) / SAMPLE_RATE
                val wave = sin(phase) * envelope * volume
                samples[i] = (wave * Short.MAX_VALUE).toInt().coerceIn(
                    Short.MIN_VALUE.toInt(),
                    Short.MAX_VALUE.toInt()
                ).toShort()
            }

            val minBuf = AudioTrack.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val bufferSize = maxOf(minBuf, samples.size * 2)

            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(samples, 0, samples.size)
            track.play()
            Thread.sleep(durationMs.toLong() + 40L)
            track.stop()
            track.release()
        } catch (_: Throwable) {
            // Safe fallback if audio hardware is unavailable
        }
    }
}
