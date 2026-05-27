package com.example.skillstat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.AnticipateOvershootInterpolator;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.example.skillstat.models.Duel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int NOTIFICATION_PERMISSION_CODE = 101;
    
    private View navHome, navSkills, navFriends, navRanks, navProfile;
    private View pillHome, pillSkills, pillFriends, pillRanks, pillProfile;
    private TextView labelHome, labelSkills, labelFriends, labelRanks, labelProfile;
    private TextView tvFriendsBadge;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private ValueEventListener friendRequestsListener, chatNotificationListener;
    private final Map<String, Long> lastChatNotified = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            mAuth = FirebaseAuth.getInstance();
            mDatabase = FirebaseDatabase.getInstance().getReference();
            FirebaseUser currentUser = mAuth.getCurrentUser();

            if (currentUser == null) {
                goToLogin();
                return;
            } else {
                setUserOnlineStatus(true);
                checkNotificationPermission();
                scheduleReminders();
                checkPendingDuels();
            }
        } catch (Exception e) {
            Log.e(TAG, "Firebase initialization error", e);
            goToLogin();
            return;
        }

        setContentView(R.layout.activity_main);

        View bottomNav = findViewById(R.id.bottom_nav_container);
        if (bottomNav != null) {
            ViewCompat.setOnApplyWindowInsetsListener(bottomNav, (v, insets) -> {
                Insets navigationBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars());
                v.setPadding(0, 0, 0, navigationBars.bottom);
                return insets;
            });
        }

        initViews();
        setupNavigation();
        setupFriendRequestsListener();
        setupChatNotificationListener();

        handleIntent(getIntent());
        
        if (savedInstanceState == null && getIntent().getStringExtra("navigate_to") == null 
                && getIntent().getStringExtra("ACTION") == null && !getIntent().getBooleanExtra("timer_finished", false)) {
            updateNavSelection(navHome);
            loadFragment(new HomeFragment(), false);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (intent == null) return;

        boolean timerFinished = intent.getBooleanExtra("timer_finished", false);
        boolean triggerFinish = intent.getBooleanExtra("trigger_finish", false);
        long timeLeft = intent.getLongExtra("time_left", -1);

        if (timerFinished || triggerFinish) {
            String skillName = intent.getStringExtra("skill_name");
            Log.d(TAG, "Timer finished or triggered for: " + skillName + " timeLeft: " + timeLeft);
            
            Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
            if (currentFragment instanceof PracticeFragment) {
                if (timeLeft != -1) {
                    ((PracticeFragment) currentFragment).updateTimeLeftExternally(timeLeft);
                }
                ((PracticeFragment) currentFragment).onNotificationFinishTriggered();
            } else {
                navigateToPractice(skillName, true, timeLeft);
            }
            return;
        }

        String action = intent.getStringExtra("ACTION");
        if ("OPEN_ADD_SKILL".equals(action)) {
            String category = intent.getStringExtra("CATEGORY_NAME");
            AddSkillFragment fragment = new AddSkillFragment();
            if (category != null) {
                Bundle args = new Bundle();
                args.putString("selected_category", category);
                fragment.setArguments(args);
            }
            updateNavSelection(navSkills);
            loadFragment(fragment, true);
            return;
        }

        String navigateTo = intent.getStringExtra("navigate_to");
        if ("practice".equals(navigateTo)) {
            String skillName = intent.getStringExtra("skill_name");
            navigateToPractice(skillName, false, -1);
        }
    }

    private void navigateToPractice(String skillName, boolean autoFinish, long timeLeft) {
        Log.d(TAG, "Navigating to practice for: " + skillName + " autoFinish: " + autoFinish);
        updateNavSelection(navHome);
        
        PracticeFragment fragment = new PracticeFragment();
        Bundle args = new Bundle();
        args.putString("skill_name", skillName);
        if (autoFinish) args.putBoolean("auto_finish", true);
        if (timeLeft != -1) args.putLong("time_left", timeLeft);
        fragment.setArguments(args);
        
        getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        
        findViewById(R.id.fragment_container).post(() -> {
            loadFragment(fragment, false);
        });
    }

    private void checkPendingDuels() {
        String uid = mAuth.getUid();
        if (uid == null) return;

        mDatabase.child("users").child(uid).child("activeDuels").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String duelId = ds.getKey();
                    if (duelId != null) verifyAndNotifyDuel(duelId);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void verifyAndNotifyDuel(String duelId) {
        mDatabase.child("duels").child(duelId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Duel duel = snapshot.getValue(Duel.class);
                if (duel != null) {
                    if ("completed".equals(duel.getStatus())) {
                        showDuelSettledNotification(duel);
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void showDuelSettledNotification(Duel duel) {
        new AlertDialog.Builder(this, R.style.CustomAlertDialog)
                .setTitle("Duel Result!")
                .setMessage("Your duel for " + duel.getSkillName() + " has been settled. Check your stats!")
                .setPositiveButton("View Details", (dialog, which) -> {
                    Intent intent = new Intent(this, ActiveDuelActivity.class);
                    intent.putExtra("duel_id", duel.getDuelId());
                    intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                })
                .setNegativeButton("Dismiss", null)
                .show();
    }

    private void initViews() {
        navHome = findViewById(R.id.navHome);
        navSkills = findViewById(R.id.navSkills);
        navFriends = findViewById(R.id.navFriends);
        navRanks = findViewById(R.id.navRanks);
        navProfile = findViewById(R.id.navProfile);

        pillHome = findViewById(R.id.pillHome);
        pillSkills = findViewById(R.id.pillSkills);
        pillFriends = findViewById(R.id.pillFriends);
        pillRanks = findViewById(R.id.pillRanks);
        pillProfile = findViewById(R.id.pillProfile);

        labelHome = findViewById(R.id.labelHome);
        labelSkills = findViewById(R.id.labelSkills);
        labelFriends = findViewById(R.id.labelFriends);
        labelRanks = findViewById(R.id.labelRanks);
        labelProfile = findViewById(R.id.labelProfile);
        
        tvFriendsBadge = findViewById(R.id.tv_friends_badge);
        if (tvFriendsBadge != null) tvFriendsBadge.setVisibility(View.GONE);
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_CODE);
            }
        }
    }

    private void scheduleReminders() {
        PeriodicWorkRequest reminderRequest =
                new PeriodicWorkRequest.Builder(ReminderWorker.class, 1, TimeUnit.HOURS)
                        .setInitialDelay(30, TimeUnit.MINUTES)
                        .addTag("skill_reminders")
                        .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "SkillStatReminders",
                ExistingPeriodicWorkPolicy.KEEP,
                reminderRequest
        );
    }

    private void setupFriendRequestsListener() {
        String uid = mAuth.getUid();
        if (uid == null) return;

        friendRequestsListener = new ValueEventListener() {
            private long lastCount = -1;
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long count = snapshot.getChildrenCount();
                if (lastCount != -1 && count > lastCount) {
                    NotificationHelper.showNotification(MainActivity.this, 
                            "New Friend Request! 👥", 
                            "Someone wants to connect with you.", 
                            NotificationHelper.CHANNEL_ID_SOCIAL);
                }
                lastCount = count;

                if (tvFriendsBadge != null) {
                    if (count > 0) {
                        tvFriendsBadge.setText(String.valueOf(count));
                        tvFriendsBadge.setVisibility(View.VISIBLE);
                        tvFriendsBadge.setScaleX(0);
                        tvFriendsBadge.setScaleY(0);
                        tvFriendsBadge.animate()
                                .scaleX(1)
                                .scaleY(1)
                                .setDuration(300)
                                .setInterpolator(new AnticipateOvershootInterpolator())
                                .start();
                    } else {
                        tvFriendsBadge.setVisibility(View.GONE);
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Friend requests listener failed", error.toException());
            }
        };
        mDatabase.child("users").child(uid).child("friendRequests").addValueEventListener(friendRequestsListener);
    }

    private void setupChatNotificationListener() {
        String uid = mAuth.getUid();
        if (uid == null) return;

        chatNotificationListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String otherUid = ds.getKey();
                    Integer unread = ds.child("unreadCount").getValue(Integer.class);
                    Long timestamp = ds.child("lastMessageTimestamp").getValue(Long.class);
                    String lastMsg = ds.child("lastMessage").getValue(String.class);

                    if (unread != null && unread > 0 && timestamp != null) {
                        Long lastKnown = lastChatNotified.get(otherUid);
                        if (lastKnown == null || timestamp > lastKnown) {
                            lastChatNotified.put(otherUid, timestamp);
                            NotificationHelper.showNotification(MainActivity.this, 
                                    "New Message 💬", 
                                    lastMsg != null ? lastMsg : "You have a new message.", 
                                    NotificationHelper.CHANNEL_ID_SOCIAL);
                        }
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        };
        mDatabase.child("users").child(uid).child("recentChats").addValueEventListener(chatNotificationListener);
    }

    private void setUserOnlineStatus(boolean online) {
        String uid = mAuth.getUid();
        if (uid != null && mDatabase != null) {
            DatabaseReference userRef = mDatabase.child("users").child(uid);
            userRef.child("online").setValue(online);
            if (online) {
                userRef.child("online").onDisconnect().setValue(false);
                userRef.child("lastOnlineTimestamp").onDisconnect().setValue(ServerValue.TIMESTAMP);
            } else {
                userRef.child("lastOnlineTimestamp").setValue(ServerValue.TIMESTAMP);
            }
        }
    }

    @Override protected void onResume() { super.onResume(); setUserOnlineStatus(true); }
    @Override protected void onPause() { super.onPause(); setUserOnlineStatus(false); }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        String uid = mAuth.getUid();
        if (uid != null) {
            if (friendRequestsListener != null) mDatabase.child("users").child(uid).child("friendRequests").removeEventListener(friendRequestsListener);
            if (chatNotificationListener != null) mDatabase.child("users").child(uid).child("recentChats").removeEventListener(chatNotificationListener);
        }
    }

    private void goToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    private void setupNavigation() {
        navHome.setOnClickListener(v -> { updateNavSelection(navHome); loadFragment(new HomeFragment(), false); });
        navSkills.setOnClickListener(v -> { updateNavSelection(navSkills); loadFragment(new SkillsFragment(), false); });
        navFriends.setOnClickListener(v -> { updateNavSelection(navFriends); loadFragment(new FriendsFragment(), false); });
        navRanks.setOnClickListener(v -> { updateNavSelection(navRanks); loadFragment(new RanksFragment(), false); });
        navProfile.setOnClickListener(v -> { updateNavSelection(navProfile); loadFragment(new ProfileFragment(), false); });
    }

    private void updateNavSelection(View selectedNav) {
        resetNavItem(pillHome, labelHome);
        resetNavItem(pillSkills, labelSkills);
        resetNavItem(pillFriends, labelFriends);
        resetNavItem(pillRanks, labelRanks);
        resetNavItem(pillProfile, labelProfile);

        if (selectedNav == navHome) setSelectedItem(pillHome, labelHome);
        else if (selectedNav == navSkills) setSelectedItem(pillSkills, labelSkills);
        else if (selectedNav == navFriends) setSelectedItem(pillFriends, labelFriends);
        else if (selectedNav == navRanks) setSelectedItem(pillRanks, labelRanks);
        else if (selectedNav == navProfile) setSelectedItem(pillProfile, labelProfile);
    }

    private void resetNavItem(View pill, TextView label) {
        if (pill != null) {
            pill.setBackground(null);
            pill.animate().scaleX(1.0f).scaleY(1.0f).setDuration(200).start();
        }
        if (label != null) {
            label.setTextColor(0xFF8E8E93);
            label.animate().alpha(0.6f).setDuration(200).start();
        }
    }

    private void setSelectedItem(View pill, TextView label) {
        if (pill != null) {
            pill.setBackgroundResource(R.drawable.shape_nav_pill_active);
            pill.setScaleX(0.8f);
            pill.setScaleY(0.8f);
            pill.animate()
                    .scaleX(1.1f)
                    .scaleY(1.1f)
                    .setDuration(300)
                    .setInterpolator(new AnticipateOvershootInterpolator())
                    .withEndAction(() -> pill.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start())
                    .start();
        }
        if (label != null) {
            label.setTextColor(ContextCompat.getColor(this, R.color.splash_green));
            label.setAlpha(0f);
            label.animate().alpha(1.0f).setDuration(300).start();
        }
    }

    public void loadFragment(Fragment fragment, boolean addToBackStack) {
        if (isFinishing() || isDestroyed()) return;
        
        androidx.fragment.app.FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        
        transaction.setCustomAnimations(
                R.anim.gentle_fade_in, R.anim.gentle_fade_out,
                R.anim.gentle_fade_in, R.anim.gentle_fade_out
        );
        
        transaction.replace(R.id.fragment_container, fragment);
        if (addToBackStack) transaction.addToBackStack(null);
        transaction.commitAllowingStateLoss();
    }
}
