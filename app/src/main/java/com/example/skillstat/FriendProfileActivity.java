package com.example.skillstat;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.skillstat.models.User;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class FriendProfileActivity extends AppCompatActivity {

    private TextView tvProfileTitle, tvName, tvEmail, tvPoints, tvStreak, tvOnlineStatus, tvLevel, tvDuels;
    private TextView tvRankBadge, tvBestStreak, tvTotalSessions, tvWinRate;
    private ImageView ivAvatar;
    private View vAvatarDot, vStatusDot;
    private LinearLayout llSkillsList;
    private DatabaseReference mDatabase;
    private String friendUid;
    private User friendUser;
    private ValueEventListener friendDataListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friend_profile);

        // Handle Window Insets for the header to avoid notch/status bar cutting
        View header = findViewById(R.id.rl_profile_header);
        if (header != null) {
            ViewCompat.setOnApplyWindowInsetsListener(header, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                float density = getResources().getDisplayMetrics().density;
                // Add system bar top inset to the base padding
                int paddingTop = (int) (20 * density) + systemBars.top;
                v.setPadding(v.getPaddingLeft(), paddingTop, v.getPaddingRight(), v.getPaddingBottom());
                return insets;
            });
        }

        mDatabase = FirebaseDatabase.getInstance().getReference();
        friendUid = getIntent().getStringExtra("friend_uid");

        if (friendUid == null) {
            Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        tvProfileTitle = findViewById(R.id.tv_profile_title);
        tvName = findViewById(R.id.tv_friend_name);
        tvEmail = findViewById(R.id.tv_friend_handle);
        ivAvatar = findViewById(R.id.iv_friend_avatar);
        tvPoints = findViewById(R.id.tv_friend_xp);
        tvStreak = findViewById(R.id.tv_friend_streak);
        tvLevel = findViewById(R.id.tv_friend_level);
        tvDuels = findViewById(R.id.tv_friend_duels);
        tvOnlineStatus = findViewById(R.id.tv_friend_online_status_text);
        vAvatarDot = findViewById(R.id.v_friend_profile_avatar_dot);
        vStatusDot = findViewById(R.id.v_friend_online_status_dot);
        llSkillsList = findViewById(R.id.ll_friend_skills_list);

        tvRankBadge = findViewById(R.id.tv_friend_rank_badge);
        tvBestStreak = findViewById(R.id.tv_friend_best_streak);
        tvTotalSessions = findViewById(R.id.tv_friend_total_sessions);
        tvWinRate = findViewById(R.id.tv_friend_win_rate);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        findViewById(R.id.btn_challenge_friend).setOnClickListener(v -> {
            Intent intent = new Intent(this, StartDuelActivity.class);
            intent.putExtra("opponent_uid", friendUid);
            startActivity(intent);
        });

        findViewById(R.id.btn_message_friend).setOnClickListener(v -> {
            if (friendUser != null) {
                Intent intent = new Intent(this, ChatActivity.class);
                intent.putExtra("receiver_uid", friendUid);
                intent.putExtra("receiver_name", friendUser.getUsername());
                intent.putExtra("receiver_avatar", friendUser.getAvatarUrl());
                startActivity(intent);
            }
        });

        View btnNudge = findViewById(R.id.btn_profile_nudge);
        if (btnNudge != null) {
            btnNudge.setOnClickListener(v -> {
                String name = tvName.getText().toString();
                Toast.makeText(this, "Nudged " + name + "! 👋", Toast.LENGTH_SHORT).show();
                btnNudge.setEnabled(false);
                btnNudge.setAlpha(0.5f);
            });
        }

        // Long click on duels to reset (for testing/admin)
        if (tvDuels != null) {
            tvDuels.setOnLongClickListener(v -> {
                resetDuelsRecord();
                return true;
            });
        }

        loadFriendData();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mDatabase != null && friendDataListener != null && friendUid != null) {
            mDatabase.child("users").child(friendUid).removeEventListener(friendDataListener);
        }
    }

    private void resetDuelsRecord() {
        mDatabase.child("users").child(friendUid).child("wins").setValue(0);
        mDatabase.child("users").child(friendUid).child("losses").setValue(0)
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Duels record reset!", Toast.LENGTH_SHORT).show());
    }

    private void loadFriendData() {
        friendDataListener = mDatabase.child("users").child(friendUid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                friendUser = snapshot.getValue(User.class);
                if (friendUser != null) {
                    if (tvProfileTitle != null) {
                        tvProfileTitle.setText(String.format("%s's profile", friendUser.getUsername()));
                    }
                    tvName.setText(friendUser.getUsername());
                    tvEmail.setText(String.format("@%s", friendUser.getUsername().toLowerCase()));

                    if (ivAvatar != null) {
                        ivAvatar.setImageResource(getAvatarResourceId(friendUser.getAvatarUrl()));
                    }

                    tvPoints.setText(String.format(Locale.getDefault(), "%,d", friendUser.getTotalPoints()));
                    tvStreak.setText(String.format(Locale.getDefault(), "%dd", friendUser.getStreak()));

                    if (tvDuels != null) {
                        tvDuels.setText(String.format(Locale.getDefault(), "%d-%d", friendUser.getWins(), friendUser.getLosses()));
                    }

                    if (tvLevel != null) tvLevel.setText(String.format(Locale.getDefault(), "Lv.%d", friendUser.getLevel()));

                    if (tvRankBadge != null) {
                        tvRankBadge.setText(String.format("%s %s Rank", friendUser.getRankEmoji(), friendUser.getRankName()));
                    }

                    if (tvBestStreak != null) tvBestStreak.setText(String.format(Locale.getDefault(), "%d days", friendUser.getBestStreak()));
                    if (tvTotalSessions != null) tvTotalSessions.setText(String.valueOf(friendUser.getTotalSessionsCount()));
                    if (tvWinRate != null) tvWinRate.setText(String.format(Locale.getDefault(), "%.1f%%", friendUser.getWinRate()));

                    updateOnlineStatus(friendUser);
                    displaySkills(friendUser);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FriendProfile", "Load failed", error.toException());
            }
        });
    }

    private int getAvatarResourceId(String avatarName) {
        if (avatarName == null || avatarName.isEmpty()) return R.drawable.prof1;
        int resId = getResources().getIdentifier(avatarName, "drawable", getPackageName());
        return resId != 0 ? resId : R.drawable.prof1;
    }

    private void updateOnlineStatus(User user) {
        if (user.isOnline()) {
            tvOnlineStatus.setText("Online now");
            tvOnlineStatus.setTextColor(ContextCompat.getColor(this, R.color.splash_green));
            vAvatarDot.setBackgroundTintList(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this, R.color.splash_green)));
            vStatusDot.setBackgroundTintList(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this, R.color.splash_green)));
        } else {
            String timeAgo = getTimeAgo(user.getLastOnlineTimestamp());
            tvOnlineStatus.setText(String.format("Offline %s", timeAgo));
            tvOnlineStatus.setTextColor(0xFF8E8E93);
            vAvatarDot.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF8E8E93));
            vStatusDot.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF8E8E93));
        }
    }

    private String getTimeAgo(long timestamp) {
        if (timestamp <= 0) return "a long time ago";

        long now = System.currentTimeMillis();
        long diff = now - timestamp;

        if (diff < TimeUnit.MINUTES.toMillis(1)) return "just now";
        if (diff < TimeUnit.HOURS.toMillis(1)) return (diff / TimeUnit.MINUTES.toMillis(1)) + "m ago";
        if (diff < TimeUnit.DAYS.toMillis(1)) return (diff / TimeUnit.HOURS.toMillis(1)) + "h ago";
        return (diff / TimeUnit.DAYS.toMillis(1)) + "d ago";
    }

    private void displaySkills(User user) {
        if (llSkillsList == null) return;
        llSkillsList.removeAllViews();

        List<String> skills = user.getSkills();
        Map<String, Double> mastery = user.getSkillMastery();

        if (skills != null) {
            for (String skill : skills) {
                double progress = mastery != null ? mastery.getOrDefault(skill, 0.0) : 0.0;

                View item = getLayoutInflater().inflate(R.layout.item_home_skill, llSkillsList, false);
                ((TextView) item.findViewById(R.id.tv_skill_name)).setText(skill);

                TextView tvStatus = item.findViewById(R.id.tv_status_text);
                View vStatusDotLocal = item.findViewById(R.id.v_status_dot);

                if (progress >= 90) {
                    tvStatus.setText("Mastered");
                    tvStatus.setTextColor(0xFF58CC02);
                    vStatusDotLocal.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF58CC02));
                } else if (progress < 20) {
                    tvStatus.setText("Starting");
                    tvStatus.setTextColor(0xFFFF9600);
                    vStatusDotLocal.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFFF9600));
                } else {
                    tvStatus.setText("In Progress");
                    tvStatus.setTextColor(0xFF00C2FF);
                    vStatusDotLocal.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF00C2FF));
                }

                ((TextView) item.findViewById(R.id.tv_skill_stats)).setText(String.format(Locale.getDefault(), "%d%%", (int) progress));
                item.findViewById(R.id.ll_time_info).setVisibility(View.GONE);

                // Set progress bar width
                View fill = item.findViewById(R.id.v_progress_fill);
                View bg = item.findViewById(R.id.v_progress_bg);
                if (bg != null && fill != null) {
                    bg.post(() -> {
                        ViewGroup.LayoutParams params = fill.getLayoutParams();
                        params.width = (int) (bg.getWidth() * (progress / 100f));
                        fill.setLayoutParams(params);
                    });
                }

                llSkillsList.addView(item);
            }
        }
    }
}
