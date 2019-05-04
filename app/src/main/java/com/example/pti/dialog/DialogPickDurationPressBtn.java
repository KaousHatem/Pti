package com.example.pti.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.NumberPicker;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import com.example.pti.R;
import com.example.pti.model.Duration;

public class DialogPickDurationPressBtn extends DialogFragment {

    NumberPicker numberPicker_press_btn;
    Duration duration;
    DialogPickDurationPressBtnListener dialogPickDurationPressBtnListener;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        final SharedPreferences preferences = getContext()
                .getSharedPreferences(getContext().getResources().getString(R.string.pref_key_duration),getContext().MODE_PRIVATE);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater layoutInflater = getActivity().getLayoutInflater();
        View view = layoutInflater.inflate(R.layout.dialog_duration_press_btn,null);
        builder.setView(view);

        duration = new Duration(0,
                preferences.getInt(getContext().getResources().getString(R.string.key_press_btn),-1));

        numberPicker_press_btn = view.findViewById(R.id.numberPicker_press_btn);
        numberPicker_press_btn.setMinValue(1);
        numberPicker_press_btn.setMaxValue(5);
        numberPicker_press_btn.setValue(duration.getSecond());

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                SharedPreferences.Editor editor = preferences.edit();
                duration.setSecond(numberPicker_press_btn.getValue());
                editor.putInt(getContext().getResources().getString(R.string.key_press_btn),duration.getSecond());
                editor.commit();
                dialogPickDurationPressBtnListener.applyText_pressBtn(duration);
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
            dialogPickDurationPressBtnListener = (DialogPickDurationPressBtn.DialogPickDurationPressBtnListener) context;
        }catch (ClassCastException e){
            throw new ClassCastException(context.toString()+
                    "must implement pickDurationPreAlerteListener");
        }
    }

    public interface DialogPickDurationPressBtnListener{
        public void applyText_pressBtn(Duration duration);
    }
}
