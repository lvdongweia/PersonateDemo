package com.avatarmind.vision.cover;

public class handcover {
    static {
        System.loadLibrary("handcover");
    }
    public handcover() {
    }
    public static native String detectcover(byte[] inputImagebuffer, int w,
            int h);
}
