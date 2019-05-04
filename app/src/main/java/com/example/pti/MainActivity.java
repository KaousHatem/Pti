package com.example.pti;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.provider.Settings;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.example.pti.model.Contact;
import com.example.pti.model.Duration;
import com.example.pti.service.FloatingWidgetService;
import com.example.pti.service.SendSmsService;
import com.example.pti.service.SensorDetectionService;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import static android.Manifest.permission.SEND_SMS;

public class MainActivity extends AppCompatActivity {

    public static MainActivity instance;

    private ImageView btn_setting, btn_alert;
    private Switch switch_mode;

    private ImageView man_down_icon,man_icon,sos_icon,man_down_icon_dark,man_icon_dark,sos_icon_dark;
    private TextView textViewVerticality,textViewImmobility,textViewManuelle;

    private final String CHANNEL_ID = "mode_active_notification";
    private final int NOTIFICATION_ID = 100;

    private static final int DRAW_OVER_APP_PERMISSION = 101;
    private static final int REQUEST_SMS = 102;
    private static final int LOCATION_PERMISSION = 103;

    private AlertDialog.Builder builderLcoation,builderModeDetection;
    private AlertDialog dialogLocation,dialogModeDetection;

    private ProgressBar progressBar_sos_main;


    LocationManager locationManager;
    LocationListener locationListener;

    private boolean appInTop = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        instance = this;

        locationManager = (LocationManager)getApplicationContext().getSystemService(Context.LOCATION_SERVICE);

        build_create_dialog_location();

        // ask for permission
        askForSystemOverlayPermission();
        askForSendSmsPermission();

        // set up preference of the application
        setModePreference();
        setUpDuration();
        setSensibilityPrefrence();

        //dont show floating btn of sos
        stopService(new Intent(MainActivity.this,FloatingWidgetService.class));

        progressBar_sos_main = findViewById(R.id.progressBar_sos_main);

        // init btn
        switch_mode = findViewById(R.id.switch_mode);
        btn_setting = findViewById(R.id.btn_setting);
        btn_alert = findViewById(R.id.btn_alert);

        // bottom side init
        man_down_icon = findViewById(R.id.man_down_icon);
        man_icon = findViewById(R.id.man_icon);
        sos_icon = findViewById(R.id.sos_icon);
        man_down_icon_dark = findViewById(R.id.man_down_icon_dark);
        man_icon_dark = findViewById(R.id.man_icon_dark);
        sos_icon_dark = findViewById(R.id.sos_icon_dark);

        textViewVerticality = findViewById(R.id.textViewVerticality);
        textViewImmobility = findViewById(R.id.textViewImmobility);
        textViewManuelle = findViewById(R.id.textViewManuelle);

        // init mode detection _ btn and notification
        iniSwitchButton();
        turnModeNotifiaction();

        // Switch btn mode alerte
        switch_mode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!checkContacts()) {
                    switch_mode.setChecked(false);
                    Toast.makeText(MainActivity.this, "Veuillez ajouter au moins un contact d'urgence", Toast.LENGTH_SHORT).show();

                }
            }
        });

        switch_mode.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                turnOnMode(isChecked);
                turnModeNotifiaction();

            }
        });

        // setting btn
        btn_setting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                appInTop = true;
                Intent intent = new Intent(MainActivity.this, SettingActivity.class);
                startActivity(intent);
            }
        });

        // alert btn
        btn_alert.setOnTouchListener(new View.OnTouchListener() {
            CountDownTimer count;
            @Override
            public boolean onTouch(View v, MotionEvent motionEvent) {

                SharedPreferences preferences = getApplicationContext()
                        .getSharedPreferences(getApplicationContext().getResources().getString(R.string.pref_key_duration),getApplicationContext().MODE_PRIVATE);

                switch (motionEvent.getAction()){
                    case MotionEvent.ACTION_DOWN:
                        if (checkContacts()==false){
                            Toast.makeText(MainActivity.this, "Veuillez ajouter au moins un contact d'urgence", Toast.LENGTH_SHORT).show();
                            return false;
                        }

                        final Duration duration = new Duration(0,
                                preferences.getInt(getApplicationContext().getResources().getString(R.string.key_press_btn),-1));

                        progressBar_sos_main.setMax(duration.getSecond()*1000);

                        count = new CountDownTimer(duration.getSecond()*1000, 1) {

                            public void onTick(long millisUntilFinished) {
                                //Toast.makeText(FloatingWidgetService.this, ""+millisUntilFinished, Toast.LENGTH_SHORT).show();

                                progressBar_sos_main.setProgress(progressBar_sos_main.getMax()-(int)millisUntilFinished);
                                //here you can have your logic to set text to edittext
                            }

                            public void onFinish() {
                                startService(new Intent(MainActivity.this, SendSmsService.class));

                                progressBar_sos_main.setProgress(0);

                            }


                        }.start();


                        return true;

                    case MotionEvent.ACTION_UP:
                        if (count != null){
                            count.cancel();
                        }


                        progressBar_sos_main.setProgress(0);

                        return true;

                }
                return false;
            }
        });


    }

    private void build_create_dialog_location() {
        builderLcoation = new AlertDialog.Builder(this);
        builderLcoation.setMessage("Veuillez activer le service de localisation");
        builderLcoation.setPositiveButton("Activer", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivityForResult(intent,LOCATION_PERMISSION);
            }
        }).setNegativeButton("Annuler", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        });

        dialogLocation = builderLcoation.create();
        dialogLocation.setCanceledOnTouchOutside(false);
    }

    private boolean checkContacts(){
        SharedPreferences contact_pref = getApplicationContext().getSharedPreferences(getApplicationContext().getResources().getString(R.string.pref_key_contact),getApplicationContext().MODE_PRIVATE);
        List<Contact> contactList = new ArrayList<Contact>();
        String serializedContact = contact_pref.getString(getApplicationContext().getResources().getString(R.string.key_contact),null);
        if (serializedContact != null){
            Gson gson = new Gson();
            Type type = new TypeToken<List<Contact>>(){}.getType();
            contactList = gson.fromJson(serializedContact,type);
        }
        if (contactList.size() == 0){
            return false;
        }
        return true;
    }

    private void askForSendSmsPermission(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            if (!checkSmsPermission()) requestPermissions(new String[]{SEND_SMS},REQUEST_SMS);
        }else{
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_PHONE_STATE}, REQUEST_SMS);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case REQUEST_SMS:
                if (grantResults.length >0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    Toast.makeText(getApplicationContext(), "Sms Permission Granted, Now you can access sms", Toast.LENGTH_SHORT).show();
                }else{
                    Toast.makeText(getApplicationContext(), "Sms Permission Denied, You cannot access and sms", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            case LOCATION_PERMISSION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(getApplicationContext(), "Location Permission Granted, Now you can access location", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Location Permission denied", Toast.LENGTH_SHORT).show();
                }
                break;
            }
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }


    }

    private boolean checkSmsPermission() {
        return ( ContextCompat.checkSelfPermission(getApplicationContext(), SEND_SMS ) == PackageManager.PERMISSION_GRANTED);
    }

    public boolean checkLocationPermission() {
        return (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED);
    }

    private void askForSystemOverlayPermission() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {

            //If the draw over permission is not available open the settings screen
            //to grant the permission.
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, DRAW_OVER_APP_PERMISSION);
        }
    }

    private void askForSystemGps(){
        if (!checkLocationPermission()) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION);
        }

        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
            if (!dialogLocation.isShowing()) dialogLocation.show();
        }




    }

    private void setLocationListener(){
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {

            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {
                if (dialogLocation != null) if (dialogLocation.isShowing()) dialogLocation.dismiss();
                Log.i("TAG", "onStatusChanged:1 ");
            }

            @Override
            public void onProviderDisabled(String provider) {
                Log.i("TAG", "onStatusChanged:2 ");
                if (dialogLocation != null) if (!dialogLocation.isShowing()) dialogLocation.show();
            }
        };
        if (checkLocationPermission()){
            if (locationManager != null){
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,0,locationListener);
            }

        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //Toast.makeText(this, ""+requestCode, Toast.LENGTH_LONG).show();
        if (requestCode == DRAW_OVER_APP_PERMISSION) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!Settings.canDrawOverlays(this)) {
                    //Permission is not available. Display error text.
                    Toast.makeText(this, "Draw over other app permission not available. Can't start the application without the permission.", Toast.LENGTH_LONG).show();
                    finish();
                }
            }
        } else if (requestCode == LOCATION_PERMISSION){
            locationManager = (LocationManager)getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
                Toast.makeText(this, "Location Mode is not active. Can't start the application without the location mode active.", Toast.LENGTH_LONG).show();
                finish();
            }else{
                Toast.makeText(this, "Location Mode active", Toast.LENGTH_LONG).show();
            }

        }else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(getApplicationContext().getResources().getString(R.string.pref_key_mode_alerte),getApplicationContext().MODE_PRIVATE);

        SharedPreferences preferences =  getApplicationContext().getSharedPreferences(getApplicationContext().getResources().getString(R.string.pref_key_mode_alerte),getApplicationContext().MODE_PRIVATE);
        if (preferences.getInt(getString(R.string.pref_key_active_mode),-1)==1){
            if (sharedPref.getInt(getString(R.string.key_manuelle_alarme),1)==1){
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(MainActivity.this)){

                    Intent intent = new Intent(MainActivity.this, FloatingWidgetService.class);
                    intent.putExtra("activity_background", true);
                    if (!appInTop){
                        startService(intent);
                    }

                }else {
                    Toast.makeText(MainActivity.this, "Draw over other app permission not available. Can't start the application without the permission.", Toast.LENGTH_LONG).show();
                }
            }

        }else{
            stopService(new Intent(getApplicationContext(), FloatingWidgetService.class));
        }

        Log.i("TAG", "onStatusChanged onPause: "+String.valueOf(locationListener != null));
        if (locationListener != null) if (checkLocationPermission()) locationManager.removeUpdates(locationListener);

    }


    @Override
    protected void onResume() {
        super.onResume();
        appInTop = false;
        setLocationListener();
        askForSystemGps();
        stopService(new Intent(getApplicationContext(), FloatingWidgetService.class));
        initIconModeAlert();

    }

    private void initIconModeAlert(){
        SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(getApplicationContext().getResources().getString(R.string.pref_key_mode_alerte),getApplicationContext().MODE_PRIVATE);
        if (sharedPref.getInt(getString(R.string.key_verticality),1)==1){
            man_down_icon.setVisibility(View.INVISIBLE);
            man_down_icon_dark.setVisibility(View.VISIBLE);
            textViewVerticality.setTextColor(getApplicationContext().getResources().getColor(R.color.checked));
        }else {
            man_down_icon.setVisibility(View.VISIBLE);
            man_down_icon_dark.setVisibility(View.INVISIBLE);
            textViewVerticality.setTextColor(getApplicationContext().getResources().getColor(R.color.unchecked));
        }
        if (sharedPref.getInt(getString(R.string.key_immobility),1)==1){
            man_icon.setVisibility(View.INVISIBLE);
            man_icon_dark.setVisibility(View.VISIBLE);
            textViewImmobility.setTextColor(getApplicationContext().getResources().getColor(R.color.checked));
        }else {
            man_icon.setVisibility(View.VISIBLE);
            man_icon_dark.setVisibility(View.INVISIBLE);
            textViewImmobility.setTextColor(getApplicationContext().getResources().getColor(R.color.unchecked));
        }
        if (sharedPref.getInt(getString(R.string.key_manuelle_alarme),1)==1){
            sos_icon.setVisibility(View.INVISIBLE);
            sos_icon_dark.setVisibility(View.VISIBLE);
            textViewManuelle.setTextColor(getApplicationContext().getResources().getColor(R.color.checked));
        }else {
            sos_icon.setVisibility(View.VISIBLE);
            sos_icon_dark.setVisibility(View.INVISIBLE);
            textViewManuelle.setTextColor(getApplicationContext().getResources().getColor(R.color.unchecked));
        }
    }

    private void iniSwitchButton() {
        SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(getApplicationContext().getResources().getString(R.string.pref_key_mode_alerte),getApplicationContext().MODE_PRIVATE);

        if (sharedPref.getInt(getString(R.string.pref_key_active_mode),-1)==1){
            switch_mode.setChecked(true);
        }else{
            switch_mode.setChecked(false);
        }
    }


    public void setModePreference(){
        SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(getApplicationContext().getResources().getString(R.string.pref_key_mode_alerte),getApplicationContext().MODE_PRIVATE);

        SharedPreferences.Editor editor = sharedPref.edit();
        if (sharedPref.contains(getString(R.string.key_verticality)) == false){
            editor.putInt(getString(R.string.key_verticality), 1);
            editor.commit();
        }
        if (sharedPref.contains(getString(R.string.key_immobility)) == false){
            editor.putInt(getString(R.string.key_immobility), 1);
            editor.commit();
        }
        if (sharedPref.contains(getString(R.string.key_manuelle_alarme)) == false){
            editor.putInt(getString(R.string.key_manuelle_alarme), 1);
            editor.commit();
        }
        if (sharedPref.contains(getString(R.string.pref_key_active_mode))==false){
            editor.putInt(getString(R.string.pref_key_active_mode),0);
            editor.commit();
        }

    }

    public void setSensibilityPrefrence(){
        SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(getApplicationContext().getResources().getString(R.string.pref_key_sensibility),getApplicationContext().MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        if (sharedPref.contains(getString(R.string.key_sensiblity_verticality)) == false){
            editor.putInt(getString(R.string.key_sensiblity_verticality),45);
        }
        if (sharedPref.contains(getString(R.string.key_sensiblity_immobility)) == false){
            editor.putFloat(getString(R.string.key_sensiblity_immobility),2f);
        }
        editor.commit();

    }

    public void setUpDuration(){
        SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(getApplicationContext().getResources().getString(R.string.pref_key_duration),getApplicationContext().MODE_PRIVATE);
        if (sharedPref.contains(getApplicationContext().getResources().getString(R.string.key_pre_alerte_m)) == false){
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putInt(getString(R.string.key_pre_alerte_m), 0);
            editor.commit();
        }
        if (sharedPref.contains(getApplicationContext().getResources().getString(R.string.key_pre_alerte_s)) == false){
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putInt(getString(R.string.key_pre_alerte_s), 15);
            editor.commit();
        }

        if (sharedPref.contains(getApplicationContext().getResources().getString(R.string.key_pre2_alerte_m)) == false){
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putInt(getString(R.string.key_pre2_alerte_m), 0);
            editor.commit();
        }
        if (sharedPref.contains(getApplicationContext().getResources().getString(R.string.key_pre2_alerte_s)) == false){
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putInt(getString(R.string.key_pre2_alerte_s), 30);
            editor.commit();
        }
        if (sharedPref.contains(getApplicationContext().getResources().getString(R.string.key_press_btn)) == false){
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putInt(getString(R.string.key_press_btn), 5);
            editor.commit();
        }

    }

    private void turnOnMode(boolean isChecked){
        SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(getApplicationContext().getResources().getString(R.string.pref_key_mode_alerte),getApplicationContext().MODE_PRIVATE);

        if (isChecked){
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putInt(getString(R.string.pref_key_active_mode),1);
            editor.commit();

            Intent intent = new Intent(getApplicationContext(), SensorDetectionService.class);
            intent.putExtra("immobilityMode",(sharedPref.getInt(getString(R.string.key_immobility),-1)==1));
            intent.putExtra("verticalityMode",(sharedPref.getInt(getString(R.string.key_verticality),-1)==1));
            startService(intent);
        }else {
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putInt(getString(R.string.pref_key_active_mode),0);
            editor.commit();
            stopService(new Intent(getApplicationContext(),SensorDetectionService.class));

        }

    }

    private void notificationOn() {
        createNotificationChannel();
        Intent homeIntent = new Intent(this,MainActivity.class);
        homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(this,0,homeIntent,0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this,CHANNEL_ID);
        builder.setSmallIcon(R.drawable.ic_action_power);
        builder.setContentTitle("Mode PIT est active");
        builder.setContentText("Le mode de detection est mode active");
        builder.setPriority(NotificationCompat.PRIORITY_DEFAULT);
        builder.setContentIntent(pendingIntent);
        builder.setOngoing(true);

        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(this);
        notificationManagerCompat.notify(NOTIFICATION_ID,builder.build());
    }

    private void createNotificationChannel(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    public void notificationOff() {
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.deleteNotificationChannel(CHANNEL_ID);

        }else{
            notificationManager.cancel(NOTIFICATION_ID);
        }
    }

    private void turnModeNotifiaction(){

        SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(getApplicationContext().getResources().getString(R.string.pref_key_mode_alerte),getApplicationContext().MODE_PRIVATE);

        if (sharedPref.getInt(getString(R.string.pref_key_active_mode),-1)==0){
            notificationOff();
        }else{
            notificationOn();
        }
    }

}
