package com.aster.engine

import android.app.Activity
import android.os.Bundle
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Color
import android.graphics.Path
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    companion object { init { System.loadLibrary("aster_engine") } }
    private external fun nativeGetEngineStatus(): String
    private external fun nativeStartEngine()
    private external fun nativeStopEngine()

    private lateinit var status: TextView
    private lateinit var hierarchy: LinearLayout
    private lateinit var inspector: LinearLayout
    private var objectCount = 2
    private var playing = false
    private var selectedObject = "Cube_01"

    private val bg = Color.rgb(13, 18, 26)
    private val panel = Color.rgb(22, 29, 40)
    private val panel2 = Color.rgb(29, 38, 51)
    private val muted = Color.rgb(145, 160, 180)
    private val bright = Color.rgb(229, 236, 245)
    private val cyan = Color.rgb(91, 190, 222)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        buildEditor()
    }

    private fun buildEditor() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
            setPadding(dp(5), dp(3), dp(5), dp(3))
        }

        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(5), 0, dp(5), 0)
        }
        val brand = TextView(this).apply {
            text = "✺  ASTER ENGINE  /  v1.0"
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(bright)
        }
        top.addView(brand, LinearLayout.LayoutParams(0, dp(42), 1.2f))
        status = smallBadge(nativeGetEngineStatus())
        top.addView(status)
        top.addView(topAction("SCENE") { showToast("Scene tools") })
        top.addView(topAction("ADD +") { addObject() })
        top.addView(topAction(if (playing) "STOP" else "▶ PLAY") { togglePlay() })
        top.addView(topAction("ASSETS") { showToast("Asset browser is planned for the asset-system milestone") })
        top.addView(topAction("⚙") { showToast("Engine settings will be added in a later milestone") })
        root.addView(top)

        val workspace = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val left = panelColumn()
        left.addView(sectionHeader("HIERARCHY"))
        hierarchy = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(4), dp(3), dp(4), dp(3))
        }
        addHierarchyRow("Camera")
        addHierarchyRow("Directional Light")
        addHierarchyRow("Cube_01")
        addHierarchyRow("Cube_02")
        left.addView(hierarchy)
        left.addView(TextView(this).apply {
            text = "⌕  SEARCH"
            textSize = 10f
            setTextColor(muted)
            setPadding(dp(8), dp(8), dp(4), dp(8))
        })
        workspace.addView(left, LinearLayout.LayoutParams(0, -1, 0.22f))

        val center = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(3), 0, dp(3), 0)
        }
        val viewportTitle = TextView(this).apply {
            text = "VIEWPORT / PERSPECTIVE     •     RENDERER: CORE PREVIEW"
            textSize = 9f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(muted)
            setBackgroundColor(panel2)
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(7), 0, dp(4), 0)
        }
        center.addView(viewportTitle, LinearLayout.LayoutParams(-1, dp(28)))
        center.addView(ViewportView(), LinearLayout.LayoutParams(-1, 0, 1f))
        workspace.addView(center, LinearLayout.LayoutParams(0, -1, 0.56f))

        val right = panelColumn()
        right.addView(sectionHeader("INSPECTOR"))
        right.addView(TextView(this).apply {
            text = selectedObject
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(bright)
            setPadding(dp(7), dp(8), dp(4), dp(8))
            setBackgroundColor(panel2)
        })
        right.addView(sectionHeader("⌖  TRANSFORM"))
        right.addView(propertyLine("Position", "X 0.0   Y 1.2   Z 0.0"))
        right.addView(propertyLine("Rotation", "X 0    Y 45    Z 0"))
        right.addView(propertyLine("Scale", "X 1.0   Y 1.0   Z 1.0"))
        right.addView(sectionHeader("▧  MESH RENDERER"))
        right.addView(propertyLine("Mesh", "Cube_Mesh"))
        right.addView(propertyLine("Material", "Default_Lit"))
        right.addView(sectionHeader("◈  PHYSICS"))
        right.addView(propertyLine("Body", "RigidBody (Static)"))
        workspace.addView(right, LinearLayout.LayoutParams(0, -1, 0.22f))
        root.addView(workspace, LinearLayout.LayoutParams(-1, 0, 1f))

        val bottom = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(Color.rgb(17, 23, 32))
            setPadding(dp(7), 0, dp(7), 0)
        }
        bottom.addView(TextView(this).apply {
            text = "●  Core initialized   •   C++20"
            textSize = 9f
            setTextColor(Color.rgb(117, 190, 164))
        }, LinearLayout.LayoutParams(0, dp(25), 1f))
        bottom.addView(TextView(this).apply {
            text = "CONSOLE   |   ASSETS   |   PROFILER     •     ANDROID CORE"
            textSize = 8f
            setTextColor(muted)
        })
        root.addView(bottom)
        setContentView(root)
    }

    private fun panelColumn() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setBackgroundColor(panel)
    }

    private fun sectionHeader(text: String) = TextView(this).apply {
        this.text = text
        textSize = 9f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(muted)
        setPadding(dp(7), dp(8), dp(3), dp(6))
        setBackgroundColor(Color.rgb(19, 25, 35))
    }

    private fun smallBadge(textValue: String) = TextView(this).apply {
        text = "● " + if (textValue.contains("ONLINE")) "CORE ONLINE" else "CORE READY"
        textSize = 9f
        setTextColor(Color.rgb(105, 220, 170))
        setPadding(dp(7), dp(6), dp(7), dp(6))
        setBackgroundColor(Color.rgb(25, 49, 45))
    }

    private fun topAction(title: String, action: () -> Unit) = TextView(this).apply {
        text = title
        textSize = 9f
        typeface = Typeface.DEFAULT_BOLD
        gravity = Gravity.CENTER
        setTextColor(bright)
        setPadding(dp(7), 0, dp(7), 0)
        setBackgroundColor(panel2)
        setOnClickListener { action() }
        layoutParams = LinearLayout.LayoutParams(-2, dp(30)).apply {
            setMargins(dp(2), 0, dp(2), 0)
        }
    }

    private fun propertyLine(title: String, value: String) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(7), dp(5), dp(4), dp(5))
        addView(TextView(this@MainActivity).apply {
            text = title
            textSize = 9f
            setTextColor(muted)
        })
        addView(TextView(this@MainActivity).apply {
            text = value
            textSize = 9f
            setTextColor(bright)
            setPadding(0, dp(3), 0, 0)
        })
    }

    private fun addObject() {
        objectCount++
        val name = "Cube_%02d".format(objectCount)
        addHierarchyRow(name)
        selectedObject = name
        showToast("$name added")
    }

    private fun addHierarchyRow(name: String) {
        hierarchy.addView(TextView(this).apply {
            text = if (name == selectedObject) "◆  $name" else "◇  $name"
            textSize = 11f
            setTextColor(if (name == selectedObject) cyan else bright)
            setPadding(dp(7), dp(7), dp(2), dp(7))
            setBackgroundColor(if (name == selectedObject) Color.rgb(35, 61, 76) else Color.TRANSPARENT)
            setOnClickListener {
                selectedObject = name
                showToast("Selected $name")
                buildEditor()
            }
        })
    }

    private fun togglePlay() {
        playing = !playing
        if (playing) nativeStartEngine() else nativeStopEngine()
        status.text = if (playing) "● CORE ONLINE • PLAY" else "● CORE READY"
        showToast(if (playing) "Play mode started" else "Play mode stopped")
    }

    private fun showToast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private inner class ViewportView : View(this@MainActivity) {
        private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(54, 72, 91); strokeWidth = dp(1).toFloat()
        }
        private val cubePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(103, 190, 225); style = Paint.Style.STROKE; strokeWidth = dp(2).toFloat()
        }
        private val xPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(219, 83, 78); strokeWidth = dp(2).toFloat() }
        private val yPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(103, 201, 118); strokeWidth = dp(2).toFloat() }
        private val zPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(77, 145, 224); strokeWidth = dp(2).toFloat() }
        private val caption = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(144, 164, 185); textSize = dp(9).toFloat()
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            canvas.drawColor(Color.rgb(25, 38, 52))
            val w = width.toFloat()
            val h = height.toFloat()
            val cx = w * 0.5f
            val horizon = h * 0.43f

            for (i in -18..18) {
                val x = cx + i * w / 18f
                canvas.drawLine(cx, horizon, x, h, gridPaint)
            }
            for (i in 1..14) {
                val y = horizon + (i * i * h / 210f)
                if (y < h) canvas.drawLine(0f, y, w, y, gridPaint)
            }

            val s = minOf(w * 0.15f, h * 0.22f)
            val top = horizon - s * 1.45f
            val front = Path().apply {
                moveTo(cx - s, top + s * 0.35f)
                lineTo(cx, top)
                lineTo(cx + s, top + s * 0.35f)
                lineTo(cx + s, top + s * 1.55f)
                lineTo(cx, top + s * 1.9f)
                lineTo(cx - s, top + s * 1.55f)
                close()
            }
            canvas.drawPath(front, cubePaint)
            canvas.drawLine(cx, top, cx, top + s * 1.9f, cubePaint)
            canvas.drawLine(cx - s, top + s * 0.35f, cx, top + s * 0.72f, cubePaint)
            canvas.drawLine(cx + s, top + s * 0.35f, cx, top + s * 0.72f, cubePaint)
            canvas.drawLine(cx, top + s * 0.72f, cx, top + s * 1.9f, cubePaint)

            val gx = cx
            val gy = top + s * 1.05f
            canvas.drawLine(gx, gy, gx + s * 1.45f, gy, xPaint)
            canvas.drawLine(gx, gy, gx, gy - s * 1.5f, yPaint)
            canvas.drawLine(gx, gy, gx - s * 0.95f, gy + s * 0.65f, zPaint)
            canvas.drawText("PERSPECTIVE", dp(8).toFloat(), dp(16).toFloat(), caption)
            canvas.drawText("GRID  •  SELECTED: $selectedObject", dp(8).toFloat(), h - dp(8).toFloat(), caption)
        }
    }
}
