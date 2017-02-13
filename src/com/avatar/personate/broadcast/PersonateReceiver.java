package com.avatar.personate.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.avatar.personate.service.PersonateService;

public class PersonateReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent sv = new Intent(context, PersonateService.class);
        context.startService(sv);
    }
}
