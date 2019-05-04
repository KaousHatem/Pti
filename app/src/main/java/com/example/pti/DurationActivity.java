package com.example.pti;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.pti.dialog.DialogPickDurationPre2Alerte;
import com.example.pti.dialog.DialogPickDurationPreAlerte;
import com.example.pti.dialog.DialogPickDurationPressBtn;
import com.example.pti.model.Duration;
import com.example.pti.service.FloatingWidgetService;

public class DurationActivity extends AppCompatActivity implements DialogPickDurationPreAlerte.DialogPickDurationPreAlerteListener, DialogPickDurationPre2Alerte.DialogPickDurationPre2AlerteListener, DialogPickDurationPressBtn.DialogPickDurationPressBtnListener {

    private Toolbar toolbar;
    private View layout_pre_alerte,layout_pre2_alerte,layout_press_btn;
    private TextView textView_pre_alerte_value;
    private TextView textView_pre2_alerte_value;
    private TextView textView_press_btn_value;
    SharedPreferences preferences;
    SharedPreferences.Editor editor;
    Duration duration,duration2,duration3;

    private boolean appInTop = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_duration);

        init_toolbar("Parametres de durée");

        preferences = getApplicationContext()
                .getSharedPreferences(getApplicationContext().getResources().getString(R.string.pref_key_duration),getApplicationContext().MODE_PRIVATE);


        duration = new Duration(preferences.getInt(getApplicationContext().getResources().getString(R.string.key_pre_alerte_m),-1),
                preferences.getInt(getApplicationContext().getResources().getString(R.string.key_pre_alerte_s),-1));

        duration2 = new Duration(preferences.getInt(getApplicationContext().getResources().getString(R.string.key_pre2_alerte_m),-1),
                preferences.getInt(getApplicationContext().getResources().getString(R.string.key_pre2_alerte_s),-1));

        duration3 = new Duration(0,
                preferences.getInt(getApplicationContext().getResources().getString(R.string.key_press_btn),-1));

        textView_pre_alerte_value = findViewById(R.id.textView_pre_alerte_value);
        textView_pre2_alerte_value = findViewById(R.id.textView_pre2_alerte_value);
        textView_press_btn_value = findViewById(R.id.textView_press_btn_value);

        setText_pressBtn_duration(duration3);
        setText_pre2_duration(duration2);
        setText_pre_duration(duration);

        layout_pre_alerte = findViewById(R.id.layout_pre_alerte);
        layout_pre2_alerte = findViewById(R.id.layout_pre2_alerte);
        layout_press_btn = findViewById(R.id.layout_press_btn);

        layout_pre_alerte.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new DialogPickDurationPreAlerte().show(getSupportFragmentManager(),"duration_pre_alerte");
            }
        });

        layout_pre2_alerte.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new DialogPickDurationPre2Alerte().show(getSupportFragmentManager(),"duration_pre2_alerte");
            }
        });

        layout_press_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new DialogPickDurationPressBtn().show(getSupportFragmentManager(),"duration_pre_alerte");
            }
        });

    }

    private void init_toolbar(String titletoolbar) {

        toolbar = findViewById(R.id.toolbar_layout);
        setSupportActionBar(toolbar);

        getSupportActionBar().setTitle(titletoolbar);

        if (getSupportActionBar() != null){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        stopServiceFloatingWidgetService();
        appInTop = false;
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (!appInTop) startServiceFloatingWidgetService();
    }

    private void stopServiceFloatingWidgetService(){
        stopService(new Intent(getApplicationContext(), FloatingWidgetService.class));
    }

    private void startServiceFloatingWidgetService(){
        SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(getApplicationContext().getResources().getString(R.string.pref_key_mode_alerte),getApplicationContext().MODE_PRIVATE);

        SharedPreferences preferences =  getApplicationContext().getSharedPreferences(getApplicationContext().getResources().getString(R.string.pref_key_mode_alerte),getApplicationContext().MODE_PRIVATE);
        if (preferences.getInt(getString(R.string.pref_key_active_mode),-1)==1){
            if (sharedPref.getInt(getString(R.string.key_manuelle_alarme),1)==1){
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(DurationActivity.this)){

                    Intent intent = new Intent(getApplicationContext(), FloatingWidgetService.class);
                    intent.putExtra("activity_background", true);
                    startService(intent);

                }else {
                    Toast.makeText(DurationActivity.this, "Draw over other app permission not available. Can't start the application without the permission.", Toast.LENGTH_LONG).show();
                }
            }

        }
    }

    @Override
    public void applyText_pre_alarme(Duration duration) {
        setText_pre_duration(duration);
    }

    @Override
    public void applyText_pre2_alarme(Duration duration) {
        setText_pre2_duration(duration);
    }

    @Override
    public void applyText_pressBtn(Duration duration) {
        setText_pressBtn_duration(duration);
    }

    private void setText_pre_duration(Duration duration){
        String zero = "0";
        if (Integer.toString(duration.getSecond()).length()==2) zero = "";
        textView_pre_alerte_value.setText("0"+duration.getMinute()+":"+zero+duration.getSecond());
    }



    private void setText_pre2_duration(Duration duration){
        String zero = "0";
        if (Integer.toString(duration.getSecond()).length()==2) zero = "";
        textView_pre2_alerte_value.setText("0"+duration.getMinute()+":"+zero+duration.getSecond());
    }

    private void setText_pressBtn_duration(Duration duration){

        textView_press_btn_value.setText(duration.getSecond()+"s");
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        appInTop = true;
        return true;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        appInTop = true;
    }
}
