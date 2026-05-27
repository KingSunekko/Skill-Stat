package com.example.skillstat;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.animation.AnticipateOvershootInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
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

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private EditText etEmail, etPassword;
    private ImageView ivPasswordToggle;
    private boolean isPasswordVisible = false;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private GoogleSignInClient mGoogleSignInClient;
    private boolean isFirebaseConfigured = true;

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
        setContentView(R.layout.activity_login);

        try {
            mAuth = FirebaseAuth.getInstance();
            mDatabase = FirebaseDatabase.getInstance().getReference();
        } catch (Exception e) {
            isFirebaseConfigured = false;
        }

        try {
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(getString(R.string.default_web_client_id))
                    .requestEmail()
                    .build();
            mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        } catch (Exception e) {
            Log.e(TAG, "Google Sign-In configuration failed", e);
        }

        // Edge-to-edge insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Dark status/nav bar
        getWindow().setStatusBarColor(getResources().getColor(R.color.splash_bg_dark, getTheme()));
        getWindow().setNavigationBarColor(getResources().getColor(R.color.splash_bg_dark, getTheme()));

        // ── View references ──────────────────────────────────────────────────
        etEmail          = findViewById(R.id.et_email);
        etPassword       = findViewById(R.id.et_password);
        ivPasswordToggle = findViewById(R.id.iv_password_toggle);
        Button   btnLogin    = findViewById(R.id.btn_login);
        Button   btnFacebook = findViewById(R.id.btn_facebook);
        Button   btnGoogle   = findViewById(R.id.btn_google);
        TextView tvSignUp     = findViewById(R.id.tv_sign_up);

        // ── Professional Entrance Animations ──────────────────────────────────
        animateEntrance(findViewById(R.id.main));

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

        // ── LOG IN button ────────────────────────────────────────────────────
        btnLogin.setOnClickListener(v -> {
            applyClickEffect(v);
            if (!isFirebaseConfigured) {
                Toast.makeText(this, "Firebase is not set up. Please add google-services.json", Toast.LENGTH_SHORT).show();
                // For development: skip to main if firebase is missing
                navigateToMain();
                return;
            }

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

            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            checkUserInDatabase(mAuth.getCurrentUser());
                        } else {
                            String errorMessage = task.getException() != null ? task.getException().getMessage() : "Login failed.";
                            Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                        }
                    });
        });

        // ── Social buttons ───────────────────────────────────────────────────
        btnFacebook.setOnClickListener(v -> {
            applyClickEffect(v);
            Toast.makeText(this, "Facebook login coming soon!", Toast.LENGTH_SHORT).show();
        });

        btnGoogle.setOnClickListener(v -> {
            applyClickEffect(v);
            if (mGoogleSignInClient != null) {
                Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                googleSignInLauncher.launch(signInIntent);
            } else {
                Toast.makeText(this, "Google Sign-In not configured", Toast.LENGTH_SHORT).show();
            }
        });

        // ── "No account? Sign Up →" link ──────────────────────────────────────
        tvSignUp.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, SignUpActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_up_enter, R.anim.gentle_fade_out);
            finish();
        });

        // Handle back navigation
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
                overridePendingTransition(R.anim.gentle_fade_in, R.anim.slide_down_exit);
            }
        });
    }

    private void animateEntrance(View root) {
        View logo = findViewById(R.id.shape_logo_glow); // Parent or logo view
        View welcomeText = findViewById(android.R.id.content).findViewWithTag("welcome_text"); // I'll search for it or use index

        // Staggered animation for all children of the first LinearLayout
        if (root instanceof androidx.constraintlayout.widget.ConstraintLayout) {
            View scroll = ((androidx.constraintlayout.widget.ConstraintLayout) root).getChildAt(0);
            if (scroll instanceof android.widget.ScrollView) {
                View layout = ((android.widget.ScrollView) scroll).getChildAt(0);
                if (layout instanceof android.widget.LinearLayout) {
                    android.widget.LinearLayout ll = (android.widget.LinearLayout) layout;
                    for (int i = 0; i < ll.getChildCount(); i++) {
                        View child = ll.getChildAt(i);
                        child.setAlpha(0);
                        child.setTranslationY(30);
                        child.animate()
                                .alpha(1)
                                .translationY(0)
                                .setDuration(500)
                                .setStartDelay(100 + (i * 100L))
                                .setInterpolator(new DecelerateInterpolator())
                                .start();
                    }
                }
            }
        }
    }

    private void applyClickEffect(View v) {
        v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(100).start()).start();
    }

    private void firebaseAuthWithGoogle(String idToken) {
        if (mAuth == null) return;
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        checkUserInDatabase(user);
                    } else {
                        Toast.makeText(LoginActivity.this, "Authentication Failed.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void checkUserInDatabase(FirebaseUser firebaseUser) {
        if (firebaseUser == null || mDatabase == null) return;

        mDatabase.child("users").child(firebaseUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    navigateToMain();
                } else {
                    createUserProfile(firebaseUser);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Database error", error.toException());
                navigateToMain();
            }
        });
    }

    private void createUserProfile(FirebaseUser firebaseUser) {
        if (mDatabase == null) return;
        User user = new User(firebaseUser.getUid(), firebaseUser.getDisplayName(), firebaseUser.getEmail());
        if (firebaseUser.getPhotoUrl() != null) {
            user.setAvatarUrl(firebaseUser.getPhotoUrl().toString());
        }

        mDatabase.child("users").child(firebaseUser.getUid()).setValue(user)
                .addOnSuccessListener(aVoid -> {
                    Intent intent = new Intent(LoginActivity.this, OnboardingActivity.class);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to create profile", Toast.LENGTH_SHORT).show();
                    navigateToMain();
                });
    }

    private void navigateToMain() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_up_enter, R.anim.gentle_fade_out);
        finish();
    }
}
