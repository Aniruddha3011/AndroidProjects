package com.example.emergencysos.Screens;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.*;
import android.telephony.SmsManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.example.emergencysos.Models.EmergencyContact;
import com.google.android.gms.location.*;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class SosService extends Service {
    private static final String CHANNEL_ID = "SOS_SERVICE_CHANNEL";
    private static final int MAX_LOCATION_UPDATES = 2;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private Handler handler;

    private MediaRecorder recorder;
    private String audioFilePath;

    private List<String> emergencyContactList = new ArrayList<>();
    private int locationUpdateCount = 0;
    private boolean audioUploaded = false;
    private boolean locationDone = false;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onCreate() {
        super.onCreate();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        handler = new Handler(Looper.getMainLooper());
        loadEmergencyContacts();
        createNotificationChannel();
    }

    private void loadEmergencyContacts() {
        SharedPreferences prefs = getSharedPreferences("emergency_contacts", MODE_PRIVATE);
        String json = prefs.getString("contact_list", null);
        if (json != null) {
            Type type = new TypeToken<ArrayList<EmergencyContact>>() {}.getType();
            List<EmergencyContact> contacts = new Gson().fromJson(json, type);
            if (contacts != null) {
                for (EmergencyContact c : contacts) {
                    if (c != null && c.getPhone() != null) emergencyContactList.add(c.getPhone());
                }
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "SOS Service", NotificationManager.IMPORTANCE_HIGH);
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) manager.createNotificationChannel(channel);
    }

    private Notification getNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("SOS Active")
                .setContentText("Sharing live location & recording audio...")
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(101, getNotification());

        // ensure permissions are present
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Missing required permissions", Toast.LENGTH_SHORT).show();
            stopSelf();
            return START_NOT_STICKY;
        }

        startLiveLocationUpdates();
        startAudioRecording();

        return START_NOT_STICKY;
    }

    private void startLiveLocationUpdates() {
        LocationRequest request = LocationRequest.create()
                .setInterval(30000)
                .setFastestInterval(15000)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult result) {
                if (result == null) return;
                if (locationUpdateCount >= MAX_LOCATION_UPDATES) return;

                Location loc = result.getLastLocation();
                if (loc != null) {
                    String message = "Live Location: https://maps.google.com/?q=" + loc.getLatitude() + "," + loc.getLongitude();
                    SmsManager sms = SmsManager.getDefault();
                    for (String num : emergencyContactList) {
                        try { sms.sendTextMessage(num, null, message, null, null); }
                        catch (Exception ex) { /* handle or log */ }
                    }
                    locationUpdateCount++;
                    if (locationUpdateCount >= MAX_LOCATION_UPDATES) {
                        locationDone = true;
                        stopLiveLocationUpdates();
                        checkStopSelf();
                    }
                }
            }
        };

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        fusedLocationClient.requestLocationUpdates(request, locationCallback, handler.getLooper());
    }

    private void stopLiveLocationUpdates() {
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    private void startAudioRecording() {
        try {
            audioFilePath = getExternalFilesDir(Environment.DIRECTORY_MUSIC).getAbsolutePath()
                    + "/sos_audio_" + System.currentTimeMillis() + ".m4a";

            recorder = new MediaRecorder();
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            recorder.setOutputFile(audioFilePath);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            recorder.prepare();
            recorder.start();

            handler.postDelayed(this::stopAudioRecording, 10_000); // stop after 10s
        } catch (IOException e) {
            Toast.makeText(this, "Recording error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            audioUploaded = true; // avoid blocking stop
            checkStopSelf();
        }
    }

    private void stopAudioRecording() {
        try {
            if (recorder != null) {
                recorder.stop();
                recorder.release();
                recorder = null;
                uploadAudioToFirebase();
            } else {
                audioUploaded = true;
                checkStopSelf();
            }
        } catch (Exception e) {
            audioUploaded = true;
            checkStopSelf();
        }
    }

    private void uploadAudioToFirebase() {
        File f = new File(audioFilePath);
        if (!f.exists()) {
            audioUploaded = true;
            checkStopSelf();
            return;
        }

        Uri uri = Uri.fromFile(f);
        StorageReference storageRef = FirebaseStorage.getInstance().getReference();
        StorageReference audioRef = storageRef.child("sos_recordings/" + uri.getLastPathSegment());

        audioRef.putFile(uri)
                .addOnSuccessListener(task -> audioRef.getDownloadUrl()
                        .addOnSuccessListener(downloadUri -> {
                            sendAudioLinkSMS(downloadUri.toString());
                            audioUploaded = true;
                            checkStopSelf();
                        })
                        .addOnFailureListener(e -> {
                            audioUploaded = true;
                            checkStopSelf();
                        }))
                .addOnFailureListener(e -> {
                    audioUploaded = true;
                    checkStopSelf();
                });
    }

    private void sendAudioLinkSMS(String link) {
        String message = "Emergency Audio Recording: " + link;
        SmsManager sms = SmsManager.getDefault();
        for (String num : emergencyContactList) {
            try { sms.sendTextMessage(num, null, message, null, null); }
            catch (Exception ex) { /* handle */ }
        }
    }

    private void checkStopSelf() {
        if (audioUploaded && locationDone) {
            stopForeground(true);
            stopSelf();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopLiveLocationUpdates();
        if (recorder != null) {
            recorder.release();
            recorder = null;
        }
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
