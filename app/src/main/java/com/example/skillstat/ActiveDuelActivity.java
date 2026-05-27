package com.example.skillstat;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.Gravity;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AnticipateOvershootInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.skillstat.models.Duel;
import com.example.skillstat.models.PracticeSession;
import com.example.skillstat.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ActiveDuelActivity extends AppCompatActivity {

    private ImageView ivMyAvatar, ivOpponentAvatar, ivDuelStatusIcon, ivStatusSmallIcon;
    private TextView tvMyName, tvMyGain, tvMyMasteryPercent, tvMyMultiplier;
    private TextView tvOpponentName, tvOpponentGain, tvOpponentMultiplier, tvOpponentLabelSmall, tvOpponentMasteryPercent;
    private TextView tvDuelInfoBadge, tvDuelStatusText, tvRivalryStats, tvPrizePool, tvDuelCountdown;
    private View vMyMasteryFill, vOpponentMasteryFill, vMyMasteryBg, vOpponentMasteryBg, vProgressCard;
    private LinearLayout llActivityFeed;
    private TextView btnPractice;

    private DatabaseReference mDatabase;
    private String currentUid;
    private String duelId;
    private Duel currentDuel;
    private boolean isDuelFinished = false;
    private boolean resultDialogShown = false;

    private double myEffortPoints = 0.0;
    private double opponentEffortPoints = 0.0;
    private double mySkillMastery = 0.0;
    private double opponentSkillMastery = 0.0;
    private String opponentDisplayName = "Opponent";
    private String opponentUid;
    private String lastOpponentUidLoaded = null;

    private ValueEventListener duelListener;
    private ValueEventListener rivalryListener;
    private ValueEventListener feedListener;
    private Map<View, ValueAnimator> activeAnimators = new HashMap<>();
    private Vibrator vibrator;

    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            updateCountdown();
            timerHandler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_active_duel);

        mDatabase = FirebaseDatabase.getInstance().getReference();
        currentUid = FirebaseAuth.getInstance().getUid();
        duelId = getIntent().getStringExtra("duel_id");
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        if (duelId == null) {
            Toast.makeText(this, "Duel ID missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        loadDuelData();
        animateEntrance();
        loadUserData(currentUid, true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        timerHandler.post(timerRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        timerHandler.removeCallbacks(timerRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mDatabase != null) {
            if (duelListener != null) mDatabase.child("duels").child(duelId).removeEventListener(duelListener);
            if (rivalryListener != null && opponentUid != null) mDatabase.child("users").child(currentUid).child("rivalry").child(lastOpponentUidLoaded != null ? lastOpponentUidLoaded : opponentUid).removeEventListener(rivalryListener);
            if (feedListener != null) mDatabase.child("duels").child(duelId).child("sessions").removeEventListener(feedListener);
        }
        for (ValueAnimator animator : activeAnimators.values()) {
            animator.cancel();
        }
        activeAnimators.clear();
    }

    private void animateEntrance() {
        View header = findViewById(R.id.rl_active_header);
        if (header != null) {
            header.setAlpha(0);
            header.setTranslationY(-50);
            header.animate().alpha(1).translationY(0).setDuration(500).setInterpolator(new DecelerateInterpolator()).start();
        }

        if (vProgressCard != null) {
            vProgressCard.setAlpha(0);
            vProgressCard.setScaleX(0.9f);
            vProgressCard.animate().alpha(1).scaleX(1.0f).setDuration(600).setStartDelay(200).setInterpolator(new AnticipateOvershootInterpolator()).start();
        }
    }

    private void initViews() {
        ivMyAvatar = findViewById(R.id.iv_my_avatar);
        tvMyName = findViewById(R.id.tv_my_name);
        tvMyGain = findViewById(R.id.tv_my_gain);
        tvMyMultiplier = findViewById(R.id.tv_my_multiplier);
        tvMyMasteryPercent = findViewById(R.id.tv_my_mastery_percent);
        vMyMasteryFill = findViewById(R.id.v_my_mastery_fill);
        vMyMasteryBg = findViewById(R.id.v_my_mastery_bg);

        ivOpponentAvatar = findViewById(R.id.iv_opponent_avatar);
        tvOpponentName = findViewById(R.id.tv_opponent_name);
        tvOpponentGain = findViewById(R.id.tv_opponent_gain);
        tvOpponentMultiplier = findViewById(R.id.tv_opponent_multiplier);
        tvOpponentMasteryPercent = findViewById(R.id.tv_opponent_mastery_percent);
        tvOpponentLabelSmall = findViewById(R.id.tv_opponent_label_small);
        vOpponentMasteryFill = findViewById(R.id.v_opponent_mastery_fill);
        vOpponentMasteryBg = findViewById(R.id.v_opponent_mastery_bg);

        vProgressCard = findViewById(R.id.v_progress_card);
        tvRivalryStats = findViewById(R.id.tv_rivalry_stats);
        llActivityFeed = findViewById(R.id.ll_activity_feed);
        tvPrizePool = findViewById(R.id.tv_prize_pool);
        tvDuelCountdown = findViewById(R.id.tv_duel_countdown);

        tvDuelInfoBadge = findViewById(R.id.tv_duel_info_badge);
        tvDuelStatusText = findViewById(R.id.tv_duel_status_text);
        ivDuelStatusIcon = findViewById(R.id.iv_duel_status_icon);
        ivStatusSmallIcon = findViewById(R.id.iv_status_small_icon);
        btnPractice = findViewById(R.id.btn_practice_now_duel);

        findViewById(R.id.btn_back_active).setOnClickListener(v -> finish());
        findViewById(R.id.btn_back_to_friends).setOnClickListener(v -> finish());
        findViewById(R.id.btn_duel_info).setOnClickListener(v -> showDuelRules());

        btnPractice.setOnClickListener(v -> {
            triggerVibration(20);
            v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).withEndAction(() -> {
                v.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
                
                if (currentDuel != null && !isDuelFinished) {
                    String skillToPractice = currentDuel.getSkillName();
                    
                    // Siguraduhin na malinis ang skill name (walang extra spaces)
                    if (skillToPractice != null) {
                        skillToPractice = skillToPractice.trim();
                        // Alisin ang " Duel" suffix kung meron
                        if (skillToPractice.endsWith(" Duel")) {
                            skillToPractice = skillToPractice.substring(0, skillToPractice.length() - 5).trim();
                        }
                    }

                    Intent intent = new Intent(this, MainActivity.class);
                    intent.putExtra("navigate_to", "practice");
                    intent.putExtra("skill_name", skillToPractice);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(ActiveDuelActivity.this, "Cannot start practice. Duel is finished or inactive.", Toast.LENGTH_SHORT).show();
                }
            }).start();
        });

        setupTauntButton(R.id.btn_taunt_1, "🏃💨", "Catch me if you can!");
        setupTauntButton(R.id.btn_taunt_2, "🐢", "Too slow!");
        setupTauntButton(R.id.btn_taunt_3, "🔥", "I'm on fire!");
        setupTauntButton(R.id.btn_taunt_4, "🙌", "Go for it!");
    }

    private void triggerVibration(int ms) {
        if (vibrator == null || !vibrator.hasVibrator()) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            vibrator.vibrate(ms);
        }
    }

    private void showDuelRules() {
        new AlertDialog.Builder(this, R.style.CustomAlertDialog)
                .setTitle("Duel Rules")
                .setMessage("1. Competition based on Effort Points.\n" +
                        "2. Effort = Seconds practiced / 10.\n" +
                        "3. Bonus multipliers (1.25x) apply if you are trailing behind.\n" +
                        "4. Highest effort wins at the end.\n" +
                        "5. Winner takes the prize pool!")
                .setPositiveButton("Got it", null)
                .show();
    }

    private void setupTauntButton(int resId, String emoji, String message) {
        View v = findViewById(resId);
        if (v != null) {
            v.setOnClickListener(view -> {
                triggerVibration(30);
                view.animate().scaleX(1.2f).scaleY(1.2f).setDuration(150).withEndAction(() -> view.animate().scaleX(1f).scaleY(1f).setDuration(150).start()).start();
                sendTaunt(emoji, message);
            });
        }
    }

    private void sendTaunt(String emoji, String message) {
        if (currentDuel == null || currentUid == null || opponentUid == null) return;
        mDatabase.child("users").child(currentUid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isFinishing()) return;
                User me = snapshot.getValue(User.class);
                if (me != null) {
                    Map<String, Object> notif = new HashMap<>();
                    notif.put("type", "nudge");
                    notif.put("message", me.getUsername() + " says: " + message + " " + emoji);
                    notif.put("timestamp", System.currentTimeMillis());
                    mDatabase.child("users").child(opponentUid).child("notifications").push().setValue(notif);
                    Toast.makeText(ActiveDuelActivity.this, "Taunt sent!", Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadDuelData() {
        duelListener = mDatabase.child("duels").child(duelId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isFinishing()) return;
                currentDuel = snapshot.getValue(Duel.class);
                if (currentDuel != null) {
                    currentDuel.setDuelId(snapshot.getKey());
                    opponentUid = currentUid.equals(currentDuel.getInitiatorUid()) ? currentDuel.getOpponentUid() : currentDuel.getInitiatorUid();

                    checkDuelExpiry();
                    updateUI();
                    applySkillTheme(currentDuel.getSkillName());

                    if (lastOpponentUidLoaded == null || !lastOpponentUidLoaded.equals(opponentUid)) {
                        loadUserData(opponentUid, false);
                        loadRivalryStats();
                        lastOpponentUidLoaded = opponentUid;
                    }

                    if (feedListener == null) {
                        loadActivityFeed();
                    }

                    if ("completed".equals(currentDuel.getStatus()) && !resultDialogShown) {
                        checkIfClaimedAndShow();
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void checkIfClaimedAndShow() {
        mDatabase.child("users").child(currentUid).child("completedDuels").child(duelId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isFinishing()) return;
                if (snapshot.exists()) {
                    Object val = snapshot.getValue();
                    if ("claimed".equals(val)) return;
                    showResultDialog();
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void checkDuelExpiry() {
        if (currentDuel != null && "active".equals(currentDuel.getStatus())) {
            if (System.currentTimeMillis() > currentDuel.getEndTime()) {
                settleDuelAtomic();
            }
        }
    }

    private void updateCountdown() {
        if (currentDuel == null || "completed".equals(currentDuel.getStatus())) {
            if (tvDuelCountdown != null) tvDuelCountdown.setText("Duel Ended");
            return;
        }

        long timeLeft = currentDuel.getEndTime() - System.currentTimeMillis();
        if (timeLeft <= 0) {
            tvDuelCountdown.setText("Settling...");
            checkDuelExpiry();
        } else {
            String timeStr = String.format(Locale.US, "Time Left: %02d:%02d:%02d",
                    TimeUnit.MILLISECONDS.toHours(timeLeft),
                    TimeUnit.MILLISECONDS.toMinutes(timeLeft) % 60,
                    TimeUnit.MILLISECONDS.toSeconds(timeLeft) % 60);
            tvDuelCountdown.setText(timeStr);
        }
    }

    private void settleDuelAtomic() {
        mDatabase.child("duels").child(duelId).runTransaction(new Transaction.Handler() {
            @NonNull @Override public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                Duel duel = currentData.getValue(Duel.class);
                if (duel != null && "active".equals(duel.getStatus())) {
                    if (System.currentTimeMillis() < duel.getEndTime()) return Transaction.abort();

                    duel.setStatus("completed");
                    double initEff = duel.getInitiatorEffort();
                    double oppEff = duel.getOpponentEffort();
                    boolean draw = initEff == oppEff;
                    duel.setWinnerUid(draw ? "draw" : (initEff > oppEff ? duel.getInitiatorUid() : duel.getOpponentUid()));

                    currentData.setValue(duel);
                    return Transaction.success(currentData);
                }
                return Transaction.abort();
            }

            @Override public void onComplete(DatabaseError error, boolean committed, DataSnapshot currentData) {
                if (committed && currentData.exists()) {
                    Duel finalized = currentData.getValue(Duel.class);
                    if (finalized != null) applyFinalRewards(finalized);
                }
            }
        });
    }

    private void applyFinalRewards(Duel duel) {
        String winner = duel.getWinnerUid();
        String initiator = duel.getInitiatorUid();
        String opponent = duel.getOpponentUid();
        int pool = 500 + (duel.getWagerAmount() * 2);
        int drawPool = 250 + duel.getWagerAmount();

        Map<String, Object> updates = new HashMap<>();
        updates.put("users/" + initiator + "/activeDuels/" + duelId, null);
        updates.put("users/" + initiator + "/completedDuels/" + duelId, true);
        updates.put("users/" + opponent + "/activeDuels/" + duelId, null);
        updates.put("users/" + opponent + "/completedDuels/" + duelId, true);

        if ("draw".equals(winner)) {
            updates.put("users/" + initiator + "/totalPoints", ServerValue.increment(drawPool));
            updates.put("users/" + opponent + "/totalPoints", ServerValue.increment(drawPool));
        } else {
            String loser = winner.equals(initiator) ? opponent : initiator;
            updates.put("users/" + winner + "/totalPoints", ServerValue.increment(pool));
            updates.put("users/" + winner + "/wins", ServerValue.increment(1));
            updates.put("users/" + winner + "/rivalry/" + loser + "/wins", ServerValue.increment(1));
            updates.put("users/" + loser + "/losses", ServerValue.increment(1));
            updates.put("users/" + loser + "/rivalry/" + winner + "/losses", ServerValue.increment(1));
        }
        mDatabase.updateChildren(updates);
    }

    private void loadRivalryStats() {
        if (opponentUid == null) return;
        if (rivalryListener != null) {
            mDatabase.child("users").child(currentUid).child("rivalry").child(lastOpponentUidLoaded != null ? lastOpponentUidLoaded : opponentUid).removeEventListener(rivalryListener);
        }
        rivalryListener = mDatabase.child("users").child(currentUid).child("rivalry").child(opponentUid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isFinishing()) return;
                if (snapshot.exists()) {
                    long w = snapshot.hasChild("wins") ? (long)snapshot.child("wins").getValue() : 0;
                    long l = snapshot.hasChild("losses") ? (long)snapshot.child("losses").getValue() : 0;
                    tvRivalryStats.setText("Record vs " + opponentDisplayName + ": " + w + "W - " + l + "L");
                } else {
                    tvRivalryStats.setText("First duel with " + opponentDisplayName);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadActivityFeed() {
        feedListener = mDatabase.child("duels").child(duelId).child("sessions").limitToLast(5).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isFinishing()) return;
                llActivityFeed.removeAllViews();
                if (!snapshot.exists() || snapshot.getChildrenCount() == 0) {
                    TextView empty = new TextView(ActiveDuelActivity.this);
                    empty.setText("No activity yet. Be the first to practice!");
                    empty.setTextColor(Color.parseColor("#8E8E93"));
                    empty.setTextSize(13);
                    empty.setPadding(0, 20, 0, 20);
                    empty.setGravity(Gravity.CENTER);
                    llActivityFeed.addView(empty);
                    return;
                }
                List<PracticeSession> sessions = new ArrayList<>();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    PracticeSession s = ds.getValue(PracticeSession.class);
                    if (s != null) sessions.add(s);
                }
                Collections.reverse(sessions);
                for (PracticeSession s : sessions) addFeedItem(s);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void addFeedItem(PracticeSession session) {
        View view = getLayoutInflater().inflate(R.layout.item_duel_activity, llActivityFeed, false);
        TextView tvUser = view.findViewById(R.id.tv_feed_user);
        TextView tvAction = view.findViewById(R.id.tv_feed_action);
        TextView tvPoints = view.findViewById(R.id.tv_feed_points);

        boolean isMe = session.getUserId() != null && session.getUserId().equals(currentUid);
        tvUser.setText(isMe ? "You" : opponentDisplayName);
        tvUser.setTextColor(isMe ? Color.parseColor("#4CAF50") : Color.parseColor("#FF4B4B"));
        tvAction.setText("practiced " + session.getSkillName());
        tvPoints.setText("+" + String.format(Locale.US, "%.1f", session.getEffortPoints()));
        llActivityFeed.addView(view);
    }

    private void updateUI() {
        if (currentDuel == null) return;
        boolean isInit = currentUid.equals(currentDuel.getInitiatorUid());

        double oldMyPoints = myEffortPoints;
        myEffortPoints = isInit ? currentDuel.getInitiatorEffort() : currentDuel.getOpponentEffort();
        opponentEffortPoints = isInit ? currentDuel.getOpponentEffort() : currentDuel.getInitiatorEffort();

        tvMyGain.setText(String.format(Locale.US, "%.1f", myEffortPoints));
        tvOpponentGain.setText(String.format(Locale.US, "%.1f", opponentEffortPoints));

        boolean myTrailing = opponentEffortPoints > (myEffortPoints * 1.2) && opponentEffortPoints > 5;
        tvMyMultiplier.setText(myTrailing ? "1.25x Bonus" : "1.0x");
        tvMyMultiplier.setTextColor(myTrailing ? Color.parseColor("#FFD700") : Color.GRAY);

        boolean oppTrailing = myEffortPoints > (opponentEffortPoints * 1.2) && myEffortPoints > 5;
        tvOpponentMultiplier.setText(oppTrailing ? "1.25x Bonus" : "1.0x");
        tvOpponentMultiplier.setTextColor(oppTrailing ? Color.parseColor("#FFD700") : Color.GRAY);

        if ("completed".equals(currentDuel.getStatus())) {
            isDuelFinished = true;
            btnPractice.setEnabled(false);
            btnPractice.setAlpha(0.5f);
            btnPractice.setText("Duel Ended");
            tvDuelStatusText.setText(myEffortPoints > opponentEffortPoints ? "VICTORY" : (opponentEffortPoints > myEffortPoints ? "DEFEAT" : "DRAW"));
        } else {
            updateLiveStatus();
            if (myEffortPoints > oldMyPoints) {
                triggerVibration(40);
                ivDuelStatusIcon.animate().scaleX(1.3f).scaleY(1.3f).setDuration(200).withEndAction(() -> ivDuelStatusIcon.animate().scaleX(1f).scaleY(1f).setDuration(200).start()).start();
            }
        }
        updateMasteryBars();
        if (tvPrizePool != null) tvPrizePool.setText("Prize: " + (500 + (currentDuel.getWagerAmount() * 2)));
    }

    private void updateLiveStatus() {
        if (myEffortPoints == 0 && opponentEffortPoints == 0) {
            tvDuelStatusText.setText("READY TO START?");
            return;
        }
        if (myEffortPoints > opponentEffortPoints) {
            tvDuelStatusText.setText("LEADING");
        } else if (opponentEffortPoints > myEffortPoints) {
            tvDuelStatusText.setText("TRAILING");
        } else {
            tvDuelStatusText.setText("NECK AND NECK");
        }
    }

    private void updateMasteryBars() {
        // Now using actual skill mastery for these bars
        tvMyMasteryPercent.setText(String.format(Locale.US, "%.0f%%", mySkillMastery));
        tvOpponentMasteryPercent.setText(String.format(Locale.US, "%.0f%%", opponentSkillMastery));
        animateBar(vMyMasteryFill, vMyMasteryBg, (float)mySkillMastery);
        animateBar(vOpponentMasteryFill, vOpponentMasteryBg, (float)opponentSkillMastery);
    }

    private void animateBar(View fill, View bg, float pct) {
        if (activeAnimators.containsKey(fill)) {
            activeAnimators.get(fill).cancel();
        }

        bg.post(() -> {
            if (isFinishing()) return;
            int bgWidth = bg.getWidth();
            if (bgWidth == 0) return;
            int targetWidth = (int)(bgWidth * (pct / 100));
            ValueAnimator a = ValueAnimator.ofInt(Math.max(0, fill.getLayoutParams().width), targetWidth);
            a.addUpdateListener(animation -> {
                fill.getLayoutParams().width = (int)animation.getAnimatedValue();
                fill.requestLayout();
            });
            a.setDuration(1000).setInterpolator(new AccelerateDecelerateInterpolator());
            activeAnimators.put(fill, a);
            a.start();
        });
    }

    private void loadUserData(String uid, boolean isMe) {
        if (uid == null) return;
        mDatabase.child("users").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot s) {
                if (isFinishing()) return;
                User u = s.getValue(User.class);
                if (u != null) {
                    int resId = getResources().getIdentifier(u.getAvatarUrl(), "drawable", getPackageName());
                    String skillKey = currentDuel != null ? currentDuel.getSkillName().replace(".", "_") : null;

                    if (isMe) {
                        ivMyAvatar.setImageResource(resId != 0 ? resId : R.drawable.prof1);
                        tvMyName.setText("You");
                        if (skillKey != null) mySkillMastery = u.getSkillMastery().getOrDefault(skillKey, 0.0);
                    } else {
                        opponentDisplayName = u.getUsername();
                        ivOpponentAvatar.setImageResource(resId != 0 ? resId : R.drawable.prof2);
                        tvOpponentName.setText(opponentDisplayName);
                        tvOpponentLabelSmall.setText(opponentDisplayName);
                        if (skillKey != null) opponentSkillMastery = u.getSkillMastery().getOrDefault(skillKey, 0.0);
                        loadRivalryStats();
                    }
                    updateMasteryBars();
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError e) {}
        });
    }

    private void applySkillTheme(String s) {
        tvDuelInfoBadge.setText((s != null ? s : "Skill") + " Duel");
    }

    private void showResultDialog() {
        if (isFinishing() || isDestroyed()) return;
        resultDialogShown = true;
        View v = getLayoutInflater().inflate(R.layout.dialog_duel_result, null);
        AlertDialog d = new AlertDialog.Builder(this).setView(v).setCancelable(false).create();
        if (d.getWindow() != null) d.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        boolean win = myEffortPoints > opponentEffortPoints;
        boolean draw = myEffortPoints == opponentEffortPoints;

        ((TextView)v.findViewById(R.id.tv_result_icon)).setText(win ? "🏆" : (draw ? "🤝" : "💀"));
        ((TextView)v.findViewById(R.id.tv_result_title)).setText(win ? "VICTORY!" : (draw ? "DRAW" : "DEFEAT"));
        ((TextView)v.findViewById(R.id.tv_result_message)).setText(win ? "You dominated the duel!" : (draw ? "A worthy match!" : opponentDisplayName + " was more focused."));
        ((TextView)v.findViewById(R.id.tv_reward_amount)).setText(win ? "+" + (500 + currentDuel.getWagerAmount() * 2) : (draw ? "+" + (250 + currentDuel.getWagerAmount()) : "0"));

        if (win) triggerVibration(100);

        v.findViewById(R.id.btn_share_result).setOnClickListener(view -> {
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("text/plain");
            String msg = win ? "I just won a " + currentDuel.getSkillName() + " duel on SkillStat! 🏆" : "Had an epic duel on SkillStat!";
            share.putExtra(Intent.EXTRA_TEXT, msg);
            startActivity(Intent.createChooser(share, "Share Result"));
        });

        v.findViewById(R.id.btn_claim).setOnClickListener(view -> {
            mDatabase.child("users").child(currentUid).child("completedDuels").child(duelId).setValue("claimed");
            d.dismiss();
        });
        d.show();
    }
}
