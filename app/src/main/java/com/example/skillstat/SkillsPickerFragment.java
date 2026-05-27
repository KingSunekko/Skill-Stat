package com.example.skillstat;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnticipateOvershootInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.skillstat.models.User;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SkillsPickerFragment extends Fragment {

    private ChipGroup cgSkills, cgSelectedSummary;
    private LinearLayout llSummary;
    private TextView tvSelectedCount;
    private Button btnContinue;
    private final List<String> selectedSkills = new ArrayList<>();
    private DatabaseReference mDatabase;

    private final List<String> availableSkills = Arrays.asList(
            "Java 💻", "Python 🐍", "Guitar 🎸",
            "Public Speaking 🎤", "Spanish 🇪🇸", "Piano 🎹",
            "Drawing 🎨", "Cooking 🍳", "Math 📐",
            "Design 🎨", "Chess ♟️", "Photography 📷",
            "French 🇫🇷", "Yoga 🧘", "Boxing 🥊"
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_skills_picker, container, false);

        mDatabase = FirebaseDatabase.getInstance().getReference();

        cgSkills = view.findViewById(R.id.cg_skills);
        cgSelectedSummary = view.findViewById(R.id.cg_selected_summary);
        llSummary = view.findViewById(R.id.ll_summary);
        tvSelectedCount = view.findViewById(R.id.tv_selected_count);
        btnContinue = view.findViewById(R.id.btn_continue);

        setupSkillsGrid();
        updateButtonState();

        btnContinue.setOnClickListener(v -> {
            applyClickEffect(v);
            saveSkillsAndContinue();
        });

        animateEntrance(view);

        return view;
    }

    private void animateEntrance(View view) {
        View title = view.findViewById(R.id.tv_picker_title);
        View sub = view.findViewById(R.id.tv_picker_subtitle);
        
        if (title != null) {
            title.setAlpha(0);
            title.setTranslationY(20);
            title.animate().alpha(1).translationY(0).setDuration(500).setInterpolator(new DecelerateInterpolator()).start();
        }
        if (sub != null) {
            sub.setAlpha(0);
            sub.setTranslationY(20);
            sub.animate().alpha(1).translationY(0).setDuration(500).setStartDelay(100).setInterpolator(new DecelerateInterpolator()).start();
        }
        
        cgSkills.setAlpha(0);
        cgSkills.setTranslationY(30);
        cgSkills.animate().alpha(1).translationY(0).setDuration(600).setStartDelay(200).start();
        
        btnContinue.setAlpha(0);
        btnContinue.setTranslationY(20);
        btnContinue.animate().alpha(1).translationY(0).setDuration(500).setStartDelay(400).start();
    }

    private void applyClickEffect(View v) {
        v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(100).start()).start();
    }

    private void saveSkillsAndContinue() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        // Fetch user object to ensure we don't overwrite other data
        mDatabase.child("users").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                if (user != null) {
                    user.setSkills(selectedSkills);
                    
                    Map<String, Long> lastPractice = user.getSkillLastPractice();
                    Map<String, Integer> decaySettings = user.getSkillDecaySettings();
                    Map<String, Double> mastery = user.getSkillMastery();
                    long now = System.currentTimeMillis();

                    for (String skill : selectedSkills) {
                        String skillKey = skill.replace(".", "_");
                        if (!lastPractice.containsKey(skillKey)) {
                            lastPractice.put(skillKey, now); // Initialize with 'now' so it starts at 7d
                        }
                        if (!decaySettings.containsKey(skillKey)) {
                            decaySettings.put(skillKey, 7); // Default 7 days
                        }
                        if (!mastery.containsKey(skillKey)) {
                            mastery.put(skillKey, 0.0);
                        }
                    }

                    mDatabase.child("users").child(uid).setValue(user)
                            .addOnSuccessListener(aVoid -> {
                                if (getActivity() != null) {
                                    getActivity().getSupportFragmentManager().beginTransaction()
                                            .setCustomAnimations(
                                                    R.anim.step_forward_enter,
                                                    R.anim.step_forward_exit,
                                                    R.anim.step_backward_enter,
                                                    R.anim.step_backward_exit
                                            )
                                            .replace(R.id.onboarding_container, new GoalPickerFragment())
                                            .addToBackStack(null)
                                            .commit();
                                }
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(getContext(), "Failed to save skills", Toast.LENGTH_SHORT).show();
                            });
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void setupSkillsGrid() {
        cgSkills.removeAllViews();
        int index = 0;
        for (String skill : availableSkills) {
            TextView chip = create3DSkillChip(skill, false);
            final int staggerIndex = index++;
            chip.setOnClickListener(v -> toggleSkillSelection(skill, chip));
            
            // Staggered entrance for individual chips
            chip.setAlpha(0);
            chip.setScaleX(0.8f);
            chip.setScaleY(0.8f);
            chip.animate().alpha(1).scaleX(1.0f).scaleY(1.0f).setDuration(300).setStartDelay(200 + (staggerIndex * 30L)).start();
            
            cgSkills.addView(chip);
        }
    }

    private TextView create3DSkillChip(String text, boolean isSummary) {
        TextView chip = (TextView) getLayoutInflater().inflate(R.layout.layout_skill_chip, cgSkills, false);
        
        if (isSummary) {
            chip.setText(text);
            chip.setSelected(true);
            chip.setClickable(false);
            chip.setFocusable(false);
        } else {
            boolean isSelected = selectedSkills.contains(text);
            chip.setSelected(isSelected);
            chip.setText(isSelected ? "✓ " + text : text);
        }
        
        return chip;
    }

    private void toggleSkillSelection(String skill, TextView chip) {
        if (selectedSkills.contains(skill)) {
            selectedSkills.remove(skill);
            chip.setSelected(false);
            chip.setText(skill);
            chip.animate().scaleX(1.0f).scaleY(1.0f).setDuration(200).start();
        } else {
            selectedSkills.add(skill);
            chip.setSelected(true);
            chip.setText("✓ " + skill);
            chip.animate().scaleX(1.05f).scaleY(1.05f).setDuration(200).setInterpolator(new AnticipateOvershootInterpolator()).start();
        }
        updateSummary();
        updateButtonState();
    }

    private void updateSummary() {
        if (selectedSkills.isEmpty()) {
            if (llSummary.getVisibility() == View.VISIBLE) {
                llSummary.animate().alpha(0).setDuration(200).withEndAction(() -> llSummary.setVisibility(View.GONE)).start();
            }
        } else {
            if (llSummary.getVisibility() != View.VISIBLE) {
                llSummary.setVisibility(View.VISIBLE);
                llSummary.setAlpha(0);
                llSummary.animate().alpha(1).setDuration(300).start();
            }
            String countText = selectedSkills.size() == 1 ? "1 skill" : selectedSkills.size() + " skills";
            tvSelectedCount.setText("☑ Selected — " + countText);
            cgSelectedSummary.removeAllViews();
            for (String skill : selectedSkills) {
                TextView summaryChip = create3DSkillChip(skill, true);
                summaryChip.setAlpha(0);
                summaryChip.setScaleX(0.5f);
                summaryChip.animate().alpha(1).scaleX(1.0f).setDuration(300).start();
                cgSelectedSummary.addView(summaryChip);
            }
        }
    }

    private void updateButtonState() {
        boolean isEnabled = !selectedSkills.isEmpty();
        btnContinue.setEnabled(isEnabled);
        btnContinue.animate().alpha(isEnabled ? 1.0f : 0.5f).setDuration(200).start();
    }
}
