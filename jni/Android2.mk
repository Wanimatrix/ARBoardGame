LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
OPENCV_CAMERA_MODULES:=on
OPENCV_INSTALL_MODULES:=on

LOCAL_MODULE    := nonfree_prebuilt
LOCAL_SRC_FILES := /Volumes/MacintoshHD/Users/wouterfranken/Development/OpenCVSiftAnd/libnonfree.so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE    := opencv_java_prebuilt
LOCAL_SRC_FILES := /Volumes/MacintoshHD/Users/wouterfranken/Development/OpenCVSiftAnd/libopencv_java.so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_C_INCLUDES:= /Volumes/MacintoshHD/Users/wouterfranken/Development/OpenCV-2.4.9-android-sdk/sdk/native/jni/include
LOCAL_MODULE    := jni_interface
LOCAL_CFLAGS    := -Werror -O3 -ffast-math
LOCAL_LDLIBS    += -llog -ldl
LOCAL_SHARED_LIBRARIES := nonfree_prebuilt opencv_java_prebuilt
LOCAL_SRC_FILES := pattern.cpp pattern_detector.cpp utilities.cpp jni_interface.cpp
include $(BUILD_SHARED_LIBRARY)