package com.example.pti.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.NumberPicker;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import com.example.pti.R;
import com.example.pti.model.Duration;
import com.example.pti.service.SensorDetectionService;

public class DialogPickDurationPreAlerte extends DialogFragment implements NumberPicker.OnValueChangeListener {

    NumberPicker numberPicker_pre_alerte_m,numberPicker_pre_alerte_s;
    Duration duration;


    DialogPickDurationPreAlerteListener pickDurationPreAlerteListener;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {


        final SharedPreferences preferences = getContext()
                .getSharedPreferences(getContext().getResources().getString(R.string.pref_key_duration),getContext().MODE_PRIVATE);




        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater layoutInflater = getActivity().getLayoutInflater();
        View view = layoutInflater.inflate(R.layout.dialog_duration_pre_alarme,null);
        builder.setView(view);

        duration = new Duration(preferences.getInt(getContext().getResources().getString(R.string.key_pre_alerte_m),-1),
                preferences.getInt(getContext().getResources().getString(R.string.key_pre_alerte_s),-1));

        numberPicker_pre_alerte_m = view.findViewById(R.id.numberPicker_pre_alerte_m);
        numberPicker_pre_alerte_m.setMaxValue(2);
        numberPicker_pre_alerte_m.setMinValue(0);
        numberPicker_pre_alerte_m.setValue(duration.getMinute());

        numberPicker_pre_alerte_s = view.findViewById(R.id.numberPicker_pre_alerte_s);

        if (numberPicker_pre_alerte_m.getValue() == 0){
            numberPicker_pre_alerte_s.setMinValue(5);
            numberPicker_pre_alerte_s.setMaxValue(59);
        }else if(numberPicker_pre_alerte_m.getValue() == 2){
            numberPicker_pre_alerte_s.setMaxValue(0);
            numberPicker_pre_alerte_s.setMinValue(0);
        }else{
            numberPicker_pre_alerte_s.setMinValue(0);
            numberPicker_pre_alerte_s.setMaxValue(59);
        }

        numberPicker_pre_alerte_s.setValue(duration.getSecond());

        numberPicker_pre_alerte_m.setOnValueChangedListener(this);

        builder.setPositiveButton("ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                SharedPreferences.Editor editor = preferences.edit();
                duration.setMinute(numberPicker_pre_alerte_m.getValue());
                duration.setSecond(numberPicker_pre_alerte_s.getValue());
                editor.putInt(getContext().getResources().getString(R.string.key_pre_alerte_s),duration.getSecond());
                editor.putInt(getContext().getResources().getString(R.string.key_pre_alerte_m),duration.getMinute());
                editor.commit();
                pickDurationPreAlerteListener.applyText_pre_alarme(duration);
                SharedPreferences preferences = getContext().getSharedPreferences(getContext().getResources().getString(R.string.pref_key_mode_alerte),getContext().MODE_PRIVATE);
                if (preferences.getInt(getString(R.string.pref_key_active_mode),0)==1){
                    getActivity().stopService(new Intent(getActivity().getApplicationContext(), SensorDetectionService.class));
                    Intent intent = new Intent(getActivity().getApplicationContext(), SensorDetectionService.class);
                    intent.putExtra("immobilityMode",(preferences.getInt(getString(R.string.key_immobility),-1)==1));
                    intent.putExtra("verticalityMode",(preferences.getInt(getString(R.string.key_verticality),-1)==1));
                    getActivity().startService(intent);
                }

            }
        }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
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
            pickDurationPreAlerteListener = (DialogPickDurationPreAlerteListener) context;
        }catch (ClassCastException e){
            throw new ClassCastException(context.toString()+
                    "must implement pickDurationPreAlerteListener");
        }
    }

    @Override
    public void onValueChange(NumberPicker numberPicker, int i, int i1) {
        if (i1 == 0){
            numberPicker_pre_alerte_s.setMinValue(5);
            numberPicker_pre_alerte_s.setMaxValue(59);
        }else if(i1 == 2){
            numberPicker_pre_alerte_s.setMaxValue(0);
            numberPicker_pre_alerte_s.setMinValue(0);
        }else {
            numberPicker_pre_alerte_s.setMinValue(0);
            numberPicker_pre_alerte_s.setMaxValue(59);
        }
    }

    public interface DialogPickDurationPreAlerteListener{
        void applyText_pre_alarme(Duration duration);
    }
}