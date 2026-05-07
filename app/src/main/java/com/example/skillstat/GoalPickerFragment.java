package com.example.skillstat;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class GoalPickerFragment extends Fragment {

    private TextView tvGoalValue;
    private LinearLayout llOption10, llOption20, llOption30, llOption45;
    private Button btnContinue;
    private int selectedGoal = 10;
    private DatabaseReference mDatabase;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_goal_picker, container, false);

        mDatabase = FirebaseDatabase.getInstance().getReference();

        tvGoalValue = view.findViewById(R.id.tv_goal_value);
        llOption10 = view.findViewById(R.id.ll_option_10);
        llOption20 = view.findViewById(R.id.ll_option_20);
        llOption30 = view.findViewById(R.id.ll_option_30);
        llOption45 = view.findViewById(R.id.ll_option_45);
        btnContinue = view.findViewById(R.id.btn_continue);

        setupGoalOptions();

        btnContinue.setOnClickListener(v -> saveGoalAndFinish());

        return view;
    }

    private void saveGoalAndFinish() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        mDatabase.child("users").child(uid).child("dailyGoalMinutes").setValue(selectedGoal)
                .addOnSuccessListener(aVoid -> {
                    Intent intent = new Intent(getActivity(), MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    if (getActivity() != null) {
                        getActivity().finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to save goal", Toast.LENGTH_SHORT).show();
                });
    }

    private void setupGoalOptions() {
        llOption10.setOnClickListener(v -> selectGoal(10, llOption10));
        llOption20.setOnClickListener(v -> selectGoal(20, llOption20));
        llOption30.setOnClickListener(v -> selectGoal(30, llOption30));
        llOption45.setOnClickListener(v -> selectGoal(45, llOption45));
    }

    private void selectGoal(int minutes, LinearLayout selectedView) {
        selectedGoal = minutes;
        tvGoalValue.setText(String.valueOf(minutes));

        resetOption(llOption10);
        resetOption(llOption20);
        resetOption(llOption30);
        resetOption(llOption45);

        selectedView.setBackgroundResource(R.drawable.shape_goal_option_selected);
        updateChildColors(selectedView, true);
    }

    private void resetOption(LinearLayout option) {
        option.setBackgroundResource(R.drawable.shape_goal_option_unselected);
        updateChildColors(option, false);
    }

    private void updateChildColors(LinearLayout layout, boolean isSelected) {
        for (int i = 0; i < layout.getChildCount(); i++) {
            View child = layout.getChildAt(i);
            if (child instanceof TextView) {
                if (isSelected) {
                    ((TextView) child).setTextColor(Color.parseColor("#58CC02"));
                } else {
                    if (i == 0) {
                        ((TextView) child).setTextColor(Color.WHITE);
                    } else {
                        ((TextView) child).setTextColor(Color.parseColor("#AAAACC"));
                    }
                }
            }
        }
    }
}
