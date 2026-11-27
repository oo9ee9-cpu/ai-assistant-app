#include "aster/EngineRuntime.hpp"
#include "aster/OpenGLES3Renderer.hpp"
#include <jni.h>
#include <algorithm>
#include <memory>
namespace { aster::Engine engine; std::unique_ptr<aster::OpenGLES3Renderer> renderer; }
extern "C" JNIEXPORT void JNICALL Java_com_aster_engine_MainActivity_nativeResetScene(JNIEnv*,jobject){engine.Stop();engine.GetScene().Clear();engine.Select(0);}
extern "C" JNIEXPORT void JNICALL Java_com_aster_engine_MainActivity_nativeStartEngine(JNIEnv*,jobject){engine.Start();}
extern "C" JNIEXPORT void JNICALL Java_com_aster_engine_MainActivity_nativeStopEngine(JNIEnv*,jobject){engine.Stop();}
extern "C" JNIEXPORT void JNICALL Java_com_aster_engine_MainActivity_nativeAddEntity(JNIEnv*,jobject,jint kind){auto k=static_cast<aster::EntityKind>(std::clamp(kind,0,3));auto n=engine.GetScene().Entities().size();auto id=engine.GetScene().Add(k,{float(n%4)*1.5f-1.5f,0,float(n/4)*-1.5f});engine.Select(id);}
extern "C" JNIEXPORT void JNICALL Java_com_aster_engine_MainActivity_nativeSelectEntity(JNIEnv*,jobject,jint id){engine.Select(static_cast<aster::EntityId>(id));}
extern "C" JNIEXPORT void JNICALL Java_com_aster_engine_MainActivity_nativeOrbit(JNIEnv*,jobject,jfloat x,jfloat y){engine.Orbit(x,y);}
extern "C" JNIEXPORT void JNICALL Java_com_aster_engine_MainActivity_nativePan(JNIEnv*,jobject,jfloat x,jfloat y){engine.Pan(x,y);}
extern "C" JNIEXPORT void JNICALL Java_com_aster_engine_MainActivity_nativeZoom(JNIEnv*,jobject,jfloat f){engine.Zoom(f);}
extern "C" JNIEXPORT jint JNICALL Java_com_aster_engine_MainActivity_nativePickGizmoAxis(JNIEnv*,jobject,jfloat x,jfloat y){return renderer?renderer->PickGizmoAxis(x,y):0;}
extern "C" JNIEXPORT void JNICALL Java_com_aster_engine_MainActivity_nativeDragGizmoAxis(JNIEnv*,jobject,jint a,jfloat x,jfloat y){if(renderer)renderer->DragGizmoAxis(a,x,y);}
extern "C" JNIEXPORT void JNICALL Java_com_aster_engine_MainActivity_nativeSetPosition(JNIEnv*,jobject,jint id,jfloat x,jfloat y,jfloat z){engine.SetPosition(id,{x,y,z});}
extern "C" JNIEXPORT void JNICALL Java_com_aster_engine_MainActivity_nativeSetScale(JNIEnv*,jobject,jint id,jfloat x,jfloat y,jfloat z){engine.SetScale(id,{x,y,z});}
extern "C" JNIEXPORT void JNICALL Java_com_aster_engine_MainActivity_nativeSetRotation(JNIEnv*,jobject,jint id,jfloat x,jfloat y,jfloat z){engine.SetRotation(id,{x,y,z});}
extern "C" JNIEXPORT void JNICALL Java_com_aster_engine_MainActivity_nativeSetStatic(JNIEnv*,jobject,jint id,jboolean v){engine.SetStatic(id,v==JNI_TRUE);}
extern "C" JNIEXPORT void JNICALL Java_com_aster_engine_MainActivity_nativeSetBodyProperties(JNIEnv*,jobject,jint id,jfloat m,jfloat f,jfloat r,jboolean g){engine.SetBodyProperties(id,m,f,r,g==JNI_TRUE);}
extern "C" JNIEXPORT void JNICALL Java_com_aster_engine_MainActivity_nativeSetVelocity(JNIEnv*,jobject,jint id,jfloat x,jfloat y,jfloat z){engine.SetVelocity(id,{x,y,z});}
extern "C" JNIEXPORT jfloatArray JNICALL Java_com_aster_engine_MainActivity_nativeGetTransform(JNIEnv*env,jobject,jint id){
 const auto*e=engine.GetScene().Find(id);jfloat v[17]={0,0,0,1,1,1,0,0,0,0,1,.65f,.05f,1,0,0,0};
 if(e){v[0]=e->transform.position.x;v[1]=e->transform.position.y;v[2]=e->transform.position.z;v[3]=e->transform.scale.x;v[4]=e->transform.scale.y;v[5]=e->transform.scale.z;v[6]=e->transform.rotation.x;v[7]=e->transform.rotation.y;v[8]=e->transform.rotation.z;v[9]=e->body.isStatic?1:0;v[10]=e->body.mass;v[11]=e->body.friction;v[12]=e->body.restitution;v[13]=e->body.useGravity?1:0;v[14]=e->body.velocity.x;v[15]=e->body.velocity.y;v[16]=e->body.velocity.z;}
 jfloatArray a=env->NewFloatArray(17);if(a)env->SetFloatArrayRegion(a,0,17,v);return a;
}
extern "C" JNIEXPORT jstring JNICALL Java_com_aster_engine_MainActivity_nativeGetEngineStatus(JNIEnv*e,jobject){return e->NewStringUTF(engine.IsRunning()?"CORE ONLINE":"CORE PAUSED");}
extern "C" JNIEXPORT void JNICALL Java_com_aster_engine_MainActivity_nativeSurfaceCreated(JNIEnv*,jobject){renderer=std::make_unique<aster::OpenGLES3Renderer>(&engine.GetScene(),&engine);renderer->Initialize();}
extern "C" JNIEXPORT void JNICALL Java_com_aster_engine_MainActivity_nativeSurfaceChanged(JNIEnv*,jobject,jint w,jint h){if(renderer)renderer->Resize(w,h);}
extern "C" JNIEXPORT void JNICALL Java_com_aster_engine_MainActivity_nativeRender(JNIEnv*,jobject,jfloat dt){engine.Tick(dt);if(renderer)renderer->RenderFrame();}
extern "C" JNIEXPORT void JNICALL Java_com_aster_engine_MainActivity_nativeSurfaceDestroyed(JNIEnv*,jobject){if(renderer){renderer->Shutdown();renderer.reset();}}
