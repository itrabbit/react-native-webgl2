#import "RNWebGLTextureManager.h"
#import "RNWebGLTextureLoader.h"
#import "RNWebGLTexture.h"

@implementation RNWebGLTextureManager

@synthesize bridge = _bridge;

RCT_EXPORT_MODULE();

RCT_EXPORT_METHOD(create:(NSDictionary *)config
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
  [_bridge.webglObjectManager loadWithConfigAndWaitAttached:config withCompletionBlock:^(NSError *error, RNWebGLObject *obj) {
    if (error) {
      reject(error.domain, error.description, error);
      return;
    }
    RNWebGLTexture *texture = (RNWebGLTexture *)obj;
    if(texture) {
      resolve(@{ @"objId": @(texture.objId),
                 @"width": @(texture.width),
                 @"height": @(texture.height) });
    } else {
      resolve(@{ @"objId": @(obj.objId) });
    }    
  }];
}

RCT_EXPORT_METHOD(destroy:(nonnull NSNumber *)id) {
  [_bridge.webglObjectManager unloadWithObjId:[id intValue]];
}

@end
