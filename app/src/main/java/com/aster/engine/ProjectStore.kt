package com.aster.engine
import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID
import java.util.zip.ZipInputStream

data class AsterProject(val id:String,val name:String,val template:String,val updatedAt:Long,val entityCount:Int)
object ProjectStore {
 private fun root(c:Context):File{
  val target=File(c.getExternalFilesDir(null)?:c.filesDir,"AsterEngine/Projects").apply{mkdirs()}
  val old=File(c.filesDir,"aster_projects")
  if(old.exists())old.listFiles()?.forEach{entry->val destination=File(target,entry.name);if(!destination.exists())entry.renameTo(destination)}
  return target
 }
 fun storagePath(c:Context)=root(c).absolutePath
 private fun folder(c:Context,id:String)=File(root(c),id).apply{mkdirs()}
 private fun file(c:Context,id:String)=File(folder(c,id),"project.json")
 fun list(c:Context):List<AsterProject> = root(c).listFiles()?.mapNotNull{d->runCatching{val j=JSONObject(File(d,"project.json").readText());AsterProject(j.optString("id",d.name),j.optString("name","Untitled"),j.optString("template","3D Starter"),j.optLong("updatedAt"),j.optJSONArray("entities")?.length()?:0)}.getOrNull()}?.sortedByDescending{it.updatedAt}?: emptyList()
 fun create(c:Context,name:String,template:String):String{val id=UUID.randomUUID().toString();val a=JSONArray();if(template!="Empty"){a.put(entity("Cube_01",0,0f,0f,0f));a.put(entity("Cube_02",0,2f,0f,0f))};val j=JSONObject().put("format","aster-project").put("version",1).put("id",id).put("name",name).put("template",template).put("createdAt",System.currentTimeMillis()).put("updatedAt",System.currentTimeMillis()).put("entities",a);file(c,id).writeText(j.toString(2));return id}
 fun entity(name:String,kind:Int,x:Float,y:Float,z:Float,sx:Float=1f,sy:Float=1f,sz:Float=1f,rx:Float=0f,ry:Float=0f,rz:Float=0f,isStatic:Boolean=false,mass:Float=1f,friction:Float=.65f,restitution:Float=.05f,useGravity:Boolean=true,vx:Float=0f,vy:Float=0f,vz:Float=0f)=JSONObject().put("name",name).put("kind",kind).put("x",x).put("y",y).put("z",z).put("sx",sx).put("sy",sy).put("sz",sz).put("rx",rx).put("ry",ry).put("rz",rz).put("isStatic",isStatic).put("mass",mass).put("friction",friction).put("restitution",restitution).put("useGravity",useGravity).put("vx",vx).put("vy",vy).put("vz",vz)
 fun read(c:Context,id:String)=JSONObject(file(c,id).readText())
 fun save(c:Context,id:String,name:String,template:String,entities:JSONArray){val f=file(c,id);val j=runCatching{JSONObject(f.readText())}.getOrElse{JSONObject()};j.put("format","aster-project").put("version",1).put("id",id).put("name",name).put("template",template).put("createdAt",j.optLong("createdAt",System.currentTimeMillis())).put("updatedAt",System.currentTimeMillis()).put("entities",entities);val tmp=File(f.parentFile,"project.json.tmp");tmp.writeText(j.toString(2));if(!tmp.renameTo(f)){f.writeText(j.toString(2));tmp.delete()}}
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
 fun rename(c:Context,id:String,name:String){val j=read(c,id).put("name",name).put("updatedAt",System.currentTimeMillis());file(c,id).writeText(j.toString(2))}
 fun delete(c:Context,id:String){folder(c,id).deleteRecursively()}
}