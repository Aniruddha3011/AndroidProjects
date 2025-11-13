package com.example.emergencysos.Screens;

import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.IBinder;
import android.telephony.SmsManager;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import com.example.emergencysos.Models.EmergencyContact;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;

public class EmergencyService extends Service {
    FusedLocationProviderClient locationClient;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        locationClient = LocationServices.getFusedLocationProviderClient(this);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Location permission not granted", Toast.LENGTH_SHORT).show();
            stopSelf();
            return START_NOT_STICKY;
        }

        locationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                sendAlertSMS(location);
            }
        });

        return START_NOT_STICKY;
    }

    private void sendAlertSMS(Location location) {
        SharedPreferences prefs = getSharedPreferences("emergency_contacts", MODE_PRIVATE);
        String json = prefs.getString("contact_list", null);

        String msg = "🚨 Emergency Safety Alert!\nLive Location: https://maps.google.com/?q=" +
                location.getLatitude() + "," + location.getLongitude();

        if (json != null) {
            ArrayList<EmergencyContact> contacts = new Gson().fromJson(json,
                    new TypeToken<ArrayList<EmergencyContact>>(){}.getType());
            SmsManager smsManager = SmsManager.getDefault();
            for (EmergencyContact contact : contacts) {
                smsManager.sendTextMessage(contact.getPhone(), null, "🚨 Emergency Safety Alert Started!", null, null);
                smsManager.sendTextMessage(contact.getPhone(), null, msg, null, null);
            }
        }

        Toast.makeText(this, "Emergency messages sent", Toast.LENGTH_SHORT).show();
        stopSelf();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
