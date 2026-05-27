package com.example.skillstat;

import android.animation.ValueAnimator;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.skillstat.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class StatsFragment extends Fragment {

    private TextView tvSkillName, tvMainPercentage, tvGrowthBadge, tvPeak, tvLow, tvAvg;
    private LinearLayout llBarChart, llDecayList, llTabsContainer;
    private GridLayout glHeatmap;
    private DatabaseReference mDatabase;
    private ValueEventListener statsListener;
    private String currentUid, selectedSkill = null;
    private User cachedUser;
    private DataSnapshot lastSnapshot;
    private boolean hasAnimatedEntrance = false;

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

        initViews(view);
        startStatsListener();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mDatabase != null && statsListener != null && currentUid != null) {
            mDatabase.child("users").child(currentUid).removeEventListener(statsListener);
        }
    }

    private void initViews(View v) {
        tvSkillName = v.findViewById(R.id.tv_stats_skill_name);
        tvMainPercentage = v.findViewById(R.id.tv_stats_percentage);
        tvGrowthBadge = v.findViewById(R.id.tv_stats_growth_badge);
        tvPeak = v.findViewById(R.id.tv_peak_value);
        tvLow = v.findViewById(R.id.tv_low_value);
        tvAvg = v.findViewById(R.id.tv_avg_value);
        llBarChart = v.findViewById(R.id.ll_bar_chart);
        llDecayList = v.findViewById(R.id.ll_decay_list);
        llTabsContainer = v.findViewById(R.id.ll_stats_tabs);
        glHeatmap = v.findViewById(R.id.gl_heatmap);
        
        v.findViewById(R.id.card_stats_main).setAlpha(0);
        v.findViewById(R.id.card_stats_main).setTranslationY(30);
    }

    private void startStatsListener() {
        if (currentUid == null) return;
        statsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                lastSnapshot = snapshot;
                cachedUser = snapshot.getValue(User.class);
                if (cachedUser != null) {
                    updateUI();
                    if (!hasAnimatedEntrance) {
                        hasAnimatedEntrance = true;
                        View mainCard = getView().findViewById(R.id.card_stats_main);
                        if (mainCard != null) {
                            mainCard.animate().alpha(1).translationY(0).setDuration(600).setInterpolator(new DecelerateInterpolator()).start();
                        }
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        };
        mDatabase.child("users").child(currentUid).addValueEventListener(statsListener);
    }

    private void updateUI() {
        if (cachedUser == null) return;
        List<String> skills = cachedUser.getSkills();
        if (skills == null || skills.isEmpty()) return;
        
        if (selectedSkill == null) selectedSkill = skills.get(0);

        llTabsContainer.removeAllViews();
        for (String skill : skills) addSkillTab(skill);

        String skillKey = selectedSkill.replace(".", "_");
        double currentMastery = cachedUser.getSkillMastery().getOrDefault(skillKey, 0.0);
        tvSkillName.setText(selectedSkill);
        animateNumber(tvMainPercentage, (int)currentMastery, "%");

        if (lastSnapshot != null) {
            updateChartData(lastSnapshot.child("history"), skillKey, currentMastery);
            updateHeatmap(lastSnapshot.child("history"), skillKey);
        }
        
        populateDecayList(cachedUser);
    }

    private void updateChartData(DataSnapshot historySnap, String skillKey, double liveMastery) {
        double[] weekData = new double[7];
        Calendar cal = Calendar.getInstance();
        cal.setFirstDayOfWeek(Calendar.MONDAY);
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0);
        
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        Calendar today = Calendar.getInstance();
        String todayStr = df.format(today.getTime());
        double baseline = 0;
        
        for (DataSnapshot dateSnap : historySnap.getChildren()) {
            String key = dateSnap.getKey();
            if (key != null && key.compareTo(df.format(cal.getTime())) < 0) {
                Object val = dateSnap.child(skillKey).getValue();
                if (val != null) baseline = ((Number) val).doubleValue();
            }
        }

        double lastVal = baseline;
        for (int i = 0; i < 7; i++) {
            String dateKey = df.format(cal.getTime());
            if (dateKey.equals(todayStr)) {
                weekData[i] = liveMastery;
                lastVal = liveMastery;
            } else if (cal.after(today)) {
                weekData[i] = 0;
            } else {
                if (historySnap.hasChild(dateKey) && historySnap.child(dateKey).hasChild(skillKey)) {
                    Object val = historySnap.child(dateKey).child(skillKey).getValue();
                    if (val != null) lastVal = ((Number) val).doubleValue();
                }
                weekData[i] = lastVal;
            }
            cal.add(Calendar.DATE, 1);
        }
        renderBars(weekData, baseline, liveMastery);
    }

    private void renderBars(double[] data, double baseline, double liveMastery) {
        llBarChart.removeAllViews();
        String[] labels = {"M", "T", "W", "T", "F", "S", "S"};
        double peak = 0, low = 100, sum = 0;
        int count = 0;
        Calendar today = Calendar.getInstance();
        int currentDayIdx = (today.get(Calendar.DAY_OF_WEEK) + 5) % 7;

        for (int i = 0; i < 7; i++) {
            if (data[i] > 0) {
                peak = Math.max(peak, data[i]);
                low = Math.min(low, data[i]);
                sum += data[i];
                count++;
            }
            View barView = getLayoutInflater().inflate(R.layout.item_stats_bar, llBarChart, false);
            View bar = barView.findViewById(R.id.view_bar);
            TextView tvValue = barView.findViewById(R.id.tv_bar_value);
            TextView tvDay = barView.findViewById(R.id.tv_bar_day);
            tvDay.setText(labels[i]);
            if (i == currentDayIdx) tvDay.setTextColor(Color.parseColor("#58CC02"));
            if (data[i] > 0) {
                tvValue.setText((int)data[i] + "%");
                float scale = (float) (data[i] / 100.0);
                int maxHeight = (int) (110 * getResources().getDisplayMetrics().density);
                bar.getLayoutParams().height = Math.max((int)(12 * getResources().getDisplayMetrics().density), (int)(maxHeight * scale));
            } else {
                tvValue.setVisibility(View.INVISIBLE);
                bar.setVisibility(View.INVISIBLE);
            }
            llBarChart.addView(barView);
        }
        animateNumber(tvPeak, (int)peak, "%");
        animateNumber(tvLow, (int)(low == 100 ? 0 : low), "%");
        animateNumber(tvAvg, (int)(count == 0 ? 0 : sum / count), "%");
        double growth = liveMastery - baseline;
        tvGrowthBadge.setText(String.format(Locale.US, "%s%.1f%% growth this week", (growth >= 0 ? "+" : ""), growth));
    }

    private void updateHeatmap(DataSnapshot historySnap, String skillKey) {
        if (glHeatmap == null || cachedUser == null) return;
        glHeatmap.removeAllViews();
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -27);
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        String todayStr = df.format(Calendar.getInstance().getTime());
        long lastPractice = cachedUser.getSkillLastPractice().getOrDefault(skillKey, 0L);
        boolean practicedToday = df.format(lastPractice).equals(todayStr);

        for (int i = 0; i < 28; i++) {
            View cell = new View(getContext());
            int size = (int) (14 * getResources().getDisplayMetrics().density);
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = size; params.height = size;
            params.setMargins(6, 6, 6, 6);
            cell.setLayoutParams(params);
            String dateKey = df.format(cal.getTime());
            boolean active = (historySnap.hasChild(dateKey) && historySnap.child(dateKey).hasChild(skillKey)) 
                            || (dateKey.equals(todayStr) && practicedToday);
            cell.setBackgroundResource(R.drawable.shape_heatmap_cell);
            if (active) cell.getBackground().setTint(Color.parseColor("#58CC02"));
            glHeatmap.addView(cell);
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }
    }

    private void populateDecayList(User user) {
        llDecayList.removeAllViews();
        List<String> skills = user.getSkills();
        if (skills == null) return;
        for (String name : skills) {
            String key = name.replace(".", "_");
            long last = user.getSkillLastPractice().getOrDefault(key, 0L);
            int decayDays = user.getSkillDecaySettings().getOrDefault(key, 7);
            
            long msSinceLast = System.currentTimeMillis() - last;
            long daysSinceLast = msSinceLast / (1000 * 60 * 60 * 24);
            long daysLeft = Math.max(0, decayDays - daysSinceLast);
            
            // Logic consistency with HomeFragment
            boolean practicedToday = false;
            if (last > 0) {
                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                practicedToday = df.format(last).equals(df.format(System.currentTimeMillis()));
            }

            View item = getLayoutInflater().inflate(R.layout.item_decay_status, llDecayList, false);
            ((TextView)item.findViewById(R.id.tv_decay_skill_name)).setText(name);
            
            TextView tvStatus = item.findViewById(R.id.tv_decay_status_pill);
            ProgressBar pb = item.findViewById(R.id.pb_decay_status);
            TextView tvMsg = item.findViewById(R.id.tv_decay_message);
            TextView tvIcon = item.findViewById(R.id.tv_decay_icon);

            int progress = (int) (((double) daysLeft / decayDays) * 100);
            pb.setProgress(progress);

            if (daysSinceLast >= decayDays) {
                tvStatus.setText("Decayed");
                tvStatus.setTextColor(Color.parseColor("#FF4B4B"));
                tvStatus.setBackgroundResource(R.drawable.shape_status_pill_at_risk);
                pb.setProgressDrawable(ContextCompat.getDrawable(getContext(), R.drawable.progress_bar_decay_red));
                tvMsg.setText("Skill has faded. Practice to restore!");
                tvIcon.setText("💀");
            } else if (!practicedToday && daysSinceLast >= (decayDays - 1)) {
                tvStatus.setText("At Risk");
                tvStatus.setTextColor(Color.parseColor("#FFD700"));
                tvStatus.setBackgroundResource(R.drawable.shape_status_pill_at_risk);
                pb.setProgressDrawable(ContextCompat.getDrawable(getContext(), R.drawable.progress_bar_decay_red));
                tvMsg.setText("Practice NOW to prevent decay!");
                tvIcon.setText("⚠️");
            } else {
                tvStatus.setText("Sharp");
                tvStatus.setTextColor(Color.parseColor("#58CC02"));
                tvStatus.setBackgroundResource(R.drawable.shape_status_pill_sharp);
                pb.setProgressDrawable(ContextCompat.getDrawable(getContext(), R.drawable.progress_bar_decay_green)); 
                tvMsg.setText("Your skill is currently sharp!");
                tvIcon.setText("✅");
            }
            llDecayList.addView(item);
        }
    }

    private void addSkillTab(String name) {
        TextView tv = new TextView(getContext());
        tv.setText(name);
        tv.setPadding(44, 22, 44, 22);
        tv.setTypeface(null, android.graphics.Typeface.BOLD);
        tv.setTextColor(name.equals(selectedSkill) ? Color.parseColor("#58CC02") : Color.GRAY);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 24, 0);
        tv.setLayoutParams(lp);
        if (name.equals(selectedSkill)) {
            tv.setBackgroundResource(R.drawable.shape_reward_skill_pill);
        } else {
            tv.setBackgroundResource(R.drawable.shape_skill_chip_selector);
        }
        tv.setOnClickListener(v -> { selectedSkill = name; updateUI(); });
        llTabsContainer.addView(tv);
    }

    private void animateNumber(TextView tv, int target, String suffix) {
        if (tv == null) return;
        int current = 0;
        try { 
            String val = tv.getText().toString().replaceAll("[^0-9]", "");
            if (!val.isEmpty()) current = Integer.parseInt(val);
        } catch (Exception ignored) {}
        ValueAnimator a = ValueAnimator.ofInt(current, target);
        a.setDuration(800);
        a.addUpdateListener(anim -> tv.setText(anim.getAnimatedValue() + suffix));
        a.start();
    }
}
