package org.itrabbit.rnwebgl2;

import com.facebook.react.bridge.ReadableMap;

public interface RNWebGLObjectConfigLoader {
    boolean canLoadConfig (ReadableMap config);
    void loadWithConfig (ReadableMap config, RNWebGLObjectCompletionBlock callback);

}
