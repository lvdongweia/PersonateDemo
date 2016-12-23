package com.avatar.personate.scene;


import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.robot.speech.SpeechManager;

import com.avatar.robot.Robot;

public abstract class PersonateScene {
    protected Context mContext;
    protected Handler mHandler;
    private int mSceneType;
    protected SpeechManager mSpeechManager;

    protected boolean mIsWorking;

    public static final int SCENE_DEFAULT = 0;

    public static final int MSG_NLU_EVENT   = 0;
    public static final int MSG_SPEAK_BEGIN = 1;
    public static final int MSG_SPEAK_END   = 2;
    public static final int MSG_RC_EVENT    = 3;
    public static final int MSG_TOUCH_EVENT = 4;
    public static final int MSG_IDLE_CHECK  = 5;
    public static final int MSG_IDLE_ACTION = 6;
    public static final int MSG_HAND_COVER_DETECT = 7;

    protected static final int MSG_START_FACE_TRACK = 5;


    public PersonateScene(Context context, int scene) {
        mContext = context;
        mSceneType = scene;

        HandlerThread handlerThread = new HandlerThread("Personate Scene:" + mSceneType);
        handlerThread.start();
        mHandler = new MyHandler(handlerThread.getLooper());

        mSpeechManager = (SpeechManager)context.getSystemService(Context.SPEECH_SERVICE);
    }

    public abstract void start();
    public abstract void stop();
    public abstract void handleMessageInner(Message msg);

    public int getType() {
        return mSceneType;
    }

    public void addMessage(Message msg) {
        mHandler.sendMessage(msg);
    }

    private class MyHandler extends Handler {

        public MyHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            handleMessageInner(msg);
        }

    }

}
