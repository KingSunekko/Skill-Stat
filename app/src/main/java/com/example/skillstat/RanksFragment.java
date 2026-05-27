package com.example.skillstat;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnticipateOvershootInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;

import com.example.skillstat.models.Duel;
import com.example.skillstat.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RanksFragment extends Fragment {

    private static final String TAG = "RanksFragment";
    private LinearLayout llLeaderboardList;
    private View podiumSection;
    private TextView tabGlobal, tabWeekly, tabSkills, tabDuels;

    private TextView tvUserPoints, tvUserName, tvUserStreak, tvUserRank, tvUserRankLabel, tvTopRankBadge;
    private ImageView ivHeaderUserAvatar;

    private ImageView ivPodium1Avatar, ivPodium2Avatar, ivPodium3Avatar;
    private TextView tvPodium1Name, tvPodium1Score;
    private TextView tvPodium2Name, tvPodium2Score;
    private TextView tvPodium3Name, tvPodium3Score;

    private List<TextView> tabs = new ArrayList<>();
    private DatabaseReference mDatabase;
    private String currentUid;
    private boolean isFirebaseAvailable = false;
    private String currentTabMode = "global"; 
    private boolean hasAnimatedEntrance = false;

    private static class SkillRankEntry {
        User user;
        String skillName;
        double mastery;

        SkillRankEntry(User user, String skillName, double mastery) {
            this.user = user;
            this.skillName = skillName;
            this.mastery = mastery;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_ranks, container, false);
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

        llLeaderboardList = view.findViewById(R.id.ll_leaderboard_list);
        podiumSection = view.findViewById(R.id.ll_podium_section);

        tvUserPoints = view.findViewById(R.id.tv_user_points);
        tvUserName = view.findViewById(R.id.tv_user_name);
        tvUserStreak = view.findViewById(R.id.tv_user_streak);
        tvUserRank = view.findViewById(R.id.tv_user_rank);
        tvUserRankLabel = view.findViewById(R.id.tv_user_rank_label);
        ivHeaderUserAvatar = view.findViewById(R.id.iv_header_user_avatar);
        tvTopRankBadge = view.findViewById(R.id.tv_top_rank_badge);

        tabGlobal = view.findViewById(R.id.tab_global);
        tabDuels = view.findViewById(R.id.tab_duels);
        tabWeekly = view.findViewById(R.id.tab_weekly);
        tabSkills = view.findViewById(R.id.tab_skills_rank);

        ivPodium1Avatar = view.findViewById(R.id.iv_podium_1_avatar);
        tvPodium1Name = view.findViewById(R.id.tv_podium_1_name);
        tvPodium1Score = view.findViewById(R.id.tv_podium_1_score);

        ivPodium2Avatar = view.findViewById(R.id.iv_podium_2_avatar);
        tvPodium2Name = view.findViewById(R.id.tv_podium_2_name);
        tvPodium2Score = view.findViewById(R.id.tv_podium_2_score);

        ivPodium3Avatar = view.findViewById(R.id.iv_podium_3_avatar);
        tvPodium3Name = view.findViewById(R.id.tv_podium_3_name);
        tvPodium3Score = view.findViewById(R.id.tv_podium_3_score);

        tabs.add(tabGlobal);
        tabs.add(tabDuels);
        tabs.add(tabWeekly);
        tabs.add(tabSkills);

        setupTabs();
        selectTab(tabGlobal);
        
        // Initial setup for entrance animations
        View userCard = view.findViewById(R.id.card_user_rank);
        if (userCard != null) {
            userCard.setAlpha(0);
            userCard.setTranslationY(-30);
        }
        if (podiumSection != null) {
            podiumSection.setAlpha(0);
            podiumSection.setScaleY(0.8f);
        }

        if (isFirebaseAvailable) {
            loadLeaderboard("totalPoints");
        }
    }

    private void runEntranceAnimations() {
        if (hasAnimatedEntrance) return;
        hasAnimatedEntrance = true;

        View userCard = getView() != null ? getView().findViewById(R.id.card_user_rank) : null;
        if (userCard != null) {
            userCard.animate().alpha(1).translationY(0).setDuration(600).setInterpolator(new DecelerateInterpolator()).start();
        }

        if (podiumSection != null) {
            podiumSection.animate().alpha(1).scaleY(1.0f).setDuration(700).setStartDelay(200).setInterpolator(new AnticipateOvershootInterpolator()).start();
        }
    }

    private void setupTabs() {
        tabGlobal.setOnClickListener(v -> { selectTab(tabGlobal); currentTabMode = "global"; if (isFirebaseAvailable) loadLeaderboard("totalPoints"); });
        tabDuels.setOnClickListener(v -> { selectTab(tabDuels); currentTabMode = "duels"; if (isFirebaseAvailable) loadDuelsLeaderboard(); });
        tabWeekly.setOnClickListener(v -> { selectTab(tabWeekly); currentTabMode = "weekly"; if (isFirebaseAvailable) loadLeaderboard("weeklyPoints"); });
        tabSkills.setOnClickListener(v -> { selectTab(tabSkills); currentTabMode = "skills"; if (isFirebaseAvailable) loadSkillsLeaderboard(); });
    }

    private void selectTab(TextView selectedTab) {
        if (getContext() == null) return;
        for (TextView tab : tabs) {
            boolean isActive = (tab == selectedTab);
            tab.setSelected(isActive);
            tab.setBackgroundResource(isActive ? R.drawable.shape_skill_chip_selector : 0);
            tab.setTextColor(ContextCompat.getColor(requireContext(), isActive ? R.color.white : R.color.splash_text_secondary));
            tab.animate().scaleX(isActive ? 1.05f : 1.0f).scaleY(isActive ? 1.05f : 1.0f).setDuration(200).start();
        }
    }

    private void loadLeaderboard(String orderByField) {
        if (tvUserRankLabel != null) {
            tvUserRankLabel.setText("weeklyPoints".equals(orderByField) ? "Weekly Rank" : "Global Rank");
        }
        Query query = mDatabase.child("users").orderByChild(orderByField).limitToLast(50);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                List<User> userList = new ArrayList<>();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    User user = ds.getValue(User.class);
                    if (user != null) {
                        user.setUid(ds.getKey());
                        userList.add(user);
                    }
                }
                Collections.reverse(userList);
                updateUI(userList);
                runEntranceAnimations();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadDuelsLeaderboard() {
        if (tvUserRankLabel != null) tvUserRankLabel.setText("Duels Rank");
        mDatabase.child("users").orderByChild("wins").limitToLast(50).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded() || !"duels".equals(currentTabMode)) return;
                List<User> userList = new ArrayList<>();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    User user = ds.getValue(User.class);
                    if (user != null) {
                        user.setUid(ds.getKey());
                        userList.add(user);
                    }
                }
                Collections.reverse(userList);
                updateUI(userList);
                loadLiveDuels();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadLiveDuels() {
        mDatabase.child("duels").orderByChild("status").equalTo("active").limitToLast(5).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded() || !"duels".equals(currentTabMode)) return;
                if (snapshot.exists()) {
                    addSectionHeader("LIVE DUELS ⚔️");
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        Duel d = ds.getValue(Duel.class);
                        if (d != null) { d.setDuelId(ds.getKey()); addLiveDuelItem(d); }
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void addSectionHeader(String title) {
        TextView tv = new TextView(getContext());
        tv.setText(title); tv.setTextColor(0xFF8E8E93); tv.setTextSize(11);
        tv.setPadding(0, 32, 0, 16); tv.setTypeface(Typeface.DEFAULT_BOLD);
        llLeaderboardList.addView(tv);
    }

    private void addLiveDuelItem(Duel duel) {
        View v = getLayoutInflater().inflate(R.layout.item_duel_history, llLeaderboardList, false);
        ((TextView) v.findViewById(R.id.tv_title)).setText(duel.getSkillName() + " Battle");
        ((TextView) v.findViewById(R.id.tv_subtitle)).setText("Ongoing Live Match");
        ((TextView) v.findViewById(R.id.tv_your_score)).setText("Pts: " + String.format("%.1f", duel.getInitiatorEffort()));
        ((TextView) v.findViewById(R.id.tv_their_score)).setText("Pts: " + String.format("%.1f", duel.getOpponentEffort()));
        v.setOnClickListener(view -> {
            Intent i = new Intent(getActivity(), ActiveDuelActivity.class);
            i.putExtra("duel_id", duel.getDuelId()); startActivity(i);
        });
        llLeaderboardList.addView(v);
    }

    private void loadSkillsLeaderboard() {
        if (tvUserRankLabel != null) tvUserRankLabel.setText("Skills Rank");
        mDatabase.child("users").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded() || !"skills".equals(currentTabMode)) return;
                List<SkillRankEntry> skillEntries = new ArrayList<>();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    User user = ds.getValue(User.class);
                    if (user != null) {
                        user.setUid(ds.getKey());
                        if (user.getSkillMastery() != null) {
                            for (Map.Entry<String, Double> entry : user.getSkillMastery().entrySet()) {
                                skillEntries.add(new SkillRankEntry(user, entry.getKey(), entry.getValue()));
                            }
                        }
                    }
                }
                Collections.sort(skillEntries, (e1, e2) -> Double.compare(e2.mastery, e1.mastery));
                updateSkillsUI(skillEntries);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void updateSkillsUI(List<SkillRankEntry> entries) {
        llLeaderboardList.removeAllViews();
        podiumSection.setVisibility(View.GONE);
        for (int i = 0; i < entries.size(); i++) {
            SkillRankEntry entry = entries.get(i);
            boolean isMe = currentUid != null && entry.user.getUid().equals(currentUid);
            addListItem(i + 1, (i < 3 ? (i == 0 ? "🥇" : (i == 1 ? "🥈" : "🥉")) : ""), entry.user, entry.skillName, (int) entry.mastery + "%", isMe, i);
        }
    }

    private void updateUI(List<User> users) {
        llLeaderboardList.removeAllViews();
        podiumSection.setVisibility(View.VISIBLE);
        int myRank = -1; User me = null;
        for (int i = 0; i < users.size(); i++) {
            if (currentUid != null && users.get(i).getUid().equals(currentUid)) { myRank = i + 1; me = users.get(i); break; }
        }
        if (me != null) {
            tvUserName.setText(me.getUsername());
            ivHeaderUserAvatar.setImageResource(getAvatarResourceId(me.getAvatarUrl()));
            tvUserStreak.setText("🔥 " + me.getStreak() + " streak");
            String pts;
            if ("duels".equals(currentTabMode)) pts = me.getWins() + " wins";
            else if ("weekly".equals(currentTabMode)) pts = me.getWeeklyPoints() + " XP";
            else pts = me.getTotalPoints() + " pts";
            
            animateNumber(tvUserPoints, Integer.parseInt(pts.replaceAll("[^0-9]", "")), pts.replaceAll("[0-9,]", ""), "");
            tvUserRank.setText("#" + myRank);
            tvTopRankBadge.setText("#" + myRank + " " + currentTabMode.toUpperCase());
            if (myRank <= 3) checkAndAwardPodiumBadge(me);
        }

        if (users.size() >= 1) setPodium(1, users.get(0));
        if (users.size() >= 2) setPodium(2, users.get(1));
        if (users.size() >= 3) setPodium(3, users.get(2));

        for (int i = 0; i < users.size(); i++) {
            User u = users.get(i);
            String score;
            if ("duels".equals(currentTabMode)) score = String.valueOf(u.getWins());
            else if ("weekly".equals(currentTabMode)) score = String.valueOf(u.getWeeklyPoints());
            else score = String.valueOf(u.getTotalPoints());
            
            addListItem(i + 1, (i < 3 ? (i == 0 ? "🥇" : (i == 1 ? "🥈" : "🥉")) : ""), u, "🔥 " + u.getStreak() + "d streak", score, currentUid != null && u.getUid().equals(currentUid), i);
        }
    }

    private void checkAndAwardPodiumBadge(User user) {
        if (user.getBadges() != null && !user.getBadges().contains("Podium")) {
            List<String> newBadges = new ArrayList<>(user.getBadges());
            newBadges.add("Podium");
            mDatabase.child("users").child(currentUid).child("badges").setValue(newBadges);
            mDatabase.child("users").child(currentUid).child("totalPoints").setValue(user.getTotalPoints() + 500);
            Toast.makeText(getContext(), "🏆 UNLOCKED: Podium Badge! (+500 XP)", Toast.LENGTH_LONG).show();
        }
    }

    private void animateNumber(TextView tv, int target, String prefix, String suffix) {
        if (tv == null) return;
        ValueAnimator animator = ValueAnimator.ofInt(0, target);
        animator.setDuration(1000);
        animator.addUpdateListener(animation -> tv.setText(prefix + animation.getAnimatedValue() + suffix));
        animator.start();
    }

    private void setPodium(int place, User user) {
        ImageView av = place == 1 ? ivPodium1Avatar : (place == 2 ? ivPodium2Avatar : ivPodium3Avatar);
        TextView name = place == 1 ? tvPodium1Name : (place == 2 ? tvPodium2Name : tvPodium3Name);
        TextView score = place == 1 ? tvPodium1Score : (place == 2 ? tvPodium2Score : tvPodium3Score);
        av.setImageResource(getAvatarResourceId(user.getAvatarUrl()));
        name.setText(user.getUsername());
        
        String scoreVal;
        if ("duels".equals(currentTabMode)) scoreVal = String.valueOf(user.getWins());
        else if ("weekly".equals(currentTabMode)) scoreVal = String.valueOf(user.getWeeklyPoints());
        else scoreVal = String.valueOf(user.getTotalPoints());
        
        score.setText(scoreVal);
        av.setScaleX(0); av.setScaleY(0);
        av.animate().scaleX(1).scaleY(1).setDuration(500).setStartDelay(300 + place * 100L).setInterpolator(new AnticipateOvershootInterpolator()).start();
    }

    private void addListItem(int rank, String medal, User user, String sub, String score, boolean isYou, int index) {
        View item = getLayoutInflater().inflate(R.layout.item_leaderboard, llLeaderboardList, false);
        ((ImageView) item.findViewById(R.id.iv_avatar)).setImageResource(getAvatarResourceId(user.getAvatarUrl()));
        ((TextView) item.findViewById(R.id.tv_name)).setText(isYou ? "You" : user.getUsername());
        ((TextView) item.findViewById(R.id.tv_sub_info)).setText(sub);
        ((TextView) item.findViewById(R.id.tv_score)).setText(score);
        
        TextView tvRank = item.findViewById(R.id.tv_rank_number);
        TextView tvMedal = item.findViewById(R.id.tv_rank_medal);
        if (!medal.isEmpty()) { 
            tvMedal.setText(medal); 
            tvMedal.setVisibility(View.VISIBLE); 
            tvRank.setVisibility(View.GONE); 
        } else {
            tvRank.setText(String.valueOf(rank));
        }
        
        if (isYou) item.setBackgroundResource(R.drawable.shape_leaderboard_you);

        item.setAlpha(0); item.setTranslationX(50);
        item.animate().alpha(1).translationX(0).setDuration(400).setStartDelay(500 + index * 50L).start();
        llLeaderboardList.addView(item);
    }

    private int getAvatarResourceId(String avatarName) {
        if (avatarName == null) return R.drawable.prof1;
        int resId = getResources().getIdentifier(avatarName, "drawable", requireContext().getPackageName());
        return resId != 0 ? resId : R.drawable.prof1;
    }
}
