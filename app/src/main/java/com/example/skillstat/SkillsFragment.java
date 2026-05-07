package com.example.skillstat;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import com.example.skillstat.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;
import java.util.Map;

public class SkillsFragment extends Fragment {

    private LinearLayout llSkillsContainer;
    private DatabaseReference mDatabase;
    private String currentUid;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_skills, container, false);

        llSkillsContainer = view.findViewById(R.id.ll_skills_container);
        mDatabase = FirebaseDatabase.getInstance().getReference();
        currentUid = FirebaseAuth.getInstance().getUid();

        // Setup the "Add New Skill" buttons
        View btnAdd = view.findViewById(R.id.btn_add_new_skill);
        if (btnAdd != null) btnAdd.setOnClickListener(v -> navigateToAddSkill());

        View btnAddSmall = view.findViewById(R.id.btn_add_small);
        if (btnAddSmall != null) btnAddSmall.setOnClickListener(v -> navigateToAddSkill());

        loadUserSkills();

        return view;
    }

    private void loadUserSkills() {
        if (currentUid == null) return;

        mDatabase.child("users").child(currentUid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                
                User user = snapshot.getValue(User.class);
                if (user != null) {
                    populateSkillsList(user);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("SkillsFragment", "Load failed", error.toException());
            }
        });
    }

    private void populateSkillsList(User user) {
        llSkillsContainer.removeAllViews();
        List<String> skills = user.getSkills();
        Map<String, Integer> masteryMap = user.getSkillMastery();

        if (skills == null || skills.isEmpty()) {
            showEmptyState();
            return;
        }

        for (String skillName : skills) {
            int progress = masteryMap.getOrDefault(skillName, 0);
            addSkillCard(skillName, progress);
        }
    }

    private void addSkillCard(String name, int progress) {
        View card = getLayoutInflater().inflate(R.layout.item_skill_card, llSkillsContainer, false);

        TextView tvName = card.findViewById(R.id.tv_skill_name);
        TextView tvProgressPercent = card.findViewById(R.id.tv_progress_percent);
        TextView tvStatusText = card.findViewById(R.id.tv_status_text);
        View statusDot = card.findViewById(R.id.status_dot);
        View progressFill = card.findViewById(R.id.progress_fill);
        View progressBg = card.findViewById(R.id.progress_bg);
        View btnEdit = card.findViewById(R.id.btn_edit);
        View btnPractice = card.findViewById(R.id.btn_practice);

        tvName.setText(name);
        
        // Determine status and color
        String status = "Learning";
        String colorHex = "#58CC02"; // Default green
        if (progress >= 100) {
            status = "Mastered";
            colorHex = "#00BCD4";
        } else if (progress > 70) {
            status = "Advanced";
        } else if (progress < 20) {
            status = "At Risk";
            colorHex = "#FF4B4B";
        }

        int themeColor = Color.parseColor(colorHex);
        tvStatusText.setText(status);
        tvStatusText.setTextColor(themeColor);
        statusDot.setBackgroundTintList(ColorStateList.valueOf(themeColor));
        tvProgressPercent.setText(progress + "% • " + status);

        // Update progress bar width
        progressBg.post(() -> {
            int fullWidth = progressBg.getWidth();
            ViewGroup.LayoutParams params = progressFill.getLayoutParams();
            params.width = (int) (fullWidth * (progress / 100f));
            progressFill.setLayoutParams(params);
        });

        btnEdit.setOnClickListener(v -> navigateToEditSkill(name));
        btnPractice.setOnClickListener(v -> navigateToPractice(name));

        llSkillsContainer.addView(card);
    }

    private void showEmptyState() {
        TextView tv = new TextView(getContext());
        tv.setText("You haven't added any skills yet.\nTap the button below to start!");
        tv.setTextColor(0x88FFFFFF);
        tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        tv.setPadding(0, 80, 0, 80);
        llSkillsContainer.addView(tv);
    }

    private void navigateToEditSkill(String skillName) {
        if (getActivity() != null) {
            EditSkillFragment fragment = new EditSkillFragment();
            Bundle args = new Bundle();
            args.putString("skill_name", skillName);
            fragment.setArguments(args);
            
            getActivity().getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(R.anim.step_forward_enter, R.anim.step_forward_exit, R.anim.step_backward_enter, R.anim.step_backward_exit)
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit();
        }
    }

    private void navigateToPractice(String skillName) {
        if (getActivity() != null) {
            PracticeFragment fragment = new PracticeFragment();
            Bundle args = new Bundle();
            args.putString("skill_name", skillName);
            fragment.setArguments(args);

            getActivity().getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(R.anim.step_forward_enter, R.anim.step_forward_exit, R.anim.step_backward_enter, R.anim.step_backward_exit)
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit();
        }
    }

    private void navigateToAddSkill() {
        if (getActivity() != null) {
            getActivity().getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(R.anim.step_forward_enter, R.anim.step_forward_exit, R.anim.step_backward_enter, R.anim.step_backward_exit)
                    .replace(R.id.fragment_container, new AddSkillFragment())
                    .addToBackStack(null)
                    .commit();
        }
    }
}
