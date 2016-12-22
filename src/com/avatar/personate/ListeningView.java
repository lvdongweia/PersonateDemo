package com.avatar.personate;

import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.util.AttributeSet;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;


public class ListeningView extends RelativeLayout {
    private ImageView mListenView;
    private AnimationDrawable mListenAnim;

    private Context mContext;

    public ListeningView(Context context, AttributeSet attrs) {
        super(context, attrs);

        mContext = context;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mListenAnim.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mListenAnim.stop();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mListenView = (ImageView) findViewById(R.id.anim_listen);
        mListenView.setBackgroundResource(R.drawable.listen_anim);
        mListenAnim = (AnimationDrawable) mListenView.getBackground();
    }
}
