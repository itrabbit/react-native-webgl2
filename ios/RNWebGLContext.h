#import <OpenGLES/EAGL.h>

#if __has_include(<React/RCTConvert.h>)
#import <React/RCTConvert.h>
#elif __has_include("React/RCTConvert.h")
#import "React/RCTConvert.h"
#else
#import "RCTConvert.h"
#endif

#import "RNWebGLObjectManager.h"
#import "RNWebGL.h"

@class RNWebGLContext;

@protocol RNWebGLContextDelegate <NSObject>

- (void)glContextFlushed:(nonnull RNWebGLContext *)context;
- (void)glContextInitialized:(nonnull RNWebGLContext *)context;
- (void)glContextWillDestroy:(nonnull RNWebGLContext *)context;
- (RNWebGLObjectId)glContextGetDefaultFramebuffer;

@end

@interface RNWebGLContext : NSObject

- (instancetype)initWithDelegate:(id<RNWebGLContextDelegate>)delegate andObjectMananger:(nonnull RNWebGLObjectManager *)objectManager;
- (void)initialize:(void(^)(BOOL))callback;
- (BOOL)isInitialized;
- (EAGLContext *)createSharedEAGLContext;
- (void)runAsync:(void(^)(void))callback;
- (void)runInEAGLContext:(EAGLContext*)context callback:(void(^)(void))callback;
- (void)takeSnapshotWithOptions:(nonnull NSDictionary *)options resolve:(RCTPromiseResolveBlock)resolve reject:(RCTPromiseRejectBlock)reject;
- (void)destroy;

// "protected"
@property (nonatomic, assign) RNWebGLContextId contextId;
@property (nonatomic, strong, nonnull) EAGLContext *eaglCtx;
@property (nonatomic, weak, nullable) id <RNWebGLContextDelegate> delegate;

@end
