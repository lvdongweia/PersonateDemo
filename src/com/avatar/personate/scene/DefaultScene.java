package com.avatar.personate.scene;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Message;
import android.robot.hw.RobotDevices;
import android.robot.motion.RobotMotion;
import android.robot.motion.RobotMotion.Emoji;
import android.robot.scheduler.RobotConstants;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.avatar.personate.Util;

import com.avatar.personate.R;
import com.avatar.robot.util.SystemMotion;
import com.avatarmind.vision.cover.handcover;
import com.avatarmind.vision.wave.handwave;
import com.avatar.personate.scene.CameraEvent.FaceInformation;

public class DefaultScene extends PersonateScene {
    private final String TAG = "DefaultScene";

    private final int STATE_ACTIVE = 0;
    private final int STATE_IDLE = 1;
    private final long IDLE_TIME = 5 * 60 * 1000; //ms
    private final long COVER_FREQ = 5 * 1000; //ms

    private Set<MoodRes> mMoodSet;
    private String[] mHelloWords;

    private RobotMotion mRobotCtl;
    private AudioManager mAudioManager;
    private CameraEvent mCameraEvent;
    private FaceRecord mFaceRecords;

    private HandlerWatchDog mIdleWatchDog;

    private int mState = STATE_ACTIVE;
    private boolean mIsPersonNearby;
    private long mLastHandEventTime = System.currentTimeMillis();
    private int mTurnSession;
    private int mArmSession;
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
            public void onCompleted(int session_id, int result, int errorcode) {
                if (session_id == mTurnSession && mIsTurning) {
                    mIsTurning = false;
                    Util.Logd(TAG, "Turn Over.");
                    //mHandler.sendEmptyMessage(MSG_SAY_HELLO);
                } else if (session_id == mArmSession) {
                    //mHandler.sendEmptyMessageDelayed(MSG_RESET_ARM_IDLE, 200);
                    mHandler.sendEmptyMessage(MSG_RESET_ARM_IDLE);
                }
            }
        });

        mCameraEvent = CameraEvent.getInstance(context);
        mCameraEvent.setListener(mListener);
        mFaceRecords = new FaceRecord(mContext);

        // 延后检查是否为idle状态
        mIdleWatchDog = new HandlerWatchDog(mHandler, IDLE_TIME, 10*1000, "IDLE STATE") {
            @Override
            public void onTimeout() {
                Util.Logd(TAG, "#######Enter idle state");
                mState = STATE_IDLE;
                //暂停事件检测
                mCameraEvent.setFaceDetect(false);
                mCameraEvent.setHandCover(false);
                mHandler.sendEmptyMessage(MSG_IDLE_ACTION);
            }
        };

        File AppDir = context.getDir("cascade", Context.MODE_PRIVATE);
        String strAppPath = AppDir.getAbsolutePath();
        handwave.nativeInitial(strAppPath);
    }

    @Override
    public void startScene() {
        mIsWorking = true;
        mIsTurning = false;
        mState = STATE_ACTIVE;
        idleAction();
        registerEvent();
        mIdleWatchDog.start();
    }

    @Override
    public void stopScene() {
        mIsWorking = false;
        if (mIdleMotionThread != null) {
            mIdleMotionThread.interrupt();
        }

        unregisterEvent();
        mHandler.removeMessages(MSG_IDLE_ACTION);
        randomActionReset();
        mIdleWatchDog.stop();
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
        public void onFaceRecognize(FaceInformation face) {
            //Util.Logd(TAG, "******onFaceRecognize:" + name);
            mHandler.obtainMessage(MSG_FACE_RECONGIZE, face).sendToTarget();
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
            case MSG_ASR_RESULT:
                // check idle
                if (mState == STATE_IDLE) {
                    mState = STATE_ACTIVE;
                    Util.Logd(TAG, "#######Exit idle state");
                    if (mIdleMotionThread != null) {
                        mIdleMotionThread.interrupt();
                    }

                    // 重启事件检测
                    mCameraEvent.setFaceDetect(true);
                    mCameraEvent.setHandCover(true);

                    mHandler.removeMessages(MSG_IDLE_ACTION);
                    mIdleWatchDog.start();
                } else {
                    mIdleWatchDog.heartBeat();
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
                doExpression(Emoji.LAUGH);
                break;

            case MSG_RC_EVENT:
                doEventMotion(msg.arg1, msg.arg2);
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
                if ((time - mLastHandEventTime) > COVER_FREQ) {
                    mArmSession = mRobotCtl.doAction(SystemMotion.WAVE, 0, 500);

                    FaceRecord.PeopleInfo person = mFaceRecords.getLasted();
                    if (person == null) {
                        String unKnow = mContext.getString(R.string.hi);
                        startSpeaking(unKnow);
                    } else {
                        if ((time - person.mLastUpdate) > 2000) {
                            startSpeaking(mContext.getString(R.string.hi));
                        } else {
                            String know = mContext.getString(R.string.hi) + mFaceRecords.getNameCalls(person.mInfo);
                            startSpeaking(know);
                        }
                    }
                    mLastHandEventTime = time;
                }
                break;
            case MSG_FACE_RECONGIZE:
                mFaceRecords.doFaceResponse((FaceInformation) msg.obj);
                break;

            case MSG_SAY_HELLO:
                int random = (int)(Math.random()*10) % mHelloWords.length;
                String strCover = mHelloWords[random];
                mArmSession = mRobotCtl.doAction(SystemMotion.WAVE, 0, 500);
                startSpeaking(strCover);
                break;

            case MSG_SET_EXPRESSION:
                doExpression(msg.arg1);
                break;

            case MSG_RESET_ARM_IDLE:
                mRobotCtl.doAction(SystemMotion.IDLE, 0, 500);
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
        //intent.addAction(RobotConstants.ACTION_RC_EVENTS);
    }

    private void registerEvent() {
        mAudioManager.setMicArrayEventListener(mContext.getPackageName(), mMicArraryEvent);

        IntentFilter intent = new IntentFilter();
        registerTouchEvent(intent);
        registerRCEvent(intent);
        mContext.registerReceiver(mEventReceiver, intent);

        // start detect camera event
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mCameraEvent.init()) {
                    mCameraEvent.start();
                }
            }
        }, 1000);
    }

    private void unregisterEvent() {
        mAudioManager.removeMicArrayEventListener(mContext.getPackageName());
        mContext.unregisterReceiver(mEventReceiver);

        mCameraEvent.stop();
        mCameraEvent.uninit();
    }
    
    private class MoodRes {
        public int mEmoji;
        public short[] mMotions;
        public String[] mMoods;
        
        public MoodRes(int emoji, short[] motions, String[] moods) {
            this.mEmoji = emoji;
            this.mMotions = motions;
            this.mMoods = moods;
        }
    }

    private void loadMoodResource() {
        if (mMoodSet == null)
            mMoodSet = new HashSet<MoodRes>();
        else
            mMoodSet.clear();
        
        String[] md_smile = mContext.getString(R.string.mood_smile).split("\\|");
        String[] md_sad = mContext.getString(R.string.mood_sad).split("\\|");
        String[] md_surprise = mContext.getString(R.string.mood_surprise).split("\\|");
        String[] md_shy = mContext.getString(R.string.mood_shy).split("\\|");
        String[] md_cover_smile = mContext.getString(R.string.mood_cover_smile).split("\\|");
        String[] md_grimace = mContext.getString(R.string.mood_grimace).split("\\|");
        String[] md_naughty = mContext.getString(R.string.mood_naughty).split("\\|");
        String[] md_think = mContext.getString(R.string.mood_think).split("\\|");

        mMoodSet.add(new MoodRes(Emoji.SMILE,
                new short[]{SystemMotion.LAUGH,SystemMotion.KISS}, md_smile));
        mMoodSet.add(new MoodRes(Emoji.SAD,
                new short[]{SystemMotion.AKIMBO,SystemMotion.NO}, md_sad));
        mMoodSet.add(new MoodRes(Emoji.SURPRISE,
                new short[]{SystemMotion.HUG}, md_surprise));
        mMoodSet.add(new MoodRes(Emoji.SHY,
                new short[]{SystemMotion.HANDSBACK,SystemMotion.SHY}, md_shy));
        mMoodSet.add(new MoodRes(Emoji.COVER_SMILE,
                new short[]{SystemMotion.KISS,SystemMotion.YES,SystemMotion.WE}, md_cover_smile));
        mMoodSet.add(new MoodRes(Emoji.GRIMACE,
                new short[]{SystemMotion.WORRY}, md_grimace));
        mMoodSet.add(new MoodRes(Emoji.NAUGHTY,
                new short[]{SystemMotion.SHY, SystemMotion.CLAP}, md_naughty));
        mMoodSet.add(new MoodRes(Emoji.THINKING,
                new short[]{SystemMotion.SHY}, md_think));

        mHelloWords = mContext.getString(R.string.hello).split("\\|");
    }

    private final BroadcastReceiver mEventReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String strAction = intent.getAction();
            Util.Logd(TAG, "onReceive:" + intent.getAction());

            /*if (strAction.equals(RobotConstants.ACTION_RC_EVENTS)) {
                int type = intent.getIntExtra("event_type", 0);
                int pos = intent.getIntExtra("position", -1);
                mHandler.obtainMessage(MSG_RC_EVENT, type, -pos).sendToTarget();
            } else*/ if (strAction.equals(RobotConstants.AI_ACTION_LEFT_OXTER_TOUCHED)) {
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
        int expression = -1;
        short motion = -1;

        Iterator<MoodRes> iter = mMoodSet.iterator();
        while (iter.hasNext()) {
            MoodRes moodRes = iter.next();
            for (String mood : moodRes.mMoods) {
                if (text.contains(mood)) {
                    expression = moodRes.mEmoji;
                    int rand = (int)(10 * Math.random());
                    rand %= moodRes.mMotions.length;
                    motion = moodRes.mMotions[rand];
                    Util.Logd(TAG, "Emo:" + expression + ",Mo:" + motion);
                    mRobotCtl.doAction(motion, 1, 3000);
                    break;
                }
            }

            if (expression != -1) {
                mExpression_id = expression;
                break;
            }
        }
    }

    private void doExpression(int requestId) {
        //Util.Logd(TAG, "Set expression:" + requestId);
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
            Util.Logd(TAG, "##Mic Ori:" + angle + "  Head Pos:" + headPos + "##");

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
                // after turn over (mTurnSession is false), send MSG_SAY_HELLO;
            } else {
                // if not need turn, send MSG_SAY_HELLO directelly
                mHandler.sendEmptyMessage(MSG_SAY_HELLO);
            }

            mCameraEvent.setNeckRotateAngle(headPos);
            Util.Logd(TAG, "##New HeadPos:" + headPos + "  WheelTurn:" + turnAngle + "##");

            boolean isSuc = mAudioManager.setMicArrayOrientation(AudioManager.MIC_ARRAY_ORI_0_360);
            if (!isSuc) {
                Util.Logd(TAG, "setMicArrayOrientation 0 fail");
            }

            if (mState == STATE_IDLE) {
                mState = STATE_ACTIVE;
                Util.Logd(TAG, "#######Exit idle state");
                if (mIdleMotionThread != null) {
                    mIdleMotionThread.interrupt();
                }

                // 重启事件检测
                mCameraEvent.setFaceDetect(true);
                mCameraEvent.setHandCover(true);

                mHandler.removeMessages(MSG_IDLE_ACTION);
                mIdleWatchDog.start();
            } else {
                mIdleWatchDog.heartBeat();
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

            idleAction();
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

    private void idleAction() {
        mRobotCtl.reset(RobotDevices.Units.NECT_MOTORS);
        mRobotCtl.doAction(SystemMotion.IDLE, 0, 500);
    }

    private void randomActionReset() {
        mRobotCtl.reset(RobotDevices.Units.ALL_MOTORS);
    }

    public class FaceRecord {
        private final int MAX_RECORD = 5;
        private List<PeopleInfo> mItems;
        private PeopleInfo mLastedPerson;
        private final String mLanguage;
        private final String brother;
        private final String sister;
        private final String uncle;
        private final String aunt;
        private final String good_am;
        private final String good_pm;
        private final String meet_again;
        private final String children;

        public class PeopleInfo {
            public FaceInformation mInfo;
            public long mLastUpdate;

            public PeopleInfo(FaceInformation info) {
                mInfo = info;
                mLastUpdate = System.currentTimeMillis();
            }
        }

        public FaceRecord(Context context) {
            mItems = new ArrayList<PeopleInfo>();
            mLanguage = context.getResources().getConfiguration().locale.getLanguage();
            brother = context.getString(R.string.brother);
            sister = context.getString(R.string.sister);
            uncle = context.getString(R.string.uncle);
            aunt = context.getString(R.string.aunt);
            good_am = context.getString(R.string.good_am);
            good_pm = context.getString(R.string.good_pm);
            meet_again = context.getString(R.string.meet_again);
            children = context.getString(R.string.hi_children);
        }

        public synchronized PeopleInfo getLasted() {
            return mLastedPerson;
        }

        private synchronized void doFaceResponse(FaceInformation person) {
            boolean bExist = false;
            int oldIndex = 0;
            PeopleInfo tmp = null;

            Util.Logd(TAG, "Name:" + person.mPerson +
                    " Gender:" + person.mGender + " Age:" + person.mAge);

            for (int i = 0; i < mItems.size(); i++) {
                tmp = mItems.get(i);
                if (tmp.mInfo.mPerson.equals(person.mPerson)) {
                    oldIndex = i;
                    bExist = true;
                    break;
                }
                // get the oldest record
                oldIndex = tmp.mLastUpdate > mItems.get(oldIndex).mLastUpdate ? oldIndex : i;
            }

            long time = System.currentTimeMillis();
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(time);
            int apm = calendar.get(Calendar.AM_PM);
            if (bExist) {
                tmp = mItems.get(oldIndex);
                long timeDiff = time - tmp.mLastUpdate;
                if (timeDiff > 30 * 1000) {
                    startSpeaking(getNameCalls(person) + "," + meet_again);
                }
                tmp.mLastUpdate = time;
            } else {
                tmp = new PeopleInfo(person);
                if(mItems.size() >= MAX_RECORD)
                    mItems.set(oldIndex, tmp);
                else
                    mItems.add(tmp);

                String name = "";
                if (apm == Calendar.AM) {
                    name = good_am + "," + getNameCalls(person);
                } else if (apm == Calendar.PM) {
                    name = good_pm + "," + getNameCalls(person);
                }
                startSpeaking(name);
            }
            mLastedPerson = tmp;
        }

        public String getNameCalls(FaceInformation face) {
            String call = "";
            if (face.mPerson.equals("unknown")) {
                if (face.mGender == FaceInformation.Male) {
                    if (face.mAge >= 40) {
                        call = uncle;
                    } else if (face.mAge > 8) {
                        call = brother;
                    } else {
                        call = children;
                    }
                } else if (face.mGender == FaceInformation.FeMale) {
                    if (face.mAge >= 40) {
                        call = aunt;
                    } else if (face.mAge > 8) {
                        call = sister;
                    } else {
                        call = children;
                    }
                } else {
                    call = "";
                }
            } else {
                call = face.mPerson;
            }
            return call;
        }
    }
}
