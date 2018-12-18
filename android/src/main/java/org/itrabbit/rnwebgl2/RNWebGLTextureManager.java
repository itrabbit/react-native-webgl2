package org.itrabbit.rnwebgl2;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;

public class RNWebGLTextureManager extends ReactContextBaseJavaModule {
    @Override
    public String getName() {
        return "RNWebGLTextureManager";
    }

    public RNWebGLTextureManager(ReactApplicationContext reactContext) {
        super(reactContext);
    }

    @ReactMethod
    public void create(final ReadableMap config, final Promise promise) {
        this.getReactApplicationContext()
                .getNativeModule(RNWebGLObjectManager.class)
                .loadWithConfigAndWaitAttached(config, new RNWebGLObjectCompletionBlock() {
            public void call(Exception e, RNWebGLObject obj) {
                if (e != null) {
                    promise.reject(e);
                } else if(obj instanceof RNWebGLTexture) {
                    RNWebGLTexture texture = (RNWebGLTexture)obj;
                    WritableMap response = Arguments.createMap();
                    response.putInt("objId", texture.objId);
                    response.putInt("width", texture.width);
                    response.putInt("height", texture.height);
                    android.util.Log.i("RNWebGL", obj.objId+" of size "+texture.width+"x"+texture.height);
                    promise.resolve(response);
                } else {
                    WritableMap response = Arguments.createMap();
                    response.putInt("objId", obj.objId);
                    promise.resolve(response);
                }
            }
        });
    }

    @ReactMethod
    public void destroy(final int objId) {
        this.getReactApplicationContext().getNativeModule(RNWebGLObjectManager.class).unloadWithObjId(objId);
    }
}
