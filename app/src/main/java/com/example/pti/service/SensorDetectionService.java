package com.example.pti.service;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.util.Log;

import com.example.pti.R;
import com.example.pti.model.Duration;
import com.example.pti.utils.ImmobilityDetection;
import com.example.pti.utils.VerticalityDetection;

public class SensorDetectionService extends Service implements SensorEventListener {

    private ImmobilityDetection immobilityDetection;
    private VerticalityDetection verticalityDetection;

    private float mAccel;
    private float mAccelCurrent;
    private float mAccelLast;

    public CountDownTimer count,count_2;

    public SharedPreferences preferences,sensibilty_pref;

    private boolean immobilityMode,verticalityMode;

    private final int VERTICALITY_CODE=101;
    private final int IMMOBILITY_CODE=102;

    public static SensorDetectionService instance;


    public SensorDetectionService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent != null) {
            immobilityMode = intent.getBooleanExtra("immobilityMode", false);
            verticalityMode = intent.getBooleanExtra("verticalityMode", false);

        }
        preferences = getApplicationContext().getSharedPreferences(getApplicationContext().getResources().getString(R.string.pref_key_duration),getApplicationContext().MODE_PRIVATE);
        sensibilty_pref = getApplicationContext().getSharedPreferences(getApplicationContext().getResources().getString(R.string.pref_key_sensibility),getApplicationContext().MODE_PRIVATE);

        final Duration duration = new Duration(preferences.getInt(getApplicationContext().getResources().getString(R.string.key_pre2_alerte_m),-1),
                preferences.getInt(getApplicationContext().getResources().getString(R.string.key_pre2_alerte_s),-1));
        final long countTimer = duration.getMinute()*60*1000+duration.getSecond()*1000;

        if (immobilityMode) sensorImmobility(countTimer);
        if (verticalityMode) sensorVerticality(countTimer);


        return super.onStartCommand(intent, flags, startId);
    }

    public void sensorVerticality(final long countTimer){
        final int angle = sensibilty_pref.getInt(getResources().getString(R.string.key_sensiblity_verticality),5);
        verticalityDetection = new VerticalityDetection(getApplicationContext());
        verticalityDetection.register();
        verticalityDetection.setListener(new VerticalityDetection.VerticalityDetectionListener() {
            @Override
            public void senRotation(float tx, float ty, float tz) {
                float norm_Of_g = (float) Math.sqrt(tx * tx + ty * ty + tz * tz);
                ty = ty / norm_Of_g;

                int inclination = (int) Math.round(Math.toDegrees(Math.acos(ty)));
                //int rotation = (int) Math.round(Math.toDegrees(Math.atan2(tx,tz)));


                if (inclination >angle && inclination <180-angle){
                    Log.i("rotation", "setinclination: "+inclination);
                    if (count_2 == null){
                        count_2 = new CountDownTimer(countTimer,1000) {
                            @Override
                            public void onTick(long millisUntilFinished) {

                            }

                            @Override
                            public void onFinish() {
                                if (immobilityDetection != null){
                                    immobilityDetection.unregister();
                                }
                                if (count != null){
                                    count.cancel();
                                    count=null;
                                }
                                Intent intent = new Intent(SensorDetectionService.this,DetectionModeService.class);

                                intent.putExtra("SENSOR_DETECTION_CODE",VERTICALITY_CODE);
                                startService(intent);

                            }
                        }.start();
                    }
                }else {
                    if (count_2 != null){
                        count_2.cancel();
                        count_2 = null;
                    }
                    stopService(new Intent(SensorDetectionService.this,DetectionModeService.class));
                }


            }
        });
    }

    public void sensorImmobility(final long countTimer){
        final float seuil = sensibilty_pref.getFloat(getResources().getString(R.string.key_sensiblity_immobility),1f);
        mAccel = 0.00f;
        count = null;
        mAccelCurrent = SensorManager.GRAVITY_EARTH;
        mAccelLast = SensorManager.GRAVITY_EARTH;
        final float[] max_value = {-10};
        immobilityDetection = new ImmobilityDetection(getApplicationContext());
        immobilityDetection.register();
        immobilityDetection.setListener(new ImmobilityDetection.ImmobilityDetectionListener() {
            @Override
            public void senTranslation(float tx, float ty, float tz) {
                mAccelLast = mAccelCurrent;
                mAccelCurrent  = (float) Math.sqrt(tx * tx + ty * ty + tz * tz);
                float delta = mAccelCurrent - mAccelLast;
                mAccel = mAccel * 0.9f + delta;
                //Toast.makeText(SensorDetectionService.this, "moved: "+mAccel, Toast.LENGTH_SHORT).show();

                if (mAccel > max_value[0]) max_value[0] = mAccel;
                Log.i("Sensor", "senTranslation: max_Value "+max_value[0]);
                Log.i("Sensor", "senTranslation: "+seuil);
                if(mAccel < seuil){
                    //Toast.makeText(SensorDetectionService.this, "moved", Toast.LENGTH_SHORT).show();

                    if (count == null){
                        count = new CountDownTimer(countTimer,1000) {
                            @Override
                            public void onTick(long millisUntilFinished) {
                                //Log.i("Sensor", "senTranslation: fixed "+mAccel);
                            }

                            @Override
                            public void onFinish() {
                                if (verticalityDetection != null){
                                    verticalityDetection.unregister();
                                }
                                if (count_2 != null){
                                    count_2.cancel();
                                    count_2=null;
                                }
                                Intent intent = new Intent(SensorDetectionService.this,DetectionModeService.class);
                                intent.putExtra("SENSOR_DETECTION_CODE",IMMOBILITY_CODE);
                                startService(intent);
                                //Toast.makeText(SensorDetectionService.this, "you are not mobile!", Toast.LENGTH_SHORT).show();
                            }
                        }.start();
                    }
                }else{
                    if (count != null){
                        count.cancel();
                        count = null;
                        stopService(new Intent(SensorDetectionService.this,DetectionModeService.class));


                    }


                }

            }
        });
    }


    @Override
    public void onCreate() {
        super.onCreate();
        instance = SensorDetectionService.this;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (count != null){
            count.cancel();
            count = null;
        }
        if (count_2 != null){
            count_2.cancel();
            count_2 = null;
        }
        if (immobilityDetection != null)immobilityDetection.unregister();
        if (verticalityDetection != null)verticalityDetection.unregister();

       /* SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(getApplicationContext().getResources().getString(R.string.pref_key_mode_alerte),getApplicationContext().MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt(getString(R.string.pref_key_active_mode),0);
        editor.commit();*/

       /* MainActivity activity = MainActivity.instance;
        activity.notificationOff();*/
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
