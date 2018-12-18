#import "RNWebGLObjectManager.h"
#import "RNWebGLContext.h"

#import "RNWebGLTextureUIImage.h"

@interface RNWebGLObjectManager()

@property (nonatomic, strong) NSMutableDictionary<NSNumber *, RNWebGLContext *> *glContexts;
@property (nonatomic, strong) NSMutableDictionary<NSNumber *, RNWebGLObject *> *objects;
@property (nonatomic, strong) NSArray<id<RNWebGLObjectConfigLoader>> *loaders;

@end

@implementation RNWebGLObjectManager

@synthesize bridge = _bridge;

RCT_EXPORT_MODULE(RNWebGLObjectManager);

- (instancetype)init {
    if ((self = [super init])) {
        _glContexts = [NSMutableDictionary dictionary];
        _objects = [NSMutableDictionary dictionary];
        _loaders = NULL;
    }
    return self;
}

- (dispatch_queue_t)methodQueue {
    return dispatch_queue_create("host.rn.exponent.GLObjectManager", DISPATCH_QUEUE_SERIAL);
}

- (RNWebGLContext *)getContextWithId:(NSNumber *)contextId {
    return _glContexts[contextId];
}

- (void)saveContext:(nonnull RNWebGLContext *)glContext {
    if (glContext.isInitialized) {
        [_glContexts setObject:glContext forKey:@(glContext.contextId)];
    }
}

- (void)deleteContextWithId:(nonnull NSNumber *)contextId {
    [_glContexts removeObjectForKey:contextId];
}

- (void)dealloc {
    [_glContexts enumerateKeysAndObjectsUsingBlock:^(NSNumber * _Nonnull contextId, RNWebGLContext * _Nonnull glContext, BOOL * _Nonnull stop) {
        [glContext destroy];
    }];
}

# pragma mark - Snapshots

RCT_REMAP_METHOD(takeSnapshotAsync,
                 takeSnapshotWithContextId:(nonnull NSNumber *)ctxId
                 andOptions:(nonnull NSDictionary *)options
                 resolver:(RCTPromiseResolveBlock)resolve
                 rejecter:(RCTPromiseRejectBlock)reject)
{
    RNWebGLContext *glContext = [self getContextWithId:ctxId];
    if (glContext == nil) {
        reject(@"RNWEBGL_BAD_VIEW_TAG", nil, RCTErrorWithMessage(@"ExponentGLObjectManager.takeSnapshotAsync: RNWebGLContext not found for given context id."));
        return;
    }
    [glContext takeSnapshotWithOptions:options resolve:resolve reject:reject];
}

# pragma mark - Headless Context

RCT_REMAP_METHOD(createContextAsync,
                 createContext:(RCTPromiseResolveBlock)resolve
                 reject:(RCTPromiseRejectBlock)reject)
{    
    RNWebGLContext *glContext = [[RNWebGLContext alloc] initWithDelegate:nil andObjectMananger:self];
    
    [glContext initialize:^(BOOL success) {
        if (success) {
            resolve(@{ @"ctxId": @(glContext.contextId) });
        } else {
            reject(
                   @"RNWEB_CONTEXT_NOT_INITIALIZED",
                   nil,
                   RCTErrorWithMessage(@"RNWebObjectManager.createContextAsync: Unexpected error occurred when initializing headless context")
                   );
        }
    }];
}

RCT_REMAP_METHOD(destroyContextAsync,
                 destroyContextWithId:(nonnull NSNumber *)ctxId
                 resolve:(RCTPromiseResolveBlock)resolve
                 reject:(RCTPromiseRejectBlock)reject) {
    RNWebGLContext *glContext = [self getContextWithId:ctxId];
    if (glContext != nil) {
        [glContext destroy];
        resolve(@(YES));
    } else {
        resolve(@(NO));
    }
}

RCT_REMAP_METHOD(destroyObjectAsync,
                 destroyObjectAsync:(nonnull NSNumber *)objId
                 resolve:(RCTPromiseResolveBlock)resolve
                 reject:(RCTPromiseRejectBlock)reject) {
    _objects[objId] = nil;
    resolve(@(YES));
}


# pragma mark - Object Loading

-(id<RNWebGLObjectConfigLoader>) objectLoaderForConfig:(NSDictionary *)config {
    if (!_loaders) {
        _loaders = [_bridge modulesConformingToProtocol:@protocol(RNWebGLObjectConfigLoader)];
    }
    for (id<RNWebGLObjectConfigLoader> loader in _loaders) {
        if ([loader canLoadConfig:config]) {
            return loader;
        }
    }
    return nil;
}

-(void)loadWithConfig:(NSDictionary *)config withCompletionBlock:(RNWebGLObjectCompletionBlock)callback {
    id<RNWebGLObjectConfigLoader> loader = [self objectLoaderForConfig:config];
    if (!loader) {
        if (RCT_DEBUG) RCTLogError(@"No suitable RNWebGLTextureLoader found for %@", config);
        callback([NSError errorWithDomain:@"RNWebGL" code:1 userInfo:@{ NSLocalizedDescriptionKey: @"No suitable RNWebGLTextureLoader found" }], nil);
    }
    else {
        __weak RNWebGLObjectManager *weakSelf = self;
        [loader loadWithConfig:config withCompletionBlock:^(NSError *err, RNWebGLObject *obj) {
            if(obj != nil && weakSelf != nil) {
                weakSelf.objects[@(obj.objId)] = obj;
            }
            callback(err, obj);
        }];
    }
}

-(void)loadWithConfigAndWaitAttached:(NSDictionary *)config withCompletionBlock:(RNWebGLObjectCompletionBlock)callback {
    [self loadWithConfig:config withCompletionBlock:^(NSError *error, RNWebGLObject *obj) {
        if(obj != nil) {
            if([obj isKindOfClass:[RNWebGLTexture class]]) {
                if ([((RNWebGLTexture*)obj) isAttached]) {
                    callback(error, obj);
                    return;
                }
                [((RNWebGLTexture*)obj) listenAttached:^{
                    callback(error, obj);
                }];
                return;
            }
            callback(error, obj);
            return;
        }
        callback(error, nil);
    }];
}

-(void)unloadWithObjId:(RNWebGLObjectId)objId {
    NSNumber *key = @(objId);
    RNWebGLObject *t = _objects[key];
    if (t) {
        if([t isKindOfClass:[RNWebGLTexture class]]) {
            [((RNWebGLTexture*)t) unload];
            
        }
        [_objects removeObjectForKey:key];
    }
}

-(void)unloadWithCtxId:(RNWebGLContextId)ctxId {
    NSMutableArray *unloadedKeys = [NSMutableArray array];
    for (NSNumber *key in [_objects keyEnumerator]) {
        RNWebGLObject *t = _objects[key];
        if (t.ctxId == ctxId) {
            if([t isKindOfClass:[RNWebGLTexture class]]) {
                [((RNWebGLTexture*)t) unload];
                
            }
            [unloadedKeys addObject:key];
        }
    }
    [_objects removeObjectsForKeys:unloadedKeys];
}

@end

# pragma mark - Bridge

@implementation RCTBridge (RNWebGLObjectManager)

- (RNWebGLObjectManager *)webglObjectManager {
    return [self moduleForClass:[RNWebGLObjectManager class]];
}

@end

