package com.avatar.personate;

import android.content.Context;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;

import com.avatar.personate.scene.DefaultScene;
import com.avatar.personate.scene.PersonateScene;
import com.avatar.personate.Util;

import com.avatar.personate.R;

public class PersonateImpl {
    private final String TAG = "PersonateImpl";

    private Context mContext;
    private Handler mMainHandler;
    private PersonateScene mScene;

    public PersonateImpl(Context context, Handler handler) {
        mContext = context;
        mMainHandler = handler;
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
            mScene.setMainHandler(mMainHandler);
        } else if (mScene.getType() != scene) {
            mScene.stopScene();
            mScene = createScene(scene);
            mScene.setMainHandler(mMainHandler);
        }

        if (mScene == null)
            throw new IllegalArgumentException("Inavlid scene.");

        mScene.startScene();
    }

    public void stopScene() {
        if (mScene != null) {
            mScene.stopScene();
        }
    }

    public void onAsrBegin() {
        if (mScene != null) {
            Message msg = new Message();
            msg.what = PersonateScene.MSG_ASR_BEGIN;
            mScene.addMessage(msg);
        }
    }

    public void onAsrResult(String text) {
        if (mScene != null) {
            Message msg = new Message();
            msg.what = PersonateScene.MSG_ASR_RESULT;
            mScene.addMessage(msg);
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

}
