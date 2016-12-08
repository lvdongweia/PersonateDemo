package com.avatar.personate;


import android.util.Log;

public class Util {
    private static String TAG = "Personate";
    private static boolean DEBUG = true;

    public static int Logd(String tag, String msg) {
        if (DEBUG) {
            return Log.d(TAG, "[" + tag +"]:" + msg);
        }

        return 0;
    }

    public static int Loge(String tag, String msg) {
        if (DEBUG) {
            return Log.e(TAG, "[" + tag + "]:" + msg);
        }

        return 0;
    }
}
