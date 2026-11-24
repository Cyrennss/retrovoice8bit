package com.retrovoice.bit

import android.app.Activity
import android.os.Bundle
import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.media.AudioManager
import android.media.audiofx.LoudnessEnhancer
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.ScrollView
import android.widget.Button
import android.widget.Toast
import android.os.Process
import kotlin.concurrent.thread
import kotlin.math.pow

class MainActivity : Activity() {
    private val cyan = Color.rgb(0, 230, 255)
    private val purple = Color.rgb(170, 80, 255)
    private val dark = Color.rgb(15, 10, 30)

    private lateinit var status: TextView
    private lateinit var selected: TextView
    private var effect = "BLUE MEME"
    private var recording: AudioRecord? = null
    private var playing: AudioTrack? = null
    @Volatile private var running = false

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
                typeface = Typeface.create("monospace", Typeface.BOLD)
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
            val b = Button(this).apply {
                text = name
                setOnClickListener {
                    effect = name
                    selected.text = "Efek: $name"
                }
            }
            root.addView(b)
        }

        val start = Button(this).apply {
            text = "▶ START VOICE REAL-TIME"
            setOnClickListener {
                if (!running) {
                    if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                        != PackageManager.PERMISSION_GRANTED) {
                        requestPermissions(
                            arrayOf(Manifest.permission.RECORD_AUDIO), 100
                        )
                    } else {
                        startVoice()
                    }
                }
            }
        }
        root.addView(start)

        val stop = Button(this).apply {
            text = "■ STOP VOICE"
            setOnClickListener { stopVoice() }
        }
        root.addView(stop)

        root.addView(label("RETRO GAMING • PIXEL SOUND", 12f, cyan))

        setContentView(ScrollView(this).apply {
            addView(root)
        })
    }

    private fun startVoice() {
        val rate = 44100
        val min = AudioRecord.getMinBufferSize(
            rate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        if (min <= 0) {
            status.text = "Mikrofon tidak tersedia"
            return
        }

        try {
            val input = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                rate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                min * 2
            )

            val output = AudioTrack(
                AudioManager.STREAM_MUSIC,
                rate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                min * 2,
                AudioTrack.MODE_STREAM
            )

            if (input.state != AudioRecord.STATE_INITIALIZED ||
                output.state != AudioTrack.STATE_INITIALIZED) {
                input.release()
                output.release()
                status.text = "Gagal membuka audio"
                return
            }

            recording = input
            playing = output
            running = true

            input.startRecording()
            output.play()
            status.text = "🎙️ VOICE AKTIF: $effect"

            thread {
                Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
                val buffer = ShortArray(min / 2)

                while (running) {
                    val count = input.read(buffer, 0, buffer.size)
                    if (count > 0) {
                        for (i in 0 until count) {
                            var sample = buffer[i].toDouble()

                            sample = when (effect) {
                                "BLUE MEME" -> (sample * 1.7).coerceIn(-26000.0, 26000.0)
                                "MAFIOSO", "DEEP VOICE" -> sample * 1.25
                                "CHIPMUNK" -> sample * 1.5
                                "8-BIT ROBOT" -> (sample / 4096.0).toInt() * 4096.0
                                "RETRO RADIO" -> (sample / 2048.0).toInt() * 2048.0
                                else -> sample
                            }

                            buffer[i] = sample.toInt().coerceIn(-32768, 32767).toShort()
                        }
                        output.write(buffer, 0, count)
                    }
                }
            }
        } catch (e: Exception) {
            running = false
            status.text = "Audio error: ${e.message}"
        }
    }

    private fun stopVoice() {
        running = false
        try {
            recording?.stop()
            recording?.release()
            playing?.stop()
            playing?.release()
        } catch (_: Exception) {
        }
        recording = null
        playing = null
        status.text = "⏹ VOICE BERHENTI"
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startVoice()
        } else {
            status.text = "Izin mikrofon ditolak"
        }
    }

    override fun onDestroy() {
        stopVoice()
        super.onDestroy()
    }
}
