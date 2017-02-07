/*#########################################################################################################
# Author: wuhaizhou,420660135@qq.com,15996211983                                                          #
#                                                                                                         #
# Data: 2016.11.15                                                                                        #
#########################################################################################################*/

package com.avatarmind.visionservice;

import com.avatar.personate.Util;

public class Native {

    private static int connection = 0;
    private static boolean loaded = false;
    private OnDetectorListening listeners = null;

    static {
        System.loadLibrary("RVFClientJNI_rv_srv_0.02.0001_20170124_T");  
    }
     
    public Native() {
        this.Connect();
    }

    protected void finallize() {
        this.DisConnect();
    }

    public void Connect() {
        Util.Logd("Native", "Connect");
        if (connection != 0) {
            native_disconnect(connection);
            connection = 0;
        }
        connection = this.native_connect();
    }

    public void DisConnect() {
        Util.Logd("Native", "DisConnect");
        if (connection != 0) {
            this.native_disconnect(connection);
            connection = 0;
        }
    }

    public void setOnDetectorListening(OnDetectorListening e){
        Util.Logd("Native", "setOnDetectorListening");
        listeners = e;
    }

    public void eventNotify(String event, String lable, int PosX, int PoxY,
                            int Width, int Height) {
        if (listeners != null) {
            listeners.DetectorListening(event, lable, PosX, PoxY, Width, Height);
        }
    }

    public void switchDetector(String event, boolean enable) {
        Util.Logd("Native", "switchDetector");
        if (connection != 0) {
            native_switchDetector(connection, event, enable);
        }
    }

    public void registerFaceInfo(String name, String file) {
        Util.Logd("Native", "registerFaceInfo");
        if (connection != 0) {
            native_registerFaceInfo(connection, name, file);
        }
    }

    public void deleteFaceInfo(String name) {
        Util.Logd("Native", "deleteFaceInfo");
        if (connection != 0) {
            native_deleteFaceInfo(connection, name);
        }
    }

    public void suspend() {
        Util.Logd("Native", "suspend");
        if (connection != 0) {
            native_suspend(connection);
        }
    }

    public void resume() {
        Util.Logd("Native", "resume");
        if (connection != 0) {
            native_resume(connection);
        }
    }

    private native int  native_connect();
    private native void native_disconnect(int con);
    private native void native_switchDetector(int con, String event, boolean enable);
    private native void native_deleteFaceInfo(int con, String name);
    private native void native_registerFaceInfo(int con, String namesou, String file);
    private native void native_suspend(int con);
    private native void native_resume(int con);
}