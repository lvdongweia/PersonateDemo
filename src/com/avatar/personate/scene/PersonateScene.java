package com.avatar.personate.scene;


import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.robot.speech.SpeechManager;

import com.avatar.personate.PersonateActivity;
import com.avatar.robot.Robot;

public abstract class PersonateScene {
    protected Context mContext;
    protected Handler mHandler;
    protected Handler mMainHandler;

    private int mSceneType;
    protected boolean mIsWorking;

    public static final int SCENE_DEFAULT = 0;

    public static final int MSG_ASR_BEGIN   = 12;
    public static final int MSG_ASR_RESULT  = 13;
    public static final int MSG_NLU_EVENT   = 0;
    public static final int MSG_SPEAK_BEGIN = 1;
    public static final int MSG_SPEAK_END   = 2;
    public final int MSG_RC_EVENT    = 3;
    public final int MSG_TOUCH_EVENT = 4;
    public final int MSG_IDLE_CHECK  = 5;
    public final int MSG_IDLE_ACTION = 6;
    public final int MSG_HAND_COVER_DETECT = 7;
    public final int MSG_HAND_WAVE_DETECT = 8;
    public final int MSG_FACE_RECONGIZE = 9;
    public final int MSG_SAY_HELLO = 10;
    public final int MSG_SET_EXPRESSION = 11;

    public PersonateScene(Context context, int scene) {
        mContext = context;
        mSceneType = scene;

        HandlerThread handlerThread = new HandlerThread("Personate Scene:" + mSceneType);
        handlerThread.start();
        mHandler = new MyHandler(handlerThread.getLooper());
    }

    public abstract void startScene();
    public abstract void stopScene();
    public abstract void handleMessageInner(Message msg);

    public int getType() {
        return mSceneType;
    }

    public void addMessage(Message msg) {
        mHandler.sendMessage(msg);
    }

    public void setMainHandler(Handler handler) {
        mMainHandler = handler;
    }

    protected void startSpeaking(String text) {
        mMainHandler.obtainMessage(PersonateActivity.MSG_ADD_SPEAKING, text).sendToTarget();
    }

    protected void startUnderstanding(String text) {
        mMainHandler.obtainMessage(PersonateActivity.MSG_ADD_UNDERTAND, text).sendToTarget();
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
