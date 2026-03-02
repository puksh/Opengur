package com.puksh.pokenimgur.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.puksh.pokenimgur.classes.PokengurApp;
import com.puksh.pokenimgur.util.LogUtil;

/**
 * Created by kcampagna on 8/12/15.
 */
public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = BootReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        PokengurApp app = PokengurApp.getInstance(context);

        // We only care if we have a valid user
        if (app.getUser() != null) {
            LogUtil.v(TAG, "User present, creating notification alarm");
            AlarmReceiver.createNotificationAlarm(context);
        } else {
            LogUtil.v(TAG, "No user present, not creating notification alarm");
        }
    }
}
