package com.avatar.personate.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.avatar.personate.Util;
import com.avatar.personate.service.PersonateService;

public class PersonateReceiver extends BroadcastReceiver {
    private String TAG = "PersonateReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Util.Logd(TAG, "Receive--" + intent.getAction());

        Intent sv = new Intent(context, PersonateService.class);
        context.startService(sv);
    }
}
