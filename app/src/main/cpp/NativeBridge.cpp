#include "EngineCore.hpp"
#include <jni.h>
namespace { aster::EngineCore engine; }
extern "C" JNIEXPORT jstring JNICALL
Java_com_aster_engine_MainActivity_nativeGetEngineStatus(JNIEnv* env, jobject) {
    return env->NewStringUTF(engine.IsRunning() ? "CORE ONLINE" : "CORE READY");
}
extern "C" JNIEXPORT void JNICALL
Java_com_aster_engine_MainActivity_nativeStartEngine(JNIEnv*, jobject) { engine.Start(); }
extern "C" JNIEXPORT void JNICALL
Java_com_aster_engine_MainActivity_nativeStopEngine(JNIEnv*, jobject) { engine.Stop(); }
