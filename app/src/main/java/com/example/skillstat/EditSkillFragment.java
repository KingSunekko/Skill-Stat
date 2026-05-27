package com.example.skillstat;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.skillstat.models.User;
import com.google.android.material.slider.Slider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EditSkillFragment extends Fragment {

    private EditText etSkillName;
    private Slider sliderDecay;
    private TextView tvMasteryLabel, tvDecayLabel;
    private View masteryViewFill;
    private View btnSave, btnCancel, btnRemove, btnBack;

    private View llDecayStatus;
    private TextView tvDecayStatusIcon, tvDecayStatusText;

    private TextView tvGoalValue, tvWeeklyTotal;
    private View btnMinusGoal, btnPlusGoal;
    private TextView[] tvPresets = new TextView[6];
    private int dailyGoalMinutes = 10;

    private String originalSkillName;
    private DatabaseReference mDatabase;
    private String currentUid;
    private double currentMasteryValue = 0.0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_edit_skill, container, false);

        if (getArguments() != null) {
            originalSkillName = getArguments().getString("skill_name");
        }

        mDatabase = FirebaseDatabase.getInstance().getReference();
        currentUid = FirebaseAuth.getInstance().getUid();

        etSkillName = view.findViewById(R.id.et_skill_name_edit);
        sliderDecay = view.findViewById(R.id.slider_decay);
        tvMasteryLabel = view.findViewById(R.id.tv_mastery_label);
        tvDecayLabel = view.findViewById(R.id.tv_decay_label);
        masteryViewFill = view.findViewById(R.id.mastery_view_fill);

        llDecayStatus = view.findViewById(R.id.ll_decay_status);
        tvDecayStatusIcon = view.findViewById(R.id.tv_decay_status_icon);
        tvDecayStatusText = view.findViewById(R.id.tv_decay_status_text);

        btnSave = view.findViewById(R.id.btn_skill_save);
        btnCancel = view.findViewById(R.id.btn_skill_cancel);
        btnRemove = view.findViewById(R.id.btn_remove_skill);
        btnBack = view.findViewById(R.id.btn_back_skill);

        tvGoalValue = view.findViewById(R.id.tv_goal_value);
        tvWeeklyTotal = view.findViewById(R.id.tv_weekly_total);
        btnMinusGoal = view.findViewById(R.id.btn_minus_goal);
        btnPlusGoal = view.findViewById(R.id.btn_plus_goal);

        tvPresets[0] = view.findViewById(R.id.tv_preset_5);
        tvPresets[1] = view.findViewById(R.id.tv_preset_10);
        tvPresets[2] = view.findViewById(R.id.tv_preset_15);
        tvPresets[3] = view.findViewById(R.id.tv_preset_20);
        tvPresets[4] = view.findViewById(R.id.tv_preset_30);
        tvPresets[5] = view.findViewById(R.id.tv_preset_45);

        if (originalSkillName != null) {
            etSkillName.setText(originalSkillName);
            loadSkillData();
        }

        setupListeners();
        animateEntrance(view);

        return view;
    }

    private String getSanitizedKey(String key) {
        return key != null ? key.replace(".", "_") : "";
    }

    private void loadSkillData() {
        if (currentUid == null || originalSkillName == null) return;
        String skillKey = getSanitizedKey(originalSkillName);
        
        mDatabase.child("users").child(currentUid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                User user = snapshot.getValue(User.class);
                if (user != null) {
                    currentMasteryValue = user.getSkillMastery().getOrDefault(skillKey, 0.0);
                    int decay = user.getSkillDecaySettings().getOrDefault(skillKey, 7);
                    dailyGoalMinutes = user.getSkillDailyGoals().getOrDefault(skillKey, 10);

                    sliderDecay.setValue(decay);
                    tvMasteryLabel.setText("MASTERY — " + (int) currentMasteryValue + "%");
                    tvDecayLabel.setText("DECAY TIMER — " + decay + " DAYS");

                    updateMasteryVisual((int) currentMasteryValue);
                    updateDecayStatus(decay);
                    updateGoalUI();
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void setupListeners() {
        sliderDecay.addOnChangeListener((slider, value, fromUser) -> {
            int days = (int) value;
            tvDecayLabel.setText("DECAY TIMER — " + days + " DAYS");
            updateDecayStatus(days);
        });

        btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());
        btnCancel.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        btnSave.setOnClickListener(v -> {
            applyClickEffect(v);
            saveSkillData();
        });

        btnRemove.setOnClickListener(v -> {
            applyClickEffect(v);
            removeSkill();
        });

        btnMinusGoal.setOnClickListener(v -> {
            if (dailyGoalMinutes > 1) {
                dailyGoalMinutes--;
                updateGoalUI();
            }
        });

        btnPlusGoal.setOnClickListener(v -> {
            if (dailyGoalMinutes < 1440) {
                dailyGoalMinutes++;
                updateGoalUI();
            }
        });

        for (TextView preset : tvPresets) {
            preset.setOnClickListener(v -> {
                String text = preset.getText().toString().replace(" min", "");
                dailyGoalMinutes = Integer.parseInt(text);
                updateGoalUI();
            });
        }
    }

    private void updateGoalUI() {
        if (tvGoalValue == null) return;
        tvGoalValue.setText(String.valueOf(dailyGoalMinutes));
        tvWeeklyTotal.setText((dailyGoalMinutes * 7) + " min");

        for (TextView preset : tvPresets) {
            String val = preset.getText().toString().replace(" min", "");
            if (Integer.parseInt(val) == dailyGoalMinutes) {
                preset.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#1A58CC02")));
                preset.setTextColor(Color.parseColor("#58CC02"));
            } else {
                preset.setBackgroundTintList(null);
                preset.setTextColor(Color.WHITE);
            }
        }
    }

    private void saveSkillData() {
        if (currentUid == null || originalSkillName == null) return;

        int newDecay = (int) sliderDecay.getValue();
        String newSkillName = etSkillName.getText().toString().trim();
        if (newSkillName.isEmpty()) return;

        mDatabase.child("users").child(currentUid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                if (user != null) {
                    String oldKey = getSanitizedKey(originalSkillName);
                    String newKey = getSanitizedKey(newSkillName);
                    
                    Map<String, Object> updates = new HashMap<>();

                    if (!newSkillName.equals(originalSkillName)) {
                        List<String> skills = user.getSkills();
                        skills.remove(originalSkillName);
                        skills.add(newSkillName);
                        updates.put("skills", skills);

                        updates.put("skillMastery/" + newKey, user.getSkillMastery().getOrDefault(oldKey, 0.0));
                        updates.put("skillMastery/" + oldKey, null);

                        updates.put("skillDecaySettings/" + newKey, newDecay);
                        updates.put("skillDecaySettings/" + oldKey, null);

                        updates.put("skillDailyGoals/" + newKey, dailyGoalMinutes);
                        updates.put("skillDailyGoals/" + oldKey, null);

                        updates.put("skillLastPractice/" + newKey, user.getSkillLastPractice().getOrDefault(oldKey, System.currentTimeMillis()));
                        updates.put("skillLastPractice/" + oldKey, null);
                    } else {
                        updates.put("skillDecaySettings/" + oldKey, newDecay);
                        updates.put("skillDailyGoals/" + oldKey, dailyGoalMinutes);
                    }

                    mDatabase.child("users").child(currentUid).updateChildren(updates)
                            .addOnSuccessListener(aVoid -> {
                                if (isAdded()) {
                                    Toast.makeText(getContext(), "Skill updated!", Toast.LENGTH_SHORT).show();
                                    getParentFragmentManager().popBackStack();
                                }
                            });
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void removeSkill() {
        if (currentUid == null || originalSkillName == null) return;
        mDatabase.child("users").child(currentUid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                if (user != null) {
                    String key = getSanitizedKey(originalSkillName);
                    Map<String, Object> updates = new HashMap<>();
                    
                    List<String> skills = user.getSkills();
                    skills.remove(originalSkillName);
                    updates.put("skills", skills);
                    updates.put("skillMastery/" + key, null);
                    updates.put("skillDecaySettings/" + key, null);
                    updates.put("skillLastPractice/" + key, null);
                    updates.put("skillDailyGoals/" + key, null);

                    mDatabase.child("users").child(currentUid).updateChildren(updates)
                            .addOnSuccessListener(aVoid -> {
                                if (isAdded()) {
                                    Toast.makeText(getContext(), "Skill removed", Toast.LENGTH_SHORT).show();
                                    getParentFragmentManager().popBackStack();
                                }
                            });
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void updateMasteryVisual(int progress) {
        if (masteryViewFill != null) {
            float percent = progress / 100f;
            ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) masteryViewFill.getLayoutParams();
            params.matchConstraintPercentWidth = Math.max(0.01f, percent);
            masteryViewFill.setLayoutParams(params);
        }
    }

    private void updateDecayStatus(int days) {
        if (getContext() == null) return;
        int color; String statusText; String icon; int bgTint;

        if (days <= 1) { color = Color.parseColor("#FF4B4B"); statusText = "At Risk"; icon = "🛑"; bgTint = Color.parseColor("#26FF4B4B"); }
        else if (days <= 4) { color = Color.parseColor("#FF9600"); statusText = "Fading"; icon = "🟠"; bgTint = Color.parseColor("#26FF9600"); }
        else if (days <= 9) { color = ContextCompat.getColor(getContext(), R.color.splash_green); statusText = "Sharp"; icon = "🟢"; bgTint = Color.parseColor("#2658CC02"); }
        else { color = Color.parseColor("#00BCD4"); statusText = "Mastered"; icon = "💎"; bgTint = Color.parseColor("#2600BCD4"); }

        if (tvDecayStatusIcon != null) tvDecayStatusIcon.setText(icon);
        if (tvDecayStatusText != null) { tvDecayStatusText.setText(statusText); tvDecayStatusText.setTextColor(color); }
        if (llDecayStatus != null) llDecayStatus.setBackgroundTintList(ColorStateList.valueOf(bgTint));
        sliderDecay.setThumbTintList(ColorStateList.valueOf(color));
        sliderDecay.setTrackActiveTintList(ColorStateList.valueOf(color));
    }

    private void applyClickEffect(View v) {
        v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(100).start()).start();
    }

    private void animateEntrance(View view) {
        View header = view.findViewById(R.id.ll_edit_skill_header);
        if (header != null) {
            header.setAlpha(0); header.setTranslationY(-20);
            header.animate().alpha(1).translationY(0).setDuration(400).setInterpolator(new DecelerateInterpolator()).start();
        }
    }
}
