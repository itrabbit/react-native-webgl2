#include <stdint.h>
#include <jni.h>
#include <JavaScriptCore/JSContextRef.h>
#include "RNWebGL.h"

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT void JNICALL
Java_org_itrabbit_rnwebgl2_RNWebGL_RNWebGLInit
(JNIEnv *env, jclass clazz) {
  InitJVM(env);
}

JNIEXPORT jint JNICALL
Java_org_itrabbit_rnwebgl2_RNWebGL_RNWebGLContextCreate
(JNIEnv *env, jclass clazz, jlong jsCtxPtr) {
  JSGlobalContextRef jsCtx = (JSGlobalContextRef) (intptr_t) jsCtxPtr;
  if (jsCtx) {
    return RNWebGLContextCreate(jsCtx);
  }
  return 0;
}

JNIEXPORT void JNICALL
Java_org_itrabbit_rnwebgl2_RNWebGL_RNWebGLContextDestroy
(JNIEnv *env, jclass clazz, jint ctxId) {
  RNWebGLContextDestroy(ctxId);
}

JNIEXPORT void JNICALL
Java_org_itrabbit_rnwebgl2_RNWebGL_RNWebGLContextFlush
(JNIEnv *env, jclass clazz, jint ctxId) {
  RNWebGLContextFlush(ctxId);
}

JNIEXPORT jint JNICALL
Java_org_itrabbit_rnwebgl2_RNWebGL_RNWebGLContextCreateObject
(JNIEnv *env, jclass clazz, jint ctxId) {
  return RNWebGLContextCreateObject(ctxId);
}

JNIEXPORT void JNICALL
Java_org_itrabbit_rnwebgl2_RNWebGL_RNWebGLContextDestroyObject
(JNIEnv *env, jclass clazz, jint ctxId, jint objId) {
  RNWebGLContextDestroyObject(ctxId, objId);
}

JNIEXPORT void JNICALL
Java_org_itrabbit_rnwebgl2_RNWebGL_RNWebGLContextMapObject
(JNIEnv *env, jclass clazz, jint ctxId, jint objId, jint glObj) {
  RNWebGLContextMapObject(ctxId, objId, glObj);
}

JNIEXPORT jint JNICALL
Java_org_itrabbit_rnwebgl2_RNWebGL_RNWebGLContextGetObject
(JNIEnv *env, jclass clazz, jint ctxId, jint objId) {
  return RNWebGLContextGetObject(ctxId, objId);
}
  
JNIEXPORT bool JNICALL
Java_org_itrabbit_rnwebgl2_RNWebGL_RNWebGLContextNeedsRedraw
(JNIEnv *env, jclass clazz, jint ctxId) {
  return RNWebGLContextNeedsRedraw(ctxId);
}

JNIEXPORT void JNICALL
Java_org_itrabbit_rnwebgl2_RNWebGL_RNWebGLContextDrawEnded
(JNIEnv *env, jclass clazz, jint ctxId) {
  RNWebGLContextDrawEnded(ctxId);
}

#ifdef __cplusplus
}
#endif
