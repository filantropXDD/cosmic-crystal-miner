package com.cosmicminer.app.util

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

object SoundEffects {
    private val audioDispatcher = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "cosmic-audio-thread").apply { isDaemon = true }
    }.asCoroutineDispatcher()
    private val scope = CoroutineScope(SupervisorJob() + audioDispatcher)
    private const val SAMPLE_RATE = 22050
    private const val MIN_TAP_INTERVAL_MS = 38L

    @Volatile
    private var lastTapMs = 0L

    private val minBufSize: Int by lazy {
        AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(1024)
    }

    private val purchaseSamples by lazy { buildChimeSamples(420.0, 840.0, 110, 0.18) }
    private val cometSamples by lazy { buildChimeSamples(520.0, 1560.0, 220, 0.26) }
    private val feverSamples by lazy { buildChimeSamples(440.0, 1480.0, 250, 0.27) }
    private val bossSamples by lazy { buildChimeSamples(330.0, 990.0, 290, 0.28) }
    private val prestigeSamples by lazy { buildChimeSamples(220.0, 880.0, 350, 0.28) }
    private val critNormalSamples by lazy { buildChimeSamples(660.0, 1320.0, 150, 0.23) }
    private val critFeverSamples by lazy { buildChimeSamples(818.0, 1636.0, 105, 0.22) }

    fun playTap(isCritical: Boolean, isFever: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!isCritical && now - lastTapMs < MIN_TAP_INTERVAL_MS) return
        lastTapMs = now

        scope.launch {
            if (isCritical) {
                playSamples(if (isFever) critFeverSamples else critNormalSamples, if (isFever) 105 else 150)
            } else {
                val pitchMult = if (isFever) 1.24 else 1.0
                val durMs = if (isFever) 46 else 62
                val randomPitch = (460.0 + (Math.random() * 70.0)) * pitchMult
                playSamples(buildChimeSamples(randomPitch, randomPitch * 1.32, durMs, 0.14), durMs)
            }
        }
    }

    fun playPurchase() {
        scope.launch { playSamples(purchaseSamples, 110) }
    }

    fun playComet() {
        scope.launch { playSamples(cometSamples, 220) }
    }

    fun playFeverStart() {
        scope.launch { playSamples(feverSamples, 250) }
    }

    fun playBossDefeated() {
        scope.launch { playSamples(bossSamples, 290) }
    }

    fun playPrestige() {
        scope.launch { playSamples(prestigeSamples, 350) }
    }

    private fun buildChimeSamples(
        startFreq: Double,
        endFreq: Double,
        durationMs: Int,
        volume: Double
    ): ShortArray {
        val clampedDuration = durationMs.coerceAtLeast(35)
        val numSamples = (SAMPLE_RATE * clampedDuration) / 1000
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
        return samples
    }

    private fun playSamples(samples: ShortArray, durationMs: Int) {
        var track: AudioTrack? = null
        try {
            val bufferSize = maxOf(minBufSize, samples.size * 2)
            track = AudioTrack.Builder()
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
            Thread.sleep(durationMs.toLong() + 25L)
        } catch (_: Throwable) {
            // Safe fallback if audio hardware is unavailable
        } finally {
            try {
                track?.stop()
                track?.release()
            } catch (_: Throwable) {
            }
        }
    }
}
