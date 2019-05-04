package com.example.pti.listener;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import com.example.pti.utils.AppController;
import com.example.pti.utils.Config;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;

public class LocationAlertListener extends Service implements LocationListener {



    private static final LocationRequest mLocationRequest = LocationRequest.create()
            .setInterval(5000)         // 5 seconds
            .setFastestInterval(16)    // 16ms = 60fps
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationCallback locationCallback;
    private boolean isRequestingLocationUpdates;
    private ArrayList<LocationUpdateListener> locationUpdateListeners = new ArrayList<>();

    private Context context;
    public LocationAlertListener() {
        this.context = AppController.getInstance().getApplicationContext();
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(context);

        locationCallback = new LocationCallback() {

            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    setLocationUpdate(location);
                }
            }
        };

        getCurrentLocation();
    }

    @Override
    public void onCreate() {
        super.onCreate();

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void setLocationUpdate(Location location){
        if (locationUpdateListeners != null && !locationUpdateListeners.isEmpty()) {
            try{
                for (LocationUpdateListener locationUpdateListener : locationUpdateListeners) {
                    locationUpdateListener.onLocationUpdated(location);
                }
            }catch (Exception e){
                Log.e("TAG", "setLocationUpdate: "+e.getMessage(),e );
            }

        }
    }

    void getCurrentLocation() {



        if (ActivityCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

        } else {



            /*mFusedLocationClient.getLastLocation()
                    .addOnSuccessListener((Activity) context, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            if (location != null) {
                                // Logic to handle location object
                                setLocationUpdate(location);
                            }
                        }
                    });*/

            mFusedLocationClient.requestLocationUpdates(mLocationRequest,
                    locationCallback, null /* Looper */);

//            Toast.makeText(BaseAppCompatActivity.this, "Retrieving Current Location...", Toast.LENGTH_SHORT).show();
            LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, (android.location.LocationListener) this);
            } else {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, (android.location.LocationListener) this);
            }
            //			mHandler.postDelayed(periodicTask, 3000);
        }
    }

    public void addLocationUpdateListener(LocationUpdateListener listener) {
        isRequestingLocationUpdates = true;
        if (locationUpdateListeners == null) {
            locationUpdateListeners = new ArrayList<>();
        }
        locationUpdateListeners.add(listener);
    }

    public void removeLocationUpdateListener(LocationUpdateListener listener) {
        isRequestingLocationUpdates = false;
        if (locationUpdateListeners != null) {
            locationUpdateListeners.remove(listener);
        }

    }





    @Override
    public void onLocationChanged(Location location) {
        try {
            setLocationUpdate(location);
        } catch (Exception e) {
        }
        Config.getInstance().setCurrentLatitude("" + location.getLatitude());
        Config.getInstance().setCurrentLongitude("" + location.getLongitude());
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onProviderDisabled(String provider) {
    }




}