LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)
OPENCV_CAMERA_MODULES:=on
OPENCV_INSTALL_MODULES:=on

LOCAL_MODULE    := sift_prebuilt
LOCAL_SRC_FILES := /Volumes/MacintoshHD/Users/wouterfranken/Development/OpenCVSiftAnd/libnonfree.so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE    := opencv_java_prebuilt
LOCAL_SRC_FILES := /Volumes/MacintoshHD/Users/wouterfranken/Development/opencv/platforms/build_android_arm/lib/armeabi-v7a/libopencv_java.so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE    := aruco
LOCAL_SRC_FILES := /Volumes/MacintoshHD/Users/wouterfranken/Development/arucoBuild/obj/local/armeabi-v7a/libaruco_opencv.so
include $(PREBUILT_SHARED_LIBRARY)



include $(CLEAR_VARS)
LOCAL_C_INCLUDES:= /Volumes/MacintoshHD/Users/wouterfranken/Development/OpenCV-2.4.9-android-sdk/sdk/native/jni/include
LOCAL_C_INCLUDES+= /Volumes/MacintoshHD/Users/wouterfranken/Development/arucoBuild/jni
LOCAL_MODULE    := jni_interface
LOCAL_CFLAGS    := -Werror -O3 -pipe -fPIC -mfpu=vfpv3 -DWITH_TBB=YES -ffast-math -fopenmp -pg
LOCAL_LDLIBS    += -llog -ldl 
LOCAL_LD_FLAGS  += -fopenmp
LOCAL_SHARED_LIBRARIES := opencv_java_prebuilt aruco
LOCAL_STATIC_LIBRARIES := android-ndk-profiler
LOCAL_SRC_FILES := pattern.cpp pattern_detector.cpp utilities.cpp jni_interface.cpp
include $(BUILD_SHARED_LIBRARY)
$(call import-module,android-ndk-profiler)