package com.example.skillstat;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.skillstat.models.PracticeSession;
import com.example.skillstat.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class HeatmapFragment extends Fragment {

    private TextView tvActiveDays, tvBestStreak, tvTrackedWeeks;
    private GridLayout glHeatmap;
    private LinearLayout llMonthlyList, llWeekLabels;
    private DatabaseReference mDatabase;
    private String currentUid;
    
    private Map<String, Integer> dailyActivityData = new HashMap<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_heatmap, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mDatabase = FirebaseDatabase.getInstance().getReference();
        currentUid = FirebaseAuth.getInstance().getUid();

        tvActiveDays = view.findViewById(R.id.tv_active_days);
        tvBestStreak = view.findViewById(R.id.tv_best_streak);
        tvTrackedWeeks = view.findViewById(R.id.tv_tracked_weeks);
        glHeatmap = view.findViewById(R.id.gl_heatmap);
        llWeekLabels = view.findViewById(R.id.ll_week_labels);
        llMonthlyList = view.findViewById(R.id.ll_monthly_list);

        view.findViewById(R.id.btn_back).setOnClickListener(v -> getParentFragmentManager().popBackStack());
        
        loadHeatmapData();
    }

    private void loadHeatmapData() {
        if (currentUid == null) return;

        mDatabase.child("users").child(currentUid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                
                User user = snapshot.getValue(User.class);
                DataSnapshot sessionsSnapshot = snapshot.child("sessions");
                
                Calendar windowStart = Calendar.getInstance();
                windowStart.setFirstDayOfWeek(Calendar.MONDAY);
                windowStart.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
                windowStart.add(Calendar.WEEK_OF_YEAR, -14);
                windowStart.set(Calendar.HOUR_OF_DAY, 0);
                windowStart.set(Calendar.MINUTE, 0);
                windowStart.set(Calendar.SECOND, 0);
                windowStart.set(Calendar.MILLISECOND, 0);
                long windowStartMs = windowStart.getTimeInMillis();

                long latestBeforeWindow = -1;
                long earliestEver = Long.MAX_VALUE;

                dailyActivityData.clear();
                for (DataSnapshot ds : sessionsSnapshot.getChildren()) {
                    PracticeSession session = ds.getValue(PracticeSession.class);
                    if (session != null) {
                        long ts = session.getTimestamp();
                        if (ts < earliestEver) earliestEver = ts;
                        
                        if (ts < windowStartMs) {
                            if (ts > latestBeforeWindow) latestBeforeWindow = ts;
                        }

                        String dateKey = getFormattedDate(ts);
                        dailyActivityData.put(dateKey, dailyActivityData.getOrDefault(dateKey, 0) + session.getMinutes());
                    }
                }
                
                int finalDecay = populateHeatmap(dailyActivityData, latestBeforeWindow, earliestEver);
                updateStats(user, dailyActivityData, finalDecay);
                populateMonthlyBreakdown(dailyActivityData);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("HeatmapFragment", "Load failed", error.toException());
            }
        });
    }

    private String getFormattedDate(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(timestamp);
    }

    private int populateHeatmap(Map<String, Integer> dailyActivity, long latestBeforeWindow, long earliestEver) {
        if (glHeatmap == null) return 0;
        glHeatmap.removeAllViews();
        llWeekLabels.removeAllViews();

        Calendar cal = Calendar.getInstance();
        cal.setFirstDayOfWeek(Calendar.MONDAY);
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        cal.add(Calendar.WEEK_OF_YEAR, -14);

        // Initialize state
        int daysSinceLastPractice = 0;
        if (latestBeforeWindow != -1) {
            long diff = cal.getTimeInMillis() - latestBeforeWindow;
            daysSinceLastPractice = (int) (diff / (24 * 60 * 60 * 1000));
        }
        boolean hasStarted = (earliestEver <= cal.getTimeInMillis());

        for (int col = 0; col < 15; col++) {
            // Add Week Label
            TextView weekLabel = new TextView(getContext());
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(38, ViewGroup.LayoutParams.WRAP_CONTENT);
            weekLabel.setLayoutParams(lp);
            weekLabel.setTextSize(9);
            weekLabel.setTextColor(Color.parseColor("#8E8E93"));
            weekLabel.setGravity(Gravity.CENTER);
            if (col == 0) weekLabel.setText("W1");
            else if (col == 4) weekLabel.setText("W5");
            else if (col == 8) weekLabel.setText("W9");
            else if (col == 12) weekLabel.setText("W13");
            llWeekLabels.addView(weekLabel);

            Calendar dayCal = (Calendar) cal.clone();
            for (int row = 0; row < 7; row++) {
                long currentMs = dayCal.getTimeInMillis();
                String dateKey = getFormattedDate(currentMs);
                int minutes = dailyActivity.getOrDefault(dateKey, 0);

                if (!hasStarted && currentMs >= earliestEver) hasStarted = true;

                if (minutes > 0) {
                    daysSinceLastPractice = 0;
                } else if (hasStarted) {
                    daysSinceLastPractice++;
                }

                View cell = new View(getContext());
                GridLayout.LayoutParams params = new GridLayout.LayoutParams(
                        GridLayout.spec(row),
                        GridLayout.spec(col)
                );
                params.width = 30; 
                params.height = 30;
                params.setMargins(4, 4, 4, 4);
                cell.setLayoutParams(params);
                cell.setBackgroundResource(R.drawable.shape_heatmap_cell);
                cell.getBackground().setTint(getHeatmapColor(minutes, daysSinceLastPractice, hasStarted));
                
                glHeatmap.addView(cell);
                dayCal.add(Calendar.DAY_OF_YEAR, 1);
            }
            cal.add(Calendar.WEEK_OF_YEAR, 1);
        }
        return daysSinceLastPractice;
    }

    private int getHeatmapColor(int minutes, int daysSinceLastPractice, boolean hasStarted) {
        if (minutes > 0) {
            // Practiced today!
            if (minutes < 20) return Color.parseColor("#4CAF50"); // Fresh
            return Color.parseColor("#58CC02"); // Very Fresh / High Activity
        }
        
        if (!hasStarted) return Color.parseColor("#2D2D44"); // Before user started using the app

        // Decay logic for days with 0 minutes
        if (daysSinceLastPractice <= 2) return Color.parseColor("#2D2D44"); // Resting (Healthy)
        if (daysSinceLastPractice <= 4) return Color.parseColor("#FFD43B"); // Warning (Yellow)
        return Color.parseColor("#FF4B4B"); // At Risk (Red)
    }

    private void updateStats(User user, Map<String, Integer> dailyActivity, int currentDecay) {
        if (user == null) return;
        tvActiveDays.setText(dailyActivity.size() + "d");
        tvBestStreak.setText(user.getStreak() + "d");
        
        // Sharpness calculation based on current decay
        int sharpness = 100;
        if (currentDecay > 2) {
            sharpness = Math.max(0, 100 - (currentDecay * 10));
        }
        tvTrackedWeeks.setText(sharpness + "%");
    }

    private void populateMonthlyBreakdown(Map<String, Integer> dailyActivity) {
        if (llMonthlyList == null) return;
        llMonthlyList.removeAllViews();

        TreeMap<String, Integer> monthlyCount = new TreeMap<>((o1, o2) -> o2.compareTo(o1)); 
        SimpleDateFormat monthSdf = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());

        for (String date : dailyActivity.keySet()) {
            try {
                Calendar c = Calendar.getInstance();
                c.setTime(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(date));
                String monthKey = monthSdf.format(c.getTime());
                monthlyCount.put(monthKey, monthlyCount.getOrDefault(monthKey, 0) + 1);
            } catch (Exception e) {}
        }

        for (Map.Entry<String, Integer> entry : monthlyCount.entrySet()) {
            View itemView = getLayoutInflater().inflate(R.layout.item_monthly_breakdown, llMonthlyList, false);
            TextView tvMonth = itemView.findViewById(R.id.tv_month_name);
            TextView tvCount = itemView.findViewById(R.id.tv_month_count);
            ProgressBar progress = itemView.findViewById(R.id.pb_month_progress);

            tvMonth.setText(entry.getKey());
            tvCount.setText(entry.getValue() + " days");
            progress.setMax(31);
            progress.setProgress(entry.getValue());

            llMonthlyList.addView(itemView);

            View spacer = new View(getContext());
            spacer.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 16));
            llMonthlyList.addView(spacer);
        }
    }
}
