package com.avatar.personate;

import android.content.Context;
import android.graphics.Canvas;
import android.text.Layout;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.robot.widget.CommonPopupWindow;

public class ListItemLayout extends RelativeLayout implements
        View.OnClickListener {
    private static final String TAG = "ListItemLayout";
    private static final int POP_WINDOW_WIDTH = 1260;
    private static final int POP_WINDOW_HEIGHT = 780;

    private TextView mTextView;
    private ImageView mImageView;
    private CommonPopupWindow mTextPopWindow;

    public ListItemLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public ListItemLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ListItemLayout(Context context) {
        super(context);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        initView();
    }

    private void initView() {
        mTextView = (TextView) findViewById(R.id.tv_content);
        mImageView = (ImageView) findViewById(R.id.im_hint);
        mImageView.setOnClickListener(this);
        mImageView.setVisibility(INVISIBLE);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        Layout layout = mTextView.getLayout();
        if (layout != null) {
            int lines = layout.getLineCount();
            if (lines > 0) {
                if (layout.getEllipsisCount(lines - 1) > 0) {
                    Util.Logd(TAG,
                            "Text is ellipsized" + lines
                                    + " l.getEllipsisCount(lines - 1): "
                                    + layout.getEllipsisCount(lines - 1));
                    mImageView.setVisibility(VISIBLE);
                } else {
                    Util.Logd(TAG, "Text is not ellipsized" + lines);
                    mImageView.setVisibility(GONE);
                }
            }
        } else {
            Util.Logd(TAG, "layout is null");
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
    }

    public void setText(String string) {
        mTextView.setText(string);
        invalidate();
        mImageView.invalidate();
    }

    private void showTextPopWindow() {
        mTextPopWindow = new AutoDismissPopWindow(getContext(), this,
                R.layout.list_item_popupwindow, POP_WINDOW_WIDTH,
                POP_WINDOW_HEIGHT);
        View view = mTextPopWindow.getContentView();
        view.findViewById(R.id.alert_dialog_cancel).setOnClickListener(this);
        ((TextView) view.findViewById(R.id.alert_dialog_text))
                .setText(mTextView.getText());
    }

    private void removeTextPopWindow() {
        if (mTextPopWindow != null) {
            mTextPopWindow.dismiss();
            mTextPopWindow = null;
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.im_hint:
                showTextPopWindow();
                break;
            case R.id.alert_dialog_cancel:
                removeTextPopWindow();
                break;
            default:
                break;
        }
    }

}
