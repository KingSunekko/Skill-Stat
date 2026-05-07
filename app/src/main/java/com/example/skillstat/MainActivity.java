package com.example.skillstat;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private View navHome, navSkills, navFriends, navRanks, navProfile;
    private View pillHome, pillSkills, pillFriends, pillRanks, pillProfile;
    private TextView labelHome, labelSkills, labelFriends, labelRanks, labelProfile;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Safely initialize FirebaseAuth
        try {
            mAuth = FirebaseAuth.getInstance();
            FirebaseUser currentUser = mAuth.getCurrentUser();

            if (currentUser == null) {
                goToLogin();
                return;
            }
        } catch (Exception e) {
            Log.e(TAG, "Firebase not initialized. Redirecting to Login.", e);
            // If Firebase fails, we can't check user, so go to login where we handle the error
            goToLogin();
            return;
        }

        setContentView(R.layout.activity_main);

        View bottomNav = findViewById(R.id.bottom_nav_container);
        if (bottomNav != null) {
            ViewCompat.setOnApplyWindowInsetsListener(bottomNav, (v, insets) -> {
                Insets navigationBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars());
                v.setPadding(0, 0, 0, navigationBars.bottom);
                return insets;
            });
        }

        // Initialize views
        navHome = findViewById(R.id.navHome);
        navSkills = findViewById(R.id.navSkills);
        navFriends = findViewById(R.id.navFriends);
        navRanks = findViewById(R.id.navRanks);
        navProfile = findViewById(R.id.navProfile);

        pillHome = findViewById(R.id.pillHome);
        pillSkills = findViewById(R.id.pillSkills);
        pillFriends = findViewById(R.id.pillFriends);
        pillRanks = findViewById(R.id.pillRanks);
        pillProfile = findViewById(R.id.pillProfile);

        labelHome = findViewById(R.id.labelHome);
        labelSkills = findViewById(R.id.labelSkills);
        labelFriends = findViewById(R.id.labelFriends);
        labelRanks = findViewById(R.id.labelRanks);
        labelProfile = findViewById(R.id.labelProfile);

        setupNavigation();

        // Check for specific navigation requests (e.g. from Duel screen)
        String navigateTo = getIntent().getStringExtra("navigate_to");
        if ("practice".equals(navigateTo)) {
            String skillName = getIntent().getStringExtra("skill_name");
            PracticeFragment fragment = new PracticeFragment();
            Bundle args = new Bundle();
            args.putString("skill_name", skillName);
            fragment.setArguments(args);
            
            updateNavSelection(navHome);
            loadFragment(fragment);
        } else if (savedInstanceState == null) {
            updateNavSelection(navHome);
            loadFragment(new HomeFragment());
        }
    }

    private void goToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    private void setupNavigation() {
        if (navHome != null) navHome.setOnClickListener(v -> {
            updateNavSelection(navHome);
            loadFragment(new HomeFragment());
        });

        if (navSkills != null) navSkills.setOnClickListener(v -> {
            updateNavSelection(navSkills);
            loadFragment(new SkillsFragment());
        });

        if (navFriends != null) navFriends.setOnClickListener(v -> {
            updateNavSelection(navFriends);
            loadFragment(new FriendsFragment());
        });

        if (navRanks != null) navRanks.setOnClickListener(v -> {
            updateNavSelection(navRanks);
            loadFragment(new RanksFragment());
        });

        if (navProfile != null) navProfile.setOnClickListener(v -> {
            updateNavSelection(navProfile);
            loadFragment(new ProfileFragment());
        });
    }

    private void updateNavSelection(View selectedNav) {
        resetNavItem(pillHome, labelHome);
        resetNavItem(pillSkills, labelSkills);
        resetNavItem(pillFriends, labelFriends);
        resetNavItem(pillRanks, labelRanks);
        resetNavItem(pillProfile, labelProfile);

        if (selectedNav == navHome) setSelectedItem(pillHome, labelHome);
        else if (selectedNav == navSkills) setSelectedItem(pillSkills, labelSkills);
        else if (selectedNav == navFriends) setSelectedItem(pillFriends, labelFriends);
        else if (selectedNav == navRanks) setSelectedItem(pillRanks, labelRanks);
        else if (selectedNav == navProfile) setSelectedItem(pillProfile, labelProfile);
    }

    private void resetNavItem(View pill, TextView label) {
        if (pill != null) pill.setBackground(null);
        if (label != null) label.setTextColor(0xFF8E8E93);
    }

    private void setSelectedItem(View pill, TextView label) {
        if (pill != null) pill.setBackgroundResource(R.drawable.shape_nav_pill_active);
        if (label != null) label.setTextColor(ContextCompat.getColor(this, R.color.splash_green));
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }
}
