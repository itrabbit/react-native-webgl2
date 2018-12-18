#import "RNWebGL.h"

@interface RNWebGLObject : NSObject

@property (nonatomic, assign) RNWebGLContextId ctxId;
@property (nonatomic, assign) RNWebGLObjectId objId;

- (instancetype)initWithConfig:(NSDictionary *)config;

@end
