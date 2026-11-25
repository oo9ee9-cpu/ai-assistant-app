package com.aster.engine

import android.app.Activity
import android.os.Bundle
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    companion object { init { System.loadLibrary("aster_engine") } }
    private external fun nativeGetEngineStatus(): String
    private external fun nativeStartEngine()
    private external fun nativeStopEngine()
    private lateinit var status: TextView
    private lateinit var hierarchy: LinearLayout
    private var objectCount = 1
    private var playing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(12, 16, 24)
        window.navigationBarColor = Color.rgb(12, 16, 24)
        buildEditor()
    }

    private fun buildEditor() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(13, 17, 25))
            setPadding(dp(12), dp(8), dp(12), dp(8))
        }
        val header = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        header.addView(TextView(this).apply {
            text = "ASTER  /  ENGINE"; textSize = 18f; typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
        }, LinearLayout.LayoutParams(0, dp(48), 1f))
        status = TextView(this).apply {
            text = nativeGetEngineStatus(); textSize = 10f
            setTextColor(Color.rgb(93, 220, 166)); setPadding(dp(8), dp(6), dp(8), dp(6))
            setBackgroundColor(Color.rgb(28, 48, 47))
        }
        header.addView(status)
        root.addView(header)

        val toolbar = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        toolbar.addView(toolButton("SCENE") { showToast("Scene panel") })
        toolbar.addView(toolButton("ADD +") { addObject() })
        toolbar.addView(toolButton("PLAY") { togglePlay() })
        toolbar.addView(toolButton("ASSETS") { showToast("Asset browser comes in a later milestone") })
        root.addView(toolbar)

        root.addView(label("VIEWPORT  /  PERSPECTIVE"))
        root.addView(ViewportView(), LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(label("HIERARCHY"))
        hierarchy = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(8), dp(4), dp(8), dp(4))
            setBackgroundColor(Color.rgb(24, 30, 41))
        }
        addHierarchyRow("Camera")
        addHierarchyRow("Directional Light")
        addHierarchyRow("Cube")
        root.addView(hierarchy, LinearLayout.LayoutParams(-1, dp(118)))
        root.addView(TextView(this).apply {
            text = "ASTER ENGINE 0.1  •  C++20 CORE  •  ANDROID"
            textSize = 10f; gravity = Gravity.CENTER
            setTextColor(Color.rgb(122, 137, 157)); setPadding(0, dp(10), 0, dp(4))
        })
        setContentView(root)
    }

    private fun label(value: String) = TextView(this).apply {
        text = value; textSize = 10f; typeface = Typeface.DEFAULT_BOLD
        setTextColor(Color.rgb(139, 157, 181)); setPadding(dp(2), dp(12), 0, dp(6))
    }

    private fun toolButton(value: String, action: () -> Unit) = Button(this).apply {
        text = value; textSize = 10f; isAllCaps = false; setTextColor(Color.WHITE)
        setBackgroundColor(Color.rgb(39, 49, 66)); setOnClickListener { action() }
        layoutParams = LinearLayout.LayoutParams(0, dp(44), 1f).apply {
            setMargins(dp(2), dp(4), dp(2), dp(4))
        }
    }

    private fun addObject() {
        objectCount++
        addHierarchyRow("Cube_$objectCount")
        showToast("Object added to hierarchy")
    }

    private fun addHierarchyRow(name: String) {
        hierarchy.addView(TextView(this).apply {
            text = "▣   $name"; textSize = 13f
            setTextColor(Color.rgb(220, 228, 240)); setPadding(dp(6), dp(4), 0, dp(4))
        })
    }

    private fun togglePlay() {
        playing = !playing
        if (playing) nativeStartEngine() else nativeStopEngine()
        status.text = nativeGetEngineStatus()
        showToast(if (playing) "Engine core started" else "Engine core stopped")
    }

    private fun showToast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private inner class ViewportView : View(this@MainActivity) {
        private val grid = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(49, 61, 79); strokeWidth = dp(1).toFloat() }
        private val axis = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(69, 143, 190); strokeWidth = dp(2).toFloat() }
        private val cube = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(76, 145, 205); style = Paint.Style.STROKE; strokeWidth = dp(2).toFloat()
        }
        private val caption = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(130, 148, 170); textSize = dp(10).toFloat()
        }
        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            canvas.drawColor(Color.rgb(17, 23, 33))
            val horizon = height * 0.48f
            val cx = width * 0.5f
            for (i in -10..10) canvas.drawLine(cx, horizon, cx + i * dp(24), height.toFloat(), grid)
            for (i in 1..12) {
                val y = horizon + i * i * dp(2).toFloat()
                if (y < height) canvas.drawLine(0f, y, width.toFloat(), y, grid)
            }
            canvas.drawLine(0f, horizon, width.toFloat(), horizon, grid)
            canvas.drawLine(cx, horizon, cx + dp(95), horizon, axis)
            canvas.drawLine(cx, horizon, cx, horizon - dp(85), cube)
            canvas.drawRect(cx - dp(35), horizon - dp(58), cx + dp(35), horizon + dp(12), cube)
            canvas.drawText("EDITOR PREVIEW", dp(12).toFloat(), dp(20).toFloat(), caption)
            canvas.drawText("Renderer module: next milestone", dp(12).toFloat(), height - dp(12).toFloat(), caption)
        }
    }
}
