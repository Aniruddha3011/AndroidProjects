package com.example.emergencysos.Screens;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.emergencysos.R;
import com.example.emergencysos.databinding.ActivityUserInfoBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class UserInfoActivity extends AppCompatActivity {

    @NonNull
    ActivityUserInfoBinding binding;

    private Uri imageUri;
    private FirebaseStorage storage;
    private StorageReference storageReference;

    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityUserInfoBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Uploading...");

        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);

        String savedUrl = prefs.getString("profileImageUrl", null);
        if (savedUrl != null && !savedUrl.isEmpty()) {
            UserDataHolder.getInstance().profileImageUrl = savedUrl;
            Glide.with(this)
                    .load(savedUrl)
                    .placeholder(R.drawable.person2)
                    .into(binding.profileimage);
        }

        String firstName = prefs.getString("firstName", "");
        String lastName = prefs.getString("lastName", "");
        binding.user.setText(firstName);
        binding.name.setText(lastName);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String userEmail = currentUser.getEmail();
            binding.usserid.setText(userEmail);
        } else {
            binding.usserid.setText("User not signed in");
        }

        ActivityResultLauncher<String> pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        imageUri = uri;
                        binding.profileimage.setImageURI(imageUri);
                        uploadImageToFirebase(imageUri);
                    }
                }
        );

        binding.profileimage.setOnClickListener(view -> {
            pickImageLauncher.launch("image/*");
        });

        binding.Recording.setOnClickListener(view -> {
            Intent intent = new Intent(UserInfoActivity.this, downloads.class);
            startActivity(intent);
        });

        binding.Report.setOnClickListener(view -> {
            Intent intent = new Intent(UserInfoActivity.this, ReportsActivity.class);
            startActivity(intent);
        });

        binding.editprofile.setOnClickListener(view -> {
            Intent intent = new Intent(UserInfoActivity.this, NameActivity.class);
            startActivity(intent);
        });

        binding.manageCheckIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(UserInfoActivity.this, Safety_check_in.class);
                startActivity(intent);
            }
        });

        binding.logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LayoutInflater inflater = LayoutInflater.from(UserInfoActivity.this);
                View dialogView = inflater.inflate(R.layout.dialog_logout, null);

                Button btnYes = dialogView.findViewById(R.id.btnYes);
                Button btnCancel = dialogView.findViewById(R.id.btnCancel);

                AlertDialog dialog = new AlertDialog.Builder(UserInfoActivity.this)
                        .setView(dialogView)
                        .setCancelable(false)
                        .create();

                btnCancel.setOnClickListener(v -> dialog.dismiss());

                btnYes.setOnClickListener(v -> {
                    FirebaseAuth.getInstance().signOut();
                    startActivity(new Intent(UserInfoActivity.this, SignIn.class));
                    finish();
                    dialog.dismiss();
                });

                dialog.show();
            }
        });
    }

    private void uploadImageToFirebase(Uri uri) {
        progressDialog.show();
        String uid = FirebaseAuth.getInstance().getUid();
        StorageReference fileRef = storageReference.child("profile_images/" + uid + ".jpg");

        fileRef.putFile(uri)
                .addOnSuccessListener(taskSnapshot -> fileRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                    String url = downloadUri.toString();
                    UserDataHolder.getInstance().profileImageUrl = url;

                    getSharedPreferences("UserPrefs", MODE_PRIVATE)
                            .edit()
                            .putString("profileImageUrl", url)
                            .apply();

                    Glide.with(this).load(url).into(binding.profileimage);
                    progressDialog.dismiss();
                    Toast.makeText(UserInfoActivity.this, "Profile image updated", Toast.LENGTH_SHORT).show();
                }))
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(UserInfoActivity.this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
