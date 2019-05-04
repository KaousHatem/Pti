package com.example.pti.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import com.example.pti.R;
import com.example.pti.model.AlerteMode;
import com.example.pti.service.SensorDetectionService;

public class DialogAlertMode extends DialogFragment {

    private boolean[] mAlertModeActive;

    AlerteMode alerteMode;



    private DialogAlertModeListener listener;


    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mAlertModeActive = new boolean[3];
        final SharedPreferences preferences = getContext().getSharedPreferences(getContext().getResources().getString(R.string.pref_key_mode_alerte),getContext().MODE_PRIVATE);
        mAlertModeActive[0] = preferences.getInt(getString(R.string.key_verticality),-1) == 1;
        mAlertModeActive[1] =  preferences.getInt(getString(R.string.key_immobility),-1) == 1;
        mAlertModeActive[2] =  preferences.getInt(getString(R.string.key_manuelle_alarme),-1) == 1;
        alerteMode = new AlerteMode();






        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("choisir le mode d'alerte à utiliser");



        builder.setMultiChoiceItems(R.array.pref_mode_alert, mAlertModeActive, new DialogInterface.OnMultiChoiceClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i, boolean b) {


                if(b){

                    mAlertModeActive[i]=true;

                }
                else {
                    mAlertModeActive[i]=false;
                }
            }
        });

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                SharedPreferences.Editor editor = preferences.edit();
                boolean check = false;
                for (int j=0;j<3;j++){
                    if (mAlertModeActive[j]){
                        check = true;
                        break;
                    }
                }
                if (!check){

                    listener.show_error_dialog();

                }else {
                    if(mAlertModeActive[0]){
                        alerteMode.setVerticality(1);
                        editor.putInt(getString(R.string.key_verticality), 1);
                        editor.commit();
                    }else{
                        alerteMode.setVerticality(0);
                        editor.putInt(getString(R.string.key_verticality), 0);
                        editor.commit();
                    }
                    if(mAlertModeActive[1]){
                        alerteMode.setImmobility(1);
                        editor.putInt(getString(R.string.key_immobility), 1);
                        editor.commit();
                    }else{
                        alerteMode.setImmobility(0);
                        editor.putInt(getString(R.string.key_immobility), 0);
                        editor.commit();
                    }
                    if(mAlertModeActive[2]){
                        alerteMode.setImmobility(1);
                        editor.putInt(getString(R.string.key_manuelle_alarme), 1);
                        editor.commit();
                    }else{
                        alerteMode.setImmobility(0);
                        editor.putInt(getString(R.string.key_manuelle_alarme), 0);
                        editor.commit();
                    }
                    if (preferences.getInt(getString(R.string.pref_key_active_mode),0)==1){
                        getActivity().stopService(new Intent(getActivity().getApplicationContext(), SensorDetectionService.class));
                        Intent intent = new Intent(getActivity().getApplicationContext(), SensorDetectionService.class);
                        intent.putExtra("immobilityMode",(preferences.getInt(getString(R.string.key_immobility),-1)==1));
                        intent.putExtra("verticalityMode",(preferences.getInt(getString(R.string.key_verticality),-1)==1));
                        getActivity().startService(intent);
                    }



                }

            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });



        return builder.create();

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            listener = (DialogAlertMode.DialogAlertModeListener) context;
        }catch (ClassCastException e){
            throw new ClassCastException(context.toString()+
                    "must implement DialogAlertModeListener");
        }
    }

    public interface DialogAlertModeListener{
        void show_error_dialog();
    }


}
