#import "RNWebGLObject.h"

@implementation RNWebGLObject

- (instancetype)initWithConfig:(NSDictionary *)config {
    if ((self = [super init])) {
        _ctxId = [config[@"ctxId"] unsignedIntValue];
        _objId = RNWebGLContextCreateObject(_ctxId);
    }
    return self;
}

- (void)dealloc {
    if (_objId != 0) {
        RNWebGLContextDestroyObject(_ctxId, _objId);
    }
}

@end
