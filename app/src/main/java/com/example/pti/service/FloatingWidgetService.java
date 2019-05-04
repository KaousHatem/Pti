package com.example.pti.service;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.example.pti.R;
import com.example.pti.model.Duration;

public class FloatingWidgetService extends Service {

    private WindowManager mWindowManager;
    private View mOverlayView;
    Button imageButton_sos;
    int mWidth;
    boolean activity_background;
    WindowManager.LayoutParams params;
    SharedPreferences preferences;
    ProgressBar progressBar;


    public FloatingWidgetService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            activity_background = intent.getBooleanExtra("activity_background", false);

        }
        if (mOverlayView == null){

            //preferences



            mOverlayView = LayoutInflater.from(this).inflate(R.layout.overlay_layout,null);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
                params = new WindowManager.LayoutParams(
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                                | WindowManager.LayoutParams.FLAG_FULLSCREEN ,
                        PixelFormat.TRANSLUCENT
                );
            }else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            {
                params = new WindowManager.LayoutParams(
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        WindowManager.LayoutParams.TYPE_SYSTEM_ERROR,
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                                | WindowManager.LayoutParams.FLAG_FULLSCREEN
                                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                        PixelFormat.TRANSLUCENT
                );
            }else {
                params = new WindowManager.LayoutParams(
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        WindowManager.LayoutParams.TYPE_SYSTEM_ERROR,
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                                | WindowManager.LayoutParams.FLAG_FULLSCREEN
                                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                        PixelFormat.TRANSLUCENT
                );
            }




            params.gravity = Gravity.TOP | Gravity.LEFT;

            params.x = 0;
            params.y = 100;

            mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
            mWindowManager.addView(mOverlayView,params);


            Display display = mWindowManager.getDefaultDisplay();
            final Point size = new Point();
            display.getSize(size);

            imageButton_sos =  mOverlayView.findViewById(R.id.imageButton_sos);
            progressBar = mOverlayView.findViewById(R.id.progressBar);



            final RelativeLayout layout = (RelativeLayout) mOverlayView.findViewById(R.id.overlay_layout);
            ViewTreeObserver vto = layout.getViewTreeObserver();
            vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        layout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    }


                    int width = layout.getMeasuredWidth();
                    mWidth = size.x - width;

                }
            });

            imageButton_sos.setOnTouchListener(new View.OnTouchListener() {

                private int initialX;
                private int initialY;
                private float initialTouchX;
                private float initialTouchY;
                CountDownTimer count;

                @Override
                public boolean onTouch(View view, MotionEvent motionEvent) {
                    switch (motionEvent.getAction()){
                        case MotionEvent.ACTION_DOWN:

                            //remember the initial position.
                            initialX = params.x;
                            initialY = params.y;
                            final Duration duration = new Duration(0,
                                    preferences.getInt(getApplicationContext().getResources().getString(R.string.key_press_btn),-1));

                            progressBar.setMax(duration.getSecond()*1000);
                            //get the touch location
                            initialTouchX = motionEvent.getRawX();
                            initialTouchY = motionEvent.getRawY();
                            count = new CountDownTimer(duration.getSecond()*1000, 1) {

                                public void onTick(long millisUntilFinished) {
                                    //Toast.makeText(FloatingWidgetService.this, ""+millisUntilFinished, Toast.LENGTH_SHORT).show();
                                    //imageButton_sos.setText(Integer.toString((int)millisUntilFinished/1000));
                                    progressBar.setProgress(duration.getSecond()*1000-(int)millisUntilFinished);
                                    //here you can have your logic to set text to edittext
                                }

                                public void onFinish() {
                                    startService(new Intent(FloatingWidgetService.this,SendSmsService.class));
                                    progressBar.setProgress(0);

                                }


                            }.start();


                            return true;

                        case MotionEvent.ACTION_UP:
                            count.cancel();
                            progressBar.setProgress(0);
                            if (activity_background){

                                float xDiff = motionEvent.getRawX() - initialTouchX;
                                float yDiff = motionEvent.getRawY() - initialTouchY;



                                if ((Math.abs(xDiff)<5) && (Math.abs(yDiff)<5)){

                                }
                            }
                            int middle = mWidth / 2;
                            float nearestXWall = params.x >= middle ? mWidth : 0;
                            params.x = (int) nearestXWall;


                            mWindowManager.updateViewLayout(mOverlayView, params);
                            return true;
                        case MotionEvent.ACTION_MOVE:



                            float Xdiff = Math.round(motionEvent.getRawX() - initialTouchX);
                            float Ydiff = Math.round(motionEvent.getRawY() - initialTouchY);


                            params.x = initialX + (int) Xdiff;
                            params.y = initialY + (int) Ydiff;
                            Log.i("TAG", "onTouch:Xdiff "+Xdiff+",Ydiff"+Ydiff);
                            if (Xdiff<-30 || Xdiff>30 || Ydiff<-30 || Ydiff>30 ){
                                count.cancel();
                                progressBar.setProgress(0);
                            }
                            mWindowManager.updateViewLayout(mOverlayView, params);
                            return true;

                    }
                    return false;
                }
            });
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        setTheme(R.style.AppTheme);

        preferences = getApplicationContext().getSharedPreferences(getApplicationContext().getResources().getString(R.string.pref_key_duration),getApplicationContext().MODE_PRIVATE);


    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mOverlayView != null){
            mWindowManager.removeView(mOverlayView);
        }
    }
}
