package org.itrabbit.rnwebgl2;

import android.content.Context;
import android.view.TextureView;
import android.annotation.SuppressLint;
import android.graphics.SurfaceTexture;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.uimanager.events.RCTEventEmitter;

@SuppressWarnings({"FieldCanBeLocal", "unused"})
@SuppressLint("ViewConstructor")
public class RNWebGLView extends TextureView implements TextureView.SurfaceTextureListener {
  private boolean mOnSurfaceCreateCalled = false;
  private boolean mOnSurfaceTextureCreatedWithZeroSize = false;

  private RNWebGLContext mGLContext;
  private RNWebGLObjectManager mObjectManagerModule;

  // Suppresses ViewConstructor warnings
  public RNWebGLView(Context context, RNWebGLObjectManager objectManagerModule) {
    super(context);
    setSurfaceTextureListener(this);
    setOpaque(false);

    mGLContext = new RNWebGLContext(objectManagerModule);
  }

  // Public interface to allow running events on GL thread

  public void runOnGLThread(Runnable r) {
    mGLContext.runAsync(r);
  }

  public void runOnGLThread(int ctxId, Runnable r) {
    if(mGLContext.getContextId() == ctxId) {
      this.runOnGLThread(r);
      return;
    }
    RNWebGLContext ctx = mObjectManagerModule.getContextWithId(ctxId);
    if(ctx != null) {
      ctx.runAsync(r);
    }
  }

  public RNWebGLContext getGLContext() {
    return mGLContext;
  }


  // `TextureView.SurfaceTextureListener` events

  @Override
  synchronized public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
    if (!mOnSurfaceCreateCalled) {
      // onSurfaceTextureAvailable is sometimes called with 0 size texture
      // and immediately followed by onSurfaceTextureSizeChanged with actual size
      if (width == 0 || height == 0) {
        mOnSurfaceTextureCreatedWithZeroSize = true;
      }
      if (!mOnSurfaceTextureCreatedWithZeroSize) {
        initializeSurfaceInGLContext(surfaceTexture);
      }
      mOnSurfaceCreateCalled = true;
    }
  }

  @Override
  public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
    mGLContext.destroy();

    // reset flag, so the context will be recreated when the new surface is available
    mOnSurfaceCreateCalled = false;

    return true;
  }

  @Override
  synchronized public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {
    if (mOnSurfaceTextureCreatedWithZeroSize && width != 0 && height != 0) {
      initializeSurfaceInGLContext(surfaceTexture);
      mOnSurfaceTextureCreatedWithZeroSize = false;
    }
  }

  @Override
  public void onSurfaceTextureUpdated(SurfaceTexture surface) {
  }

  public void flush() {
    mGLContext.flush();
  }

  public int getCtxId() {
    return mGLContext.getContextId();
  }

  private void initializeSurfaceInGLContext(SurfaceTexture surfaceTexture) {
    mGLContext.initialize(surfaceTexture, new Runnable() {
      @Override
      public void run() {
        WritableMap event = Arguments.createMap();
        event.putInt("ctxId", mGLContext.getContextId());
        ReactContext reactContext = (ReactContext)getContext();
        reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(getId(), "onSurfaceCreate", event);
      }
    });
  }
}
