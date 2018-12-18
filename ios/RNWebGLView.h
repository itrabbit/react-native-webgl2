#if __has_include(<React/RCTBridge.h>)
#import <React/RCTBridge.h>
#elif __has_include("React/RCTBridge.h")
#import "React/RCTBridge.h"
#else
#import "RCTBridge.h"
#endif


#import "RNWebGL.h"
#import "RNWebGLContext.h"
#import "RNWebGLViewManager.h"

NS_ASSUME_NONNULL_BEGIN

@interface RNWebGLView : UIView <RNWebGLContextDelegate>

- (instancetype)initWithManager:(RNWebGLViewManager *)viewManager;
- (RNWebGLContextId)ctxId;

@property (nonatomic, copy, nullable) RCTDirectEventBlock onSurfaceCreate;
@property (nonatomic, assign) NSNumber *msaaSamples;

@property (nonatomic, strong, nullable) RNWebGLContext *glContext;
@property (nonatomic, strong, nullable) EAGLContext *uiEaglCtx;

@end

NS_ASSUME_NONNULL_END
