package com.example.skillstat;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.slider.Slider;

public class EditSkillFragment extends Fragment {

    private EditText etSkillName;
    private SeekBar sbMastery;
    private Slider sliderDecay;
    private TextView tvMasteryLabel, tvDecayLabel;
    private View masteryViewFill;
    private View optBeginner, optIntermediate, optAdvanced;
    private View btnSave, btnCancel, btnRemove, btnBack;

    private View llDecayStatus;
    private TextView tvDecayStatusIcon, tvDecayStatusText;

    private String currentLevel = "Advanced"; // Default state

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_edit_skill, container, false);

        // Initialize views
        etSkillName = view.findViewById(R.id.et_skill_name_edit);
        sbMastery = view.findViewById(R.id.sb_mastery);
        sliderDecay = view.findViewById(R.id.slider_decay);
        tvMasteryLabel = view.findViewById(R.id.tv_mastery_label);
        tvDecayLabel = view.findViewById(R.id.tv_decay_label);
        masteryViewFill = view.findViewById(R.id.mastery_view_fill);
        
        llDecayStatus = view.findViewById(R.id.ll_decay_status);
        tvDecayStatusIcon = view.findViewById(R.id.tv_decay_status_icon);
        tvDecayStatusText = view.findViewById(R.id.tv_decay_status_text);

        optBeginner = view.findViewById(R.id.opt_beginner);
        optIntermediate = view.findViewById(R.id.opt_intermediate);
        optAdvanced = view.findViewById(R.id.opt_advanced);
        
        btnSave = view.findViewById(R.id.btn_skill_save);
        btnCancel = view.findViewById(R.id.btn_skill_cancel);
        btnRemove = view.findViewById(R.id.btn_remove_skill);
        btnBack = view.findViewById(R.id.btn_back_skill);

        setupListeners();
        
        // Initial UI sync
        updateLevelUI();
        updateMasteryVisual(sbMastery.getProgress());
        updateDecayStatus((int) sliderDecay.getValue());

        return view;
    }

    private void setupListeners() {
        // Mastery SeekBar Logic
        sbMastery.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvMasteryLabel.setText("MASTERY — " + progress + "%");
                updateMasteryVisual(progress);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Decay Slider Logic
        sliderDecay.addOnChangeListener((slider, value, fromUser) -> {
            int days = (int) value;
            tvDecayLabel.setText("DECAY TIMER — " + days + " DAYS");
            updateDecayStatus(days);
        });

        // Level Selection Logic
        optBeginner.setOnClickListener(v -> { currentLevel = "Beginner"; updateLevelUI(); });
        optIntermediate.setOnClickListener(v -> { currentLevel = "Intermediate"; updateLevelUI(); });
        optAdvanced.setOnClickListener(v -> { currentLevel = "Advanced"; updateLevelUI(); });

        // Navigation Actions
        btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());
        btnCancel.setOnClickListener(v -> getParentFragmentManager().popBackStack());
        btnSave.setOnClickListener(v -> getParentFragmentManager().popBackStack());
        btnRemove.setOnClickListener(v -> getParentFragmentManager().popBackStack());
    }

    private void updateMasteryVisual(int progress) {
        if (masteryViewFill != null) {
            float percent = progress / 100f;
            ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) masteryViewFill.getLayoutParams();
            params.matchConstraintPercentWidth = percent;
            masteryViewFill.setLayoutParams(params);
        }
    }

    private void updateDecayStatus(int days) {
        int color;
        String statusText;
        String icon;
        int bgTint;

        if (days <= 1) {
            color = Color.parseColor("#FF4B4B");
            statusText = "At Risk";
            icon = "🛑";
            bgTint = Color.parseColor("#26FF4B4B");
        } else if (days <= 4) {
            color = Color.parseColor("#FF9600");
            statusText = "Fading";
            icon = "🟠";
            bgTint = Color.parseColor("#26FF9600");
        } else if (days <= 9) {
            color = ContextCompat.getColor(getContext(), R.color.splash_green);
            statusText = "Sharp";
            icon = "🟢";
            bgTint = Color.parseColor("#2658CC02");
        } else {
            color = Color.parseColor("#00BCD4");
            statusText = "Mastered";
            icon = "💎";
            bgTint = Color.parseColor("#2600BCD4");
        }

        if (tvDecayStatusIcon != null) tvDecayStatusIcon.setText(icon);
        if (tvDecayStatusText != null) {
            tvDecayStatusText.setText(statusText);
            tvDecayStatusText.setTextColor(color);
        }
        if (llDecayStatus != null) {
            llDecayStatus.setBackgroundTintList(ColorStateList.valueOf(bgTint));
        }
        
        sliderDecay.setThumbTintList(ColorStateList.valueOf(color));
        sliderDecay.setTrackActiveTintList(ColorStateList.valueOf(color));
    }

    private void updateLevelUI() {
        // Reset all buttons to unselected state
        resetOption(optBeginner, R.id.check_beginner);
        resetOption(optIntermediate, R.id.check_intermediate);
        resetOption(optAdvanced, R.id.check_advanced);

        // Apply selected state
        switch (currentLevel) {
            case "Beginner": setOptionSelected(optBeginner, R.id.check_beginner); break;
            case "Intermediate": setOptionSelected(optIntermediate, R.id.check_intermediate); break;
            case "Advanced": setOptionSelected(optAdvanced, R.id.check_advanced); break;
        }
    }

    private void resetOption(View option, int checkId) {
        option.setBackgroundResource(R.drawable.shape_profile_card);
        View check = option.findViewById(checkId);
        if (check != null) check.setVisibility(View.GONE);
    }

    private void setOptionSelected(View option, int checkId) {
        option.setBackgroundResource(R.drawable.shape_achievement_badge);
        View check = option.findViewById(checkId);
        if (check != null) check.setVisibility(View.VISIBLE);
    }
}
