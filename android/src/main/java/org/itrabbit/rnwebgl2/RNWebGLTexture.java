package org.itrabbit.rnwebgl2;

import com.facebook.react.bridge.ReadableMap;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.itrabbit.rnwebgl2.RNWebGL.*;

@SuppressWarnings("WeakerAccess")
public class RNWebGLTexture extends RNWebGLObject {

    public final int width;
    public final int height;

    private AtomicBoolean attached = new AtomicBoolean(false);
    private ArrayList<Runnable> mAttachEventQueue = new ArrayList<>();


    public RNWebGLTexture(ReadableMap config, int width, int height) {
        super(config.getInt("ctxId"));
        this.width = width;
        this.height = height;
    }

    public synchronized void attachTexture (int texture) {
        RNWebGLContextMapObject(ctxId, objId, texture);
        attached.set(true);
        if (!mAttachEventQueue.isEmpty()) {
            for (Runnable r : new ArrayList<>(mAttachEventQueue)) {
                r.run();
            }
            mAttachEventQueue.clear();
        }
    }

    @SuppressWarnings("UnusedReturnValue")
    public synchronized boolean listenAttached (Runnable r) {
        if(attached.get()){
            r.run();
            return true;
        } else {
            return mAttachEventQueue.add(r);
        }
    }

    public void runOnGLThread (Runnable runnable) {
        if(mGlView != null) {
            mGlView.runOnGLThread(ctxId, runnable);
        }
    }
}
