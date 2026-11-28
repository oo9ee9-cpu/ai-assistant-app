#include "aster/EngineRuntime.hpp"
#include "aster/OpenGLES3Renderer.hpp"
#include <jni.h>
#include <memory>
#include <algorithm>
#include <string>
namespace{aster::Engine engine;std::unique_ptr<aster::OpenGLES3Renderer>renderer;std::string utf(JNIEnv*e,jstring s){if(!s)return {};const char*p=e->GetStringUTFChars(s,nullptr);std::string r=p?p:"";if(p)e->ReleaseStringUTFChars(s,p);return r;}}
extern "C" JNIEXPORT jstring JNICALL Java_com_aster_engine_MainActivity_nativeSetPbrTextures(JNIEnv*e,jobject,jint id,jstring a,jstring n,jstring r,jstring ao){if(!renderer)return e->NewStringUTF("المحرّك الرسومي غير جاهز");std::string error;if(!renderer->SetMaterialTextures((aster::EntityId)id,utf(e,a),utf(e,n),utf(e,r),utf(e,ao),error))return e->NewStringUTF(error.c_str());return e->NewStringUTF("تم تطبيق خرائط PBR");}
extern "C" JNIEXPORT void JNICALL Java_com_aster_engine_MainActivity_nativeSetPbrTiling(JNIEnv*,jobject,jint id,jfloat v){if(renderer)renderer->SetMaterialTiling((aster::EntityId)id,v);}
extern "C" JNIEXPORT void JNICALL Java_com_aster_engine_MainActivity_nativeResetScene(JNIEnv*,jobject){engine.GetScene().Clear();engine.Select(0);}
extern "C" JNIEXPORT void JNICALL Java_com_aster_engine_MainActivity_nativeStartEngine(JNIEnv*,jobject){engine.Start();}
extern "C" JNIEXPORT void JNICALL Java_com_aster_engine_MainActivity_nativeSetThirdPerson(JNIEnv*,jobject,jint id,jboolean enabled){engine.SetThirdPerson(static_cast<aster::EntityId>(id),enabled==JNI_TRUE);}
extern "C" JNIEXPORT void JNICALL Java_com_aster_engine_MainActivity_nativeStopEngine(JNIEnv*,jobject){engine.Stop();}
extern "C" JNIEXPORT void JNICALL Java_com_aster_engine_MainActivity_nativePlayMove(JNIEnv*,jobject,jfloat f,jfloat s,jfloat dt){engine.PlayMove(f,s,dt);}
extern "C" JNIEXPORT void JNICALL Java_com_aster_engine_MainActivity_nativePlayLook(JNIEnv*,jobject,jfloat x,jfloat y){engine.PlayLook(x,y);}
extern "C" JNIEXPORT void JNICALL Java_com_aster_engine_MainActivity_nativeAddEntity(JNIEnv*,jobject,jint k){auto n=engine.GetScene().Entities().size();engine.Select(engine.GetScene().Add(static_cast<aster::EntityKind>(std::clamp(k,0,3)),{float(n%4)*1.5f-1.5f,0,float(n/4)*-1.5f}));}
extern "C" JNIEXPORT void JNICALL Java_com_aster_engine_MainActivity_nativeSelectEntity(JNIEnv*,jobject,jint id){engine.Select(id);}
extern "C" JNIEXPORT void JNICALL Java_com_aster_engine_MainActivity_nativeDeleteEntity(JNIEnv*,jobject,jint id){engine.GetScene().Remove(static_cast<aster::EntityId>(id));if(engine.Selected()==static_cast<aster::EntityId>(id))engine.Select(0);}
extern "C" JNIEXPORT void JNICALL Java_com_aster_engine_MainActivity_nativeOrbit(JNIEnv*,jobject,jfloat x,jfloat y){engine.Orbit(x,y);}
extern "C" JNIEXPORT void JNICALL Java_com_aster_engine_MainActivity_nativePan(JNIEnv*,jobject,jfloat x,jfloat y){engine.Pan(x,y);}
extern "C" JNIEXPORT void JNICALL Java_com_aster_engine_MainActivity_nativeZoom(JNIEnv*,jobject,jfloat f){engine.Zoom(f);}
extern "C" JNIEXPORT jint JNICALL Java_com_aster_engine_MainActivity_nativePickGizmoAxis(JNIEnv*,jobject,jfloat x,jfloat y){return renderer?renderer->PickGizmoAxis(x,y):0;}
extern "C" JNIEXPORT jint JNICALL Java_com_aster_engine_MainActivity_nativePickEntity(JNIEnv*,jobject,jfloat x,jfloat y){return renderer?renderer->PickEntity(x,y):0;}
extern "C" JNIEXPORT void JNICALL Java_com_aster_engine_MainActivity_nativeDragGizmoAxis(JNIEnv*,jobject,jint a,jfloat x,jfloat y){if(renderer)renderer->DragGizmoAxis(a,x,y);}
extern "C" JNIEXPORT void JNICALL Java_com_aster_engine_MainActivity_nativeSetPosition(JNIEnv*,jobject,jint id,jfloat x,jfloat y,jfloat z){engine.SetPosition(id,{x,y,z});}
extern "C" JNIEXPORT void JNICALL Java_com_aster_engine_MainActivity_nativeSetScale(JNIEnv*,jobject,jint id,jfloat x,jfloat y,jfloat z){engine.SetScale(id,{x,y,z});}
extern "C" JNIEXPORT void JNICALL Java_com_aster_engine_MainActivity_nativeSetRotation(JNIEnv*,jobject,jint id,jfloat x,jfloat y,jfloat z){if(auto*t=engine.GetScene().Find(id))t->transform.rotation={x,y,z};}
extern "C" JNIEXPORT void JNICALL Java_com_aster_engine_MainActivity_nativeSetStatic(JNIEnv*,jobject,jint id,jboolean v){engine.SetStatic(id,v==JNI_TRUE);}
extern "C" JNIEXPORT void JNICALL Java_com_aster_engine_MainActivity_nativeSetBodyProperties(JNIEnv*,jobject,jint id,jfloat m,jfloat f,jfloat r,jboolean g){if(auto*t=engine.GetScene().Find(id)){t->body.mass=std::max(.001f,m);t->body.friction=std::clamp(f,0.f,1.f);t->body.restitution=std::clamp(r,0.f,1.f);t->body.useGravity=g==JNI_TRUE;}}
extern "C" JNIEXPORT void JNICALL Java_com_aster_engine_MainActivity_nativeSetVelocity(JNIEnv*,jobject,jint id,jfloat x,jfloat y,jfloat z){engine.SetVelocity(id,{x,y,z});}
extern "C" JNIEXPORT jfloatArray JNICALL Java_com_aster_engine_MainActivity_nativeGetTransform(JNIEnv*e,jobject,jint id){float v[17]={0,0,0,1,1,1,0,0,0,0,1,.65f,.05f,1,0,0,0};if(auto*t=engine.GetScene().Find(id)){v[0]=t->transform.position.x;v[1]=t->transform.position.y;v[2]=t->transform.position.z;v[3]=t->transform.scale.x;v[4]=t->transform.scale.y;v[5]=t->transform.scale.z;v[6]=t->transform.rotation.x;v[7]=t->transform.rotation.y;v[8]=t->transform.rotation.z;v[9]=t->body.isStatic;v[10]=t->body.mass;v[11]=t->body.friction;v[12]=t->body.restitution;v[13]=t->body.useGravity;v[14]=t->body.velocity.x;v[15]=t->body.velocity.y;v[16]=t->body.velocity.z;}auto a=e->NewFloatArray(17);e->SetFloatArrayRegion(a,0,17,v);return a;}
extern "C" JNIEXPORT jstring JNICALL Java_com_aster_engine_MainActivity_nativeImportModel(JNIEnv*e,jobject,jstring path,jint entityId){if(!renderer)return e->NewStringUTF("المحرّك الرسومي غير جاهز");std::string error;if(!renderer->ImportModel(utf(e,path),static_cast<aster::EntityId>(entityId),error))return e->NewStringUTF(error.c_str());return e->NewStringUTF("تم استيراد النموذج وعرضه");}
extern "C" JNIEXPORT jstring JNICALL Java_com_aster_engine_MainActivity_nativeGetEngineStatus(JNIEnv*e,jobject){return e->NewStringUTF(engine.IsRunning()?"المحرّك يعمل":"المحرّك متوقف");}
extern "C" JNIEXPORT void JNICALL Java_com_aster_engine_MainActivity_nativeSurfaceCreated(JNIEnv*,jobject){renderer=std::make_unique<aster::OpenGLES3Renderer>(&engine.GetScene(),&engine);renderer->Initialize();}
extern "C" JNIEXPORT void JNICALL Java_com_aster_engine_MainActivity_nativeSurfaceChanged(JNIEnv*,jobject,jint w,jint h){if(renderer)renderer->Resize(w,h);}
extern "C" JNIEXPORT void JNICALL Java_com_aster_engine_MainActivity_nativeRender(JNIEnv*,jobject,jfloat d){engine.Tick(d);if(renderer)renderer->RenderFrame();}
extern "C" JNIEXPORT void JNICALL Java_com_aster_engine_MainActivity_nativeSurfaceDestroyed(JNIEnv*,jobject){if(renderer){renderer->Shutdown();renderer.reset();}}
