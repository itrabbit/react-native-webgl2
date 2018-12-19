package org.itrabbit.rnwebgl2;

import static org.itrabbit.rnwebgl2.RNWebGL.*;

@SuppressWarnings("WeakerAccess")
public class RNWebGLObject {

    protected int ctxId;
    protected int objId;

    RNWebGLObject(int ctxId) {
        // Generic
        this.ctxId = ctxId;
        this.objId = RNWebGLContextCreateObject(ctxId);
    }

    int getObjId() {
        return objId;
    }

    void destroy() {
        if (objId != 0) {
            RNWebGLContextDestroyObject(ctxId, objId);
        }
    }
}
