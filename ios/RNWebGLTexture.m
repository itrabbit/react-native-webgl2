#import "RNWebGLTexture.h"

@implementation RNWebGLTexture {
  bool _attached;
  NSMutableArray *attachedEventQueue;
}

- (instancetype)initWithConfig:(NSDictionary *)config withWidth:(int)width withHeight:(int)height {
  _attached = false;
  attachedEventQueue = nil;
  if ((self = [super initWithConfig:config])) {
    _width = width;
    _height = height;
    attachedEventQueue = [[NSMutableArray alloc] init];
  }
  return self;
}

- (void)attachTexture: (GLuint)texture {
    if (self.objId != 0) {
        RNWebGLContextMapObject(self.ctxId, self.objId, texture);
        _attached = true;
        for (RNWebGLListenAttachedCallback cb in attachedEventQueue) {
            cb();
        }
    }
    attachedEventQueue = [[NSMutableArray alloc] init];
}

- (bool)isAttached {
  return _attached;
}

- (void)listenAttached: (RNWebGLListenAttachedCallback)callback {
    if(attachedEventQueue != nil) {
        [attachedEventQueue addObject:callback];
    }
}

- (void)unload {
  // this is meant to be overrated by implementations. they should dispose everything so the dealloc below is reached
}

@end
