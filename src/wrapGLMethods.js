//@flow
import { Platform } from "react-native";

import {
  RNWebGLQuery,
  RNWebGLBuffer,
  RNWebGLObject,
  RNWebGLShader,
  RNWebGLSampler,
  RNWebGLTexture,
  RNWebGLProgram,
  RNWebGLFramebuffer,
  RNWebGLRenderbuffer,
  RNWebGLVertexArrayObject,
  RNWebGLTransformFeedback
} from "./webglTypes";

import type { Extension } from "./RNExtension";

export const objectId = (obj: WebGLObject) => obj && obj.id;

// Many functions need wrapping/unwrapping of arguments and return value. We
// handle each case specifically so we can write the tightest code for
// better performance.
export default (gl: Object, extension: Extension) => {
  const wrap = (methodNames, wrapper) =>
    (Array.isArray(methodNames) ? methodNames : [methodNames]).forEach(
      methodName => (gl[methodName] = wrapper(gl[methodName]))
    );

  // We can be slow in `gl.getParameter(...)` since it's a blocking call anyways
  // noinspection JSUnresolvedVariable
  const getParameterTypes = {
    [gl.CURRENT_PROGRAM]: WebGLProgram,
    [gl.SAMPLER_BINDING]: WebGLSampler,
    [gl.ARRAY_BUFFER_BINDING]: WebGLBuffer,
    [gl.FRAMEBUFFER_BINDING]: WebGLFramebuffer,
    [gl.COPY_READ_BUFFER_BINDING]: WebGLBuffer,
    [gl.COPY_WRITE_BUFFER_BINDING]: WebGLBuffer,
    [gl.DRAW_FRAMEBUFFER_BINDING]: WebGLFramebuffer,
    [gl.ELEMENT_ARRAY_BUFFER_BINDING]: WebGLBuffer,
    [gl.READ_FRAMEBUFFER_BINDING]: WebGLFramebuffer,
    [gl.RENDERBUFFER_BINDING]: WebGLRenderbuffer,
    [gl.TEXTURE_BINDING_2D_ARRAY]: WebGLTexture,
    [gl.TEXTURE_BINDING_2D]: WebGLTexture,
    [gl.TEXTURE_BINDING_3D]: WebGLTexture,
    [gl.TEXTURE_BINDING_CUBE_MAP]: WebGLTexture,
    [gl.TRANSFORM_FEEDBACK_BINDING]: WebGLTransformFeedback,
    [gl.TRANSFORM_FEEDBACK_BUFFER_BINDING]: WebGLBuffer,
    [gl.UNIFORM_BUFFER_BINDING]: WebGLBuffer,
    [gl.VERTEX_ARRAY_BINDING]: WebGLVertexArrayObject,
  };
  wrap("getParameter", orig => pname => {
    let ret = orig.call(gl, pname);
    if (pname === gl.VERSION) {
      // Wrap native version name
      ret = `WebGL 2.0 (rn-webgl2, ${Platform.OS}) (${ret})`;
    }
    const type = getParameterTypes[pname];
    return type ? RNWebGLObject.wrap(type, ret) : ret;
  });

  // Buffers
  wrap("bindBuffer", orig => (target, buffer) =>
    orig.call(gl, target, buffer && buffer.id)
  );
  wrap("createBuffer", orig => () =>
    RNWebGLObject.wrap(RNWebGLBuffer, orig.call(gl))
  );
  wrap("deleteBuffer", orig => buffer => orig.call(gl, buffer && buffer.id));
  wrap("isBuffer", orig => buffer =>
    buffer instanceof WebGLBuffer && orig.call(gl, buffer.id)
  );

  // Frame buffers
  wrap("bindFramebuffer", orig => (target, framebuffer) =>
    orig.call(gl, target, framebuffer && framebuffer.id)
  );
  wrap("createFramebuffer", orig => () =>
    RNWebGLObject.wrap(RNWebGLFramebuffer, orig.call(gl))
  );
  wrap("deleteFramebuffer", orig => framebuffer =>
    orig.call(gl, framebuffer && framebuffer.id)
  );
  wrap("framebufferRenderbuffer", orig => (target, attachment, rbtarget, rb) =>
    orig.call(gl, target, attachment, rbtarget, rb && rb.id)
  );
  wrap(
    "framebufferTexture2D",
    orig => (target, attachment, textarget, tex, level) =>
      orig.call(gl, target, attachment, textarget, tex && tex.id, level)
  );
  wrap("isFramebuffer", orig => framebuffer =>
    framebuffer instanceof WebGLFramebuffer && orig.call(gl, framebuffer.id)
  );
  wrap("framebufferTextureLayer", orig => (target, attachment, texture, level, layer) => {
    return orig.call(gl, target, attachment, objectId(texture), level, layer);
  });

  // Render buffers
  wrap("bindRenderbuffer", orig => (target, renderbuffer) =>
    orig.call(gl, target, renderbuffer && renderbuffer.id)
  );
  wrap("createRenderbuffer", orig => () =>
    RNWebGLObject.wrap(RNWebGLRenderbuffer, orig.call(gl))
  );
  wrap("deleteRenderbuffer", orig => renderbuffer =>
    orig.call(gl, renderbuffer && renderbuffer.id)
  );
  wrap("isRenderbuffer", orig => renderbuffer =>
    renderbuffer instanceof WebGLRenderbuffer && orig.call(gl, renderbuffer.id)
  );

  // Textures
  wrap("bindTexture", orig => (target, texture) =>
    orig.call(gl, target, texture && texture.id)
  );
  wrap("createTexture", orig => () =>
    RNWebGLObject.wrap(RNWebGLTexture, orig.call(gl))
  );
  wrap("deleteTexture", orig => texture =>
    orig.call(gl, texture && texture.id)
  );
  wrap("isTexture", orig => texture =>
    texture instanceof WebGLTexture && orig.call(gl, texture.id)
  );

  // Programs and shaders
  wrap("attachShader", orig => (program, shader) =>
    orig.call(gl, program && program.id, shader && shader.id)
  );
  wrap("bindAttribLocation", orig => (program, index, name) =>
    orig.call(gl, program && program.id, index, name)
  );
  wrap("compileShader", orig => shader => orig.call(gl, shader && shader.id));
  wrap("createProgram", orig => () =>
    RNWebGLObject.wrap(RNWebGLProgram, orig.call(gl))
  );
  wrap("createShader", orig => type =>
    RNWebGLObject.wrap(RNWebGLShader, orig.call(gl, type))
  );
  wrap("deleteProgram", orig => program =>
    orig.call(gl, program && program.id)
  );
  wrap("deleteShader", orig => shader => orig.call(gl, shader && shader.id));
  wrap("detachShader", orig => (program, shader) =>
    orig.call(gl, program && program.id, shader && shader.id)
  );
  wrap("getAttachedShaders", orig => program =>
    orig
      .call(gl, program && program.id)
      .map(id => RNWebGLObject.wrap(RNWebGLShader, id))
  );
  wrap("getProgramParameter", orig => (program, pname) =>
    orig.call(gl, program && program.id, pname)
  );
  wrap("getProgramInfoLog", orig => program =>
    orig.call(gl, program && program.id)
  );
  wrap("getShaderParameter", orig => (shader, pname) =>
    orig.call(gl, shader && shader.id, pname)
  );
  wrap("getShaderPrecisionFormat", orig => (shadertype, precisiontype) =>
    // $FlowFixMe
    new WebGLShaderPrecisionFormat(orig.call(gl, shadertype, precisiontype))
  );
  wrap("getShaderInfoLog", orig => shader =>
    orig.call(gl, shader && shader.id)
  );
  wrap("getShaderSource", orig => shader => orig.call(gl, shader && shader.id));
  wrap("linkProgram", orig => program => orig.call(gl, program && program.id));
  wrap("shaderSource", orig => (shader, source) =>
    orig.call(gl, shader && shader.id, source)
  );
  wrap("useProgram", orig => program => orig.call(gl, program && program.id));
  wrap("validateProgram", orig => program =>
    orig.call(gl, program && program.id)
  );
  wrap("isShader", orig => shader =>
    shader instanceof WebGLShader && orig.call(gl, shader.id)
  );
  wrap("isProgram", orig => program =>
    program instanceof WebGLProgram && orig.call(gl, program.id)
  );
  wrap("getFragDataLocation", orig => program => orig.call(gl, objectId(program)));
  wrap("getExtension", orig => id => {
    if (id === "RN") return extension;
    return orig.call(gl, id);
  });

  // Uniforms and attributes
  wrap("getActiveAttrib", orig => (program, index) =>
    // $FlowFixMe
    new WebGLActiveInfo(orig.call(gl, program && program.id, index))
  );
  wrap("getActiveUniform", orig => (program, index) =>
    // $FlowFixMe
    new WebGLActiveInfo(orig.call(gl, program && program.id, index))
  );
  wrap("getAttribLocation", orig => (program, name) =>
    orig.call(gl, program && program.id, name)
  );
  wrap("getUniform", orig => (program, location) =>
    orig.call(gl, program && program.id, location && location.id)
  );
  wrap("getUniformLocation", orig => (program, name) =>
    // $FlowFixMe
    new WebGLUniformLocation(orig.call(gl, program && program.id, name))
  );
  wrap(["uniform1f", "uniform1i", "uniform1ui"], orig => (loc, x) =>
    orig.call(gl, loc && loc.id, x)
  );
  wrap(["uniform2f", "uniform2i", "uniform2ui"], orig => (loc, x, y) =>
    orig.call(gl, loc && loc.id, x, y)
  );
  wrap(["uniform3f", "uniform3i", "uniform3ui"], orig => (loc, x, y, z) =>
    orig.call(gl, loc && loc.id, x, y, z)
  );
  wrap(["uniform4f", "uniform4i", "uniform4ui"], orig => (loc, x, y, z, w) =>
    orig.call(gl, loc && loc.id, x, y, z, w)
  );
  wrap(
    ["uniform1fv", "uniform2fv", "uniform3fv", "uniform4fv"],
    orig => (loc, val) => orig.call(gl, loc && loc.id, new Float32Array(val))
  );
  wrap(
    ["uniform1iv", "uniform2iv", "uniform3iv", "uniform4iv"],
    orig => (loc, val) => orig.call(gl, loc && loc.id, new Int32Array(val))
  );
  wrap(
    [
      "uniformMatrix2fv",
      "uniformMatrix3fv",
      "uniformMatrix4fv",
      "uniformMatrix3x2fv",
      "uniformMatrix4x2fv",
      "uniformMatrix2x3fv",
      "uniformMatrix4x3fv",
      "uniformMatrix2x4fv",
      "uniformMatrix3x4fv",
    ],
    orig => (loc, transpose, val) =>
      orig.call(gl, loc && loc.id, transpose, new Float32Array(val))
  );
  wrap(
    [
      "vertexAttrib1fv",
      "vertexAttrib2fv",
      "vertexAttrib3fv",
      "vertexAttrib4fv"
    ],
    orig => (index, val) => orig.call(gl, index, new Float32Array(val))
  );
  wrap("vertexAttribI4iv", orig => (index, val) => orig.call(gl, index, new Int32Array(val)));
  wrap("vertexAttribI4uiv", orig => (index, val) => orig.call(gl, index, new Uint32Array(val)));

  // Query objects
  wrap("createQuery", orig => () => RNWebGLObject.wrap(RNWebGLQuery, orig.call(gl)));
  wrap("deleteQuery", orig => query => orig.call(gl, objectId(query)));
  wrap("beginQuery", orig => (target, query) => orig.call(gl, target, objectId(query)));
  wrap("getQuery", orig => (target, pname) => {
    const id = orig.call(gl, target, pname);
    return id ? RNWebGLObject.wrap(RNWebGLQuery, id) : id;
  });
  wrap("getQueryParameter", orig => (query, pname) => orig.call(gl, objectId(query), pname));

  // Samplers
  wrap("bindSampler", orig => (unit, sampler) => orig.call(gl, unit, objectId(sampler)));
  wrap("createSampler", orig => () => RNWebGLObject.wrap(RNWebGLSampler, orig.call(gl)));
  wrap("deleteSampler", orig => sampler => orig.call(gl, objectId(sampler)));
  wrap("isSampler", orig => sampler => sampler instanceof WebGLSampler && orig.call(gl, sampler.id));
  wrap(["samplerParameteri", "samplerParameterf"], orig => (sampler, pname, param) => {
    return orig.call(gl, objectId(sampler), pname, param);
  });
  wrap("getSamplerParameter", orig => (sampler, pname) => {
    return orig.call(gl, objectId(sampler), pname);
  });

  // Transform feedback
  wrap("bindTransformFeedback", orig => (target, transformFeedback) => {
    return orig.call(gl, target, objectId(transformFeedback));
  });
  wrap("createTransformFeedback", orig => () => RNWebGLObject.wrap(RNWebGLTransformFeedback, orig.call(gl)));
  wrap("deleteTransformFeedback", orig => transformFeedback => {
    return orig.call(gl, objectId(transformFeedback));
  });
  wrap("transformFeedbackVaryings", orig => (program, varyings, bufferMode) => {
    return orig.call(gl, objectId(program), varyings, bufferMode);
  });
  wrap("getTransformFeedbackVarying", orig => (program, index) => {
    return new WebGLActiveInfo(orig.call(gl, objectId(program), index));
  });

  // Uniforms and attributes
  wrap(["bindBufferBase", "bindBufferRange"], orig => (target, index, buffer, ...rest) => {
    return orig.call(gl, target, index, objectId(buffer), ...rest);
  });
  wrap("getUniformIndices", orig => (program, uniformNames) => {
    // according to WebGL2 specs, it returns Array instead of Uint32Array
    const uintArray = orig.call(gl, objectId(program), uniformNames);
    return Array.from(uintArray);
  });
  wrap("getActiveUniforms", orig => (program, uniformIndices, pname) => {
    // according to WebGL2 specs, it returns Array instead of Int32Array
    const intArray = orig.call(gl, objectId(program), new Uint32Array(uniformIndices), pname);
    // noinspection JSUnresolvedVariable
    const boolResult = pname === gl.UNIFORM_IS_ROW_MAJOR;
    const arr = Array.from(intArray);
    return boolResult ? arr.map(val => !!val) : arr;
  });
  wrap("getUniformBlockIndex", orig => (program, uniformBlockName) =>
    orig.call(gl, objectId(program), uniformBlockName)
  );
  wrap("getActiveUniformBlockName", orig => (program, uniformBlockIndex) =>
    orig.call(gl, objectId(program), uniformBlockIndex)
  );
  wrap("uniformBlockBinding", orig => (program, uniformBlockIndex, uniformBlockBinding) => {
    return orig.call(gl, objectId(program), uniformBlockIndex, uniformBlockBinding);
  });

  // Vertex array objects
  wrap("bindVertexArray", orig => vertexArray => orig.call(gl, vertexArray && vertexArray.id));
  wrap("createVertexArray", orig => () => RNWebGLObject.wrap(RNWebGLVertexArrayObject, orig.call(gl)));
  wrap("deleteVertexArray", orig => vertexArray => orig.call(gl, vertexArray && vertexArray.id));
  wrap("isVertexArray", orig => vertexArray =>
    vertexArray instanceof WebGLVertexArrayObject && orig.call(gl, vertexArray.id)
  );
};
