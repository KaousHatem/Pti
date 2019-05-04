package com.example.pti.model;

public class ObservableInteger {

    private OnIntegerChangeListener listener;

    private int value;

    public void setOnIntegerChangeListener(OnIntegerChangeListener listener)
    {
        this.listener = listener;
    }

    public int get()
    {
        return value;
    }

    public void set(int value)
    {
        this.value = value;

        if(listener != null)
        {
            listener.onIntegerChanged(value);
        }
    }

    public interface OnIntegerChangeListener
    {
        public void onIntegerChanged(int newValue);
    }
}

