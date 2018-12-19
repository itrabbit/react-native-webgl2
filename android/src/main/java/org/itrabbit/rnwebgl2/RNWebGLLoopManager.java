package org.itrabbit.rnwebgl2;

import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactMethod;


public class RNWebGLLoopManager extends ReactContextBaseJavaModule {
    @Override
    public String getName() {
        return "RNWebGLLoopManager";
    }

    public RNWebGLLoopManager(ReactApplicationContext reactContext) {
        super(reactContext);
    }

    @ReactMethod
    public void endFrame(final int ctxId) {
        RNWebGLView.endFrame(ctxId);
    }

}