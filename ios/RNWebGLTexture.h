#import "RNWebGLObject.h"

typedef void (^RNWebGLListenAttachedCallback)();

@interface RNWebGLTexture: RNWebGLObject

- (instancetype)initWithConfig:(NSDictionary *)config withWidth:(int)width withHeight:(int)height;

- (void)attachTexture: (GLuint)texture;

- (bool)isAttached;
- (void)listenAttached: (RNWebGLListenAttachedCallback)callback;

- (void)unload;

@property (nonatomic, assign) int width;
@property (nonatomic, assign) int height;

@end
