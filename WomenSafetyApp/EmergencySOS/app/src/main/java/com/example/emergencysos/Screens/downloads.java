package com.example.emergencysos.Screens;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.emergencysos.Adapters.RecordingAdapter;
import com.example.emergencysos.Models.RecordingModel;
import com.example.emergencysos.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.List;

public class downloads extends AppCompatActivity {

    private List<RecordingModel> allRecordings = new ArrayList<>();
    private List<RecordingModel> filteredRecordings = new ArrayList<>();
    private RecordingAdapter adapter;
    private SharedPreferences deletedPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(com.example.emergencysos.R.layout.activity_downloads);

        RecyclerView recyclerView = findViewById(R.id.recordingsRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Initialize SharedPreferences to store deleted items
        deletedPrefs = getSharedPreferences("deleted_recordings", MODE_PRIVATE);

        // Set up adapter with filtered list
        adapter = new RecordingAdapter(this, filteredRecordings);
        recyclerView.setAdapter(adapter);

        FirebaseAuth auth = FirebaseAuth.getInstance();
        String uid = auth.getUid();
        DatabaseReference dbRef = FirebaseDatabase.getInstance()
                .getReference("recordings")
                .child(uid);

        dbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allRecordings.clear();
                filteredRecordings.clear();

                for (DataSnapshot snap : snapshot.getChildren()) {
                    RecordingModel model = snap.getValue(RecordingModel.class);
                    if (model != null && !deletedPrefs.getBoolean(model.downloadUrl, false)) {
                        filteredRecordings.add(model);

                    }
                }

                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(downloads.this, "Failed to load recordings", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
