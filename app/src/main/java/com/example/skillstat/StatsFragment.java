package com.example.skillstat;

import android.animation.ValueAnimator;
import android.graphics.Color;
import android.os.Bundle;
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
import java.util.ArrayList;
import java.util.List;

public class StatsFragment extends Fragment {

    private TextView tvSkillName, tvMainPercentage;
    private TextView tvPeak, tvLow, tvAvg;
    private LinearLayout llBarChart;
    private LinearLayout llDecayList;
    private List<TextView> tabs = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_stats, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvSkillName = view.findViewById(R.id.tv_stats_skill_name);
        tvMainPercentage = view.findViewById(R.id.tv_stats_percentage);
        tvPeak = view.findViewById(R.id.tv_peak_value);
        tvLow = view.findViewById(R.id.tv_low_value);
        tvAvg = view.findViewById(R.id.tv_avg_value);
        llBarChart = view.findViewById(R.id.ll_bar_chart);
        llDecayList = view.findViewById(R.id.ll_decay_list);

        setupTabs(view);
        setupDecayList();
        
        // Default select Java
        selectTab(view.findViewById(R.id.tab_java), new int[]{55, 62, 70, 68, 74, 78, 82});
    }

    private void setupTabs(View view) {
        TextView tabJava = view.findViewById(R.id.tab_java);
        TextView tabGuitar = view.findViewById(R.id.tab_guitar);
        TextView tabPublicSpeaking = view.findViewById(R.id.tab_public_speaking);

        tabs.add(tabJava);
        tabs.add(tabGuitar);
        tabs.add(tabPublicSpeaking);

        tabJava.setOnClickListener(v -> selectTab(tabJava, new int[]{55, 62, 70, 68, 74, 78, 82}));
        tabGuitar.setOnClickListener(v -> selectTab(tabGuitar, new int[]{30, 45, 42, 50, 60, 58, 65}));
        tabPublicSpeaking.setOnClickListener(v -> selectTab(tabPublicSpeaking, new int[]{20, 25, 35, 40, 38, 45, 52}));
    }

    private void selectTab(TextView selectedTab, int[] data) {
        for (TextView tab : tabs) {
            tab.setSelected(tab == selectedTab);
        }
        updateChart(selectedTab.getText().toString(), data);
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
                final int finalValue = data[i];
                final String dayText = days[i];

                // Reset state for animation
                val.setAlpha(0f);
                day.setText(dayText);

                ValueAnimator animator = ValueAnimator.ofFloat(0f, targetWeight);
                animator.setDuration(1000);
                animator.setStartDelay(i * 100); // Staggered "grow" effect
                animator.setInterpolator(new DecelerateInterpolator());
                animator.addUpdateListener(animation -> {
                    float currentWeight = (float) animation.getAnimatedValue();
                    
                    LinearLayout.LayoutParams fillParams = (LinearLayout.LayoutParams) fill.getLayoutParams();
                    fillParams.weight = currentWeight;
                    fill.setLayoutParams(fillParams);
                    
                    LinearLayout.LayoutParams spacerParams = (LinearLayout.LayoutParams) spacer.getLayoutParams();
                    spacerParams.weight = 1.0f - currentWeight;
                    spacer.setLayoutParams(spacerParams);

                    int currentPercent = (int) (currentWeight * 100);
                    val.setText(currentPercent + "%");
                    
                    float alpha = targetWeight > 0 ? currentWeight / targetWeight : 1.0f;
                    val.setAlpha(alpha);
                });
                animator.start();
            }
        }

        tvPeak.setText(peak + "%");
        tvLow.setText(low + "%");
        tvAvg.setText((total / data.length) + "%");
    }

    private void setupDecayList() {
        if (llDecayList == null) return;
        llDecayList.removeAllViews();
        
        addDecayItem("Java 💻", 82, "Sharp", "7 days", "#58CC02");
        addDecayItem("Guitar 🎸", 65, "Sharp", "5 days", "#58CC02");
        addDecayItem("Public Speaking 🎤", 52, "Mastered", "12 days", "#00BCD4");
    }

    private void addDecayItem(String name, int progress, String status, String time, String colorHex) {
        View item = getLayoutInflater().inflate(R.layout.item_decay_status, llDecayList, false);
        
        TextView tvName = item.findViewById(R.id.tv_decay_skill_name);
        TextView tvStatus = item.findViewById(R.id.tv_decay_status_text);
        TextView tvStatusIcon = item.findViewById(R.id.tv_decay_status_icon);
        TextView tvTime = item.findViewById(R.id.tv_next_check);
        View fill = item.findViewById(R.id.v_decay_progress_fill);

        tvName.setText(name);
        tvStatus.setText(status);
        int color = Color.parseColor(colorHex);
        tvStatus.setTextColor(color);
        if (tvStatusIcon != null) {
            tvStatusIcon.setTextColor(color);
        }
        tvTime.setText("Next check in " + time);
        
        // Progress bar width using ConstraintLayout.LayoutParams
        ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) fill.getLayoutParams();
        params.matchConstraintPercentWidth = (float) progress / 100f;
        fill.setLayoutParams(params);

        llDecayList.addView(item);
        
        View space = new View(getContext());
        space.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 32));
        llDecayList.addView(space);
    }
}
