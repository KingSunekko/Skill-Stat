package com.example.skillstat;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

public class MainActivity extends AppCompatActivity {

    private View navHome, navSkills, navStats, navRanks, navProfile;
    private View pillHome, pillSkills, pillStats, pillRanks, pillProfile;
    private TextView labelHome, labelSkills, labelStats, labelRanks, labelProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Apply window insets to bottom navigation to avoid system bar overlap
        View bottomNav = findViewById(R.id.bottom_nav_container);
        ViewCompat.setOnApplyWindowInsetsListener(bottomNav, (v, insets) -> {
            Insets navigationBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars());
            v.setPadding(0, 0, 0, navigationBars.bottom);
            return insets;
        });

        // Initialize containers
        navHome = findViewById(R.id.navHome);
        navSkills = findViewById(R.id.navSkills);
        navStats = findViewById(R.id.navStats);
        navRanks = findViewById(R.id.navRanks);
        navProfile = findViewById(R.id.navProfile);

        // Initialize pills
        pillHome = findViewById(R.id.pillHome);
        pillSkills = findViewById(R.id.pillSkills);
        pillStats = findViewById(R.id.pillStats);
        pillRanks = findViewById(R.id.pillRanks);
        pillProfile = findViewById(R.id.pillProfile);

        // Initialize labels
        labelHome = findViewById(R.id.labelHome);
        labelSkills = findViewById(R.id.labelSkills);
        labelStats = findViewById(R.id.labelStats);
        labelRanks = findViewById(R.id.labelRanks);
        labelProfile = findViewById(R.id.labelProfile);

        setupNavigation();

        // Load default fragment
        if (savedInstanceState == null) {
            updateNavSelection(navHome);
            loadFragment(new HomeFragment());
        }
    }

    private void setupNavigation() {
        navHome.setOnClickListener(v -> {
            updateNavSelection(navHome);
            loadFragment(new HomeFragment());
        });

        navSkills.setOnClickListener(v -> {
            updateNavSelection(navSkills);
            loadFragment(new SkillsFragment());
        });

        navStats.setOnClickListener(v -> {
            updateNavSelection(navStats);
            loadFragment(new StatsFragment());
        });

        navRanks.setOnClickListener(v -> {
            updateNavSelection(navRanks);
            loadFragment(new RanksFragment());
        });

        navProfile.setOnClickListener(v -> {
            updateNavSelection(navProfile);
            loadFragment(new ProfileFragment());
        });
    }

    private void updateNavSelection(View selectedNav) {
        // Reset all
        resetNavItem(pillHome, labelHome);
        resetNavItem(pillSkills, labelSkills);
        resetNavItem(pillStats, labelStats);
        resetNavItem(pillRanks, labelRanks);
        resetNavItem(pillProfile, labelProfile);

        // Highlight selected
        if (selectedNav == navHome) setSelectedItem(pillHome, labelHome);
        else if (selectedNav == navSkills) setSelectedItem(pillSkills, labelSkills);
        else if (selectedNav == navStats) setSelectedItem(pillStats, labelStats);
        else if (selectedNav == navRanks) setSelectedItem(pillRanks, labelRanks);
        else if (selectedNav == navProfile) setSelectedItem(pillProfile, labelProfile);
    }

    private void resetNavItem(View pill, TextView label) {
        pill.setBackground(null);
        label.setTextColor(0xFF8E8E93);
    }

    private void setSelectedItem(View pill, TextView label) {
        pill.setBackgroundResource(R.drawable.shape_nav_pill_active);
        label.setTextColor(ContextCompat.getColor(this, R.color.splash_green));
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }
}