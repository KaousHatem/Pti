package com.example.pti;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.pti.service.FloatingWidgetService;
import com.example.pti.service.SensorDetectionService;

public class SensibilityActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private SeekBar seekBar_verticality,seekBar_immobility;
    private TextView textView_verticality;

    private boolean appInTop = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensibility);

        init_toolbar("Sensibilité des detecteurs");

        textView_verticality = findViewById(R.id.textView_verticality);

        initTextView();


        seekBar_verticality = findViewById(R.id.seekBar_verticality);
        initSeekBarVerticality();

        seekBar_immobility = findViewById(R.id.seekBar_immobility);
        initSeekBarImmobility();
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

        SharedPreferences preferences = getApplicationContext().getSharedPreferences(getApplicationContext().getResources().getString(R.string.pref_key_mode_alerte),getApplicationContext().MODE_PRIVATE);
        if (preferences.getInt(getString(R.string.pref_key_active_mode),0)==1){
            stopService(new Intent(getApplicationContext(), SensorDetectionService.class));
            Intent intent = new Intent(getApplicationContext(), SensorDetectionService.class);
            intent.putExtra("immobilityMode",(preferences.getInt(getString(R.string.key_immobility),-1)==1));
            intent.putExtra("verticalityMode",(preferences.getInt(getString(R.string.key_verticality),-1)==1));
            startService(intent);
        }

    }

    private void stopServiceFloatingWidgetService(){
        stopService(new Intent(getApplicationContext(), FloatingWidgetService.class));
    }

    private void startServiceFloatingWidgetService(){
        SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(getApplicationContext().getResources().getString(R.string.pref_key_mode_alerte),getApplicationContext().MODE_PRIVATE);

        SharedPreferences preferences =  getApplicationContext().getSharedPreferences(getApplicationContext().getResources().getString(R.string.pref_key_mode_alerte),getApplicationContext().MODE_PRIVATE);
        if (preferences.getInt(getString(R.string.pref_key_active_mode),-1)==1){
            if (sharedPref.getInt(getString(R.string.key_manuelle_alarme),1)==1){
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(SensibilityActivity.this)){

                    Intent intent = new Intent(getApplicationContext(), FloatingWidgetService.class);
                    intent.putExtra("activity_background", true);
                    startService(intent);

                }else {
                    Toast.makeText(SensibilityActivity.this, "Draw over other app permission not available. Can't start the application without the permission.", Toast.LENGTH_LONG).show();
                }
            }

        }
    }

    private void initTextView(){
        SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(getApplicationContext().getResources().getString(R.string.pref_key_sensibility),getApplicationContext().MODE_PRIVATE);
        textView_verticality.setText(""+sharedPref.getInt(getApplicationContext().getResources().getString(R.string.key_sensiblity_verticality),-1));
    }

    private void updatePreference(String key,float value){
        SharedPreferences sharedPreferences = getApplicationContext()
                .getSharedPreferences(getApplicationContext().getResources().getString(R.string.pref_key_sensibility),getApplicationContext().MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        if (key == getApplicationContext().getResources().getString(R.string.key_sensiblity_verticality)){
            editor.putInt(key, (int) value);
        }
        if (key == getApplicationContext().getResources().getString(R.string.key_sensiblity_immobility)){
            editor.putFloat(key,value);
        }
        editor.commit();
    }

    private void initSeekBarVerticality(){


        seekBar_verticality.setMax(17);
        seekBar_verticality.setProgress(Integer.valueOf(textView_verticality.getText().toString())/5-1);



        seekBar_verticality.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                textView_verticality.setText(""+(progress+1)*5);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                updatePreference(getApplicationContext().getResources().getString(R.string.key_sensiblity_verticality),Integer.valueOf(textView_verticality.getText().toString()));
            }
        });
    }

    private void initSeekBarImmobility(){
        SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(getApplicationContext().getResources().getString(R.string.pref_key_sensibility),getApplicationContext().MODE_PRIVATE);
        float tmp = sharedPref.getFloat(getApplicationContext().getResources().getString(R.string.key_sensiblity_immobility),-1f);
        //min=0.5 and max=3.5
        seekBar_immobility.setMax(6);
        seekBar_immobility.setProgress((int)(tmp*2f-1f));

        seekBar_immobility.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                updatePreference(getApplicationContext().getResources().getString(R.string.key_sensiblity_immobility),(seekBar.getProgress()/2f+(1f/2f)));
            }
        });


    }
}
