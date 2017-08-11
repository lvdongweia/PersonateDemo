
LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES := \
	$(call all-java-files-under, src) \

LOCAL_RESOURCE_DIR := $(LOCAL_PATH)/res

LOCAL_MODULE_TAGS := optional

LOCAL_PACKAGE_NAME := PersonateDemo
LOCAL_CERTIFICATE := platform
LOCAL_DEX_EXPORT := false

LOCAL_JNI_SHARED_LIBRARIES := \
	libhandcover \
	libhandwave

LOCAL_PROGUARD_ENABLED := disabled
LOCAL_STATIC_JAVA_LIBRARIES := RobotVision

include $(BUILD_PACKAGE)

####################################################

include $(CLEAR_VARS)
LOCAL_PREBUILT_LIBS := \
	libhandcover:libs/libhandcover.so \
	libhandwave:libs/libhandwave.so
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := \
	RobotVision:libs/libRobotVisionService.jar

LOCAL_MODULE_TAGS := optional
include $(BUILD_MULTI_PREBUILT)
