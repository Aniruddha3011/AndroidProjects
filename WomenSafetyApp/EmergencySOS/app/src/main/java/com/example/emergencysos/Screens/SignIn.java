package com.example.emergencysos.Screens;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.emergencysos.R;
import com.example.emergencysos.databinding.ActivitySignInBinding;
import com.facebook.CallbackManager;
import com.google.android.gms.auth.api.signin.*;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.*;
import com.google.firebase.database.*;

import java.util.HashMap;
import java.util.Objects;

public class SignIn extends AppCompatActivity {

    private ActivitySignInBinding binding;
    private FirebaseAuth auth;
    private GoogleSignInClient googleClient;
    private CallbackManager fbCallback; // Facebook login was commented out, still declared
    private static final int RC_SIGN_IN = 1001;
    private DatabaseReference dbRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivitySignInBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        auth = FirebaseAuth.getInstance();
        dbRef = FirebaseDatabase.getInstance().getReference();

        setupGoogleSignIn();

        binding.loginButton.setOnClickListener(v -> {
            String email = binding.Email.getText().toString().trim();
            String pass = binding.password.getText().toString().trim();

            if (email.isEmpty()) {
                binding.Email.setError("Please Enter Email");
                return;
            }
            if (pass.isEmpty()) {
                binding.password.setError("Please Enter Password");
                return;
            }

            auth.signInWithEmailAndPassword(email, pass)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(this, "User Exist", Toast.LENGTH_SHORT).show();
                            checkUserProfile(auth.getCurrentUser());
                        } else {
                            Toast.makeText(this, Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        binding.Signup.setOnClickListener(v -> startActivity(new Intent(SignIn.this, SignUp.class)));


    }

    private void setupGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        googleClient = GoogleSignIn.getClient(this, gso);

        binding.google.setOnClickListener(v -> {
            Intent signInIntent = googleClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_SIGN_IN);
        });

        if(auth.getCurrentUser()!=null){
            Intent intent = new Intent(SignIn.this, HomePageActivity.class);
            startActivity(intent);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        // Safely ignore Facebook CallbackManager if it's not used (avoids null crash)
        if (fbCallback != null) {
            fbCallback.onActivityResult(requestCode, resultCode, data);
        }

        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
                auth.signInWithCredential(credential)
                        .addOnCompleteListener(this, t -> {
                            if (t.isSuccessful()) {
                                saveBasicUserInfo(auth.getCurrentUser());
                                checkUserProfile(auth.getCurrentUser());
                                Toast.makeText(this, "Google Auth Successful", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(this, "Google Auth Failed", Toast.LENGTH_SHORT).show();
                            }
                        });
            } catch (ApiException e) {
                Toast.makeText(this, "Google Sign-In Failed", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void checkUserProfile(FirebaseUser user) {
        if (user == null) return;

        dbRef.child("Users_Info").child(user.getUid()).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                Toast.makeText(this, "Profile exists.", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, HomePageActivity.class));
            } else {
                Toast.makeText(this, "Profile missing.", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, NameActivity.class));
            }
            finish();
        });
    }


    private void saveBasicUserInfo(FirebaseUser user) {
        if (user == null) return;

        HashMap<String, String> map = new HashMap<>();
        map.put("uid", user.getUid());
        map.put("name", user.getDisplayName() != null ? user.getDisplayName() : "Username");
        map.put("email", user.getEmail() != null ? user.getEmail() : "user@gmail.com");

        dbRef.child("Users").child(user.getUid()).setValue(map)
                .addOnSuccessListener(unused ->
                        Toast.makeText(this, "User info saved", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Save failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

}
