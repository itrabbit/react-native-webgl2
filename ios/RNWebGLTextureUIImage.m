#if __has_include(<React/RCTConvert.h>)
#import <React/RCTConvert.h>
#elif __has_include("React/RCTConvert.h")
#import "React/RCTConvert.h"
#else
#import "RCTConvert.h"
#endif

#import "RNWebGLContext.h"
#import "RNWebGLObjectManager.h"
#import "RNWebGLTextureUIImage.h"

@implementation RNWebGLTextureUIImage

- (instancetype)initWithConfig:(NSDictionary *)config byObjectManager:(RNWebGLObjectManager *)manager withImage:(UIImage *)image {
    bool yflip = [RCTConvert BOOL:config[@"yflip"]];
    if (yflip) {
        image = [RNWebGLTextureUIImage flipImageVertically: image];
    }
    if((self = [super initWithConfig:config withWidth:image.size.width withHeight:image.size.height])) {
      RNWebGLContext * ctx = [manager getContextWithId: @(self.ctxId)];
      if(ctx) {
          // TODO: Add support more UIImage
          [ctx runAsync:^{
              CGImageRef imageRef = [image CGImage];
              
              unsigned long width = CGImageGetWidth(imageRef);
              unsigned long height = CGImageGetHeight(imageRef);
            
              GLubyte* textureData = (GLubyte *)malloc(width * height * 4);
              CGColorSpaceRef colorSpace = CGColorSpaceCreateDeviceRGB();
              NSUInteger bytesPerPixel = 4;
              NSUInteger bytesPerRow = bytesPerPixel * width;
              NSUInteger bitsPerComponent = 8;
              CGContextRef context = CGBitmapContextCreate(textureData, width, height,
                                                           bitsPerComponent, bytesPerRow, colorSpace,
                                                           kCGImageAlphaPremultipliedLast | kCGBitmapByteOrder32Big);
              CGColorSpaceRelease(colorSpace);
              CGContextDrawImage(context, CGRectMake(0, 0, width, height), imageRef);
              CGContextRelease(context);
              
              GLuint textureID;
              GLint boundedBefore;
              glGenTextures(1, &textureID);
              glGetIntegerv(GL_TEXTURE_BINDING_2D, &boundedBefore);
              
              glBindTexture(GL_TEXTURE_2D, textureID);
              glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
              glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
              
              glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, (int)width, (int)height, 0, GL_RGBA, GL_UNSIGNED_BYTE, textureData);
              
              glBindTexture(GL_TEXTURE_2D, boundedBefore);

              free(textureData);

              [self attachTexture:textureID];
          }];
      }
    }
    return self;
}

+ (UIImage *) flipImageVertically:(UIImage *)image {
    CGSize size = image.size;
    UIGraphicsBeginImageContextWithOptions(size, NO, [UIScreen mainScreen].scale);
    CGContextRef context = UIGraphicsGetCurrentContext();
    CGAffineTransform flipVertical = CGAffineTransformMake(1, 0, 0, -1, 0, size.height);
    CGContextConcatCTM(context, flipVertical);
    [image drawInRect: CGRectMake(0, 0, size.width, size.height)];
    UIImage *flipedImage = UIGraphicsGetImageFromCurrentImageContext();
    UIGraphicsEndImageContext();
    return flipedImage;
}

@end
