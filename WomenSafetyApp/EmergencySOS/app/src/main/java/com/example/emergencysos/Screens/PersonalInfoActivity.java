package com.example.emergencysos.Screens;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.emergencysos.databinding.ActivityPersonalInfoBinding;

public class PersonalInfoActivity extends AppCompatActivity {
    ActivityPersonalInfoBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPersonalInfoBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnPrevious.setOnClickListener(v -> finish());

        binding.btnNext.setOnClickListener(view -> {
            String phone = binding.etphone.getText().toString().trim();
            String age = binding.etAge.getText().toString().trim();
            String emergency = binding.etEmergencyContact.getText().toString().trim();

            if (phone.isEmpty()) {
                binding.etphone.setError("Enter Phone");
                return;
            }
            if (age.isEmpty()) {
                binding.etAge.setError("Enter Age");
                return;
            }
            if (emergency.isEmpty()) {
                binding.etEmergencyContact.setError("Enter Emergency Contact");
                return;
            }

            // Store emergency contact and name info in SharedPreferences
            SharedPreferences prefs = getSharedPreferences("emergency_contacts", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("phone", emergency);
            editor.putString("fname", UserDataHolder.getInstance().fname);
            editor.putString("lname", UserDataHolder.getInstance().lname);


            editor.putBoolean("sharedprefs_default_added", false);

            editor.apply();


            // Store other data
            UserDataHolder data = UserDataHolder.getInstance();
            data.mobno = phone;
            data.age = age;
            data.emergencyContact = emergency;

            startActivity(new Intent(PersonalInfoActivity.this, AddressActivity.class));
        });
    }
}
