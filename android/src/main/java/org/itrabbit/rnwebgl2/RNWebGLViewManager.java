package org.itrabbit.rnwebgl2;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.common.MapBuilder;
import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;

import java.util.Map;

import javax.annotation.Nullable;

@SuppressWarnings("WeakerAccess")
public class RNWebGLViewManager extends SimpleViewManager<RNWebGLView> {

    private static final String REACT_CLASS = "RNWebGLView";

    private ReactApplicationContext mContext;

    public RNWebGLViewManager(ReactApplicationContext context) {
        super();
        mContext = context;
    }

    @Override
    public String getName() {
        return RNWebGLViewManager.REACT_CLASS;
    }

    @Override
    public RNWebGLView createViewInstance(ThemedReactContext context) {
        return new RNWebGLView(context, mContext.getNativeModule(RNWebGLObjectManager.class));
    }

    @SuppressWarnings("unchecked")
    @Override
    public @Nullable
    Map getExportedCustomDirectEventTypeConstants() {
        return MapBuilder.of("surfaceCreate", MapBuilder.of("registrationName", "onSurfaceCreate"));
    }
}
