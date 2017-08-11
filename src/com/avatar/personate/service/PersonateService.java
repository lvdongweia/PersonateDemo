package com.avatar.personate.service;

import android.app.ActivityManager;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.AudioManager.MicArrayEventListener;
import android.os.Binder;
import android.os.IBinder;
import android.robot.motion.RobotMotion;
import android.robot.speech.SpeechManager;
import android.text.TextUtils;

import com.avatar.personate.PersonateActivity;
import com.avatar.personate.Util;

import com.avatar.personate.R;

public class PersonateService extends Service {
    private final String TAG = "PersonateService";

    private static final String ACTION_PLAYER_STOP = "com.avatar.player.stop";

    private AudioManager mAudioManager;
    private SpeechManager mSpeechManager;
    private RobotMotion mRobotMotion;

    private final MicArrayEventListener mListener = new MicArrayEventListener() {
        @Override
        public void onWakeUp(int angle) {
            Util.Logd(TAG, "MicArrayEventListener");
            if (!isRunningForeground(PersonateService.this)) {
                Util.Logd(TAG, "Set PersonateDemo to foreground");

                // stop avatar player if exists
                sendBroadcast(new Intent(ACTION_PLAYER_STOP));

                String iPal = getString(R.string.iPaliPal);
                mSpeechManager.startSpeaking(iPal, true);

                mRobotMotion.turn(angle, 2);

                Intent intent = new Intent(PersonateService.this, PersonateActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        Util.Logd(TAG, "onCreate");

        mAudioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        mAudioManager.setMicArrayEventListener("com.avatar.personate.service", mListener);

        mSpeechManager = (SpeechManager)getSystemService(Context.SPEECH_SERVICE);
        mRobotMotion = new RobotMotion();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Util.Logd(TAG, "onDestroy");
    }

    @Override
    public IBinder onBind(Intent intent) {
        Util.Logd(TAG, "onBind");
        return null;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Util.Logd(TAG, "onUnbind");
        return super.onUnbind(intent);
    }

    private boolean isRunningForeground(Context context) {
        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        ComponentName cn = am.getRunningTasks(1).get(0).topActivity;
        String packageName = cn.getPackageName();
        if (!TextUtils.isEmpty(packageName) &&
                packageName.equals(context.getPackageName())) {
            return true;
        }

        return false;
    }
}
