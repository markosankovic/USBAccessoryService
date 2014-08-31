package com.sankovicmarko.android.usbaccessoryservice;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

/**
 * OBCMessenger
 * <p/>
 * Uses the LocalBroadcastManager to deliver messages from the OBC to any Activity.
 */
public class OBCBroadcastMessenger {

    private final Context context;

    public OBCBroadcastMessenger(Context context) {
        this.context = context;
    }

    public static final String ACTION_SPEED = "com.sankovicmarko.android.usbaccessoryservice.OBCBroadcastMessenger.action.speed";

    public void sendSpeed(int speed) {
        Intent intent = new Intent(ACTION_SPEED);
        intent.putExtra("speed", String.valueOf(speed));
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }
}
