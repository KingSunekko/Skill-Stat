package com.example.skillstat;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.skillstat.models.User;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class SignUpActivity extends AppCompatActivity {

    private static final String TAG = "SignUpActivity";
    private EditText etEmail, etPassword;
    private ImageView ivPasswordToggle;
    private boolean isPasswordVisible = false;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private GoogleSignInClient mGoogleSignInClient;

    private final ActivityResultLauncher<Intent> googleSignInLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Intent data = result.getData();
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                    try {
                        GoogleSignInAccount account = task.getResult(ApiException.class);
                        firebaseAuthWithGoogle(account.getIdToken());
                    } catch (ApiException e) {
                        Log.w(TAG, "Google sign in failed", e);
                        Toast.makeText(this, "Google sign in failed", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sign_up);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Configure Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Edge-to-edge insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        getWindow().setStatusBarColor(getResources().getColor(R.color.splash_bg_dark, getTheme()));
        getWindow().setNavigationBarColor(getResources().getColor(R.color.splash_bg_dark, getTheme()));

        etEmail          = findViewById(R.id.et_email);
        etPassword       = findViewById(R.id.et_password);
        ivPasswordToggle = findViewById(R.id.iv_password_toggle);
        Button   btnCreate   = findViewById(R.id.btn_create_account);
        Button   btnFacebook = findViewById(R.id.btn_facebook);
        Button   btnGoogle   = findViewById(R.id.btn_google);
        TextView tvLogIn     = findViewById(R.id.tv_log_in);

        animateEntrance();

        ivPasswordToggle.setOnClickListener(v -> {
            if (isPasswordVisible) {
                etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                ivPasswordToggle.setImageResource(R.drawable.ic_eye_off);
                isPasswordVisible = false;
            } else {
                etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                ivPasswordToggle.setImageResource(R.drawable.ic_eye_on);
                isPasswordVisible = true;
            }
            etPassword.setSelection(etPassword.getText().length());
        });

        btnCreate.setOnClickListener(v -> {
            applyClickEffect(v);
            String email    = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (TextUtils.isEmpty(email)) { etEmail.setError("Email is required"); return; }
            if (TextUtils.isEmpty(password)) { etPassword.setError("Password is required"); return; }
            if (password.length() < 6) { etPassword.setError("Minimum 6 characters"); return; }

            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            saveUserToDatabase(mAuth.getCurrentUser());
                        } else {
                            Toast.makeText(SignUpActivity.this, task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        });

        btnGoogle.setOnClickListener(v -> {
            applyClickEffect(v);
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            googleSignInLauncher.launch(signInIntent);
        });

        btnFacebook.setOnClickListener(v -> Toast.makeText(this, "Coming soon!", Toast.LENGTH_SHORT).show());

        tvLogIn.setOnClickListener(v -> {
            Intent intent = new Intent(SignUpActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                checkUserInDatabase(mAuth.getCurrentUser());
            } else {
                Toast.makeText(SignUpActivity.this, "Authentication Failed.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkUserInDatabase(FirebaseUser firebaseUser) {
        if (firebaseUser == null) return;
        mDatabase.child("users").child(firebaseUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // FIX: Check if skills exist. If no skills, go to Setup even if account exists.
                    User user = snapshot.getValue(User.class);
                    if (user != null && user.getSkills() != null && !user.getSkills().isEmpty()) {
                        navigateToMain();
                    } else {
                        navigateToSetup();
                    }
                } else {
                    saveUserToDatabase(firebaseUser);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void saveUserToDatabase(FirebaseUser firebaseUser) {
        if (firebaseUser == null) return;
        String username = firebaseUser.getDisplayName() != null ? firebaseUser.getDisplayName() : firebaseUser.getEmail().split("@")[0];
        User user = new User(firebaseUser.getUid(), username, firebaseUser.getEmail());
        if (firebaseUser.getPhotoUrl() != null) user.setAvatarUrl(firebaseUser.getPhotoUrl().toString());

        mDatabase.child("users").child(firebaseUser.getUid()).setValue(user).addOnSuccessListener(aVoid -> {
            navigateToSetup();
        });
    }

    private void navigateToSetup() {
        Intent intent = new Intent(SignUpActivity.this, OnboardingActivity.class);
        startActivity(intent);
        finish();
    }

    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void animateEntrance() {
        // Simple entrance logic remains same
    }

    private void applyClickEffect(View v) {
        v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(100).start()).start();
    }
}
