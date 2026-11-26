package com.aster.engine
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.*
class ProjectHubActivity:Activity(){
 private val bg=Color.rgb(10,15,23);private val panel=Color.rgb(20,29,42);private val accent=Color.rgb(74,183,210);private val white=Color.rgb(235,242,250);private val muted=Color.rgb(143,160,181);private val pick=4102
 override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);window.statusBarColor=bg;window.navigationBarColor=bg;showHub()}
 private fun showHub(){
  val root=LinearLayout(this).apply{orientation=1;setBackgroundColor(bg);setPadding(dp(18),dp(12),dp(18),dp(12))}
  val head=LinearLayout(this).apply{gravity=Gravity.CENTER_VERTICAL}
  head.addView(txt("✺ ASTER ENGINE",22,true),LinearLayout.LayoutParams(0,dp(48),1f));head.addView(txt("PROJECT HUB • LOCAL",10).apply{setTextColor(accent)});root.addView(head)
  root.addView(txt("Your creative workspace",19,true).apply{setPadding(0,dp(15),0,dp(3))})
  root.addView(txt("Create a world or continue building.",12).apply{setTextColor(muted);setPadding(0,0,0,dp(14))})
  val actions=LinearLayout(this);actions.addView(btn("+ NEW PROJECT",true){newProject()},LinearLayout.LayoutParams(0,dp(46),1f));actions.addView(btn("IMPORT PROJECT",false){importProject()},LinearLayout.LayoutParams(0,dp(46),1f).apply{leftMargin=dp(8)});root.addView(actions)
  root.addView(txt("RECENT PROJECTS",11,true).apply{setTextColor(accent);setPadding(0,dp(22),0,dp(9))})
  val scroll=ScrollView(this);val list=LinearLayout(this).apply{orientation=1};val projects=ProjectStore.list(this)
  if(projects.isEmpty())list.addView(txt("No projects yet. Create your first 3D project.",14).apply{gravity=Gravity.CENTER;setTextColor(muted);setPadding(dp(20),dp(38),dp(20),dp(38));setBackgroundColor(panel)})
  projects.forEach{p->val card=LinearLayout(this).apply{orientation=1;setPadding(dp(14),dp(10),dp(14),dp(10));setBackgroundColor(panel)}
   val row=LinearLayout(this).apply{gravity=Gravity.CENTER_VERTICAL};row.addView(txt("⬡",25).apply{setTextColor(accent)},LinearLayout.LayoutParams(dp(42),dp(48)))
   val info=LinearLayout(this).apply{orientation=1};info.addView(txt(p.name,15,true));info.addView(txt(p.template+" • "+p.entityCount+" objects",10).apply{setTextColor(muted)})
   row.addView(info,LinearLayout.LayoutParams(0,-2,1f));row.addView(txt("⋮",22).apply{gravity=Gravity.CENTER;setOnClickListener{manage(p)}},LinearLayout.LayoutParams(dp(34),dp(45)));card.addView(row)
   card.addView(btn("OPEN PROJECT →",true){open(p.id)});card.setOnLongClickListener{manage(p);true};list.addView(card,LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(9)})
  }
  scroll.addView(list);root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f));root.addView(txt("Projects are stored privately on this device.",10).apply{setTextColor(muted);gravity=Gravity.CENTER;setPadding(0,dp(8),0,0)});setContentView(root)
 }
 private fun newProject(){val box=LinearLayout(this).apply{orientation=1;setPadding(dp(16),0,dp(16),0)};val name=EditText(this).apply{hint="Project name";setSingleLine()};val type=Spinner(this).apply{adapter=ArrayAdapter(this@ProjectHubActivity,android.R.layout.simple_spinner_dropdown_item,listOf("3D Starter","Empty","Mobile Game"))};box.addView(name);box.addView(txt("STARTER TEMPLATE",10,true).apply{setTextColor(muted);setPadding(0,dp(10),0,0)});box.addView(type);AlertDialog.Builder(this).setTitle("Create project").setView(box).setNegativeButton("Cancel",null).setPositiveButton("CREATE"){_,_->val n=name.text.toString().trim().ifBlank{"My First Project"};open(ProjectStore.create(this,n,type.selectedItem.toString()))}.show()}
 private fun importProject(){startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply{addCategory(Intent.CATEGORY_OPENABLE);type="*/*"},pick)}
 @Deprecated("Use Activity Result API")
 override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?){super.onActivityResult(requestCode,resultCode,data);if(requestCode==pick&&resultCode==RESULT_OK){val uri=data?.data?:return;runCatching{ProjectStore.import(this,uri)}.onSuccess{open(it)}.onFailure{Toast.makeText(this,it.message?:"Import failed",Toast.LENGTH_LONG).show()}}}
 private fun manage(p:AsterProject){AlertDialog.Builder(this).setTitle(p.name).setItems(arrayOf("Open","Rename","Delete")){_,i->when(i){0->open(p.id);1->rename(p);2->AlertDialog.Builder(this).setMessage("Delete "+p.name+"?").setNegativeButton("Cancel",null).setPositiveButton("Delete"){_,_->ProjectStore.delete(this,p.id);showHub()}.show()}}.show()}
 private fun rename(p:AsterProject){val e=EditText(this).apply{setText(p.name);setSingleLine()};AlertDialog.Builder(this).setTitle("Rename project").setView(e).setNegativeButton("Cancel",null).setPositiveButton("Save"){_,_->val n=e.text.toString().trim();if(n.isNotEmpty()){ProjectStore.rename(this,p.id,n);showHub()}}.show()}
 private fun open(id:String){startActivity(Intent(this,MainActivity::class.java).putExtra("project_id",id));finish()}
 private fun txt(s:String,size:Int,bold:Boolean=false)=TextView(this).apply{text=s;textSize=size.toFloat();setTextColor(white);if(bold)typeface=Typeface.DEFAULT_BOLD;gravity=Gravity.CENTER_VERTICAL}
 private fun btn(s:String,primary:Boolean,action:()->Unit)=TextView(this).apply{text=s;textSize=11f;typeface=Typeface.DEFAULT_BOLD;gravity=Gravity.CENTER;setTextColor(white);setBackgroundColor(if(primary)Color.rgb(30,111,137)else Color.rgb(34,46,63));setPadding(dp(9),0,dp(9),0);setOnClickListener{action()}}
 private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
}