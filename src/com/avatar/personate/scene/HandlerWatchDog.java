package com.avatar.personate.scene;

import android.os.Handler;

import com.avatar.personate.Util;


public abstract class HandlerWatchDog implements Runnable {
    private final String TAG = "WatchDog";
    private static final long DEFAULT_TIMEOUT = 10000;  // 10s
    private static final long DEFAULT_HEART_RATE = 1000; // 1s

    private Handler mHandler;
    private long mTimeout;
    private long mHeartRate;
    private String mName;
    private long mDuration;

    public HandlerWatchDog(Handler handler) {
        this(handler, DEFAULT_TIMEOUT);
    }

    public HandlerWatchDog(Handler handler, long timeout) {
        this(handler, timeout, DEFAULT_HEART_RATE);
    }

    public HandlerWatchDog(Handler handler, long timeout, long rate) {
        this(handler, timeout, rate, "");
    }

    public HandlerWatchDog(Handler handler, long timeout, long rate, String name) {
        this.mHandler = handler;
        this.mTimeout = timeout;
        this.mHeartRate = rate;
        this.mName = name;
    }

    public abstract void onTimeout();

    public boolean start() {
        if (mHandler.hasCallbacks(this)) {
            Util.Logd(TAG, "Watchdog [" + mName + "] restart");
            return true;
        }

        Util.Logd(TAG, "Watchdog [" + mName + "] start");
        mDuration = 0;
        return mHandler.postDelayed(this, mHeartRate);
    }
    
    public void heartBeat() {
        mDuration = 0;
    }

    public void stop() {
        mHandler.removeCallbacks(this);
        Util.Logd(TAG, "Watchdog [" + mName + "] stoped");
    }

    @Override
    public void run() {
        mDuration += mHeartRate;
        if (mDuration > mTimeout) {
            Util.Logd(TAG, "Watchdog [" + mName + "] timeout!");
            onTimeout();
        } else {
            mHandler.postDelayed(this, mHeartRate);
        }

    }
}
