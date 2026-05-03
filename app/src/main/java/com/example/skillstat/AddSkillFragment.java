package com.example.skillstat;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.slider.Slider;

public class AddSkillFragment extends Fragment {

    private EditText etSkillName;
    private ChipGroup cgPopularSkills;
    private View llBeginner, llIntermediate, llAdvanced;
    private View ivBeginnerCheck, ivIntermediateCheck, ivAdvancedCheck;
    private AppCompatButton btnAddSkill;

    private TextView tvDecayTimerTitle, tvDecayStatusIcon, tvDecayStatusText;
    private Slider sliderDecay;
    private View llDecayStatus;

    private String selectedLevel = "Beginner";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_skill, container, false);

        initViews(view);
        setupListeners();
        
        // Default selections
        selectLevel("Beginner");
        updateDecayStatus(1);

        return view;
    }

    private void initViews(View view) {
        etSkillName = view.findViewById(R.id.et_skill_name);
        cgPopularSkills = view.findViewById(R.id.cg_popular_skills);
        llBeginner = view.findViewById(R.id.ll_beginner);
        llIntermediate = view.findViewById(R.id.ll_intermediate);
        llAdvanced = view.findViewById(R.id.ll_advanced);
        ivBeginnerCheck = view.findViewById(R.id.iv_beginner_check);
        ivIntermediateCheck = view.findViewById(R.id.iv_intermediate_check);
        ivAdvancedCheck = view.findViewById(R.id.iv_advanced_check);
        btnAddSkill = view.findViewById(R.id.btn_add_skill);

        tvDecayTimerTitle = view.findViewById(R.id.tv_decay_timer_title);
        sliderDecay = view.findViewById(R.id.slider_decay);
        llDecayStatus = view.findViewById(R.id.ll_decay_status);
        tvDecayStatusIcon = view.findViewById(R.id.tv_decay_status_icon);
        tvDecayStatusText = view.findViewById(R.id.tv_decay_status_text);

        view.findViewById(R.id.btn_back).setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });

        view.findViewById(R.id.btn_cancel).setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });
    }

    private void setupListeners() {
        cgPopularSkills.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!checkedIds.isEmpty()) {
                Chip chip = group.findViewById(checkedIds.get(0));
                if (chip != null) {
                    etSkillName.setText(chip.getText().toString());
                }
            }
        });

        etSkillName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                boolean hasText = s.toString().trim().length() > 0;
                btnAddSkill.setEnabled(hasText);
                btnAddSkill.setAlpha(hasText ? 1.0f : 0.5f);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        sliderDecay.addOnChangeListener((slider, value, fromUser) -> {
            int days = (int) value;
            tvDecayTimerTitle.setText("DECAY TIMER — " + days + " DAYS");
            updateDecayStatus(days);
        });

        llBeginner.setOnClickListener(v -> selectLevel("Beginner"));
        llIntermediate.setOnClickListener(v -> selectLevel("Intermediate"));
        llAdvanced.setOnClickListener(v -> selectLevel("Advanced"));
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

        tvDecayStatusIcon.setText(icon);
        tvDecayStatusText.setText(statusText);
        tvDecayStatusText.setTextColor(color);
        llDecayStatus.setBackgroundTintList(ColorStateList.valueOf(bgTint));
        
        sliderDecay.setThumbTintList(ColorStateList.valueOf(color));
        sliderDecay.setTrackActiveTintList(ColorStateList.valueOf(color));
    }

    private void selectLevel(String level) {
        selectedLevel = level;

        // Reset all
        llBeginner.setBackgroundTintList(null);
        llIntermediate.setBackgroundTintList(null);
        llAdvanced.setBackgroundTintList(null);
        ivBeginnerCheck.setVisibility(View.GONE);
        ivIntermediateCheck.setVisibility(View.GONE);
        ivAdvancedCheck.setVisibility(View.GONE);

        int selectedColor = Color.parseColor("#1A58CC02"); // 10% Alpha green

        switch (level) {
            case "Beginner":
                llBeginner.setBackgroundTintList(ColorStateList.valueOf(selectedColor));
                ivBeginnerCheck.setVisibility(View.VISIBLE);
                break;
            case "Intermediate":
                llIntermediate.setBackgroundTintList(ColorStateList.valueOf(selectedColor));
                ivIntermediateCheck.setVisibility(View.VISIBLE);
                break;
            case "Advanced":
                llAdvanced.setBackgroundTintList(ColorStateList.valueOf(selectedColor));
                ivAdvancedCheck.setVisibility(View.VISIBLE);
                break;
        }
    }
}
