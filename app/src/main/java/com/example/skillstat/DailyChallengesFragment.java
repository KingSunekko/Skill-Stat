package com.example.skillstat;

import android.animation.ValueAnimator;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AnticipateOvershootInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;
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

import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DailyChallengesFragment extends Fragment {

    private TextView tvChallengesCount, tvChallengesXpToday, tvFreezeCount;
    private View vProgressFill, vProgressBg;
    private LinearLayout llChallengesList;
    private DatabaseReference mDatabase;
    private String currentUid;
    private User cachedUser;
    private boolean hasAnimatedEntrance = false;
    private ValueEventListener userListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_daily_challenges, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mDatabase = FirebaseDatabase.getInstance().getReference();
        currentUid = FirebaseAuth.getInstance().getUid();

        tvChallengesCount = view.findViewById(R.id.tv_challenges_count);
        tvChallengesXpToday = view.findViewById(R.id.tv_challenges_xp_today);
        tvFreezeCount = view.findViewById(R.id.tv_freeze_count);
        vProgressFill = view.findViewById(R.id.v_challenges_progress_fill);
        vProgressBg = view.findViewById(R.id.v_challenges_progress_bg);
        llChallengesList = view.findViewById(R.id.ll_challenges_list);

        view.findViewById(R.id.btn_back).setOnClickListener(v -> getParentFragmentManager().popBackStack());

        loadChallengesData();
        
        // Initial setup for entrance
        view.findViewById(R.id.ll_challenges_header).setAlpha(0);
        view.findViewById(R.id.ll_challenges_header).setTranslationY(-20);
        view.findViewById(R.id.card_challenges_summary).setAlpha(0);
        view.findViewById(R.id.card_challenges_summary).setScaleX(0.9f);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mDatabase != null && userListener != null && currentUid != null) {
            mDatabase.child("users").child(currentUid).removeEventListener(userListener);
        }
    }

    private void runEntranceAnimations() {
        if (hasAnimatedEntrance || getView() == null) return;
        hasAnimatedEntrance = true;

        View header = getView().findViewById(R.id.ll_challenges_header);
        if (header != null) {
            header.animate().alpha(1).translationY(0).setDuration(400).setInterpolator(new DecelerateInterpolator()).start();
        }

        View summary = getView().findViewById(R.id.card_challenges_summary);
        if (summary != null) {
            summary.animate().alpha(1).scaleX(1.0f).setDuration(600).setStartDelay(100).setInterpolator(new AnticipateOvershootInterpolator()).start();
        }
    }

    private void loadChallengesData() {
        if (currentUid == null) return;

        userListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                cachedUser = snapshot.getValue(User.class);
                if (cachedUser != null) {
                    updateUI(cachedUser);
                    runEntranceAnimations();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("DailyChallenges", "Load failed", error.toException());
            }
        };
        mDatabase.child("users").child(currentUid).addValueEventListener(userListener);
    }

    private void updateUI(User user) {
        int completed = user.getCompletedChallengesCount();
        int total = 5;
        tvChallengesCount.setText(String.format(Locale.US, "%d/%d", completed, total));
        
        animateProgressBar(vProgressBg, vProgressFill, (float) completed / total);

        tvChallengesXpToday.setText(String.format(Locale.US, "⭐ %d XP earned today from challenges", user.getChallengeXpToday()));
        tvFreezeCount.setText(String.format(Locale.US, "You have %d freeze left this week. It protects your streak if you miss a day.", user.getStreakFreezes()));

        llChallengesList.removeAllViews();

        int index = 0;
        addChallengeItem("Speed Round", "Practice any skill for just 5 minutes", "+30 XP", "⚡", "speed_round", user.isChallengeCompleted("speed_round"), index++);
        addChallengeItem("Focus Mode", "Practice your weakest skill today", "+50 XP", "🎯", "focus_mode", user.isChallengeCompleted("focus_mode"), index++);
        addChallengeItem("Double Session", "Practice 2 different skills today", "+80 XP", "🔥", "double_session", user.isChallengeCompleted("double_session"), index++);
        addChallengeItem("Reflect & Write", "Complete a session and write a practice note", "+60 XP", "📝", "reflect_write", user.isChallengeCompleted("reflect_write"), index++);
        addChallengeItem("No Excuses", "Practice before 9am", "+100 XP", "🏃", "no_excuses", user.isChallengeCompleted("no_excuses"), index++);
    }

    private void animateProgressBar(View bg, View fill, float ratio) {
        if (bg == null || fill == null) return;
        bg.post(() -> {
            if (!isAdded()) return;
            int bgWidth = bg.getWidth();
            if (bgWidth <= 0) return;
            
            int targetWidth = (int) (bgWidth * Math.min(1.0f, ratio));
            int initialWidth = fill.getWidth();
            
            ValueAnimator animator = ValueAnimator.ofInt(initialWidth, targetWidth);
            animator.setDuration(1000);
            animator.setInterpolator(new AccelerateDecelerateInterpolator());
            animator.addUpdateListener(animation -> {
                if (!isAdded() || fill == null) return;
                ViewGroup.LayoutParams params = fill.getLayoutParams();
                params.width = (int) animation.getAnimatedValue();
                fill.setLayoutParams(params);
            });
            animator.start();
        });
    }

    private void addChallengeItem(String title, String desc, String xp, String icon, String key, boolean isCompleted, int index) {
        if (getContext() == null) return;
        View item = getLayoutInflater().inflate(R.layout.layout_challenge_item, llChallengesList, false);
        ((TextView) item.findViewById(R.id.tv_challenge_title)).setText(title);
        ((TextView) item.findViewById(R.id.tv_challenge_desc)).setText(desc);
        ((TextView) item.findViewById(R.id.tv_challenge_xp)).setText(xp);
        ((TextView) item.findViewById(R.id.tv_challenge_icon)).setText(icon);

        View btnGo = item.findViewById(R.id.btn_challenge_go);
        View check = item.findViewById(R.id.iv_challenge_check);

        if (isCompleted) {
            btnGo.setVisibility(View.GONE);
            check.setVisibility(View.VISIBLE);
            item.setAlpha(0.6f);
            TextView tvTitle = item.findViewById(R.id.tv_challenge_title);
            tvTitle.setPaintFlags(tvTitle.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
        } else {
            btnGo.setVisibility(View.VISIBLE);
            check.setVisibility(View.GONE);
            btnGo.setOnClickListener(v -> {
                v.animate().scaleX(0.9f).scaleY(0.9f).setDuration(100).withEndAction(() -> v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start()).start();
                startChallenge(key, title);
            });
        }

        // Only animate if it's the first time
        if (!hasAnimatedEntrance) {
            item.setAlpha(0);
            item.setTranslationX(50);
            item.animate().alpha(1).translationX(0).setDuration(400).setStartDelay(300 + (index * 100L)).start();
        }

        llChallengesList.addView(item);
        
        View space = new View(getContext());
        space.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 16));
        llChallengesList.addView(space);
    }

    private void startChallenge(String key, String title) {
        if (cachedUser == null || !isAdded()) return;

        String targetSkill = "General Practice";
        List<String> skills = cachedUser.getSkills();
        
        if (skills == null || skills.isEmpty()) {
            Toast.makeText(getContext(), "Add a skill first to start challenges!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (key.equals("focus_mode")) {
            double minMastery = 101.0;
            Map<String, Double> masteryMap = cachedUser.getSkillMastery();
            for (String s : skills) {
                double m = masteryMap.getOrDefault(s, 0.0);
                if (m < minMastery) {
                    minMastery = m;
                    targetSkill = s;
                }
            }
        } else {
            targetSkill = skills.get(0);
        }

        PracticeFragment practiceFragment = new PracticeFragment();
        Bundle args = new Bundle();
        args.putString("skill_name", targetSkill);
        args.putString("challenge_key", key);
        args.putString("challenge_title", title);
        practiceFragment.setArguments(args);

        getParentFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.step_forward_enter, R.anim.step_forward_exit, R.anim.step_backward_enter, R.anim.step_backward_exit)
                .replace(R.id.fragment_container, practiceFragment)
                .addToBackStack(null)
                .commit();
        
        Toast.makeText(getContext(), "Challenge Started: " + title, Toast.LENGTH_SHORT).show();
    }
}
