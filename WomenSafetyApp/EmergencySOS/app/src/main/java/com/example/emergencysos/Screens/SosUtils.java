package com.example.emergencysos.Screens;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Environment;
import android.telephony.SmsManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SosUtils {

    private static MediaRecorder mediaRecorder;
    private static String audioFilePath;

    /**
     * Main SOS trigger method
     */
    public static void triggerSOS(Context context, String[] emergencyContacts) {
        sendLocationToContacts(context, emergencyContacts);
        startAudioRecording(context);
    }

    /**
     * Get last known location and send via SMS
     */
    @SuppressLint("MissingPermission")
    private static void sendLocationToContacts(Context context, String[] emergencyContacts) {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e("SOS", "Location permission not granted");
            return;
        }

        Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (location == null) {
            location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        }

        if (location != null) {
            String locationUrl = "https://maps.google.com/?q=" + location.getLatitude() + "," + location.getLongitude();
            for (String contact : emergencyContacts) {
                SmsManager.getDefault().sendTextMessage(contact, null, "SOS! I need help. My location: " + locationUrl, null, null);
            }
        }
    }

    /**
     * Start audio recording
     */
    private static void startAudioRecording(Context context) {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        audioFilePath = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC) + "/SOS_" + timeStamp + ".3gp";

        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mediaRecorder.setOutputFile(audioFilePath);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        try {
            mediaRecorder.prepare();
            mediaRecorder.start();
            Log.d("SOS", "Recording started...");
            // Stop and upload after 10 seconds
            new android.os.Handler().postDelayed(SosUtils::stopAndUploadAudio, 10000);
        } catch (IOException e) {
            Log.e("SOS", "Recording failed", e);
        }
    }

    /**
     * Stop recording and upload to Firebase
     */
    private static void stopAndUploadAudio() {
        try {
            mediaRecorder.stop();
            mediaRecorder.release();
            mediaRecorder = null;
            Log.d("SOS", "Recording stopped. Uploading to Firebase...");
            uploadAudioToFirebase();
        } catch (Exception e) {
            Log.e("SOS", "Stop recording failed", e);
        }
    }

    /**
     * Upload audio file to Firebase Storage
     */
    private static void uploadAudioToFirebase() {
        if (audioFilePath == null) return;

        StorageReference storageRef = FirebaseStorage.getInstance().getReference();
        Uri fileUri = Uri.fromFile(new File(audioFilePath));
        StorageReference audioRef = storageRef.child("sos_audios/" + fileUri.getLastPathSegment());

        audioRef.putFile(fileUri)
                .addOnSuccessListener(taskSnapshot -> Log.d("SOS", "Audio uploaded successfully"))
                .addOnFailureListener(e -> Log.e("SOS", "Audio upload failed", e));
    }
}
