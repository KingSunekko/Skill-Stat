package com.example.skillstat;

import android.animation.ValueAnimator;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnticipateOvershootInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.skillstat.models.PracticeSession;
import com.example.skillstat.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class WeeklyReportFragment extends Fragment {

    private TextView tvWeeklyXp, tvWeeklyMinutes, tvWeeklySessions;
    private LinearLayout llBarChart, llInsightsList, llBySkillList;
    private DatabaseReference mDatabase;
    private String currentUid;
    private boolean hasAnimatedEntrance = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_weekly_report, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mDatabase = FirebaseDatabase.getInstance().getReference();
        currentUid = FirebaseAuth.getInstance().getUid();

        tvWeeklyXp = view.findViewById(R.id.tv_weekly_xp);
        tvWeeklyMinutes = view.findViewById(R.id.tv_weekly_minutes);
        tvWeeklySessions = view.findViewById(R.id.tv_weekly_sessions);
        llBarChart = view.findViewById(R.id.ll_weekly_bar_chart);
        llInsightsList = view.findViewById(R.id.ll_insights_list);
        llBySkillList = view.findViewById(R.id.ll_by_skill_list);

        view.findViewById(R.id.btn_back).setOnClickListener(v -> getParentFragmentManager().popBackStack());

        loadWeeklyData();
        
        // Initial setup for entrance
        view.findViewById(R.id.ll_weekly_stats_header).setAlpha(0);
        view.findViewById(R.id.ll_weekly_stats_header).setTranslationY(-20);
    }

    private void runEntranceAnimations() {
        if (hasAnimatedEntrance) return;
        hasAnimatedEntrance = true;

        View header = getView().findViewById(R.id.ll_weekly_stats_header);
        if (header != null) {
            header.animate().alpha(1).translationY(0).setDuration(400).setInterpolator(new DecelerateInterpolator()).start();
        }

        View chartCard = getView().findViewById(R.id.card_weekly_chart);
        if (chartCard != null) {
            chartCard.setAlpha(0);
            chartCard.setScaleY(0.9f);
            chartCard.animate().alpha(1).scaleY(1.0f).setDuration(600).setStartDelay(100).setInterpolator(new AnticipateOvershootInterpolator()).start();
        }
    }

    private void loadWeeklyData() {
        if (currentUid == null) return;

        mDatabase.child("users").child(currentUid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;

                DataSnapshot sessionsSnapshot = snapshot.child("sessions");
                
                int totalXp = 0;
                int totalMinutes = 0;
                int sessionCount = 0;
                
                int[] dailyXp = new int[7]; // Mon-Sun
                Map<String, Integer> skillXpMap = new HashMap<>();
                
                Calendar now = Calendar.getInstance();
                now.set(Calendar.HOUR_OF_DAY, 0);
                now.set(Calendar.MINUTE, 0);
                now.set(Calendar.SECOND, 0);
                now.set(Calendar.MILLISECOND, 0);
                
                // Set to Monday of current week
                Calendar weekStart = (Calendar) now.clone();
                int dayOfWeek = weekStart.get(Calendar.DAY_OF_WEEK);
                int diff = (dayOfWeek == Calendar.SUNDAY) ? -6 : (Calendar.MONDAY - dayOfWeek);
                weekStart.add(Calendar.DAY_OF_YEAR, diff);

                for (DataSnapshot ds : sessionsSnapshot.getChildren()) {
                    PracticeSession session = ds.getValue(PracticeSession.class);
                    if (session != null && session.getTimestamp() >= weekStart.getTimeInMillis()) {
                        totalXp += session.getXpEarned();
                        totalMinutes += session.getMinutes();
                        sessionCount++;
                        
                        Calendar sessionCal = Calendar.getInstance();
                        sessionCal.setTimeInMillis(session.getTimestamp());
                        int dayIdx = getDayIndex(sessionCal.get(Calendar.DAY_OF_WEEK));
                        dailyXp[dayIdx] += session.getXpEarned();
                        
                        skillXpMap.put(session.getSkillName(), skillXpMap.getOrDefault(session.getSkillName(), 0) + session.getXpEarned());
                    }
                }
                
                updateUI(totalXp, totalMinutes, sessionCount, dailyXp, skillXpMap);
                runEntranceAnimations();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("WeeklyReport", "Load failed", error.toException());
            }
        });
    }

    private int getDayIndex(int calendarDay) {
        switch (calendarDay) {
            case Calendar.MONDAY: return 0;
            case Calendar.TUESDAY: return 1;
            case Calendar.WEDNESDAY: return 2;
            case Calendar.THURSDAY: return 3;
            case Calendar.FRIDAY: return 4;
            case Calendar.SATURDAY: return 5;
            case Calendar.SUNDAY: return 6;
            default: return 0;
        }
    }

    private void updateUI(int xp, int mins, int sessions, int[] dailyXp, Map<String, Integer> skillXp) {
        animateNumber(tvWeeklyXp, xp, " XP");
        animateNumber(tvWeeklyMinutes, mins, "m");
        animateNumber(tvWeeklySessions, sessions, "");

        populateChart(dailyXp);
        populateInsights(dailyXp, sessions);
        populateBySkill(skillXp);
    }

    private void animateNumber(TextView tv, int target, String suffix) {
        int current = 0;
        try { current = Integer.parseInt(tv.getText().toString().replace(suffix, "")); } catch (Exception ignored) {}
        ValueAnimator animator = ValueAnimator.ofInt(current, target);
        animator.setDuration(1000);
        animator.addUpdateListener(animation -> tv.setText(animation.getAnimatedValue() + suffix));
        animator.start();
    }

    private void populateChart(int[] data) {
        llBarChart.removeAllViews();
        int max = 0;
        for (int val : data) if (val > max) max = val;
        
        // For normalization, if all are 0, use a baseline
        int chartMax = Math.max(max, 100);

        String[] labels = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        for (int i = 0; i < data.length; i++) {
            View barView = getLayoutInflater().inflate(R.layout.layout_stat_bar, llBarChart, false);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
            barView.setLayoutParams(params);

            View fill = barView.findViewById(R.id.view_bar_fill);
            TextView valText = barView.findViewById(R.id.tv_bar_value);
            TextView labelText = barView.findViewById(R.id.tv_bar_day);
            View spacer = barView.findViewById(R.id.v_bar_spacer);

            valText.setText(String.valueOf(data[i]));
            labelText.setText(labels[i]);

            float targetWeight = (float) data[i] / (float) chartMax;
            
            // Staggered bar animation
            final int index = i;
            fill.post(() -> {
                ValueAnimator anim = ValueAnimator.ofFloat(0f, Math.max(0.02f, targetWeight));
                anim.setDuration(1000);
                anim.setStartDelay(index * 50L);
                anim.setInterpolator(new DecelerateInterpolator());
                anim.addUpdateListener(animation -> {
                    if (!isAdded()) return;
                    float val = (float) animation.getAnimatedValue();
                    LinearLayout.LayoutParams fillParams = (LinearLayout.LayoutParams) fill.getLayoutParams();
                    LinearLayout.LayoutParams spacerParams = (LinearLayout.LayoutParams) spacer.getLayoutParams();
                    fillParams.weight = val;
                    spacerParams.weight = 1f - val;
                    fill.setLayoutParams(fillParams);
                    spacer.setLayoutParams(spacerParams);
                    valText.setAlpha(val > 0.05f ? 1f : 0f);
                });
                anim.start();
            });

            if (data[i] == max && data[i] > 0) {
                fill.setBackgroundResource(R.drawable.shape_stat_bar_fill); // Bright green
                valText.setTextColor(Color.parseColor("#58CC02"));
            } else {
                fill.setBackgroundResource(R.drawable.shape_stat_bar_fill_muted); // Darker green
            }

            llBarChart.addView(barView);
        }
    }

    private void populateInsights(int[] dailyXp, int sessions) {
        llInsightsList.removeAllViews();
        
        int bestDayIdx = 0;
        int max = 0;
        int missedDays = 0;
        String[] labels = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        
        for (int i = 0; i < dailyXp.length; i++) {
            if (dailyXp[i] > max) {
                max = dailyXp[i];
                bestDayIdx = i;
            }
            if (dailyXp[i] == 0) missedDays++;
        }

        addInsightItem("🏆", "Best day: " + labels[bestDayIdx] + " with " + max + " XP", 0);
        addInsightItem("📈", sessions > 5 ? "Great week! You're above your average." : "Keep it up! Consistency is key to mastery.", 1);
        addInsightItem("🎯", sessions + " sessions completed this week", 2);
        if (missedDays > 0) {
            addInsightItem("⚠️", missedDays + " missed day" + (missedDays > 1 ? "s" : "") + " — try a Streak Freeze next time!", 3);
        }
    }

    private void addInsightItem(String emoji, String text, int index) {
        View item = getLayoutInflater().inflate(R.layout.item_insight, llInsightsList, false);
        ((TextView) item.findViewById(R.id.tv_insight_icon)).setText(emoji);
        ((TextView) item.findViewById(R.id.tv_insight_text)).setText(text);
        
        item.setAlpha(0);
        item.setTranslationX(30);
        item.animate().alpha(1).translationX(0).setDuration(400).setStartDelay(400 + (index * 100L)).start();
        
        llInsightsList.addView(item);
        
        View space = new View(getContext());
        space.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 12));
        llInsightsList.addView(space);
    }

    private void populateBySkill(Map<String, Integer> skillXp) {
        llBySkillList.removeAllViews();
        if (skillXp.isEmpty()) {
            TextView tv = new TextView(getContext());
            tv.setText("No skill data for this week yet.");
            tv.setTextColor(Color.GRAY);
            tv.setGravity(android.view.Gravity.CENTER);
            llBySkillList.addView(tv);
            return;
        }

        int index = 0;
        int total = 0;
        for (int v : skillXp.values()) total += v;
        final int finalTotal = total;

        for (Map.Entry<String, Integer> entry : skillXp.entrySet()) {
            View item = getLayoutInflater().inflate(R.layout.item_by_skill, llBySkillList, false);
            ((TextView) item.findViewById(R.id.tv_skill_name)).setText(entry.getKey());
            ((TextView) item.findViewById(R.id.tv_skill_xp)).setText("+" + entry.getValue() + " XP");
            
            ProgressBar pb = item.findViewById(R.id.pb_skill_progress);
            pb.setMax(finalTotal);
            
            // Animate progress bar
            ValueAnimator anim = ValueAnimator.ofInt(0, entry.getValue());
            anim.setDuration(1000);
            anim.setStartDelay(600 + (index * 100L));
            anim.addUpdateListener(animation -> pb.setProgress((int) animation.getAnimatedValue()));
            anim.start();
            
            item.setAlpha(0);
            item.setTranslationY(20);
            item.animate().alpha(1).translationY(0).setDuration(400).setStartDelay(600 + (index * 100L)).start();
            
            llBySkillList.addView(item);
            
            View space = new View(getContext());
            space.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 16));
            llBySkillList.addView(space);
            index++;
        }
    }
}
