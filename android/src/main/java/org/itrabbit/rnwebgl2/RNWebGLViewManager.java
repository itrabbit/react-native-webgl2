package org.itrabbit.rnwebgl2;

import com.facebook.react.common.MapBuilder;
import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;

import java.util.Map;

import javax.annotation.Nullable;

@SuppressWarnings("WeakerAccess")
public class RNWebGLViewManager extends SimpleViewManager<RNWebGLView> {

  private static final String REACT_CLASS = "RNWebGLView";

  private RNWebGLObjectManager mObjectManagerModule;

  public RNWebGLViewManager(RNWebGLObjectManager module) {
    super();
    mObjectManagerModule = module;
  }

  @Override
  public String getName() {
    return RNWebGLViewManager.REACT_CLASS;
  }

  @Override
  public RNWebGLView createViewInstance(ThemedReactContext context) {
    return new RNWebGLView(context, mObjectManagerModule);
  }

  @SuppressWarnings("unchecked")
  @Override
  public @Nullable Map getExportedCustomDirectEventTypeConstants() {
    return MapBuilder.of("surfaceCreate", MapBuilder.of("registrationName", "onSurfaceCreate"));
  }
}
