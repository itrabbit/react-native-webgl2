package org.itrabbit.rnwebgl2;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.util.Base64;
import android.util.Log;
import android.util.SparseArray;

import com.facebook.common.references.CloseableReference;
import com.facebook.datasource.DataSource;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.common.RotationOptions;
import com.facebook.imagepipeline.core.DefaultExecutorSupplier;
import com.facebook.imagepipeline.core.ExecutorSupplier;
import com.facebook.imagepipeline.datasource.BaseBitmapDataSubscriber;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.memory.PoolConfig;
import com.facebook.imagepipeline.memory.PoolFactory;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.views.imagehelper.ImageSource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SuppressWarnings({"WeakerAccess", "unused"})
public class RNWebGLObjectManager extends ReactContextBaseJavaModule implements RNWebGLObjectConfigLoader {

    @SuppressWarnings("WeakerAccess")
    ExecutorSupplier executorSupplier;

    private List<RNWebGLObjectConfigLoader> mLoaders = null;
    private SparseArray<RNWebGLObject> mObjects = new SparseArray<>();
    private SparseArray<RNWebGLContext> mContextMap = new SparseArray<>();

    public RNWebGLObjectManager(ReactApplicationContext reactContext) {
        super(reactContext);
        PoolFactory poolFactory = new PoolFactory(PoolConfig.newBuilder().build());
        int numCpuBoundThreads = poolFactory.getFlexByteArrayPoolMaxNumThreads();
        executorSupplier = new DefaultExecutorSupplier(numCpuBoundThreads);
    }

    @Override
    public String getName() {
        return "RNWebGLObjectManager";
    }

    public ReactApplicationContext getContext() {
        return this.getReactApplicationContext();
    }

    public RNWebGLContext getContextWithId(int ctxId) {
        return mContextMap.get(ctxId);
    }

    public void saveContext(final RNWebGLContext glContext) {
        mContextMap.put(glContext.getContextId(), glContext);
    }

    public void deleteContextWithId(final int ctxId) {
        mContextMap.delete(ctxId);
    }

    @ReactMethod
    public void destroyObjectAsync(final int objId, final Promise promise) {
        RNWebGLObject glObject = mObjects.get(objId);
        if (glObject != null) {
            mObjects.remove(objId);
            glObject.destroy();
            promise.resolve(true);
        } else {
            promise.resolve(false);
        }
    }

    @ReactMethod
    public void takeSnapshotAsync(final int ctxId, final Map<String, Object> options, final Promise promise) {
        RNWebGLContext glContext = getContextWithId(ctxId);
        if (glContext == null) {
            promise.reject("RNWEBGL_NO_CONTEXT", "ExponentGLObjectManager.takeSnapshotAsync: RNWebGLContext not found for given context id.");
        } else {
            glContext.takeSnapshot(options, getReactApplicationContext().getBaseContext(), promise);
        }
    }

    @ReactMethod
    public void createContextAsync(final Promise promise) {
        final RNWebGLContext glContext = new RNWebGLContext(this);
        glContext.initialize(null, new Runnable() {
            @Override
            public void run() {
                Bundle results = new Bundle();
                results.putInt("ctxId", glContext.getContextId());
                promise.resolve(results);
            }
        });
    }

    @ReactMethod
    public void destroyContextAsync(final int ctxId, final Promise promise) {
        RNWebGLContext glContext = getContextWithId(ctxId);
        if (glContext != null) {
            glContext.destroy();
            promise.resolve(true);
        } else {
            promise.resolve(false);
        }
    }

    @Override
    public boolean canLoadConfig(ReadableMap config) {
        return config.hasKey("image");
    }


    @Override
    public void loadWithConfig(final ReadableMap config, final RNWebGLObjectCompletionBlock callback) {
        String source;
        boolean isBase64 = false, isLocal = false, isAsset = false;
        try {
            source = config.getString("image");
        }
        catch (Exception ignoredException) {
            ReadableMap map = config.getMap("image");
            if (map.hasKey("uri")) {
                source = config.getMap("image").getString("uri");
            } else if (map.hasKey("base64")) {
                source = config.getMap("image").getString("base64");
                isBase64 = true;
            } else {
                callback.call(new Exception("Not found image source in config!"), null);
                return;
            }
        }
        // Load from base64
        if(isBase64) {
            byte[] raw = Base64.decode(source, Base64.DEFAULT);
            try {
                Bitmap bitmap = BitmapFactory.decodeByteArray(raw, 0, raw.length);
                callback.call(null, new RNWebGLTextureBitmap(config, bitmap));
            } catch (Exception e) {
                callback.call(e, null);
            }
            return;
        }
        if(source.startsWith("file://")) {
            source = this.getReactApplicationContext().getFilesDir().getAbsolutePath() + "/" + source.substring(7).trim();
            isLocal = true;
        } else if(source.startsWith("external://")) {
            File externalStorageDirectory = Environment.getExternalStorageDirectory();
            if (externalStorageDirectory != null) {
                source = externalStorageDirectory.getAbsolutePath() + "/" + source.substring(11).trim();
                isLocal = true;
            } else {
                callback.call(new Exception("Get External Storage Directory Failure"), null);
                return;
            }
        } else if(source.startsWith("asset://")) {
            source = source.substring(8).trim();
            isAsset = true;
        }
        // Load local bitmap file
        if(isLocal) {
            try {
                Bitmap bitmap = BitmapFactory.decodeFile(source);
                callback.call(null, new RNWebGLTextureBitmap(config, bitmap));
            } catch (Exception e) {
                callback.call(e, null);
            }
            return;
        }
        // Load from assets
        if(isAsset) {
            InputStream stream = null;
            //noinspection TryFinallyCanBeTryWithResources
            try {
                AssetManager assetManager = getReactApplicationContext().getAssets();
                stream = assetManager.open(source, 0);
                //noinspection ConstantConditions
                if (stream == null) {
                    callback.call(new Exception("Failed to open file"), null);
                    return;
                }
                byte[] raw = new byte[stream.available()];
                //noinspection ResultOfMethodCallIgnored
                stream.read(raw);
                Bitmap bitmap = BitmapFactory.decodeByteArray(raw, 0, raw.length);
                if(bitmap != null) {
                    callback.call(null, new RNWebGLTextureBitmap(config, bitmap));
                } else {
                    callback.call(new Exception("Failed decode bytes to bitmap"), null);
                }
                return;
            } catch (Exception e) {
                e.printStackTrace();
                callback.call(e, null);
            } finally {
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (IOException ignored) {
                    }
                }
            }
            return;
        }
        // Load from url
        ImageSource imageSource = new ImageSource(this.getReactApplicationContext(), source);
        ImageRequest imageRequest = ImageRequestBuilder
                .newBuilderWithSource(imageSource.getUri())
                .setRotationOptions(RotationOptions.disableRotation()) // FIXME is it still correct? check with diff EXIF images
                .build();

        DataSource<CloseableReference<CloseableImage>> pending =
                Fresco.getImagePipeline().fetchDecodedImage(imageRequest, null);

        pending.subscribe(new BaseBitmapDataSubscriber() {
            @Override
            protected void onNewResultImpl(@Nullable Bitmap bitmap) {
                if(bitmap != null) {
                    callback.call(null, new RNWebGLTextureBitmap(config, bitmap));
                } else {
                    callback.call(new Exception("Image Load Failure"), null);
                }
            }
            @Override
            protected void onFailureImpl(DataSource<CloseableReference<CloseableImage>> dataSource) {
                callback.call(new Exception("Image Load Failure"), null);
            }
        }, executorSupplier.forDecode());
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
