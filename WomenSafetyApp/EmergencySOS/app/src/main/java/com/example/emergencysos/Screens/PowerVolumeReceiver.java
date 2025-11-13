package com.example.emergencysos.Screens;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.view.KeyEvent;

public class PowerVolumeReceiver extends BroadcastReceiver {
    private static long lastPowerTime = 0;
    private static boolean volumePressed = false;

    @Override
    public void onReceive(Context context, Intent intent) {
        // Volume button press
        if (Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())) {
            KeyEvent keyEvent = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
            if (keyEvent != null && keyEvent.getKeyCode() == KeyEvent.KEYCODE_VOLUME_DOWN &&
                    keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
                volumePressed = true;
                checkTrigger(context);
            }
        }

        // Power button press (simulated via screen off)
        if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastPowerTime < 1000) {
                checkTrigger(context);
            }
            lastPowerTime = currentTime;
        }
    }

    private void checkTrigger(Context context) {
        if (volumePressed) {
            volumePressed = false;
            Intent serviceIntent = new Intent(context, EmergencyService.class);
            context.startService(serviceIntent);
        }
    }
}
