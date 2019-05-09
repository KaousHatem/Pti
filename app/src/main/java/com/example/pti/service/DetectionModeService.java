package com.example.pti.service;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.location.Location;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.pti.AlertActivity;
import com.example.pti.R;
import com.example.pti.listener.LocationAlertListener;
import com.example.pti.listener.LocationUpdateListener;
import com.example.pti.model.Contact;
import com.example.pti.model.Duration;
import com.example.pti.utils.Config;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import static android.Manifest.permission.SEND_SMS;

public class DetectionModeService extends LocationAlertListener {

    CountDownTimer count_2;
    public static final String ACTION_ALERT_BROADCAST = DetectionModeService.class.getName() + "CountDownAlert",
            MILLIS = "time_left";
    public SharedPreferences preferences;
    private boolean threadRunning = false;

    private Location lastLocation;
    LocationUpdateListener locationUpdateListener;

    private final int VERTICALITY_CODE=101;
    private final int IMMOBILITY_CODE=102;
    private final int SOS_CODE=103;

    private int SENSOR_DETECTION_CODE=0;

    private String type_sos_message = "";
    private String type_sos_alert = "";

    private final String CHANNEL_ID = "alarme envoyé";
    private final int NOTIFICATION_ID = 103;

    Uri uri;
    Ringtone ringtone = null;

    private BroadcastReceiver sentStatusReceiver, deliveredStatusReceiver;

    public DetectionModeService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent != null) {
            SENSOR_DETECTION_CODE = intent.getIntExtra("SENSOR_DETECTION_CODE", 0);
        }

        broadcast();

        getlocation();



        switch (SENSOR_DETECTION_CODE){
            case IMMOBILITY_CODE:{
                type_sos_alert = "d'immobilité";
                break;
            }
            case VERTICALITY_CODE:{
                type_sos_alert = "de perte de verticalité";
                break;
            }
            default:
                break;
        }


        final Duration duration = new Duration(preferences.getInt(getApplicationContext().getResources().getString(R.string.key_pre_alerte_m),-1),
                preferences.getInt(getApplicationContext().getResources().getString(R.string.key_pre_alerte_s),-1));



        if (getApplicationContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)){
            new Thread(new Runnable() {
                @Override
                public void run() {
                    blinkFlash();
                }
            }).start();
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                playAudioRepeatly();
            }
        }).start();

        long countTimer = duration.getMinute()*60*1000+duration.getSecond()*1000;

        count_2 = new CountDownTimer(countTimer,1000){
            @Override
            public void onTick(long millisUntilFinished) {
                sendBroadCastMessage(millisUntilFinished/1000);
            }

            @Override
            public void onFinish() {
                threadRunning = false;
                send_alert_sms();
                stopSelf();
                //Toast.makeText(DetectionModeService.this, ""+textView.getText(), Toast.LENGTH_SHORT).show();
            }

        };
        Intent intent1 = new Intent(DetectionModeService.this, AlertActivity.class);
        intent1.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent1.putExtra("TYPE_SOS",type_sos_alert);
        count_2.start();
        startActivity(intent1);






        return super.onStartCommand(intent, flags, startId);


    }


    private void getlocation(){
        locationUpdateListener = new LocationUpdateListener() {
            @Override
            public void onLocationUpdated(Location location) {
                Config.getInstance().setCurrentLatitude("" + location.getLatitude());
                Config.getInstance().setCurrentLongitude("" + location.getLongitude());

                Log.i("TAG", "onLocationChanged: LATITUDE : " + location.getLatitude());
                Log.i("TAG", "onLocationChanged: LONGITUDE : " + location.getLongitude());
                lastLocation=location;

            }
        };
        addLocationUpdateListener(locationUpdateListener);

    }

    private void send_alert_sms(){
        SharedPreferences contact_pref = getApplicationContext().getSharedPreferences(getApplicationContext().getResources().getString(R.string.pref_key_contact),getApplicationContext().MODE_PRIVATE);
        List<Contact> contactList = new ArrayList<Contact>();
        String serializedContact = contact_pref.getString(getApplicationContext().getResources().getString(R.string.key_contact),null);
        if (serializedContact != null){
            Gson gson = new Gson();
            Type type = new TypeToken<List<Contact>>(){}.getType();
            contactList = gson.fromJson(serializedContact,type);
        }
        // sendSms();
        switch (SENSOR_DETECTION_CODE){
            case IMMOBILITY_CODE:{
                type_sos_message = "Immobilité";
                break;
            }
            case VERTICALITY_CODE:{
                type_sos_message = "Perte de verticalité";
                break;
            }
            default:
                break;
        }

        if (checkPermission()){
            for(int i=0;i<contactList.size();i++){
                sendSms(contactList.get(i));
            }

        }

    }

    private void sendSms(Contact contact){
        String messageText;
        if (Config.getInstance().getCurrentLatitude()!=null && Config.getInstance().getCurrentLongitude()!=null ){
            messageText = "C'est un messsage d'alerte de type: "+type_sos_message+".\r\nInformation sur la position:\nLatitude: "+Config.getInstance().getCurrentLatitude()+"\nLongitude: "+Config.getInstance().getCurrentLongitude()+"\nLe lien sur google Map: https://www.google.com/maps/search/?api=1&query="+Config.getInstance().getCurrentLatitude()+","+Config.getInstance().getCurrentLongitude();
        }else {
            messageText = "C'est un messsage d'alerte de type: "+type_sos_message+".\r\nInformation sur la position:inconnu";
        }
        ArrayList<PendingIntent> sentPendingIntents = new ArrayList<PendingIntent>();
        ArrayList<PendingIntent> deliveredPendingIntents = new ArrayList<PendingIntent>();
        String number = contact.getNumber();
        SmsManager smsManager = SmsManager.getDefault();
        ArrayList<String> parts = smsManager.divideMessage(messageText);
        PendingIntent sentIntent = PendingIntent.getBroadcast(this,0,new Intent("SENT_SMS"),0);
        PendingIntent deliveredIntent = PendingIntent.getBroadcast(this,0,new Intent("SMS_DELIVERED"),0);
        try{
            for (int i =0;i<parts.size();i++){
                sentPendingIntents.add(sentIntent);
                //deliveredPendingIntents.add(deliveredIntent);
            }
            smsManager.sendMultipartTextMessage(number,null, parts,sentPendingIntents,null);
            Log.i("TAG", "onLocationChanged: sent");
            notificationOn(1);
            //Toast.makeText(this, "sms sent", Toast.LENGTH_SHORT).show();
        }catch (Exception e){
            e.printStackTrace();
            notificationOn(0);
            //Toast.makeText(this, "sms not sent", Toast.LENGTH_SHORT).show();
        }

    }

    private boolean checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            return (ContextCompat.checkSelfPermission(getApplicationContext(), SEND_SMS ) == PackageManager.PERMISSION_GRANTED);
        }else{
            return (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED);
        }
    }

    private void broadcast(){
        sentStatusReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String s = "Unkown Error";
                switch (getResultCode()) {
                    /*case Activity.RESULT_OK:
                        s = "Message Sent Successfully !!";
                        notificationOn(1);
                        break;
                        */
                    case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                        s = "Generic Failure Error";
                        notificationOn(0);
                        Toast.makeText(DetectionModeService.this, ""+s, Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_NO_SERVICE:
                        s = "Error : No Service Available";
                        notificationOn(0);
                        Toast.makeText(DetectionModeService.this, ""+s, Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_NULL_PDU:
                        s = "Error : Null PDU";
                        notificationOn(0);
                        Toast.makeText(DetectionModeService.this, ""+s, Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_RADIO_OFF:
                        s = "Error : Radio is off";
                        notificationOn(0);
                        Toast.makeText(DetectionModeService.this, ""+s, Toast.LENGTH_SHORT).show();
                        break;
                    default:
                        break;
                }
                Log.i("TAG", "broadcast: sentStatusReceiver "+getResultCode() );


            }
        };
        /*deliveredStatusReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String s = "Message Not Delivered";
                switch(getResultCode()) {
                    case Activity.RESULT_OK:
                        s = "Message Delivered Successfully";
                        break;
                    case Activity.RESULT_CANCELED:
                        break;
                }
                Log.i("TAG", "broadcast: deliveredStatusReceiver "+getResultCode() );
                Toast.makeText(SendSmsService.this, ""+s, Toast.LENGTH_SHORT).show();
            }
        };*/
        Log.i("TAG", "broadcast: running");
        registerReceiver(sentStatusReceiver,new IntentFilter("SENT_SMS"));
        /*registerReceiver(deliveredStatusReceiver,new IntentFilter("SMS_DELIVERED"));*/

    }


    private void playAudioRepeatly(){
        try {
            //uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            uri = Uri.parse("android.resource://"+getPackageName()+"/raw/alarm_alert");

            RingtoneManager.setActualDefaultRingtoneUri(
                    getApplicationContext(), RingtoneManager.TYPE_RINGTONE,
                    uri);
            ringtone =RingtoneManager.getRingtone(getApplicationContext(),uri);
            ringtone.play();
            while (true){
                if (!threadRunning){
                    ringtone.stop();
                    break;
                }else {
                    if (!ringtone.isPlaying()){
                        ringtone.play();
                    }
                }

            }

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void blinkFlash() {
        threadRunning = true;
        CameraManager cameraManager = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

            String myString = "01";
            int i = 0;
            long blinkDelay = 100; //Delay in ms
            while (threadRunning) {
                if (myString.charAt(i) == '0') {
                    try {
                        String cameraId = cameraManager.getCameraIdList()[0];
                        cameraManager.setTorchMode(cameraId, true);
                        i = 1;
                    } catch (CameraAccessException e) {
                    }
                } else {
                    try {
                        String cameraId = cameraManager.getCameraIdList()[0];
                        cameraManager.setTorchMode(cameraId, false);
                        i = 0;
                    } catch (CameraAccessException e) {
                    }
                }
                try {
                    Thread.sleep(blinkDelay);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            try {
                String cameraId = cameraManager.getCameraIdList()[0];
                cameraManager.setTorchMode(cameraId, false);
                i = 0;
            } catch (CameraAccessException e) {
            }


        }
    }



    private void sendBroadCastMessage(long millis){
        if (millis != 0){
            Intent intent = new Intent(ACTION_ALERT_BROADCAST);
            intent.putExtra(MILLIS,millis);
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        preferences = getApplicationContext().getSharedPreferences(getApplicationContext().getResources().getString(R.string.pref_key_duration),getApplicationContext().MODE_PRIVATE);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        count_2.cancel();
        AlertActivity activity = AlertActivity.instance;
        activity.notificationOff();
        activity.finish();
        SensorDetectionService service = SensorDetectionService.instance;
        if (service.count_2 != null){
            service.count_2.cancel();
            service.count_2 = null;
        }
        if (service.count != null){
            service.count.cancel();
            service.count = null;
        }
        //stopService(new Intent(DetectionModeService.this, SensorDetectionService.class));

        threadRunning = false;
        unregisterReceiver(sentStatusReceiver);
        //unregisterReceiver(deliveredStatusReceiver);

        removeLocationUpdateListener(locationUpdateListener);

    }

    private void notificationOn(int result) {
        createNotificationChannel();


        NotificationCompat.Builder builder = new NotificationCompat.Builder(this,CHANNEL_ID);
        builder.setContentTitle("Sms Alarme");
        if (result == 0){
            builder.setSmallIcon(R.drawable.ic_action_fail);

            builder.setContentText("Le Message d'alarme c'est pas envoyé!");
        }else{
            builder.setSmallIcon(R.drawable.ic_action_success);
            builder.setContentText("Le Message d'alarme est envoyé.");
        }

        builder.setPriority(NotificationCompat.PRIORITY_DEFAULT);

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
}
