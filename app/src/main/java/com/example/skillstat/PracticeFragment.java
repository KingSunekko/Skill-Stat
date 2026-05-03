package com.example.skillstat;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import java.util.Locale;

public class PracticeFragment extends Fragment {

    private CircularProgressIndicator cpTimer;
    private TextView tvCountdown, tvPercentage, tvSkillNameDisplay;
    private AppCompatButton btnPauseResume, btnFinish;
    private View btnBack;

    private CountDownTimer countDownTimer;
    private boolean isTimerRunning;
    private long timeLeftInMillis;
    private long totalTimeInMillis = 5 * 60 * 1000; // 5 minutes default
    private String skillName = "Java 💻";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_practice, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            skillName = getArguments().getString("skill_name", "Java 💻");
        }

        tvSkillNameDisplay = view.findViewById(R.id.tv_skill_name);
        cpTimer = view.findViewById(R.id.cp_timer);
        tvCountdown = view.findViewById(R.id.tv_timer_countdown);
        tvPercentage = view.findViewById(R.id.tv_timer_percentage);
        btnPauseResume = view.findViewById(R.id.btn_pause_resume);
        btnFinish = view.findViewById(R.id.btn_finish);
        btnBack = view.findViewById(R.id.btn_back);

        if (tvSkillNameDisplay != null) {
            tvSkillNameDisplay.setText(skillName);
        }

        timeLeftInMillis = totalTimeInMillis;
        updateTimerUI();

        btnPauseResume.setOnClickListener(v -> {
            if (isTimerRunning) {
                pauseTimer();
            } else {
                startTimer();
            }
        });

        btnFinish.setOnClickListener(v -> {
            navigateToRewards();
        });

        btnBack.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });

        startTimer();
    }

    private void navigateToRewards() {
        if (getActivity() != null) {
            RewardsFragment rewardsFragment = new RewardsFragment();
            Bundle args = new Bundle();
            args.putString("skill_name", skillName);
            // Calculate actual practice time
            long timePracticed = totalTimeInMillis - timeLeftInMillis;
            int minutes = (int) (timePracticed / 1000) / 60;
            if (minutes == 0) minutes = 1; // Show at least 1 min for reward
            args.putInt("minutes_practiced", minutes);
            rewardsFragment.setArguments(args);

            getActivity().getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(
                            R.anim.step_forward_enter,
                            R.anim.step_forward_exit,
                            R.anim.step_backward_enter,
                            R.anim.step_backward_exit
                    )
                    .replace(R.id.fragment_container, rewardsFragment)
                    .addToBackStack(null)
                    .commit();
        }
    }

    private void startTimer() {
        if (countDownTimer != null) countDownTimer.cancel();
        
        countDownTimer = new CountDownTimer(timeLeftInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeftInMillis = millisUntilFinished;
                updateTimerUI();
            }

            @Override
            public void onFinish() {
                isTimerRunning = false;
                timeLeftInMillis = 0;
                updateTimerUI();
                btnPauseResume.setText("▶ START");
            }
        }.start();

        isTimerRunning = true;
        btnPauseResume.setText("⏸ PAUSE");
    }

    private void pauseTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        isTimerRunning = false;
        btnPauseResume.setText("▶ RESUME");
    }

    private void updateTimerUI() {
        int minutes = (int) (timeLeftInMillis / 1000) / 60;
        int seconds = (int) (timeLeftInMillis / 1000) % 60;

        String timeLeftFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
        tvCountdown.setText(timeLeftFormatted);

        int progress = (int) ((totalTimeInMillis - timeLeftInMillis) * 100 / totalTimeInMillis);
        cpTimer.setProgress(progress);
        tvPercentage.setText(progress + "%");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
}
