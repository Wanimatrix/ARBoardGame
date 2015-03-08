LOCAL_PATH:= $(call my-dir)
LOCAL_PATH_EXT	:= $(call my-dir)/../extra_libs/

include $(CLEAR_VARS)
LOCAL_MODULE    := opencv_java_prebuilt
LOCAL_SRC_FILES := /Volumes/MacintoshHD/Users/wouterfranken/Development/opencv/platforms/build_android_arm/lib/armeabi-v7a/libopencv_java.so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE    := aruco
LOCAL_SRC_FILES := /Volumes/MacintoshHD/Users/wouterfranken/Development/arucoBuild/obj/local/armeabi-v7a/libaruco_opencv.so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
OpenCV_INSTALL_MODULES:=on
OPENCV_CAMERA_MODULES:=off
OPENCV_LIB_TYPE:=SHARE
ifeq ("$(wildcard $(OPENCV_MK_PATH))","")
include /Volumes/MacintoshHD/Users/wouterfranken/Development/OpenCV-2.4.9-android-sdk/sdk/native/jni/OpenCV.mk
else
include $(OPENCV_MK_PATH)
endif

LOCAL_C_INCLUDES:= /Volumes/MacintoshHD/Users/wouterfranken/Development/OpenCV-2.4.9-android-sdk/sdk/native/jni/include
LOCAL_C_INCLUDES+= /Volumes/MacintoshHD/Users/wouterfranken/Development/arucoBuild/jni
LOCAL_C_INCLUDES+= $(LOCAL_PATH)/../include
LOCAL_C_INCLUDES+= $(LOCAL_PATH)/BrickDetectorLines
LOCAL_MODULE    := jni_interface
LOCAL_CFLAGS    := -Werror -O3 -pipe -fPIC -mfpu=neon -ffast-math -fopenmp -DANDROID_CL -DWITH_TBB=YES
LOCAL_LDLIBS    += -llog -ldl $(LOCAL_PATH_EXT)libOpenCL.so
LOCAL_LDFLAGS   += -O3 -fopenmp
LOCAL_SHARED_LIBRARIES := opencv_java_prebuilt aruco
LOCAL_SRC_FILES := utilities.cpp jni_interface.cpp BrickDetectorLines/brickDetectorLines.cpp showHOG.cpp features.cpp detect.cpp
include $(BUILD_SHARED_LIBRARY)
