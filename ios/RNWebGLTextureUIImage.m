#if __has_include(<React/RCTConvert.h>)
#import <React/RCTConvert.h>
#elif __has_include("React/RCTConvert.h")
#import "React/RCTConvert.h"
#else
#import "RCTConvert.h"
#endif

#import "RNWebGLTextureUIImage.h"

@implementation RNWebGLTextureUIImage

- (instancetype)initWithConfig:(NSDictionary *)config withImage:(UIImage *)image {
    bool yflip = [RCTConvert BOOL:config[@"yflip"]];
    if (yflip) {
        image = [RNWebGLTextureUIImage flipImageVertically: image];
    }
    if((self = [super initWithConfig:config withWidth:image.size.width withHeight:image.size.height])) {
      CGImageRef imageRef = [image CGImage];
      unsigned long width = CGImageGetWidth(imageRef);
      unsigned long height = CGImageGetHeight(imageRef);
      
      NSUInteger bytesPerPixel = CGImageGetBitsPerPixel(imageRef);
      NSUInteger bitsPerComponent = CGImageGetBitsPerComponent(imageRef);
      
      GLubyte *textureData = (GLubyte *)malloc(width * height * bytesPerPixel);
      
      CGColorSpaceRef colorSpace = CGColorSpaceCreateDeviceRGB();
      NSUInteger bytesPerRow = bytesPerPixel * width;
      CGContextRef context = CGBitmapContextCreate(textureData, width, height,
                                                   bitsPerComponent, bytesPerRow, colorSpace,
                                                   kCGImageAlphaPremultipliedLast | kCGBitmapByteOrder32Big);
      CGColorSpaceRelease(colorSpace);
      
      CGContextDrawImage(context, CGRectMake(0, 0, width, height), imageRef);
      CGContextRelease(context);
      
      GLuint textureID;
      glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
      glGenTextures(1, &textureID);
      
      glBindTexture(GL_TEXTURE_2D, textureID);
      glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, (int)width, (int)height, 0, GL_RGBA, GL_UNSIGNED_BYTE, textureData);
      
      [self attachTexture:textureID];
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
