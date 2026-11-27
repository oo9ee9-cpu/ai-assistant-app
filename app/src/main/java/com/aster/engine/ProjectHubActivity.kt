package com.aster.engine

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.*

class ProjectHubActivity:Activity(){
 private val bg=Color.rgb(7,14,25);private val panel=Color.rgb(17,29,43);private val cyan=Color.rgb(78,225,245);private val white=Color.rgb(238,246,255);private val muted=Color.rgb(145,166,190)
 private val pick=4102
 private lateinit var rootView:FrameLayout
 override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);requestedOrientation=android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;window.statusBarColor=bg;window.navigationBarColor=bg;showHub()}
 override fun onResume(){super.onResume();if(::rootView.isInitialized)showHub()}
 private fun showHub(){
  val frame=FrameLayout(this).apply{setBackgroundColor(bg)}
  frame.addView(CinematicBackdrop(this),FrameLayout.LayoutParams(-1,-1))
  val shell=LinearLayout(this).apply{orientation=1;setPadding(dp(14),dp(7),dp(14),dp(5))}
  shell.addView(header(),LinearLayout.LayoutParams(-1,dp(45)))
  val body=LinearLayout(this).apply{orientation=0}
  body.addView(leftPanel(),LinearLayout.LayoutParams(0,-1,.31f))
  body.addView(rightPanel(),LinearLayout.LayoutParams(0,-1,.69f))
  shell.addView(body,LinearLayout.LayoutParams(-1,0,1f));shell.addView(footer(),LinearLayout.LayoutParams(-1,dp(24)))
  frame.addView(shell,FrameLayout.LayoutParams(-1,-1));rootView=frame;setContentView(frame)
 }
 private fun header():View{
  val row=LinearLayout(this).apply{orientation=0;gravity=Gravity.CENTER_VERTICAL;setPadding(dp(9),0,dp(8),0);background=glass(0xCC101C2B.toInt(),dp(9))}
  row.addView(txt("✹",25,true).apply{setTextColor(cyan)},LinearLayout.LayoutParams(dp(36),-1))
  row.addView(txt("أستر // إدارة المشاريع",15,true),LinearLayout.LayoutParams(0,-1,1f))
  row.addView(badge("مساحة العمل المحلية • دون اتصال"))
  val right=LinearLayout(this).apply{orientation=1;gravity=Gravity.CENTER_VERTICAL;setPadding(dp(10),0,0,0)}
  right.addView(txt("الإصدار 1.5 • إعدادات المحرك ⚙",10,true))
  right.addView(txt("التخزين المحلي: "+storageFree(),8).apply{setTextColor(muted);gravity=Gravity.RIGHT})
  row.addView(right);return row
 }
 private fun leftPanel():View{
  val col=LinearLayout(this).apply{orientation=1;setPadding(dp(5),dp(9),dp(13),dp(4))}
  col.addView(txt("إنشاء وبناء",21,true),LinearLayout.LayoutParams(-1,dp(31)))
  col.addView(txt("أدر مشاريعك أو ابدأ عالماً ثلاثي الأبعاد",10).apply{setTextColor(muted)},LinearLayout.LayoutParams(-1,dp(28)))
  col.addView(actionCard("⬡","مشروع جديد","إنشاء عالم ثلاثي الأبعاد","قوالب • فيزياء • مشاهد",true){newProject()},LinearLayout.LayoutParams(-1,0,1f))
  col.addView(space(7));col.addView(actionCard("▱","استيراد مشروع","استيراد من مساحة الجهاز",".asterproj • .zip",false){importProject()},LinearLayout.LayoutParams(-1,0,1f))
  col.addView(space(7));col.addView(actionCard("▣","التعلّم والنماذج","استكشف المشاهد الجاهزة","ثلاثي الأبعاد • فيزياء • مدينة",false){showSamples()},LinearLayout.LayoutParams(-1,0,.7f));col.addView(space(6));col.addView(actionCard("⬇","مستودع نماذج 3D","فهرس وتنزيل انتقائي من GitHub","إنشاء مشهد لعبة جاهز",false){showRepositoryModels()},LinearLayout.LayoutParams(-1,0,.7f))
  return col
 }
 private fun rightPanel():View{
  val col=LinearLayout(this).apply{orientation=1;setPadding(dp(5),dp(9),dp(2),dp(4))}
  val projects=ProjectStore.list(this).sortedWith(compareBy<AsterProject>{when(it.name){"وثيب"->0;"مغامرة النماذج"->1;else->2}}.thenByDescending{it.updatedAt})
  val title=LinearLayout(this).apply{orientation=0;gravity=Gravity.CENTER_VERTICAL}
  val labels=LinearLayout(this).apply{orientation=1};labels.addView(txt("المشاريع الأخيرة",18,true));labels.addView(txt("${projects.size} مشروع • محفوظ محلياً",10).apply{setTextColor(muted)})
  title.addView(labels,LinearLayout.LayoutParams(0,-1,1f));title.addView(badge("المكتبة المحلية"));col.addView(title,LinearLayout.LayoutParams(-1,dp(48)))
  if(projects.isEmpty()){
   val empty=LinearLayout(this).apply{orientation=1;gravity=Gravity.CENTER;setBackground(glass(0x99203348.toInt(),dp(12)))}
   empty.addView(txt("◇",35,true).apply{setTextColor(cyan);gravity=Gravity.CENTER})
   empty.addView(txt("لا توجد مشاريع بعد — أنشئ عالمك الأول",11).apply{setTextColor(muted);gravity=Gravity.CENTER})
   col.addView(empty,LinearLayout.LayoutParams(-1,0,1f))
  }else{
   val scroll=HorizontalScrollView(this).apply{isHorizontalScrollBarEnabled=false;clipToPadding=false}
   val cards=LinearLayout(this).apply{orientation=0}
   projects.forEachIndexed{index,p->cards.addView(projectCard(p,index),LinearLayout.LayoutParams(dp(214),-1).apply{rightMargin=dp(10)})}
   scroll.addView(cards);col.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
  }
  return col
 }
 private fun projectCard(p:AsterProject,index:Int):View{
  val card=LinearLayout(this).apply{orientation=1;setPadding(dp(5),dp(5),dp(5),dp(6));background=glass(if(index==0)0xDD183447.toInt() else 0xDD142437.toInt(),dp(12));elevation=dp(5).toFloat()}
  card.addView(ProjectThumbnail(this,index),LinearLayout.LayoutParams(-1,0,.55f))
  val details=LinearLayout(this).apply{orientation=1;setPadding(dp(7),dp(5),dp(7),0)}
  details.addView(txt(p.name,13,true),LinearLayout.LayoutParams(-1,dp(24)))
  val date=if(p.updatedAt>0)SimpleDateFormat("MMM d, HH:mm",Locale.getDefault()).format(Date(p.updatedAt)) else "مؤخراً"
  details.addView(txt("آخر تعديل: $date",9).apply{setTextColor(muted)},LinearLayout.LayoutParams(-1,dp(18)))
  details.addView(txt("عرض ثلاثي الأبعاد • ${p.entityCount} عنصر",8).apply{setTextColor(muted)},LinearLayout.LayoutParams(-1,dp(17)))
  card.addView(details,LinearLayout.LayoutParams(-1,0,.34f))
  val actions=LinearLayout(this).apply{orientation=0;gravity=Gravity.CENTER_VERTICAL}
  actions.addView(button("▶  فتح المشروع",index==0){open(p.id)},LinearLayout.LayoutParams(0,dp(35),1f))
  val more=TextView(this).apply{text="•••";textSize=13f;gravity=Gravity.CENTER;setTextColor(white);background=glass(0xFF29394D.toInt(),dp(8));setOnClickListener{manage(p,this)}}
  actions.addView(more,LinearLayout.LayoutParams(dp(38),dp(35)).apply{leftMargin=dp(5)})
  card.addView(actions,LinearLayout.LayoutParams(-1,0,.23f));return card
 }
 private fun actionCard(icon:String,title:String,line1:String,line2:String,primary:Boolean,onClick:()->Unit):View{
  val box=LinearLayout(this).apply{orientation=0;gravity=Gravity.CENTER_VERTICAL;setPadding(dp(10),dp(4),dp(7),dp(4));background=glass(if(primary)0xE52A9FB7.toInt() else 0xCC182A3D.toInt(),dp(11));elevation=if(primary)dp(8).toFloat() else dp(2).toFloat();setOnClickListener{onClick()}}
  box.addView(txt(icon,29,true).apply{setTextColor(cyan);gravity=Gravity.CENTER},LinearLayout.LayoutParams(dp(52),-1))
  val text=LinearLayout(this).apply{orientation=1;gravity=Gravity.CENTER_VERTICAL}
  text.addView(txt(title,12,true));text.addView(txt(line1,9).apply{setTextColor(if(primary)white else muted)});text.addView(txt(line2,8).apply{setTextColor(if(primary)0xFFD8F8FF.toInt() else muted)})
  box.addView(text,LinearLayout.LayoutParams(0,-2,1f));return box
 }
 private fun footer():View{
  val row=LinearLayout(this).apply{orientation=0;gravity=Gravity.CENTER_VERTICAL;setPadding(dp(8),0,dp(8),0);background=glass(0xB8101C2B.toInt(),dp(6))}
  row.addView(txt("▰  المشاريع المحفوظة محلياً: "+ProjectStore.storagePath(this@ProjectHubActivity),8).apply{setTextColor(muted)},LinearLayout.LayoutParams(0,-1,1f))
  row.addView(txt("بنية المحرك: C++20 • OpenGL ES 3.0 • جاهز",8,true).apply{setTextColor(0xFF9AB9CE.toInt())});return row
 }
 private fun newProject(){
  val box=LinearLayout(this).apply{orientation=1;setPadding(dp(16),dp(8),dp(16),dp(4))}
  val name=EditText(this).apply{
   hint="اسم المشروع"
   inputType=android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
   imeOptions=android.view.inputmethod.EditorInfo.IME_ACTION_DONE
   setSingleLine(true)
   textDirection=View.TEXT_DIRECTION_FIRST_STRONG
   setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG)
   maxLines=1
  }
  val type=Spinner(this).apply{adapter=ArrayAdapter(this@ProjectHubActivity,android.R.layout.simple_spinner_dropdown_item,listOf("3D Game","Physics Sandbox","Empty Scene","Mobile Game"))}
  box.addView(name,LinearLayout.LayoutParams(-1,dp(52)))
  box.addView(txt("قالب البداية",10,true).apply{setTextColor(muted);setPadding(0,dp(10),0,0)})
  box.addView(type)
  val dialog=AlertDialog.Builder(this).setTitle("إنشاء عالم جديد").setView(box)
   .setNegativeButton("إلغاء",null).setPositiveButton("إنشاء",null).create()
  dialog.setOnShowListener{
   dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener{
    val projectName=name.text?.toString().orEmpty().trim()
    if(projectName.isBlank()){name.error="اكتب اسم المشروع";name.requestFocus();return@setOnClickListener}
    try{
     val id=ProjectStore.create(this,projectName,type.selectedItem?.toString().orEmpty())
     dialog.dismiss()
     open(id)
    }catch(error:Exception){
     Toast.makeText(this,"تعذر إنشاء المشروع: ${error.localizedMessage?: "خطأ غير معروف"}",Toast.LENGTH_LONG).show()
    }
   }
  }
  dialog.show()
 }
 private fun importProject(){startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply{addCategory(Intent.CATEGORY_OPENABLE);type="*/*"},pick)}
 @Deprecated("Use Activity Result API")
 override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?){super.onActivityResult(requestCode,resultCode,data);if(requestCode==pick&&resultCode==RESULT_OK){val uri=data?.data?:return;runCatching{ProjectStore.import(this,uri)}.onSuccess{showHub();Toast.makeText(this,"تم استيراد المشروع",Toast.LENGTH_SHORT).show()}.onFailure{Toast.makeText(this,it.message?:"فشل الاستيراد",Toast.LENGTH_LONG).show()}}}
 private fun showRepositoryModels(){Toast.makeText(this,"جارٍ جلب فهرس النماذج…",Toast.LENGTH_SHORT).show();Thread{val result=runCatching{ProjectStore.repositoryModelIndex()};runOnUiThread{val models=result.getOrElse{Toast.makeText(this,it.message?:"تعذر الاتصال",Toast.LENGTH_LONG).show();return@runOnUiThread};val labels=models.map{it.first+" • "+(it.second/1048576).coerceAtLeast(1)+" MB"}.toTypedArray();val checked=BooleanArray(models.size);AlertDialog.Builder(this).setTitle("اختر حتى 4 نماذج").setMultiChoiceItems(labels,checked){_,i,on->checked[i]=on}.setNegativeButton("إلغاء",null).setPositiveButton("تنزيل وإنشاء اللعبة"){_,_->val chosen=models.indices.filter{checked[it]}.take(4).map{models[it]};if(chosen.isEmpty())return@setPositiveButton;val id=ProjectStore.create(this,"مغامرة النماذج","Empty");Thread{val downloaded=mutableListOf<Pair<String,String>>();val done=runCatching{chosen.forEach{downloaded.add(it.first to ProjectStore.downloadRepositoryModel(this,id,it.first))};ProjectStore.createRepositoryGame(this,id,downloaded)};runOnUiThread{done.onSuccess{showHub();Toast.makeText(this,"تم إنشاء مشروع اللعبة",Toast.LENGTH_LONG).show()}.onFailure{ProjectStore.delete(this,id);Toast.makeText(this,it.message?:"فشل التنزيل",Toast.LENGTH_LONG).show()}}}.start()}.show()}}}.start()}
 private fun showSamples(){AlertDialog.Builder(this).setTitle("نماذج البداية").setItems(arrayOf("مغامرة الصحراء","مختبر الفيزياء","المدينة الذكية")){_,which->val names=listOf("مغامرة_الصحراء","مختبر_الفيزياء","المدينة_الذكية");val templates=listOf("3D Game","Physics Sandbox","City Scene");open(ProjectStore.create(this,names[which],templates[which]))}.show()}
 private fun manage(p:AsterProject,anchor:View){val menu=PopupMenu(this,anchor);listOf("فتح المشروع","إعادة تسمية","حذف").forEachIndexed{i,s->menu.menu.add(s).setOnMenuItemClickListener{when(i){0->open(p.id);1->rename(p);2->AlertDialog.Builder(this).setMessage("حذف ${p.name}?").setNegativeButton("إلغاء",null).setPositiveButton("حذف"){_,_->ProjectStore.delete(this,p.id);showHub()}.show()};true}};menu.show()}
 private fun rename(p:AsterProject){val e=EditText(this).apply{setText(p.name);setSingleLine()};AlertDialog.Builder(this).setTitle("إعادة تسمية المشروع").setView(e).setNegativeButton("إلغاء",null).setPositiveButton("حفظ"){_,_->val n=e.text.toString().trim();if(n.isNotEmpty()){ProjectStore.rename(this,p.id,n);showHub()}}.show()}
 private fun open(id:String){startActivity(Intent(this,MainActivity::class.java).putExtra("project_id",id));finish()}
 private fun badge(s:String)=TextView(this).apply{text=s;textSize=8f;typeface=Typeface.DEFAULT_BOLD;setTextColor(0xFF8BEFFF.toInt());gravity=Gravity.CENTER;setPadding(dp(8),dp(5),dp(8),dp(5));background=glass(0x553CBAD1,dp(6))}
 private fun button(s:String,primary:Boolean,action:()->Unit)=TextView(this).apply{text=s;textSize=9f;typeface=Typeface.DEFAULT_BOLD;gravity=Gravity.CENTER;setTextColor(if(primary)Color.rgb(4,35,48) else white);background=glass(if(primary)0xFF50D9EC.toInt() else 0xFF293B50.toInt(),dp(8));setOnClickListener{action()}}
 private fun txt(s:String,size:Int,bold:Boolean=false)=TextView(this).apply{text=s;textSize=size.toFloat();setTextColor(white);if(bold)typeface=Typeface.DEFAULT_BOLD;gravity=Gravity.CENTER_VERTICAL;layoutDirection=View.LAYOUT_DIRECTION_RTL;textDirection=View.TEXT_DIRECTION_FIRST_STRONG;maxLines=1;ellipsize=android.text.TextUtils.TruncateAt.END}
 private fun glass(color:Int,r:Int)=GradientDrawable().apply{shape=GradientDrawable.RECTANGLE;cornerRadius=r.toFloat();setColor(color);setStroke(dp(1),0x554FCDE8)}
 private fun space(h:Int)=View(this).apply{layoutParams=LinearLayout.LayoutParams(1,dp(h))}
 private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
 private fun storageFree():String{val mb=filesDir.freeSpace/1024/1024;return "%.1f GB FREE".format(mb/1024f)}
 private class CinematicBackdrop(c:android.content.Context):View(c){
  private val p=Paint(3);private val rnd=java.util.Random(731);private val particles=List(45){floatArrayOf(rnd.nextFloat(),rnd.nextFloat(),.4f+rnd.nextFloat()*1.8f)}
  private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
  override fun onDraw(canvas:Canvas){super.onDraw(canvas);val w=width.toFloat();val h=height.toFloat()
   p.shader=LinearGradient(0f,0f,w,h,intArrayOf(Color.rgb(7,17,30),Color.rgb(10,36,52),Color.rgb(7,15,27)),null,Shader.TileMode.CLAMP);canvas.drawRect(0f,0f,w,h,p);p.shader=null
   p.shader=RadialGradient(w*.72f,h*.35f,w*.55f,intArrayOf(0x4435DDF5,0x0035DDF5),null,Shader.TileMode.CLAMP);canvas.drawRect(0f,0f,w,h,p);p.shader=null
   val horizon=h*.57f;p.color=0x5538B8D5;p.strokeWidth=1f
   for(i in 0..28){val t=i/28f;val y=horizon+(h-horizon)*t*t;canvas.drawLine(0f,y,w,y,p)}
   for(i in -20..20){val x=w*.5f+i*w*.026f;canvas.drawLine(w*.5f,horizon,x,h,p)}
   p.style=Paint.Style.STROKE;p.strokeWidth=1.2f;p.color=0x7741D4E8
   val path=Path();for(i in 0..120){val x=i*w/120f;val y=horizon+cos(i*.12f)*h*.025f+sin(i*.045f)*h*.025f;if(i==0)path.moveTo(x,y)else path.lineTo(x,y)};canvas.drawPath(path,p);p.style=Paint.Style.FILL
   particles.forEachIndexed{i,a->val x=a[0]*w;val y=a[1]*h;val radius=a[2]*dp(1).toFloat();p.color=0x553DF2FF;p.setShadowLayer(dp(7).toFloat(),0f,0f,0xFF20DFFF.toInt());canvas.drawCircle(x,y,radius,p);p.clearShadowLayer();if(i%9==0){p.color=0x4448E8F8;canvas.drawLine(x-dp(3),y,x+dp(3),y,p);canvas.drawLine(x,y-dp(3),x,y+dp(3),p)}}
  }
 }
 private class ProjectThumbnail(c:android.content.Context,val variant:Int):View(c){
  private val p=Paint(3);private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
  override fun onDraw(canvas:Canvas){super.onDraw(canvas);val w=width.toFloat();val h=height.toFloat()
   p.shader=LinearGradient(0f,0f,w,h,if(variant%3==0)intArrayOf(0xFF544334.toInt(),0xFF172B3B.toInt())else if(variant%3==1)intArrayOf(0xFF18384B.toInt(),0xFF101D30.toInt())else intArrayOf(0xFF19223D.toInt(),0xFF30204A.toInt()),null,Shader.TileMode.CLAMP);canvas.drawRect(0f,0f,w,h,p);p.shader=null
   val cx=w*.5f;val cy=h*.62f;p.color=0x6657D7E9;p.strokeWidth=1f
   for(i in -6..6)canvas.drawLine(cx+i*w*.065f,cy-h*.38f,cx+i*w*.13f,h,p)
   for(i in 0..7){val yy=cy+i*h*.045f;canvas.drawLine(0f,yy,w,yy,p)}
   when(variant%3){0->drawDesert(canvas,cx,cy);1->drawPhysics(canvas,cx,cy);else->drawCity(canvas,cx,cy)}
   p.color=0xCC0A1725.toInt();canvas.drawRect(0f,h-dp(16),w,h,p);p.color=0xFFB5EFFF.toInt();p.textSize=dp(8).toFloat();canvas.drawText(if(variant%3==0) "3D GAME" else if(variant%3==1) "PHYSICS" else "3D SCENE",dp(7).toFloat(),h-dp(5).toFloat(),p)
  }
  private fun drawDesert(c:Canvas,x:Float,y:Float){val path=Path().apply{moveTo(x*.18f,y);lineTo(x*.52f,y-height*.43f);lineTo(x*1.78f,y);lineTo(x,y+height*.22f);close()};p.color=0xFFB98759.toInt();c.drawPath(path,p);for(i in 0..4){val bx=x*.55f+i*dp(14);val by=y-height*.10f+sin(i.toDouble()).toFloat()*dp(7);p.color=0xFFD7A16E.toInt();c.drawRoundRect(bx,by,bx+dp(14),by+dp(11),dp(2).toFloat(),dp(2).toFloat(),p);p.color=0xFF956344.toInt();c.drawRect(bx+dp(3),by-dp(6),bx+dp(11),by,p)}}
  private fun drawPhysics(c:Canvas,x:Float,y:Float){p.color=0xFF55CBE8.toInt();p.style=Paint.Style.STROKE;p.strokeWidth=dp(2).toFloat();for(i in 0..3){val xx=x*.48f+i*dp(30);c.drawCircle(xx,y-height*.15f,dp(8+i%2*4).toFloat(),p)};p.style=Paint.Style.FILL;p.color=0xFF8AD8E8.toInt();c.drawRect(x*.65f,y-height*.3f,x*.72f,y-height*.12f,p);c.drawRect(x*1.2f,y-height*.26f,x*1.29f,y-height*.08f,p)}
  private fun drawCity(c:Canvas,x:Float,y:Float){for(i in 0..5){val bx=dp(8)+i*width/7f;val bh=dp(24+(i*17)%45).toFloat();p.color=if(i%2==0)0xFF3E6F98.toInt()else 0xFF624A91.toInt();c.drawRect(bx,y-bh,bx+dp(17),y,p);p.color=0xFF55E5F4.toInt();for(k in 0..2)c.drawRect(bx+dp(4+k*4),y-bh+dp(6),bx+dp(6+k*4),y-bh+dp(11),p)}}
 }
}