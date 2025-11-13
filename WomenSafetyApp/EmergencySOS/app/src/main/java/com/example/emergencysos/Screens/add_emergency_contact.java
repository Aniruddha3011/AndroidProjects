package com.example.emergencysos.Screens;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.emergencysos.Adapters.EmergencyContactAdapter;
import com.example.emergencysos.Models.EmergencyContact;
import com.example.emergencysos.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class add_emergency_contact extends AppCompatActivity {

    RecyclerView recyclerView;
    EmergencyContactAdapter adapter;
    ArrayList<EmergencyContact> contactList = new ArrayList<>();
    SharedPreferences prefs;
    ImageView backBtn;

    EditText nameInput, phoneInput, relationInput;
    Button pickContactBtn;

    private static final int REQUEST_CONTACT_PICK = 101;
    private boolean firebaseLoaded = false; // New flag

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.example.emergencysos.R.layout.activity_add_emergency_contact);

        prefs = getSharedPreferences("emergency_contacts", MODE_PRIVATE);

        recyclerView = findViewById(com.example.emergencysos.R.id.recycler_contacts);
        nameInput = findViewById(com.example.emergencysos.R.id.et_contact_name);
        phoneInput = findViewById(com.example.emergencysos.R.id.et_contact_phone);
        relationInput = findViewById(com.example.emergencysos.R.id.et_contact_relation);
        pickContactBtn = findViewById(com.example.emergencysos.R.id.btn_pick_contact);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new EmergencyContactAdapter(contactList, this);
        recyclerView.setAdapter(adapter);

        loadContactList();
        loadFirebaseDefaultContact();

        // Only load SharedPreferences default if Firebase doesn't load
        if (!firebaseLoaded) {
            loadDefaultEmergencyContactFromPrefs();
        }

        findViewById(com.example.emergencysos.R.id.btn_save_contact).setOnClickListener(v -> {
            String name = nameInput.getText().toString().trim();
            String phone = phoneInput.getText().toString().trim();
            String relation = relationInput.getText().toString().trim();

            if (name.isEmpty() || phone.isEmpty() || relation.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (isDuplicate(name, phone)) {
                Toast.makeText(this, "Contact already exists", Toast.LENGTH_SHORT).show();
                return;
            }

            EmergencyContact newContact = new EmergencyContact(name, phone, relation);
            contactList.add(newContact);
            adapter.notifyDataSetChanged();
            saveContactList();

            nameInput.setText("");
            phoneInput.setText("");
            relationInput.setText("");

            Toast.makeText(this, "Contact saved", Toast.LENGTH_SHORT).show();
        });

        // Pick contact button click
        pickContactBtn.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
            startActivityForResult(intent, REQUEST_CONTACT_PICK);
        });

        backBtn = findViewById(R.id.backarrow);
        backBtn.setOnClickListener(v -> {
            onBackPressed();
        });
    }

    // Contact Picker Result
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CONTACT_PICK && resultCode == RESULT_OK && data != null) {
            Uri contactUri = data.getData();
            Cursor cursor = getContentResolver().query(contactUri,
                    null, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                String name = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                String phone = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER));

                nameInput.setText(name);
                phoneInput.setText(phone);
                relationInput.setText("Friend"); // Default or allow user to change

                cursor.close();
            }
        }
    }

    private void loadContactList() {
        contactList.clear();
        Set<String> savedSet = prefs.getStringSet("contacts", new HashSet<>());
        for (String entry : savedSet) {
            String[] parts = entry.split(",");
            if (parts.length == 3) {
                contactList.add(new EmergencyContact(parts[0], parts[1], parts[2]));
            }
        }
    }

    private void saveContactList() {
        Set<String> saveSet = new HashSet<>();
        for (EmergencyContact c : contactList) {
            saveSet.add(c.getName() + "," + c.getPhone() + "," + c.getRelation());
        }
        prefs.edit().putStringSet("contacts", saveSet).apply();
    }

    private void loadFirebaseDefaultContact() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("Users_Info").child(uid);

        // Real-time listener so number always updates
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String em = snapshot.child("Emergency_contact").getValue(String.class);
                String fn = snapshot.child("Fname").getValue(String.class);
                String ln = snapshot.child("Lname").getValue(String.class);

                if (em != null && fn != null && ln != null) {
                    String full = fn + " " + ln;

                    // Remove old firebase default contact
                    contactList.removeIf(c -> "Default".equalsIgnoreCase(c.getRelation()));

                    EmergencyContact def = new EmergencyContact(full, em, "Default");
                    contactList.add(def);
                    saveContactList();
                    firebaseLoaded = true; // Mark Firebase as loaded
                    prefs.edit().putBoolean("firebase_default_added", true).apply();
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(add_emergency_contact.this, "Failed to load emergency contact", Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void loadDefaultEmergencyContactFromPrefs() {
        String emergencyPhone = prefs.getString("phone", null);
        String fname = prefs.getString("fname", "");
        String lname = prefs.getString("lname", "");
        boolean alreadyAdded = prefs.getBoolean("sharedprefs_default_added", false);

        if (emergencyPhone != null && !alreadyAdded) {
            // Remove old default contact if exists
            contactList.removeIf(c -> "Default".equalsIgnoreCase(c.getRelation()));

            String fullName = (fname + " " + lname).trim();
            if (fullName.isEmpty()) fullName = "Emergency Contact";
            String relation = "Default";

            EmergencyContact defaultContact = new EmergencyContact(fullName, emergencyPhone, relation);
            contactList.add(defaultContact);
            saveContactList();

            prefs.edit().putBoolean("sharedprefs_default_added", true).apply();
            adapter.notifyDataSetChanged();
        }
    }

    private boolean isDuplicate(String name, String phone) {
        for (EmergencyContact c : contactList) {
            if (c.getName().equalsIgnoreCase(name) && c.getPhone().equals(phone)) {
                return true;
            }
        }
        return false;
    }
}
