package com.example.pti.utils;

public class Config {

    private static Config instance;

    private String currentLatitude;
    private String currentLongitude;

    public String getCurrentLatitude() {
        return currentLatitude;
    }

    public void setCurrentLatitude(String currentLatitude) {
        this.currentLatitude = currentLatitude;
    }

    public String getCurrentLongitude() {
        return currentLongitude;
    }

    public void setCurrentLongitude(String currentLongitude) {
        this.currentLongitude = currentLongitude;
    }

    public static Config getInstance() {
        if (instance == null)
            instance = new Config();

        return instance;
    }
}
