package org.itrabbit.rnwebgl2;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.PixelFormat;
import android.opengl.EGL14;
import android.opengl.GLSurfaceView;
import android.util.SparseArray;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.uimanager.events.RCTEventEmitter;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;


@SuppressLint("ViewConstructor")
public class RNWebGLView extends GLSurfaceView implements GLSurfaceView.Renderer  {

  private boolean onSurfaceCreateCalled = false;

  private RNWebGLContext mGLContext;
  private RNWebGLObjectManager mObjectManagerModule;

  private static SparseArray<RNWebGLView> mGLViewMap = new SparseArray<>();

  // Suppresses ViewConstructor warnings
  public RNWebGLView(Context context, RNWebGLObjectManager objectManagerModule) {
    super(context);

    mObjectManagerModule = objectManagerModule;
    mGLContext = new RNWebGLContext(mObjectManagerModule);

    setEGLContextClientVersion(3);
    setEGLConfigChooser(8, 8, 8, 8, 16, 0);
    getHolder().setFormat(PixelFormat.TRANSLUCENT);
    setRenderer(this);
  }

  // Public interface to allow running events on GL thread
  public synchronized static void runOnGLThread(int ctxId, Runnable r) {
    RNWebGLView glView = mGLViewMap.get(ctxId);
    if (glView != null) {
      glView.queueEvent(r);
    }
  }

  public static void endFrame(int ctxId) {
    RNWebGLView glView = mGLViewMap.get(ctxId);
    if (glView != null) {
      glView.endFrame();
    }
  }

  public static void requestFlush(final int ctxId){
    final RNWebGLView glView = mGLViewMap.get(ctxId);
    if (glView != null) {
      glView.queueEvent(new Runnable() {
        @Override
        public void run() {
          glView.flush();
        }
      });
    }
  }

  public void onReceiveSurfaceCreateEvent() {
    // Add view to map
    mGLViewMap.put(mGLContext.getContextId(), this);

    WritableMap event = Arguments.createMap();
    event.putInt("ctxId", mGLContext.getContextId());
    ReactContext reactContext = (ReactContext)getContext();
    reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(getId(), "surfaceCreate", event);
  }


  @Override
  public void onSurfaceCreated(GL10 gl, EGLConfig config) {

    EGL14.eglSurfaceAttrib(EGL14.eglGetCurrentDisplay(), EGL14.eglGetCurrentSurface(EGL14.EGL_DRAW),
            EGL14.EGL_SWAP_BEHAVIOR, EGL14.EGL_BUFFER_PRESERVED);

    if(mGLContext != null) {
      mGLContext.flush();
    }

    final RNWebGLView selfView = this;
    if (!onSurfaceCreateCalled) {
      final ReactContext reactContext = (ReactContext) getContext();
      reactContext.runOnJSQueueThread(new Runnable() {
        @Override
        public void run() {
          mGLContext.initialize(new Runnable() {
            @Override
            public void run() {
              selfView.setRenderMode(RENDERMODE_WHEN_DIRTY);
              selfView.onReceiveSurfaceCreateEvent();
            }
          });
        }
      });
      onSurfaceCreateCalled = true;
    }
  }

  public void endFrame() {
    requestRender();
  }

  public void flush() {
    if(mGLContext != null) {
      mGLContext.flush();
    }
  }

  @Override
  public void onSurfaceChanged(GL10 gl, int width, int height) {
  }

  @Override
  public synchronized void onDrawFrame(GL10 gl) {
    flush();
  }

  @Override
  public void onDetachedFromWindow() {
    if(mGLContext != null) {
      mGLContext.destroy();
    }
    super.onDetachedFromWindow();
  }
}