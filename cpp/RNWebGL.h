#ifndef __RNWebGL_H__
#define __RNWebGL_H__


#ifdef __ANDROID__
#include <GLES3/gl3.h>
#include <jni.h>
#endif
#ifdef __APPLE__
#include <OpenGLES/ES3/gl.h>
#endif

#ifdef __cplusplus
#include <functional>
#endif

#include <JavaScriptCore/JSBase.h>


// NOTE: The symbols exposed by this header are named with a `UEX` prefix rather than an `EX`
//       prefix so that they are unaffected by the automated iOS versioning script when
//       referenced in versioned Objective-C code. The WebGL C/C++ library is not versioned
//       and there is only one copy of its code in the binary form of the Expo application.


#ifdef __cplusplus
extern "C" {
#endif
    
    // Identifies an WebGL context. No WebGL context has the id 0, so that can be
    // used as a 'null' value.
    typedef unsigned int RNWebGLContextId;
    
    // Identifies an WebGL object. WebGL objects represent virtual mappings to underlying OpenGL objects.
    // No WebGL object has the id 0, so that can be used as a 'null' value.
    typedef unsigned int RNWebGLObjectId;
    
    typedef RNWebGLObjectId RNWebGLTextureId;
    
    // [JS thread] Create an WebGL context and return its id number. Saves the
    // JavaScript interface object (has a WebGLRenderingContext-style API) at
    // `global.__WebGLContexts[id]` in JavaScript.
    RNWebGLContextId RNWebGLContextCreate(JSGlobalContextRef jsCtx);

    // [Any thread] Check whether we should redraw the surface
    bool RNWebGLContextNeedsRedraw(RNWebGLContextId ctxId);
    
    // [GL thread] Tell cpp that we finished drawing to the surface
    void RNWebGLContextDrawEnded(RNWebGLContextId ctxId);
    
    // [Any thread] Release the resources for an WebGL context. The same id is never
    // reused.
    void RNWebGLContextDestroy(RNWebGLContextId ctxId);
    
    // [GL thread] Perform one frame's worth of queued up GL work
    void RNWebGLContextFlush(RNWebGLContextId ctxId);
    
    // [GL thread] Set the default framebuffer (used when binding 0). Allows using
    // platform-specific extensions on the default framebuffer, such as MSAA.
    void RNWebGLContextSetDefaultFramebuffer(RNWebGLContextId ctxId, GLint framebuffer);
    
    // [Any thread] Create an WebGL object. Initially maps to the OpenGL object zero.
    RNWebGLObjectId RNWebGLContextCreateObject(RNWebGLContextId ctxId);
    
    // [GL thread] Destroy an WebGL object.
    void RNWebGLContextDestroyObject(RNWebGLContextId ctxId, RNWebGLObjectId objId);
    
    // [GL thread] Set the underlying OpenGL object an WebGL object maps to.
    void RNWebGLContextMapObject(RNWebGLContextId ctxId, RNWebGLObjectId objId, GLuint glObj);
    
    // [GL thread] Get the underlying OpenGL object an WebGL object maps to.
    GLuint RNWebGLContextGetObject(RNWebGLContextId ctxId, RNWebGLObjectId objId);

#ifdef __ANDROID__
    // Initializes the JVM, used to flush
    void InitJVM(JNIEnv *env);
    // Request Flush for Android
    void requestFlush(RNWebGLContextId id);
#endif

#ifdef __cplusplus
}
#endif


#endif
