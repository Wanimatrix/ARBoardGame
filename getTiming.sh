#! /bin/sh

adb pull /sdcard/timerData/yuv2bgr.txt ../Tekst/Timing/Naive/$1
adb pull /sdcard/timerData/camerapose.txt ../Tekst/Timing/Naive/$1
adb pull /sdcard/timerData/frameTicks.txt ../Tekst/Timing/Naive/$1
adb pull /sdcard/timerData/BrickTracking.txt ../Tekst/Timing/Naive/$1
adb pull /sdcard/timerData/Total.txt ../Tekst/Timing/Naive/$1
adb pull /sdcard/timerData/lemmingUpdate.txt ../Tekst/Timing/Naive/$1
