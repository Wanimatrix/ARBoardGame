#ifndef LOGGER_H
#define LOGGER_H

#ifdef ANDROID
        // LOGS ANDROID
  #include <android/log.h>
  #define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG,__VA_ARGS__)
  #define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG  , LOG_TAG,__VA_ARGS__)
  #define LOGI(...) __android_log_print(ANDROID_LOG_INFO   , LOG_TAG,__VA_ARGS__)
  #define LOGW(...) __android_log_print(ANDROID_LOG_WARN   , LOG_TAG,__VA_ARGS__)
  #define LOGE(...) __android_log_print(ANDROID_LOG_ERROR  , LOG_TAG,__VA_ARGS__)
  #define LOGSIMPLE(...)

  #define LOGIMG(filename, mat) imwrite(string("/sdcard/arbg/")+LOG_TAG+"_"+filename+".png",mat);
#else
        // LOGS NO ANDROID
  #include <stdio.h>
  #define LOGV(...) printf("<%s> ",LOG_TAG);printf(__VA_ARGS__); printf("\n");
  #define LOGD(...) printf("<%s> ",LOG_TAG);printf(__VA_ARGS__); printf("\n");
  #define LOGI(...) printf("<%s> ",LOG_TAG);printf(__VA_ARGS__); printf("\n");
  #define LOGW(...) printf("<%s> * Warning: ",LOG_TAG); printf(__VA_ARGS__); printf("\n");
  #define LOGE(...) printf("<%s> *** Error:  ",LOG_TAG);printf(__VA_ARGS__); printf("\n");
  #define LOGSIMPLE(...) printf(" ");printf(__VA_ARGS__);

  #define LOGIMG(filename, mat) imshow(filename,mat);
#endif // ANDROID

#endif // LOGGER_H
