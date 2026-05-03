package com.example.skillstat;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class HomeFragment extends Fragment {

    private LinearLayout llSkillsList;
    private View btnStartPractice, btnCharts, btnRanks, btnNotifications;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        llSkillsList = view.findViewById(R.id.ll_home_skills_list);
        btnStartPractice = view.findViewById(R.id.btn_start_practice);
        btnCharts = view.findViewById(R.id.btn_home_charts);
        btnRanks = view.findViewById(R.id.btn_home_ranks);
        btnNotifications = view.findViewById(R.id.btn_notifications);

        setupClickAnimations();
        populateSkills();

        btnNotifications.setOnClickListener(v -> {
            NotificationsBottomSheet bottomSheet = new NotificationsBottomSheet();
            bottomSheet.show(getChildFragmentManager(), "NotificationsBottomSheet");
        });

        btnCharts.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new StatsFragment())
                    .addToBackStack(null)
                    .commit();
        });

        btnRanks.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new RanksFragment())
                    .addToBackStack(null)
                    .commit();
        });
    }

    private void populateSkills() {
        if (llSkillsList == null) return;
        llSkillsList.removeAllViews();

        addSkillItem("Java 💻", 82, "At Risk", "#FF4B4B", "1d left");
        addSkillItem("Guitar 🎸", 65, "Sharp", "#58CC02", "5d left");
        addSkillItem("Public Speaking 🎤", 91, "Mastered", "#00BCD4", "12d left");
    }

    private void addSkillItem(String name, int progress, String status, String statusColor, String timeLeft) {
        View item = getLayoutInflater().inflate(R.layout.item_home_skill, llSkillsList, false);

        TextView tvName = item.findViewById(R.id.tv_skill_name);
        TextView tvStatus = item.findViewById(R.id.tv_status_text);
        View vStatusDot = item.findViewById(R.id.v_status_dot);
        View vFill = item.findViewById(R.id.v_progress_fill);
        TextView tvStats = item.findViewById(R.id.tv_skill_stats);
        TextView tvTime = item.findViewById(R.id.tv_time_left);

        tvName.setText(name);
        tvStatus.setText(status);
        tvStatus.setTextColor(android.graphics.Color.parseColor(statusColor));
        vStatusDot.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor(statusColor)));
        
        tvStats.setText(progress + "% • " + (progress > 85 ? "Advanced" : "Intermediate"));
        tvTime.setText(timeLeft);

        // Calculate width for progress fill
        item.post(() -> {
            View bg = item.findViewById(R.id.v_progress_bg);
            if (bg != null) {
                int fullWidth = bg.getWidth();
                ViewGroup.LayoutParams params = vFill.getLayoutParams();
                params.width = (int) (fullWidth * (progress / 100f));
                vFill.setLayoutParams(params);
            }
        });

        applyHoverEffect(item);
        llSkillsList.addView(item);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupClickAnimations() {
        applyHoverEffect(btnStartPractice);
        applyHoverEffect(btnCharts);
        applyHoverEffect(btnRanks);
        applyHoverEffect(btnNotifications);
        
        View riskCard = getView().findViewById(R.id.card_risk_alert);
        if (riskCard != null) applyHoverEffect(riskCard);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void applyHoverEffect(View view) {
        view.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.animate().scaleX(0.97f).scaleY(0.97f).setDuration(100)
                            .setInterpolator(new DecelerateInterpolator()).start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(150)
                            .setInterpolator(new AccelerateInterpolator()).start();
                    break;
            }
            return false;
        });
    }
}
