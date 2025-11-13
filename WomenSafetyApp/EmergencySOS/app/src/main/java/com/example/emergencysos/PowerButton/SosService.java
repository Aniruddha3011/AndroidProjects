package com.example.emergencysos.PowerButton;

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
import com.example.emergencysos.Services.Utils;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.util.List;

public class SosService extends Service {

    private FusedLocationProviderClient fusedLocationClient;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Missing permissions", Toast.LENGTH_SHORT).show();
            stopSelf();
            return START_NOT_STICKY;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                sendSosSms(location);
            } else {
                Toast.makeText(this, "Unable to fetch location", Toast.LENGTH_SHORT).show();
            }
            stopSelf(); // Stop service after work done
        });

        return START_NOT_STICKY;
    }

    private void sendSosSms(Location location) {
        SharedPreferences prefs = getSharedPreferences("emergency_contacts", MODE_PRIVATE);
        String contactJson = prefs.getString("contact_list", null);

        if (contactJson == null) return;

        String message = "Emergency Safety Alert!\nMy location:\n" +
                "https://www.google.com/maps?q=" + location.getLatitude() + "," + location.getLongitude();

        SmsManager smsManager = SmsManager.getDefault();
        try {
            List<EmergencyContact> contactList = Utils.getSavedContacts(prefs);
            for (EmergencyContact contact : contactList) {
                smsManager.sendTextMessage(contact.getPhone(), null, "Emergency Safety Alert Triggered!", null, null);
                smsManager.sendTextMessage(contact.getPhone(), null, message, null, null);
            }
            Toast.makeText(this, "SOS sent!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

