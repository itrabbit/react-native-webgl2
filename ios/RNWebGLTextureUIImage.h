#import <UIKit/UIKit.h>

#import "RNWebGLTexture.h"


@interface RNWebGLTextureUIImage: RNWebGLTexture

- (instancetype)initWithConfig:(NSDictionary *)config byObjectManager:(RNWebGLObjectManager*) manager withImage:(UIImage *)image;

@end
