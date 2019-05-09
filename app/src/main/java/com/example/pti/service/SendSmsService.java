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
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.example.pti.R;
import com.example.pti.listener.LocationAlertListener;
import com.example.pti.listener.LocationUpdateListener;
import com.example.pti.model.Contact;
import com.example.pti.model.ObservableInteger;
import com.example.pti.utils.Config;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import static android.Manifest.permission.SEND_SMS;

public class SendSmsService extends LocationAlertListener {

    LocationUpdateListener locationUpdateListener;
    private BroadcastReceiver sentStatusReceiver, deliveredStatusReceiver;
    ObservableInteger obsInt;

    private final String CHANNEL_ID = "alarme envoyé";
    private final int NOTIFICATION_ID = 103;

    public static SendSmsService instance;

    public SendSmsService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        instance = this;
        broadcast();



        getlocation();

        obsInt = new ObservableInteger();
        obsInt.set(0);
        obsInt.setOnIntegerChangeListener(new ObservableInteger.OnIntegerChangeListener() {
            @Override
            public void onIntegerChanged(int newValue) {
                Log.i("TAG", "onIntegerChanged: "+newValue);
                if (newValue == 2){
                    send_alert_sms();

                    //stopSelf();
                }else if (newValue >1){
                    while (locationUpdateListener != null) {
                        removeLocationUpdateListener(locationUpdateListener);
                        locationUpdateListener = null;
                    }
                    stopSelf();
                }
            }
        });

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (locationUpdateListener != null) {
            removeLocationUpdateListener(locationUpdateListener);
            locationUpdateListener = null;
        }
        unregisterReceiver(sentStatusReceiver);
        //unregisterReceiver(deliveredStatusReceiver);
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

        if (checkPermission()){
            if (locationUpdateListener != null) removeLocationUpdateListener(locationUpdateListener);
            for(int i=0;i<contactList.size();i++){
                sendSms(contactList.get(i));
            }
        }

    }

    private void sendSms(Contact contact){
        String messageText;
        if (Config.getInstance().getCurrentLatitude()!=null && Config.getInstance().getCurrentLongitude()!=null ){
            messageText = "C'est un messsage d'alerte de type: AIDE SOS.\r\nInformation sur la position:\nLatitude: "+Config.getInstance().getCurrentLatitude()+"\nLongitude: "+Config.getInstance().getCurrentLongitude()+"\nLe lien sur google Map: https://www.google.com/maps/search/?api=1&query="+Config.getInstance().getCurrentLatitude()+","+Config.getInstance().getCurrentLongitude();
        }else {
            messageText = "C'est un messsage d'alerte de type: AIDE SOS.\r\nInformation sur la position:inconnu";
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
                sentPendingIntents.add(i,sentIntent);
                deliveredPendingIntents.add(i,deliveredIntent);
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
                        Toast.makeText(SendSmsService.this, ""+s, Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_NO_SERVICE:
                        s = "Error : No Service Available";
                        notificationOn(0);
                        Toast.makeText(SendSmsService.this, ""+s, Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_NULL_PDU:
                        s = "Error : Null PDU";
                        notificationOn(0);
                        Toast.makeText(SendSmsService.this, ""+s, Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_RADIO_OFF:
                        s = "Error : Radio is off";
                        notificationOn(0);
                        Toast.makeText(SendSmsService.this, ""+s, Toast.LENGTH_SHORT).show();
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

    private boolean checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            return (ContextCompat.checkSelfPermission(getApplicationContext(), SEND_SMS ) == PackageManager.PERMISSION_GRANTED);
        }else{
            return (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED);
        }
    }

    private void getlocation(){

        final int[] step = {0};
        locationUpdateListener = new LocationUpdateListener() {
            @Override
            public void onLocationUpdated(Location location) {
                Config.getInstance().setCurrentLatitude("" + location.getLatitude());
                Config.getInstance().setCurrentLongitude("" + location.getLongitude());
                step[0]++;
                Log.i("TAG", "onLocationChanged: LATITUDE : " + location.getLatitude());
                Log.i("TAG", "onLocationChanged: LONGITUDE : " + location.getLongitude());
                obsInt.set(step[0]);

            }
        };

        addLocationUpdateListener(locationUpdateListener);
    }

    private void notificationOn(int result) {
        createNotificationChannel();


        NotificationCompat.Builder builder = new NotificationCompat.Builder(this,CHANNEL_ID);
        builder.setContentTitle("Sms Alarme");
        if (result == 0){
            builder.setSmallIcon(R.drawable.ic_action_fail);

            builder.setContentText("Le Message d'alarme n'est pas envoyé!");
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
