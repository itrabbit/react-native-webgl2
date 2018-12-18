#import <UIKit/UIKit.h>

#import "RNWebGLTexture.h"

@interface RNWebGLTextureUIImage: RNWebGLTexture
- (instancetype)initWithConfig:(NSDictionary *)config withImage:(UIImage *)image;
@end
