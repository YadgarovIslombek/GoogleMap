package com.ervinod.googlemaproute;

import android.content.Context;
import android.util.Log;
import org.json.JSONArray;


/**
 * Created by DELL on 13-10-2017.
 */

public class LocationPublishHelper {
    private Context context;
    private boolean routeSent, hitInProgress;
    private JSONArray routeArray;

    public LocationPublishHelper(Context context) {
        this.context = context;
    }

    public void publishLocation(double latitude, double longitude) {

        Log.d("Service", "Hit API");
        if (!hitInProgress) {

            try {

                hitInProgress = true;


            } catch (Exception e) {
                e.printStackTrace();
                hitInProgress = false;
            }
        }
    }
}
