package com.example.skillstat;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

        btnContinue.setOnClickListener(v -> saveSkillsAndContinue());

        return view;
    }

    private void saveSkillsAndContinue() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        mDatabase.child("users").child(uid).child("skills").setValue(selectedSkills)
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

    private void setupSkillsGrid() {
        cgSkills.removeAllViews();
        for (String skill : availableSkills) {
            TextView chip = create3DSkillChip(skill, false);
            chip.setOnClickListener(v -> toggleSkillSelection(skill, chip));
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
        } else {
            selectedSkills.add(skill);
            chip.setSelected(true);
            chip.setText("✓ " + skill);
        }
        updateSummary();
        updateButtonState();
    }

    private void updateSummary() {
        if (selectedSkills.isEmpty()) {
            llSummary.setVisibility(View.GONE);
        } else {
            llSummary.setVisibility(View.VISIBLE);
            String countText = selectedSkills.size() == 1 ? "1 skill" : selectedSkills.size() + " skills";
            tvSelectedCount.setText("☑ Selected — " + countText);
            cgSelectedSummary.removeAllViews();
            for (String skill : selectedSkills) {
                TextView summaryChip = create3DSkillChip(skill, true);
                cgSelectedSummary.addView(summaryChip);
            }
        }
    }

    private void updateButtonState() {
        boolean isEnabled = !selectedSkills.isEmpty();
        btnContinue.setEnabled(isEnabled);
        btnContinue.setAlpha(isEnabled ? 1.0f : 0.5f);
    }
}
