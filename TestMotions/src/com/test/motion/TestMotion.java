package com.test.motion;

import android.app.Activity;
import android.os.Bundle;
import android.robot.motion.RobotMotion;
import android.view.View;

import com.avatar.robot.util.SystemMotion;

public class TestMotion extends Activity
{
    private RobotMotion mRobotCtl;


    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mRobotCtl = new RobotMotion();
    }

    public void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            // ignore
        }
    }

    public void onReset(View v) {
        mRobotCtl.reset(RobotMotion.Units.ALL);
    }

    private boolean isIdle() {
        return true;
    }

    public void onButton1(View v) {
        mRobotCtl.runMotor(RobotMotion.Motors.FOREARM_ROTATION_LEFT, 60, 2000, 0);
        mRobotCtl.runMotor(RobotMotion.Motors.FOREARM_ROTATION_RIGHT, 60, 2000, 0);
        sleep(3000);
        if (!isIdle()) return;

        mRobotCtl.runMotor(RobotMotion.Motors.FOREARM_ROTATION_LEFT, 0, 2000, 0);
        mRobotCtl.runMotor(RobotMotion.Motors.FOREARM_ROTATION_RIGHT, 0, 2000, 0);
        sleep(3000);
        if (!isIdle()) return;

        mRobotCtl.runMotor(RobotMotion.Motors.FOREARM_ROTATION_LEFT, 60, 2000, 0);
        mRobotCtl.runMotor(RobotMotion.Motors.FOREARM_ROTATION_RIGHT, 60, 2000, 0);
        sleep(3000);
        if (!isIdle()) return;

        mRobotCtl.runMotor(RobotMotion.Motors.FOREARM_ROTATION_LEFT, 0, 2000, 0);
        mRobotCtl.runMotor(RobotMotion.Motors.FOREARM_ROTATION_RIGHT, 0, 2000, 0);
        sleep(3000);
        if (!isIdle()) return;
    }

    public void onButton2(View v) {
        mRobotCtl.runMotor(RobotMotion.Motors.NECK_TILT, 10, 2000, 0);
        sleep(2500);
        if (!isIdle()) return;

        mRobotCtl.runMotor(RobotMotion.Motors.NECK_ROTATION, 40, 3000, 0);
        sleep(5000);
        if (!isIdle()) return;

        mRobotCtl.runMotor(RobotMotion.Motors.NECK_ROTATION, 0, 3000, 0);
        sleep(3000);
        if (!isIdle()) return;

        mRobotCtl.runMotor(RobotMotion.Motors.NECK_ROTATION, -40, 3000, 0);
        sleep(5000);
        if (!isIdle()) return;

        mRobotCtl.runMotor(RobotMotion.Motors.NECK_ROTATION, 0, 3000, 0);
        sleep(3000);
        if (!isIdle()) return;

        mRobotCtl.runMotor(RobotMotion.Motors.NECK_TILT, 0, 2000, 0);
        sleep(5000);
    }

    public void onButton3(View v) {
        mRobotCtl.runMotor(RobotMotion.Motors.ARM_SWING_LEFT, 20, 3000, 0);
        mRobotCtl.runMotor(RobotMotion.Motors.ARM_SWING_RIGHT, 20, 3000, 0);
        sleep(3000);
        if (!isIdle()) return;

        mRobotCtl.runMotor(RobotMotion.Motors.ARM_SWING_LEFT, 0, 3000, 0);
        mRobotCtl.runMotor(RobotMotion.Motors.ARM_SWING_RIGHT, 0, 3000, 0);
        sleep(5000);
    }

    public void onButton4(View v) {
        mRobotCtl.runMotor(RobotMotion.Motors.ARM_ROTATION_LEFT, -25, 3000, 0);
        mRobotCtl.runMotor(RobotMotion.Motors.ARM_ROTATION_RIGHT, 25, 3000, 0);
        sleep(5000);
        if (!isIdle()) return;
        mRobotCtl.runMotor(RobotMotion.Motors.ARM_ROTATION_LEFT, 25, 3000, 0);
        mRobotCtl.runMotor(RobotMotion.Motors.ARM_ROTATION_RIGHT, -25, 3000, 0);
        sleep(5000);
    }

    public void onButton6(View v) {
        mRobotCtl.doAction(SystemMotion.IDLE, 1, 5000);
        sleep(5000);

        mRobotCtl.doAction(SystemMotion.CHAT_HEAD_ARMS_CURVED, 1, 5000);
        sleep(5000);

        mRobotCtl.doAction(SystemMotion.CHAT_HEAD_ARMS, 1, 5000);
        sleep(5000);

        mRobotCtl.doAction(SystemMotion.CHAT_RIGHT_ARM, 1, 5000);
        sleep(5000);

        mRobotCtl.doAction(SystemMotion.CHAT_LEFT_ARM, 1, 5000);
        sleep(5000);
    }

    public void onButton7(View v) {

    }
}
