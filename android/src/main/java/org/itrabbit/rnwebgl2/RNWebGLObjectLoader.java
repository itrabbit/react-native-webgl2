package org.itrabbit.rnwebgl2;

import android.util.Log;
import android.util.SparseArray;

import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReadableMap;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"WeakerAccess", "unused"})
public class RNWebGLObjectLoader extends ReactContextBaseJavaModule {
    private List<RNWebGLObjectConfigLoader> mLoaders = null;
    private SparseArray<RNWebGLObject> mObjects = new SparseArray<>();

    public RNWebGLObjectLoader(ReactApplicationContext reactContext) {
        super(reactContext);
    }

    @Override
    public String getName() {
        return "RNWebGLTextureLoader";
    }

    public RNWebGLObjectConfigLoader objectLoaderForConfig (final ReadableMap config) {
        if (mLoaders == null) {
            mLoaders = new ArrayList<>();
            for (NativeModule module: this.getReactApplicationContext().getCatalystInstance().getNativeModules()) {
                if (module instanceof RNWebGLObjectConfigLoader) {
                    mLoaders.add((RNWebGLObjectConfigLoader) module);
                }
            }
        }
        for (RNWebGLObjectConfigLoader loader : mLoaders) {
            if (loader.canLoadConfig(config)) {
                return loader;
            }
        }
        return null;
    }

    public void loadWithConfig (final ReadableMap config, final RNWebGLObjectCompletionBlock callback) {
        RNWebGLObjectConfigLoader loader = this.objectLoaderForConfig(config);
        if (loader == null) {
            Log.e("RNWebGL", "No suitable RNWebGLObjectLoader found for " + config);
            callback.call(new ObjectLoaderNotFoundException(), null);
        }
        else {
            final RNWebGLObjectLoader self = this;
            loader.loadWithConfig(config, new RNWebGLObjectCompletionBlock() {
                public void call (Exception e, RNWebGLObject obj) {
                    if (obj != null) {
                        self.mObjects.put(obj.objId, obj);
                    }
                    callback.call(e, obj);
                }
            });
        }
    }

    public void loadWithConfigAndWaitAttached (final ReadableMap config, final RNWebGLObjectCompletionBlock callback) {
        loadWithConfig(config, new RNWebGLObjectCompletionBlock() {
            @Override
            public void call(final Exception e, final RNWebGLObject obj) {
                if (obj != null) {
                    if (obj instanceof RNWebGLTexture) {
                        ((RNWebGLTexture)obj).listenAttached(new Runnable() {
                            public void run() {
                                callback.call(e, obj);
                            }
                        });
                    }
                }
            }
        });
    }

    public void unloadWithObjId (int objId) {
        RNWebGLObject obj = mObjects.get(objId);
        if (obj != null) {
            mObjects.remove(objId);
            obj.destroy();
        }
    }

    public void unloadWithCtxId (int ctxId) {
        SparseArray<RNWebGLObject> remaining = new SparseArray<>();
        for(int i = 0; i < mObjects.size(); i++) {
            int objId = mObjects.keyAt(i);
            RNWebGLObject obj = mObjects.get(objId);
            if (obj.ctxId == ctxId) {
                obj.destroy();
            }
            else {
                remaining.put(objId, obj);
            }
        }
        mObjects = remaining;
    }
}
