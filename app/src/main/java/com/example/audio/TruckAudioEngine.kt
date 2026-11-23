package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.example.model.BlinkerState
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.sin

class TruckAudioEngine {
    private val sampleRate = 22050
    @Volatile private var running = false
    @Volatile private var enabled = true
    @Volatile private var rpm = 680f
    @Volatile private var throttle = 0f
    @Volatile private var hornActive = false
    @Volatile private var reverseActive = false
    @Volatile private var blinkerActive = false
    @Volatile private var engineRunning = true

    private var workerThread: Thread? = null

    fun setEnabled(isEnabled: Boolean) {
        enabled = isEnabled
    }

    fun updateState(
        currentRpm: Float,
        currentThrottle: Float,
        isHorn: Boolean,
        isReverse: Boolean,
        blinkers: BlinkerState,
        hasFuel: Boolean
    ) {
        rpm = currentRpm
        throttle = currentThrottle
        hornActive = isHorn
        reverseActive = isReverse
        blinkerActive = blinkers != BlinkerState.NONE
        engineRunning = hasFuel
    }

    fun start() {
        if (running) return
        running = true
        workerThread = thread(start = true, isDaemon = true, name = "TruckAudioDSP") {
            val minBuf = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(2048)

            val track = try {
                AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
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
                    .setBufferSizeInBytes(minBuf)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()
            } catch (_: Exception) {
                running = false
                return@thread
            }

            val buffer = ShortArray(512)
            var phase1 = 0.0
            var phase2 = 0.0
            var hornPhaseA = 0.0
            var hornPhaseB = 0.0
            var beepPhase = 0.0
            var sampleCounter = 0L

            try {
                track.play()
                while (running) {
                    if (!enabled) {
                        buffer.fill(0)
                        track.write(buffer, 0, buffer.size)
                        Thread.sleep(20)
                        continue
                    }

                    val baseFreq = if (engineRunning) 26.0 + (rpm / 2400.0) * 58.0 else 0.0
                    val harmonicFreq = baseFreq * 2.0
                    val horn = hornActive
                    val rev = reverseActive
                    val blink = blinkerActive
                    val throt = throttle.toDouble()

                    for (i in buffer.indices) {
                        var sample = 0.0

                        // Diesel rumble (fundamental + 2nd harmonic + intake growl)
                        if (engineRunning && baseFreq > 1.0) {
                            phase1 += (2.0 * PI * baseFreq) / sampleRate
                            phase2 += (2.0 * PI * harmonicFreq) / sampleRate
                            if (phase1 > 2.0 * PI) phase1 -= 2.0 * PI
                            if (phase2 > 2.0 * PI) phase2 -= 2.0 * PI

                            val rawPulse = sin(phase1) * 0.55 + sin(phase2) * 0.30 + sin(phase1 * 3.0) * 0.15
                            val loadGain = 0.14 + throt * 0.16
                            sample += rawPulse * loadGain
                        }

                        // Dual-tone truck air horn (185 Hz + 233 Hz)
                        if (horn) {
                            hornPhaseA += (2.0 * PI * 185.0) / sampleRate
                            hornPhaseB += (2.0 * PI * 233.0) / sampleRate
                            if (hornPhaseA > 2.0 * PI) hornPhaseA -= 2.0 * PI
                            if (hornPhaseB > 2.0 * PI) hornPhaseB -= 2.0 * PI
                            val hWave = (sin(hornPhaseA) + 0.3 * sin(hornPhaseA * 2.0)) * 0.5 +
                                (sin(hornPhaseB) + 0.3 * sin(hornPhaseB * 2.0)) * 0.5
                            sample += hWave * 0.38
                        }

                        // Reverse warning beep
                        if (rev) {
                            val cyclePos = (sampleCounter % (sampleRate / 2)).toDouble() / (sampleRate / 2)
                            if (cyclePos < 0.38) {
                                beepPhase += (2.0 * PI * 980.0) / sampleRate
                                if (beepPhase > 2.0 * PI) beepPhase -= 2.0 * PI
                                sample += sin(beepPhase) * 0.14
                            }
                        }

                        // Turn signal relay click
                        if (blink) {
                            val blinkPos = sampleCounter % (sampleRate * 3 / 4)
                            if (blinkPos < 180L || (blinkPos in (sampleRate * 3L / 8L)..(sampleRate * 3L / 8L + 160L))) {
                                sample += if ((blinkPos / 15L) % 2L == 0L) 0.12 else -0.12
                            }
                        }

                        val clamped = (sample * 32767.0).coerceIn(-32767.0, 32767.0)
                        buffer[i] = clamped.toInt().toShort()
                        sampleCounter++
                    }

                    track.write(buffer, 0, buffer.size)
                }
            } catch (_: Exception) {
                // Ignore audio stop exceptions
            } finally {
                try {
                    track.stop()
                    track.release()
                } catch (_: Exception) {
                }
            }
        }
    }

    fun stop() {
        running = false
        workerThread?.interrupt()
        workerThread = null
    }
}
