#if __has_include(<React/RCTViewManager.h>)
#import <React/RCTViewManager.h>
#elif __has_include("React/RCTViewManager.h")
#import "React/RCTViewManager.h"
#else
#import "RCTViewManager.h"
#endif

@interface RNWebGLViewManager : RCTViewManager

@end
