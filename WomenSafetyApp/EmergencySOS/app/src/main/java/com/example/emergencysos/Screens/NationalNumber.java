package com.example.emergencysos.Screens;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.emergencysos.Adapters.NationalNumberAdapter;
import com.example.emergencysos.R;

import java.util.ArrayList;
import java.util.List;

public class NationalNumber extends AppCompatActivity {

    private static final int REQUEST_CALL_PERMISSION = 101;
    private String pendingPhoneNumber;
    private RecyclerView recyclerView;
    private NationalNumberAdapter adapter;
    private List<Helpline> helplineList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_national_number);

        recyclerView = findViewById(R.id.recyclerViewNumbers);

        helplineList = getNationalHelplines();
        adapter = new NationalNumberAdapter(helplineList, this::makePhoneCall);
        recyclerView.setAdapter(adapter);
    }

    private List<Helpline> getNationalHelplines() {
        List<Helpline> list = new ArrayList<>();
        list.add(new Helpline("Women Helpline", "+917248934207"));
        list.add(new Helpline("Police", "100"));
        list.add(new Helpline("Fire", "101"));
        list.add(new Helpline("Ambulance", "102"));
        list.add(new Helpline("Emergency Disaster Management", "108"));
        list.add(new Helpline("National Emergency Number", "112"));
        list.add(new Helpline("Child Helpline", "1098"));
        list.add(new Helpline("Senior Citizens Helpline", "1291"));
        list.add(new Helpline("Anti Poison (Delhi)", "1066"));
        list.add(new Helpline("Railway Enquiry", "139"));
        list.add(new Helpline("LPG Leak Helpline", "1906"));
        return list;
    }

    private void makePhoneCall(String phoneNumber) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            pendingPhoneNumber = phoneNumber;
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CALL_PHONE}, REQUEST_CALL_PERMISSION);
        } else {
            startActivity(new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + phoneNumber)));
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CALL_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && pendingPhoneNumber != null) {
                makePhoneCall(pendingPhoneNumber);
            } else {
                Toast.makeText(this, "Call Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
