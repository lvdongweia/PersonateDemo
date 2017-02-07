
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
LOCAL_JAVA_LIBRARIES := \
	com.avatarmind.camera.extensions

include $(BUILD_PACKAGE)

####################################################

include $(CLEAR_VARS)
LOCAL_PREBUILT_LIBS := \
	lib/libRVFClientJNI_rv_srv_0.02.0001_20170124_T.so \
	lib/libRVFTCP_rv_srv_0.02.0001_20170124_T.so \
	lib/libRVFClientCpp_rv_srv_0.02.0001_20170124_T.so

LOCAL_MODULE_TAGS := optional
include $(BULID_MULTI_PREBUILT)
