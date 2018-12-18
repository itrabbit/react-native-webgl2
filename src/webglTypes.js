//@flow

// JavaScript WebGL types to wrap around native objects

export const RNWebGLRenderingContext = class WebGLRenderingContext {
  __ctxId: ?number;
};

export const RNWebGL2RenderingContext = class WebGL2RenderingContext extends RNWebGLRenderingContext {};

const idToObject = {};

export const RNWebGLObject = class WebGLObject {
  id: *;
  constructor(id: *) {
    if (idToObject[id]) {
      throw new Error(
        `WebGL object with underlying RNWebGLTextureId '${id}' already exists!`
      );
    }
    this.id = id; // Native GL object id
  }
  toString() {
    return `[WebGLObject ${this.id}]`;
  }

  static wrap = (type: Class<RNWebGLObject>, id: *): WebGLObject => {
    const found = idToObject[id];
    if (found) {
      return found;
    }
    return (idToObject[id] = new type(id));
  };
};

// WebGL classes
export const RNWebGLBuffer = class WebGLBuffer extends RNWebGLObject {};
export const RNWebGLFramebuffer = class WebGLFramebuffer extends RNWebGLObject {};
export const RNWebGLProgram = class WebGLProgram extends RNWebGLObject {};
export const RNWebGLRenderbuffer = class WebGLRenderbuffer extends RNWebGLObject {};
export const RNWebGLShader = class WebGLShader extends RNWebGLObject {};
export const RNWebGLTexture = class WebGLTexture extends RNWebGLObject {};
export const RNWebGLUniformLocation = class WebGLUniformLocation {
  id: *;
  constructor(id: *) {
    this.id = id; // Native GL object id
  }
};
export const RNWebGLActiveInfo = class WebGLActiveInfo {
  constructor(obj: *) {
    Object.assign(this, obj);
  }
};
export const RNWebGLShaderPrecisionFormat = class WebGLShaderPrecisionFormat {
  constructor(obj: *) {
    Object.assign(this, obj);
  }
};

// WebGL2 classes
export const RNWebGLQuery = class WebGLQuery extends RNWebGLObject {};
export const RNWebGLSampler = class WebGLSampler extends RNWebGLObject {};
export const RNWebGLSync = class WebGLSync extends RNWebGLObject {};
export const RNWebGLTransformFeedback = class WebGLTransformFeedback extends RNWebGLObject {};
export const RNWebGLVertexArrayObject = class WebGLVertexArrayObject extends RNWebGLObject {};

// also leak them in global, like in a browser
global.WebGLSync = RNWebGLSync;
global.WebGLQuery = RNWebGLQuery;
global.WebGLShader = RNWebGLShader;
global.WebGLObject = RNWebGLObject;
global.WebGLBuffer = RNWebGLBuffer;
global.WebGLProgram = RNWebGLProgram;
global.WebGLSampler = RNWebGLSampler;
global.WebGLTexture = RNWebGLTexture;
global.WebGLActiveInfo = RNWebGLActiveInfo;
global.WebGLFramebuffer = RNWebGLFramebuffer;
global.WebGLRenderbuffer = RNWebGLRenderbuffer;
global.WebGLUniformLocation = RNWebGLUniformLocation;
global.WebGLRenderingContext = RNWebGLRenderingContext;
global.WebGL2RenderingContext = RNWebGL2RenderingContext;
global.WebGLTransformFeedback = RNWebGLTransformFeedback;
global.WebGLVertexArrayObject = RNWebGLVertexArrayObject;
global.WebGLShaderPrecisionFormat = RNWebGLShaderPrecisionFormat;
