package com.example.emergencysos.Screens;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.emergencysos.R;

public class CheckInService extends Service {

    private static final String CHANNEL_ID = "CheckInChannel";
    private static final int CHECK_IN_INTERVAL = 60 * 1000; // 1 min interval for testing
    private Handler handler;
    private Runnable checkInRunnable;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(1, getNotification());

        handler = new Handler();
        checkInRunnable = new Runnable() {
            @Override
            public void run() {
                sendCheckIn();
                handler.postDelayed(this, CHECK_IN_INTERVAL);
            }
        };
        handler.post(checkInRunnable);

        Log.d("CheckInService", "Service started");
    }

    private void sendCheckIn() {
        // You can replace this with actual logic — for now it just logs
        Log.d("CheckInService", "Check-in sent to emergency contacts!");
        // Example: Call API or send SMS location to emergency contacts here
    }

    private Notification getNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Check-In Active")
                .setContentText("Your safety check-in is running.")
                .setSmallIcon(R.drawable.warning)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Check-In Service Channel",
                    NotificationManager.IMPORTANCE_HIGH
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (handler != null && checkInRunnable != null) {
            handler.removeCallbacks(checkInRunnable);
        }
        Log.d("CheckInService", "Service stopped");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
