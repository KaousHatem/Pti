package com.example.pti.utils;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class ImmobilityDetection {

    private SensorManager sensorManager;
    private Sensor sensor;
    private SensorEventListener eventListener;

    private ImmobilityDetectionListener listener;

    public ImmobilityDetection(Context context) {
        sensorManager = (SensorManager) context.getSystemService(context.SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        eventListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {

                if (listener != null){
                    listener.senTranslation(event.values[0],event.values[1],event.values[2]);
                }

            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        };
    }

    public void setListener(ImmobilityDetectionListener l){
        this.listener = l;
    }

    public void register(){
        sensorManager.registerListener(eventListener,sensor,sensorManager.SENSOR_DELAY_UI);
    }

    public void unregister(){
        sensorManager.unregisterListener(eventListener);
    }

    public interface ImmobilityDetectionListener{
        void senTranslation(float tx,float ty,float tz);
    }

}
