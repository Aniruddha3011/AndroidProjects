package com.example.emergencysos.Screens;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.emergencysos.Models.User;
import com.example.emergencysos.R;
import com.example.emergencysos.databinding.ActivitySignUpBinding;
import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

public class SignUp extends AppCompatActivity {

    ActivitySignUpBinding binding;
    private FirebaseAuth auth;
    private FirebaseDatabase database;
    private CallbackManager callbackManager;
    private GoogleSignInClient mGoogleSignInClient;
    private static final int RC_SIGN_IN = 9001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        FacebookSdk.sdkInitialize(getApplicationContext());
        binding = ActivitySignUpBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();

        binding.Signin.setOnClickListener(view -> {
            Intent intent = new Intent(SignUp.this, SignIn.class);
            startActivity(intent);
        });

        binding.btnSignUp.setOnClickListener(view -> {
            if (binding.etUsername.getText().toString().isEmpty()) {
                binding.etUsername.setError(" Please Enter Username");
                return;
            }
            if (binding.etEmail.getText().toString().isEmpty()) {
                binding.etEmail.setError(" Please Enter Email");
                return;
            }
            if (binding.etPassword.getText().toString().isEmpty()) {
                binding.etPassword.setError(" Please Enter Password");
                return;
            }
            if (binding.etConfirmPassword.getText().toString().isEmpty()) {
                binding.etConfirmPassword.setError(" Please Enter Confirm Password");
                return;
            }
            if (!binding.etPassword.getText().toString().equals(binding.etConfirmPassword.getText().toString())) {
                Toast.makeText(SignUp.this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            auth.createUserWithEmailAndPassword(binding.etEmail.getText().toString(), binding.etPassword.getText().toString()).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    User user = new User(binding.etUsername.getText().toString(), binding.etEmail.getText().toString(), binding.etPassword.getText().toString(), binding.etConfirmPassword.getText().toString());
                    String id = task.getResult().getUser().getUid();
                    database.getReference().child("Users").child(id).setValue(user);
                    Toast.makeText(SignUp.this, "Sign Up Successful", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(SignUp.this, NameActivity.class);
                    startActivity(intent);

                } else {
                    Toast.makeText(SignUp.this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        binding.google.setOnClickListener(v -> {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_SIGN_IN);
        });

        callbackManager = CallbackManager.Factory.create();
        binding.facebook.setOnClickListener(v -> {
            LoginButton fbLoginButton = new LoginButton(this);
            fbLoginButton.setPermissions("email", "public_profile");
            fbLoginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
                @Override
                public void onSuccess(LoginResult loginResult) {
                    handleFacebookAccessToken(loginResult.getAccessToken());
                }

                @Override
                public void onCancel() {
                    Toast.makeText(SignUp.this, "Facebook login cancelled", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onError(FacebookException error) {
                    Toast.makeText(SignUp.this, "Facebook login failed: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
            fbLoginButton.performClick();
        });
    }

    private void handleFacebookAccessToken(AccessToken token) {
        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        auth.signInWithCredential(credential).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                FirebaseUser firebaseUser = auth.getCurrentUser();
                HashMap<String, String> map = new HashMap<>();
                map.put("uid", firebaseUser.getUid());
                map.put("name", firebaseUser.getDisplayName() != null ? firebaseUser.getDisplayName() : "");
                map.put("email", firebaseUser.getEmail() != null ? firebaseUser.getEmail() : "");
                database.getReference().child("Users").child(firebaseUser.getUid()).setValue(map);
                Toast.makeText(SignUp.this, "Facebook Login Successful", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(SignUp.this, NameActivity.class));
            } else {
                Toast.makeText(SignUp.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken(), account);
            } catch (ApiException e) {
                Toast.makeText(this, "Google sign-in failed", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken, GoogleSignInAccount account) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = auth.getCurrentUser();

                        HashMap<String, String> map = new HashMap<>();
                        map.put("uid", firebaseUser.getUid());
                        map.put("name", firebaseUser.getDisplayName() != null ? firebaseUser.getDisplayName() : "");
                        map.put("email", firebaseUser.getEmail() != null ? firebaseUser.getEmail() : "");

                        database.getReference().child("Users").child(firebaseUser.getUid()).setValue(map);
                        Toast.makeText(SignUp.this, "Google Sign-Up Successful", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(SignUp.this, NameActivity.class));
                    } else {
                        Toast.makeText(SignUp.this, "Google sign-Up failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
