package com.example.emergencysos.Screens;

import com.google.android.gms.maps.model.LatLng;

public class CrimeZone {
    public LatLng center;
    public String level;
    public double radius;

    public CrimeZone(LatLng center, String level, double radius) {
        this.center = center;
        this.level = level;
        this.radius = radius;
    }

    public int getStrokeColor() {
        switch (level.toLowerCase()) {
            case "red":
                return 0xFFFF0000;
            case "yellow":
                return 0xFFFFFF00;
            case "green":
                return 0xFF00FF00;
            default:
                return 0xFF888888;
        }
    }

    public int getFillColor() {
        switch (level.toLowerCase()) {
            case "red":
                return 0x44FF0000;
            case "yellow":
                return 0x44FFFF00;
            case "green":
                return 0x4400FF00;
            default:
                return 0x44888888;
        }
    }
}
