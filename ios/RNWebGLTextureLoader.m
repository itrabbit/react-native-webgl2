#if __has_include(<React/RCTConvert.h>)
#import <React/RCTConvert.h>
#elif __has_include("React/RCTConvert.h")
#import "React/RCTConvert.h"
#else
#import "RCTConvert.h"
#endif

#if __has_include(<React/RCTLog.h>)
#import <React/RCTLog.h>
#elif __has_include("React/RCTLog.h")
#import "React/RCTLog.h"
#else
#import "RCTLog.h"
#endif

#if __has_include(<React/RCTImageSource.h>)
#import <React/RCTImageSource.h>
#elif __has_include("React/RCTImageSource.h")
#import "React/RCTImageSource.h"
#else
#import "RCTImageSource.h"
#endif

#if __has_include(<React/RCTBridge.h>)
#import <React/RCTBridge.h>
#elif __has_include("React/RCTBridge.h")
#import "React/RCTBridge.h"
#else
#import "RCTBridge.h"
#endif

#if __has_include(<React/RCTImageLoader.h>)
#import <React/RCTImageLoader.h>
#elif __has_include("React/RCTImageLoader.h")
#import "React/RCTImageLoader.h"
#else
#import "RCTImageLoader.h"
#endif

#if __has_include(<React/RCTUtils.h>)
#import <React/RCTUtils.h>
#elif __has_include("React/RCTUtils.h")
#import "React/RCTUtils.h"
#else
#import "RCTUtils.h"
#endif

#import "RNWebGLTextureLoader.h"
#import "RNWebGLTextureUIImage.h"

@implementation RNWebGLTextureLoader

@synthesize bridge = _bridge;

RCT_EXPORT_MODULE()

- (BOOL)canLoadConfig:(NSDictionary *)config {
    return [config objectForKey:@"image"] != nil;
}

- (NSString *)getPathForDirectory:(int)directory {
    NSArray* paths = NSSearchPathForDirectoriesInDomains(directory, NSUserDomainMask, YES);
    return [paths firstObject];
}

-(void)loadWithConfig:(NSDictionary *)config byObjectManager:(RNWebGLObjectManager *)manager withCompletionBlock:(RNWebGLObjectCompletionBlock)callback {
    id imageDict = [config objectForKey:@"image"];
    if(imageDict != NULL) {
        if([imageDict isKindOfClass:[NSDictionary class]]) {
            NSString* base64 = [imageDict objectForKey:@"base64"];
            if(base64 != NULL) {
                NSData* data = [[NSData alloc]initWithBase64EncodedString:base64 options:NSDataBase64DecodingIgnoreUnknownCharacters];
                RNWebGLTextureUIImage *obj = [[RNWebGLTextureUIImage alloc] initWithConfig:config byObjectManager:manager withImage: [UIImage imageWithData:data]];
                callback(nil, (RNWebGLObject*)obj);
                return;
            }
        }
    }
    RCTImageSource* source = [RCTConvert RCTImageSource:[config objectForKey:@"image"]];
    if(source == NULL) {
        callback([[NSError alloc] initWithDomain:@"" code:0 userInfo: @{@"NSLocalizedDescriptionKey": @"Cannot get image source!"}], nil);
        return;
    }
    NSString* path = [[source.request URL] absoluteString];
    if(path != NULL && path.length > 0) {
        NSString* localPath = NULL;
        if([path.lowercaseString hasPrefix:@"asset://"]) {
            localPath = [NSString stringWithFormat:@"%@/%@", [[NSBundle mainBundle] bundlePath], [path substringFromIndex:8]];
        } else if([path.lowercaseString hasPrefix:@"file://"]) {
            localPath = [NSString stringWithFormat:@"%@/%@", [self getPathForDirectory:NSDocumentDirectory], [path substringFromIndex:7]];
        } else if([path.lowercaseString hasPrefix:@"external://"]) {
            localPath = [path substringFromIndex:11];
        }      
        if(localPath != NULL && [[NSFileManager defaultManager] fileExistsAtPath:localPath]) {
            NSLog(@"[RNWebGLTextureRCTImageLoader] Check loading file: %@", localPath);

            NSError *error = nil;
            NSDictionary *attributes = [[NSFileManager defaultManager] attributesOfItemAtPath:localPath error:&error];
            if (error) {
                callback([[NSError alloc] initWithDomain:@"" code:0 userInfo: @{@"NSLocalizedDescriptionKey": error.localizedDescription}], nil);
                return;
            }
            if ([attributes objectForKey:NSFileType] == NSFileTypeDirectory) {
                callback([[NSError alloc] initWithDomain:@"" code:0 userInfo: @{@"NSLocalizedDescriptionKey": @"Cannot read directory, only files!"}], nil);
                return;
            }
            NSData *data = [[NSFileManager defaultManager] contentsAtPath:localPath];
            RNWebGLTextureUIImage *obj = [[RNWebGLTextureUIImage alloc] initWithConfig:config byObjectManager:manager withImage: [UIImage imageWithData:data]];
            callback(nil, obj);
            return;
        }
    }
    [_bridge.imageLoader loadImageWithURLRequest:source.request
                                            size:CGSizeZero
                                           scale:0
                                         clipped:YES
                                      resizeMode:RCTResizeModeStretch
                                   progressBlock:nil
                                partialLoadBlock:nil
                                 completionBlock:^(NSError *error, UIImage *loadedImage) {
                                     void (^setImageBlock)(UIImage *) = ^(UIImage *image) {
                                         RNWebGLTextureUIImage *obj = [[RNWebGLTextureUIImage alloc] initWithConfig:config byObjectManager:manager withImage:image];
                                         callback(nil, obj);
                                     };
                                     if (error) {
                                         callback(error, nil);
                                     } else {
                                         if ([NSThread isMainThread]) {
                                             setImageBlock(loadedImage);
                                         } else {
                                             RCTExecuteOnMainQueue(^{
                                                 setImageBlock(loadedImage);
                                             });
                                         }
                                     }
                                 }];
}

@end
