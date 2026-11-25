package com.aster.engine

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.opengl.GLSurfaceView
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class MainActivity : Activity() {
    companion object { init { System.loadLibrary("aster_engine") } }
    private external fun nativeGetEngineStatus(): String
    private external fun nativeStartEngine()
    private external fun nativeStopEngine()
    private external fun nativeAddCube()
    private external fun nativeSurfaceCreated()
    private external fun nativeSurfaceChanged(width:Int,height:Int)
    private external fun nativeRender(deltaSeconds:Float)

    private lateinit var glView: EngineSurfaceView
    private lateinit var status:TextView
    private lateinit var hierarchy:LinearLayout
    private lateinit var inspectorName:TextView
    private var objectCount=2
    private var playing=true
    private var selected="Cube_02"
    private val bg=Color.rgb(12,17,25)
    private val panel=Color.rgb(21,29,40)
    private val panelHead=Color.rgb(16,23,33)
    private val muted=Color.rgb(145,160,180)
    private val bright=Color.rgb(231,238,247)
    private val cyan=Color.rgb(94,190,220)

    override fun onCreate(savedInstanceState:Bundle?){
        super.onCreate(savedInstanceState)
        window.statusBarColor=bg;window.navigationBarColor=bg
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility=View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        nativeStartEngine()
        buildEditor()
    }
    private fun buildEditor(){
        val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setBackgroundColor(bg);setPadding(dp(5),dp(3),dp(5),dp(3))}
        val top=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;setPadding(dp(5),0,dp(5),0)}
        top.addView(TextView(this).apply{text="✺  ASTER ENGINE  /  v1.2";textSize=15f;typeface=Typeface.DEFAULT_BOLD;setTextColor(bright)},
            LinearLayout.LayoutParams(0,dp(43),1f))
        status=badge("CORE ONLINE");top.addView(status)
        top.addView(action("SCENE"){toast("Scene hierarchy")})
        top.addView(action("ADD +"){addCube()})
        top.addView(action(if(playing)"STOP" else "▶ PLAY"){togglePlay()})
        top.addView(action("ASSETS"){toast("Asset registry ready; import pipeline is a later task")})
        top.addView(action("⚙"){toast("Editor settings are not implemented yet")})
        root.addView(top)

        val workspace=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL}
        val left=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setBackgroundColor(panel)}
        left.addView(section("HIERARCHY"))
        hierarchy=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(3),dp(3),dp(3),dp(3))}
        rebuildHierarchy()
        left.addView(hierarchy,LinearLayout.LayoutParams(-1,0,1f))
        left.addView(TextView(this).apply{text="⌕  SEARCH";textSize=10f;setTextColor(muted);setPadding(dp(8),dp(7),0,dp(7))})
        workspace.addView(left,LinearLayout.LayoutParams(0,-1,.22f))

        val center=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(3),0,dp(3),0)}
        center.addView(section("VIEWPORT / PERSPECTIVE   •   RENDERER: OPENGL ES 3.0"))
        glView=EngineSurfaceView()
        center.addView(glView,LinearLayout.LayoutParams(-1,0,1f))
        workspace.addView(center,LinearLayout.LayoutParams(0,-1,.56f))

        val right=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setBackgroundColor(panel)}
        right.addView(section("INSPECTOR"))
        inspectorName=TextView(this).apply{text=selected;textSize=14f;typeface=Typeface.DEFAULT_BOLD;setTextColor(bright);setPadding(dp(8),dp(9),dp(4),dp(9));setBackgroundColor(panelHead)}
        right.addView(inspectorName)
        right.addView(section("⌖  TRANSFORM"))
        right.addView(property("Position","X 0.0    Y 0.0    Z 0.0"))
        right.addView(property("Rotation","X 0    Y 20    Z 0"))
        right.addView(property("Scale","X 1.0    Y 1.0    Z 1.0"))
        right.addView(section("▧  MESH RENDERER"))
        right.addView(property("Mesh","Cube_Mesh"))
        right.addView(property("Material","Default_Lit"))
        right.addView(section("◈  PHYSICS"))
        right.addView(property("Status","Physics integration point"))
        workspace.addView(right,LinearLayout.LayoutParams(0,-1,.22f))
        root.addView(workspace,LinearLayout.LayoutParams(-1,0,1f))
        root.addView(TextView(this).apply{text="●  C++20 CORE   •   OPENGL ES 3.0   |   CONSOLE   |   ASSETS   |   PROFILER   •   ANDROID";textSize=9f;setTextColor(Color.rgb(115,190,164));gravity=Gravity.CENTER_VERTICAL;setPadding(dp(8),0,dp(4),0);setBackgroundColor(panelHead)},LinearLayout.LayoutParams(-1,dp(25)))
        setContentView(root)
    }
    private fun rebuildHierarchy(){
        hierarchy.removeAllViews()
        listOf("Camera","Directional Light").forEach{addHierarchyRow(it)}
        for(i in 1..objectCount)addHierarchyRow("Cube_%02d".format(i))
    }
    private fun addHierarchyRow(name:String){
        hierarchy.addView(TextView(this).apply{
            text=(if(name==selected)"◆  " else "◇  ")+name;textSize=11f
            setTextColor(if(name==selected)cyan else bright);setPadding(dp(7),dp(8),dp(2),dp(8))
            setBackgroundColor(if(name==selected)Color.rgb(35,62,77) else Color.TRANSPARENT)
            setOnClickListener{selected=name;inspectorName.text=name;rebuildHierarchy()}
        })
    }
    private fun addCube(){
        objectCount++;nativeAddCube();selected="Cube_%02d".format(objectCount)
        inspectorName.text=selected;rebuildHierarchy()
    }
    private fun togglePlay(){
        playing=!playing
        if(playing)nativeStartEngine() else nativeStopEngine()
        status.text=if(playing)"● CORE ONLINE" else "● CORE PAUSED"
        toast(if(playing)"Simulation resumed" else "Simulation paused")
    }
    private fun badge(textValue:String)=TextView(this).apply{text="● $textValue";textSize=9f;setTextColor(Color.rgb(105,220,170));setPadding(dp(7),dp(6),dp(7),dp(6));setBackgroundColor(Color.rgb(25,49,45))}
    private fun section(value:String)=TextView(this).apply{text=value;textSize=9f;typeface=Typeface.DEFAULT_BOLD;setTextColor(muted);setPadding(dp(7),dp(8),dp(3),dp(6));setBackgroundColor(panelHead)}
    private fun property(title:String,value:String)=LinearLayout(this).apply{
        orientation=LinearLayout.VERTICAL;setPadding(dp(7),dp(5),dp(3),dp(5))
        addView(TextView(this@MainActivity).apply{text=title;textSize=9f;setTextColor(muted)})
        addView(TextView(this@MainActivity).apply{text=value;textSize=9f;setTextColor(bright);setPadding(0,dp(3),0,0)})
    }
    private fun action(title:String,click:()->Unit)=TextView(this).apply{
        text=title;textSize=9f;typeface=Typeface.DEFAULT_BOLD;gravity=Gravity.CENTER;setTextColor(bright)
        setPadding(dp(7),0,dp(7),0);setBackgroundColor(Color.rgb(29,39,53));setOnClickListener{click()}
        layoutParams=LinearLayout.LayoutParams(-2,dp(30)).apply{setMargins(dp(2),0,dp(2),0)}
    }
    private fun toast(s:String)=Toast.makeText(this,s,Toast.LENGTH_SHORT).show()
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
    override fun onPause(){if(::glView.isInitialized)glView.onPause();super.onPause()}
    override fun onResume(){super.onResume();if(::glView.isInitialized)glView.onResume()}

    private inner class EngineSurfaceView:GLSurfaceView(this@MainActivity){
        init{
            setEGLContextClientVersion(3)
            preserveEGLContextOnPause=true
            setRenderer(object:Renderer{
                private var previous=0L
                override fun onSurfaceCreated(gl:GL10?,config:EGLConfig?){nativeSurfaceCreated();previous=System.nanoTime()}
                override fun onSurfaceChanged(gl:GL10?,w:Int,h:Int){nativeSurfaceChanged(w,h)}
                override fun onDrawFrame(gl:GL10?){
                    val now=System.nanoTime()
                    val delta=((now-previous).toDouble()/1.0e9).coerceIn(0.0,.1).toFloat()
                    previous=now
                    nativeRender(delta)
                }
            })
            renderMode=RENDERMODE_CONTINUOUSLY
        }
    }
}
