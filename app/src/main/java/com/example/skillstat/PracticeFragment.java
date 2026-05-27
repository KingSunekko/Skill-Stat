package com.example.skillstat;

import android.animation.ObjectAnimator;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;

import com.example.skillstat.models.Duel;
import com.example.skillstat.models.Message;
import com.example.skillstat.models.PracticeSession;
import com.example.skillstat.models.User;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class PracticeFragment extends Fragment {

    private CircularProgressIndicator cpTimer, cpRivalGhost;
    private TextView tvCountdown, tvPercentage, tvSkillNameDisplay, tvGoalInfo, tvDuelStatusLive, tvGhostAvatar;
    private View flGhostIndicator, llDuelStatusContainer, llBattleShout;
    private AppCompatButton btnPauseResume, btnFinish;
    private View btnBack;
    private View cardChallenge;
    private TextView tvChallengeTitle;
    private EditText etPracticeNote;
    private SwitchMaterial switchStrictMode;
    private ImageView ivZenInfo;
    private GridLayout glHeatmap;

    private TextInputLayout tilSubSkill;
    private AutoCompleteTextView actvSubSkill;

    // Soundscape UI
    private LinearLayout llSoundscapes;
    private View btnSoundNone, btnSoundLofi, btnSoundWhite, btnSoundBinaural;
    private int selectedSoundId = 0; // 0 for none, or R.raw.xxx

    private boolean isTimerRunning = false;
    private boolean isFinishing = false;
    private boolean hasStartedSession = false; // Flag to prevent immediate rewards navigation
    private boolean isAutoFinishMode = false;
    private long timeLeftInMillis = -1; // Initialize to -1
    private long totalTimeInMillis = 5 * 60 * 1000; // 5 minutes default fallback
    private String skillName = "Java 💻";
    private String selectedSubSkill = null;
    private String challengeKey = null;
    private String challengeTitle = null;
    private DatabaseReference mDatabase;

    // Ghost Duel Data
    private double opponentEffortInDuel = 0;
    private double myStartEffortInDuel = 0;
    private boolean isDuelActive = false;
    private boolean wasTrailing = false;
    private String opponentAvatar = "👤";
    private String opponentName = "Opponent";
    private String opponentUid = null;
    private Duel currentDuelData = null;
    private long rivalryWins = 0, rivalryLosses = 0;
    private int myCurrentStreak = 0;

    // Self Ghost Data (Personal Best)
    private boolean isSelfGhostActive = false;
    private int pbSeconds = 0;
    private PracticeSession pbSession = null;

    private ValueEventListener duelListener;
    private Query activeDuelQuery;

    private FocusService focusService;
    private boolean isBound = false;

    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            FocusService.LocalBinder binder = (FocusService.LocalBinder) service;
            focusService = binder.getService();
            isBound = true;
            focusService.enterForeground(); // User is back

            // Sync sound if already playing
            if (selectedSoundId != 0) {
                updateServiceSound();
            }

            // Check if we need to finish immediately (from notification)
            if (isAutoFinishMode) {
                new Handler().postDelayed(() -> onNotificationFinishTriggered(), 500);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_practice, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Reset strict mode state for the new session
        FocusService.isStrictModeTriggered.setValue(false);

        mDatabase = FirebaseDatabase.getInstance().getReference();

        if (getArguments() != null) {
            skillName = getArguments().getString("skill_name", "Java 💻");
            challengeKey = getArguments().getString("challenge_key");
            challengeTitle = getArguments().getString("challenge_title");
            isAutoFinishMode = getArguments().getBoolean("auto_finish", false);
            if (getArguments().containsKey("time_left")) {
                timeLeftInMillis = getArguments().getLong("time_left");
                hasStartedSession = true; 
            }
        }

        initViews(view);
        setupClickListeners();
        setupSoundscapeListeners();

        loadUserGoalAndSubSkills();
        fetchDuelProgress();
        setupObservers();

        animateEntrance();
    }

    public void updateTimeLeftExternally(long timeLeft) {
        this.timeLeftInMillis = timeLeft;
        this.hasStartedSession = true;
        updateTimerUI();
    }

    public void onNotificationFinishTriggered() {
        if (!isFinishing) {
            navigateToRewards();
        }
    }

    private void initViews(View view) {
        tvSkillNameDisplay = view.findViewById(R.id.tv_skill_name);
        tvGoalInfo = view.findViewById(R.id.tv_goal_info);
        cpTimer = view.findViewById(R.id.cp_timer);
        cpRivalGhost = view.findViewById(R.id.cp_rival_ghost);
        tvCountdown = view.findViewById(R.id.tv_timer_countdown);
        tvPercentage = view.findViewById(R.id.tv_timer_percentage);
        tvDuelStatusLive = view.findViewById(R.id.tv_duel_status_live);
        llDuelStatusContainer = view.findViewById(R.id.ll_duel_status_container);
        tvGhostAvatar = view.findViewById(R.id.tv_ghost_avatar);
        flGhostIndicator = view.findViewById(R.id.fl_ghost_indicator);
        llBattleShout = view.findViewById(R.id.ll_battle_shout);

        btnPauseResume = view.findViewById(R.id.btn_pause_resume);
        btnFinish = view.findViewById(R.id.btn_finish);
        btnBack = view.findViewById(R.id.btn_back);
        etPracticeNote = view.findViewById(R.id.et_practice_note);
        switchStrictMode = view.findViewById(R.id.switch_strict_mode);
        ivZenInfo = view.findViewById(R.id.iv_zen_info);
        glHeatmap = view.findViewById(R.id.gl_heatmap_preview);

        tilSubSkill = view.findViewById(R.id.til_sub_skill);
        actvSubSkill = view.findViewById(R.id.actv_sub_skill);

        cardChallenge = view.findViewById(R.id.card_practice_challenge);
        tvChallengeTitle = view.findViewById(R.id.tv_practice_challenge_title);

        // Soundscape Views
        llSoundscapes = view.findViewById(R.id.ll_soundscapes);
        btnSoundNone = view.findViewById(R.id.btn_sound_none);
        btnSoundLofi = view.findViewById(R.id.btn_sound_lofi);
        btnSoundWhite = view.findViewById(R.id.btn_sound_white);
        btnSoundBinaural = view.findViewById(R.id.btn_sound_binaural);

        if (tvSkillNameDisplay != null) tvSkillNameDisplay.setText(skillName);

        if (switchStrictMode != null) {
            if (llSoundscapes != null) {
                llSoundscapes.setVisibility(switchStrictMode.isChecked() ? View.VISIBLE : View.GONE);
            }

            switchStrictMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (llSoundscapes != null) {
                    llSoundscapes.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                    if (!isChecked) {
                        selectSound(0);
                    }
                }

                if (isChecked && pbSession != null && !isDuelActive) {
                    activateSelfGhost();
                } else if (!isChecked && !isDuelActive) {
                    deactivateSelfGhost();
                }
            });
        }
    }

    private void setupSoundscapeListeners() {
        if (btnSoundNone != null) btnSoundNone.setOnClickListener(v -> selectSound(0));
        if (btnSoundLofi != null) btnSoundLofi.setOnClickListener(v -> selectSound(1));
        if (btnSoundWhite != null) btnSoundWhite.setOnClickListener(v -> selectSound(2));
        if (btnSoundBinaural != null) btnSoundBinaural.setOnClickListener(v -> selectSound(3));
    }

    private void selectSound(int soundId) {
        selectedSoundId = soundId;

        btnSoundNone.setAlpha(soundId == 0 ? 1.0f : 0.5f);
        btnSoundLofi.setAlpha(soundId == 1 ? 1.0f : 0.5f);
        btnSoundWhite.setAlpha(soundId == 2 ? 1.0f : 0.5f);
        btnSoundBinaural.setAlpha(soundId == 3 ? 1.0f : 0.5f);

        updateServiceSound();

        if (soundId != 0) {
            String name = soundId == 1 ? "Lofi Beats" : (soundId == 2 ? "Rain" : "Binaural Focus");
            Toast.makeText(getContext(), "Playing " + name + "... 🎧", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateServiceSound() {
        if (isBound && focusService != null) {
            Intent intent = new Intent(getContext(), FocusService.class);
            intent.setAction(FocusService.ACTION_UPDATE_SOUND);

            int resId = 0;
            if (selectedSoundId == 1) resId = getResources().getIdentifier("lofi_focus", "raw", getContext().getPackageName());
            else if (selectedSoundId == 2) resId = getResources().getIdentifier("rain_white_noise", "raw", getContext().getPackageName());
            else if (selectedSoundId == 3) resId = getResources().getIdentifier("binaural_beat_focus", "raw", getContext().getPackageName());

            intent.putExtra("soundResId", resId);
            getContext().startService(intent);
        }
    }

    private void activateSelfGhost() {
        isSelfGhostActive = true;
        opponentAvatar = "👻";
        opponentName = "Your Best";
        if (tvGhostAvatar != null) tvGhostAvatar.setText(opponentAvatar);
        if (llDuelStatusContainer != null) llDuelStatusContainer.setVisibility(View.VISIBLE);
        if (cpRivalGhost != null) cpRivalGhost.setVisibility(View.VISIBLE);
        if (flGhostIndicator != null) flGhostIndicator.setVisibility(View.VISIBLE);
        if (tvDuelStatusLive != null) tvDuelStatusLive.setText("VS SELF GHOST 👻");
        updateTimerUI();
    }

    private void deactivateSelfGhost() {
        isSelfGhostActive = false;
        if (!isDuelActive) {
            if (llDuelStatusContainer != null) llDuelStatusContainer.setVisibility(View.GONE);
            if (cpRivalGhost != null) cpRivalGhost.setVisibility(View.GONE);
            if (flGhostIndicator != null) flGhostIndicator.setVisibility(View.GONE);
        }
    }

    private void setupClickListeners() {
        btnPauseResume.setOnClickListener(v -> {
            applyClickEffect(v);
            if (isTimerRunning) {
                if (isBound) focusService.pauseTimer();
            } else {
                if (isBound) focusService.resumeTimer();
                else startFocusService();
            }
        });

        btnFinish.setOnClickListener(v -> {
            if (isFinishing) return;
            applyClickEffect(v);
            navigateToRewards();
        });

        btnBack.setOnClickListener(v -> {
            if (getActivity() != null) getActivity().getSupportFragmentManager().popBackStack();
        });

        if (ivZenInfo != null) ivZenInfo.setOnClickListener(v -> showZenInfoDialog());

        if (llDuelStatusContainer != null) {
            llDuelStatusContainer.setOnClickListener(v -> showDuelInfoDialog());
        }

        if (llBattleShout != null) {
            llBattleShout.setOnClickListener(v -> sendBattleShout());
        }
    }

    private void animateEntrance() {
        if (challengeTitle != null && cardChallenge != null) {
            cardChallenge.setVisibility(View.VISIBLE);
            tvChallengeTitle.setText(challengeTitle);
            cardChallenge.setAlpha(0);
            cardChallenge.setTranslationY(-50);
            cardChallenge.animate().alpha(1).translationY(0).setDuration(500).setInterpolator(new OvershootInterpolator()).start();
        }

        cpTimer.setAlpha(0);
        cpTimer.setScaleX(0.8f);
        cpTimer.setScaleY(0.8f);
        cpTimer.animate().alpha(1).scaleX(1).scaleY(1).setDuration(800).setInterpolator(new DecelerateInterpolator()).start();
    }

    private void fetchDuelProgress() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        activeDuelQuery = mDatabase.child("duels").orderByChild("status").equalTo("active");
        duelListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Duel duel = ds.getValue(Duel.class);
                    if (duel != null && skillName.equals(duel.getSkillName())) {
                        boolean isInitiator = uid.equals(duel.getInitiatorUid());
                        boolean isOpponent = uid.equals(duel.getOpponentUid());

                        if (isInitiator || isOpponent) {
                            isDuelActive = true;
                            isSelfGhostActive = false;
                            currentDuelData = duel;
                            opponentEffortInDuel = isInitiator ? duel.getOpponentEffort() : duel.getInitiatorEffort();
                            myStartEffortInDuel = isInitiator ? duel.getInitiatorEffort() : duel.getOpponentEffort();

                            wasTrailing = (myStartEffortInDuel + ((double)(totalTimeInMillis - timeLeftInMillis) / 10000.0)) <= opponentEffortInDuel;

                            opponentUid = isInitiator ? duel.getOpponentUid() : duel.getInitiatorUid();
                            loadOpponentData(opponentUid);
                            loadRivalryStats(uid, opponentUid);

                            if (llDuelStatusContainer != null) llDuelStatusContainer.setVisibility(View.VISIBLE);
                            if (cpRivalGhost != null) cpRivalGhost.setVisibility(View.VISIBLE);
                            if (flGhostIndicator != null) flGhostIndicator.setVisibility(View.VISIBLE);
                            if (llBattleShout != null) llBattleShout.setVisibility(View.VISIBLE);

                            updateTimerUI();
                            break;
                        }
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        };
        activeDuelQuery.addValueEventListener(duelListener);
    }

    private void loadOpponentData(String uid) {
        mDatabase.child("users").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    User user = snapshot.getValue(User.class);
                    if (user != null) {
                        opponentAvatar = user.getAvatarEmoji();
                        opponentName = user.getUsername();
                        if (tvGhostAvatar != null) tvGhostAvatar.setText(opponentAvatar);
                        updateTimerUI();
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadRivalryStats(String myUid, String oppUid) {
        mDatabase.child("users").child(myUid).child("rivalry").child(oppUid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    rivalryWins = snapshot.hasChild("wins") ? (long) snapshot.child("wins").getValue() : 0;
                    rivalryLosses = snapshot.hasChild("losses") ? (long) snapshot.child("losses").getValue() : 0;
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void sendBattleShout() {
        String myUid = FirebaseAuth.getInstance().getUid();
        if (myUid == null || opponentUid == null) return;

        applyClickEffect(llBattleShout);

        String[] shouts = {
                "📢 I'm catching up in our " + skillName + " duel! Better keep practicing! 🔥",
                "📢 Focus Mode: ON! See you at the top of the " + skillName + " leaderboard! 🚀",
                "📢 Just started a session for " + skillName + ". The gap is closing! ⚔️",
                "📢 Don't get too comfortable, I'm training hard! 💪"
        };
        String shoutMessage = shouts[new Random().nextInt(shouts.length)];

        DatabaseReference chatsRef = mDatabase.child("chats");
        String messageId = chatsRef.push().getKey();
        Message message = new Message(myUid, opponentUid, shoutMessage, System.currentTimeMillis());
        if (messageId != null) {
            chatsRef.child(messageId).setValue(message);
        }

        Map<String, Object> update = new HashMap<>();
        update.put("lastMessage", shoutMessage);
        update.put("lastMessageTimestamp", ServerValue.TIMESTAMP);

        mDatabase.child("users").child(myUid).child("recentChats").child(opponentUid).updateChildren(update);

        mDatabase.child("users").child(opponentUid).child("recentChats").child(myUid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int unread = 0;
                if (snapshot.exists() && snapshot.hasChild("unreadCount")) {
                    unread = snapshot.child("unreadCount").getValue(Integer.class);
                }
                Map<String, Object> receiverUpdate = new HashMap<>(update);
                receiverUpdate.put("unreadCount", unread + 1);
                mDatabase.child("users").child(opponentUid).child("recentChats").child(myUid).updateChildren(receiverUpdate);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });

        Toast.makeText(getContext(), "Battle Shout sent to " + opponentName + "!", Toast.LENGTH_SHORT).show();

        llBattleShout.setEnabled(false);
        llBattleShout.setAlpha(0.5f);
        new Handler().postDelayed(() -> {
            if (isAdded()) {
                llBattleShout.setEnabled(true);
                llBattleShout.setAlpha(1.0f);
            }
        }, 30000);
    }

    private void showZenInfoDialog() {
        if (getContext() == null) return;
        new MaterialAlertDialogBuilder(getContext(), R.style.MaterialAlertDialog_Rounded)
                .setTitle("🧘 Strict Zen Mode")
                .setMessage("When enabled, you must stay inside the app to finish your session.\n\n" +
                        "⚠️ If you leave for more than 10 seconds, the session is cancelled and progress is lost.\n\n" +
                        "🎧 FOCUS SOUNDSCAPES:\n" +
                        "Choose from Lofi, White Noise, or Binaural Beats to help you enter a deep flow state without leaving the app.\n\n" +
                        "🎁 REWARDS:\n" +
                        "• +50% XP Bonus per session\n" +
                        "• Faster Mastery Growth (100% Boost)\n" +
                        "• Special Badge in History\n" +
                        "• 👻 Compete with your Self Ghost!\n\n" +
                        "Stay focused!")
                .setPositiveButton("Got it", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void showDuelInfoDialog() {
        if (getContext() == null) return;

        if (isSelfGhostActive && pbSession != null) {
            new MaterialAlertDialogBuilder(getContext(), R.style.MaterialAlertDialog_Rounded)
                    .setTitle("👻 Personal Best Ghost")
                    .setMessage(String.format(Locale.getDefault(),
                            "You are competing against your own best record for %s!\n\n" +
                                    "Best Time: %d:%02d\n" +
                                    "Date: %s\n" +
                                    "Mode: %s\n\n" +
                                    "Keep pushing to beat your previous self!",
                            skillName, pbSeconds / 60, pbSeconds % 60,
                            new java.text.SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(new java.util.Date(pbSession.getTimestamp())),
                            pbSession.isZenMode() ? "Zen Mode 🧘" : "Normal Mode"))
                    .setPositiveButton("I can do it!", null)
                    .show();
            return;
        }

        double timePracticedMs = totalTimeInMillis - timeLeftInMillis;
        double mySessionEffort = timePracticedMs / 10000.0;
        double myTotal = myStartEffortInDuel + mySessionEffort;

        String timeRemaining = "Ending soon!";
        if (currentDuelData != null) {
            long millisLeft = currentDuelData.getEndTime() - System.currentTimeMillis();
            if (millisLeft > 0) {
                long days = TimeUnit.MILLISECONDS.toDays(millisLeft);
                long hours = TimeUnit.MILLISECONDS.toHours(millisLeft) % 24;
                timeRemaining = days > 0 ? days + "d " + hours + "h left" : hours + "h left";
            }
        }

        boolean isSurgeActive = opponentEffortInDuel > (myTotal * 1.2);
        String surgeText = isSurgeActive ? "\n\n⚡ **SURGE ACTIVE:** 1.25x Bonus applied to help you catch up!" : "";

        String message = String.format(Locale.getDefault(),
                "Live %s Duel vs %s %s\n⏳ %s\n\n" +
                        "🏆 Score:\n" +
                        "• You: %.1f pts (🔥 %d streak)\n" +
                        "• %s: %.1f pts\n\n" +
                        "⚔️ Rivalry History:\n" +
                        "Record: %d Wins - %d Losses\n" +
                        "%s\n\n" +
                        "The ghost avatar shows %s's live pace. Push harder to pass them!",
                skillName, opponentName, opponentAvatar, timeRemaining,
                myTotal, myCurrentStreak, opponentName, opponentEffortInDuel,
                rivalryWins, rivalryLosses, surgeText, opponentName);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getContext(), R.style.MaterialAlertDialog_Rounded)
                .setTitle("⚔️ Duel Insight")
                .setMessage(message)
                .setPositiveButton("Let's Win!", (dialog, which) -> dialog.dismiss());

        if (isDuelActive && llBattleShout != null && llBattleShout.isEnabled()) {
            builder.setNeutralButton("Battle Shout 📢", (dialog, which) -> sendBattleShout());
        }

        builder.show();
    }

    private void setupObservers() {
        FocusService.timeLeftLiveData.observe(getViewLifecycleOwner(), time -> {
            timeLeftInMillis = time;
            updateTimerUI();
            if (time > 0 && isTimerRunning) {
                tvCountdown.animate().scaleX(1.05f).scaleY(1.05f).setDuration(200).withEndAction(() -> tvCountdown.animate().scaleX(1f).scaleY(1f).setDuration(200).start()).start();
            }
        });

        FocusService.isTimerRunning.observe(getViewLifecycleOwner(), running -> {
            isTimerRunning = running;
            if (running) hasStartedSession = true; // Mark session as truly started
            
            if (btnPauseResume != null) btnPauseResume.setText(running ? "⏸ PAUSE" : "▶ RESUME");
            
            // Auto-finish only when timer reaches 0
            if (!running && timeLeftInMillis == 0 && !isFinishing && hasStartedSession) {
                if (tvCountdown != null) tvCountdown.animate().scaleX(1.2f).scaleY(1.2f).setDuration(500).setInterpolator(new OvershootInterpolator()).start();
                navigateToRewards();
            }
        });

        FocusService.isStrictModeTriggered.observe(getViewLifecycleOwner(), triggered -> {
            if (triggered && isAdded() && isResumed()) {
                handleStrictModeFailure();
            }
        });
    }

    private void handleStrictModeFailure() {
        if (getContext() != null) {
            Toast.makeText(getContext(), "Session Cancelled! You left focus mode.", Toast.LENGTH_LONG).show();
            stopFocusService();
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        }
    }

    private void startFocusService() {
        if (isAutoFinishMode) return; // Don't restart service if we are about to finish
        Context context = getContext();
        if (context != null) {
            Intent intent = new Intent(context, FocusService.class);
            intent.setAction("START");
            intent.putExtra("totalTime", totalTimeInMillis);
            intent.putExtra("skillName", skillName);
            context.startForegroundService(intent);
            context.bindService(intent, connection, Context.BIND_AUTO_CREATE);
        }
    }

    private void stopFocusService() {
        Context context = getContext();
        if (context != null) {
            Intent intent = new Intent(context, FocusService.class);
            intent.setAction("STOP");
            context.startService(intent);
            if (isBound) {
                context.unbindService(connection);
                isBound = false;
            }
        }
    }

    private void applyClickEffect(View v) {
        v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(100).start()).start();
    }

    private void loadUserGoalAndSubSkills() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            setupDefaultTimer();
            return;
        }

        mDatabase.child("users").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                User user = snapshot.getValue(User.class);
                if (user != null) {
                    myCurrentStreak = user.getStreak();
                    int goalMinutes = 5;
                    if ("speed_round".equals(challengeKey)) {
                        goalMinutes = 5;
                    } else {
                        Map<String, Integer> skillGoals = user.getSkillDailyGoals();
                        if (skillGoals != null && skillGoals.containsKey(skillName)) {
                            goalMinutes = skillGoals.get(skillName);
                        } else if (user.getDailyGoalMinutes() > 0) {
                            goalMinutes = user.getDailyGoalMinutes();
                        }
                    }
                    totalTimeInMillis = goalMinutes * 60 * 1000L;
                    if (tvGoalInfo != null) {
                        tvGoalInfo.setText("Goal: " + goalMinutes + " min");
                    }

                    List<String> subSkills = user.getSubSkills().get(skillName);
                    if (subSkills != null && !subSkills.isEmpty()) {
                        tilSubSkill.setVisibility(View.VISIBLE);
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, subSkills);
                        actvSubSkill.setAdapter(adapter);
                        actvSubSkill.setOnItemClickListener((parent, v, position, id) -> {
                            selectedSubSkill = (String) parent.getItemAtPosition(position);
                        });
                    } else {
                        tilSubSkill.setVisibility(View.GONE);
                    }

                    String skillKey = skillName.replace(".", "_");
                    pbSession = user.getPersonalBests().get(skillKey);
                    if (pbSession != null) {
                        pbSeconds = pbSession.getSeconds();
                        if (switchStrictMode.isChecked() && !isDuelActive) {
                            activateSelfGhost();
                        }
                    }

                    populateHeatmap(user.getSessions());
                }

                if (timeLeftInMillis == -1) {
                    timeLeftInMillis = totalTimeInMillis;
                }
                updateTimerUI();
                startFocusService();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                setupDefaultTimer();
            }
        });
    }

    private void populateHeatmap(Map<String, PracticeSession> sessions) {
        if (glHeatmap == null) return;
        glHeatmap.removeAllViews();

        int daysToShow = 14;
        long currentTime = System.currentTimeMillis();
        long oneDayMs = TimeUnit.DAYS.toMillis(1);

        Map<String, Integer> dailyMinutes = new HashMap<>();
        if (sessions != null) {
            for (PracticeSession session : sessions.values()) {
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(session.getTimestamp());
                String dayKey = cal.get(Calendar.YEAR) + "-" + cal.get(Calendar.DAY_OF_YEAR);
                int mins = dailyMinutes.getOrDefault(dayKey, 0);
                dailyMinutes.put(dayKey, mins + session.getMinutes());
            }
        }

        for (int i = daysToShow - 1; i >= 0; i--) {
            View cell = new View(getContext());
            long dayTimestamp = currentTime - (i * oneDayMs);
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(dayTimestamp);
            String dayKey = cal.get(Calendar.YEAR) + "-" + cal.get(Calendar.DAY_OF_YEAR);

            int minutes = dailyMinutes.getOrDefault(dayKey, 0);
            int color = Color.parseColor("#2D324A");

            if (minutes > 0 && minutes < 15) color = Color.parseColor("#1E4620");
            else if (minutes >= 15 && minutes < 45) color = Color.parseColor("#2E7D32");
            else if (minutes >= 45) color = Color.parseColor("#4CAF50");

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = dpToPx(12);
            params.height = dpToPx(12);
            params.setMargins(dpToPx(2), dpToPx(2), dpToPx(2), dpToPx(2));
            cell.setLayoutParams(params);

            GradientDrawable shape = new GradientDrawable();
            shape.setShape(GradientDrawable.RECTANGLE);
            shape.setCornerRadius(dpToPx(2));
            shape.setColor(color);
            cell.setBackground(shape);

            glHeatmap.addView(cell);

            cell.setAlpha(0);
            cell.animate().alpha(1).setDuration(300).setStartDelay((daysToShow - i) * 50L).start();
        }
    }

    private int dpToPx(int dp) {
        if (getContext() == null) return dp;
        return (int) (dp * getContext().getResources().getDisplayMetrics().density);
    }

    private void setupDefaultTimer() {
        if (timeLeftInMillis == -1) {
            timeLeftInMillis = totalTimeInMillis;
        }
        updateTimerUI();
        startFocusService();
    }

    private void navigateToRewards() {
        if (getActivity() != null && isAdded()) {
            long timePracticedMs = totalTimeInMillis - timeLeftInMillis;
            int totalSeconds = (int) (timePracticedMs / 1000);

            // Enforce 10s minimum for manual finish.
            if (totalSeconds < 10 && timeLeftInMillis > 0) { 
                btnFinish.setEnabled(true);
                btnFinish.setAlpha(1.0f);
                Toast.makeText(getContext(), "Practice for at least 10 seconds to earn stars!", Toast.LENGTH_SHORT).show();
                return;
            }

            isFinishing = true;
            btnFinish.setEnabled(false);
            btnFinish.setAlpha(0.5f);

            stopFocusService();

            String note = etPracticeNote != null ? etPracticeNote.getText().toString().trim() : "";
            boolean isZenMode = switchStrictMode != null && switchStrictMode.isChecked();

            RewardsFragment rewardsFragment = new RewardsFragment();
            Bundle args = new Bundle();
            args.putString("skill_name", skillName);
            args.putString("sub_skill_name", selectedSubSkill);
            args.putInt("seconds_practiced", totalSeconds);
            args.putString("practice_note", note);
            args.putBoolean("zen_mode", isZenMode);
            if (challengeKey != null) {
                args.putString("challenge_key", challengeKey);
            }
            rewardsFragment.setArguments(args);

            getActivity().getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(
                            R.anim.step_forward_enter,
                            R.anim.step_forward_exit,
                            R.anim.step_backward_enter,
                            R.anim.step_backward_exit
                    )
                    .replace(R.id.fragment_container, rewardsFragment)
                    .commitAllowingStateLoss();
        }
    }

    private void updateTimerUI() {
        int totalSeconds = (int) (timeLeftInMillis / 1000);
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;

        if (tvCountdown != null) tvCountdown.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));

        long timePracticedMs = totalTimeInMillis - timeLeftInMillis;
        int progress = (int) (timePracticedMs * 100 / Math.max(1, totalTimeInMillis));

        if (cpTimer != null) {
            ObjectAnimator anim = ObjectAnimator.ofInt(cpTimer, "progress", cpTimer.getProgress(), progress);
            anim.setDuration(1000);
            anim.setInterpolator(new AccelerateDecelerateInterpolator());
            anim.start();
        }

        if (tvPercentage != null) tvPercentage.setText(progress + "%");

        if (isDuelActive) {
            updateGhostPace(timePracticedMs);
        } else if (isSelfGhostActive) {
            updateSelfGhostPace(timePracticedMs);
        }
    }

    private void updateGhostPace(long timePracticedMs) {
        double mySessionEffort = (double) timePracticedMs / 10000.0;
        double myTotalInDuel = myStartEffortInDuel + mySessionEffort;
        double sessionGoalPoints = (double) totalTimeInMillis / 10000.0;

        int ghostProgress = (int) (opponentEffortInDuel * 100 / Math.max(1, sessionGoalPoints + myStartEffortInDuel));
        ghostProgress = Math.min(100, ghostProgress);

        if (cpRivalGhost != null) {
            cpRivalGhost.setProgress(ghostProgress);
        }

        if (flGhostIndicator != null) {
            float rotation = (ghostProgress / 100f) * 360f;
            flGhostIndicator.animate().rotation(rotation).setDuration(1000).setInterpolator(new AccelerateDecelerateInterpolator()).start();
            if (tvGhostAvatar != null) tvGhostAvatar.setRotation(-rotation);
        }

        if (tvDuelStatusLive != null) {
            boolean isLeading = myTotalInDuel > opponentEffortInDuel;

            if (wasTrailing && isLeading) {
                triggerOvertakeAnimation();
                wasTrailing = false;
            } else if (!isLeading) {
                wasTrailing = true;
            }

            if (isLeading) {
                if (!tvDuelStatusLive.getText().toString().contains("OVERTAKEN")) {
                    tvDuelStatusLive.setText("LEADING " + opponentName.toUpperCase() + " 📈");
                }
                tvDuelStatusLive.setTextColor(getResources().getColor(R.color.splash_green));
            } else {
                tvDuelStatusLive.setText("TRAILING " + opponentName.toUpperCase() + " 📉");
                tvDuelStatusLive.setTextColor(getResources().getColor(android.R.color.holo_red_light));
            }
        }
    }

    private void updateSelfGhostPace(long timePracticedMs) {
        int mySeconds = (int) (timePracticedMs / 1000);
        int ghostProgress = (int) (mySeconds * 100 / Math.max(1, pbSeconds));
        ghostProgress = Math.min(100, ghostProgress);

        if (cpRivalGhost != null) {
            cpRivalGhost.setProgress(ghostProgress);
        }

        if (flGhostIndicator != null) {
            float rotation = (ghostProgress / 100f) * 360f;
            flGhostIndicator.animate().rotation(rotation).setDuration(1000).setInterpolator(new AccelerateDecelerateInterpolator()).start();
            if (tvGhostAvatar != null) tvGhostAvatar.setRotation(-rotation);
        }

        if (tvDuelStatusLive != null) {
            boolean isZenNow = switchStrictMode != null && switchStrictMode.isChecked();
            boolean wasZenThen = pbSession != null && pbSession.isZenMode();

            if (isZenNow && !wasZenThen) {
                tvDuelStatusLive.setText("BEST PACE! (+50% XP) 📈");
                tvDuelStatusLive.setTextColor(getResources().getColor(R.color.splash_green));
                if (wasTrailing) { triggerOvertakeAnimation(); wasTrailing = false; }
            } else if (!isZenNow && wasZenThen) {
                tvDuelStatusLive.setText("LAGGING BEHIND BEST 📉");
                tvDuelStatusLive.setTextColor(getResources().getColor(android.R.color.holo_red_light));
                wasTrailing = true;
            } else {
                boolean isBeatingPB = mySeconds > pbSeconds;
                if (isBeatingPB) {
                    tvDuelStatusLive.setText("NEW RECORD RANGE! 🔥");
                    tvDuelStatusLive.setTextColor(getResources().getColor(R.color.splash_green));
                    if (wasTrailing) { triggerOvertakeAnimation(); wasTrailing = false; }
                } else {
                    tvDuelStatusLive.setText("MATCHING BEST PACE ⚔️");
                    tvDuelStatusLive.setTextColor(Color.WHITE);
                    wasTrailing = true;
                }
            }
        }
    }

    private void triggerOvertakeAnimation() {
        if (tvDuelStatusLive == null) return;

        String msg = isSelfGhostActive ? "🔥 NEW BEST PACE! 🔥" : "🔥 OVERTAKEN " + opponentName.toUpperCase() + "! 🔥";
        tvDuelStatusLive.setText(msg);
        tvDuelStatusLive.animate()
                .scaleX(1.4f)
                .scaleY(1.4f)
                .setDuration(400)
                .setInterpolator(new OvershootInterpolator())
                .withEndAction(() -> {
                    tvDuelStatusLive.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(300)
                            .setStartDelay(1500)
                            .withEndAction(() -> {
                                if (!isAdded()) return;
                                if (isSelfGhostActive) {
                                    updateSelfGhostPace(totalTimeInMillis - timeLeftInMillis);
                                } else if (myStartEffortInDuel + ((double)(totalTimeInMillis - timeLeftInMillis) / 10000.0) > opponentEffortInDuel) {
                                    tvDuelStatusLive.setText("LEADING " + opponentName.toUpperCase() + " 📈");
                                }
                            })
                            .start();
                }).start();

        if (getContext() != null) {
            String toastMsg = isSelfGhostActive ? "You're at your best efficiency! 🏆" : "You just passed " + opponentName + "! 🏆";
            Toast.makeText(getContext(), toastMsg, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isBound && focusService != null) {
            focusService.enterForeground();
        } else if (isTimerRunning) {
            Intent intent = new Intent(getContext(), FocusService.class);
            if (getContext() != null) getContext().bindService(intent, connection, Context.BIND_AUTO_CREATE);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (isBound && focusService != null && isTimerRunning) {
            boolean isStrict = switchStrictMode != null && switchStrictMode.isChecked();
            focusService.enterBackground(isStrict);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (isBound && getContext() != null) {
            getContext().unbindService(connection);
            isBound = false;
        }
        if (activeDuelQuery != null && duelListener != null) {
            activeDuelQuery.removeEventListener(duelListener);
        }
    }
}
