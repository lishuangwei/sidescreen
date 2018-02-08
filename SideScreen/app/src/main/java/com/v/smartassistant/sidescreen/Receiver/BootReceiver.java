package com.v.smartassistant.sidescreen.Receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.EventLog;
import android.util.Log;

/**
 * Created by lishuangwei on 17-12-13.
 */

public class BootReceiver extends BroadcastReceiver {
    private static final String ACTION_BOOT = "android.intent.action.BOOT_COMPLETED";
    private static final String ACTION_SHUTDOWN = "android.intent.action.ACTION_SHUTDOWN";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d("shuang", "onReceive: " + action);
        if (action.equals(ACTION_BOOT)) {
            Intent service = new Intent();
            service.setAction("android.intent.action.SIDESCREEN");
            service.setPackage(context.getPackageName());
            context.startService(service);
        } else if (action.equals(ACTION_SHUTDOWN)) {
            Intent service = new Intent();
            service.setAction("android.intent.action.SIDESCREEN");
            service.setPackage(context.getPackageName());
            context.stopService(service);
        }
    }
}
