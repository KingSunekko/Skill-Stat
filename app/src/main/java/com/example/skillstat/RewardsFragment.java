package com.example.skillstat;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;

import com.example.skillstat.models.PracticeSession;
import com.example.skillstat.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class RewardsFragment extends Fragment {

    private static final String TAG = "RewardsFragment";
    private FrameLayout confettiContainer;
    private AppCompatButton btnBackDashboard;
    private TextView tvRewardTime, tvRewardXp, tvSkillTag, tvResetInfo, tvTotalXp, tvRewardGrowth, tvRewardStreak;
    private Random random = new Random();
    private DatabaseReference mDatabase;
    private String currentUid;
    private boolean isFirebaseAvailable = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_rewards, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        try {
            mDatabase = FirebaseDatabase.getInstance().getReference();
            currentUid = FirebaseAuth.getInstance().getUid();
            isFirebaseAvailable = true;
        } catch (Exception e) {
            Log.e(TAG, "Firebase unavailable", e);
            isFirebaseAvailable = false;
        }

        confettiContainer = view.findViewById(R.id.confetti_container);
        btnBackDashboard = view.findViewById(R.id.btn_back_dashboard);
        tvRewardTime = view.findViewById(R.id.tv_reward_time);
        tvRewardXp = view.findViewById(R.id.tv_reward_xp);
        tvSkillTag = view.findViewById(R.id.tv_skill_tag);
        tvResetInfo = view.findViewById(R.id.tv_reset_info);
        tvTotalXp = view.findViewById(R.id.tv_total_xp);
        tvRewardGrowth = view.findViewById(R.id.tv_reward_growth);
        tvRewardStreak = view.findViewById(R.id.tv_reward_streak);

        if (getArguments() != null) {
            String skillName = getArguments().getString("skill_name", "General Practice");
            int minutes = getArguments().getInt("minutes_practiced", 1);
            int xpEarned = minutes * 10;

            if (tvSkillTag != null) tvSkillTag.setText(skillName);
            if (tvRewardTime != null) tvRewardTime.setText(minutes + " min");
            if (tvRewardXp != null) tvRewardXp.setText("+" + xpEarned);
            if (tvResetInfo != null) tvResetInfo.setText(skillName + " — next check in 7 days 📅");
            
            if (isFirebaseAvailable && currentUid != null) {
                saveRewardsToDatabase(skillName, minutes, xpEarned);
            } else {
                if (tvTotalXp != null) tvTotalXp.setText("Offline Mode");
            }
        }

        btnBackDashboard.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);
            }
        });

        view.postDelayed(this::startConfetti, 500);
    }

    private void saveRewardsToDatabase(String skillName, int minutes, int xpEarned) {
        if (currentUid == null || mDatabase == null) return;

        mDatabase.child("users").child(currentUid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                if (user != null) {
                    user.setUid(snapshot.getKey());
                    long now = System.currentTimeMillis();
                    
                    int newTotalXp = user.getTotalPoints() + xpEarned;
                    user.setTotalPoints(newTotalXp);
                    user.setCurrentDailyMinutes(user.getCurrentDailyMinutes() + minutes);
                    
                    Map<String, Integer> mastery = user.getSkillMastery();
                    int currentMastery = mastery.getOrDefault(skillName, 0);
                    int masteryGain = (minutes / 2) + 1;
                    int newMastery = Math.min(100, currentMastery + masteryGain);
                    mastery.put(skillName, newMastery);
                    
                    updateStreak(user, now);
                    user.setLastPracticeTimestamp(now);
                    checkAndUnlockBadges(user);

                    // Save Data
                    mDatabase.child("users").child(currentUid).setValue(user);
                    
                    PracticeSession session = new PracticeSession(skillName, minutes, xpEarned, now);
                    mDatabase.child("users").child(currentUid).child("sessions").push().setValue(session);

                    // Update UI with real values
                    if (isAdded()) {
                        if (tvTotalXp != null) tvTotalXp.setText(newTotalXp + " XP");
                        if (tvRewardStreak != null) tvRewardStreak.setText(String.valueOf(user.getStreak()));
                        if (tvRewardGrowth != null) tvRewardGrowth.setText("+" + masteryGain + "%");
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void updateStreak(User user, long now) {
        long lastPractice = user.getLastPracticeTimestamp();
        if (lastPractice == 0) { user.setStreak(1); return; }
        Calendar lastCal = Calendar.getInstance(); lastCal.setTimeInMillis(lastPractice);
        Calendar currentCal = Calendar.getInstance(); currentCal.setTimeInMillis(now);
        lastCal.set(Calendar.HOUR_OF_DAY, 0); lastCal.set(Calendar.MINUTE, 0); lastCal.set(Calendar.SECOND, 0); lastCal.set(Calendar.MILLISECOND, 0);
        currentCal.set(Calendar.HOUR_OF_DAY, 0); currentCal.set(Calendar.MINUTE, 0); currentCal.set(Calendar.SECOND, 0); currentCal.set(Calendar.MILLISECOND, 0);
        long diffInDays = TimeUnit.MILLISECONDS.toDays(currentCal.getTimeInMillis() - lastCal.getTimeInMillis());
        if (diffInDays == 1) user.setStreak(user.getStreak() + 1);
        else if (diffInDays > 1) user.setStreak(1);
    }

    private void checkAndUnlockBadges(User user) {
        List<String> earned = user.getBadges();
        if (earned == null) earned = new ArrayList<>();
        if (!earned.contains("First Step") && user.getTotalPoints() > 0) earned.add("First Step");
        if (!earned.contains("XP Hunter") && user.getTotalPoints() >= 1000) earned.add("XP Hunter");
        if (!earned.contains("On Fire") && user.getStreak() >= 3) earned.add("On Fire");
        user.setBadges(earned);
    }

    private void startConfetti() {
        if (confettiContainer == null || !isAdded()) return;
        for (int i = 0; i < 60; i++) { createConfetto(); }
    }

    private void createConfetto() {
        if (!isAdded() || getContext() == null) return;
        final View confetto = new View(getContext());
        int size = random.nextInt(20) + 12;
        confetto.setLayoutParams(new FrameLayout.LayoutParams(size, size));
        GradientDrawable shape = new GradientDrawable();
        shape.setShape(random.nextInt(2) == 0 ? GradientDrawable.OVAL : GradientDrawable.RECTANGLE);
        int[] colors = {Color.parseColor("#FF4B4B"), Color.parseColor("#58CC02"), Color.parseColor("#FF9800"), Color.parseColor("#21B1FF")};
        shape.setColor(colors[random.nextInt(colors.length)]);
        confetto.setBackground(shape);
        confetto.setX(random.nextInt(Math.max(1, confettiContainer.getWidth())));
        confetto.setY(-100);
        confettiContainer.addView(confetto);
        ObjectAnimator fall = ObjectAnimator.ofFloat(confetto, "translationY", confettiContainer.getHeight() + 100);
        fall.setDuration(random.nextInt(2000) + 2000);
        fall.setInterpolator(new AccelerateInterpolator());
        fall.addListener(new AnimatorListenerAdapter() { @Override public void onAnimationEnd(Animator animation) { confettiContainer.removeView(confetto); } });
        fall.start();
    }
}
