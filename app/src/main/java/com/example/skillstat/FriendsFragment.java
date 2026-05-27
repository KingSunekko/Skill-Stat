package com.example.skillstat;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

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
import com.google.firebase.database.MutableData;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class FriendsFragment extends Fragment {

    private static final String TAG = "FriendsFragment";
    private LinearLayout llTabFriends, llTabLeaderboard, llTabSearch, llTabDuels;
    private View llTabChat;
    private TextView tvTabFriends, tvTabLeaderboard, tvTabSearch, tvTabDuels, tvTabChat, tvChatTabBadge;
    private View viewFriendsContent, viewLeaderboardContent, viewSearchContent, viewDuelsContent, viewChatContent;
    private LinearLayout llFriendsList, llLeaderboardList, llSearchList, llDuelList, llRequestsList, llChatList;
    private LinearLayout llActiveDuelsSummary;
    private TextView tvRequestsHeader, tvActiveDuelsSummaryHeader;
    private EditText etSearch;
    private DatabaseReference mDatabase;
    private String currentUid;
    private User currentUser;
    private final Set<String> friendUids = new HashSet<>();
    private final Set<String> sentRequestUids = new HashSet<>();

    private View cardNudgeAlert;
    private TextView tvNudgeTitle, tvNudgeMessage, btnNudgePractice, btnNudgeDismiss;

    private ValueEventListener recentChatsListener;
    private int chatsLoadCount = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_friends, container, false);

        try {
            mDatabase = FirebaseDatabase.getInstance().getReference();
            currentUid = FirebaseAuth.getInstance().getUid();
        } catch (Exception e) {
            Log.e(TAG, "Firebase unavailable", e);
        }

        llTabFriends = view.findViewById(R.id.ll_tab_friends);
        llTabLeaderboard = view.findViewById(R.id.ll_tab_leaderboard);
        llTabSearch = view.findViewById(R.id.ll_tab_search);
        llTabDuels = view.findViewById(R.id.ll_tab_duels);
        llTabChat = view.findViewById(R.id.ll_tab_chat);

        tvTabFriends = view.findViewById(R.id.tv_tab_friends);
        tvTabLeaderboard = view.findViewById(R.id.tv_tab_leaderboard);
        tvTabSearch = view.findViewById(R.id.tv_tab_search);
        tvTabDuels = view.findViewById(R.id.tv_tab_duels);
        tvTabChat = view.findViewById(R.id.tv_tab_chat);
        tvChatTabBadge = view.findViewById(R.id.tv_chat_tab_badge);

        viewFriendsContent = view.findViewById(R.id.nsv_friends_content);
        viewLeaderboardContent = view.findViewById(R.id.nsv_leaderboard_content);
        viewSearchContent = view.findViewById(R.id.nsv_search_content);
        viewDuelsContent = view.findViewById(R.id.nsv_duels_content);
        viewChatContent = view.findViewById(R.id.nsv_chat_content);

        llFriendsList = view.findViewById(R.id.ll_friends_list);
        llRequestsList = view.findViewById(R.id.ll_requests_list);
        tvRequestsHeader = view.findViewById(R.id.tv_requests_header);
        llLeaderboardList = view.findViewById(R.id.ll_leaderboard_list);
        llSearchList = view.findViewById(R.id.ll_suggested_list);
        llDuelList = view.findViewById(R.id.ll_duel_history_list);
        llChatList = view.findViewById(R.id.ll_chat_list);
        llActiveDuelsSummary = view.findViewById(R.id.ll_active_duels_summary);
        tvActiveDuelsSummaryHeader = view.findViewById(R.id.tv_active_duels_summary_header);
        etSearch = view.findViewById(R.id.et_search_friends);

        cardNudgeAlert = view.findViewById(R.id.card_nudge_alert);
        tvNudgeTitle = view.findViewById(R.id.tv_nudge_title);
        tvNudgeMessage = view.findViewById(R.id.tv_nudge_message);
        btnNudgePractice = view.findViewById(R.id.btn_nudge_practice);
        btnNudgeDismiss = view.findViewById(R.id.btn_nudge_dismiss);

        setupTabs();
        setupSearch();

        if (mDatabase != null && currentUid != null) {
            loadCurrentUser();
            loadLeaderboard();
            loadFriends();
            loadSentRequests();
            loadDuels();
            loadIncomingRequests();
            listenForNotifications();
            listenForUnreadMessages();
        }

        View btnAddHeader = view.findViewById(R.id.btn_add_friend_header);
        if (btnAddHeader != null) {
            btnAddHeader.setOnClickListener(v -> selectTab(2));
        }

        View btnDuel = view.findViewById(R.id.btn_start_new_duel);
        if (btnDuel != null) btnDuel.setOnClickListener(v -> selectTab(2));

        return view;
    }

    private void listenForUnreadMessages() {
        if (currentUid == null || mDatabase == null) return;
        mDatabase.child("users").child(currentUid).child("recentChats").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                int totalUnread = 0;
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Integer unread = ds.child("unreadCount").getValue(Integer.class);
                    if (unread != null) totalUnread += unread;
                }
                if (totalUnread > 0) {
                    tvChatTabBadge.setVisibility(View.VISIBLE);
                    tvChatTabBadge.setText(String.valueOf(totalUnread));
                } else {
                    tvChatTabBadge.setVisibility(View.GONE);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void listenForNotifications() {
        if (currentUid == null || mDatabase == null) return;
        mDatabase.child("users").child(currentUid).child("notifications").addValueEventListener(new ValueEventListener() {
            @Override
            @SuppressWarnings("unchecked")
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                if (snapshot.getChildrenCount() == 0 && cardNudgeAlert != null && cardNudgeAlert.getVisibility() == View.VISIBLE) {
                    cardNudgeAlert.animate().alpha(0).translationX(-100).setDuration(300).withEndAction(() -> cardNudgeAlert.setVisibility(View.GONE)).start();
                }
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Map<String, Object> notif = (Map<String, Object>) ds.getValue();
                    if (notif != null && "nudge".equals(notif.get("type"))) {
                        showNudgeBanner((String) notif.get("message"), ds.getKey());
                        return;
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void showNudgeBanner(String message, String notifId) {
        if (cardNudgeAlert != null && isAdded() && getContext() != null) {
            if (cardNudgeAlert.getVisibility() != View.VISIBLE) {
                cardNudgeAlert.setVisibility(View.VISIBLE);
                cardNudgeAlert.setAlpha(0);
                cardNudgeAlert.setTranslationX(100);
                cardNudgeAlert.animate().alpha(1).translationX(0).setDuration(500).setInterpolator(new DecelerateInterpolator()).start();
            }
            tvNudgeTitle.setText("Friend Nudge!");
            tvNudgeMessage.setText(message);
            btnNudgePractice.setOnClickListener(v -> { dismissNotification(notifId); navigateToPractice("General Practice"); });
            btnNudgeDismiss.setOnClickListener(v -> dismissNotification(notifId));
        }
    }

    private void dismissNotification(String notifId) {
        if (currentUid != null && mDatabase != null && notifId != null) {
            mDatabase.child("users").child(currentUid).child("notifications").child(notifId).removeValue();
            if (cardNudgeAlert != null) cardNudgeAlert.animate().alpha(0).translationX(-100).setDuration(300).withEndAction(() -> cardNudgeAlert.setVisibility(View.GONE)).start();
        }
    }

    private void navigateToPractice(String skillName) {
        if (!isAdded()) return;
        PracticeFragment fragment = new PracticeFragment();
        Bundle args = new Bundle();
        args.putString("skill_name", skillName);
        fragment.setArguments(args);
        getParentFragmentManager().beginTransaction().setCustomAnimations(R.anim.gentle_fade_in, R.anim.gentle_fade_out).replace(R.id.fragment_container, fragment).addToBackStack(null).commit();
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
        if (llTabChat != null) llTabChat.setOnClickListener(v -> selectTab(4));
        selectTab(0);
    }

    private void selectTab(int index) {
        if (!isAdded() || getContext() == null) return;
        resetTab(llTabFriends, tvTabFriends);
        resetTab(llTabLeaderboard, tvTabLeaderboard);
        resetTab(llTabSearch, tvTabSearch);
        resetTab(llTabDuels, tvTabDuels);
        resetTab(llTabChat, tvTabChat);

        View currentView = null;
        if (viewFriendsContent.getVisibility() == View.VISIBLE) currentView = viewFriendsContent;
        else if (viewLeaderboardContent.getVisibility() == View.VISIBLE) currentView = viewLeaderboardContent;
        else if (viewSearchContent.getVisibility() == View.VISIBLE) currentView = viewSearchContent;
        else if (viewDuelsContent.getVisibility() == View.VISIBLE) currentView = viewDuelsContent;
        else if (viewChatContent.getVisibility() == View.VISIBLE) currentView = viewChatContent;

        if (currentView != null) {
            currentView.animate().alpha(0).setDuration(200).withEndAction(() -> {
                if (isAdded()) {
                    viewFriendsContent.setVisibility(View.GONE); viewLeaderboardContent.setVisibility(View.GONE);
                    viewSearchContent.setVisibility(View.GONE); viewDuelsContent.setVisibility(View.GONE);
                    viewChatContent.setVisibility(View.GONE);
                    showSelectedTabContent(index);
                }
            }).start();
        } else {
            showSelectedTabContent(index);
        }
    }

    private void showSelectedTabContent(int index) {
        View target = null;
        switch (index) {
            case 0: setSelectedTab(llTabFriends, tvTabFriends); target = viewFriendsContent; break;
            case 1: setSelectedTab(llTabLeaderboard, tvTabLeaderboard); target = viewLeaderboardContent; break;
            case 2: setSelectedTab(llTabSearch, tvTabSearch); target = viewSearchContent; break;
            case 3: setSelectedTab(llTabDuels, tvTabDuels); target = viewDuelsContent; break;
            case 4: setSelectedTab(llTabChat, tvTabChat); target = viewChatContent; loadChats(); break;
        }
        if (target != null) {
            target.setVisibility(View.VISIBLE);
            target.setAlpha(0);
            target.animate().alpha(1).setDuration(300).start();
        }
    }

    private void loadChats() {
        if (!isAdded() || llChatList == null) return;
        if (currentUid == null) currentUid = FirebaseAuth.getInstance().getUid();
        if (currentUid == null) return;

        if (recentChatsListener != null) {
            mDatabase.child("users").child(currentUid).child("recentChats").removeEventListener(recentChatsListener);
        }

        recentChatsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                final int currentRun = ++chatsLoadCount;
                llChatList.removeAllViews();

                if (!snapshot.exists() || snapshot.getChildrenCount() == 0) {
                    TextView tv = new TextView(getContext());
                    tv.setText("No recent chats");
                    tv.setTextColor(0x88FFFFFF);
                    tv.setPadding(0, 40, 0, 0);
                    tv.setGravity(Gravity.CENTER);
                    llChatList.addView(tv);
                    return;
                }

                List<DataSnapshot> chatList = new ArrayList<>();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    chatList.add(ds);
                }

                Collections.sort(chatList, (o1, o2) -> {
                    Long t1 = o1.child("lastMessageTimestamp").getValue(Long.class);
                    Long t2 = o2.child("lastMessageTimestamp").getValue(Long.class);
                    if (t1 == null) t1 = 0L;
                    if (t2 == null) t2 = 0L;
                    return t2.compareTo(t1);
                });

                int index = 0;
                for (DataSnapshot chatDs : chatList) {
                    String otherUid = chatDs.getKey();
                    if (otherUid == null) continue;
                    final int stagger = index++;
                    
                    mDatabase.child("users").child(otherUid).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot userSnapshot) {
                            if (!isAdded() || currentRun != chatsLoadCount) return;
                            User otherUser = userSnapshot.getValue(User.class);
                            if (otherUser != null) {
                                otherUser.setUid(userSnapshot.getKey());
                                addChatItem(otherUser, stagger);
                            }
                        }
                        @Override public void onCancelled(@NonNull DatabaseError error) {}
                    });
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        };
        mDatabase.child("users").child(currentUid).child("recentChats").addValueEventListener(recentChatsListener);
    }

    private void addChatItem(User friend, int index) {
        if (!isAdded() || llChatList == null) return;
        View v = getLayoutInflater().inflate(R.layout.item_chat, llChatList, false);
        ((TextView) v.findViewById(R.id.tv_name)).setText(friend.getUsername());

        int resId = getResources().getIdentifier(friend.getAvatarUrl(), "drawable", getContext().getPackageName());
        ((ImageView) v.findViewById(R.id.iv_avatar)).setImageResource(resId != 0 ? resId : R.drawable.prof1);

        mDatabase.child("users").child(currentUid).child("recentChats").child(friend.getUid()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                if (snapshot.exists()) {
                    String lastMsg = snapshot.child("lastMessage").getValue(String.class);
                    ((TextView) v.findViewById(R.id.tv_last_message)).setText(lastMsg != null ? lastMsg : "Photo sent");

                    Long timestamp = snapshot.child("lastMessageTimestamp").getValue(Long.class);
                    if (timestamp != null) {
                        SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.getDefault());
                        ((TextView) v.findViewById(R.id.tv_time)).setText(sdf.format(new Date(timestamp)));
                    }

                    Integer unread = snapshot.child("unreadCount").getValue(Integer.class);
                    TextView tvUnread = v.findViewById(R.id.tv_unread_count);
                    if (unread != null && unread > 0) {
                        tvUnread.setVisibility(View.VISIBLE);
                        tvUnread.setText(String.valueOf(unread));
                    } else {
                        tvUnread.setVisibility(View.GONE);
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });

        mDatabase.child("users").child(friend.getUid()).child("online").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                Boolean isOnline = snapshot.getValue(Boolean.class);
                View vOnline = v.findViewById(R.id.v_online_status);
                if (vOnline != null) vOnline.setVisibility(isOnline != null && isOnline ? View.VISIBLE : View.GONE);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });

        v.setOnClickListener(view -> {
            Intent intent = new Intent(getActivity(), ChatActivity.class);
            intent.putExtra("receiver_uid", friend.getUid());
            intent.putExtra("receiver_name", friend.getUsername());
            intent.putExtra("receiver_avatar", friend.getAvatarUrl());
            startActivity(intent);
        });

        v.setAlpha(0);
        v.setTranslationY(30);
        v.animate().alpha(1).translationY(0).setDuration(400).setStartDelay(index * 50L).start();
        llChatList.addView(v);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (recentChatsListener != null && currentUid != null && mDatabase != null) {
            mDatabase.child("users").child(currentUid).child("recentChats").removeEventListener(recentChatsListener);
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
        if (!isAdded() || llSearchList == null || queryText.isEmpty()) { if (llSearchList != null) llSearchList.removeAllViews(); return; }
        mDatabase.child("users").orderByChild("username").startAt(queryText).endAt(queryText + "\uf8ff").limitToFirst(10).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                llSearchList.removeAllViews(); int index = 0;
                for (DataSnapshot ds : snapshot.getChildren()) {
                    User u = ds.getValue(User.class);
                    if (u != null) { u.setUid(ds.getKey()); if (currentUid != null && !u.getUid().equals(currentUid)) addSearchItem(u, index++); }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void addSearchItem(User user, int index) {
        if (!isAdded() || llSearchList == null) return;
        View v = getLayoutInflater().inflate(R.layout.item_user_search, llSearchList, false);
        ((TextView) v.findViewById(R.id.tv_name)).setText(user.getUsername());
        ((TextView) v.findViewById(R.id.tv_avatar_emoji)).setText(user.getAvatarEmoji());
        ((TextView) v.findViewById(R.id.tv_mutual)).setText("@" + user.getUsername());

        TextView tvSkillInfo = v.findViewById(R.id.tv_skill_info);
        if (tvSkillInfo != null) {
            tvSkillInfo.setVisibility(View.VISIBLE);
            tvSkillInfo.setText(user.getRankName() + " " + user.getRankEmoji() + " • Lv." + user.getLevel());
        }

        TextView btnAdd = v.findViewById(R.id.btn_add);
        if (friendUids.contains(user.getUid())) { btnAdd.setText("Friends"); btnAdd.setEnabled(false); }
        else if (sentRequestUids.contains(user.getUid())) { btnAdd.setText("Requested"); btnAdd.setEnabled(false); }
        else btnAdd.setOnClickListener(view -> sendFriendRequest(user, btnAdd));
        v.setAlpha(0); v.setTranslationX(-50); v.animate().alpha(1).translationX(0).setDuration(400).setStartDelay(index * 50L).start();
        llSearchList.addView(v);
    }

    private void sendFriendRequest(User friend, TextView btnAdd) {
        if (currentUid == null || friend.getUid() == null || currentUser == null) return;
        btnAdd.setText("Requested"); btnAdd.setEnabled(false);
        mDatabase.child("users").child(currentUid).child("sentRequests").child(friend.getUid()).setValue(true);
        mDatabase.child("users").child(friend.getUid()).child("friendRequests").child(currentUid).setValue(true).addOnSuccessListener(aVoid -> {
            Map<String, Object> notif = new HashMap<>(); notif.put("type", "friend_request"); notif.put("message", currentUser.getUsername() + " sent you a friend request!"); notif.put("timestamp", ServerValue.TIMESTAMP);
            mDatabase.child("users").child(friend.getUid()).child("notifications").push().setValue(notif);
        });
    }

    private void loadSentRequests() {
        if (currentUid == null) return;
        mDatabase.child("users").child(currentUid).child("sentRequests").addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                sentRequestUids.clear(); for (DataSnapshot ds : snapshot.getChildren()) sentRequestUids.add(ds.getKey());
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadIncomingRequests() {
        if (currentUid == null) return;
        mDatabase.child("users").child(currentUid).child("friendRequests").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded() || llRequestsList == null) return;
                llRequestsList.removeAllViews();
                if (snapshot.exists() && snapshot.getChildrenCount() > 0) {
                    tvRequestsHeader.setVisibility(View.VISIBLE); int index = 0;
                    for (DataSnapshot ds : snapshot.getChildren()) fetchRequestDetails(ds.getKey(), index++);
                } else tvRequestsHeader.setVisibility(View.GONE);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void fetchRequestDetails(String uid, int index) {
        mDatabase.child("users").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                User r = snapshot.getValue(User.class);
                if (r != null) { r.setUid(snapshot.getKey()); addRequestCard(r, index); }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void addRequestCard(User r, int index) {
        if (!isAdded() || llRequestsList == null) return;
        View v = getLayoutInflater().inflate(R.layout.item_friend_card, llRequestsList, false);
        ((TextView) v.findViewById(R.id.tv_name)).setText(r.getUsername());
        ((TextView) v.findViewById(R.id.tv_handle)).setText("@" + r.getUsername() + " • Lv." + r.getLevel());
        ((TextView) v.findViewById(R.id.tv_streak)).setText("🔥 " + r.getStreak() + "d");
        ((TextView) v.findViewById(R.id.tv_xp)).setText("⭐ " + r.getTotalPoints());

        int resId = getResources().getIdentifier(r.getAvatarUrl(), "drawable", getContext().getPackageName());
        ((ImageView) v.findViewById(R.id.iv_avatar)).setImageResource(resId != 0 ? resId : R.drawable.prof1);

        v.findViewById(R.id.btn_duel).setVisibility(View.GONE);
        TextView btnAccept = v.findViewById(R.id.btn_nudge); btnAccept.setText("Accept"); btnAccept.setOnClickListener(view -> acceptFriendRequest(r, v));
        v.findViewById(R.id.btn_unfriend).setOnClickListener(view -> declineRequest(r, v));
        v.setAlpha(0); v.setScaleX(0.9f); v.animate().alpha(1).scaleX(1).setDuration(400).setStartDelay(index * 100L).start();
        llRequestsList.addView(v);
    }

    private void acceptFriendRequest(User r, View v) {
        if (currentUid == null) return;
        mDatabase.child("users").child(currentUid).child("friends").child(r.getUid()).setValue(true);
        mDatabase.child("users").child(r.getUid()).child("friends").child(currentUid).setValue(true);
        mDatabase.child("users").child(currentUid).child("friendRequests").child(r.getUid()).removeValue();
        mDatabase.child("users").child(r.getUid()).child("sentRequests").child(currentUid).removeValue();
        v.animate().alpha(0).translationX(200).setDuration(300).withEndAction(() -> llRequestsList.removeView(v)).start();
    }

    private void declineRequest(User r, View v) {
        if (currentUid == null) return;
        mDatabase.child("users").child(currentUid).child("friendRequests").child(r.getUid()).removeValue();
        mDatabase.child("users").child(r.getUid()).child("sentRequests").child(currentUid).removeValue();
        v.animate().alpha(0).translationX(-200).setDuration(300).withEndAction(() -> llRequestsList.removeView(v)).start();
    }

    private void loadFriends() {
        if (currentUid == null) return;
        mDatabase.child("users").child(currentUid).child("friends").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded() || llFriendsList == null) return;
                friendUids.clear(); llFriendsList.removeAllViews(); int index = 0;
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String fUid = ds.getKey();
                    if (fUid != null) { friendUids.add(fUid); fetchFriendDetails(fUid, index++); }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void fetchFriendDetails(String uid, int index) {
        mDatabase.child("users").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                User u = snapshot.getValue(User.class);
                if (u != null) { u.setUid(snapshot.getKey()); addFriendCard(u, index); }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void addFriendCard(User f, int index) {
        if (!isAdded() || llFriendsList == null) return;
        View v = getLayoutInflater().inflate(R.layout.item_friend_card, llFriendsList, false);
        ((TextView) v.findViewById(R.id.tv_name)).setText(f.getUsername());
        ((TextView) v.findViewById(R.id.tv_handle)).setText("@" + f.getUsername() + " • Lv." + f.getLevel());
        ((TextView) v.findViewById(R.id.tv_streak)).setText("🔥 " + f.getStreak() + "d");
        ((TextView) v.findViewById(R.id.tv_xp)).setText("⭐ " + f.getTotalPoints());

        int resId = getResources().getIdentifier(f.getAvatarUrl(), "drawable", getContext().getPackageName());
        ((ImageView) v.findViewById(R.id.iv_avatar)).setImageResource(resId != 0 ? resId : R.drawable.prof1);

        v.findViewById(R.id.btn_nudge).setOnClickListener(view -> sendNudge(f, (TextView) view));
        v.findViewById(R.id.btn_unfriend).setOnClickListener(view -> unfriend(f));
        v.findViewById(R.id.btn_duel).setOnClickListener(view -> {
            Intent i = new Intent(getActivity(), StartDuelActivity.class); i.putExtra("opponent_uid", f.getUid()); startActivity(i);
        });
        v.setOnClickListener(view -> {
            Intent i = new Intent(getActivity(), FriendProfileActivity.class); i.putExtra("friend_uid", f.getUid()); startActivity(i);
        });
        v.setAlpha(0); v.setTranslationY(50); v.animate().alpha(1).translationY(0).setDuration(400).setStartDelay(index * 100L).start();
        llFriendsList.addView(v);
    }

    private void unfriend(User f) {
        if (currentUid == null || f.getUid() == null) return;
        mDatabase.child("users").child(currentUid).child("friends").child(f.getUid()).removeValue();
        mDatabase.child("users").child(f.getUid()).child("friends").child(currentUid).removeValue();
    }

    private void sendNudge(User f, TextView btn) {
        if (currentUser == null || f.getUid() == null) return;
        btn.setText("Sent"); btn.setEnabled(false);
        Map<String, Object> notif = new HashMap<>(); notif.put("type", "nudge"); notif.put("message", currentUser.getUsername() + " nudged you to practice!"); notif.put("timestamp", ServerValue.TIMESTAMP);
        mDatabase.child("users").child(f.getUid()).child("notifications").push().setValue(notif);
    }

    private void loadLeaderboard() {
        mDatabase.child("users").orderByChild("totalPoints").limitToLast(20).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded() || llLeaderboardList == null) return;
                llLeaderboardList.removeAllViews(); List<User> users = new ArrayList<>();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    User u = ds.getValue(User.class); if (u != null) { u.setUid(ds.getKey()); users.add(u); }
                }
                Collections.reverse(users); int rank = 1;
                for (User u : users) addLeaderboardItem(u, rank++);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void addLeaderboardItem(User u, int rank) {
        if (!isAdded() || llLeaderboardList == null) return;
        View v = getLayoutInflater().inflate(R.layout.item_leaderboard, llLeaderboardList, false);

        TextView tvRank = v.findViewById(R.id.tv_rank_number);
        TextView tvName = v.findViewById(R.id.tv_name);
        TextView tvScore = v.findViewById(R.id.tv_score);
        TextView tvSubInfo = v.findViewById(R.id.tv_sub_info);
        ImageView ivAvatar = v.findViewById(R.id.iv_avatar);
        View youBadge = v.findViewById(R.id.tv_you_badge);

        if (tvRank != null) tvRank.setText(String.valueOf(rank));
        boolean isMe = currentUid != null && u.getUid().equals(currentUid);
        if (tvName != null) tvName.setText(isMe ? "You" : u.getUsername());
        if (tvScore != null) tvScore.setText(String.format(Locale.getDefault(), "%,d", u.getTotalPoints()));

        if (tvSubInfo != null) {
            tvSubInfo.setText("🔥 " + u.getStreak() + "d • ⭐ " + (u.getTotalPoints() % 1000 + 100) + " XP this week");
        }

        if (ivAvatar != null && getContext() != null) {
            String avatarName = u.getAvatarUrl();
            int resId = getResources().getIdentifier(avatarName, "drawable", getContext().getPackageName());
            ivAvatar.setImageResource(resId != 0 ? resId : R.drawable.prof1);
        }

        if (youBadge != null) youBadge.setVisibility(isMe ? View.VISIBLE : View.GONE);

        v.setAlpha(0); v.setTranslationY(20); v.animate().alpha(1).translationY(0).setDuration(300).setStartDelay(rank * 50L).start();
        llLeaderboardList.addView(v);
    }

    private void loadDuels() {
        if (currentUid == null) return;
        mDatabase.child("duels").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded() || llDuelList == null) return;
                llDuelList.removeAllViews(); if (llActiveDuelsSummary != null) llActiveDuelsSummary.removeAllViews();

                LinearLayout llPending = new LinearLayout(getContext()); llPending.setOrientation(LinearLayout.VERTICAL); llDuelList.addView(llPending);
                LinearLayout llActive = new LinearLayout(getContext()); llActive.setOrientation(LinearLayout.VERTICAL); llDuelList.addView(llActive);
                LinearLayout llCompleted = new LinearLayout(getContext()); llCompleted.setOrientation(LinearLayout.VERTICAL); llDuelList.addView(llCompleted);

                List<Duel> pending = new ArrayList<>(), active = new ArrayList<>(), completed = new ArrayList<>();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Duel d = ds.getValue(Duel.class);
                    if (d != null) {
                        d.setDuelId(ds.getKey());
                        if (!currentUid.equals(d.getInitiatorUid()) && !currentUid.equals(d.getOpponentUid())) continue;
                        if ("pending".equals(d.getStatus())) pending.add(d);
                        else if ("active".equals(d.getStatus())) {
                            if (System.currentTimeMillis() >= d.getEndTime()) autoSettleDuel(d);
                            active.add(d);
                        } else if ("completed".equals(d.getStatus())) completed.add(d);
                    }
                }
                int idx = 0;
                if (!pending.isEmpty()) { addDuelHeader(llPending, "PENDING CHALLENGES"); for (Duel d : pending) addDuelItem(d, false, llPending, idx++); }
                if (!active.isEmpty()) {
                    addDuelHeader(llActive, "ACTIVE DUELS"); tvActiveDuelsSummaryHeader.setVisibility(View.VISIBLE);
                    for (Duel d : active) { addDuelItem(d, true, llActive, idx++); addDuelItem(d, true, llActiveDuelsSummary, idx); }
                } else tvActiveDuelsSummaryHeader.setVisibility(View.GONE);
                if (!completed.isEmpty()) { addDuelHeader(llCompleted, "PAST RESULTS"); Collections.reverse(completed); for (Duel d : completed) addDuelItem(d, false, llCompleted, idx++); }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void autoSettleDuel(Duel duel) {
        mDatabase.child("duels").child(duel.getDuelId()).child("status").runTransaction(new Transaction.Handler() {
            @NonNull @Override public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                if ("active".equals(currentData.getValue(String.class))) { currentData.setValue("settling"); return Transaction.success(currentData); }
                return Transaction.abort();
            }
            @Override public void onComplete(DatabaseError error, boolean committed, DataSnapshot currentData) {
                if (committed) finalizeSettlement(duel);
            }
        });
    }

    private void finalizeSettlement(Duel duel) {
        double initEff = duel.getInitiatorEffort();
        double oppEff = duel.getOpponentEffort();
        boolean draw = initEff == oppEff;
        String win = draw ? "draw" : (initEff > oppEff ? duel.getInitiatorUid() : duel.getOpponentUid());
        String lose = draw ? null : (initEff > oppEff ? duel.getOpponentUid() : duel.getInitiatorUid());

        Map<String, Object> updates = new HashMap<>();
        updates.put("duels/" + duel.getDuelId() + "/status", "completed");
        updates.put("duels/" + duel.getDuelId() + "/winnerUid", win);

        if (!draw) {
            updates.put("users/" + win + "/totalPoints", ServerValue.increment(500 + (duel.getWagerAmount() * 2)));
            updates.put("users/" + win + "/wins", ServerValue.increment(1));
            updates.put("users/" + lose + "/losses", ServerValue.increment(1));
            updates.put("users/" + win + "/rivalry/" + lose + "/wins", ServerValue.increment(1));
            updates.put("users/" + lose + "/rivalry/" + win + "/losses", ServerValue.increment(1));
        } else {
            updates.put("users/" + duel.getInitiatorUid() + "/totalPoints", ServerValue.increment(250 + duel.getWagerAmount()));
            updates.put("users/" + duel.getOpponentUid() + "/totalPoints", ServerValue.increment(250 + duel.getWagerAmount()));
        }

        updates.put("users/" + duel.getInitiatorUid() + "/activeDuels/" + duel.getDuelId(), null);
        updates.put("users/" + duel.getOpponentUid() + "/activeDuels/" + duel.getDuelId(), null);
        updates.put("users/" + duel.getInitiatorUid() + "/completedDuels/" + duel.getDuelId(), true);
        updates.put("users/" + duel.getOpponentUid() + "/completedDuels/" + duel.getDuelId(), true);

        mDatabase.updateChildren(updates);
    }

    private void addDuelHeader(LinearLayout container, String title) {
        if (!isAdded()) return;
        TextView tv = new TextView(getContext()); tv.setText(title); tv.setTextColor(0xFF8E8E93); tv.setTextSize(11); tv.setPadding(0, 24, 0, 12); container.addView(tv);
    }

    private void addDuelItem(Duel duel, boolean isActive, LinearLayout container, int index) {
        if (!isAdded()) return;
        View v = getLayoutInflater().inflate(R.layout.item_duel_history, container, false);
        String oppUid = currentUid.equals(duel.getInitiatorUid()) ? duel.getOpponentUid() : duel.getInitiatorUid();
        mDatabase.child("users").child(oppUid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isAdded()) {
                    ((TextView) v.findViewById(R.id.tv_title)).setText("vs " + snapshot.child("username").getValue(String.class));
                    String avatar = snapshot.child("avatarUrl").getValue(String.class);
                    if (avatar == null) avatar = "prof1";
                    int resId = getResources().getIdentifier(avatar, "drawable", getContext().getPackageName());
                    ((ImageView) v.findViewById(R.id.iv_avatar)).setImageResource(resId != 0 ? resId : R.drawable.prof1);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });

        if ("pending".equals(duel.getStatus())) {
            ((TextView) v.findViewById(R.id.tv_subtitle)).setText("Waiting... (" + duel.getSkillName() + ")");
        } else {
            double myEff = currentUid.equals(duel.getInitiatorUid()) ? duel.getInitiatorEffort() : duel.getOpponentEffort();
            double oppEff = currentUid.equals(duel.getInitiatorUid()) ? duel.getOpponentEffort() : duel.getInitiatorEffort();
            ((TextView) v.findViewById(R.id.tv_your_score)).setText(String.format("You: %.1f", myEff));
            ((TextView) v.findViewById(R.id.tv_their_score)).setText(String.format("Them: %.1f", oppEff));
            if (isActive) ((TextView) v.findViewById(R.id.tv_subtitle)).setText(duel.getSkillName() + " Active");
            else {
                ((TextView) v.findViewById(R.id.tv_subtitle)).setText(duel.getSkillName() + " Completed");
                v.findViewById(R.id.ll_result_badge).setVisibility(View.VISIBLE);
                v.findViewById(R.id.tv_result_emoji).setVisibility(View.GONE);
                TextView tvRes = v.findViewById(R.id.tv_result_text);
                if (myEff > oppEff) tvRes.setText("WIN"); else if (oppEff > myEff) tvRes.setText("LOSS"); else tvRes.setText("DRAW");
            }
        }
        v.setOnClickListener(view -> { if (!"pending".equals(duel.getStatus())) { Intent i = new Intent(getActivity(), ActiveDuelActivity.class); i.putExtra("duel_id", duel.getDuelId()); startActivity(i); } });
        v.setAlpha(0); v.setTranslationX(50); v.animate().alpha(1).translationX(0).setDuration(400).setStartDelay(index * 50L).start();
        container.addView(v);
    }

    private void resetTab(View layout, TextView text) {
        if (layout != null) { layout.setBackground(null); layout.animate().scaleX(1.0f).setDuration(200).start(); }
        if (text != null && isAdded()) text.setTextColor(0xFF8E8E93);
    }

    private void setSelectedTab(View layout, TextView text) {
        if (layout != null) { layout.setBackgroundResource(R.drawable.shape_skill_card); layout.animate().scaleX(1.05f).setDuration(200).start(); }
        if (text != null && isAdded()) text.setTextColor(ContextCompat.getColor(getContext(), R.color.splash_green));
    }
}
