package com.avatar.personate.scene;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.FaceDetector;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.robot.motion.RobotMotion;
import android.robot.scheduler.RobotConstants;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.avatar.personate.ThinkView;
import com.avatar.personate.Util;
import com.avatar.robot.Robot;

import com.avatar.personate.R;
import com.avatar.robot.util.SystemMotion;
import com.avatarmind.vision.cover.handcover;
import com.avatarmind.vision.wave.handwave;


public class DefaultScene extends PersonateScene {
    private final String TAG = "DefaultScene";

    private final int STATE_ACTIVE = 0;
    private final int STATE_IDLE = 1;
    private final long IDLE_TIME = 5 * 60 * 1000; //ms
    private final long COVER_FREQ = 6 * 1000; //ms

    private Map<Integer, String[]> mMoodMap;

    private RobotMotion mRobotCtl;
    private AudioManager mAudioManager;
    private CameraEvent mCameraEvent;

    private int mState = STATE_ACTIVE;
    private boolean mIsPersonNearby;
    private long mLastActiveTime = System.currentTimeMillis();
    private long mLastHandEventTime = System.currentTimeMillis();
    private long mLastPeopleTime = System.currentTimeMillis();
    private String mPeopleName;
    private int mTurnSession;
    private boolean mIsTurning;
    private int mExpression_id = -1;


    public DefaultScene(Context context) {
        super(context, SCENE_DEFAULT);

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                loadMoodResource();
            }
        });

        mAudioManager = new AudioManager(context);
        mRobotCtl = new RobotMotion();
        mRobotCtl.setListener(new RobotMotion.Listener() {

            @Override
            public void onStatusChanged(int status) {
                Util.Logd(TAG, "onStatusChanged");
            }

            @Override
            public void onCompleted(int session_id, int result) {
                if (mIsTurning && session_id == mTurnSession) {
                    mIsTurning = false;
                    Util.Logd(TAG, "Turn Over.");
                    mHandler.sendEmptyMessage(MSG_SAY_HELLO);
                }
            }
        });

        mCameraEvent = CameraEvent.getInstance(context);
        mCameraEvent.setListener(mListener);

        // 延后检查是否为idle状态
        mHandler.sendEmptyMessageDelayed(MSG_IDLE_CHECK, IDLE_TIME);

        File AppDir = context.getDir("cascade", Context.MODE_PRIVATE);
        String strAppPath = AppDir.getAbsolutePath();
        handwave.nativeInitial(strAppPath);
    }

    @Override
    public void startScene() {
        mIsWorking = true;
        mIsTurning = false;
        mState = STATE_ACTIVE;
        randomActionReset();
        registerEvent();
    }

    @Override
    public void stopScene() {
        mIsWorking = false;
        if (mIdleMotionThread != null) {
            mIdleMotionThread.interrupt();
        }

        unregisterEvent();
        mHandler.removeMessages(MSG_IDLE_ACTION);
        mHandler.removeMessages(MSG_IDLE_CHECK);
        randomActionReset();
    }

    private final CameraEvent.CameraEventListener mListener = new CameraEvent.CameraEventListener() {

        @Override
        public void onHandWave() {
            //Util.Logd(TAG, "******onHandWave");
            mHandler.obtainMessage(MSG_HAND_WAVE_DETECT).sendToTarget();
        }

        @Override
        public void onHandCover() {
            //Util.Logd(TAG, "******onHandCover");
            mHandler.obtainMessage(MSG_HAND_COVER_DETECT).sendToTarget();
        }

        @Override
        public void onFaceRecognize(String name) {
            //Util.Logd(TAG, "******onFaceRecognize:" + name);
            mHandler.obtainMessage(MSG_FACE_RECONGIZE, name).sendToTarget();
        }
    };

    @Override
    public void handleMessageInner(Message msg) {
        if (!mIsWorking) return;

        long time;
        switch (msg.what) {
            case MSG_ASR_BEGIN:
                if (mExpression_id != -1) {
                    mHandler.sendMessageDelayed(
                            mHandler.obtainMessage(MSG_SET_EXPRESSION, mExpression_id, -1), 100);
                }

                break;

            case MSG_NLU_EVENT:
                doNluResponse(msg.arg1, (String) msg.obj);
                break;

            case MSG_SPEAK_BEGIN:
                //doSpeakExpression(msg.arg1);
                break;

            case MSG_SPEAK_END:
                doEndExpression(msg.arg1);
                break;

            case MSG_TOUCH_EVENT:
                doExpression(Robot.EMOTION_LAUGH);
                break;

            case MSG_RC_EVENT:
                doEventMotion(msg.arg1, msg.arg2);
                break;

            case MSG_IDLE_CHECK:
                long diff = System.currentTimeMillis() - mLastActiveTime;
                if (diff >= IDLE_TIME && mState != STATE_IDLE) {
                    Util.Logd(TAG, "#######Enter idle state");
                    mState = STATE_IDLE;
                    //暂停事件检测
                    mCameraEvent.setFaceDetect(false);
                    mCameraEvent.setHandCover(false);
                    mHandler.sendEmptyMessage(MSG_IDLE_ACTION);
                }
                // 延后检查是否为idle状态
                mHandler.sendEmptyMessageDelayed(MSG_IDLE_CHECK, IDLE_TIME);
                break;

            case MSG_IDLE_ACTION:
                if (mState == STATE_IDLE && mIdleMotionThread == null) {
                    mIdleMotionThread = new Thread(mIdleRunnable);
                    mIdleMotionThread.start();
                }
                break;

            case MSG_HAND_COVER_DETECT:
                if (mIsTurning) return;

                mHandler.removeMessages(MSG_HAND_COVER_DETECT);
                time = System.currentTimeMillis();
                if ((time - mLastHandEventTime) > COVER_FREQ) {
                    String strCover = mContext.getString(R.string.cover_eye);
                    startSpeaking(strCover);
                    mLastHandEventTime = time;
                }
                break;
            case MSG_HAND_WAVE_DETECT:
                if (mIsTurning) return;

                mHandler.removeMessages(MSG_HAND_WAVE_DETECT);
                time = System.currentTimeMillis();
                String dontKnow = mContext.getString(R.string.noknow);
                if ((time - mLastHandEventTime) > COVER_FREQ) {
                    mRobotCtl.doAction(SystemMotion.WAVE, 0, 500);
                    if (mPeopleName == null || (time - mLastPeopleTime) > 2 * COVER_FREQ) {
                        startSpeaking(dontKnow);
                        mPeopleName = null;
                    } else {
                        String know = mContext.getString(R.string.hi) + mPeopleName;
                        startSpeaking(know);
                    }
                    mLastHandEventTime = time;
                }
                break;
            case MSG_FACE_RECONGIZE:
                mLastPeopleTime = System.currentTimeMillis();
                mPeopleName = (String)msg.obj;
                break;

            case MSG_SAY_HELLO:
                String strCover = mContext.getString(R.string.hello);
                mRobotCtl.doAction(SystemMotion.WAVE, 0, 500);
                startSpeaking(strCover);
                break;

            case MSG_SET_EXPRESSION:
                doExpression(msg.arg1);
                break;

            default:
                break;
        }
    }


    private void registerTouchEvent(IntentFilter intent) {
        intent.addAction(RobotConstants.AI_ACTION_LEFT_OXTER_TOUCHED);
        intent.addAction(RobotConstants.AI_ACTION_RIGHT_OXTER_TOUCHED);
        intent.addAction(RobotConstants.AI_ACTION_LEFT_SHOULDER_TOUCHED);
        intent.addAction(RobotConstants.AI_ACTION_RIGHT_SHOULDER_TOUCHED);
    }

    private void registerRCEvent(IntentFilter intent) {
        intent.addAction(RobotConstants.ACTION_RC_EVENTS);
    }

    private void registerEvent() {
        mAudioManager.setMicArrayEventListener(mContext.getPackageName(), mMicArraryEvent);
        IntentFilter intent = new IntentFilter();

        registerTouchEvent(intent);
        registerRCEvent(intent);

        mContext.registerReceiver(mEventReceiver, intent);

        // start detect camera event
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (mCameraEvent.init()) {
                    try {
                        Thread.sleep(1000);
                        mCameraEvent.start();
                    } catch (InterruptedException e) {
                        // ignore
                    }
                }
            }
        }).start();
    }

    private void unregisterEvent() {
        mAudioManager.removeMicArrayEventListener(mContext.getPackageName());
        mContext.unregisterReceiver(mEventReceiver);

        mCameraEvent.stop();
        mCameraEvent.uninit();
    }

    private void loadMoodResource() {
        if (mMoodMap == null)
            mMoodMap = new HashMap<Integer, String[]>();
        else
            mMoodMap.clear();

        String[] md_smile = mContext.getString(R.string.mood_smile).split("\\|");
        String[] md_sad = mContext.getString(R.string.mood_sad).split("\\|");
        String[] md_surprise = mContext.getString(R.string.mood_surprise).split("\\|");
        String[] md_shy = mContext.getString(R.string.mood_shy).split("\\|");
        String[] md_cover_smile = mContext.getString(R.string.mood_cover_smile).split("\\|");
        String[] md_grimace = mContext.getString(R.string.mood_grimace).split("\\|");
        String[] md_naughty = mContext.getString(R.string.mood_naughty).split("\\|");
        String[] md_think = mContext.getString(R.string.mood_think).split("\\|");

        mMoodMap.put(RobotMotion.Emoji.SMILE, md_smile);
        mMoodMap.put(RobotMotion.Emoji.SAD, md_sad);
        mMoodMap.put(RobotMotion.Emoji.SURPRISE, md_surprise);
        mMoodMap.put(RobotMotion.Emoji.SHY, md_shy);
        mMoodMap.put(RobotMotion.Emoji.COVER_SMILE, md_cover_smile);
        mMoodMap.put(RobotMotion.Emoji.GRIMACE, md_grimace);
        mMoodMap.put(RobotMotion.Emoji.NAUGHTY, md_naughty);
        mMoodMap.put(RobotMotion.Emoji.THINKING, md_think);
    }

    private final BroadcastReceiver mEventReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String strAction = intent.getAction();
            Util.Logd(TAG, "onReceive:" + intent.getAction());

            if (strAction.equals(RobotConstants.ACTION_RC_EVENTS)) {
                int type = intent.getIntExtra("event_type", 0);
                int pos = intent.getIntExtra("position", -1);
                mHandler.obtainMessage(MSG_RC_EVENT, type, -pos).sendToTarget();
            } else if (strAction.equals(RobotConstants.AI_ACTION_LEFT_OXTER_TOUCHED)) {
                mHandler.obtainMessage(MSG_TOUCH_EVENT, RobotConstants.RF_LEFT_OXTER_TOUCH).sendToTarget();
            } else if (strAction.equals(RobotConstants.AI_ACTION_RIGHT_OXTER_TOUCHED)) {
                mHandler.obtainMessage(MSG_TOUCH_EVENT, RobotConstants.RF_RIGHT_OXTER_TOUCH).sendToTarget();
            } else if (strAction.equals(RobotConstants.AI_ACTION_LEFT_SHOULDER_TOUCHED)) {
                mHandler.obtainMessage(MSG_TOUCH_EVENT, RobotConstants.RF_LEFT_SHOULDER_TOUCH).sendToTarget();
            } else if (strAction.equals(RobotConstants.AI_ACTION_RIGHT_SHOULDER_TOUCHED)) {
                mHandler.obtainMessage(MSG_TOUCH_EVENT, RobotConstants.RF_RIGHT_SHOULDER_TOUCH).sendToTarget();
            }
        }
    };

    private void doEventMotion(int type, int pos) {
        switch (type) {
            case RobotConstants.RC_FRONT_UPPER_OBSTACLE:
                break;
            case RobotConstants.RC_FRONT_LOWER_OBSTACLE:
                break;
            case RobotConstants.RC_BACK_UPPER_OBSTACLE:
                break;
            case RobotConstants.RC_BACK_LOWER_OBSTACLE:
                break;
            case RobotConstants.RC_LEFT_OBSTACLE:
                break;
            case RobotConstants.RC_RIGHT_OBSTACLE:
                break;
            case RobotConstants.RC_FORWARD_FALL:
                break;
            case RobotConstants.RC_BACKWARD_FALL:
                break;
            case RobotConstants.RC_FRONT_COLLISION:
                break;
            case RobotConstants.RC_ENVIRONMENT_OBSTACLE:
                break;

            case RobotConstants.RC_APPROACH_SLOW:
            case RobotConstants.RC_APPROACH_FAST:
                if (pos == RobotConstants.EVENT_FRONT && !mIsPersonNearby) {
                    mIsPersonNearby = true;
                }
                break;

            case RobotConstants.RC_GO_AWAY:
                if (pos == RobotConstants.EVENT_FRONT && mIsPersonNearby) {
                    mIsPersonNearby = false;
                }
                break;

            case RobotConstants.RC_TOO_CLOSE:
                break;
            case RobotConstants.RC_FRONT_COLLISION_RELEASE:
                break;
        }
    }


    private void doNluResponse(int requestId, String text) {
        mExpression_id = -1;

        Iterator iter = mMoodMap.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            String[] strMood = (String[]) entry.getValue();
            for (String mood : strMood) {
                if (text.contains(mood)) {
                    mExpression_id = (Integer) entry.getKey();
                    break;
                }
            }

            if (mExpression_id != -1)
                break;
        }
    }

    private void doExpression(int requestId) {
        Util.Logd(TAG, "Set expression:" + requestId);
        mRobotCtl.emoji(requestId);
    }

    private void doEndExpression(int requestId) {
        mRobotCtl.emoji(RobotMotion.Emoji.DEFAULT);
    }

    private final AudioManager.MicArrayEventListener mMicArraryEvent = new AudioManager.MicArrayEventListener() {
        @Override
        public void onWakeUp(int angle) {
            if (mIsTurning) return;

            int headPos = mCameraEvent.getNeckRotateAngle();
            Util.Logd(TAG, "##Mic Ori:" + angle + "  Head:" + headPos + "##");

            int turnAngle = 0;
            if (angle <= 180) {
                turnAngle = angle - headPos;
            }
            else {
                // angle <= 360
                turnAngle = -((360 - angle) + headPos);
            }

            if (CameraEvent.HEAD_LEFT_MAX <= turnAngle && turnAngle <= CameraEvent.HEAD_RIGHT_MAX) {
                headPos = -turnAngle;
                turnAngle = 0;
            } else {
                headPos = 0;
            }

            if (turnAngle != 0) {
                mIsTurning = true;
                mTurnSession = mRobotCtl.turn(turnAngle, 2);
            }
            mCameraEvent.setNeckRotateAngle(headPos);
            Util.Logd(TAG, "##HeadPos:" + headPos + "  WheelPos:" + turnAngle + "##");

            boolean isSuc = mAudioManager.setMicArrayOrientation(AudioManager.MIC_ARRAY_ORI_0_360);
            if (!isSuc) {
                Util.Logd(TAG, "setMicArrayOrientation 0 fail");
            }

            mLastActiveTime = System.currentTimeMillis();
            if (mState == STATE_IDLE) {
                mState = STATE_ACTIVE;
                Util.Logd(TAG, "#######Exit idle state");
                if (mIdleMotionThread != null) {
                    mIdleMotionThread.interrupt();
                }

                // 重启事件检测
                mCameraEvent.setFaceDetect(true);
                mCameraEvent.setHandCover(true);
            }
        }
    };

    private Thread mIdleMotionThread;
    private final Runnable mIdleRunnable = new Runnable() {
        @Override
        public void run() {
            if (mState != STATE_IDLE) return;

            int emojiId, index = 0;
            emojiId = (int) (Math.random() * 100) % 27; // 26个表情中随机选取
            mRobotCtl.emoji(emojiId);

            index = (int) (Math.random() * 10) % 5;
            if (index == 0) {
                mRobotCtl.doAction(SystemMotion.IDLE, 1, 5000);
                sleep(10000);
            } else if (index == 1) {
                mRobotCtl.doAction(SystemMotion.CHAT_HEAD_ARMS_CURVED, 1, 5000);
                sleep(10000);
            } else if (index == 2) {
                mRobotCtl.doAction(SystemMotion.CHAT_HEAD_ARMS, 1, 5000);
                sleep(10000);
            } else if (index == 3) {
                mRobotCtl.doAction(SystemMotion.CHAT_RIGHT_ARM, 1, 5000);
                sleep(10000);
            } else if (index == 4) {
                mRobotCtl.doAction(SystemMotion.CHAT_LEFT_ARM, 1, 5000);
                sleep(10000);
            }

            randomActionReset();
            mIdleMotionThread = null;

            // 延后5秒做下组动作
            if (isIdle() && mIsWorking) {
                Util.Logd(TAG, "send message : MSG_IDLE_ACTION");
                mHandler.sendEmptyMessageDelayed(MSG_IDLE_ACTION, 5000);
            }
        }
    };

    private boolean isIdle() {
        return mState == STATE_IDLE;
    }

    private void sleep(long millis) {
        try {
            if (mIdleMotionThread != null)
                mIdleMotionThread.currentThread().sleep(millis);
        } catch (InterruptedException e) {
            Util.Logd(TAG, "Thread interrupt!");
        }
    }

    private void randomActionReset() {
        mRobotCtl.reset(RobotMotion.Units.ALL);
    }

}
