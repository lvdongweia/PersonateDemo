package com.avatar.personate;

import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.media.Image;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;


public class ThinkView extends FrameLayout {
    private static final String TAG = "ThinkView";

    private Animation mThinkAnim;
    private ImageView mThinkView;


    public ThinkView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        Util.Logd(TAG, "onFinishInflate");

        mThinkView = (ImageView) findViewById(R.id.think_anim);
        mThinkAnim = AnimationUtils.loadAnimation(mContext, R.anim.think_anim);
    }

    public void startThinkAnim() {
        if (mThinkView.getVisibility() == View.GONE) {
            mThinkView.setVisibility(View.VISIBLE);
        }

        mThinkView.startAnimation(mThinkAnim);
    }

    public void stopThinkAnim() {
        mThinkView.clearAnimation();
        mThinkView.setVisibility(View.GONE);
    }


}
