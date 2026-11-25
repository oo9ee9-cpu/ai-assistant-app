#include "aster/EngineRuntime.hpp"
#include "aster/OpenGLES3Renderer.hpp"
#include <jni.h>
#include <memory>
namespace { aster::Engine engine; std::unique_ptr<aster::OpenGLES3Renderer> renderer; }
extern "C" JNIEXPORT void JNICALL Java_com_aster_engine_MainActivity_nativeStartEngine(JNIEnv*,jobject){engine.Start();}
extern "C" JNIEXPORT void JNICALL Java_com_aster_engine_MainActivity_nativeStopEngine(JNIEnv*,jobject){engine.Stop();}
extern "C" JNIEXPORT void JNICALL Java_com_aster_engine_MainActivity_nativeAddCube(JNIEnv*,jobject){auto n=engine.GetScene().Entities().size();engine.GetScene().AddCube({static_cast<float>(n%4)*1.4f-1.4f,0,static_cast<float>(n/4)*-1.4f});}
extern "C" JNIEXPORT jstring JNICALL Java_com_aster_engine_MainActivity_nativeGetEngineStatus(JNIEnv*e,jobject){return e->NewStringUTF(engine.IsRunning()?"CORE ONLINE":"CORE READY");}
extern "C" JNIEXPORT void JNICALL Java_com_aster_engine_MainActivity_nativeSurfaceCreated(JNIEnv*,jobject){renderer=std::make_unique<aster::OpenGLES3Renderer>(&engine.GetScene());renderer->Initialize();}
extern "C" JNIEXPORT void JNICALL Java_com_aster_engine_MainActivity_nativeSurfaceChanged(JNIEnv*,jobject,jint w,jint h){if(renderer)renderer->Resize(w,h);}
extern "C" JNIEXPORT void JNICALL Java_com_aster_engine_MainActivity_nativeRender(JNIEnv*,jobject,jfloat dt){engine.Tick(dt);if(renderer)renderer->RenderFrame();}
extern "C" JNIEXPORT void JNICALL Java_com_aster_engine_MainActivity_nativeSurfaceDestroyed(JNIEnv*,jobject){if(renderer){renderer->Shutdown();renderer.reset();}}
