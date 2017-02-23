package com.avatar.personate.scene;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.media.FaceDetector;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.robot.motion.RobotMotion;

import com.avatar.personate.FaceRecgUtil;
import com.avatarmind.camera.extensions.AvatarCameraParameters;
import com.avatar.personate.Util;
import com.avatarmind.vision.cover.handcover;
import com.avatarmind.vision.wave.handwave;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InterruptedIOException;
import java.util.List;

public class CameraEvent implements Camera.PreviewCallback{
    private final String TAG = "CameraEvent";

    private static CameraEvent mInstance;

    private final int MAX_FACES = 2;
    private final int MAX_PREVIEW_BUFFERS = 5;
    private final int WIDTH = 640;
    private final int HEIGHT = 480;
    private final int NECK_RIST = 1;
    private final int NECK_BOW = -1;
    private final int NECK_LEFT = 1;
    private final int NECK_RIGHT = -1;
    public static final int HEAD_LEFT_MAX = -30;
    public static final int HEAD_RIGHT_MAX = 30;
    public static final int HEAD_RISE_MAX = 25;
    public static final int HEAD_BOW_MAX = -15;

    private RobotMotion mRobotCtrl;
    private FaceDetector mFaceDetector;
    private FrameDecode mFaceDecode;
    private FrameDecode mHandDecode;
    private FaceRecgUtil mFaceRecg;
    private CameraEventListener mCallback;
    private RandomFindFace mRandomFindFace;

    private Context mContext;
    private Camera mCamera;
    private SurfaceTexture mSurfaceTexture;
    private Handler mEventHandler;
    private Handler mFaceHandler;

    private int mNowRotateAngle = 0;
    private int mNowTiltAngle = 0;
    private double mRotateParam;
    private double mTiltParam;
    private boolean mIsFaceDetect;
    private boolean mIsFaceRecg;
    private boolean mIsHandCover;
    private boolean mNeckRunning;
    private boolean mIsFaceRecognizing;
    private boolean mIsFindFace;

    private int mFindFailCount;
    private int mNeckSession;

    public static CameraEvent getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new CameraEvent(context);
        }

        return mInstance;
    }

    private final RobotMotion.Listener mMotionListener = new RobotMotion.Listener() {

        @Override
        public void onStatusChanged(int status) {

        }

        @Override
        public void onCompleted(int session_id, int result) {
            if (mNeckSession == session_id) {
                mNeckRunning = false;
            }
        }
    };

    private class RandomFindFace implements Runnable {
        private Handler mRunHandler;
        private int mHeadIndex;
        private boolean mIsAsrResp;

        RandomFindFace(Handler handler) {
            mRunHandler = handler;
            mHeadIndex = 0;
        }

        public void start() {
            if (mRunHandler.hasCallbacks(this))
                return;

            mIsAsrResp = false;
            mRunHandler.postDelayed(this, 1000);
        }

        public void onAsrResp() {
            mIsAsrResp = true;
        }

        @Override
        public void run() {
            if (mIsFindFace) return;
            if (!mIsAsrResp) {
                mRunHandler.postDelayed(this, 500);
                return;
            }

            switch (mHeadIndex) {
                case 0:
                    setNeckTiltAngle(15);
                    break;
                case 1:
                    setNeckRotateAngle(20);
                    break;
                case 2:
                    setNeckRotateAngle(-20);
                    break;
                case 3:
                    setNeckRotateAngle(0);
                    break;
                case 4:
                    setNeckTiltAngle(-15);
                    break;

                default:
                    break;
            }
            mHeadIndex += 1;
            mHeadIndex %= 5;

            mRunHandler.postDelayed(this, 2000);
        }
    }

    private CameraEvent(Context context) {
        mContext = context;

        mFaceRecg = new FaceRecgUtil(context);
        mRobotCtrl = new RobotMotion();
        mRobotCtrl.setListener(mMotionListener);

        mFaceDetector = new FaceDetector(WIDTH, HEIGHT, MAX_FACES);
        mFaceDecode = new FrameDecode(MAX_PREVIEW_BUFFERS);
        mHandDecode = new FrameDecode(MAX_PREVIEW_BUFFERS);

        File AppDir = mContext.getDir("cascade", Context.MODE_PRIVATE);
        String strAppPath = AppDir.getAbsolutePath();
        handwave.nativeInitial(strAppPath);
    }

    public boolean init() {
        try {
            mCamera = Camera.open(0);
            if (mCamera == null) {
                Util.Logd(TAG, "Open camera(0) fail.");
                return false;
            }

            mSurfaceTexture = new SurfaceTexture(10);
            mCamera.setPreviewTexture(mSurfaceTexture);

            Camera.Parameters param = mCamera.getParameters();
            param.setPreviewSize(WIDTH, HEIGHT);

            //set avatarmind extensions parameters
            /*AvatarCameraParameters customParams = new AvatarCameraParameters();
            //set AE Mode
            List<String> aeModeList = customParams.getSupportedAEModes(param);
            if (aeModeList != null && aeModeList.contains(AvatarCameraParameters.AE_MODE_MANUAL)) {
                customParams.setAEMode(AvatarCameraParameters.AE_MODE_MANUAL, param);
            }
            //set shutter time
            List<String> shutterList = customParams.getSupportedShutter(param);
            if (shutterList != null) {
                customParams.setShutter(shutterList.get(2), param);
            }*/

            mCamera.setParameters(param);
            getFocusDistance();

            // malloc bufer
            //int bufSize = (WIDTH * HEIGHT) * ImageFormat.getBitsPerPixel(param.getPreviewFormat()) / 8;

        } catch (Exception e) {
            Util.Logd(TAG ,"Exception:" + e.getMessage());
            if (mCamera != null) {
                mCamera.release();
                mCamera = null;
                return false;
            }
        }
        // reset angle
        mNowRotateAngle = 0;
        mNowTiltAngle = 0;

        HandlerThread eventThread = new HandlerThread("Event Handler");
        eventThread.start();
        mEventHandler = new EventHandler(eventThread.getLooper());
        mRandomFindFace = new RandomFindFace(mEventHandler);

        HandlerThread faceThread = new HandlerThread("Face Recognize");
        faceThread.start();
        mFaceHandler = new Handler(faceThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what != MSG_FACE_RECOGIZE) return;

                mIsFaceRecognizing = true;
                long t1 = System.currentTimeMillis();
                String name = null;
                String faceID = mFaceRecg.decodeFaceID((byte[])msg.obj, WIDTH, HEIGHT);
                if (faceID != null) {
                    name = mFaceRecg.recognize(faceID);
                }
                long t2 = System.currentTimeMillis();
                Util.Logd(TAG, "FaceID:" + faceID + "  Name:" + name +  "  Use:" + (t2-t1) + "ms");
                if (name != null && mEventHandler != null) {
                    mEventHandler.obtainMessage(MSG_EVENT_REPORT, EVENT_FACE_RECOGNIZE, -1, name).sendToTarget();
                }
                mIsFaceRecognizing = false;
            }
        };

        return true;
    }

    public void uninit() {
        if (mCamera != null) {
            //stop();
            mCamera.release();
            mCamera = null;

            mFaceHandler.removeMessages(MSG_FACE_RECOGIZE);
            mFaceHandler.getLooper().quitSafely();
            mFaceHandler = null;

            mEventHandler.removeMessages(MSG_EVENT_REPORT);
            mEventHandler.removeMessages(MSG_PREVIEW_FRAME);
            mEventHandler.removeMessages(MSG_RANDOM_FIND_FACE);
            mEventHandler.getLooper().quitSafely();
            mEventHandler = null;
            mRandomFindFace = null;
        }
    }

    public void start() {
        if (mCamera != null) {
            setFaceDetect(true);
            setFaceRecg(true);
            setHandCover(true);

            mFaceDecode.start(mFaceFrame);
            mHandDecode.start(mHandFrame);

            mCamera.setPreviewCallback(this);
            mCamera.startPreview();
        }
    }

    public void stop() {
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();

            setFaceDetect(false);
            setFaceRecg(false);
            setHandCover(false);

            mFaceDecode.stop();
            mHandDecode.stop();

            mEventHandler.removeCallbacks(mRandomFindFace);
        }
    }

    public void setListener(CameraEventListener callback) {
        mCallback = callback;
    }

    public void setFaceDetect(boolean enable) {
        mIsFaceDetect = enable;
    }

    public void setFaceRecg(boolean enable) {
        mIsFaceRecg = enable;
    }

    public void setHandCover(boolean enable) {
        mIsHandCover = enable;
    }

    public int getNeckRotateAngle() {
        return mNowRotateAngle;
    }

    public void setNeckRotateAngle(int angle) {
        if (HEAD_LEFT_MAX <= angle && angle <= HEAD_RIGHT_MAX) {
            mNowRotateAngle = angle;
            mNeckRunning = true;
            mNeckSession = mRobotCtrl.runMotor(RobotMotion.Motors.NECK_ROTATION, mNowRotateAngle, 800, 0);
        }
    }

    public void setNeckTiltAngle(int angle) {
        if (HEAD_BOW_MAX <= angle && angle <= HEAD_RISE_MAX) {
            mNowTiltAngle = angle;
            mRobotCtrl.runMotor(RobotMotion.Motors.NECK_TILT, mNowTiltAngle, 800, 0);
        }
    }

    public void setAsrResult(String text) {
        mRandomFindFace.onAsrResp();
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        //Util.Logd(TAG, "onPreviewFrame");
        mEventHandler.obtainMessage(MSG_PREVIEW_FRAME, data).sendToTarget();
    }

    private final int EVENT_HAND_COVER = 0;
    private final int EVENT_HAND_WAVE  = 1;
    private final int EVENT_FACE_RECOGNIZE = 2;
    /**
     * @interface
     *
     */
    public interface CameraEventListener{
        public void onHandWave();
        public void onHandCover();
        public void onFaceRecognize(String name);
    }

    private final int MSG_PREVIEW_FRAME = 0;
    private final int MSG_FACE_RECOGIZE = 1;
    private final int MSG_EVENT_REPORT  = 2;
    private final int MSG_RANDOM_FIND_FACE = 3;
    private class EventHandler extends Handler {
        public EventHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_PREVIEW_FRAME:
                    final byte[] yuvData = (byte[])msg.obj;
                    if (mIsFaceDetect) mFaceDecode.addBuffer(yuvData);
                    if (mIsHandCover) mHandDecode.addBuffer(yuvData);
                    break;

                case MSG_EVENT_REPORT:
                    if (mCallback != null) {
                        int event = msg.arg1;
                        if (event == EVENT_HAND_WAVE)
                            mCallback.onHandWave();
                        else if (event == EVENT_HAND_COVER)
                            mCallback.onHandCover();
                        else if (event == EVENT_FACE_RECOGNIZE)
                            mCallback.onFaceRecognize((String)msg.obj);
                    }
                    break;

                default:
                    break;
            }
        }
    }

    /**
     * @interface
     */
    private interface FrameAction {
        void run(byte[] data);
    }

    private final FrameAction mFaceFrame = new FrameAction() {
        @Override
        public void run(byte[] data) {
            //Util.Logd(TAG, "Face Frame");
            if (!mNeckRunning)
                findface(data);
        }
    };

    private long mLastCoverTime;
    private int mCoverCount;
    private final FrameAction mHandFrame = new FrameAction() {
        @Override
        public void run(byte[] data) {
            String wave = handwave.detectwave(data, WIDTH, HEIGHT);
            //Util.Logd(TAG, "Hand wave:" + wave);
            if (wave.equals("WAVE")) {
                Util.Logd(TAG, "@@@@@@Hand Wave@@@@@@");
                mEventHandler.obtainMessage(MSG_EVENT_REPORT, EVENT_HAND_WAVE, -1).sendToTarget();
            }

            String cover = handcover.detectcover(data, WIDTH, HEIGHT);
            //Util.Logd(TAG, "Hand cover:" + cover);
            if (cover.equals("COVER")) {
                long time = System.currentTimeMillis();
                if (mCoverCount == 0) {
                    mLastCoverTime = time;
                }

                // 指定时间内检测到覆盖事件
                if ((time - mLastCoverTime) <= 950) {
                    mCoverCount++;
                } else {
                    mCoverCount = 1;
                    mLastCoverTime = time;
                }

                if (mCoverCount >= 10) {
                    //report cover event
                    Util.Logd(TAG, "######Hand Cover######");
                    //mEventHandler.obtainMessage(MSG_EVENT_REPORT, EVENT_HAND_COVER, -1).sendToTarget();
                    // reset count
                    mCoverCount = 0;
                }
            }
        }
    };

    private void getFocusDistance() {
        double angle = 30.0; // 实际测量的摄像头可视角度

        double focusDistance = (WIDTH / 2.0) / Math.tan(Math.toRadians(angle));
        mRotateParam = focusDistance / WIDTH;

        angle = 23.0;
        focusDistance = (HEIGHT / 2.0) / Math.tan(Math.toRadians(angle));
        mTiltParam = focusDistance / HEIGHT;
    }

    private void findface(final byte[] data) {
        long start = System.currentTimeMillis();
        Bitmap bmp = decodeYUV420SP(data, WIDTH, HEIGHT);
        long time1 = System.currentTimeMillis();
        FaceDetector.Face[] faces = new FaceDetector.Face[MAX_FACES];
        int count = mFaceDetector.findFaces(bmp, faces);
        long time2 = System.currentTimeMillis();
        //Util.Logd(TAG, "Conver:" + (time1 - start) + "ms  Find:" + (time2 - time1) + "ms");
        if (count < 1 || faces[0].confidence() < FaceDetector.Face.CONFIDENCE_THRESHOLD) {
            if (mIsFindFace && ++mFindFailCount > 10) {
                mIsFindFace = false;
                mRandomFindFace.start();
            }
            return;
        }

        mIsFindFace = true;
        mFindFailCount = 0;

        // 人脸识别
        if (!mIsFaceRecognizing && mIsFaceRecg) {
            mIsFaceRecognizing = true;
            mFaceHandler.obtainMessage(MSG_FACE_RECOGIZE, data).sendToTarget();
        }

        PointF mid = new PointF();
        faces[0].getMidPoint(mid);

        int or, degree, sleep;
        double dis, grad;
        boolean isNeckMove = false;
        // neck tilt
        dis = mid.y / HEIGHT - 0.5;
        or = dis < 0 ? NECK_RIST : NECK_BOW;
        //grad = Math.atan(Math.abs(dis) / mTiltParam);
        //degree = (int)Math.toDegrees(grad);
        if (Math.abs(dis) > 0.17) {
            if (neckTilt(5, or))
                isNeckMove = true;
        }

        // neck rotate
        dis = mid.x / WIDTH - 0.5;
        or = dis < 0 ? NECK_RIGHT : NECK_LEFT;
        grad = Math.atan(Math.abs(dis) / mRotateParam);
        degree = (int)Math.toDegrees(grad);
        if (or == NECK_LEFT)
            degree -= 2;
        else
            degree += 2;
        if (Math.abs(dis) > 0.13) {
            neckRotate(degree, or);
        } else {
            if (isNeckMove) {
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    //ignore
                }
            }
        }
    }

    private boolean neckTilt(int angle, int orientation) {
        angle *= orientation;
        mNowTiltAngle += angle;

        if (mNowTiltAngle > HEAD_RISE_MAX | mNowTiltAngle < HEAD_BOW_MAX) {
            mNowTiltAngle -= angle;
            return false;
        }

        Util.Logd(TAG, "Neck Tilt:" + angle + "  Now:" + mNowTiltAngle);
        mRobotCtrl.runMotor(RobotMotion.Motors.NECK_TILT, mNowTiltAngle, 800, 0);
        return true;
    }

    private void neckRotate(int angle, int orientation) {
        angle *= orientation;
        /*int tmpAngle = mNowRotateAngle + angle;

        if (tmpAngle > HEAD_RIGHT_MAX)
            tmpAngle = HEAD_RIGHT_MAX;
        else if (tmpAngle < HEAD_LEFT_MAX)
            tmpAngle = HEAD_LEFT_MAX;

        if (mNowRotateAngle == tmpAngle)
            return;

        mNowRotateAngle = tmpAngle;
        mNeckSession = mRobotCtrl.runMotor(RobotMotion.Motors.NECK_ROTATION, mNowRotateAngle, 300, 0);
        mNeckRunning = true;
        */

        mNowRotateAngle += angle;
        if (mNowRotateAngle > HEAD_RIGHT_MAX || mNowRotateAngle < HEAD_LEFT_MAX) {
            mRobotCtrl.turn(-5*orientation, 1);
            mNowRotateAngle -= angle;
        } else {
            mNeckSession = mRobotCtrl.runMotor(RobotMotion.Motors.NECK_ROTATION, mNowRotateAngle, 800, 0);
            mNeckRunning = true;
        }
        Util.Logd(TAG, "Neck Rotate:" + angle + "  Now:" + mNowRotateAngle);
    }

    private class FrameDecode {
        private int mIndex;
        private int mCurIndex;
        private int mMaxCount;
        private boolean mIsFinish;
        private byte[][] mBuffers;
        private Handler mHandler;

        public FrameDecode(int bufcount) {
            mBuffers = new byte[bufcount][];
            mIndex = 0;
            mCurIndex = 0;
            mIsFinish = false;
            mMaxCount = bufcount;
        }

        public void addBuffer(final byte[] data) {
            synchronized (mBuffers) {
                mIndex %= mMaxCount;
                mBuffers[mIndex++] = data;
            }
        }

        public void start(final FrameAction action) {
            mIsFinish = false;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    byte[] data;
                    while (!mIsFinish) {
                        mCurIndex %= mMaxCount;
                        synchronized (mBuffers) {
                            data = mBuffers[mCurIndex];
                            mBuffers[mCurIndex] = null;
                        }

                        mCurIndex++;
                        if (mIsFinish) break;
                        if (data != null) {
                            action.run(data);
                        }

                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {
                            // ignore
                        }
                    }
                }
            }).start();
        }

        public void stop() {
            mIsFinish = true;
            synchronized (mBuffers) {
                for (int i = 0; i < mMaxCount; i++) {
                    mBuffers[i] = null;
                }
            }
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

    static int mImgCount = 0;
    private void saveBitmap(Bitmap bitmap) {
        File file = new File("/sdcard/DCIM/Camera/" + (mImgCount++) + "png");
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
}
