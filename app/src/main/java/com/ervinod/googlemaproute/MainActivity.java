package com.ervinod.googlemaproute;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Point;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.ahmadrosid.lib.drawroutemap.DrawMarker;
import com.ahmadrosid.lib.drawroutemap.DrawRouteMaps;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class MainActivity extends FragmentActivity implements LocationListener, OnMapReadyCallback {

    GoogleMap mGoogleMap;

    double mLatitude = 0.0;
    double mLongitude = 0.0;

    double destinationLatitude = 0.0;
    double destinationLongitude = 0.0;
    Marker myMarker = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (getIntent().getExtras() != null) {
            String latitude = getIntent().getStringExtra("LAT");
            String longitude = getIntent().getStringExtra("LNG");
            if (latitude != null) {
                destinationLatitude = Double.parseDouble(latitude);
            }

            if (longitude != null) {
                destinationLongitude = Double.parseDouble(longitude);
            }
        } else {
            destinationLatitude = 20.902901;
            destinationLongitude = 74.775139;
        }


        // Getting Google Play availability status
        int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(getBaseContext());

        if (status != ConnectionResult.SUCCESS) { // Google Play Services are not available

            int requestCode = 10;
            Dialog dialog = GooglePlayServicesUtil.getErrorDialog(status, this, requestCode);
            dialog.show();

        } else { // Google Play Services are available

            // Getting reference to the SupportMapFragment
            SupportMapFragment fragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

            // Getting Google Map
            fragment.getMapAsync(this);

        }

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mGoogleMap = googleMap;

        listenLocation();
        //  mService.requestLocationUpdates();
        // for Map tool disable
        //mGoogleMap.getUiSettings().setMapToolbarEnabled(false);

        // for Zoom Button Enable on Google Map
        //mGoogleMap.getUiSettings().setZoomControlsEnabled(true);

        //for Location  Button enable on Google Map
        //mGoogleMap.getUiSettings().setMyLocationButtonEnabled(true);

  /*      //add custom marker to your location
        mGoogleMap.addMarker(new MarkerOptions().position(latLng).
                icon(BitmapDescriptorFactory.fromBitmap(
                        createCustomMarker(LocationRouteActivity.this, R.drawable.ic_my_location, "Vinod")))).setTitle("Your Location");

        mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mGoogleMap.animateCamera(CameraUpdateFactory.zoomTo(12));
*/

        /*LatLng origin = new LatLng(20.904221, 74.774895);
        LatLng destination = new LatLng(20.902901, 74.775139);

        DrawRouteMaps.getInstance(LocationRouteActivity.this, GOOGLE_API_KEY)
                .draw(origin, destination, mGoogleMap);
        DrawMarker.getInstance(this).draw(mGoogleMap, origin, R.drawable.ic_source, "Your Location");
        DrawMarker.getInstance(this).draw(mGoogleMap, destination, R.drawable.ic_destination, "Destination Location");

        LatLngBounds bounds = new LatLngBounds.Builder()
                .include(origin)
                .include(destination).build();
        Point displaySize = new Point();
        getWindowManager().getDefaultDisplay().getSize(displaySize);
        mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, displaySize.x, 350, 30));*/

        LatLng origin = new LatLng(20.904221, 74.774895);
        //LatLng destination = new LatLng(20.902901, 74.775139);
        LatLng destination = new LatLng(destinationLatitude, destinationLongitude);
        plotLocation(origin, destination);


        if (Build.VERSION.SDK_INT >= 23 && (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)) {
            showPermissionDialog();
            return;
        }

        mGoogleMap.setMyLocationEnabled(true);


        // Getting LocationManager object from System Service LOCATION_SERVICE
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        // Creating a criteria object to retrieve provider
        Criteria criteria = new Criteria();

        // Getting the name of the best provider
        String provider = locationManager.getBestProvider(criteria, true);

        // Getting Current Location From GPS
        Location location = locationManager.getLastKnownLocation(provider);

        if (location != null) {
            //onLocationChanged(location);
        }

        locationManager.requestLocationUpdates(provider, 5000, 10, this);

    }



    public void updateMarkerLocation(final LatLng updatedLatLng) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                LatLng latLng = new LatLng(mLatitude, mLongitude);

                if (myMarker == null) {
                    MarkerOptions markerOptions = new MarkerOptions().position(updatedLatLng).icon(BitmapDescriptorFactory.fromBitmap(
                            createCustomMarker(MainActivity.this,
                                    R.drawable.ic_my_location, "Your Location")));

                    myMarker = mGoogleMap.addMarker(markerOptions);

                } else {
                    myMarker.setPosition(updatedLatLng);
                }
            }
        });

    }

    public void plotLocation(LatLng origin, LatLng destination) {
        if (mGoogleMap != null) {
            mGoogleMap.clear();

            //LatLng origin = new LatLng(20.904221, 74.774895);
            //LatLng destination = new LatLng(20.902901, 74.775139);

            DrawRouteMaps.getInstance(MainActivity.this, getResources().getString(R.string.google_map_api_key))
                    .draw(origin, destination, mGoogleMap);
            DrawMarker.getInstance(this).draw(mGoogleMap, origin, R.drawable.ic_source, "Your Location");
            DrawMarker.getInstance(this).draw(mGoogleMap, destination, R.drawable.ic_destination, "Destination Location");

            LatLngBounds bounds = new LatLngBounds.Builder()
                    .include(origin)
                    .include(destination).build();
            Point displaySize = new Point();
            getWindowManager().getDefaultDisplay().getSize(displaySize);

            mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, displaySize.x, 350, 30));


        }
    }

    //creating custom icon for marker to be shown on map
    public static Bitmap createCustomMarker(Context context, @DrawableRes int resource, String _name) {

        View marker = ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.custom_marker_layout, null);

        ImageView markerImage = (ImageView) marker.findViewById(R.id.user_marker);
        markerImage.setImageResource(resource);
        TextView txt_name = (TextView) marker.findViewById(R.id.title);
        txt_name.setText(_name);

        DisplayMetrics displayMetrics = new DisplayMetrics();
        ((Activity) context).getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        marker.setLayoutParams(new ViewGroup.LayoutParams(52, ViewGroup.LayoutParams.WRAP_CONTENT));
        marker.measure(displayMetrics.widthPixels, displayMetrics.heightPixels);
        marker.layout(0, 0, displayMetrics.widthPixels, displayMetrics.heightPixels);
        marker.buildDrawingCache();
        Bitmap bitmap = Bitmap.createBitmap(marker.getMeasuredWidth(), marker.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        marker.draw(canvas);

        return bitmap;
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    public void listenLocation() {
        if (!ServiceUtils.isMyServiceRunning(LocationForegroundService.class, this)) {
            Intent startIntent = new Intent(this, LocationForegroundService.class);
            startIntent.setAction("STARTFOREGROUND_ACTION");
            startService(startIntent);

        } else {
            Log.d("Service", "Already Running");
        }
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        mLatitude = location.getLatitude();
        mLongitude = location.getLongitude();
        LatLng sourceLatLng = new LatLng(mLatitude, mLongitude);
        LatLng destinationLatLng = new LatLng(destinationLatitude, destinationLongitude);
        //updateMarkerLocation(sourceLatLng);
        plotLocation(sourceLatLng, destinationLatLng);
    }

    public void showPermissionDialog() {
        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION}, 1);

    }

}