package com.example.pti.utils;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class VerticalityDetection {

    private SensorManager sensorManager;
    private Sensor sensor;
    private SensorEventListener eventListener;

    private VerticalityDetectionListener listener;

    public VerticalityDetection(Context context) {
        sensorManager = (SensorManager) context.getSystemService(context.SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        eventListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                if (listener != null){
                    listener.senRotation(event.values[0],event.values[1],event.values[2]);
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        };
    }

    public void setListener(VerticalityDetectionListener l){
        this.listener = l;
    }

    public void register(){
        sensorManager.registerListener(eventListener,sensor,sensorManager.SENSOR_DELAY_UI);
    }

    public void unregister(){
        sensorManager.unregisterListener(eventListener);
    }

    public interface VerticalityDetectionListener{
        void senRotation(float tx,float ty,float tz);
    }
}
