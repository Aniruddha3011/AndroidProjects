package com.example.emergencysos.Screens;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.emergencysos.databinding.ActivityNameBinding;

public class NameActivity extends AppCompatActivity {
    ActivityNameBinding binding;

    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityNameBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Uploading Your Data");

        binding.btnNext.setOnClickListener(view -> {
            String fname = binding.etfirst.getText().toString();
            String lname = binding.etlast.getText().toString();

            if (fname.isEmpty()) {
                binding.etfirst.setError("Enter First Name");
                return;
            }
            if (lname.isEmpty()) {
                binding.etlast.setError("Enter Last Name");
                return;
            }

            SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            prefs.edit()
                    .putString("firstName", fname)
                    .putString("lastName", lname)
                    .apply();


            UserDataHolder.getInstance().fname = fname;
            UserDataHolder.getInstance().lname = lname;

            // ✅ No image upload here, just go to next activity
            startActivity(new Intent(NameActivity.this, PersonalInfoActivity.class));
        });

        progressDialog.dismiss();
    }
}
