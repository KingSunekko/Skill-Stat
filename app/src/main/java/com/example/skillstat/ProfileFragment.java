package com.example.skillstat;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.skillstat.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

public class ProfileFragment extends Fragment {

    private TextView tvUsername, tvEmail, tvAvatar, tvPoints, tvStreak, tvDailyGoal, tvSkillsCount, tvSkillsTracked;
    private DatabaseReference mDatabase;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mDatabase = FirebaseDatabase.getInstance().getReference();

        tvUsername = view.findViewById(R.id.tv_profile_name);
        tvEmail = view.findViewById(R.id.tv_profile_handle);
        tvAvatar = view.findViewById(R.id.tv_profile_avatar_emoji);
        tvPoints = view.findViewById(R.id.tv_total_points);
        tvStreak = view.findViewById(R.id.tv_streak_days);
        tvDailyGoal = view.findViewById(R.id.tv_daily_goal_info);
        tvSkillsCount = view.findViewById(R.id.tv_skills_count);
        tvSkillsTracked = view.findViewById(R.id.tv_skills_tracked_info);

        loadUserData();

        // Edit Profile
        view.findViewById(R.id.btn_edit_profile).setOnClickListener(v -> {
            getParentFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new EditProfileFragment())
                    .addToBackStack(null)
                    .commit();
        });

        // Daily Goal
        view.findViewById(R.id.btn_daily_goal).setOnClickListener(v -> {
            getParentFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new GoalPickerFragment())
                    .addToBackStack(null)
                    .commit();
        });

        // Practice History
        view.findViewById(R.id.btn_history).setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), PracticeHistoryActivity.class));
        });

        // Achievements / Badges
        view.findViewById(R.id.btn_badges).setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), AchievementsActivity.class));
        });

        // Categories
        view.findViewById(R.id.btn_categories).setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), CategoriesActivity.class));
        });

        // Logout
        view.findViewById(R.id.btn_logout_text).setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }

    private void loadUserData() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        mDatabase.child("users").child(uid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    User user = snapshot.getValue(User.class);
                    if (user != null) {
                        if (tvUsername != null) tvUsername.setText(user.getUsername());
                        if (tvEmail != null) tvEmail.setText(user.getEmail());
                        if (tvAvatar != null && user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
                            tvAvatar.setText(user.getAvatarUrl());
                        }
                        if (tvPoints != null) tvPoints.setText("⭐ " + user.getTotalPoints() + " XP");
                        if (tvStreak != null) tvStreak.setText("🔥 " + user.getStreak() + " streak");
                        if (tvDailyGoal != null) tvDailyGoal.setText("⏱️ " + user.getDailyGoalMinutes() + " min");
                        
                        List<String> skills = user.getSkills();
                        int count = (skills != null) ? skills.size() : 0;
                        if (tvSkillsCount != null) tvSkillsCount.setText("🧠 " + count + " skills");
                        if (tvSkillsTracked != null) tvSkillsTracked.setText(count + " 📚");
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("ProfileFragment", "Error loading user", error.toException());
            }
        });
    }
}
