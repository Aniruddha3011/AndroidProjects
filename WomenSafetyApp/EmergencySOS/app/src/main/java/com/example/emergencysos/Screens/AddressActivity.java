package com.example.emergencysos.Screens;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.emergencysos.Models.Modeluserprofile;
import com.example.emergencysos.databinding.ActivityAddressBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

public class AddressActivity extends AppCompatActivity {
    ActivityAddressBinding binding;
    FirebaseAuth auth;
    FirebaseDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddressBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();

        binding.btnPrevious.setOnClickListener(v -> finish());

        binding.btnSubmit.setOnClickListener(view -> {
            String house = binding.ethouseno.getText().toString();
            String road = binding.etRoad.getText().toString();
            String landmark = binding.etlandmark.getText().toString();
            String pincode = binding.etPincode.getText().toString();
            String city = binding.etcity.getText().toString();
            String state = binding.etstate.getText().toString();

            if (house.isEmpty()) {
                binding.ethouseno.setError("Enter House No");
                return;
            }
            if (road.isEmpty()) {
                binding.etRoad.setError("Enter Road");
                return;
            }
            if (landmark.isEmpty()) {
                binding.etlandmark.setError("Enter Landmark");
                return;
            }
            if (pincode.isEmpty()) {
                binding.etPincode.setError("Enter Pincode");
                return;
            }
            if (city.isEmpty()) {
                binding.etcity.setError("Enter City");
                return;
            }
            if (state.isEmpty()) {
                binding.etstate.setError("Enter State");
                return;
            }

            UserDataHolder data = UserDataHolder.getInstance();
            data.houseNo = house;
            data.road = road;
            data.landmark = landmark;
            data.pincode = pincode;
            data.city = city;
            data.state = state;

            Modeluserprofile model = new Modeluserprofile(
                    data.fname,
                    data.lname,
                    data.age,
                    data.mobno,
                    data.emergencyContact,
                    data.houseNo,
                    data.landmark,
                    data.road,
                    data.pincode,
                    data.city,
                    data.state,
                    data.profileImageUrl
            );

            String uid = auth.getUid();
            database.getReference().child("Users_Info").child(uid).setValue(model);


            UserDataHolder.getInstance();  // not resetting to null for safety
            startActivity(new Intent(AddressActivity.this, HomePageActivity.class));
            finishAffinity(); // Optional: clear back stack
        });
    }
}
