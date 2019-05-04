package com.example.pti.utils;

import android.app.Application;

public class AppController extends Application {

    protected static AppController sInstance;

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;

    }
    public synchronized static AppController getInstance() {
        return sInstance;
    }
}
