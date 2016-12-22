package com.avatar.personate;

import android.content.Context;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;

import com.avatar.personate.scene.DefaultScene;
import com.avatar.personate.scene.PersonateScene;
import com.avatar.robot.Robot;
import com.avatar.personate.Util;

import com.avatar.personate.R;

public class PersonateImpl {
    private final String TAG = "PersonateImpl";

    private Context mContext;
    private Handler mMainHandler;
    private MyHandler mEventHandler;
    private PersonateScene mScene;

    public PersonateImpl(Context context, Handler handler) {
        mContext = context;
        mMainHandler = handler;

        HandlerThread handlerThread = new HandlerThread("Personate Handler");
        handlerThread.start();
        mEventHandler = new MyHandler(handlerThread.getLooper());
    }

    private PersonateScene createScene(int scene) {
        switch (scene) {
            case PersonateScene.SCENE_DEFAULT:
                return new DefaultScene(mContext);

            default:
                break;
        }
        return null;
    }

    public void startScene(int scene) {
        if (mScene == null) {
            mScene = createScene(scene);
        } else if (mScene.getType() != scene) {
            mScene.stop();
            mScene = createScene(scene);
        }

        if (mScene == null)
            throw new IllegalArgumentException("Inavlid scene.");

        mScene.start();
    }

    public void stopScene() {
        if (mScene != null) {
            mScene.stop();
        }
    }

    public void onNluResult(int requestId, String text) {
        if (mScene != null) {
            Message msg = new Message();
            msg.what = PersonateScene.MSG_NLU_EVENT;
            msg.arg1 = requestId;
            msg.obj  = text;
            mScene.addMessage(msg);
        }
    }

    public void onTtsBegin(int requestId) {
        if (mScene != null) {
            Message msg = new Message();
            msg.what = PersonateScene.MSG_SPEAK_BEGIN;
            msg.arg1 = requestId;
            mScene.addMessage(msg);
        }
    }

    public void onTtsError(int requestId) {
        if (mScene != null) {
            Message msg = new Message();
            msg.what = PersonateScene.MSG_SPEAK_END;
            msg.arg1 = requestId;
            mScene.addMessage(msg);
        }
    }

    public void onTtsEnd(int requestId) {
        if (mScene != null) {
            Message msg = new Message();
            msg.what = PersonateScene.MSG_SPEAK_END;
            msg.arg1 = requestId;
            mScene.addMessage(msg);
        }
    }

    private class MyHandler extends Handler {
        public MyHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            }
        }
    }


}
