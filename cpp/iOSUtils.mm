#include "iOSUtils.h"
#include <stdarg.h>

#import <Foundation/Foundation.h>

void iOSLog(const char *msg, ...) {
  va_list args;
  va_start(args, msg);
  NSLog(@"%@", [[NSString alloc] initWithFormat:[NSString stringWithUTF8String:msg] arguments:args]);
  va_end(args);
}

iOSOperatingSystemVersion iOSGetOperatingSystemVersion() {
  NSOperatingSystemVersion version = NSProcessInfo.processInfo.operatingSystemVersion;
    return iOSOperatingSystemVersion {
    version.majorVersion,
    version.minorVersion,
    version.patchVersion,
  };
}
