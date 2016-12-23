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


public class DefaultScene extends PersonateScene {
    private final String TAG = "DefaultScene";
    private Map<Integer, String[]> mMoodMap;
    private FaceTrack mFaceTrack;
    private RobotMotion mRobotCtl;
    private AudioManager mAudioManager;
    private boolean mIsPersonNearby;

    private final int STATE_ACTIVE = 0;
    private final int STATE_IDLE = 1;
    private final long IDLE_TIME = 5 * 60 * 1000; //ms
    private final long COVER_FREQ = 2 * 1000; //ms

    private int mHandCoverCount;
    private volatile int mState = STATE_ACTIVE;
    private long mLastActiveTime = System.currentTimeMillis();
    private long mLastHandCoverDetect = System.currentTimeMillis();

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
                //Util.Logd(TAG, "onCompleted");
            }
        });

        //start face track
        mFaceTrack = new FaceTrack();

        // 延后检查是否为idle状态
        mHandler.sendEmptyMessageDelayed(MSG_IDLE_CHECK, IDLE_TIME);
    }

    @Override
    public void start() {
        mIsWorking = true;
        mState = STATE_ACTIVE;
        registerEvent();

        randomActionReset();
        startFaceTrack();
    }

    @Override
    public void stop() {
        mIsWorking = false;
        if (mIdleMotionThread != null) {
            mIdleMotionThread.interrupt();
        }

        unregisterEvent();
        mHandler.removeMessages(MSG_IDLE_ACTION);
        mHandler.removeMessages(MSG_IDLE_CHECK);

        // stop to prevent not get leave event
        stopFaceTrack();
    }

    @Override
    public void handleMessageInner(Message msg) {
        if (!mIsWorking) return;

        Util.Logd(TAG, "Message:" + msg.what);
        switch (msg.what) {
            case MSG_NLU_EVENT:
                mLastActiveTime = System.currentTimeMillis();
                if (mState == STATE_IDLE) {
                    mState = STATE_ACTIVE;
                    Util.Logd(TAG, "#######Exit idle state");
                    if (mIdleMotionThread != null) {
                        mIdleMotionThread.interrupt();
                    }

                    // 重新启动人脸检测
                    mFaceTrack.resumeFaceDetect();

                    // 延后检查是否为idle状态
                    mHandler.sendEmptyMessageDelayed(MSG_IDLE_CHECK, IDLE_TIME);
                }

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
                if (diff > IDLE_TIME) {
                    mState = STATE_IDLE;
                    Util.Logd(TAG, "#######Enter idle state");

                    mFaceTrack.pauseFaceDetect();
                    mHandler.sendEmptyMessage(MSG_IDLE_ACTION);
                } else {
                    // 延后检查是否为idle状态
                    mHandler.sendEmptyMessageDelayed(MSG_IDLE_CHECK, IDLE_TIME - diff);
                }
                break;

            case MSG_IDLE_ACTION:
                if (mState == STATE_IDLE && mIdleMotionThread == null) {
                    mIdleMotionThread = new Thread(mIdleRunnable);
                    mIdleMotionThread.start();
                }
                break;

            case MSG_HAND_COVER_DETECT:
                // hand cover detect
                String hand = handcover.detectcover((byte[])msg.obj, FaceTrack.WIDTH, FaceTrack.HEIGHT);
                if (hand.equals("COVER")) {
                    Util.Logd(TAG, "Hand cover:" + hand);
                    long coverTime = System.currentTimeMillis();
                    if (mHandCoverCount == 0) {
                        mLastHandCoverDetect = coverTime;
                    }

                    if ((coverTime - mLastHandCoverDetect) <= COVER_FREQ) {
                        mHandCoverCount++;
                    } else {
                        mHandCoverCount = 0;
                    }

                    if (mHandCoverCount > 4) {
                        mSpeechManager.startUnderstanding("你好");
                        mRobotCtl.doAction(SystemMotion.WAVE, 1, 3000);
                        mHandCoverCount = 0;
                    }
                }

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
    }

    private void unregisterEvent() {
        mAudioManager.removeMicArrayEventListener(mContext.getPackageName());
        mContext.unregisterReceiver(mEventReceiver);
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
                    mSpeechManager.startUnderstanding(mContext.getString(R.string.hello));
                    //startFaceTrack();
                }
                break;

            case RobotConstants.RC_GO_AWAY:
                if (pos == RobotConstants.EVENT_FRONT && mIsPersonNearby) {
                    //stopFaceTrack();
                    mSpeechManager.startUnderstanding(mContext.getString(R.string.bye));
                    mIsPersonNearby = false;
                }
                break;

            case RobotConstants.RC_TOO_CLOSE:
                break;
            case RobotConstants.RC_FRONT_COLLISION_RELEASE:
                break;
        }
    }

    private void startFaceTrack() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (mFaceTrack.init())
                    mFaceTrack.start();
            }
        }).start();
    }

    private void stopFaceTrack() {
        mFaceTrack.stop();
        mFaceTrack.uninit();
    }

    private void doNluResponse(int requestId, String text) {
        int expression_id = -1;

        Iterator iter = mMoodMap.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            String[] strMood = (String[]) entry.getValue();
            for (String mood : strMood) {
                if (text.contains(mood)) {
                    expression_id = (Integer) entry.getKey();
                    break;
                }
            }

            if (expression_id != -1)
                break;
        }

        if (expression_id == -1)
            expression_id = RobotMotion.Emoji.TALK;

        Util.Logd(TAG, "Text:" + text + " exp:" + expression_id);
        doExpression(expression_id);
    }

    private void doExpression(int requestId) {
        mRobotCtl.emoji(requestId);
    }

    private void doEndExpression(int requestId) {
        mRobotCtl.emoji(RobotMotion.Emoji.DEFAULT);
    }

    private final AudioManager.MicArrayEventListener mMicArraryEvent = new AudioManager.MicArrayEventListener() {
        @Override
        public void onWakeUp(int angle) {
            int headPos = mFaceTrack.getNowRotateAngle();

            mFaceTrack.setNowRotateAngle(0);
            int turnAngle = 0;
            if (angle <= 180) {
                turnAngle = angle - headPos;
                mRobotCtl.turn(turnAngle, 2);
            }
            else if (angle < 360) {
                turnAngle = -((360 - angle) + headPos);
                mRobotCtl.turn(turnAngle, 2);
            }

            Util.Logd(TAG, "onWakeUp:" + angle + " headPos: " + headPos + "  Real Rotate:" + turnAngle);

            boolean isSuc = mAudioManager.setMicArrayOrientation(AudioManager.MIC_ARRAY_ORI_0_360);
            if (!isSuc) {
                Util.Logd(TAG, "setMicArrayOrientation 0 fail");
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

    /**
     *  Face Tracker
     */
    private static final int MSG_FACE_DECODE = 0;

    private class FaceTrack implements Camera.PreviewCallback {

        public static final int WIDTH = 640;
        public static final int HEIGHT = 480;
        private final int MAX_FACES = 2;
        private final int OR_NEGT = -1;
        private final int OR_POST = 1;


        private byte[] mCallbackBuf;
        private Handler mFaceHandler;
        private Camera mCamera;
        private SurfaceTexture mSurfaceTexture;
        private FaceDetector mFaceDetector;
        private RobotMotion mFaceCtl;

        private boolean mIsTracking;
        private boolean mIsPause;
        private double mRotateParam;
        private double mTiltParam;

        private volatile boolean mIsIdle;
        private int mSessionId;
        private int mNowRotateAngle = 0;
        private int mNowTiltAngle = 0;

        public FaceTrack() {
            mFaceCtl = new RobotMotion();
            mFaceCtl.setListener(mListener);

            getFocusDistance();
            mFaceDetector = new FaceDetector(WIDTH, HEIGHT, MAX_FACES);

            // reset head position
            setNowRotateAngle(0);
        }

        private final RobotMotion.Listener mListener = new RobotMotion.Listener() {

            @Override
            public void onStatusChanged(int status) {

            }

            @Override
            public void onCompleted(int session_id, int result) {
                //Util.Logd(TAG, "onCompleted:" + session_id);
                if (mSessionId == session_id) {
                    mIsIdle = true;
                }
            }
        };

        public int getNowRotateAngle() {
            return mNowRotateAngle;
        }

        public void setNowRotateAngle(int angle) {
            mIsIdle = false;
            mNowRotateAngle = angle;
            mSessionId = mFaceCtl.runMotor(RobotMotion.Motors.NECK_ROTATION, mNowRotateAngle, 500, 0);
        }

        public boolean init() {
            try {
                mCamera = Camera.open(0);
                if (mCamera == null) {
                    Util.Logd(TAG, "Open camera(0) failed.");
                    return false;
                }

                mSurfaceTexture = new SurfaceTexture(10);
                mCamera.setPreviewTexture(mSurfaceTexture);

                Camera.Parameters param = mCamera.getParameters();
                param.setPreviewSize(WIDTH, HEIGHT);
                mCamera.setParameters(param);

                //int bufSize = (WIDTH * HEIGHT) * ImageFormat.getBitsPerPixel(param.getPreviewFormat()) / 8;
                //mCallbackBuf = new byte[bufSize];
                //mCamera.setPreviewCallbackWithBuffer(this);
                //mCamera.setPreviewCallback(this);

            } catch (Exception e) {
                Util.Logd(TAG, "Exception:" + e.getMessage());
                if (mCamera != null) {
                    mCamera.release();
                    mCamera = null;
                    return false;
                }
            }

            HandlerThread faceThread = new HandlerThread("Face track");
            faceThread.start();
            mFaceHandler = new FaceHandler(faceThread.getLooper());

            return true;
        }

        public void uninit() {
            if (mCamera != null) {
                if (mIsTracking)
                    this.stop();

                mCamera.release();

                mCallbackBuf = null;
                mCamera = null;

                mFaceHandler.getLooper().quit();
            }
        }

        public void start() {
            if (mCamera != null) {
                mIsTracking = true;
                mIsIdle = true;
                mIsPause = false;
                mCamera.setPreviewCallback(this);
                mCamera.startPreview();
            }
        }

        public void stop() {
            if (mCamera != null) {
                mCamera.setPreviewCallback(null);
                mCamera.stopPreview();

                // reset
                mFaceCtl.runMotor(RobotMotion.Motors.NECK_ROTATION, 0, 500, 0);
                mFaceCtl.runMotor(RobotMotion.Motors.NECK_TILT, 0, 500, 0);

                mIsTracking = false;
            }
        }

        public void pauseFaceDetect() {
            mIsPause = true;
        }

        public void resumeFaceDetect() {
            mIsPause = false;
        }

        public boolean isTracking() {
            return mIsTracking;
        }

        private void getFocusDistance() {
            //test camera preview size(320x240)
            //double width = 320.0;

            //test angle 25 ~ 30 degree
            double angle = 28.0;

            double focusDistance = (WIDTH / 2.0) / Math.tan(Math.toRadians(angle));
            mRotateParam = focusDistance / WIDTH;

            angle = 16.0;
            focusDistance = (HEIGHT / 2.0) / Math.tan(Math.toRadians(angle));
            mTiltParam = focusDistance / HEIGHT;
        }

        private boolean neckTilt(int angle, int orientation) {
            angle *= orientation;
            mNowTiltAngle += angle;

            if (mNowTiltAngle > 25 || mNowTiltAngle < -15) {
                mNowTiltAngle -= angle;
                return false;
            }

            Util.Logd(TAG, "neckTilt: angle=" + angle + "  mNowTiltAngle=" + mNowTiltAngle);
            mFaceCtl.runMotor(RobotMotion.Motors.NECK_TILT, mNowTiltAngle, 500, 0);
            //mSessionId =
            return true;
        }

        private void neckRotate(int angle, int orientation) {
            angle *= orientation;
            mNowRotateAngle += angle;

            if (mNowRotateAngle > 25 || mNowRotateAngle < -25) {
                mSessionId = mFaceCtl.turn(-10*orientation, 2);
                mNowRotateAngle -= angle;
            } else {
                mSessionId = mFaceCtl.runMotor(RobotMotion.Motors.NECK_ROTATION, mNowRotateAngle, 500, 0);
            }

            //Util.Logd(TAG, "mNowAngle:" + mNowRotateAngle);
        }

        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            //Util.Logd(TAG, "onPreviewFrame");

            if (mIsTracking && mIsIdle) {
                mIsIdle = false;
                mFaceHandler.obtainMessage(MSG_FACE_DECODE, data).sendToTarget();
            }
        }

        private class FaceHandler extends Handler {

            public FaceHandler(Looper looper) {
                super(looper);
            }

            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case MSG_FACE_DECODE:
                        final byte[] yuvData = (byte[]) msg.obj;
                        if (yuvData == null) {
                            Util.Logd(TAG, "!!!!!!yuvData is null");
                            break;
                        }
                        // hand cover
                        mHandler.obtainMessage(MSG_HAND_COVER_DETECT, yuvData).sendToTarget();

                        if (mIsPause) {
                            mIsIdle = true;
                            break;
                        }

                        Bitmap imgBmp = decodeYUV420SP(yuvData, WIDTH, HEIGHT);
                        //saveBitmap(imgBmp);
                        long end = System.currentTimeMillis();

                        FaceDetector.Face[] faces = new FaceDetector.Face[MAX_FACES];
                        int count = mFaceDetector.findFaces(imgBmp, faces);
                        if (count < 1 || faces[0].confidence() < FaceDetector.Face.CONFIDENCE_THRESHOLD) {
                            mIsIdle = true;
                        } else {
                            // rotate head
                            PointF mid = new PointF();
                            faces[0].getMidPoint(mid);

                            int or;
                            double dis;
                            double grad;
                            double degree;

                            dis = mid.y / HEIGHT - 0.5;
                            or = dis < 0 ? OR_POST : OR_NEGT;
                            //grad = Math.atan(Math.abs(dis) / mRotateParam);
                            //degree = Math.toDegrees(grad);
                            Util.Logd(TAG, "Tilt height:" + dis);
                            // check if need neck tilt
                            boolean bneckTilt = false;
                            if (Math.abs(dis) > 0.15) {
                                if (neckTilt((int)5, or)) {
                                    bneckTilt = true;
                                }
                            }

                            dis = mid.x / WIDTH - 0.5;
                            or = dis > 0 ? OR_POST : OR_NEGT;
                            grad = Math.atan(Math.abs(dis) / mRotateParam);
                            degree = Math.toDegrees(grad);
                            if (or == OR_POST) {
                                degree -= 2;
                            } else {
                                degree += 2;
                            }
                            //Util.Logd(TAG, "Rotate Degree:" + degree*or);
                            if (Math.abs(degree) > 5.0f) {
                                neckRotate((int) degree, or);
                            } else {
                                if (bneckTilt) {
                                    try {
                                        Thread.sleep(200);
                                    } catch (InterruptedException e) {
                                        // ignore
                                    }
                                }

                                mIsIdle = true;
                            }
                        }
                        break;
                    default:
                        break;
                }
            }
        }

        private void saveBitmap(Bitmap bitmap) {
            File file = new File("/sdcard/DCIM/Camera/" + "1.png");
            if (file.exists()) {
                file.delete();
            }
            FileOutputStream out;
            try {
                out = new FileOutputStream(file);
                if (bitmap.compress(Bitmap.CompressFormat.PNG, 90, out)) {
                    out.flush();
                    out.close();
                } else {
                    Util.Logd(TAG, "Bitmap compress fail");
                }

            } catch (Exception e) {
                Util.Logd(TAG, e.getMessage());
            }
        }

        private Bitmap decodeYUV420SP(byte[] yuv420sp, int width, int height) {
            final int frameSize = width * height;
            if (yuv420sp == null) {
                throw new NullPointerException("buffer yuv420sp is null");
            }

            if (yuv420sp.length < frameSize) {
                throw new IllegalArgumentException("buffer yuv420sp is illegal");
            }

            int[] rgb = new int[frameSize];
            for (int j = 0, yp = 0; j < height; j++) {
                int uvp = frameSize + (j >> 1) * width, u = 0, v = 0;
                for (int i = 0; i < width; i++, yp++) {
                    int y = (0xff & (yuv420sp[yp])) - 16;
                    if (y < 0)
                        y = 0;
                    if ((i & 1) == 0) {
                        v = (0xff & yuv420sp[uvp++]) - 128;
                        u = (0xff & yuv420sp[uvp++]) - 128;
                    }
                    int y1192 = 1192 * y;
                    int r = (y1192 + 1634 * v);
                    int g = (y1192 - 833 * v - 400 * u);
                    int b = (y1192 + 2066 * u);
                    if (r < 0)
                        r = 0;
                    else if (r > 262143)
                        r = 262143;
                    if (g < 0)
                        g = 0;
                    else if (g > 262143)
                        g = 262143;
                    if (b < 0)
                        b = 0;
                    else if (b > 262143)
                        b = 262143;
                    rgb[yp] = 0xff000000 | ((r << 6) & 0xff0000)
                            | ((g >> 2) & 0xff00) | ((b >> 10) & 0xff);
                }
            }

            Bitmap bmp = Bitmap.createBitmap(rgb, width, height, Bitmap.Config.RGB_565);
            return bmp;
        }
    }
}
