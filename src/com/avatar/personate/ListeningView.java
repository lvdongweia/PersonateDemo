package com.avatar.personate;

import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.util.AttributeSet;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;


public class ListeningView extends RelativeLayout {
    private final String TAG = "ListeningView";
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
        mListenView.setImageResource(R.drawable.listen_anim);
        mListenAnim = (AnimationDrawable) mListenView.getDrawable();
        mListenAnim.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mListenAnim = (AnimationDrawable) mListenView.getDrawable();
        mListenAnim.stop();
        mListenView.setBackground(null);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mListenView = (ImageView) findViewById(R.id.anim_listen);
        mListenView.setImageResource(R.drawable.listen_anim);
        mListenAnim = (AnimationDrawable) mListenView.getDrawable();
    }
}
