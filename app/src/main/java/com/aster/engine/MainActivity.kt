package com.aster.engine
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import java.io.File
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.GradientDrawable
import android.widget.FrameLayout
import android.opengl.GLSurfaceView
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class MainActivity:Activity(){
 companion object{init{System.loadLibrary("aster_engine")}}
 private external fun nativeImportModel(path:String,entityId:Int):String\n private external fun nativeSetPbrTextures(id:Int,albedo:String,normal:String,roughness:String,ao:String):String\n private external fun nativeSetPbrTiling(id:Int,value:Float)
 private external fun nativeResetScene()
 private external fun nativeStartEngine()
 private external fun nativeStopEngine()
 private external fun nativePlayMove(forward:Float,strafe:Float,deltaSeconds:Float)
 private external fun nativePlayLook(dx:Float,dy:Float)
 private external fun nativeSetThirdPerson(entityId:Int,enabled:Boolean)
 private external fun nativeAddEntity(kind:Int)
 private external fun nativeSelectEntity(id:Int)
 private external fun nativeDeleteEntity(id:Int)
 private external fun nativeOrbit(dx:Float,dy:Float)
 private external fun nativePan(dx:Float,dy:Float)
 private external fun nativeZoom(factor:Float)
 private external fun nativePickGizmoAxis(x:Float,y:Float):Int
 private external fun nativePickEntity(x:Float,y:Float):Int
 private external fun nativeDragGizmoAxis(axis:Int,dx:Float,dy:Float)
 private external fun nativeSetPosition(id:Int,x:Float,y:Float,z:Float)
 private external fun nativeSetScale(id:Int,x:Float,y:Float,z:Float)
 private external fun nativeSetRotation(id:Int,x:Float,y:Float,z:Float)
 private external fun nativeSetStatic(id:Int,value:Boolean)
 private external fun nativeSetBodyProperties(id:Int,mass:Float,friction:Float,restitution:Float,gravity:Boolean)
 private external fun nativeSetVelocity(id:Int,x:Float,y:Float,z:Float)
 private external fun nativeGetTransform(id:Int):FloatArray
 private external fun nativeSurfaceCreated()
 private external fun nativeSurfaceChanged(width:Int,height:Int)
 private external fun nativeRender(deltaSeconds:Float)
 private val modelPicker=7201\n private val pbrPicker=7202\n private var pendingPbrEntity=0
 private val modelPaths=mutableListOf<String>()
 private val modelByEntity=linkedMapOf<String,String>()
 private val scriptsByEntity=linkedMapOf<String,String>()
 private val componentsByEntity=linkedMapOf<String,JSONObject>()
 private val materialsByEntity=linkedMapOf<String,JSONObject>()
 private val undoStack=java.util.ArrayDeque<String>()
 private lateinit var leftPanel:LinearLayout
 private lateinit var rightPanel:LinearLayout
 private var panelsVisible=true
 private var undoing=false
 private var projectId=""
 private var projectName="Untitled Project"
 private var projectTemplate="3D Starter"
 private lateinit var glView:EngineSurfaceView
 private lateinit var editorTop:LinearLayout
 private lateinit var editorWork:LinearLayout
 private lateinit var editorFooter:TextView
 private lateinit var viewportHeader:View
 private lateinit var playJoystick:View
 private val topEditorControls=mutableListOf<View>()
 private lateinit var hierarchy:LinearLayout
 private lateinit var inspectorName:TextView
 private lateinit var pos:TextView
 private lateinit var scale:TextView
 private lateinit var status:TextView
 private val names=mutableListOf<String>()
 private val ids=linkedMapOf<String,Int>()
 private var nextId=1
 private var selected=""
 private var playing=false
 private lateinit var playButton:TextView
 private var gizmo=true
 private val bg=Color.rgb(12,17,25);private val panel=Color.rgb(21,29,40);private val head=Color.rgb(16,23,33)
 private val muted=Color.rgb(145,160,180);private val white=Color.rgb(231,238,247);private val cyan=Color.rgb(94,190,220)

 override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);window.statusBarColor=bg;window.navigationBarColor=bg
  @Suppress("DEPRECATION") window.decorView.systemUiVisibility=View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
  projectId=intent.getStringExtra("project_id")?:run{val id=ProjectStore.create(this,"My First Project","3D Starter");id}
  loadProject();buildEditor()
 }
 private fun loadProject(){
  val data=ProjectStore.read(this,projectId);projectName=data.optString("name","Untitled Project");projectTemplate=data.optString("template","3D Starter")
  modelPaths.clear();val saved=data.optJSONArray("assets")?:JSONArray();for(i in 0 until saved.length())modelPaths.add(saved.optString(i));modelByEntity.clear();scriptsByEntity.clear();componentsByEntity.clear();materialsByEntity.clear()
  nativeResetScene();names.clear();ids.clear();names.add("Camera");names.add("Directional Light");nextId=1
  val entities=data.optJSONArray("entities")?:JSONArray()
  for(i in 0 until entities.length()){
   val e=entities.getJSONObject(i);val kind=e.optInt("kind",0).coerceIn(0,3)
   nativeAddEntity(kind);val id=nextId++;val n=e.optString("name","Object_"+id)
   names.add(n);ids[n]=id
   e.optString("assetPath").takeIf{it.isNotBlank()}?.let{modelByEntity[n]=it};e.optString("script").takeIf{it.isNotBlank()}?.let{scriptsByEntity[n]=it};e.optJSONObject("components")?.let{componentsByEntity[n]=it};e.optJSONObject("material")?.let{materialsByEntity[n]=it}
   nativeSetPosition(id,e.optDouble("x",0.0).toFloat(),e.optDouble("y",0.0).toFloat(),e.optDouble("z",0.0).toFloat())
   nativeSetScale(id,e.optDouble("sx",1.0).toFloat(),e.optDouble("sy",1.0).toFloat(),e.optDouble("sz",1.0).toFloat())
   nativeSetRotation(id,e.optDouble("rx",0.0).toFloat(),e.optDouble("ry",0.0).toFloat(),e.optDouble("rz",0.0).toFloat())
   nativeSetStatic(id,e.optBoolean("isStatic",false))
   nativeSetBodyProperties(id,e.optDouble("mass",1.0).toFloat(),e.optDouble("friction",.65).toFloat(),e.optDouble("restitution",.05).toFloat(),e.optBoolean("useGravity",true))
   nativeSetVelocity(id,e.optDouble("vx",0.0).toFloat(),e.optDouble("vy",0.0).toFloat(),e.optDouble("vz",0.0).toFloat())
  }
  selected=ids.keys.lastOrNull()?: "";if(selected.isNotEmpty())nativeSelectEntity(ids[selected]!!)
 }
 private fun saveProject(){
  if(projectId.isEmpty())return
  val array=JSONArray()
  for((name,id) in ids){val v=nativeGetTransform(id);val kind=when{ name.startsWith("Sphere")->1;name.startsWith("Point Light")->2;name.startsWith("Camera_")->3;else->0}
   val se=ProjectStore.entity(name,kind,v[0],v[1],v[2],v[3],v[4],v[5],v[6],v[7],v[8],v[9]>.5f,v[10],v[11],v[12],v[13]>.5f,v[14],v[15],v[16]);se.put("assetPath",modelByEntity[name]?:"").put("script",scriptsByEntity[name]?:"").put("components",componentsByEntity[name]?:JSONObject().put("Transform",true).put("Physics",true)).put("material",materialsByEntity[name]?:JSONObject().put("baseColor","#50BFD8").put("metallic",0.0).put("roughness",.72));array.put(se)
  }
  ProjectStore.save(this,projectId,projectName,projectTemplate,array)
  val data=ProjectStore.read(this,projectId);val assets=JSONArray();modelPaths.forEach{assets.put(it)};data.put("assets",assets);ProjectStore.writeRaw(this,projectId,data)
 }
 private fun buildEditor(){
  val root=LinearLayout(this).apply{orientation=1;setBackgroundColor(bg);setPadding(dp(5),dp(3),dp(5),dp(3))}
  val top=LinearLayout(this).apply{orientation=0;gravity=Gravity.CENTER_VERTICAL;layoutDirection=View.LAYOUT_DIRECTION_RTL};editorTop=top
  top.addView(label("✺ أستر — "+projectName,13,true),LinearLayout.LayoutParams(0,dp(42),1f))
  status=label("● المحرّك يعمل",9);top.addView(status)
  top.addView(action("المشاريع"){saveProject();startActivity(Intent(this@MainActivity,ProjectHubActivity::class.java));finish()})
  top.addView(action("تراجع"){undoLast()})
  top.addView(action("القوائم"){panelsVisible=!panelsVisible;leftPanel.visibility=if(panelsVisible)View.VISIBLE else View.GONE;rightPanel.visibility=if(panelsVisible)View.VISIBLE else View.GONE;glView.requestLayout()})
  top.addView(action("إضافة +"){showAddMenu(it)})
  top.addView(action("تحريك"){gizmo=!gizmo;toast(if(gizmo)"تم تفعيل تحريك المحاور" else "تم تفعيل تدوير الكاميرا")})
  playButton=action("▶ تجربة اللعب"){togglePlay()};top.addView(playButton)
  top.addView(action("الأصول"){showAssetBrowser()})
  top.addView(action("استيراد نموذج"){pickModel()})
  top.addView(action("مواقع النماذج"){showModelSources()})
  root.addView(top)
  val work=LinearLayout(this).apply{orientation=0};editorWork=work
  leftPanel=LinearLayout(this).apply{orientation=1;setBackgroundColor(panel);addView(section("التسلسل الهرمي"))}
  val left=leftPanel
  hierarchy=LinearLayout(this).apply{orientation=1}
  left.addView(hierarchy,LinearLayout.LayoutParams(-1,0,1f));work.addView(left,LinearLayout.LayoutParams(0,-1,.22f))
  val center=LinearLayout(this).apply{orientation=1;viewportHeader=section("منظور المشهد • OpenGL ES 3.0");addView(viewportHeader)}
  val viewport=FrameLayout(this);glView=EngineSurfaceView();viewport.addView(glView,FrameLayout.LayoutParams(-1,-1))
  val joystick=JoystickView(this){forward,strafe->if(playing)glView.queueEvent{nativePlayMove(forward,strafe,1f/60f)}};playJoystick=joystick
  viewport.addView(joystick,FrameLayout.LayoutParams(dp(148),dp(148),Gravity.BOTTOM or Gravity.LEFT).apply{leftMargin=dp(14);bottomMargin=dp(14)})
  joystick.visibility=View.GONE;center.addView(viewport,LinearLayout.LayoutParams(-1,0,1f));work.addView(center,LinearLayout.LayoutParams(0,-1,.56f))
  rightPanel=LinearLayout(this).apply{orientation=1;setBackgroundColor(panel);addView(section("الخصائص"))}
  val right=rightPanel
  inspectorName=label(selected.ifEmpty{"لا يوجد تحديد"},14,true);right.addView(inspectorName)
  val selectedActions=LinearLayout(this).apply{orientation=0;gravity=Gravity.CENTER}
  selectedActions.addView(action("حذف العنصر"){deleteSelected()});selectedActions.addView(action("تكرار"){duplicateSelected()});selectedActions.addView(action("إعادة ضبط"){resetSelected()});selectedActions.addView(action("</> برمجة"){editScript()});selectedActions.addView(action("المكونات والخامة"){editComponentsMaterial()})
  right.addView(selectedActions);right.addView(action("تغيير خامة العنصر / رفع خامة"){pickPbrTexture()});right.addView(action("تكرار الخامة UV / Scale"){editPbrTiling()});right.addView(section("التحويل"))
  pos=property("الموقع","اضغط للتعديل");pos.setOnClickListener{editVector(false)};right.addView(pos)
  right.addView(property("الدوران","اضغط للتعديل").apply{setOnClickListener{editTripleDialog("الدوران (راديان)",6){a->val id=ids[selected]?:return@editTripleDialog;nativeSetRotation(id,a[0],a[1],a[2]);saveProject();glView.requestRender()}}})
  scale=property("الحجم","اضغط للتعديل");scale.setOnClickListener{editVector(true)};right.addView(scale)
  right.addView(section("عارض المجسم"));right.addView(property("نوع المجسم","حسب نوع الكائن"));right.addView(property("الخامة","حسب النموذج المستورد"))
  right.addView(section("الفيزياء"));right.addView(property("جسم فيزيائي","اضغط للضبط").apply{setOnClickListener{editPhysics()}});right.addView(property("السرعة","اضغط للتعديل").apply{setOnClickListener{editTripleDialog("السرعة",14){a->ids[selected]?.let{id->nativeSetVelocity(id,a[0],a[1],a[2]);saveProject()}}}})
  work.addView(right,LinearLayout.LayoutParams(0,-1,.22f))
  root.addView(work,LinearLayout.LayoutParams(-1,0,1f));editorFooter=label("نواة C++20 • حفظ محلي للمشروع • أندرويد",9);root.addView(editorFooter,LinearLayout.LayoutParams(-1,dp(24)))
  setContentView(root);rebuildHierarchy();refreshInspector()
 }
 private fun showAddMenu(anchor:View){val m=PopupMenu(this,anchor);listOf("Cube","Sphere","Light","Camera").forEachIndexed{i,n->m.menu.add(n).setOnMenuItemClickListener{
  recordUndo();nativeAddEntity(i);val id=nextId++;val name=when(i){2->"Point Light_"+id;3->"Camera_"+id;else->n+"_"+id};names.add(name);ids[name]=id;select(name);saveProject();true
 }};m.show()}
 private fun select(n:String){selected=n;ids[n]?.let{nativeSelectEntity(it)};inspectorName.text=n;rebuildHierarchy();refreshInspector()}
 private fun rebuildHierarchy(){hierarchy.removeAllViews();names.forEach{n->val row=label((if(n==selected)"◆ " else "◇ ")+n,11);row.setPadding(dp(7),dp(8),dp(2),dp(8));row.setBackgroundColor(if(n==selected)Color.rgb(35,62,77)else Color.TRANSPARENT);row.setOnClickListener{select(n)};hierarchy.addView(row)}}
 private fun editScript(){val n=selected;if(n.isBlank()||ids[n]==null)return;val e=EditText(this).apply{setText(scriptsByEntity[n]?:"// أوامر العنصر\\n// الحركة والتفاعل\\n");minLines=8;gravity=Gravity.TOP;inputType=android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE};AlertDialog.Builder(this).setTitle("برمجة: $n").setView(e).setNeutralButton("نسخ",null).setNegativeButton("إلغاء",null).setPositiveButton("حفظ"){_,_->recordUndo();scriptsByEntity[n]=e.text.toString();saveProject()}.show().also{d->d.getButton(AlertDialog.BUTTON_NEUTRAL)?.setOnClickListener{(getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager).setPrimaryClip(android.content.ClipData.newPlainText("script",e.text));toast("تم نسخ الكود")}}}
 private fun editComponentsMaterial(){val n=selected;if(n.isBlank()||ids[n]==null)return;val c=componentsByEntity[n]?:JSONObject();val m=materialsByEntity[n]?:JSONObject();val mesh=android.widget.CheckBox(this).apply{text="عرض المجسم";isChecked=c.optBoolean("MeshRenderer",modelByEntity.containsKey(n))};val phys=android.widget.CheckBox(this).apply{text="الفيزياء";isChecked=c.optBoolean("Physics",true)};val script=android.widget.CheckBox(this).apply{text="البرمجة";isChecked=c.optBoolean("Script",scriptsByEntity.containsKey(n))};val color=EditText(this).apply{hint="لون الخامة #RRGGBB";setText(m.optString("baseColor","#50BFD8"))};val metal=EditText(this).apply{hint="المعدنية 0..1";setText(m.optDouble("metallic",0.0).toString());inputType=8194};val rough=EditText(this).apply{hint="الخشونة 0..1";setText(m.optDouble("roughness",.72).toString());inputType=8194};val box=LinearLayout(this).apply{orientation=1;addView(mesh);addView(phys);addView(script);addView(color);addView(metal);addView(rough)};AlertDialog.Builder(this).setTitle("مكونات وخامة: $n").setView(box).setNegativeButton("إلغاء",null).setPositiveButton("حفظ"){_,_->recordUndo();componentsByEntity[n]=JSONObject().put("Transform",true).put("MeshRenderer",mesh.isChecked).put("Physics",phys.isChecked).put("Script",script.isChecked);materialsByEntity[n]=JSONObject().put("baseColor",color.text.toString()).put("metallic",(metal.text.toString().toDoubleOrNull()?:0.0).coerceIn(0.0,1.0)).put("roughness",(rough.text.toString().toDoubleOrNull()?:.72).coerceIn(0.0,1.0));saveProject()}.show()}
 private fun refreshInspector(){val id=ids[selected]?:return;val v=nativeGetTransform(id);pos.text="الموقع X %.2f Y %.2f Z %.2f".format(v[0],v[1],v[2]);scale.text="الحجم X %.2f Y %.2f Z %.2f".format(v[3],v[4],v[5])}
 private fun editVector(isScale:Boolean){val id=ids[selected]?:return;val v=nativeGetTransform(id);val off=if(isScale)3 else 0
  val inputs=Array(3){i->EditText(this).apply{setSingleLine();setText(v[off+i].toString());hint=listOf("X","Y","Z")[i];inputType=8194}}
  val box=LinearLayout(this).apply{orientation=0;setPadding(dp(8),0,dp(8),0);inputs.forEach{addView(it,LinearLayout.LayoutParams(0,dp(52),1f))}}
  AlertDialog.Builder(this).setTitle(if(isScale)"تعديل الحجم" else "تعديل الموقع").setView(box).setNegativeButton("إلغاء",null).setPositiveButton("تطبيق"){_,_->recordUndo();val a=FloatArray(3){i->inputs[i].text.toString().toFloatOrNull()?:v[off+i]};if(isScale)nativeSetScale(id,a[0],a[1],a[2])else nativeSetPosition(id,a[0],a[1],a[2]);refreshInspector();glView.requestRender();saveProject()}.show()
 }
 private fun editTripleDialog(title:String,offset:Int,apply:(FloatArray)->Unit){
  val id=ids[selected]?:return;val v=nativeGetTransform(id)
  val fields=Array(3){i->EditText(this).apply{setSingleLine();setText(v[offset+i].toString());hint=listOf("X","Y","Z")[i];inputType=8194}}
  val box=LinearLayout(this).apply{orientation=0;setPadding(dp(8),0,dp(8),0);fields.forEach{addView(it,LinearLayout.LayoutParams(0,dp(52),1f))}}
  AlertDialog.Builder(this).setTitle(title).setView(box).setNegativeButton("إلغاء",null).setPositiveButton("تطبيق"){_,_->recordUndo();apply(FloatArray(3){i->fields[i].text.toString().toFloatOrNull()?:v[offset+i]})}.show()
 }
 private fun editPhysics(){
  val id=ids[selected]?:return;val v=nativeGetTransform(id)
  val fixed=android.widget.CheckBox(this).apply{text="ثابت (Static) — إيقاف الحركة الفيزيائية";isChecked=v[9]>.5f}
  val gravity=android.widget.CheckBox(this).apply{text="استخدام الجاذبية";isChecked=v[13]>.5f}
  val mass=EditText(this).apply{hint="الكتلة";setText(v[10].toString());inputType=8194}
  val friction=EditText(this).apply{hint="الاحتكاك (0..1)";setText(v[11].toString());inputType=8194}
  val bounce=EditText(this).apply{hint="الارتداد (0..1)";setText(v[12].toString());inputType=8194}
  val box=LinearLayout(this).apply{orientation=1;setPadding(dp(12),0,dp(12),0);addView(fixed);addView(gravity);addView(mass);addView(friction);addView(bounce)}
  AlertDialog.Builder(this).setTitle("إعدادات الفيزياء").setView(box).setNegativeButton("إلغاء",null).setPositiveButton("تطبيق"){_,_->
   recordUndo();nativeSetStatic(id,fixed.isChecked)
   nativeSetBodyProperties(id,mass.text.toString().toFloatOrNull()?:v[10],friction.text.toString().toFloatOrNull()?:v[11],bounce.text.toString().toFloatOrNull()?:v[12],gravity.isChecked)
   saveProject();glView.requestRender()
  }.show()
 }
 private fun recordUndo(){if(undoing||projectId.isEmpty())return;saveProject();undoStack.addLast(ProjectStore.read(this,projectId).toString());while(undoStack.size>30)undoStack.removeFirst()}
 private fun undoLast(){if(undoStack.isEmpty()){toast("لا توجد تعديلات للتراجع عنها");return};val raw=undoStack.removeLast();runCatching{ProjectStore.writeRaw(this,projectId,org.json.JSONObject(raw));undoing=true;loadProject();rebuildHierarchy();refreshInspector();glView.requestRender();undoing=false;toast("تم التراجع عن آخر تعديل")}.onFailure{undoing=false;toast("تعذر التراجع عن التعديل")}}
 private fun showAssetBrowser(){
  val files=ProjectStore.assetFiles(this,projectId)
  if(files.isEmpty()){AlertDialog.Builder(this).setTitle("متصفح الأصول").setMessage("لا توجد نماذج مستوردة. أضف ملف GLB أو glTF أولاً.").setPositiveButton("استيراد نموذج"){_,_->pickModel()}.setNegativeButton("إغلاق",null).show();return}
  val content=LinearLayout(this).apply{orientation=1;setPadding(dp(12),dp(4),dp(12),dp(4))}
  val scroll=android.widget.ScrollView(this).apply{addView(content)}
  fun refresh(){
   content.removeAllViews()
   ProjectStore.assetFiles(this,projectId).forEach{file->
    val row=LinearLayout(this).apply{orientation=0;gravity=Gravity.CENTER_VERTICAL}
    row.addView(label(file.name,11),LinearLayout.LayoutParams(0,dp(48),1f))
    row.addView(action("تحميل"){ 
     val relative=file.relativeTo(File(ProjectStore.storagePath(this),projectId)).invariantSeparatorsPath
     if(!modelPaths.contains(relative)){recordUndo();modelPaths.add(relative);saveProject()}
     glView.queueEvent{val msg=nativeImportModel(file.absolutePath,ids[selected]?:0);runOnUiThread{toast(msg)}}
    })
    row.addView(action("حذف"){ 
     AlertDialog.Builder(this).setTitle("حذف الأصل").setMessage("سيُحذف الملف من المشروع نهائياً. هل تريد المتابعة؟")
      .setNegativeButton("إلغاء",null).setPositiveButton("حذف"){_,_->
       recordUndo()
       val relative=file.relativeTo(File(ProjectStore.storagePath(this),projectId)).invariantSeparatorsPath
       runCatching{ProjectStore.deleteAsset(this,projectId,relative)}.onSuccess{
        modelPaths.remove(relative);saveProject();refresh();toast("تم حذف الأصل")
       }.onFailure{toast(it.message?:"تعذر حذف الأصل")}
      }.show()
    })
    content.addView(row)
   }
  }
  refresh()
  AlertDialog.Builder(this).setTitle("أصول المشروع").setView(scroll).setPositiveButton("إغلاق",null).setNeutralButton("استيراد"){_,_->pickModel()}.show()
 }
 private fun pickPbrTexture(){val id=ids[selected]?:return;pendingPbrEntity=id;startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply{addCategory(Intent.CATEGORY_OPENABLE);type="*/*";putExtra(Intent.EXTRA_MIME_TYPES,arrayOf("application/zip","image/png","image/jpeg","application/octet-stream"))},pbrPicker)}\n private fun editPbrTiling(){val id=ids[selected]?:return;val name=selected;val mat=materialsByEntity[name]?:JSONObject();val field=EditText(this).apply{hint="UV Tiling (0.1 - 64)";setText(mat.optDouble("uvTiling",4.0).toString());inputType=8194};AlertDialog.Builder(this).setTitle("تكرار الخامة").setMessage("قيمة أكبر تعني تكرار التفاصيل أكثر وتقليل تمددها.").setView(field).setNegativeButton("إلغاء",null).setPositiveButton("تطبيق"){_,_->val v=(field.text.toString().toFloatOrNull()?:4f).coerceIn(.1f,64f);mat.put("uvTiling",v.toDouble());materialsByEntity[name]=mat;glView.queueEvent{nativeSetPbrTiling(id,v)};saveProject();glView.requestRender()}.show()}\n private fun pickModel(){startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply{addCategory(Intent.CATEGORY_OPENABLE);type="*/*";putExtra(Intent.EXTRA_MIME_TYPES,arrayOf("model/gltf-binary","model/gltf+json","application/octet-stream"))},modelPicker)}
 @Deprecated("Use Activity Result API")
 override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?){super.onActivityResult(requestCode,resultCode,data);if(requestCode==pbrPicker&&resultCode==RESULT_OK){val uri=data?.data?:return;val id=pendingPbrEntity;val name=ids.entries.firstOrNull{it.value==id}?.key?:return;runCatching{ProjectStore.importPbrTexture(this,projectId,uri)}.onSuccess{maps->val mat=materialsByEntity[name]?:JSONObject();for(key in listOf("albedo","normal","roughness","ao"))if(maps.has(key))mat.put(key+"Path",maps.optString(key));materialsByEntity[name]=mat;saveProject();val path={key:String->mat.optString(key+"Path","")};glView.queueEvent{val msg=nativeSetPbrTextures(id,path("albedo"),path("normal"),path("roughness"),path("ao"));runOnUiThread{toast(msg);glView.requestRender()}}}.onFailure{toast(it.message?:"تعذر استيراد الخامة")};return};if(requestCode==modelPicker&&resultCode==RESULT_OK){val uri=data?.data?:return;runCatching{ProjectStore.importModel(this,projectId,uri)}.onSuccess{relative->if(!modelPaths.contains(relative))modelPaths.add(relative);recordUndo();nativeAddEntity(0);val id=nextId++;val n="Model_$id";names.add(n);ids[n]=id;modelByEntity[n]=relative;componentsByEntity[n]=JSONObject().put("Transform",true).put("MeshRenderer",true).put("Physics",true);materialsByEntity[n]=JSONObject().put("baseColor","#50BFD8").put("metallic",0.0).put("roughness",.72);select(n);saveProject();val path=File(ProjectStore.storagePath(this),"$projectId/$relative").absolutePath;glView.queueEvent{val msg=nativeImportModel(path,id);runOnUiThread{toast(msg)}}}.onFailure{toast(it.message?:"تعذر استيراد النموذج")}}}
 private fun deleteSelected(){
  val id=ids[selected]?:return
  recordUndo();nativeDeleteEntity(id);ids.remove(selected);names.remove(selected);modelByEntity.remove(selected);scriptsByEntity.remove(selected);componentsByEntity.remove(selected);materialsByEntity.remove(selected)
  selected=ids.keys.lastOrNull()?:"";if(selected.isNotEmpty())nativeSelectEntity(ids[selected]!!)
  rebuildHierarchy();refreshInspector();saveProject();glView.requestRender();toast("تم حذف العنصر")
 }
 private fun duplicateSelected(){
  val v=nativeGetTransform(ids[selected]?:return)
  val kind=when{selected.startsWith("Sphere")->1;selected.startsWith("Point Light")->2;selected.startsWith("Camera_")->3;else->0}
  recordUndo();nativeAddEntity(kind);val id=nextId++;val name=selected+"_نسخة_"+id
  names.add(name);ids[name]=id
  modelByEntity[selected]?.let{modelByEntity[name]=it};scriptsByEntity[selected]?.let{scriptsByEntity[name]=it};componentsByEntity[selected]?.let{componentsByEntity[name]=JSONObject(it.toString())};materialsByEntity[selected]?.let{materialsByEntity[name]=JSONObject(it.toString())}
  nativeSetPosition(id,v[0]+1f,v[1],v[2]);nativeSetScale(id,v[3],v[4],v[5]);nativeSetRotation(id,v[6],v[7],v[8])
  nativeSetStatic(id,v[9]>.5f);nativeSetBodyProperties(id,v[10],v[11],v[12],v[13]>.5f);nativeSetVelocity(id,v[14],v[15],v[16])
  select(name);saveProject();glView.requestRender()
 }
 private fun resetSelected(){
  val id=ids[selected]?:return;recordUndo();nativeSetPosition(id,0f,0f,0f);nativeSetRotation(id,0f,0f,0f);nativeSetScale(id,1f,1f,1f);nativeSetVelocity(id,0f,0f,0f)
  refreshInspector();saveProject();glView.requestRender()
 }
 private fun togglePlay(){
  if(!playing){
   saveProject()
   val thirdPersonRequested=scriptsByEntity.values.any{script->script.lineSequence().map{it.trim().lowercase()}.any{it=="third_person"||it=="camera.third_person"||it=="منظور الشخص الثالث"||it=="camera = third_person"}}
   val cameraTarget=if(thirdPersonRequested)ids.entries.firstOrNull{it.key.startsWith("Cube")||it.key.startsWith("Sphere")||it.key.startsWith("Model_")}?.value?:0 else 0
   nativeStartEngine();if(cameraTarget>0)nativeSetThirdPerson(cameraTarget,true)
   playing=true;playButton.text="↩ العودة إلى التطوير";status.visibility=View.GONE
   for(i in 0 until editorTop.childCount){val child=editorTop.getChildAt(i);child.visibility=if(child===playButton)View.VISIBLE else View.GONE}
   leftPanel.visibility=View.GONE;rightPanel.visibility=View.GONE;viewportHeader.visibility=View.GONE;editorFooter.visibility=View.GONE;playJoystick.visibility=View.VISIBLE;toast("بدأت التجربة — اسحب يمين الشاشة لتدوير الكاميرا")
  }else{
   nativeSetThirdPerson(0,false);nativeStopEngine();playing=false;playButton.text="▶ تجربة اللعب";status.visibility=View.VISIBLE
   for(i in 0 until editorTop.childCount)editorTop.getChildAt(i).visibility=View.VISIBLE
   leftPanel.visibility=View.VISIBLE;rightPanel.visibility=View.VISIBLE;viewportHeader.visibility=View.VISIBLE;editorFooter.visibility=View.VISIBLE;playJoystick.visibility=View.GONE;refreshInspector();saveProject();glView.requestRender();toast("عدت إلى وضع التطوير")
  }
 }
 private fun showModelSources(){
  val entries=arrayOf(
   "Poly Haven — CC0 (GLB عند توفره)" to "https://polyhaven.com/models",
   "Kenney — أصول مجانية، راجع ترخيص الحزمة" to "https://kenney.nl/assets",
   "Quaternius — أصول مجانية، راجع صفحة الترخيص" to "https://quaternius.com",
   "Sketchfab — فعّل Downloadable واختر GLB، وافحص الترخيص لكل نموذج" to "https://sketchfab.com/3d-models"
  )
  val box=LinearLayout(this).apply{orientation=1;setPadding(dp(12),dp(4),dp(12),dp(4))}
  entries.forEach{(title,url)->box.addView(action(title){startActivity(Intent(Intent.ACTION_VIEW,android.net.Uri.parse(url)))})}
  box.addView(label("ملاحظة: اختر GLB أو glTF مدعوماً، وتحقق من الترخيص والإسناد قبل الاستخدام التجاري. التنزيل يتم من موقع المصدر ثم الاستيراد من زر (استيراد نموذج).",11))
  AlertDialog.Builder(this).setTitle("مواقع النماذج ثلاثية الأبعاد").setView(box).setPositiveButton("إغلاق",null).show()
 }
 private fun label(s:String,size:Int,bold:Boolean=false)=TextView(this).apply{text=s;textSize=size.toFloat();setTextColor(if(s.contains("CORE ONLINE"))Color.rgb(105,220,170)else white);if(bold)typeface=Typeface.DEFAULT_BOLD;gravity=Gravity.CENTER_VERTICAL;layoutDirection=View.LAYOUT_DIRECTION_RTL;textDirection=View.TEXT_DIRECTION_FIRST_STRONG;setPadding(dp(6),dp(5),dp(5),dp(5))}
 private fun section(s:String)=label(s,9,true).apply{setTextColor(muted);setBackgroundColor(head)}
 private fun property(k:String,v:String)=label("$k   $v",9).apply{setPadding(dp(7),dp(8),dp(3),dp(8))}
 private fun action(s:String,fn:(View)->Unit)=label(s,9,true).apply{gravity=Gravity.CENTER;setBackgroundColor(Color.rgb(29,39,53));layoutParams=LinearLayout.LayoutParams(-2,dp(30)).apply{setMargins(dp(2),0,dp(2),0)};setOnClickListener{fn(it)}}
 private fun toast(s:String)=Toast.makeText(this,s,Toast.LENGTH_SHORT).show()
 private fun dp(n:Int)=(n*resources.displayMetrics.density).toInt()
 override fun onPause(){if(playing){nativeStopEngine();playing=false};if(projectId.isNotEmpty())saveProject();if(::glView.isInitialized)glView.onPause();super.onPause()}
 override fun onResume(){super.onResume();if(::glView.isInitialized)glView.onResume()}
 override fun onBackPressed(){saveProject();startActivity(Intent(this,ProjectHubActivity::class.java));finish()}
 private inner class JoystickView(context:android.content.Context,private val onMove:(Float,Float)->Unit):View(context){
  private val paint=Paint(Paint.ANTI_ALIAS_FLAG);private var cx=0f;private var cy=0f;private var knobX=0f;private var knobY=0f;private var active=false
  override fun onDraw(canvas:Canvas){super.onDraw(canvas);if(cx==0f){cx=width*.5f;cy=height*.5f;knobX=cx;knobY=cy};paint.color=0x553A5268;canvas.drawCircle(cx,cy,width*.43f,paint);paint.color=0xAA9BC9DA.toInt();paint.style=Paint.Style.STROKE;paint.strokeWidth=dp(2).toFloat();canvas.drawCircle(cx,cy,width*.43f,paint);paint.style=Paint.Style.FILL;paint.color=if(active)0xFF54B9D4.toInt() else 0xDDDAEAF0.toInt();canvas.drawCircle(knobX,knobY,width*.19f,paint)}
  override fun onTouchEvent(event:MotionEvent):Boolean{when(event.actionMasked){MotionEvent.ACTION_DOWN,MotionEvent.ACTION_MOVE->{active=true;val dx=event.x-cx;val dy=event.y-cy;val max=width*.28f;val len=kotlin.math.hypot(dx,dy).coerceAtMost(max);val scale=if(kotlin.math.hypot(dx,dy)>max)max/kotlin.math.hypot(dx,dy) else 1f;knobX=cx+dx*scale;knobY=cy+dy*scale;onMove((-dy/max).coerceIn(-1f,1f),(dx/max).coerceIn(-1f,1f));invalidate();return true};MotionEvent.ACTION_UP,MotionEvent.ACTION_CANCEL->{active=false;knobX=cx;knobY=cy;onMove(0f,0f);invalidate();return true}};return true}
 }
 private inner class EngineSurfaceView:GLSurfaceView(this@MainActivity){
  private var x0=0f;private var y0=0f;private var downX=0f;private var downY=0f;private var touchMoved=false;private var oldDist=0f;private var activeAxis=0
  init{setEGLContextClientVersion(3);preserveEGLContextOnPause=true
   setRenderer(object:Renderer{var last=0L;override fun onSurfaceCreated(gl:GL10?,c:EGLConfig?){nativeSurfaceCreated();last=System.nanoTime();for((n,relative) in modelByEntity.toMap()){val path=File(ProjectStore.storagePath(this@MainActivity),"$projectId/$relative").absolutePath;nativeImportModel(path,ids[n]?:continue)};for((n,m) in materialsByEntity){val id=ids[n]?:continue;val base=File(ProjectStore.storagePath(this@MainActivity),projectId);val path={key:String->m.optString(key+"Path","")};nativeSetPbrTextures(id,path("albedo"),path("normal"),path("roughness"),path("ao"));nativeSetPbrTiling(id,m.optDouble("uvTiling",1.0).toFloat())}}
    override fun onSurfaceChanged(gl:GL10?,w:Int,h:Int){nativeSurfaceChanged(w,h)}
    override fun onDrawFrame(gl:GL10?){val now=System.nanoTime();nativeRender(((now-last)/1e9).toFloat().coerceIn(0f,.1f));last=now}
   });renderMode=RENDERMODE_CONTINUOUSLY
   setOnTouchListener{_,e->when(e.actionMasked){
    MotionEvent.ACTION_DOWN->{x0=e.x;y0=e.y;downX=e.x;downY=e.y;touchMoved=false;activeAxis=if(!playing&&gizmo)nativePickGizmoAxis(e.x,e.y)else 0;true}
    MotionEvent.ACTION_POINTER_DOWN->{oldDist=dist(e);x0=midX(e);y0=midY(e);true}
    MotionEvent.ACTION_MOVE->{if(e.pointerCount>=2){touchMoved=true;val d=dist(e);if(oldDist>0&&d>0)nativeZoom(oldDist/d);nativePan(midX(e)-x0,midY(e)-y0);oldDist=d;x0=midX(e);y0=midY(e)}else{val totalX=e.x-downX;val totalY=e.y-downY;if(kotlin.math.hypot(totalX,totalY)>resources.displayMetrics.density*8)touchMoved=true;if(touchMoved){val dx=e.x-x0;val dy=e.y-y0;if(playing){nativePlayLook(dx,dy)}else{if(activeAxis!=0)nativeDragGizmoAxis(activeAxis,dx,dy)else nativeOrbit(dx,dy)};x0=e.x;y0=e.y}};requestRender();true}
    MotionEvent.ACTION_UP->{if(!touchMoved&&e.pointerCount==1){val hit=nativePickEntity(e.x,e.y);val name=ids.entries.firstOrNull{it.value==hit}?.key;if(name!=null)runOnUiThread{select(name)}};oldDist=0f;activeAxis=0;true}
    MotionEvent.ACTION_CANCEL->{oldDist=0f;activeAxis=0;true};else->true
   }}
  }
  private fun dist(e:MotionEvent):Float{if(e.pointerCount<2)return 0f;val x=e.getX(0)-e.getX(1);val y=e.getY(0)-e.getY(1);return kotlin.math.sqrt(x*x+y*y)}
  private fun midX(e:MotionEvent)=(e.getX(0)+e.getX(1))*.5f
  private fun midY(e:MotionEvent)=(e.getY(0)+e.getY(1))*.5f
 }
}