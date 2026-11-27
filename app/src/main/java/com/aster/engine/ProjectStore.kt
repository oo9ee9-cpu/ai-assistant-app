package com.aster.engine
import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID
import java.util.Locale
import java.util.zip.ZipInputStream

data class AsterProject(val id:String,val name:String,val template:String,val updatedAt:Long,val entityCount:Int)
object ProjectStore {
 private fun root(c:Context):File{
  val base=c.getExternalFilesDir(null)?:c.filesDir
  val target=File(base,"AsterEngine/Projects")
  if(!target.exists()&&!target.mkdirs())throw java.io.IOException("تعذر إنشاء مجلد المشاريع")
  if(!target.isDirectory)throw java.io.IOException("مسار المشاريع ليس مجلداً")
  val old=File(c.filesDir,"aster_projects")
  if(old.exists())old.listFiles()?.forEach{entry->
   val destination=File(target,entry.name)
   if(!destination.exists())runCatching{entry.renameTo(destination)}
  }
  return target
 }
 fun storagePath(c:Context)=root(c).absolutePath
 private fun safeId(id:String):String{
  require(id.matches(Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"))){ "معرّف المشروع غير صالح" }
  return id
 }
 private fun folder(c:Context,id:String):File{
  val dir=File(root(c),safeId(id))
  if(!dir.exists()&&!dir.mkdirs())throw java.io.IOException("تعذر إنشاء مجلد المشروع")
  if(!dir.isDirectory)throw java.io.IOException("مسار المشروع غير صالح")
  return dir
 }
 private fun file(c:Context,id:String)=File(folder(c,id),"project.json")
 private fun cleanDisplayName(raw:String):String{
  val value=raw.trim().replace(Regex("[\\u0000-\\u001F\\u007F]"),"").take(64)
  require(value.isNotBlank()){ "اسم المشروع فارغ" }
  return value
 }
 private fun writeJson(target:File,json:JSONObject){
  val temp=File(target.parentFile,"project.json.tmp")
  temp.writeText(json.toString(2),Charsets.UTF_8)
  if(!temp.renameTo(target)){
   target.writeText(json.toString(2),Charsets.UTF_8)
   temp.delete()
  }
 }
 fun list(c:Context):List<AsterProject> = root(c).listFiles()?.mapNotNull{d->runCatching{val j=JSONObject(File(d,"project.json").readText());AsterProject(j.optString("id",d.name),j.optString("name","Untitled"),j.optString("template","3D Starter"),j.optLong("updatedAt"),j.optJSONArray("entities")?.length()?:0)}.getOrNull()}?.sortedByDescending{it.updatedAt}?: emptyList()
 fun create(c:Context,name:String,template:String):String{
  val safeName=cleanDisplayName(name)
  val id=UUID.randomUUID().toString()
  val a=JSONArray()
  if(template!="Empty"){a.put(entity("Cube_01",0,0f,0f,0f));a.put(entity("Cube_02",0,2f,0f,0f))}
  val now=System.currentTimeMillis()
  val j=JSONObject().put("format","aster-project").put("version",1).put("id",id)
   .put("name",safeName).put("template",template).put("createdAt",now).put("updatedAt",now).put("entities",a)
  writeJson(file(c,id),j)
  return id
 }
 fun entity(name:String,kind:Int,x:Float,y:Float,z:Float,sx:Float=1f,sy:Float=1f,sz:Float=1f,rx:Float=0f,ry:Float=0f,rz:Float=0f,isStatic:Boolean=false,mass:Float=1f,friction:Float=.65f,restitution:Float=.05f,useGravity:Boolean=true,vx:Float=0f,vy:Float=0f,vz:Float=0f)=JSONObject().put("name",name).put("kind",kind).put("x",x).put("y",y).put("z",z).put("sx",sx).put("sy",sy).put("sz",sz).put("rx",rx).put("ry",ry).put("rz",rz).put("isStatic",isStatic).put("mass",mass).put("friction",friction).put("restitution",restitution).put("useGravity",useGravity).put("vx",vx).put("vy",vy).put("vz",vz)
 fun read(c:Context,id:String)=JSONObject(file(c,id).readText())
 fun assetFolder(c:Context,id:String)=File(folder(c,id),"Assets/Models").apply{mkdirs()}
 fun importModel(c:Context,id:String,uri:Uri):String{
  val name=uri.lastPathSegment?.substringAfterLast('/')?.substringAfterLast(':')?.ifBlank{null}?: "model.glb"
  val ext=name.substringAfterLast('.', "").lowercase(Locale.ROOT);require(ext=="glb"||ext=="gltf"){"اختر ملف glb أو gltf"}
  val target=File(assetFolder(c,id),name.replace(Regex("[^A-Za-z0-9._-]"),"_"))
  (c.contentResolver.openInputStream(uri)?:error("تعذر فتح الملف")).use{input->target.outputStream().use{input.copyTo(it)}}
  require(target.length()>0){"الملف فارغ"};return target.relativeTo(folder(c,id)).invariantSeparatorsPath
 }
 fun writeRaw(c:Context,id:String,data:JSONObject){writeJson(file(c,id),data)}

 fun save(c:Context,id:String,name:String,template:String,entities:JSONArray){
  val f=file(c,id)
  val j=runCatching{JSONObject(f.readText(Charsets.UTF_8))}.getOrElse{JSONObject()}
  j.put("format","aster-project").put("version",1).put("id",id).put("name",cleanDisplayName(name))
   .put("template",template).put("createdAt",j.optLong("createdAt",System.currentTimeMillis()))
   .put("updatedAt",System.currentTimeMillis()).put("entities",entities)
  writeJson(f,j)
 }
 fun import(c:Context,uri:Uri):String{
  val bytes=(c.contentResolver.openInputStream(uri)?:error("Cannot open selected file")).use{it.readBytes()}
  val raw=if(bytes.size>=2&&bytes[0]==0x50.toByte()&&bytes[1]==0x4B.toByte()){
   var found:String?=null
   ZipInputStream(bytes.inputStream()).use{zip->var entry=zip.nextEntry;while(entry!=null){if(!entry.isDirectory&&(entry.name=="project.json"||entry.name.endsWith("/project.json"))){found=zip.readBytes().toString(Charsets.UTF_8);break};zip.closeEntry();entry=zip.nextEntry}}
   found?:error("ZIP does not contain project.json")
  }else bytes.toString(Charsets.UTF_8)
  val j=JSONObject(raw);require(j.optString("format")=="aster-project"){"Not an Aster project file"}
  require(j.optInt("version")==1){"Unsupported project version"}
  val id=UUID.randomUUID().toString();j.put("id",id).put("updatedAt",System.currentTimeMillis())
  if(!j.has("entities"))j.put("entities",JSONArray());file(c,id).writeText(j.toString(2));return id
 }
 fun rename(c:Context,id:String,name:String){val j=read(c,id).put("name",cleanDisplayName(name)).put("updatedAt",System.currentTimeMillis());writeJson(file(c,id),j)}
 fun delete(c:Context,id:String){folder(c,id).deleteRecursively()}
}