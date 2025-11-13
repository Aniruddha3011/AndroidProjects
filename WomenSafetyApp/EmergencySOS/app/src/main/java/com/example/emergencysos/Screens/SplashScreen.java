package com.example.emergencysos.Screens;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.emergencysos.R;
import com.example.emergencysos.Services.ShakeService;


public class SplashScreen extends AppCompatActivity {
    ImageView imageView;
    Animation topAnim,bottomAnim,zoom;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash_screen);

        imageView=findViewById(R.id.splashimage);

        topAnim= AnimationUtils.loadAnimation(this,R.anim.top_anim);
        bottomAnim=AnimationUtils.loadAnimation(this,R.anim.bottom_anim);
        zoom=AnimationUtils.loadAnimation(this,R.anim.zoom_in_zoom_out);


        imageView.setAnimation(zoom);

       Thread thread = new Thread() {
           public void run(){
               try{
                   sleep(2800);
               }
               catch (Exception e){
                   e.printStackTrace();
               }
               finally {
                   Intent intent=new Intent(SplashScreen.this, SignIn.class);
                   startActivity(intent);
                   finish();
               }
           }
       };
       thread.start();
        Intent serviceIntent = new Intent(this, ShakeService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            if (pm != null && !pm.isIgnoringBatteryOptimizations(getPackageName())) {
                Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            }
        }



//        String[] permissions = {
//                Manifest.permission.RECORD_AUDIO,
//                Manifest.permission.SEND_SMS,
//                Manifest.permission.ACCESS_FINE_LOCATION,
//                Manifest.permission.ACCESS_COARSE_LOCATION,
//                Manifest.permission.CALL_PHONE,
//                Manifest.permission.WRITE_EXTERNAL_STORAGE
//        };
//
//        ActivityCompat.requestPermissions(this, permissions, 1);
//
//
//        serviceIntent = new Intent(this, VoiceCommandService.class);
//        startService(serviceIntent);





    }
}