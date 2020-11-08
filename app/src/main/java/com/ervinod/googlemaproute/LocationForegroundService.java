package com.ervinod.googlemaproute;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import org.json.JSONArray;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by @ervinod on 14/10/2020.
 */
public class LocationForegroundService extends Service implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private String mListnerChannel;
    //private NetworkChangeReceiver networkChangeReceiver;
    private GoogleApiClient mGoogleApiClient;
    private boolean isMyServiceStarted;
    private JSONArray jsonArrayPickUp;
    private JSONArray jsonArrayArrived;
    public static double distancespd;
    public static JSONArray jsonArray;
    private LocationRequest mLocationRequest;
    public static MediaPlayer mediaPlayer;
    Timer myTimer_publish;
    TimerTask myTimerTask_publish;
    public double distanceKm, prevLat = 0.0, prevLng = 0.0;
    public double  curLat = 0.0, curLng = 0.0;
    private static final String METER = "meters";
    private static final String KILOMETER = "Kilometers";
    private static final String NAUTICAL_MILES = "nauticalMiles";
    String strDouble;
    private static int counter;
    int CHANNEL_ID_FOREGROUND = 10010, lastShownNotificationId;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public void onCreate() {
        super.onCreate();

        buildGoogleApiClient();
        isMyServiceStarted = false;

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("Service", "onStartCommand  called");
        try {


            if (intent.getAction().equals("STARTFOREGROUND_ACTION")) {
                Intent notificationIntent = new Intent(this, MainActivity.class);
                notificationIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_RECEIVER_FOREGROUND);
                PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                        notificationIntent, 0);

                createAndShowForegroundNotification(this, CHANNEL_ID_FOREGROUND, pendingIntent);

                if (!isMyServiceStarted) {
                    isMyServiceStarted = true;
                    mGoogleApiClient.connect();
                    startPublishingWithTimer();
                }

                if (!mGoogleApiClient.isConnected()) {
                    mGoogleApiClient.connect();
                }


            } else if (intent.getAction().equals("STOPFOREGROUND_ACTION")) {
                stopForeground(true);
                stopSelf();
                isMyServiceStarted = false;
            }

        } catch (NullPointerException e) {
            e.printStackTrace();
            Log.d("Service", "Crashed in forground service");
        }

        return START_STICKY;
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////


    ////////////////////////////////////////////////////////////////////////////////////////////////
    public static boolean isApplicationSentToBackground(final Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> tasks = am.getRunningTasks(1);
        if (!tasks.isEmpty()) {
            ComponentName topActivity = tasks.get(0).topActivity;
            if (!topActivity.getPackageName().equals(context.getPackageName())) {
                return true;
            }
        }
        return false;
    }


    public void mSendNotification() {
        Intent intent = new Intent(LocationForegroundService.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mGoogleApiClient.disconnect();
        if (myTimerTask_publish != null) {
            myTimerTask_publish.cancel();
            myTimer_publish.cancel();
        }

        isMyServiceStarted = false;

        Log.d("Service", "Service  destroyed");

    }


    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(5000); // Update location every 2 second
        mLocationRequest.setSmallestDisplacement(1);

        try {
            if (ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }

            LocationServices.FusedLocationApi.requestLocationUpdates(
                    mGoogleApiClient, mLocationRequest, new LocationListener() {
                        @Override
                        public void onLocationChanged(Location currentLoc) {
                            if(currentLoc!=null){
                                curLat = currentLoc.getLatitude();
                                curLng = currentLoc.getLongitude();

                                Log.d("LOCATION", "Lat:"+prevLat+" , Lng:"+prevLng);
                            }

                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i("Foreground_Service", "onConnection_Suspended");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.i("Foreground_Service", connectionResult.getErrorMessage());
    }


    private void startPublishingWithTimer() {
        if (myTimer_publish != null) {
            Log.d("Service", "Timer already started");
            return;
        }
        myTimer_publish = new Timer();
        final LocationPublishHelper locationPublishHelper = new LocationPublishHelper(LocationForegroundService.this);
        myTimerTask_publish = new TimerTask() {
            @Override
            public void run() {
                if (ServiceUtils.isNetworkAvailable(getApplicationContext())) {

                    //checking lanlong bcoz we dont want to hit api if driver location is not changed
                    if (curLat != prevLat || curLng!=prevLng) {
                        prevLat = curLat;
                        prevLng = curLng;

                        if (distance(prevLat, prevLat, curLat, curLng, METER) >= 5) {
                            counter = 0;
                            locationPublishHelper.publishLocation(prevLat, prevLng);
                        } else {
                            if (counter >= 5 / 100) {
                                counter = 0;
                                locationPublishHelper.publishLocation(prevLat, prevLng);
                            } else {
                                counter++;
                            }

                        }
                    }


                } else {
                    Log.d("ERROR","Internet connection not available");
                }

            }

        };

        myTimer_publish.schedule(myTimerTask_publish, 5000, (long) 5000);

    }


    public static double distance(double lat1, double lng1, double lat2, double lng2, String unit) {
        double earthRadius = 3958.75; // miles (or 6371.0 kilometers)
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double sindLat = Math.sin(dLat / 2);
        double sindLng = Math.sin(dLng / 2);
        double a = Math.pow(sindLat, 2) + Math.pow(sindLng, 2) * Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2));
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double dist = earthRadius * c;

        if (KILOMETER.equals(unit))                // Kilometer
        {
            dist = dist * 1.609344;
        } else if (NAUTICAL_MILES.equals(unit))            // Nautical Miles
        {
            dist = dist * 0.8684;
        } else if (METER.equals(unit))            // meter
        {
            dist = dist * 1609.344;

        }

        return dist;
    }

    private void createAndShowForegroundNotification(Service yourService, int notificationId, PendingIntent intent) {

        //This is the intent of PendingIntent
        Intent intentAction = new Intent(this,ActionReceiver.class);
        //This is optional if you have more than one buttons and want to differentiate between two
        intentAction.putExtra("action","stop");

        PendingIntent pIntentlogin = PendingIntent.getBroadcast(this,1,intentAction,PendingIntent.FLAG_UPDATE_CURRENT);
        final NotificationCompat.Builder builder = getNotificationBuilder(yourService,
                CHANNEL_ID_FOREGROUND, // Channel id
                NotificationManagerCompat.IMPORTANCE_MAX); //Low importance prevent visual appearance for this notification channel on top
                builder.setOngoing(true)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentTitle(yourService.getString(R.string.app_name))
                .setContentText("Online")
                .addAction(R.drawable.ic_see_map, "STOP", pIntentlogin)
                .setContentIntent(intent);


        Notification notification = builder.build();

        yourService.startForeground(notificationId, notification);

        if (notificationId != lastShownNotificationId) {
            // Cancel previous notification
            final NotificationManager nm = (NotificationManager) yourService.getSystemService(Activity.NOTIFICATION_SERVICE);
            nm.cancel(lastShownNotificationId);
        }
        lastShownNotificationId = notificationId;
    }

    public static NotificationCompat.Builder getNotificationBuilder(Context context, int channelId, int importance) {
        NotificationCompat.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            prepareChannel(context, channelId + "", importance);
            builder = new NotificationCompat.Builder(context, channelId + "");
        } else {
            builder = new NotificationCompat.Builder(context);
        }
        return builder;
    }

    @TargetApi(26)
    private static void prepareChannel(Context context, String id, int importance) {
        final String appName = context.getString(R.string.app_name);
        String description = "Online";
        final NotificationManager nm = (NotificationManager) context.getSystemService(Activity.NOTIFICATION_SERVICE);

        if (nm != null) {
            NotificationChannel nChannel = nm.getNotificationChannel(id);

            if (nChannel == null) {
                nChannel = new NotificationChannel(id, appName, importance);
                nChannel.setDescription(description);
                nm.createNotificationChannel(nChannel);
            }
        }
    }

}
