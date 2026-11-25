package com.retrovoice.bit

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.media.PlaybackParams
import android.os.Bundle
import android.os.Process
import android.graphics.Color
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import kotlin.concurrent.thread
import kotlin.math.abs
import kotlin.math.sign

class MainActivity : Activity() {

    private lateinit var status: TextView
    private lateinit var selected: TextView

    @Volatile private var running = false
    @Volatile private var effect = "BLUE MEME"

    private var recording: AudioRecord? = null
    private var playing: AudioTrack? = null
    private var previousAudioMode = AudioManager.MODE_NORMAL
    private var audioThread: Thread? = null

    private var radioState = 0.0

    private val cyan = Color.rgb(0, 230, 255)
    private val purple = Color.rgb(170, 80, 255)
    private val dark = Color.rgb(15, 10, 30)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(24, 24, 24, 24)
            setBackgroundColor(dark)
        }

        fun label(value: String, size: Float, color: Int): TextView {
            return TextView(this).apply {
                text = value
                textSize = size
                setTextColor(color)
                gravity = Gravity.CENTER
                setPadding(8, 12, 8, 12)
            }
        }

        root.addView(label("🎮 RETRO VOICE", 27f, cyan))
        root.addView(label("8-BIT VOICE CHANGER", 15f, purple))

        status = label("🎤 SIAP MEMULAI", 17f, cyan)
        root.addView(status)

        selected = label("Efek: BLUE MEME", 17f, Color.WHITE)
        root.addView(selected)

        val effects = listOf(
            "BLUE MEME",
            "MAFIOSO",
            "8-BIT ROBOT",
            "CHIPMUNK",
            "DEEP VOICE",
            "RETRO RADIO"
        )

        effects.forEach { name ->
            root.addView(Button(this).apply {
                text = name
                setOnClickListener {
                    effect = name
                    selected.text = "Efek: $name"
                    applyVoicePitch()
                }
            })
        }

        root.addView(Button(this).apply {
            text = "▶ START VOICE REAL-TIME"
            setOnClickListener {
                if (!running) {
                    if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                        != PackageManager.PERMISSION_GRANTED
                    ) {
                        requestPermissions(
                            arrayOf(Manifest.permission.RECORD_AUDIO), 100
                        )
                    } else {
                        startVoice()
                    }
                }
            }
        })

        root.addView(Button(this).apply {
            text = "■ STOP VOICE"
            setOnClickListener { stopVoice() }
        })

        root.addView(label("RETRO GAMING • PIXEL SOUND", 12f, cyan))

        setContentView(ScrollView(this).apply {
            addView(root)
        })
    }

    private fun showStatus(message: String) {
        runOnUiThread {
            if (::status.isInitialized) status.text = message
        }
    }

    private fun startVoice() {
        if (running) return

        val rate = 44100
        val channelIn = AudioFormat.CHANNEL_IN_MONO
        val channelOut = AudioFormat.CHANNEL_OUT_MONO
        val encoding = AudioFormat.ENCODING_PCM_16BIT

        val minIn = AudioRecord.getMinBufferSize(rate, channelIn, encoding)
        val minOut = AudioTrack.getMinBufferSize(rate, channelOut, encoding)

        if (minIn <= 0 || minOut <= 0) {
            showStatus("Format audio tidak didukung")
            return
        }

        val bufferBytes = maxOf(minIn, minOut, 8192) * 2

        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        previousAudioMode = audioManager.mode
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION

        var input: AudioRecord? = null
        var output: AudioTrack? = null

        try {
            input = AudioRecord(
MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                rate,
                channelIn,
                encoding,
                bufferBytes
            )
            output = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(rate)
                        .setEncoding(encoding)
                        .setChannelMask(channelOut)
                        .build()
                )
                .setBufferSizeInBytes(bufferBytes)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            if (input.state != AudioRecord.STATE_INITIALIZED ||
                output.state != AudioTrack.STATE_INITIALIZED
            ) {
                input.release()
                output.release()
                showStatus("Gagal membuka perangkat audio")
                return
            }

            recording = input
            playing = output

            input.startRecording()
            output.play()
            applyVoicePitch()

            running = true
            showStatus("🎙️ AUDIO AKTIF: $effect")

            audioThread = thread(name = "RetroAudio") {
                Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)

                val buffer = ShortArray(1024)
                radioState = 0.0

                try {
                    while (running) {
                        val count = input.read(
                            buffer, 0, buffer.size,
                            AudioRecord.READ_BLOCKING
                        )

                        if (count < 0) {
                            throw IllegalStateException("Mikrofon gagal membaca audio: $count")
                        }

                        if (count == 0) continue

                        processAudio(buffer, count)

                        var written = 0
                        while (written < count && running) {
                            val result = output.write(
                                buffer, written, count - written,
                                AudioTrack.WRITE_BLOCKING
                            )

                            if (result < 0) {
                                throw IllegalStateException("AudioTrack error: $result")
                            }

                            if (result == 0) break
                            written += result
                        }
                    }
                } catch (e: Exception) {
                    if (running) {
                        showStatus("Audio error: ${e.message}")
                    }
                } finally {
                    running = false

                    try {
    val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
    audioManager.mode = previousAudioMode
} catch (_: Exception) {}

                    try { input.stop() } catch (_: Exception) {}
                    try { output.pause() } catch (_: Exception) {}
                    try { output.flush() } catch (_: Exception) {}
                    input.release()
                    output.release()

                    if (recording === input) recording = null
                    if (playing === output) playing = null

                    showStatus("⏹ VOICE BERHENTI")
                }
            }

        } catch (e: Exception) {
            running = false
            try { input?.release() } catch (_: Exception) {}
            try { output?.release() } catch (_: Exception) {}
            showStatus("Gagal memulai audio: ${e.message}")
        }
    }

    private fun applyVoicePitch() {
        val pitch = when (effect) {
            "BLUE MEME" -> 1.35f
            "MAFIOSO" -> 0.75f
            "8-BIT ROBOT" -> 1.0f
            "CHIPMUNK" -> 1.50f
            "DEEP VOICE" -> 0.70f
            "RETRO RADIO" -> 1.0f
            else -> 1.0f
        }

        try {
            playing?.playbackParams = PlaybackParams()
                .setSpeed(1.0f)
                .setPitch(pitch)
        } catch (_: Exception) {
            showStatus("Pitch tidak didukung perangkat")
        }
    }

    private fun processAudio(buffer: ShortArray, count: Int) {
        for (i in 0 until count) {
            val original = buffer[i].toDouble() / 32768.0

            val processed = when (effect) {
                "BLUE MEME" -> {
                    val boosted = original * 1.7
                    (boosted.coerceIn(-0.85, 0.85) * 32767.0)
                }

                "MAFIOSO" -> {
                    radioState = radioState * 0.78 + original * 0.22
                    val thick = radioState * 1.8
                    (thick / (1.0 + abs(thick))) * 32767.0
                }

                "8-BIT ROBOT" -> {
                    val levels = 32.0
                    ((original * levels).toInt() / levels * 32767.0)
                }

                "CHIPMUNK" -> {
                    (original * 1.3).coerceIn(-1.0, 1.0) * 32767.0
                }

                "DEEP VOICE" -> {
                    (original * 0.85).coerceIn(-1.0, 1.0) * 32767.0
                }

                "RETRO RADIO" -> {
                    radioState = radioState * 0.82 + original * 0.18
                    val quantized = (radioState * 24.0).toInt() / 24.0
                    quantized.coerceIn(-1.0, 1.0) * 32767.0
                }

                else -> original * 32767.0
            }

            buffer[i] = processed
                .toInt()
                .coerceIn(-32768, 32767)
                .toShort()
        }
    }

    private fun stopVoice() {
        running = false

        try {
    val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
    audioManager.mode = previousAudioMode
} catch (_: Exception) {}
    try { recording?.stop() } catch (_: Exception) {}

        showStatus("Menghentikan audio...")
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 100 &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            startVoice()
        } else {
            showStatus("Izin mikrofon ditolak")
        }
    }

    override fun onDestroy() {
        stopVoice()
        super.onDestroy()
    }
}
