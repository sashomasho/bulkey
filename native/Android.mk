LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_C_INCLUDES += $(LOCAL_PATH)/src

LOCAL_SRC_FILES := \
	jni/org_apelikecoder_bulgariankeyboard_BinaryDictionary.cpp \
	src/dictionary.cpp \
	src/char_utils.cpp

LOCAL_NDK_VERSION := 4
LOCAL_SDK_VERSION := 8

LOCAL_MODULE := libjni_kbd

LOCAL_MODULE_TAGS := user
LOCAL_LDLIBS     := -llog

include $(BUILD_SHARED_LIBRARY)
