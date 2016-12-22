package com.avatar.personate;

import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.RelativeLayout;


public class SpeakView extends RelativeLayout {
    private ImageView mSpeakView;
    private AnimationDrawable mSpeakAnim;

    public SpeakView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mSpeakView = (ImageView) findViewById(R.id.speak_icon);
        mSpeakView.setBackgroundResource(R.drawable.speak_anim);
        mSpeakAnim = (AnimationDrawable) mSpeakView.getBackground();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        mSpeakAnim.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        mSpeakAnim.stop();
    }
}
