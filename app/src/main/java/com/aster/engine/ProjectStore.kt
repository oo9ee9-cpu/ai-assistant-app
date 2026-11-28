package com.aster.engine
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipException
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID
import java.util.Locale
import java.util.zip.ZipInputStream
import java.net.HttpURLConnection
import java.net.URL

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
 fun repositoryModelIndex():List<Pair<String,Long>>{val con=URL("https://api.github.com/repos/oo9ee9-cpu/uploads/git/trees/main?recursive=1").openConnection() as HttpURLConnection;con.connectTimeout=15000;con.readTimeout=20000;try{require(con.responseCode in 200..299){"تعذر جلب فهرس النماذج"};val j=JSONObject(con.inputStream.bufferedReader().use{it.readText()});val a=j.optJSONArray("tree")?:JSONArray();return (0 until a.length()).mapNotNull{a.optJSONObject(it)}.filter{it.optString("type")=="blob"&&it.optString("path").endsWith(".glb",true)}.map{it.optString("path") to it.optLong("size")}.sortedBy{it.first}}finally{con.disconnect()}}
 fun downloadRepositoryModel(c:Context,id:String,name:String):String{require(name.matches(Regex("[A-Za-z0-9._-]+\\.glb",RegexOption.IGNORE_CASE)));val con=URL("https://raw.githubusercontent.com/oo9ee9-cpu/uploads/main/"+name).openConnection() as HttpURLConnection;con.connectTimeout=15000;con.readTimeout=60000;try{require(con.responseCode in 200..299){"تعذر تنزيل النموذج"};val target=File(assetFolder(c,id),name);require(!target.exists());con.inputStream.use{input->target.outputStream().use{output->val b=ByteArray(65536);var total=0L;while(true){val n=input.read(b);if(n<0)break;total+=n;require(total<40L*1024*1024);output.write(b,0,n)}}};require(target.length()>0);return target.relativeTo(folder(c,id)).invariantSeparatorsPath}catch(e:Exception){File(assetFolder(c,id),name).delete();throw e}finally{con.disconnect()}}
 fun createRepositoryGame(c:Context,id:String,models:List<Pair<String,String>>):String{require(models.isNotEmpty());val j=read(c,id);val a=JSONArray();val floor=entity("أرضية العالم",0,0f,-.35f,0f,14f,.35f,14f,isStatic=true,mass=1000f,useGravity=false).put("components",JSONObject().put("Transform",true).put("MeshRenderer",true).put("Physics",true)).put("material",JSONObject().put("baseColor","#657B83").put("metallic",0.05).put("roughness",0.9));a.put(floor);val points=listOf(floatArrayOf(-3f,0f,-2f),floatArrayOf(3f,0f,-2f),floatArrayOf(0f,0f,4f),floatArrayOf(-5f,0f,3f));models.take(4).forEachIndexed{i,v->a.put(entity("نموذج_"+(i+1),0,points[i][0],0f,points[i][2],isStatic=true,useGravity=false).put("assetPath",v.second).put("repoSource","https://github.com/oo9ee9-cpu/uploads/blob/main/"+v.first).put("components",JSONObject().put("Transform",true).put("MeshRenderer",true).put("Physics",true)).put("material",JSONObject().put("baseColor","#9AB8C2").put("metallic",0.0).put("roughness",.78)))};a.put(entity("شخصية اللاعب",1,0f,1.2f,0f,.55f,.9f,.55f,mass=1f,friction=.8f,restitution=0f,useGravity=true).put("components",JSONObject().put("Transform",true).put("MeshRenderer",true).put("Physics",true).put("PlayerController",true)).put("script","// تحكم اللاعب عبر السحب يسار الشاشة\\n// توجيه الكاميرا عبر السحب يمين الشاشة").put("material",JSONObject().put("baseColor","#35C7E8").put("metallic",0.0).put("roughness",.65)));j.put("entities",a).put("assets",JSONArray().apply{models.forEach{put(it.second)}}).put("description","مشهد تجريبي من مستودع النماذج مع أرضية وشخصية قابلة للتحكم");writeRaw(c,id,j);return id}


 fun importPbrTexture(c:Context,id:String,uri:Uri):JSONObject{
  val dir=File(folder(c,id),"Assets/Textures").apply{if(!exists()&&!mkdirs())throw java.io.IOException("تعذر إنشاء مجلد الخامات")}
  val displayName=runCatching{
   c.contentResolver.query(uri,arrayOf(OpenableColumns.DISPLAY_NAME),null,null,null)?.use{cursor->
    if(cursor.moveToFirst())cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME)) else null
   }
  }.getOrNull()?.takeIf{it.isNotBlank()} ?: "texture"
  val mime=c.contentResolver.getType(uri)?.lowercase(Locale.ROOT).orEmpty()
  fun magic(file:File):String{
   val h=ByteArray(8);val count=FileInputStream(file).use{it.read(h)}
   if(count>=4&&h[0]==0x50.toByte()&&h[1]==0x4b.toByte()&&((h[2]==0x03.toByte()&&h[3]==0x04.toByte())||(h[2]==0x05.toByte()&&h[3]==0x06.toByte())||(h[2]==0x07.toByte()&&h[3]==0x08.toByte())))return "zip"
   if(count>=8&&h.sliceArray(0..7).contentEquals(byteArrayOf(0x89.toByte(),0x50,0x4e,0x47,0x0d,0x0a,0x1a,0x0a)))return "png"
   if(count>=3&&h[0]==0xff.toByte()&&h[1]==0xd8.toByte()&&h[2]==0xff.toByte())return "jpg"
   return ""
  }
  fun safeName(raw:String,extension:String):String{
   val leaf=raw.replace('\\','/').substringAfterLast('/').replace(Regex("[^A-Za-z0-9._-]"),"_")
   val stem=leaf.substringBeforeLast('.',leaf).ifBlank{"texture"}.take(90)
   return "${stem}.${extension}"
  }
  fun uniqueFile(raw:String,extension:String):File{
   val base=safeName(raw,extension);var target=File(dir,base);var i=1
   while(target.exists()){target=File(dir,"${base.substringBeforeLast('.')}_${i}.${extension}");i++}
   return target
  }
  fun copyBounded(input:java.io.InputStream,target:File,limit:Long){
   input.use{src->FileOutputStream(target).use{dst->
    val buffer=ByteArray(64*1024);var total=0L
    while(true){val n=src.read(buffer);if(n<0)break;total+=n;require(total<=limit){"حجم الملف يتجاوز الحد المسموح"};dst.write(buffer,0,n)}
   }}
   require(target.length()>0){"الملف فارغ"}
  }
  fun copyZipEntry(zip:ZipInputStream,target:File,limit:Long):Long{
   var total=0L
   FileOutputStream(target).use{dst->
    val buffer=ByteArray(64*1024)
    while(true){val n=zip.read(buffer);if(n<0)break;total+=n;require(total<=limit){"حجم صورة ZIP يتجاوز الحد المسموح"};dst.write(buffer,0,n)}
   }
   require(target.length()>0){"صورة فارغة داخل ZIP"}
   return total
  }
  fun role(raw:String):String?{
   val n=raw.lowercase(Locale.ROOT)
   return when{
    n.contains("normalgl")||n.contains("normal_dx")||n.contains("normal")||n.contains("_nor")->"normal"
    n.contains("roughness")||n.contains("_rough")->"roughness"
    n.contains("ambientocclusion")||n.contains("_ao")||n.contains("occlusion")->"ao"
    n.contains("color")||n.contains("albedo")||n.contains("diffuse")||n.contains("_col")->"albedo"
    else->null
   }
  }
  val result=JSONObject();val staged=uniqueFile(displayName,"bin")
  try{
   copyBounded(c.contentResolver.openInputStream(uri)?:error("تعذر فتح الملف من مستعرض Android"),staged,512L*1024*1024)
   val kind=magic(staged);val ext=displayName.substringAfterLast('.',"").lowercase(Locale.ROOT)
   val declaredZip=mime=="application/zip"||mime=="application/x-zip-compressed"||ext=="zip"
   val declaredImage=mime.startsWith("image/")||ext in setOf("png","jpg","jpeg")
   require(kind.isNotEmpty()){"نوع الملف غير معروف أو تالف (MIME: "+(mime.ifBlank{"غير محدد"})+")"}
   require((declaredZip&&kind=="zip")||(!declaredZip&&kind in setOf("png","jpg"))||(!declaredImage&&kind in setOf("zip","png","jpg"))){"امتداد أو MIME لا يطابق محتوى الملف"}
   if(kind=="png"||kind=="jpg"){
    val extension=if(kind=="jpg")"jpg" else "png";val target=uniqueFile(displayName,extension)
    if(!staged.renameTo(target)){FileInputStream(staged).use{copyBounded(it,target,512L*1024*1024)};staged.delete()}
    result.put(role(displayName)?: "albedo",target.absolutePath)
   }else{
    var total=0L;var images=0
    try{
     ZipInputStream(FileInputStream(staged)).use{zip->
      var entry=zip.nextEntry
      while(entry!=null){
       if(!entry.isDirectory){
        val normalized=entry.name.replace('\\','/')
        require(!normalized.startsWith("/")&&!normalized.split('/').any{it==".."}){"مسار غير آمن داخل ZIP"}
        val entryExt=normalized.substringAfterLast('.',"").lowercase(Locale.ROOT)
        if(entryExt in setOf("png","jpg","jpeg")){
         val temp=uniqueFile(normalized,entryExt);copyZipEntry(zip,temp,128L*1024*1024)
         total+=temp.length();require(total<=512L*1024*1024){"مجموع صور ZIP أكبر من الحد المسموح"}
         val imageKind=magic(temp)
         if(imageKind=="png"||imageKind=="jpg"){
          val actualExt=if(imageKind=="jpg")"jpg" else "png"
          val target=if(temp.extension==actualExt)temp else uniqueFile(normalized,actualExt).also{if(!temp.renameTo(it))throw java.io.IOException("تعذر حفظ صورة من ZIP")}
          val key=role(normalized);if(key!=null&&!result.has(key))result.put(key,target.absolutePath)
          images++
         }else temp.delete()
        }
       }
       zip.closeEntry();entry=zip.nextEntry
      }
     }
    }catch(e:ZipException){throw IllegalArgumentException("أرشيف ZIP تالف أو غير مكتمل",e)}
    require(images>0){"الأرشيف لا يحتوي صور PNG أو JPG سليمة"}
    if(!result.has("albedo")){
     val first=dir.listFiles()?.firstOrNull{it.isFile&&it.extension in setOf("png","jpg")}
     if(first!=null)result.put("albedo",first.absolutePath)
    }
   }
   require(result.length()>0){"لم يتم العثور على خرائط خامة صالحة"}
   return result
  }catch(e:Exception){throw IllegalArgumentException(e.message?.takeIf{it.isNotBlank()}?:"تعذر قراءة ملف الخامة",e)}
  finally{staged.delete()}
 }
 fun assetFiles(c:Context,id:String):List<File> = assetFolder(c,id).listFiles()?.filter{it.isFile && it.extension.lowercase(Locale.ROOT) in setOf("glb","gltf")}?.sortedBy{it.name.lowercase(Locale.ROOT)} ?: emptyList()
 fun deleteAsset(c:Context,id:String,relativePath:String){
  val base=assetFolder(c,id).canonicalFile
  val target=File(folder(c,id),relativePath).canonicalFile
  require(target.parentFile==base){"مسار الأصل خارج مجلد النماذج"}
  require(target.isFile && target.extension.lowercase(Locale.ROOT) in setOf("glb","gltf")){"ملف الأصل غير صالح"}
  if(!target.delete())throw java.io.IOException("تعذر حذف ملف الأصل")
 }
 fun importModel(c:Context,id:String,uri:Uri):String{
  val name=uri.lastPathSegment?.substringAfterLast('/')?.substringAfterLast(':')?.ifBlank{null}?: "model.glb"
  val ext=name.substringAfterLast('.', "").lowercase(Locale.ROOT);require(ext=="glb"||ext=="gltf"){"اختر ملف glb أو gltf"}
  val safeName=name.replace(Regex("[^A-Za-z0-9._-]"),"_").ifBlank{"model.$ext"}
  val dir=assetFolder(c,id);var target=File(dir,safeName);var suffix=1
  while(target.exists()){val stem=safeName.substringBeforeLast('.',safeName);target=File(dir,"${stem}_${suffix++}.$ext")}
  try{(c.contentResolver.openInputStream(uri)?:error("تعذر فتح الملف")).use{input->target.outputStream().use{output->input.copyTo(output)}};require(target.length()>0){"الملف فارغ"}}
  catch(error:Exception){target.delete();throw error}
  return target.relativeTo(folder(c,id)).invariantSeparatorsPath
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