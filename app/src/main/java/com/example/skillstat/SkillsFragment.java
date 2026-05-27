package com.example.skillstat;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;
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
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class SkillsFragment extends Fragment {

    private LinearLayout llSkillsContainer;
    private View emptyState;
    private DatabaseReference mDatabase;
    private String currentUid;
    private boolean hasAnimatedEntrance = false;
    private ValueEventListener skillsListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_skills, container, false);

        llSkillsContainer = view.findViewById(R.id.ll_skills_container);
        emptyState = view.findViewById(R.id.ll_empty_state);
        mDatabase = FirebaseDatabase.getInstance().getReference();
        currentUid = FirebaseAuth.getInstance().getUid();

        View btnAdd = view.findViewById(R.id.btn_add_new_skill);
        if (btnAdd != null) btnAdd.setOnClickListener(v -> navigateToAddSkill());

        View btnAddSmall = view.findViewById(R.id.btn_add_small);
        if (btnAddSmall != null) btnAddSmall.setOnClickListener(v -> navigateToAddSkill());

        View btnTreeOverview = view.findViewById(R.id.btn_tree_overview);
        if (btnTreeOverview != null) btnTreeOverview.setOnClickListener(v -> navigateToMasterTree());

        loadUserSkills();

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mDatabase != null && skillsListener != null && currentUid != null) {
            mDatabase.child("users").child(currentUid).removeEventListener(skillsListener);
        }
    }

    private void loadUserSkills() {
        if (currentUid == null) return;

        skillsListener = new ValueEventListener() {
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
        };
        
        mDatabase.child("users").child(currentUid).addValueEventListener(skillsListener);
    }

    private void populateSkillsList(User user) {
        if (llSkillsContainer == null) return;
        llSkillsContainer.removeAllViews();
        
        List<String> skills = user.getSkills();
        Map<String, Double> masteryMap = user.getSkillMastery();
        Map<String, Integer> decayMap = user.getSkillDecaySettings();
        Map<String, Long> lastPracticeMap = user.getSkillLastPractice();

        if (skills == null || skills.isEmpty()) {
            showEmptyState();
            return;
        }

        if (emptyState != null) emptyState.setVisibility(View.GONE);

        int index = 0;
        for (String skillName : skills) {
            String skillKey = skillName.replace(".", "_");
            double progress = masteryMap.getOrDefault(skillKey, 0.0);
            int decayDays = decayMap.getOrDefault(skillKey, 7);
            long lastPractice = lastPracticeMap.getOrDefault(skillKey, System.currentTimeMillis());
            
            addSkillCard(skillName, progress, decayDays, lastPractice, index++);
        }
        hasAnimatedEntrance = true;
    }

    private void addSkillCard(String name, double progress, int decaySetting, long lastPractice, int index) {
        View card = getLayoutInflater().inflate(R.layout.item_skill_card, llSkillsContainer, false);

        TextView tvName = card.findViewById(R.id.tv_skill_name);
        TextView tvProgressPercent = card.findViewById(R.id.tv_progress_percent);
        TextView tvStatusText = card.findViewById(R.id.tv_status_text);
        TextView tvDecay = card.findViewById(R.id.tv_decay);
        View statusDot = card.findViewById(R.id.status_dot);
        View progressFill = card.findViewById(R.id.progress_fill);
        View progressBg = card.findViewById(R.id.progress_bg);
        View btnEdit = card.findViewById(R.id.btn_edit);
        View btnPractice = card.findViewById(R.id.btn_practice);

        tvName.setText(name);
        
        long diffMillis = System.currentTimeMillis() - lastPractice;
        long diffDays = TimeUnit.MILLISECONDS.toDays(diffMillis);
        long daysLeft = Math.max(0, decaySetting - diffDays);
        tvDecay.setText(String.format(Locale.US, "Decays in %dd ⏳", daysLeft));

        String status = "Learning";
        String colorHex = "#58CC02"; 
        
        if (daysLeft <= 1) {
            status = "At Risk";
            colorHex = "#FF4B4B";
        } else if (progress >= 100) {
            status = "Mastered";
            colorHex = "#00BCD4";
        } else if (progress > 70) {
            status = "Advanced";
        }

        int themeColor = Color.parseColor(colorHex);
        tvStatusText.setText(status);
        tvStatusText.setTextColor(themeColor);
        statusDot.setBackgroundTintList(ColorStateList.valueOf(themeColor));
        tvProgressPercent.setText(String.format(Locale.US, "%.0f%% • %s", progress, status));

        progressBg.post(() -> {
            if (!isAdded()) return;
            int fullWidth = progressBg.getWidth();
            if (fullWidth > 0) {
                ViewGroup.LayoutParams params = progressFill.getLayoutParams();
                params.width = (int) (fullWidth * (progress / 100f));
                progressFill.setLayoutParams(params);
                progressFill.setBackgroundTintList(ColorStateList.valueOf(themeColor));
            }
        });

        if (!hasAnimatedEntrance) {
            card.setAlpha(0);
            card.setTranslationY(50);
            card.animate()
                    .alpha(1)
                    .translationY(0)
                    .setDuration(400)
                    .setStartDelay(index * 80L)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        }

        btnEdit.setOnClickListener(v -> navigateToEditSkill(name));
        btnPractice.setOnClickListener(v -> navigateToPractice(name));

        llSkillsContainer.addView(card);
    }

    private void showEmptyState() {
        if (emptyState == null) {
            emptyState = getLayoutInflater().inflate(R.layout.layout_empty_state, llSkillsContainer, false);
            llSkillsContainer.addView(emptyState);
        }
        
        emptyState.setVisibility(View.VISIBLE);
        emptyState.setAlpha(0);
        emptyState.animate().alpha(1).setDuration(500).start();

        TextView tvIcon = emptyState.findViewById(R.id.tv_empty_icon);
        TextView tvTitle = emptyState.findViewById(R.id.tv_empty_title);
        TextView tvMsg = emptyState.findViewById(R.id.tv_empty_message);
        TextView btnAction = emptyState.findViewById(R.id.btn_empty_action);

        if (tvIcon != null) tvIcon.setText("🎯");
        if (tvTitle != null) tvTitle.setText("No Skills Added");
        if (tvMsg != null) tvMsg.setText("Start your journey by adding a skill you want to master!");
        if (btnAction != null) {
            btnAction.setVisibility(View.VISIBLE);
            btnAction.setText("ADD SKILL");
            btnAction.setOnClickListener(v -> navigateToAddSkill());
        }
    }

    private void navigateToMasterTree() {
        if (getActivity() != null) {
            getActivity().getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(R.anim.step_forward_enter, R.anim.step_forward_exit, R.anim.step_backward_enter, R.anim.step_backward_exit)
                    .replace(R.id.fragment_container, new MasterTreeFragment())
                    .addToBackStack(null)
                    .commit();
        }
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
