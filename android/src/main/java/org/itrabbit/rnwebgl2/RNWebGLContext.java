package org.itrabbit.rnwebgl2;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactContext;

import org.itrabbit.rnwebgl2.utils.FileSystemUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.ref.WeakReference;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;

import static android.opengl.GLES30.*;
import static org.itrabbit.rnwebgl2.RNWebGL.*;


public class RNWebGLContext {
    private int mCtxId = -1;

    private final RNWebGLObjectManager mManager;

    public RNWebGLContext(RNWebGLObjectManager manager) {
        super();
        mManager = manager;
    }

    public int getContextId() {
        return mCtxId;
    }

    public void initialize(final Runnable completionCallback) {
        ReactContext reactContext = mManager.getContext();
        long jsContextRef = reactContext.getJavaScriptContextHolder().get();
        mCtxId = RNWebGLContextCreate(jsContextRef);
        mManager.saveContext(this);
        completionCallback.run();
    }

    public void flush() {
        if (mCtxId > 0) {
            RNWebGLContextFlush(mCtxId);
            if (RNWebGLContextNeedsRedraw(mCtxId)) {
                RNWebGLContextDrawEnded(mCtxId);
            }
        }
    }


    public void destroy() {
        mManager.deleteContextWithId(mCtxId);
        mManager.unloadWithCtxId(mCtxId);
        RNWebGLContextDestroy(mCtxId);
    }

    // must be called in GL thread
    public Map<String, Object> getViewportRect() {
        int[] viewport = new int[4];
        glGetIntegerv(GL_VIEWPORT, viewport, 0);

        Map<String, Object> results = new HashMap<>();
        results.put("x", viewport[0]);
        results.put("y", viewport[1]);
        results.put("width", viewport[2]);
        results.put("height", viewport[3]);

        return results;
    }

    public void takeSnapshot(final Map<String, Object> options, final Context context, final Promise promise) {
        flush();
        RNWebGLView.runOnGLThread(mCtxId, new Runnable() {
            @SuppressWarnings({"unchecked", "ConstantConditions"})
            @Override
            public void run() {
                Map<String, Object> rect = options.containsKey("rect") ? (Map<String, Object>) options.get("rect") : getViewportRect();
                Boolean flip = options.containsKey("flip") && (Boolean) options.get("flip");
                String format = options.containsKey("format") ? (String) options.get("format") : null;
                int compressionQuality = options.containsKey("compress") ? (int) (100.0 * (Double) options.get("compress")) : 100;

                int x = castNumberToInt(rect.get("x"));
                int y = castNumberToInt(rect.get("y"));
                int width = castNumberToInt(rect.get("width"));
                int height = castNumberToInt(rect.get("height"));

                // Save surrounding framebuffer
                int[] prevFramebuffer = new int[1];
                glGetIntegerv(GL_FRAMEBUFFER_BINDING, prevFramebuffer, 0);

                // Set source framebuffer that we take snapshot from
                int sourceFramebuffer = prevFramebuffer[0];
                Map<String, Object> framebufferMap = options.containsKey("framebuffer") ? (Map<String, Object>) options.get("framebuffer") : null;

                if (framebufferMap != null && framebufferMap.containsKey("id")) {
                    Integer framebufferId = castNumberToInt(framebufferMap.get("id"));
                    sourceFramebuffer = RNWebGLContextGetObject(mCtxId, framebufferId);
                }

                // Bind source framebuffer
                glBindFramebuffer(GL_FRAMEBUFFER, sourceFramebuffer);

                // Allocate pixel buffer and read pixels
                final int[] dataArray = new int[width * height];
                final IntBuffer dataBuffer = IntBuffer.wrap(dataArray);
                dataBuffer.position(0);
                glReadPixels(x, y, width, height, GL_RGBA, GL_UNSIGNED_BYTE, dataBuffer);

                // Restore surrounding framebuffer
                glBindFramebuffer(GL_FRAMEBUFFER, prevFramebuffer[0]);

                new TakeSnapshot(context, width, height, flip, format, compressionQuality, dataArray, promise)
                        .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
        });
    }

    private static class TakeSnapshot extends AsyncTask<Void, Void, Void> {
        private final WeakReference<Context> mContext;
        private final int mWidth;
        private final int mHeight;
        private final boolean mFlip;
        private final String mFormat;
        private final int mCompress;
        private final int[] mDataArray;
        private final Promise mPromise;

        TakeSnapshot(Context context, int width, int height, boolean flip, String format, int compress, int[] dataArray, Promise promise) {
            mContext = new WeakReference<>(context);
            mWidth = width;
            mHeight = height;
            mFlip = flip;
            mFormat = format;
            mCompress = compress;
            mDataArray = dataArray;
            mPromise = promise;
        }

        @Override
        protected Void doInBackground(Void... params) {
            // Convert RGBA data format to bitmap's ARGB
            for (int i = 0; i < mHeight; i++) {
                for (int j = 0; j < mWidth; j++) {
                    int offset = i * mWidth + j;
                    int pixel = mDataArray[offset];
                    int blue = (pixel >> 16) & 0xff;
                    int red = (pixel << 16) & 0x00ff0000;
                    mDataArray[offset] = (pixel & 0xff00ff00) | red | blue;
                }
            }

            // Create Bitmap and flip
            Bitmap bitmap = Bitmap.createBitmap(mDataArray, mWidth, mHeight, Bitmap.Config.ARGB_8888);

            if (!mFlip) {
                // the bitmap is automatically flipped on Android, however we may want to unflip it
                // in case we take a snapshot from framebuffer that is already flipped
                Matrix flipMatrix = new Matrix();
                flipMatrix.postScale(1, -1, mWidth / 2, mHeight / 2);
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, mWidth, mHeight, flipMatrix, true);
            }

            // Write bitmap to file
            String path = null;
            String extension = ".jpeg";
            FileOutputStream output = null;
            Bitmap.CompressFormat compressFormat = Bitmap.CompressFormat.JPEG;

            if (mFormat != null && mFormat.equals("png")) {
                compressFormat = Bitmap.CompressFormat.PNG;
                extension = ".png";
            }

            Context context = mContext.get();

            if (context == null) {
                mPromise.reject("E_GL_CONTEXT_DESTROYED", "Context has been garbage collected.");
                return null;
            }

            try {
                path = FileSystemUtils.generateOutputPath(context.getCacheDir(), "GLView", extension);
                output = new FileOutputStream(path);
                bitmap.compress(compressFormat, mCompress, output);
                output.flush();
                output.close();
                output = null;

            } catch (Exception e) {
                e.printStackTrace();
                mPromise.reject("E_GL_CANT_SAVE_SNAPSHOT", e.getMessage());
            }

            if (output == null) {
                // Return result object which imitates Expo.Asset so it can be used again to fill the texture
                Bundle result = new Bundle();
                String fileUri = Uri.fromFile(new File(path)).toString();

                result.putString("uri", fileUri);
                result.putString("localUri", fileUri);
                result.putInt("width", mWidth);
                result.putInt("height", mHeight);

                mPromise.resolve(result);
            }
            return null;
        }
    }

    // Solves number casting problem as number values can come as Integer or Double.
    private int castNumberToInt(Object value) {
        if (value instanceof Double) {
            return ((Double) value).intValue();
        }
        return (Integer) value;
    }
}