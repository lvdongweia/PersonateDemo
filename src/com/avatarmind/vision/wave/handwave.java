package com.avatarmind.vision.wave;

public class handwave {
    static {
        System.loadLibrary("handwave");
    }

    public static native String detectwave(byte[] inputImagebuffer, int w,
            int h);
    public static native int nativeInitial(String cascadePath);
}
