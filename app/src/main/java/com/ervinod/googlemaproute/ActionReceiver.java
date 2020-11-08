package com.ervinod.googlemaproute;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class ActionReceiver extends BroadcastReceiver {

    Context context;

    @Override
    public void onReceive(Context context, Intent intent) {

        this.context = context;

        String action = intent.getStringExtra("action");
        if (action.equals("stop")) {
            stopLocationService();
        } else if (action.equals("start")) {
            startLocationService();

        }
        //This is used to close the notification tray
        Intent it = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        context.sendBroadcast(it);
        Log.d("Service", "Action Received");
    }

    public void stopLocationService() {
        if (ServiceUtils.isMyServiceRunning(LocationForegroundService.class, context)) {
            Intent startIntent = new Intent(context, LocationForegroundService.class);
            startIntent.setAction("STOPFOREGROUND_ACTION");
            context.startService(startIntent);
        }
    }

    public void startLocationService() {
        if (ServiceUtils.isMyServiceRunning(LocationForegroundService.class, context)) {
            Intent startIntent = new Intent(context, LocationForegroundService.class);
            startIntent.setAction("STARTFOREGROUND_ACTION");
            context.startService(startIntent);
        }
    }

}
