#if __has_include(<React/RCTBridgeModule.h>)
#import <React/RCTBridgeModule.h>
#elif __has_include("React/RCTBridgeModule.h")
#import "React/RCTBridgeModule.h"
#else
#import "RCTBridgeModule.h"
#endif

#import "RNWebGLObject.h"

typedef void (^RNWebGLObjectCompletionBlock)(NSError *error, RNWebGLObject *obj);

@protocol RNWebGLObjectConfigLoader <RCTBridgeModule>
-(BOOL)canLoadConfig:(NSDictionary *)config;
-(void)loadWithConfig:(NSDictionary *)config withCompletionBlock:(RNWebGLObjectCompletionBlock)callback;
@end

@interface RNWebGLObjectManager : NSObject <RCTBridgeModule>

-(id<RNWebGLObjectConfigLoader>) objectLoaderForConfig:(NSDictionary *)config;

-(void)loadWithConfig:(NSDictionary *)config withCompletionBlock:(RNWebGLObjectCompletionBlock)callback;

-(void)loadWithConfigAndWaitAttached:(NSDictionary *)config withCompletionBlock:(RNWebGLObjectCompletionBlock)callback;

-(void)unloadWithObjId:(RNWebGLObjectId)id;

-(void)unloadWithCtxId:(RNWebGLContextId)id;


- (void)saveContext:(nonnull id)glContext;
- (void)deleteContextWithId:(nonnull NSNumber *)contextId;

@end

@interface RCTBridge (RNWebGLObjectManager)
@property (nonatomic, readonly) RNWebGLObjectManager *webglObjectManager;
@end