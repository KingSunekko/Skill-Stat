package com.example.skillstat;

import android.animation.ValueAnimator;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnticipateOvershootInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

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

public class SettingsGoalFragment extends Fragment {

    private ImageButton btnBack;
    private TextView tvDisplayGoal;
    private EditText etCustomGoal;
    private View btnSave;
    private TextView[] quickSelectButtons;
    private int[] goalValues = {5, 10, 15, 20, 30, 45};
    
    private int selectedGoal = 20;
    private DatabaseReference mDatabase;
    private String currentUid;
    private boolean hasAnimatedEntrance = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings_goal, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mDatabase = FirebaseDatabase.getInstance().getReference();
        currentUid = FirebaseAuth.getInstance().getUid();

        btnBack = view.findViewById(R.id.btn_back);
        tvDisplayGoal = view.findViewById(R.id.tv_display_goal);
        etCustomGoal = view.findViewById(R.id.et_custom_goal);
        btnSave = view.findViewById(R.id.btn_save_goal);

        quickSelectButtons = new TextView[]{
                view.findViewById(R.id.btn_goal_5),
                view.findViewById(R.id.btn_goal_10),
                view.findViewById(R.id.btn_goal_15),
                view.findViewById(R.id.btn_goal_20),
                view.findViewById(R.id.btn_goal_30),
                view.findViewById(R.id.btn_goal_45)
        };

        loadCurrentGoal();
        setupListeners();
        
        animateEntrance(view);
    }

    private void animateEntrance(View view) {
        View header = view.findViewById(R.id.ll_goal_header);
        if (header != null) {
            header.setAlpha(0);
            header.setTranslationY(-20);
            header.animate().alpha(1).translationY(0).setDuration(400).setInterpolator(new DecelerateInterpolator()).start();
        }

        View displayCard = view.findViewById(R.id.cv_goal_display);
        if (displayCard != null) {
            displayCard.setAlpha(0);
            displayCard.setScaleX(0.9f);
            displayCard.animate().alpha(1).scaleX(1.0f).setDuration(500).setStartDelay(100).setInterpolator(new AnticipateOvershootInterpolator()).start();
        }

        View grid = view.findViewById(R.id.gl_goal_presets);
        if (grid instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) grid;
            for (int i = 0; i < vg.getChildCount(); i++) {
                View child = vg.getChildAt(i);
                child.setAlpha(0);
                child.setTranslationY(20);
                child.animate().alpha(1).translationY(0).setDuration(400).setStartDelay(200 + (i * 50L)).start();
            }
        }
        
        btnSave.setAlpha(0);
        btnSave.setTranslationY(20);
        btnSave.animate().alpha(1).translationY(0).setDuration(500).setStartDelay(500).start();
    }

    private void loadCurrentGoal() {
        if (currentUid == null) return;
        mDatabase.child("users").child(currentUid).child("dailyGoalMinutes").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                if (snapshot.exists()) {
                    Integer goal = snapshot.getValue(Integer.class);
                    if (goal != null) {
                        updateSelection(goal);
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        for (int i = 0; i < quickSelectButtons.length; i++) {
            final int value = goalValues[i];
            final View btn = quickSelectButtons[i];
            btn.setOnClickListener(v -> {
                btn.animate().scaleX(1.1f).scaleY(1.1f).setDuration(100).withEndAction(() -> btn.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start()).start();
                etCustomGoal.setText("");
                updateSelection(value);
            });
        }

        etCustomGoal.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    try {
                        int value = Integer.parseInt(s.toString());
                        updateSelection(value);
                    } catch (NumberFormatException e) {}
                }
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnSave.setOnClickListener(v -> {
            v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).withEndAction(() -> {
                v.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
                saveGoal();
            }).start();
        });
    }

    private void updateSelection(int value) {
        if (selectedGoal != value) {
            animateNumber(tvDisplayGoal, value);
        }
        selectedGoal = value;

        // Update UI for buttons
        for (int i = 0; i < quickSelectButtons.length; i++) {
            if (goalValues[i] == value) {
                quickSelectButtons[i].setBackgroundResource(R.drawable.shape_goal_option_selected);
                quickSelectButtons[i].setTextColor(Color.parseColor("#58CC02"));
            } else {
                quickSelectButtons[i].setBackgroundResource(R.drawable.shape_skill_card);
                quickSelectButtons[i].setTextColor(Color.WHITE);
            }
        }
    }

    private void animateNumber(TextView tv, int target) {
        int current = 0;
        try { current = Integer.parseInt(tv.getText().toString()); } catch (Exception ignored) {}
        ValueAnimator animator = ValueAnimator.ofInt(current, target);
        animator.setDuration(400);
        animator.addUpdateListener(animation -> tv.setText(String.valueOf(animation.getAnimatedValue())));
        animator.start();
        
        tv.setScaleX(1.2f); tv.setScaleY(1.2f);
        tv.animate().scaleX(1.0f).scaleY(1.0f).setDuration(300).setInterpolator(new AnticipateOvershootInterpolator()).start();
    }

    private void saveGoal() {
        if (currentUid == null) return;
        
        mDatabase.child("users").child(currentUid).child("dailyGoalMinutes").setValue(selectedGoal)
                .addOnSuccessListener(aVoid -> {
                    if (isAdded()) {
                        Toast.makeText(getContext(), "Goal updated! 🎯", Toast.LENGTH_SHORT).show();
                        getParentFragmentManager().popBackStack();
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        Toast.makeText(getContext(), "Failed to update goal", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
