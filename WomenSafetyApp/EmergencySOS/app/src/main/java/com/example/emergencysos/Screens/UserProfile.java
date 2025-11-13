package com.example.emergencysos.Screens;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.emergencysos.Models.Modeluserprofile;
import com.example.emergencysos.databinding.ActivityUserprofileBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

public class UserProfile extends AppCompatActivity {

    FirebaseAuth auth;
    FirebaseDatabase database;
    ActivityUserprofileBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding=ActivityUserprofileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        auth=FirebaseAuth.getInstance();
        database=FirebaseDatabase.getInstance();
        String uid=auth.getUid();

        binding.btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
              if(binding.etfirst.getText().toString().isEmpty()){
                  binding.etfirst.setError("Please Enter First Name");
              }
              if(binding.etmiddle.getText().toString().isEmpty()){
                  binding.etmiddle.setError("Please Enter Middle Name");
              }
              if(binding.etlast.getText().toString().isEmpty()){
                  binding.etlast.setError("Please Enter Last Name");
              }
              if(binding.etphone.getText().toString().isEmpty()){
                  binding.etphone.setError("Please Enter Phone Number");
              }
              if(binding.etAge.getText().toString().isEmpty()){
                  binding.etAge.setError("Please Enter Age");
              }
              if(binding.etEmergencyContact.getText().toString().isEmpty()){
                  binding.etEmergencyContact.setError("Please Enter Emergency Contact");
              }
              if(binding.ethouseno.getText().toString().isEmpty()){
                  binding.ethouseno.setError("Please Enter House No");
              }
              if(binding.etlandmark.getText().toString().isEmpty()){
                  binding.etlandmark.setError("Please Enter Landmark");
              }
              if(binding.etRoad.getText().toString().isEmpty()){
                  binding.etRoad.setError("Please Enter Road");
              }
              if(binding.etPincode.getText().toString().isEmpty()){
                  binding.etPincode.setError("Please Enter Pincode");
              }
              if(binding.etcity.getText().toString().isEmpty()){
                  binding.etcity.setError("Please Enter City");
              }
              if(binding.etstate.getText().toString().isEmpty()){
                  binding.etstate.setError("Please Enter State");
              }
              else{
                  Modeluserprofile modeluserprofile=new Modeluserprofile(binding.etfirst.getText().toString(),binding.etlast.getText().toString(),binding.etAge.getText().toString(),binding.etphone.getText().toString(),binding.etEmergencyContact.getText().toString(),binding.ethouseno.getText().toString(),binding.etlandmark.getText().toString(),binding.etRoad.getText().toString(),binding.etPincode.getText().toString(),binding.etcity.getText().toString(),binding.etstate.getText().toString(),"");
                  database.getReference().child("Users_Info").child(uid).setValue(modeluserprofile);
                  Intent intent=new Intent(UserProfile.this, MainActivity.class);
                  startActivity(intent);
              }


            }
        });



    }
}