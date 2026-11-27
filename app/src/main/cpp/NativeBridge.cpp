#include "aster/EngineRuntime.hpp"
#include "aster/OpenGLES3Renderer.hpp"
#include <jni.h>
#include <memory>\n#include <algorithm>
namespace{aster::Engine engine;std::unique_ptr<aster::OpenGLES3Renderer>renderer;}
extern "C" JNIEXPORT void JNICALL Java_com_aster_engine_MainActivity_nativeResetScene(JNIEnv*,jobject){engine.GetScene().Clear();engine.Select(0);}
extern "C" JNIEXPORT void JNICALL Java_com_aster_engine_MainActivity_nativeStartEngine(JNIEnv*,jobject){engine.Start();}
extern "C" JNIEXPORT void JNICALL Java_com_aster_engine_MainActivity_nativeStopEngine(JNIEnv*,jobject){engine.Stop();}
extern "C" JNIEXPORT void JNICALL Java_com_aster_engine_MainActivity_nativeAddEntity(JNIEnv*,jobject,jint kind){auto k=static_cast<aster::EntityKind>(std::clamp(kind,0,3));auto n=engine.GetScene().Entities().size();auto id=engine.GetScene().Add(k,{static_cast<float>(n%4)*1.5f-1.5f,0,static_cast<float>(n/4)*-1.5f});engine.Select(id);}
extern "C" JNIEXPORT void JNICALL Java_com_aster_engine_MainActivity_nativeSelectEntity(JNIEnv*,jobject,jint id){engine.Select(static_cast<aster::EntityId>(id));}
extern "C" JNIEXPORT void JNICALL Java_com_aster_engine_MainActivity_nativeOrbit(JNIEnv*,jobject,jfloat x,jfloat y){engine.Orbit(x,y);}
extern "C" JNIEXPORT void JNICALL Java_com_aster_engine_MainActivity_nativePan(JNIEnv*,jobject,jfloat x,jfloat y){engine.Pan(x,y);}
extern "C" JNIEXPORT void JNICALL Java_com_aster_engine_MainActivity_nativeZoom(JNIEnv*,jobject,jfloat f){engine.Zoom(f);}
extern "C" JNIEXPORT jint JNICALL Java_com_aster_engine_MainActivity_nativePickGizmoAxis(JNIEnv*,jobject,jfloat x,jfloat y){return renderer?renderer->PickGizmoAxis(x,y):0;}
extern "C" JNIEXPORT void JNICALL Java_com_aster_engine_MainActivity_nativeDragGizmoAxis(JNIEnv*,jobject,jint axis,jfloat dx,jfloat dy){if(renderer)renderer->DragGizmoAxis(axis,dx,dy);}
extern "C" JNIEXPORT void JNICALL Java_com_aster_engine_MainActivity_nativeSetPosition(JNIEnv*,jobject,jint id,jfloat x,jfloat y,jfloat z){engine.SetPosition(id,{x,y,z});}
extern "C" JNIEXPORT void JNICALL Java_com_aster_engine_MainActivity_nativeSetScale(JNIEnv*,jobject,jint id,jfloat x,jfloat y,jfloat z){engine.SetScale(id,{x,y,z});}
extern "C" JNIEXPORT jfloatArray JNICALL Java_com_aster_engine_MainActivity_nativeGetTransform(JNIEnv*e,jobject,jint id){auto*t=engine.GetScene().Find(id);jfloat v[6]={0,0,0,1,1,1};if(t){v[0]=t->transform.position.x;v[1]=t->transform.position.y;v[2]=t->transform.position.z;v[3]=t->transform.scale.x;v[4]=t->transform.scale.y;v[5]=t->transform.scale.z;}auto a=e->NewFloatArray(6);e->SetFloatArrayRegion(a,0,6,v);return a;}
extern "C" JNIEXPORT jstring JNICALL Java_com_aster_engine_MainActivity_nativeGetEngineStatus(JNIEnv*e,jobject){return e->NewStringUTF(engine.IsRunning()?"CORE ONLINE":"CORE PAUSED");}
extern "C" JNIEXPORT void JNICALL Java_com_aster_engine_MainActivity_nativeSurfaceCreated(JNIEnv*,jobject){renderer=std::make_unique<aster::OpenGLES3Renderer>(&engine.GetScene(),&engine);renderer->Initialize();}
extern "C" JNIEXPORT void JNICALL Java_com_aster_engine_MainActivity_nativeSurfaceChanged(JNIEnv*,jobject,jint w,jint h){if(renderer)renderer->Resize(w,h);}
extern "C" JNIEXPORT void JNICALL Java_com_aster_engine_MainActivity_nativeRender(JNIEnv*,jobject,jfloat dt){engine.Tick(dt);if(renderer)renderer->RenderFrame();}
extern "C" JNIEXPORT void JNICALL Java_com_aster_engine_MainActivity_nativeSurfaceDestroyed(JNIEnv*,jobject){if(renderer){renderer->Shutdown();renderer.reset();}}
