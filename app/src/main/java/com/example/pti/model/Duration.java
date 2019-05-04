package com.example.pti.model;

public class Duration {
    private int minute;
    private int second;

    public Duration(int minute, int second) {
        this.minute = minute;
        this.second = second;
    }

    public int getMinute() {
        return minute;
    }

    public void setMinute(int minute) {
        this.minute = minute;
    }

    public int getSecond() {
        return second;
    }

    public void setSecond(int second) {
        this.second = second;
    }
}
