package com.example.skillstat;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;
import java.util.Random;

public class RewardsFragment extends Fragment {

    private FrameLayout confettiContainer;
    private AppCompatButton btnBackDashboard;
    private TextView tvRewardTime, tvRewardXp, tvSkillTag, tvResetInfo;
    private Random random = new Random();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_rewards, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        confettiContainer = view.findViewById(R.id.confetti_container);
        btnBackDashboard = view.findViewById(R.id.btn_back_dashboard);
        tvRewardTime = view.findViewById(R.id.tv_reward_time);
        tvRewardXp = view.findViewById(R.id.tv_reward_xp);
        tvSkillTag = view.findViewById(R.id.tv_skill_tag);
        tvResetInfo = view.findViewById(R.id.tv_reset_info);

        // Get data from arguments
        if (getArguments() != null) {
            String skillName = getArguments().getString("skill_name", "Java 💻");
            int minutes = getArguments().getInt("minutes_practiced", 1);
            
            if (tvSkillTag != null) tvSkillTag.setText(skillName);
            if (tvRewardTime != null) tvRewardTime.setText(minutes + " min");
            if (tvResetInfo != null) tvResetInfo.setText(skillName + " — next check in 7 days 📅");
        }

        btnBackDashboard.setOnClickListener(v -> {
            if (getActivity() != null) {
                // Pop back to the main Skills screen
                getActivity().getSupportFragmentManager().popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);
            }
        });

        // Start confetti animation
        view.postDelayed(this::startConfetti, 500);
    }

    private void startConfetti() {
        if (confettiContainer == null || getContext() == null) return;
        
        for (int i = 0; i < 100; i++) {
            createConfetto();
        }
    }

    private void createConfetto() {
        if (getContext() == null) return;

        final View confetto = new View(getContext());
        
        // Random size and shapes
        int size = random.nextInt(20) + 12;
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(size, size);
        confetto.setLayoutParams(params);

        GradientDrawable shape = new GradientDrawable();
        // Mix of circles, squares, and triangles (approximated by rectangles)
        int shapeType = random.nextInt(3);
        if (shapeType == 0) shape.setShape(GradientDrawable.OVAL);
        else shape.setShape(GradientDrawable.RECTANGLE);
        
        // Vibrant colors
        int[] colors = {
            Color.parseColor("#FF4B4B"), // Red
            Color.parseColor("#58CC02"), // Green
            Color.parseColor("#FF9800"), // Orange
            Color.parseColor("#21B1FF"), // Blue
            Color.parseColor("#FFD700"), // Gold
            Color.parseColor("#E040FB"), // Purple
            Color.parseColor("#00BCD4")  // Cyan
        };
        shape.setColor(colors[random.nextInt(colors.length)]);
        confetto.setBackground(shape);

        // Position at random width, slightly above screen
        int width = confettiContainer.getWidth();
        if (width <= 0) width = 1080; // Fallback
        confetto.setX(random.nextInt(width));
        confetto.setY(-100);

        confettiContainer.addView(confetto);

        // Animate falling
        long duration = random.nextInt(2000) + 2500;
        int height = confettiContainer.getHeight();
        if (height <= 0) height = 2000; // Fallback

        ObjectAnimator fall = ObjectAnimator.ofFloat(confetto, "translationY", height + 100);
        fall.setDuration(duration);
        fall.setInterpolator(new AccelerateInterpolator());

        // Animate rotation
        ObjectAnimator rotate = ObjectAnimator.ofFloat(confetto, "rotation", 0, random.nextInt(1080));
        rotate.setDuration(duration);

        // Animate horizontal drift
        float drift = (random.nextFloat() - 0.5f) * 300;
        ObjectAnimator sideWay = ObjectAnimator.ofFloat(confetto, "translationX", confetto.getX() + drift);
        sideWay.setDuration(duration);

        fall.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                confettiContainer.removeView(confetto);
            }
        });

        fall.start();
        rotate.start();
        sideWay.start();
    }
}
