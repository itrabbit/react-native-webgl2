#ifndef __EXIOSUTILS_H__
#define __EXIOSUTILS_H__

void iOSLog(const char *msg, ...) __attribute__((format(printf, 1, 2)));

struct iOSOperatingSystemVersion {
  long majorVersion;
  long minorVersion;
  long patchVersion;
};

iOSOperatingSystemVersion iOSGetOperatingSystemVersion();

#endif

