package com.example.pti;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.pti.dialog.DialogAlertMode;
import com.example.pti.service.FloatingWidgetService;

public class SettingActivity extends AppCompatActivity implements DialogAlertMode.DialogAlertModeListener {

    private Toolbar toolbar;
    private View layout_alert_mode,layout_contact,layout_duration,layout_sensibility;

    private SharedPreferences preferences;

    private TextView textViewSummaryAM;
    private String verticality,immobility;

    private boolean appInTop = false;

    private AlertDialog.Builder builderAlert;
    private AlertDialog dialogAlert;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);




        preferences = getApplicationContext()
                .getSharedPreferences(getApplicationContext().getResources().getString(R.string.pref_key_mode_alerte),getApplicationContext().MODE_PRIVATE);

        textViewSummaryAM = findViewById(R.id.textViewSummaryAM);
        textViewSummaryAM.setText("Perte de verticalité, Immobilité, Alarme Manuelle");

        // this dialog shows when there is no alerte mode is checked
        build_create_dialog_check_modeA();

        // init layouts view
        layout_alert_mode = findViewById(R.id.layout_alert_mode);
        layout_contact = findViewById(R.id.layout_contact);
        layout_duration = findViewById(R.id.layout_duration);
        layout_sensibility = findViewById(R.id.layout_sensibility);

        // init toolbar
        init_toolbar("Parametres");

        // set up listener of layouts view
        layout_alert_mode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new DialogAlertMode().show(getSupportFragmentManager(),"Alerte Mode");
            }
        });

        layout_contact.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                appInTop = true;
                Intent intent = new Intent(SettingActivity.this, ContactActivity.class);
                startActivity(intent);
            }
        });

        layout_duration.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                appInTop = true;
                Intent intent = new Intent(SettingActivity.this,DurationActivity.class);
                startActivity(intent);
            }
        });

        layout_sensibility.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                appInTop = true;
                Intent intent = new Intent(SettingActivity.this,SensibilityActivity.class);
                startActivity(intent);
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

    private void build_create_dialog_check_modeA() {
        builderAlert = new AlertDialog.Builder(this);
        builderAlert.setMessage("Veuillez activer au moin un mode d'alerte");
        builderAlert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                new DialogAlertMode().show(getSupportFragmentManager(),"Alerte Mode");
            }
        });
        dialogAlert = builderAlert.create();
        dialogAlert.setCanceledOnTouchOutside(false);
    }

    @Override
    public boolean onSupportNavigateUp() {
        appInTop = true;
        onBackPressed();
        return true;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        appInTop = true;
    }


    @Override
    protected void onPause() {
        super.onPause();
        if (!appInTop) startServiceFloatingWidgetService();
    }

    @Override
    protected void onResume() {
        super.onResume();
        appInTop = false;
        stopServiceFloatingWidgetService();


    }

    private void stopServiceFloatingWidgetService(){
        stopService(new Intent(getApplicationContext(), FloatingWidgetService.class));
    }

    private void startServiceFloatingWidgetService(){
        SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(getApplicationContext().getResources().getString(R.string.pref_key_mode_alerte),getApplicationContext().MODE_PRIVATE);

        SharedPreferences preferences =  getApplicationContext().getSharedPreferences(getApplicationContext().getResources().getString(R.string.pref_key_mode_alerte),getApplicationContext().MODE_PRIVATE);
        if (preferences.getInt(getString(R.string.pref_key_active_mode),-1)==1){
            if (sharedPref.getInt(getString(R.string.key_manuelle_alarme),1)==1){
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(SettingActivity.this)){

                    Intent intent = new Intent(getApplicationContext(), FloatingWidgetService.class);
                    intent.putExtra("activity_background", true);
                    startService(intent);

                }else {
                    Toast.makeText(SettingActivity.this, "Draw over other app permission not available. Can't start the application without the permission.", Toast.LENGTH_LONG).show();
                }
            }

        }
    }

    @Override
    public void show_error_dialog() {
        dialogAlert.show();
    }
}
