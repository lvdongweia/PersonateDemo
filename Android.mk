
LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES := \
	$(call all-java-files-under, src) \

LOCAL_RESOURCE_DIR := $(LOCAL_PATH)/res

LOCAL_MODULE_TAGS := optional

LOCAL_PACKAGE_NAME := PersonateDemo
LOCAL_CERTIFICATE := platform
LOCAL_DEX_EXPORT := false

LOCAL_PROGUARD_ENABLED := disabled

include $(BUILD_PACKAGE)

