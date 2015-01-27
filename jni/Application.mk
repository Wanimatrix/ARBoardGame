APP_ABI := armeabi-v7a
APP_STL := gnustl_shared
APP_OPTIM := debug
APP_CPPFLAGS := -std=c++11 -frtti -fexceptions -O3 -pipe -fPIC -mfpu=neon -fopenmp #-DWITH_TBB=YES 
NDK_DEBUG	 := 1
APP_PLATFORM := android-19
NDK_TOOLCHAIN := arm-linux-androideabi-4.6
