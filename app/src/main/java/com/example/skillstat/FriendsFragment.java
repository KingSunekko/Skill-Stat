package com.example.skillstat;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.skillstat.models.Duel;
import com.example.skillstat.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FriendsFragment extends Fragment {

    private static final String TAG = "FriendsFragment";
    private LinearLayout llTabFriends, llTabLeaderboard, llTabSearch, llTabDuels;
    private TextView tvTabFriends, tvTabLeaderboard, tvTabSearch, tvTabDuels;
    private View viewFriendsContent, viewLeaderboardContent, viewSearchContent, viewDuelsContent;
    private LinearLayout llFriendsList, llLeaderboardList, llSearchList, llDuelList;
    private EditText etSearch;
    private DatabaseReference mDatabase;
    private String currentUid;
    private User currentUser;
    private boolean isFirebaseAvailable = false;
    private Set<String> friendUids = new HashSet<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_friends, container, false);

        try {
            mDatabase = FirebaseDatabase.getInstance().getReference();
            currentUid = FirebaseAuth.getInstance().getUid();
            isFirebaseAvailable = true;
        } catch (Exception e) {
            Log.e(TAG, "Firebase unavailable", e);
            isFirebaseAvailable = false;
        }

        // Initialize Tabs
        llTabFriends = view.findViewById(R.id.ll_tab_friends);
        llTabLeaderboard = view.findViewById(R.id.ll_tab_leaderboard);
        llTabSearch = view.findViewById(R.id.ll_tab_search);
        llTabDuels = view.findViewById(R.id.ll_tab_duels);

        tvTabFriends = view.findViewById(R.id.tv_tab_friends);
        tvTabLeaderboard = view.findViewById(R.id.tv_tab_leaderboard);
        tvTabSearch = view.findViewById(R.id.tv_tab_search);
        tvTabDuels = view.findViewById(R.id.tv_tab_duels);

        // Initialize Content Containers
        viewFriendsContent = view.findViewById(R.id.nsv_friends_content);
        viewLeaderboardContent = view.findViewById(R.id.nsv_leaderboard_content);
        viewSearchContent = view.findViewById(R.id.nsv_search_content);
        viewDuelsContent = view.findViewById(R.id.nsv_duels_content);
        
        llFriendsList = view.findViewById(R.id.ll_friends_list);
        llLeaderboardList = view.findViewById(R.id.ll_leaderboard_list);
        llSearchList = view.findViewById(R.id.ll_suggested_list);
        llDuelList = view.findViewById(R.id.ll_duel_history_list);
        etSearch = view.findViewById(R.id.et_search_friends);

        setupTabs();
        setupSearch();
        
        if (isFirebaseAvailable) {
            loadCurrentUser();
            loadLeaderboard();
            loadFriends();
            loadActiveDuels();
        }

        view.findViewById(R.id.btn_start_new_duel).setOnClickListener(v -> selectTab(2));

        return view;
    }

    private void loadCurrentUser() {
        if (currentUid == null || mDatabase == null) return;
        mDatabase.child("users").child(currentUid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                currentUser = snapshot.getValue(User.class);
                if (currentUser != null) currentUser.setUid(snapshot.getKey());
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void setupTabs() {
        if (llTabFriends != null) llTabFriends.setOnClickListener(v -> selectTab(0));
        if (llTabLeaderboard != null) llTabLeaderboard.setOnClickListener(v -> selectTab(1));
        if (llTabSearch != null) llTabSearch.setOnClickListener(v -> selectTab(2));
        if (llTabDuels != null) llTabDuels.setOnClickListener(v -> selectTab(3));
        selectTab(0);
    }

    private void selectTab(int index) {
        if (getContext() == null) return;
        resetTab(llTabFriends, tvTabFriends);
        resetTab(llTabLeaderboard, tvTabLeaderboard);
        resetTab(llTabSearch, tvTabSearch);
        resetTab(llTabDuels, tvTabDuels);

        if (viewFriendsContent != null) viewFriendsContent.setVisibility(View.GONE);
        if (viewLeaderboardContent != null) viewLeaderboardContent.setVisibility(View.GONE);
        if (viewSearchContent != null) viewSearchContent.setVisibility(View.GONE);
        if (viewDuelsContent != null) viewDuelsContent.setVisibility(View.GONE);

        switch (index) {
            case 0: setSelectedTab(llTabFriends, tvTabFriends); if (viewFriendsContent != null) viewFriendsContent.setVisibility(View.VISIBLE); break;
            case 1: setSelectedTab(llTabLeaderboard, tvTabLeaderboard); if (viewLeaderboardContent != null) viewLeaderboardContent.setVisibility(View.VISIBLE); break;
            case 2: setSelectedTab(llTabSearch, tvTabSearch); if (viewSearchContent != null) viewSearchContent.setVisibility(View.VISIBLE); break;
            case 3: setSelectedTab(llTabDuels, tvTabDuels); if (viewDuelsContent != null) viewDuelsContent.setVisibility(View.VISIBLE); break;
        }
    }

    private void setupSearch() {
        if (etSearch == null) return;
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { searchUsers(s.toString().trim()); }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void searchUsers(String queryText) {
        if (llSearchList == null) return;
        if (queryText.isEmpty() || mDatabase == null) { llSearchList.removeAllViews(); return; }
        Query query = mDatabase.child("users").orderByChild("username").startAt(queryText).endAt(queryText + "\uf8ff").limitToFirst(10);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                llSearchList.removeAllViews();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    User user = ds.getValue(User.class);
                    if (user != null) {
                        user.setUid(ds.getKey());
                        if (currentUid != null && !user.getUid().equals(currentUid)) {
                            addSearchItem(user);
                        }
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void addSearchItem(User user) {
        if (llSearchList == null) return;
        View v = getLayoutInflater().inflate(R.layout.item_user_search, llSearchList, false);
        TextView tvAvatar = v.findViewById(R.id.tv_avatar_emoji);
        TextView tvName = v.findViewById(R.id.tv_name);
        TextView tvMutual = v.findViewById(R.id.tv_mutual);
        
        if (tvAvatar != null) tvAvatar.setText(user.getAvatarUrl() != null ? user.getAvatarUrl() : "👤");
        if (tvName != null) tvName.setText(user.getUsername() != null ? user.getUsername() : "User");
        
        String handle = user.getUsername() != null ? "@" + user.getUsername().toLowerCase() : "";
        if (tvMutual != null) tvMutual.setText(handle);
        
        TextView btnAdd = v.findViewById(R.id.btn_add);
        if (btnAdd != null) {
            if (friendUids.contains(user.getUid())) {
                btnAdd.setText("Unfriend");
                btnAdd.setBackgroundResource(R.drawable.shape_skill_card);
                btnAdd.setOnClickListener(view -> unfriend(user));
            } else {
                btnAdd.setText("+ Add");
                btnAdd.setBackgroundResource(R.drawable.shape_btn_green_3d);
                btnAdd.setOnClickListener(view -> addFriend(user, btnAdd));
            }
        }
        
        llSearchList.addView(v);
    }

    private void addFriend(User friend, TextView btnAdd) {
        if (currentUid == null || friend.getUid() == null || mDatabase == null) return;
        
        if (btnAdd != null) {
            btnAdd.setText("Added!");
            btnAdd.setEnabled(false);
            btnAdd.setAlpha(0.7f);
            btnAdd.setBackgroundResource(R.drawable.shape_skill_card);
        }

        mDatabase.child("users").child(currentUid).child("friends").child(friend.getUid()).setValue(true)
                .addOnSuccessListener(aVoid -> {
                    if (isAdded() && getContext() != null) {
                        String name = friend.getUsername() != null ? friend.getUsername() : "Friend";
                        Toast.makeText(getContext(), "Added " + name, Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded() && btnAdd != null) {
                        btnAdd.setText("+ Add");
                        btnAdd.setEnabled(true);
                        btnAdd.setAlpha(1.0f);
                        btnAdd.setBackgroundResource(R.drawable.shape_btn_green_3d);
                    }
                });
    }

    private void loadFriends() {
        if (currentUid == null || mDatabase == null) return;
        mDatabase.child("users").child(currentUid).child("friends").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                friendUids.clear();
                if (llFriendsList != null) llFriendsList.removeAllViews();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String fUid = ds.getKey();
                    if (fUid != null) {
                        friendUids.add(fUid);
                        fetchFriendDetails(fUid);
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void fetchFriendDetails(String friendUid) {
        if (mDatabase == null) return;
        mDatabase.child("users").child(friendUid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                User user = snapshot.getValue(User.class);
                if (user != null) {
                    user.setUid(snapshot.getKey());
                    addFriendCard(user);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void addFriendCard(User friend) {
        if (llFriendsList == null) return;
        View v = getLayoutInflater().inflate(R.layout.item_friend_card, llFriendsList, false);
        
        TextView tvAvatar = v.findViewById(R.id.tv_avatar);
        TextView tvName = v.findViewById(R.id.tv_name);
        TextView tvStreak = v.findViewById(R.id.tv_streak);
        TextView tvXp = v.findViewById(R.id.tv_xp);
        
        if (tvAvatar != null) tvAvatar.setText(friend.getAvatarUrl() != null ? friend.getAvatarUrl() : "👤");
        if (tvName != null) tvName.setText(friend.getUsername() != null ? friend.getUsername() : "User");
        if (tvStreak != null) tvStreak.setText("🔥 " + friend.getStreak() + "d");
        if (tvXp != null) tvXp.setText("⭐ " + friend.getTotalPoints());
        
        TextView btnNudge = v.findViewById(R.id.btn_nudge);
        if (btnNudge != null) btnNudge.setOnClickListener(view -> sendNudge(friend, btnNudge));

        View btnUnfriend = v.findViewById(R.id.btn_unfriend);
        if (btnUnfriend != null) {
            btnUnfriend.setOnClickListener(view -> {
                // STOP PROPAGATION to prevent card click crash
                unfriend(friend);
            });
        }
        
        View btnDuel = v.findViewById(R.id.btn_duel);
        if (btnDuel != null) {
            btnDuel.setOnClickListener(view -> {
                Intent intent = new Intent(getActivity(), StartDuelActivity.class);
                intent.putExtra("opponent_uid", friend.getUid());
                startActivity(intent);
            });
        }

        v.setOnClickListener(view -> {
            if (isAdded() && getActivity() != null && friend.getUid() != null) {
                // Double check they are still a friend before navigating
                if (friendUids.contains(friend.getUid())) {
                    Intent intent = new Intent(getActivity(), FriendProfileActivity.class);
                    intent.putExtra("friend_uid", friend.getUid());
                    startActivity(intent);
                }
            }
        });
        llFriendsList.addView(v);
    }

    private void unfriend(User friend) {
        if (currentUid == null || friend.getUid() == null || mDatabase == null) return;
        
        // Final safety check: Don't navigate if we are unfriending
        mDatabase.child("users").child(currentUid).child("friends").child(friend.getUid()).removeValue()
                .addOnSuccessListener(aVoid -> {
                    if (isAdded() && getContext() != null) {
                        String name = friend.getUsername() != null ? friend.getUsername() : "Friend";
                        Toast.makeText(getContext(), "Unfriended " + name, Toast.LENGTH_SHORT).show();
                    }
                    if (viewSearchContent != null && viewSearchContent.getVisibility() == View.VISIBLE && etSearch != null) {
                        searchUsers(etSearch.getText().toString().trim());
                    }
                });
    }

    private void sendNudge(User friend, TextView btnNudge) {
        if (currentUser == null || mDatabase == null || friend.getUid() == null) return;

        if (btnNudge != null) {
            btnNudge.setText("✅ Sent!");
            btnNudge.setEnabled(false);
            btnNudge.setAlpha(0.7f);
            btnNudge.setBackgroundResource(R.drawable.shape_leaderboard_you);
        }

        Map<String, Object> notif = new HashMap<>();
        notif.put("type", "nudge");
        notif.put("message", (currentUser.getUsername() != null ? currentUser.getUsername() : "A friend") + " nudged you! Go practice! 👋");
        notif.put("timestamp", ServerValue.TIMESTAMP);
        
        mDatabase.child("users").child(friend.getUid()).child("notifications").push().setValue(notif);
    }

    private void loadLeaderboard() {
        if (mDatabase == null) return;
        mDatabase.child("users").orderByChild("totalPoints").limitToLast(20)
                .addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                if (llLeaderboardList != null) llLeaderboardList.removeAllViews();
                List<User> users = new ArrayList<>();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    User u = ds.getValue(User.class);
                    if (u != null) {
                        u.setUid(ds.getKey());
                        users.add(u);
                    }
                }
                Collections.reverse(users);
                int rank = 1;
                for (User user : users) addLeaderboardItem(user, rank++);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void addLeaderboardItem(User user, int rank) {
        if (llLeaderboardList == null) return;
        View v = getLayoutInflater().inflate(R.layout.item_leaderboard, llLeaderboardList, false);
        
        TextView tvRank = v.findViewById(R.id.tv_rank_number);
        TextView tvAvatar = v.findViewById(R.id.tv_avatar_emoji);
        TextView tvName = v.findViewById(R.id.tv_name);
        TextView tvScore = v.findViewById(R.id.tv_score);
        
        if (tvRank != null) tvRank.setText(String.valueOf(rank));
        if (tvAvatar != null) tvAvatar.setText(user.getAvatarUrl() != null ? user.getAvatarUrl() : "👤");
        
        boolean isMe = currentUid != null && user.getUid() != null && user.getUid().equals(currentUid);
        if (tvName != null) tvName.setText(isMe ? "You" : (user.getUsername() != null ? user.getUsername() : "User"));
        if (tvScore != null) tvScore.setText(String.valueOf(user.getTotalPoints()));
        
        View badge = v.findViewById(R.id.tv_you_badge);
        if (badge != null) badge.setVisibility(isMe ? View.VISIBLE : View.GONE);
        llLeaderboardList.addView(v);
    }

    private void loadActiveDuels() {
        if (currentUid == null || mDatabase == null) return;
        mDatabase.child("users").child(currentUid).child("activeDuels").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                if (llDuelList != null) llDuelList.removeAllViews();
                for (DataSnapshot ds : snapshot.getChildren()) fetchDuelDetails(ds.getKey());
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void fetchDuelDetails(String duelId) {
        if (mDatabase == null) return;
        mDatabase.child("duels").child(duelId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                Duel duel = snapshot.getValue(Duel.class);
                if (duel != null && "active".equals(duel.getStatus())) {
                    duel.setDuelId(snapshot.getKey());
                    addDuelItem(duel);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void addDuelItem(Duel duel) {
        if (llDuelList == null) return;
        View v = getLayoutInflater().inflate(R.layout.item_duel_history, llDuelList, false);
        
        TextView tvTitle = v.findViewById(R.id.tv_title);
        TextView tvSubtitle = v.findViewById(R.id.tv_subtitle);
        TextView tvYourScore = v.findViewById(R.id.tv_your_score);
        TextView tvTheirScore = v.findViewById(R.id.tv_their_score);
        TextView tvAvatar = v.findViewById(R.id.tv_avatar);

        if (tvTitle != null) tvTitle.setText("⚔️ Duel: " + (duel.getSkillName() != null ? duel.getSkillName() : "Skill"));
        
        String opponentUid = (currentUid != null && currentUid.equals(duel.getInitiatorUid())) ? duel.getOpponentUid() : duel.getInitiatorUid();
        if (opponentUid != null && mDatabase != null) {
            mDatabase.child("users").child(opponentUid).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (!isAdded()) return;
                    User opp = snapshot.getValue(User.class);
                    if (opp != null) {
                        if (tvSubtitle != null) tvSubtitle.setText("vs " + (opp.getUsername() != null ? opp.getUsername() : "User"));
                        if (tvAvatar != null) tvAvatar.setText(opp.getAvatarUrl() != null ? opp.getAvatarUrl() : "👤");
                    }
                }
                @Override public void onCancelled(@NonNull DatabaseError error) {}
            });
        }

        int myGain, oppGain;
        if (currentUid != null && currentUid.equals(duel.getInitiatorUid())) {
            myGain = duel.getInitiatorCurrentMastery() - duel.getInitiatorStartMastery();
            oppGain = duel.getOpponentCurrentMastery() - duel.getOpponentStartMastery();
        } else {
            myGain = duel.getOpponentCurrentMastery() - duel.getOpponentStartMastery();
            oppGain = duel.getInitiatorCurrentMastery() - duel.getInitiatorStartMastery();
        }

        if (tvYourScore != null) tvYourScore.setText("You +" + myGain + "%");
        if (tvTheirScore != null) tvTheirScore.setText("Opponent +" + oppGain + "%");

        v.setOnClickListener(view -> {
            Intent intent = new Intent(getActivity(), ActiveDuelActivity.class);
            intent.putExtra("duel_id", duel.getDuelId());
            startActivity(intent);
        });

        llDuelList.addView(v);
    }

    private void resetTab(LinearLayout layout, TextView text) {
        if (layout != null) layout.setBackground(null);
        if (text != null && getContext() != null) text.setTextColor(ContextCompat.getColor(getContext(), R.color.splash_text_secondary));
    }

    private void setSelectedTab(LinearLayout layout, TextView text) {
        if (layout != null) layout.setBackgroundResource(R.drawable.shape_skill_card);
        if (text != null && getContext() != null) text.setTextColor(ContextCompat.getColor(getContext(), R.color.splash_green));
    }
}
