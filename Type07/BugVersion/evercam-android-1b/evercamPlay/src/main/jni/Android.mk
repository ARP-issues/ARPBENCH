LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

GSTREAMER_ROOT_ANDROID := /Users/liutingdu/Android3rdParty/gstreamer-1.0-android-arm-1.6.1

SHELL := PATH=/usr/bin:/bin:/usr/sbin:/sbin:/usr/local/bin /bin/bash

LOCAL_MODULE    := evercam
LOCAL_SRC_FILES := evercam.cpp mediaplayer.cpp eventloop.cpp android_rt_gles2.cpp frameflipper.cpp
LOCAL_CPPFLAGS = -std=c++11 -fexceptions -frtti
LOCAL_SHARED_LIBRARIES := gstreamer_android
LOCAL_LDLIBS := -llog -landroid -lGLESv2 -lEGL
include $(BUILD_SHARED_LIBRARY)

ifndef GSTREAMER_ROOT
ifndef GSTREAMER_ROOT_ANDROID
$(error GSTREAMER_ROOT_ANDROID is not defined!)
endif
GSTREAMER_ROOT        := $(GSTREAMER_ROOT_ANDROID)
endif

GSTREAMER_NDK_BUILD_PATH  := $(GSTREAMER_ROOT)/share/gst-android/ndk-build/

include $(GSTREAMER_NDK_BUILD_PATH)/plugins.mk
GSTREAMER_PLUGINS         := $(GSTREAMER_PLUGINS_CORE) $(GSTREAMER_PLUGINS_PLAYBACK) $(GSTREAMER_PLUGINS_CODECS) $(GSTREAMER_PLUGINS_NET) $(GSTREAMER_PLUGINS_SYS) $(GSTREAMER_PLUGINS_CODECS_RESTRICTED) $(GSTREAMER_PLUGINS_CODECS_GPL) $(GSTREAMER_PLUGINS_ENCODING)
GSTREAMER_EXTRA_DEPS      := gstreamer-video-1.0

# TODO: Remove TARGET_LDFLAGS after upgrading to GStreamer 1.6.2
TARGET_LDFLAGS := -Wl,-soname,libgstreamer_android.so
include $(GSTREAMER_NDK_BUILD_PATH)/gstreamer-1.0.mk
