package org.itrabbit.rnwebgl2;

import com.facebook.soloader.SoLoader;

// Java bindings for RNWebGL.h interface
@SuppressWarnings("JniMissingFunction")
public class RNWebGL {
    static {
        SoLoader.loadLibrary("rnwebgl2");
        RNWebGL.RNWebGLInit();
    }

    public static native void RNWebGLInit();

    public static native void RNWebGLContextFlush(int ctxId);

    public static native void RNWebGLContextDestroy(int ctxId);

    public static native void RNWebGLContextDrawEnded(int ctxId);

    public static native int RNWebGLContextCreate(long jsCtxPtr);

    public static native int RNWebGLContextCreateObject(int ctxId);

    public static native boolean RNWebGLContextNeedsRedraw(int ctxId);

    public static native int RNWebGLContextGetObject(int ctxId, int objId);

    public static native void RNWebGLContextDestroyObject(int ctxId, int objId);

    public static native void RNWebGLContextMapObject(int ctxId, int objId, int glObj);

    public static void requestFlush(int ctxId){
        RNWebGLView.requestFlush(ctxId);
    }
}