package com.avatar.personate;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.robot.widget.CommonPopupWindow;
import android.view.View;

public class AutoDismissPopWindow extends CommonPopupWindow {

    public AutoDismissPopWindow(Context context, View parentView,
                                int layoutResId, int width, int height) {
        super(context, parentView, layoutResId, width, height);
    }

    public AutoDismissPopWindow(Context context, View parentView,
                                View layoutView, int width, int height) {
        super(context, parentView, layoutView, width, height);
    }

    @Override
    protected void bindView(final Context context, View parentView,
                            View layout, int width, int height) {
        ColorDrawable cd = new ColorDrawable(-0000);
        setBackgroundDrawable(cd);
        setOutsideTouchable(true);
        super.bindView(context, parentView, layout, width, height);
    }
}
