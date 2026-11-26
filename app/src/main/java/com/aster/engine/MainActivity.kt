package com.aster.engine
import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.opengl.GLSurfaceView
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class MainActivity:Activity(){
 companion object{init{System.loadLibrary("aster_engine")}}
 private external fun nativeGetEngineStatus():String
 private external fun nativeStartEngine()
 private external fun nativeStopEngine()
 private external fun nativeAddEntity(kind:Int)
 private external fun nativeSelectEntity(id:Int)
 private external fun nativeOrbit(dx:Float,dy:Float)
 private external fun nativePan(dx:Float,dy:Float)
 private external fun nativeZoom(factor:Float)
 private external fun nativeDragSelected(dx:Float,dy:Float)
 private external fun nativeSetPosition(id:Int,x:Float,y:Float,z:Float)
 private external fun nativeSetScale(id:Int,x:Float,y:Float,z:Float)
 private external fun nativeGetTransform(id:Int):FloatArray
 private external fun nativeSurfaceCreated()
 private external fun nativeSurfaceChanged(width:Int,height:Int)
 private external fun nativeRender(deltaSeconds:Float)
 private lateinit var glView:EngineSurfaceView
 private lateinit var hierarchy:LinearLayout
 private lateinit var inspectorName:TextView
 private lateinit var pos:TextView
 private lateinit var scale:TextView
 private lateinit var status:TextView
 private val names=mutableListOf("Camera","Directional Light","Cube_01","Cube_02")
 private val ids=mutableMapOf("Cube_01" to 1,"Cube_02" to 2)
 private var nextId=3
 private var selected="Cube_02"
 private var playing=true
 private var gizmo=true
 private val bg=Color.rgb(12,17,25);private val panel=Color.rgb(21,29,40);private val head=Color.rgb(16,23,33)
 private val muted=Color.rgb(145,160,180);private val white=Color.rgb(231,238,247);private val cyan=Color.rgb(94,190,220)
 override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);window.statusBarColor=bg;window.navigationBarColor=bg
 @Suppress("DEPRECATION") window.decorView.systemUiVisibility=View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
 nativeStartEngine();buildEditor()}
 private fun buildEditor(){
  val root=LinearLayout(this).apply{orientation=1;setBackgroundColor(bg);setPadding(dp(5),dp(3),dp(5),dp(3))}
  val top=LinearLayout(this).apply{orientation=0;gravity=Gravity.CENTER_VERTICAL}
  top.addView(label("✺ ASTER ENGINE / v1.3",15,true),LinearLayout.LayoutParams(0,dp(42),1f))
  status=label("● CORE ONLINE",9);top.addView(status)
  top.addView(action("SCENE"){})
  top.addView(action("ADD +"){showAddMenu(it)})
  top.addView(action("MOVE"){gizmo=!gizmo;toast(if(gizmo)"Gizmo drag enabled" else "Camera orbit enabled")})
  top.addView(action(if(playing)"STOP" else "PLAY"){togglePlay()})
  top.addView(action("ASSETS"){toast("Asset registry prototype")})
  root.addView(top)
  val work=LinearLayout(this).apply{orientation=0}
  val left=LinearLayout(this).apply{orientation=1;setBackgroundColor(panel);addView(section("HIERARCHY"))}
  hierarchy=LinearLayout(this).apply{orientation=1}
  left.addView(hierarchy,LinearLayout.LayoutParams(-1,0,1f));work.addView(left,LinearLayout.LayoutParams(0,-1,.22f))
  val center=LinearLayout(this).apply{orientation=1;addView(section("VIEWPORT / PERSPECTIVE • OPENGL ES 3.0"))}
  glView=EngineSurfaceView();center.addView(glView,LinearLayout.LayoutParams(-1,0,1f));work.addView(center,LinearLayout.LayoutParams(0,-1,.56f))
  val right=LinearLayout(this).apply{orientation=1;setBackgroundColor(panel);addView(section("INSPECTOR"))}
  inspectorName=label(selected,14,true);right.addView(inspectorName);right.addView(section("TRANSFORM"))
  pos=property("Position","Tap to edit");pos.setOnClickListener{editVector(false)};right.addView(pos)
  right.addView(property("Rotation","Automatic in Play mode"))
  scale=property("Scale","Tap to edit");scale.setOnClickListener{editVector(true)};right.addView(scale)
  right.addView(section("MESH RENDERER"));right.addView(property("Mesh","Cube_Mesh"));right.addView(property("Material","Default_Lit"))
  right.addView(section("PHYSICS"));right.addView(property("Mode","Gravity / rotation prototype"))
  work.addView(right,LinearLayout.LayoutParams(0,-1,.22f))
  root.addView(work,LinearLayout.LayoutParams(-1,0,1f));root.addView(label("C++20 CORE • TOUCH ORBIT / PAN / PINCH • ANDROID",9),LinearLayout.LayoutParams(-1,dp(24)))
  setContentView(root);rebuildHierarchy();refreshInspector()
 }
 private fun showAddMenu(anchor:View){
  val m=PopupMenu(this,anchor)
  listOf("Cube","Sphere","Light","Camera").forEachIndexed{i,n->m.menu.add(n).setOnMenuItemClickListener{
   nativeAddEntity(i);val id=nextId++;val name=when(i){2->"Point Light_%02d".format(id-2);3->"Camera_%02d".format(id-2);else->"${n}_%02d".format(id-2)}
   names.add(name);ids[name]=id;select(name);true
  }};m.show()
 }
 private fun select(n:String){selected=n;ids[n]?.let{nativeSelectEntity(it)};inspectorName.text=n;rebuildHierarchy();refreshInspector()}
 private fun rebuildHierarchy(){hierarchy.removeAllViews();names.forEach{n->val row=label((if(n==selected)"◆ " else "◇ ")+n,11);row.setPadding(dp(7),dp(8),dp(2),dp(8));row.setBackgroundColor(if(n==selected)Color.rgb(35,62,77) else Color.TRANSPARENT);row.setOnClickListener{select(n)};hierarchy.addView(row)}}
 private fun refreshInspector(){val id=ids[selected]?:return;val v=nativeGetTransform(id);pos.text="Position  X %.2f  Y %.2f  Z %.2f".format(v[0],v[1],v[2]);scale.text="Scale  X %.2f  Y %.2f  Z %.2f".format(v[3],v[4],v[5])}
 private fun editVector(isScale:Boolean){val id=ids[selected]?:return;val v=nativeGetTransform(id);val off=if(isScale)3 else 0
  val inputs=Array(3){i->EditText(this).apply{setSingleLine();setText(v[off+i].toString());hint=listOf("X","Y","Z")[i];inputType=8194}}
  val box=LinearLayout(this).apply{orientation=0;setPadding(dp(8),0,dp(8),0);inputs.forEach{addView(it,LinearLayout.LayoutParams(0,dp(52),1f))}}
  AlertDialog.Builder(this).setTitle(if(isScale)"Edit Scale" else "Edit Position").setView(box).setNegativeButton("Cancel",null).setPositiveButton("Apply"){_,_->val a=FloatArray(3){i->inputs[i].text.toString().toFloatOrNull()?:v[off+i]};if(isScale)nativeSetScale(id,a[0],a[1],a[2])else nativeSetPosition(id,a[0],a[1],a[2]);refreshInspector();glView.requestRender()}.show()
 }
 private fun togglePlay(){playing=!playing;if(playing)nativeStartEngine()else nativeStopEngine();status.text=if(playing)"● CORE ONLINE" else "● CORE PAUSED";toast(if(playing)"Play started" else "Play stopped")}
 private fun label(s:String,size:Int,bold:Boolean=false)=TextView(this).apply{text=s;textSize=size.toFloat();setTextColor(if(s.contains("CORE ONLINE"))Color.rgb(105,220,170)else white);if(bold)typeface=Typeface.DEFAULT_BOLD;gravity=Gravity.CENTER_VERTICAL;setPadding(dp(6),dp(5),dp(5),dp(5))}
 private fun section(s:String)=label(s,9,true).apply{setTextColor(muted);setBackgroundColor(head)}
 private fun property(k:String,v:String)=LinearLayout(this).apply{orientation=1;setPadding(dp(7),dp(6),dp(3),dp(6));addView(label(k,9).apply{setTextColor(muted)});addView(label(v,9))}
 private fun action(s:String,fn:(View)->Unit)=label(s,9,true).apply{gravity=Gravity.CENTER;setBackgroundColor(Color.rgb(29,39,53));layoutParams=LinearLayout.LayoutParams(-2,dp(30)).apply{setMargins(dp(2),0,dp(2),0)};setOnClickListener{fn(it)}}
 private fun toast(s:String)=Toast.makeText(this,s,Toast.LENGTH_SHORT).show()
 private fun dp(n:Int)=(n*resources.displayMetrics.density).toInt()
 override fun onPause(){if(::glView.isInitialized)glView.onPause();super.onPause()}
 override fun onResume(){super.onResume();if(::glView.isInitialized)glView.onResume()}
 private inner class EngineSurfaceView:GLSurfaceView(this@MainActivity){
  private var x0=0f;private var y0=0f;private var oldDist=0f
  init{setEGLContextClientVersion(3);preserveEGLContextOnPause=true
   setRenderer(object:Renderer{var last=0L;override fun onSurfaceCreated(gl:GL10?,c:EGLConfig?){nativeSurfaceCreated();last=System.nanoTime()}
    override fun onSurfaceChanged(gl:GL10?,w:Int,h:Int){nativeSurfaceChanged(w,h)}
    override fun onDrawFrame(gl:GL10?){val now=System.nanoTime();nativeRender(((now-last)/1e9).toFloat().coerceIn(0f,.1f));last=now}
   });renderMode=RENDERMODE_CONTINUOUSLY
   setOnTouchListener{_,e->when(e.actionMasked){
    MotionEvent.ACTION_DOWN->{x0=e.x;y0=e.y;true}
    MotionEvent.ACTION_POINTER_DOWN->{oldDist=dist(e);x0=midX(e);y0=midY(e);true}
    MotionEvent.ACTION_MOVE->{if(e.pointerCount>=2){val d=dist(e);if(oldDist>0&&d>0)nativeZoom(oldDist/d);nativePan(midX(e)-x0,midY(e)-y0);oldDist=d;x0=midX(e);y0=midY(e)}else{val dx=e.x-x0;val dy=e.y-y0;if(gizmo)nativeDragSelected(dx,dy)else nativeOrbit(dx,dy);x0=e.x;y0=e.y};requestRender();true}
    MotionEvent.ACTION_UP,MotionEvent.ACTION_CANCEL->{oldDist=0f;true};else->true
   }}
  }
  private fun dist(e:MotionEvent):Float{if(e.pointerCount<2)return 0f;val x=e.getX(0)-e.getX(1);val y=e.getY(0)-e.getY(1);return kotlin.math.sqrt(x*x+y*y)}
  private fun midX(e:MotionEvent)=(e.getX(0)+e.getX(1))*.5f
  private fun midY(e:MotionEvent)=(e.getY(0)+e.getY(1))*.5f
 }
}