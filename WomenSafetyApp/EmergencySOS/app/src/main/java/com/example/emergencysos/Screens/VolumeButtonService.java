package com.example.emergencysos.Screens;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.pm.PackageManager;
import android.content.Intent;
import android.location.Location;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.telephony.SmsManager;
import android.view.KeyEvent;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.example.emergencysos.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

public class VolumeButtonService extends Service {

    private MediaSession mediaSession;
    private boolean volUpPressed = false;
    private boolean volDownPressed = false;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private boolean isSendingSOS = false;

    @Override
    public void onCreate() {
        super.onCreate();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        startForegroundServiceWithNotification();
        initMediaSession();
    }

    @SuppressLint("ForegroundServiceType")
    private void startForegroundServiceWithNotification() {
        String channelId = "sos_service_channel";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "SOS Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }

        Notification notification = new NotificationCompat.Builder(this, channelId)
                .setContentTitle("SOS Monitoring Active")
                .setContentText("Listening for Volume+ & Volume- combo")
                .setSmallIcon(R.drawable.warning) // your icon here
                .setOngoing(true)
                .build();

        startForeground(1, notification);
    }

    private void initMediaSession() {
        mediaSession = new MediaSession(this, "SOSMediaSession");

        mediaSession.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS | MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);

        PlaybackState state = new PlaybackState.Builder()
                .setActions(PlaybackState.ACTION_PLAY_PAUSE)
                .setState(PlaybackState.STATE_PLAYING, 0, 0)
                .build();
        mediaSession.setPlaybackState(state);

        mediaSession.setCallback(new MediaSession.Callback() {
            @Override
            public boolean onMediaButtonEvent(@NonNull Intent mediaButtonIntent) {
                KeyEvent event = mediaButtonIntent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
                if (event != null && event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_UP) {
                        volUpPressed = true;
                        checkCombo();
                    }
                    if (event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_DOWN) {
                        volDownPressed = true;
                        checkCombo();
                    }
                } else if (event != null && event.getAction() == KeyEvent.ACTION_UP) {
                    if (event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_UP) volUpPressed = false;
                    if (event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_DOWN) volDownPressed = false;
                }
                return super.onMediaButtonEvent(mediaButtonIntent);
            }
        });

        mediaSession.setActive(true);
    }

    private void checkCombo() {
        if (volUpPressed && volDownPressed) {
            triggerSOS();
        }
    }

    private void triggerSOS() {
        if (isSendingSOS) return; // prevent multiple triggers
        isSendingSOS = true;

        Toast.makeText(this, "SOS triggered by Volume+ & Volume-", Toast.LENGTH_SHORT).show();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Location permission not granted", Toast.LENGTH_SHORT).show();
            isSendingSOS = false;
            return;
        }

        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(1000);
        locationRequest.setNumUpdates(1); // single update

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult == null) {
                    isSendingSOS = false;
                    return;
                }
                Location location = locationResult.getLastLocation();
                if (location != null) {
                    sendSmsToEmergencyContacts(location);
                }
                fusedLocationClient.removeLocationUpdates(locationCallback);
                isSendingSOS = false;
            }
        };

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, new Handler().getLooper());
    }

    private void sendSmsToEmergencyContacts(Location location) {
        // TODO: Replace this with your actual emergency contacts loading logic
        String[] emergencyContacts = {"+919876543210", "+918208331164"};

        String message = "Emergency! Live location: https://maps.google.com/?q="
                + location.getLatitude() + "," + location.getLongitude();

        SmsManager smsManager = SmsManager.getDefault();
        for (String number : emergencyContacts) {
            smsManager.sendTextMessage(number, null, message, null, null);
        }

        Toast.makeText(this, "Live location SMS sent to emergency contacts", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaSession != null) {
            mediaSession.release();
        }
        if (locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
