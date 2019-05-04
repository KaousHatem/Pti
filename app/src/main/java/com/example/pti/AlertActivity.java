package com.example.pti;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.app.ActivityManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.pti.service.DetectionModeService;
import com.example.pti.service.FloatingWidgetService;

public class AlertActivity extends AppCompatActivity {

    private final String CHANNEL_ID = "prealarm_notification";
    private final int NOTIFICATION_ID = 002;


    public static AlertActivity instance;
    Button btn_cancel_alert;
    TextView textViewCondition;
    private boolean appInTop = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_alert);
        instance = AlertActivity.this;
        notificationOn();
        stopServiceFloatingWidgetService();

        textViewCondition = findViewById(R.id.textViewCondition);

        setTextCondition();

        final TextView textView = findViewById(R.id.textView_countDown_prealert);
        LocalBroadcastManager.getInstance(this).registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                long millis = intent.getLongExtra(DetectionModeService.MILLIS,0);
                int min,sec;
                min = (int) millis / 60;
                sec = (int) millis % 60;
                String zero = "";
                if (sec<10) zero = "0";
                textView.setText(min+":"+zero+sec);
            }
        },new IntentFilter(DetectionModeService.ACTION_ALERT_BROADCAST));

        btn_cancel_alert = findViewById(R.id.btn_cancel_alert);
        btn_cancel_alert.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopService(new Intent(AlertActivity.this,DetectionModeService.class));
                //stopService(new Intent(AlertActivity.this, SensorDetectionService.class));
                onBackPressed();
                finish();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        boolean check=false;
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (DetectionModeService.class.getName().equals(service.service.getClassName())) {
                check=true;
            }
        }
        if (!check){
            notificationOff();
        }

    }

    private void stopServiceFloatingWidgetService(){
        stopService(new Intent(AlertActivity.this, FloatingWidgetService.class));
    }

    private void startServiceFloatingWidgetService(){
        SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(getApplicationContext().getResources().getString(R.string.pref_key_mode_alerte),getApplicationContext().MODE_PRIVATE);

        SharedPreferences preferences =  getApplicationContext().getSharedPreferences(getApplicationContext().getResources().getString(R.string.pref_key_mode_alerte),getApplicationContext().MODE_PRIVATE);
        if (preferences.getInt(getString(R.string.pref_key_active_mode),-1)==1){
            if (sharedPref.getInt(getString(R.string.key_manuelle_alarme),1)==1){
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(AlertActivity.this)){

                    Intent intent = new Intent(AlertActivity.this, FloatingWidgetService.class);
                    intent.putExtra("activity_background", true);
                    startService(intent);

                }else {
                    Toast.makeText(AlertActivity.this, "Draw over other app permission not available. Can't start the application without the permission.", Toast.LENGTH_LONG).show();
                }
            }

        }
    }

    private void setTextCondition(){
        Intent intent = getIntent();
        textViewCondition.setText("En raison "+intent.getStringExtra("TYPE_SOS"));
    }

    private void notificationOn() {
        createNotificationChannel();
        Intent homeIntent = new Intent(this,AlertActivity.class);
        homeIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        homeIntent.putExtra("TYPE_SOS",getIntent().getStringExtra("TYPE_SOS"));
        //homeIntent.setAction("ok");

        PendingIntent pendingIntent = PendingIntent.getActivity(this,0,homeIntent,PendingIntent.FLAG_UPDATE_CURRENT);


        NotificationCompat.Builder builder = new NotificationCompat.Builder(this,CHANNEL_ID);
        builder.setSmallIcon(R.drawable.ic_action_prealarme);
        builder.setContentTitle("Préalarme notificaiont");
        builder.setContentText("La préalarme est déclanchée");
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

    @Override
    protected void onPause() {
        super.onPause();

        startServiceFloatingWidgetService();

    }

    @Override
    protected void onResume() {
        super.onResume();
        stopServiceFloatingWidgetService();
    }


}
