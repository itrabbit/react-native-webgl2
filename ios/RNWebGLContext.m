#import "RNWebGLContext.h"

#include <OpenGLES/ES3/gl.h>
#include <OpenGLES/ES3/glext.h>

#define BLOCK_SAFE_RUN(block, ...) block ? block(__VA_ARGS__) : nil

@interface RNWebGLContext ()

@property (nonatomic, strong) dispatch_queue_t glQueue;
@property (nonatomic, weak) RNWebGLObjectManager *objectManager;

@end

@interface RCTBridge ()

- (JSGlobalContextRef)jsContextRef;
- (void)dispatchBlock:(dispatch_block_t)block queue:(dispatch_queue_t)queue;

@end

@implementation RNWebGLContext

- (instancetype)initWithDelegate:(id<RNWebGLContextDelegate>)delegate andObjectMananger:(nonnull RNWebGLObjectManager *)objectManager {
    if (self = [super init]) {
        self.delegate = delegate;
        _objectManager = objectManager;
        _glQueue = dispatch_queue_create("host.rn.gl", DISPATCH_QUEUE_SERIAL);
        _eaglCtx = [[EAGLContext alloc] initWithAPI:kEAGLRenderingAPIOpenGLES3] ?: [[EAGLContext alloc] initWithAPI:kEAGLRenderingAPIOpenGLES2];
    }
    return self;
}

- (BOOL)isInitialized {
    return _contextId != 0;
}

- (EAGLContext *)createSharedEAGLContext {
    return [[EAGLContext alloc] initWithAPI:[_eaglCtx API] sharegroup:[_eaglCtx sharegroup]];
}

- (void)runInEAGLContext:(EAGLContext*)context callback:(void(^)(void))callback
{
    [EAGLContext setCurrentContext:context];
    callback();
    glFlush();
    [EAGLContext setCurrentContext:nil];
}

- (void)runAsync:(void(^)(void))callback {
    if (_glQueue) {
        dispatch_async(_glQueue, ^{
            [self runInEAGLContext:self->_eaglCtx callback:callback];
        });
    }
}

- (void)initialize:(void(^)(BOOL))callback {
    RCTBridge *bridge = _objectManager.bridge;
    if (!bridge.executorClass || [NSStringFromClass(bridge.executorClass) isEqualToString:@"RCTJSCExecutor"]) {
        __weak __typeof__(self) weakSelf = self;
        __weak __typeof__(bridge) weakBridge = bridge;
        [bridge dispatchBlock:^{
            __typeof__(self) self = weakSelf;
            __typeof__(bridge) bridge = weakBridge;
            if (!self || !bridge || !bridge.valid) {
                BLOCK_SAFE_RUN(callback, NO);
                return;
            }
            JSGlobalContextRef jsContextRef = [bridge jsContextRef];
            if (!jsContextRef) {
                BLOCK_SAFE_RUN(callback, NO);
                RCTLogError(@"RNWebGL: The React Native bridge unexpectedly does not have a JavaScriptCore context.");
                return;
            }
            self->_contextId = RNWebGLContextCreate(jsContextRef);
            [self->_objectManager saveContext:self];
            /*
            RNWebGLContextSetFlushMethodObjc(self->_contextId, ^{
                [self flush];
            });
            */
            
            if ([self.delegate respondsToSelector:@selector(glContextInitialized:)]) {
                [self.delegate glContextInitialized:self];
            }
            BLOCK_SAFE_RUN(callback, YES);
        } queue:RCTJSThread];
        return;
    }
    BLOCK_SAFE_RUN(callback, NO);
    RCTLogError(@"RNWebGL: Can only run on JavaScriptCore! Do you have 'Remote Debugging' enabled in your app's Developer Menu (https://facebook.github.io/react-native/docs/debugging.html)? RNWebGL is not supported while using Remote Debugging, you will need to disable it to use RNWebGL.");
}

- (void)flush {
    [self runAsync:^{
        RNWebGLContextFlush(self->_contextId);
        if ([self.delegate respondsToSelector:@selector(glContextFlushed:)]) {
            [self.delegate glContextFlushed:self];
        }
    }];
}

- (void)destroy {
    [self runAsync:^{
        if ([self.delegate respondsToSelector:@selector(glContextWillDestroy:)]) {
            [self.delegate glContextWillDestroy:self];
        }
        // Flush all the stuff
        RNWebGLContextFlush(self->_contextId);
        // Destroy JS binding
        RNWebGLContextDestroy(self->_contextId);
        // Unload objects
        [self->_objectManager unloadWithCtxId:self->_contextId];
        // Remove from dictionary of contexts
        [self->_objectManager deleteContextWithId:@(self->_contextId)];
    }];
}

# pragma mark - snapshots

// Saves the contents of the framebuffer to a file.
// Possible options:
// - `flip`: if true, the image will be flipped vertically.
// - `framebuffer`: WebGLFramebuffer that we will be reading from. If not specified, the default framebuffer for this context will be used.
// - `rect`: { x, y, width, height } object used to crop the snapshot.
// - `format`: "jpeg" or "png" - specifies what type of compression and file extension should be used.
// - `compress`: A value in 0 - 1 range specyfing compression level. JPEG format only.
- (void)takeSnapshotWithOptions:(nonnull NSDictionary *)options resolve:(RCTPromiseResolveBlock)resolve reject:(RCTPromiseRejectBlock)reject {
    [self flush];
    [self runAsync:^{
        NSDictionary *rect = options[@"rect"] ?: [self currentViewport];
        BOOL flip = options[@"flip"] != nil && [options[@"flip"] boolValue];
        NSString *format = options[@"format"];
        
        int x = [rect[@"x"] intValue];
        int y = [rect[@"y"] intValue];
        int width = [rect[@"width"] intValue];
        int height = [rect[@"height"] intValue];
        
        // Save surrounding framebuffer
        GLint prevFramebuffer;
        glGetIntegerv(GL_FRAMEBUFFER_BINDING, &prevFramebuffer);
        
        // Set source framebuffer that we take snapshot from
        GLint sourceFramebuffer = 0;
        
        if (options[@"framebuffer"] && options[@"framebuffer"][@"id"]) {
            int RNWebGLFramebufferId = [options[@"framebuffer"][@"id"] intValue];
            sourceFramebuffer = RNWebGLContextGetObject(self.contextId, RNWebGLFramebufferId);
        } else {
            // headless context doesn't have default framebuffer, so we use the current one
            sourceFramebuffer = [self defaultFramebuffer] || prevFramebuffer;
        }
        if (sourceFramebuffer == 0) {
            reject(
                   @"RNWEBGL_NO_FRAMEBUFFER",
                   nil,
                   RCTErrorWithMessage(@"No framebuffer bound. Create and bind one to take a snapshot from it.")
                   );
            return;
        }
        if (width <= 0 || height <= 0) {
            reject(
                   @"RNWEBGL_INVALID_VIEWPORT",
                   nil,
                   RCTErrorWithMessage(@"Rect's width and height must be greater than 0. If you didn't set `rect` option, check if the viewport is set correctly.")
                   );
            return;
        }
        
        // Bind source framebuffer
        glBindFramebuffer(GL_FRAMEBUFFER, sourceFramebuffer);
        
        // Allocate pixel buffer and read pixels
        NSInteger dataLength = width * height * 4;
        GLubyte *buffer = (GLubyte *) malloc(dataLength * sizeof(GLubyte));
        glReadBuffer(GL_COLOR_ATTACHMENT0);
        glReadPixels(x, y, width, height, GL_RGBA, GL_UNSIGNED_BYTE, buffer);
        
        // Create CGImage
        CGDataProviderRef providerRef = CGDataProviderCreateWithData(NULL, buffer, dataLength, NULL);
        CGColorSpaceRef colorspaceRef = CGColorSpaceCreateDeviceRGB();
        CGImageRef imageRef = CGImageCreate(width, height, 8, 32, width * 4, colorspaceRef, kCGBitmapByteOrder32Big | kCGImageAlphaPremultipliedLast,
                                            providerRef, NULL, true, kCGRenderingIntentDefault);
        
        // Begin image context
        CGFloat scale = [RNWebGLContext screenScale];
        NSInteger widthInPoints = width / scale;
        NSInteger heightInPoints = height / scale;
        UIGraphicsBeginImageContextWithOptions(CGSizeMake(widthInPoints, heightInPoints), NO, scale);
        
        // Flip and draw image to CGImage
        CGContextRef cgContext = UIGraphicsGetCurrentContext();
        if (flip) {
            CGAffineTransform flipVertical = CGAffineTransformMake(1, 0, 0, -1, 0, heightInPoints);
            CGContextConcatCTM(cgContext, flipVertical);
        }
        CGContextDrawImage(cgContext, CGRectMake(0.0, 0.0, widthInPoints, heightInPoints), imageRef);
        
        // Retrieve the UIImage from the current context
        UIImage *image = UIGraphicsGetImageFromCurrentImageContext();
        UIGraphicsEndImageContext();
        
        // Cleanup
        free(buffer);
        CFRelease(providerRef);
        CFRelease(colorspaceRef);
        CGImageRelease(imageRef);
        
        // Write image to file
        NSData *imageData;
        NSString *extension;
        
        if ([format isEqualToString:@"png"]) {
            imageData = UIImagePNGRepresentation(image);
            extension = @".png";
        } else {
            float compress = 1.0;
            if (options[@"compress"] != nil) {
                compress = [(NSString *)options[@"compress"] floatValue];
            }
            imageData = UIImageJPEGRepresentation(image, compress);
            extension = @".jpeg";
        }
        
        NSString *filePath = [self generateSnapshotPathWithExtension:extension];
        [imageData writeToFile:filePath atomically:YES];
        
        // Restore surrounding framebuffer
        glBindFramebuffer(GL_FRAMEBUFFER, prevFramebuffer);
        
        // Return result object which imitates Expo.Asset so it can be used again to fill the texture
        NSMutableDictionary *result = [[NSMutableDictionary alloc] init];
        NSString *fileUrl = [[NSURL fileURLWithPath:filePath] absoluteString];
        
        result[@"uri"] = fileUrl;
        result[@"localUri"] = fileUrl;
        result[@"width"] = @(width);
        result[@"height"] = @(height);
        
        resolve(result);
    }];
}

- (NSDictionary *)currentViewport {
    GLint viewport[4];
    glGetIntegerv(GL_VIEWPORT, viewport);
    return @{ @"x": @(viewport[0]), @"y": @(viewport[1]), @"width": @(viewport[2]), @"height": @(viewport[3]) };
}

- (GLint)defaultFramebuffer {
    if ([self.delegate respondsToSelector:@selector(glContextGetDefaultFramebuffer)]) {
        return [self.delegate glContextGetDefaultFramebuffer];
    }
    return 0;
}

- (NSString *)getPathForDirectory:(int)directory {
    NSArray *paths = NSSearchPathForDirectoriesInDomains(directory, NSUserDomainMask, YES);
    return [paths firstObject];
}

- (NSString *)generateSnapshotPathWithExtension:(NSString *)extension {
    NSString *directory = [[self getPathForDirectory: NSCachesDirectory] stringByAppendingPathComponent:@"GLView"];
    NSString *fileName = [[[NSUUID UUID] UUIDString] stringByAppendingString:extension];
    
    [[NSFileManager defaultManager] createDirectoryAtPath:directory withIntermediateDirectories:YES attributes:@{} error:nil];
    
    return [directory stringByAppendingPathComponent:fileName];
}

#pragma mark - Copy from RN

+ (BOOL)isMainQueue {
    static void *mainQueueKey = &mainQueueKey;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        dispatch_queue_set_specific(dispatch_get_main_queue(),
                                    mainQueueKey, mainQueueKey, NULL);
    });
    return dispatch_get_specific(mainQueueKey) == mainQueueKey;
}

+ (void)unsafeExecuteOnMainQueueOnceSync:(dispatch_once_t *)onceToken block:(dispatch_block_t)block {
    // The solution was borrowed from a post by Ben Alpert:
    // https://benalpert.com/2014/04/02/dispatch-once-initialization-on-the-main-thread.html
    // See also: https://www.mikeash.com/pyblog/friday-qa-2014-06-06-secrets-of-dispatch_once.html
    if ([self isMainQueue]) {
        dispatch_once(onceToken, block);
    } else {
        if (DISPATCH_EXPECT(*onceToken == 0L, NO)) {
            dispatch_sync(dispatch_get_main_queue(), ^{
                dispatch_once(onceToken, block);
            });
        }
    }
}

+ (CGFloat)screenScale {
    static dispatch_once_t onceToken;
    static CGFloat scale;
    [self unsafeExecuteOnMainQueueOnceSync:&onceToken block:^{
        scale = [UIScreen mainScreen].scale;
    }];
    return scale;
}

@end
