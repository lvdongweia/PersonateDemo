package com.avatar.personate.scene;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.PointF;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.media.FaceDetector;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.robot.motion.RobotMotion;
import android.robot.scheduler.RobotConstants;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


import com.avatar.personate.Util;
import com.avatar.robot.Robot;

import com.avatar.personate.R;

public class DefaultScene extends PersonateScene {
    private final String TAG = "DefaultScene";
    private Map<Integer, String[]> mMoodMap;
    private FaceTrack mFaceTrack;
    private RobotMotion mRobotCtl;

    private boolean mIsPersonNearby;

    public DefaultScene(Context context) {
        super(context, SCENE_DEFAULT);

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                loadMoodResource();
            }
        });

        mRobotCtl = new RobotMotion();
        mRobotCtl.setListener(new RobotMotion.Listener() {

            @Override
            public void onStatusChanged(int status) {
                Util.Logd(TAG, "onStatusChanged");
            }

            @Override
            public void onCompleted(int session_id, int result) {
                Util.Logd(TAG, "onCompleted");
            }
        });

        //start face track
        mFaceTrack = new FaceTrack();
    }

    @Override
    public void start() {
        mIsWorking = true;
        registerEvent();

        startFaceTrack();
    }

    @Override
    public void stop() {
        unregisterEvent();

        // stop to prevent not get leave event
        stopFaceTrack();
        mIsWorking = false;
    }

    @Override
    public void handleMessageInner(Message msg) {
        Util.Logd(TAG, "Message:" + msg.what);
        switch (msg.what) {
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
        IntentFilter intent = new IntentFilter();

        registerTouchEvent(intent);
        registerRCEvent(intent);

        mContext.registerReceiver(mEventReceiver, intent);
    }

    private void unregisterEvent() {
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
        if (mFaceTrack.init())
            mFaceTrack.start();
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


    private static final int MSG_FACE_DECODE = 0;

    private class FaceTrack implements Camera.PreviewCallback {

        private final int WIDTH = 640;
        private final int HEIGHT = 480;
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
                Util.Logd(TAG, e.getMessage());
                if (mCamera != null) {
                    mCamera.release();
                    mCamera = null;
                    return false;
                }
            }

            HandlerThread faceThread = new HandlerThread("Face track");
            faceThread.start();
            mFaceHandler = new FaceHandler(faceThread.getLooper());

            // reset head position
            mNowRotateAngle = 0;
            neckRotate(0, OR_POST);

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

        private void neckTilt(int angle, int orientation) {
            angle *= orientation;
            mNowTiltAngle += angle;
            mSessionId = mFaceCtl.runMotor(RobotMotion.Motors.NECK_TILT, mNowTiltAngle, 500, 0);
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
                            if (Math.abs(dis) > 0.2) {
                                neckTilt((int) 5, or);
                            } else {
                                dis = mid.x / WIDTH - 0.5;
                                or = dis > 0 ? OR_POST : OR_NEGT;
                                grad = Math.atan(Math.abs(dis) / mRotateParam);
                                degree = Math.toDegrees(grad);
                                if (or == OR_POST) {
                                    degree -= 2;
                                } else {
                                    degree += 2;
                                }
                                Util.Logd(TAG, "Rotate Degree:" + degree*or);
                                if (Math.abs(degree) > 5.0f) {
                                    neckRotate((int)degree, or);
                                } else {
                                    mIsIdle = true;
                                }
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
