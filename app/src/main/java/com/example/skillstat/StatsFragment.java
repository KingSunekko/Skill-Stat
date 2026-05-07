package com.example.skillstat;

import android.animation.ValueAnimator;
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
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import com.example.skillstat.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StatsFragment extends Fragment {

    private TextView tvSkillName, tvMainPercentage;
    private TextView tvPeak, tvLow, tvAvg;
    private LinearLayout llBarChart, llDecayList, llTabsContainer;
    private DatabaseReference mDatabase;
    private String currentUid;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_stats, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mDatabase = FirebaseDatabase.getInstance().getReference();
        currentUid = FirebaseAuth.getInstance().getUid();

        tvSkillName = view.findViewById(R.id.tv_stats_skill_name);
        tvMainPercentage = view.findViewById(R.id.tv_stats_percentage);
        tvPeak = view.findViewById(R.id.tv_peak_value);
        tvLow = view.findViewById(R.id.tv_low_value);
        tvAvg = view.findViewById(R.id.tv_avg_value);
        llBarChart = view.findViewById(R.id.ll_bar_chart);
        llDecayList = view.findViewById(R.id.ll_decay_list);
        
        // Find or assume a container for dynamic tabs
        llTabsContainer = view.findViewById(R.id.ll_stats_tabs); 

        loadUserStats();
    }

    private void loadUserStats() {
        if (currentUid == null) return;

        mDatabase.child("users").child(currentUid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                if (user != null) {
                    updateUI(user);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("StatsFragment", "Load failed", error.toException());
            }
        });
    }

    private void updateUI(User user) {
        // 1. Update Decay List with real skills
        if (llDecayList != null) {
            llDecayList.removeAllViews();
            Map<String, Integer> mastery = user.getSkillMastery();
            if (user.getSkills() != null) {
                for (String skill : user.getSkills()) {
                    int progress = mastery.getOrDefault(skill, 0);
                    String status = "Learning";
                    String color = "#58CC02";
                    if (progress >= 90) { status = "Mastered"; color = "#00BCD4"; }
                    else if (progress < 30) { status = "At Risk"; color = "#FF4B4B"; }
                    addDecayItem(skill, progress, status, "7 days", color);
                }
            }
        }

        // 2. Load Chart for the first skill (or selected one)
        if (user.getSkills() != null && !user.getSkills().isEmpty()) {
            String firstSkill = user.getSkills().get(0);
            int currentMastery = user.getSkillMastery().getOrDefault(firstSkill, 0);
            
            // Dummy historical data generation based on real mastery
            int[] data = new int[7];
            for (int i = 0; i < 7; i++) {
                data[i] = Math.max(0, currentMastery - (7 - i) * 2);
            }
            updateChart(firstSkill, data);
        }
    }

    private void updateChart(String name, int[] data) {
        tvSkillName.setText(name);
        int latest = data[data.length - 1];
        tvMainPercentage.setText(latest + "%");

        int peak = 0;
        int low = 100;
        int total = 0;
        String[] days = {"M", "T", "W", "T", "F", "S", "S"};

        for (int i = 0; i < data.length; i++) {
            if (data[i] > peak) peak = data[i];
            if (data[i] < low) low = data[i];
            total += data[i];

            final View barView = llBarChart.getChildAt(i);
            if (barView != null) {
                final View fill = barView.findViewById(R.id.view_bar_fill);
                final TextView val = barView.findViewById(R.id.tv_bar_value);
                final TextView day = barView.findViewById(R.id.tv_bar_day);
                final View spacer = barView.findViewById(R.id.v_bar_spacer);
                
                final float targetWeight = (float) data[i] / 100f;
                day.setText(days[i]);

                ValueAnimator animator = ValueAnimator.ofFloat(0f, targetWeight);
                animator.setDuration(1000);
                animator.setInterpolator(new DecelerateInterpolator());
                animator.addUpdateListener(animation -> {
                    float currentWeight = (float) animation.getAnimatedValue();
                    LinearLayout.LayoutParams fillParams = (LinearLayout.LayoutParams) fill.getLayoutParams();
                    fillParams.weight = currentWeight;
                    fill.setLayoutParams(fillParams);
                    
                    LinearLayout.LayoutParams spacerParams = (LinearLayout.LayoutParams) spacer.getLayoutParams();
                    spacerParams.weight = 1.0f - currentWeight;
                    spacer.setLayoutParams(spacerParams);

                    val.setText((int)(currentWeight * 100) + "%");
                });
                animator.start();
            }
        }

        tvPeak.setText(peak + "%");
        tvLow.setText(low + "%");
        tvAvg.setText((total / data.length) + "%");
    }

    private void addDecayItem(String name, int progress, String status, String time, String colorHex) {
        View item = getLayoutInflater().inflate(R.layout.item_decay_status, llDecayList, false);
        ((TextView) item.findViewById(R.id.tv_decay_skill_name)).setText(name);
        ((TextView) item.findViewById(R.id.tv_decay_status_text)).setText(status);
        int color = Color.parseColor(colorHex);
        ((TextView) item.findViewById(R.id.tv_decay_status_text)).setTextColor(color);
        
        View fill = item.findViewById(R.id.v_decay_progress_fill);
        ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) fill.getLayoutParams();
        params.matchConstraintPercentWidth = (float) progress / 100f;
        fill.setLayoutParams(params);

        llDecayList.addView(item);
        View space = new View(getContext());
        space.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 32));
        llDecayList.addView(space);
    }
}
