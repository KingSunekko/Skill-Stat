package com.example.skillstat;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.skillstat.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class SignUpActivity extends AppCompatActivity {

    private static final String TAG = "SignUpActivity";
    private EditText etEmail, etPassword;
    private ImageView ivPasswordToggle;
    private boolean isPasswordVisible = false;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sign_up);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Edge-to-edge insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Set dark status/nav bar colors to match background
        getWindow().setStatusBarColor(getResources().getColor(R.color.splash_bg_dark, getTheme()));
        getWindow().setNavigationBarColor(getResources().getColor(R.color.splash_bg_dark, getTheme()));

        // ── View references ──────────────────────────────────────────────────
        etEmail          = findViewById(R.id.et_email);
        etPassword       = findViewById(R.id.et_password);
        ivPasswordToggle = findViewById(R.id.iv_password_toggle);
        Button   btnCreate   = findViewById(R.id.btn_create_account);
        Button   btnFacebook = findViewById(R.id.btn_facebook);
        Button   btnGoogle   = findViewById(R.id.btn_google);
        TextView tvLogIn     = findViewById(R.id.tv_log_in);

        // ── Password show / hide toggle ──────────────────────────────────────
        ivPasswordToggle.setOnClickListener(v -> {
            if (isPasswordVisible) {
                etPassword.setInputType(
                        InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                ivPasswordToggle.setImageResource(R.drawable.ic_eye_off);
                isPasswordVisible = false;
            } else {
                etPassword.setInputType(
                        InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                ivPasswordToggle.setImageResource(R.drawable.ic_eye_on);
                isPasswordVisible = true;
            }
            etPassword.setSelection(etPassword.getText().length());
        });

        // ── CREATE ACCOUNT button logic ──────────────────────────────────────
        btnCreate.setOnClickListener(v -> {
            String email    = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (TextUtils.isEmpty(email)) {
                etEmail.setError("Email is required");
                etEmail.requestFocus();
                return;
            }
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etEmail.setError("Enter a valid email");
                etEmail.requestFocus();
                return;
            }
            if (TextUtils.isEmpty(password)) {
                etPassword.setError("Password is required");
                etPassword.requestFocus();
                return;
            }
            if (password.length() < 6) {
                etPassword.setError("Minimum 6 characters");
                etPassword.requestFocus();
                return;
            }

            // Firebase Create User
            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser firebaseUser = mAuth.getCurrentUser();
                            saveUserToDatabase(firebaseUser);
                        } else {
                            String errorMessage = task.getException() != null ? task.getException().getMessage() : "Authentication failed.";
                            Toast.makeText(SignUpActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                        }
                    });
        });

        // ── Social Sign Up placeholders ──────────────────────────────────────
        btnFacebook.setOnClickListener(v ->
                Toast.makeText(this, "Facebook sign up coming soon!", Toast.LENGTH_SHORT).show());

        btnGoogle.setOnClickListener(v ->
                Toast.makeText(this, "Use Login screen for Google Sign-In", Toast.LENGTH_SHORT).show());

        // ── Navigate back to Login ───────────────────────────────────────────
        tvLogIn.setOnClickListener(v -> {
            Intent intent = new Intent(SignUpActivity.this, LoginActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.gentle_fade_in, R.anim.slide_down_exit);
            finish();
        });
    }

    private void saveUserToDatabase(FirebaseUser firebaseUser) {
        if (firebaseUser == null) return;

        // Default username from email if display name is null
        String username = firebaseUser.getEmail().split("@")[0];
        User user = new User(firebaseUser.getUid(), username, firebaseUser.getEmail());

        mDatabase.child("users").child(firebaseUser.getUid()).setValue(user)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(SignUpActivity.this, "Account created successfully!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(SignUpActivity.this, OnboardingActivity.class);
                    startActivity(intent);
                    overridePendingTransition(R.anim.slide_up_enter, R.anim.gentle_fade_out);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Database error", e);
                    Toast.makeText(SignUpActivity.this, "Failed to save user data", Toast.LENGTH_SHORT).show();
                    // Still proceed to onboarding as the auth succeeded
                    Intent intent = new Intent(SignUpActivity.this, OnboardingActivity.class);
                    startActivity(intent);
                    finish();
                });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.gentle_fade_in, R.anim.slide_down_exit);
    }
}
