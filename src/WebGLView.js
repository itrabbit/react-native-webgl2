//@flow
import React from "react";
import PropTypes from "prop-types";

import {
  View,
  Platform,
  ViewPropTypes,
  NativeModules,
  requireNativeComponent
} from "react-native";

import RNExtension from "./RNExtension";
import wrapGLMethods from "./wrapGLMethods";

const { RNWebGLObjectManager } = NativeModules;

// Get the GL interface from an RNWebGLContextID and do JS-side setup
const getGl = (ctxId: number): ?WebGLRenderingContext => {
  // noinspection JSUnresolvedVariable
  if (!global.__RNWebGLContexts) {
    console.warn(
      "RNWebGL: Can only run on JavaScriptCore! Do you have 'Remote Debugging' enabled in your app's Developer Menu (https://facebook.github.io/react-native/docs/debugging.html)? RNWebGL is not supported while using Remote Debugging, you will need to disable it to use RNWebGL."
    );
    return null;
  }
  const gl = global.__RNWebGLContexts[ctxId];
  gl.__ctxId = ctxId;
  delete global.__RNWebGLContexts[ctxId];

  // determine the prototype to use, depending on OpenGL ES version
  const glesVersion = gl.getParameter(gl.VERSION);
  const supportsWebGL2 = parseFloat(glesVersion.split(/[^\d.]+/g).join(" ")) >= 3.0;
  // noinspection JSUnresolvedVariable
  const prototype = supportsWebGL2 ? global.WebGL2RenderingContext.prototype : global.WebGLRenderingContext.prototype;

  if (Object.setPrototypeOf) {
    Object.setPrototypeOf(gl, prototype);
  } else {
    gl.__proto__ = prototype;
  }
  wrapGLMethods(gl, RNExtension.createWithContext(gl, ctxId));

  gl.canvas = null;

  const viewport = gl.getParameter(gl.VIEWPORT);
  gl.drawingBufferWidth = viewport[2];
  gl.drawingBufferHeight = viewport[3];

  // Enable/disable logging of all GL function calls
  let enableLogging = false;

  // $FlowIssue: Flow wants a "value" field
  Object.defineProperty(gl, "enableLogging", {
    configurable: true,
    get(): boolean {
      return enableLogging;
    },
    set(enable: boolean): void {
      if (enable === enableLogging) {
        return;
      }
      if (enable) {
        Object.keys(gl).forEach(key => {
          if (typeof gl[key] === "function") {
            const original = gl[key];
            gl[key] = (...args) => {
              // eslint-disable-next-line
              console.log(`RNWebGL: ${key}(${args.join(", ")})`);
              const r = original.apply(gl, args);
              // eslint-disable-next-line
              console.log(`RNWebGL:    = ${r}`);
              return r;
            };
            gl[key].original = original;
          }
        });
      } else {
        Object.keys(gl).forEach(key => {
          if (typeof gl[key] === "function" && gl[key].original) {
            gl[key] = gl[key].original;
          }
        });
      }
      enableLogging = enable;
    },
  });

  return gl;
};

const getContextId = (gl: WebGLRenderingContext | ?number) => {
  // noinspection JSUnresolvedVariable
  const ctxId = gl && typeof gl === "object" ? gl.__ctxId : gl;
  if (!ctxId || typeof ctxId !== "number") {
    throw new Error(`Invalid RNWebGLContext id: ${String(ctxId)}`);
  }
  return ctxId;
};

export type SnapshotOptions = {
  flip?: boolean,
  framebuffer?: WebGLFramebuffer,
  rect?: {
    x: number,
    y: number,
    width: number,
    height: number,
  },
  format?: 'jpeg' | 'png',
  compress?: number,
};

export type SurfaceCreateEvent = {
  nativeEvent: {
    ctxId: number,
  },
};

export default class WebGLView extends React.Component {
  props: {
    onContextCreate: (gl: WebGLRenderingContext) => void,
    onContextFailure: (e: Error) => void,
    msaaSamples: number
  };

  static propTypes = {
    msaaSamples: PropTypes.number,
    onContextCreate: PropTypes.func,
    onContextFailure: PropTypes.func,
    nativeRef_EXPERIMENTAL: PropTypes.func,
    ...ViewPropTypes
  };

  static defaultProps = {
    msaaSamples: 4
  };

  static async createContextAsync() {
    const { ctxId } = await RNWebGLObjectManager.createContextAsync();
    return getGl(ctxId);
  }

  static async destroyContextAsync(gl: WebGLRenderingContext | ?number) {
    const ctxId = getContextId(gl);
    return RNWebGLObjectManager.destroyContextAsync(ctxId);
  }

  static async takeSnapshotAsync(gl: WebGLRenderingContext | ?number, options: SnapshotOptions = {}) {
    const ctxId = getContextId(gl);
    return RNWebGLObjectManager.takeSnapshotAsync(ctxId, options);
  }

  nativeRef: ?WebGLView.NativeView;
  ctxId: ?number;

  render() {
    const {
      msaaSamples,
      ...viewProps
    } = this.props;

    return (
      <View {...viewProps}>
        <WebGLView.NativeView
          ref={this.setNativeRef}
          style={{ flex: 1, ...(Platform.OS === "ios" ? {backgroundColor: "transparent"} : {}) }}
          onSurfaceCreate={this.onSurfaceCreate}
          msaaSamples={Platform.OS === "ios" ? msaaSamples : undefined}
        />
      </View>
    );
  }

  setNativeRef = (nativeRef: React.ElementRef<typeof WebGLView.NativeView>) => {
    if (this.props.nativeRef_EXPERIMENTAL) {
      this.props.nativeRef_EXPERIMENTAL(nativeRef);
    }
    this.nativeRef = nativeRef;
  };

  onSurfaceCreate = ({ nativeEvent: { ctxId } }: SurfaceCreateEvent) => {
    let gl, error;
    try {
      gl = getGl(ctxId);
      if (!gl) {
        error = new Error("RNWebGL context creation failed");
      }
    } catch (e) {
      error = e;
    }
    if (error) {
      if (this.props.onContextFailure) {
        this.props.onContextFailure(error);
      } else {
        throw error;
      }
    } else if (gl && this.props.onContextCreate) {
      this.props.onContextCreate(gl);
    }
  };

  destroyObjectAsync(glObject: WebGLObject) {
    return RNWebGLObjectManager.destroyObjectAsync(glObject.id);
  }

  takeSnapshotAsync(options: SnapshotOptions = {}) {
    const { ctxId } = this;
    return WebGLView.takeSnapshotAsync(ctxId, options);
  }

  static NativeView = requireNativeComponent("RNWebGLView", WebGLView, {
    nativeOnly: { onSurfaceCreate: true }
  });
}
