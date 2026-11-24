package com.retrovoice.bit

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {

    private val purple = Color.rgb(170, 80, 255)
    private val cyan = Color.rgb(0, 230, 255)
    private val dark = Color.rgb(15, 10, 30)

    private lateinit var status: TextView
    private lateinit var selected: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(24, 30, 24, 30)
            setBackgroundColor(dark)
        }

        fun text(value: String, size: Float, color: Int): TextView {
            return TextView(this).apply {
                text = value
                textSize = size
                setTextColor(color)
                gravity = Gravity.CENTER
                typeface = Typeface.create("monospace", Typeface.BOLD)
                setPadding(8, 12, 8, 12)
            }
        }

        root.addView(text("🎮 RETRO VOICE", 27f, cyan))
        root.addView(text("8-BIT VOICE CHANGER", 15f, purple))

        root.addView(text("━━━━━━━━━━━━━━━━━━", 16f, purple))

        status = text("🎤 SIAP MEMULAI", 17f, cyan)
        root.addView(status)

        selected = text("Pilih Efek Suara", 18f, Color.WHITE)
        root.addView(selected)

        val effects = listOf(
            "🎰 BLUE MEME",
            "🕴️ MAFIOSO",
            "🤖 8-BIT ROBOT",
            "🐿️ CHIPMUNK",
            "👹 DEEP VOICE",
            "📻 RETRO RADIO"
        )

        effects.forEach { effect ->
            val button = TextView(this).apply {
                text = effect
                textSize = 16f
                gravity = Gravity.CENTER
                setTextColor(Color.WHITE)
                typeface = Typeface.create("monospace", Typeface.BOLD)
                setPadding(12, 14, 12, 14)
                setBackgroundColor(Color.rgb(55, 25, 85))
                setOnClickListener {
                    selected.text = "Efek dipilih: $effect"
                    status.text = "✨ EFEK SIAP DIUJI"
                }
            }

            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 5, 0, 5)
            root.addView(button, params)
        }

        root.addView(text("━━━━━━━━━━━━━━━━━━", 16f, purple))
        root.addView(
            text("RETRO GAMING • PIXEL SOUND", 12f, cyan)
        )

        setContentView(root)
    }
}
