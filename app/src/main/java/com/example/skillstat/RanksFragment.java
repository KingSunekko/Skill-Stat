package com.example.skillstat;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

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
import java.util.List;

public class RanksFragment extends Fragment {

    private static final String TAG = "RanksFragment";
    private LinearLayout llLeaderboardList;
    private View podiumSection;
    private TextView tabGlobal, tabWeekly, tabSkills;

    private TextView tvUserPoints, tvUserName, tvUserStreak, tvUserRank, tvUserRankLabel, tvHeaderEmoji, tvTopRankBadge;

    private TextView tvPodium1Emoji, tvPodium1Name, tvPodium1Score;
    private TextView tvPodium2Emoji, tvPodium2Name, tvPodium2Score;
    private TextView tvPodium3Emoji, tvPodium3Name, tvPodium3Score;

    private List<TextView> tabs = new ArrayList<>();
    private DatabaseReference mDatabase;
    private String currentUid;
    private boolean isFirebaseAvailable = false;

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
        tvHeaderEmoji = view.findViewById(R.id.tv_header_user_emoji);
        tvTopRankBadge = view.findViewById(R.id.tv_top_rank_badge);

        tabGlobal = view.findViewById(R.id.tab_global);
        tabWeekly = view.findViewById(R.id.tab_weekly);
        tabSkills = view.findViewById(R.id.tab_skills_rank);

        tvPodium1Emoji = view.findViewById(R.id.tv_podium_1_emoji);
        tvPodium1Name = view.findViewById(R.id.tv_podium_1_name);
        tvPodium1Score = view.findViewById(R.id.tv_podium_1_score);

        tvPodium2Emoji = view.findViewById(R.id.tv_podium_2_emoji);
        tvPodium2Name = view.findViewById(R.id.tv_podium_2_name);
        tvPodium2Score = view.findViewById(R.id.tv_podium_2_score);

        tvPodium3Emoji = view.findViewById(R.id.tv_podium_3_emoji);
        tvPodium3Name = view.findViewById(R.id.tv_podium_3_name);
        tvPodium3Score = view.findViewById(R.id.tv_podium_3_score);

        tabs.add(tabGlobal);
        tabs.add(tabWeekly);
        tabs.add(tabSkills);

        setupTabs();
        selectTab(tabGlobal);
        
        if (isFirebaseAvailable) {
            loadGlobalLeaderboard();
        } else {
            podiumSection.setVisibility(View.GONE);
            if (tvUserName != null) tvUserName.setText("Offline Mode");
        }
    }

    private void setupTabs() {
        tabGlobal.setOnClickListener(v -> { selectTab(tabGlobal); if (isFirebaseAvailable) loadGlobalLeaderboard(); });
        tabWeekly.setOnClickListener(v -> { selectTab(tabWeekly); if (isFirebaseAvailable) loadGlobalLeaderboard(); });
        tabSkills.setOnClickListener(v -> { selectTab(tabSkills); loadSkillsPlaceholder(); });
    }

    private void selectTab(TextView selectedTab) {
        if (getContext() == null) return;
        for (TextView tab : tabs) {
            if (tab == selectedTab) {
                tab.setSelected(true);
                tab.setBackgroundResource(R.drawable.shape_skill_chip_selector);
                tab.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
            } else {
                tab.setSelected(false);
                tab.setBackground(null);
                tab.setTextColor(ContextCompat.getColor(requireContext(), R.color.splash_text_secondary));
            }
        }
    }

    private void loadGlobalLeaderboard() {
        if (tvUserRankLabel != null) tvUserRankLabel.setText("Global Rank");
        if (mDatabase == null) return;
        
        Query query = mDatabase.child("users").orderByChild("totalPoints").limitToLast(50);
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
            }
            @Override public void onCancelled(@NonNull DatabaseError error) { Log.e(TAG, "Load failed", error.toException()); }
        });
    }

    private void updateUI(List<User> users) {
        if (llLeaderboardList == null) return;
        llLeaderboardList.removeAllViews();
        if (users.isEmpty()) {
            podiumSection.setVisibility(View.GONE);
            return;
        }

        int myRank = -1;
        User me = null;

        for (int i = 0; i < users.size(); i++) {
            if (currentUid != null && users.get(i).getUid() != null && users.get(i).getUid().equals(currentUid)) {
                myRank = i + 1;
                me = users.get(i);
                break;
            }
        }

        if (me != null) {
            if (tvUserName != null) tvUserName.setText(me.getUsername());
            if (tvUserPoints != null) tvUserPoints.setText(me.getTotalPoints() + " pts");
            if (tvUserStreak != null) tvUserStreak.setText(me.getStreak() + " streak");
            if (tvHeaderEmoji != null) tvHeaderEmoji.setText(me.getAvatarUrl());
            if (tvUserRank != null) tvUserRank.setText(myRank != -1 ? "#" + myRank : "--");
            if (tvTopRankBadge != null) tvTopRankBadge.setText(myRank != -1 ? "#" + myRank + " Global" : "Unranked");
        }

        podiumSection.setVisibility(View.VISIBLE);
        if (users.size() >= 1) setPodium(1, users.get(0));
        if (users.size() >= 2) setPodium(2, users.get(1));
        else if (tvPodium2Name != null) tvPodium2Name.setText("---");
        if (users.size() >= 3) setPodium(3, users.get(2));
        else if (tvPodium3Name != null) tvPodium3Name.setText("---");

        for (int i = 0; i < users.size(); i++) {
            User user = users.get(i);
            int rank = i + 1;
            String medal = "";
            if (rank == 1) medal = "🥇";
            else if (rank == 2) medal = "🥈";
            else if (rank == 3) medal = "🥉";

            String sub = "🔥 " + user.getStreak() + "d • Lv." + ((user.getTotalPoints() / 1000) + 1);
            boolean isMe = currentUid != null && user.getUid() != null && user.getUid().equals(currentUid);
            addListItem(rank, medal, user, sub, String.valueOf(user.getTotalPoints()), isMe);
        }
    }

    private void setPodium(int place, User user) {
        boolean isMe = currentUid != null && user.getUid() != null && user.getUid().equals(currentUid);
        String name = isMe ? "You" : user.getUsername();
        
        TextView emojiView = null, nameView = null, scoreView = null;
        if (place == 1) { emojiView = tvPodium1Emoji; nameView = tvPodium1Name; scoreView = tvPodium1Score; }
        else if (place == 2) { emojiView = tvPodium2Emoji; nameView = tvPodium2Name; scoreView = tvPodium2Score; }
        else if (place == 3) { emojiView = tvPodium3Emoji; nameView = tvPodium3Name; scoreView = tvPodium3Score; }

        if (emojiView != null) emojiView.setText(user.getAvatarUrl());
        if (nameView != null) nameView.setText(name);
        if (scoreView != null) scoreView.setText(String.valueOf(user.getTotalPoints()));

        if (nameView != null && nameView.getParent() instanceof View) {
            View podiumContainer = (View) nameView.getParent();
            podiumContainer.setOnClickListener(v -> openFriendProfile(user.getUid()));
        }
    }

    private void addListItem(int rank, String medal, User user, String sub, String score, boolean isYou) {
        View item = getLayoutInflater().inflate(R.layout.item_leaderboard, llLeaderboardList, false);
        ((TextView) item.findViewById(R.id.tv_avatar_emoji)).setText(user.getAvatarUrl());
        ((TextView) item.findViewById(R.id.tv_name)).setText(isYou ? "You" : user.getUsername());
        ((TextView) item.findViewById(R.id.tv_sub_info)).setText(sub);
        ((TextView) item.findViewById(R.id.tv_score)).setText(score);
        
        TextView tvRank = item.findViewById(R.id.tv_rank_number);
        TextView tvMedal = item.findViewById(R.id.tv_rank_medal);
        if (!medal.isEmpty()) {
            if (tvMedal != null) { tvMedal.setText(medal); tvMedal.setVisibility(View.VISIBLE); }
            if (tvRank != null) tvRank.setVisibility(View.GONE);
        } else if (tvRank != null) {
            tvRank.setText(String.valueOf(rank));
        }

        if (isYou) {
            item.setBackgroundResource(R.drawable.shape_leaderboard_you);
            View badge = item.findViewById(R.id.tv_you_badge);
            if (badge != null) badge.setVisibility(View.VISIBLE);
        }

        item.setOnClickListener(v -> openFriendProfile(user.getUid()));
        llLeaderboardList.addView(item);
    }

    private void openFriendProfile(String uid) {
        if (uid == null) return;
        Intent intent = new Intent(getActivity(), FriendProfileActivity.class);
        intent.putExtra("friend_uid", uid);
        startActivity(intent);
    }

    private void loadSkillsPlaceholder() {
        llLeaderboardList.removeAllViews();
        podiumSection.setVisibility(View.GONE);
        if (getContext() != null) {
            TextView tv = new TextView(getContext());
            tv.setText("Skills leaderboard coming soon!");
            tv.setTextColor(0x88FFFFFF);
            tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            tv.setPadding(0, 100, 0, 0);
            llLeaderboardList.addView(tv);
        }
    }
}
