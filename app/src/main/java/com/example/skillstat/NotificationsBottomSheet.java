package com.example.skillstat;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.skillstat.models.Duel;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Map;

public class NotificationsBottomSheet extends BottomSheetDialogFragment {

    private static final String TAG = "NotificationsBottomSheet";
    private LinearLayout llNotificationsContainer;
    private DatabaseReference mDatabase;
    private String currentUid;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.layout_notifications_sheet, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mDatabase = FirebaseDatabase.getInstance().getReference();
        currentUid = FirebaseAuth.getInstance().getUid();

        llNotificationsContainer = view.findViewById(R.id.ll_notifications_container);
        View btnMarkAllRead = view.findViewById(R.id.btn_mark_all_read);
        
        btnMarkAllRead.setOnClickListener(v -> markAllAsRead());
        applyHoverEffect(btnMarkAllRead);

        loadNotifications();
    }

    private void loadNotifications() {
        if (currentUid == null) return;

        mDatabase.child("users").child(currentUid).child("notifications")
                .addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded() || llNotificationsContainer == null) return;
                llNotificationsContainer.removeAllViews();

                if (!snapshot.exists()) {
                    showEmptyState();
                    return;
                }

                for (DataSnapshot ds : snapshot.getChildren()) {
                    Object data = ds.getValue();
                    if (data instanceof Map) {
                        Map<String, Object> notif = (Map<String, Object>) data;
                        addNotificationItemFromMap(ds.getKey(), notif);
                    } else if (data instanceof String) {
                        addSimpleNotification(ds.getKey(), (String) data);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Load failed", error.toException());
            }
        });
    }

    private void addNotificationItemFromMap(String key, Map<String, Object> notif) {
        String type = (String) notif.get("type");
        String message = (String) notif.get("message");
        
        View item = getLayoutInflater().inflate(R.layout.item_notification, llNotificationsContainer, false);
        TextView tvIcon = item.findViewById(R.id.tv_notif_icon);
        TextView tvTitle = item.findViewById(R.id.tv_notif_title);
        TextView tvDesc = item.findViewById(R.id.tv_notif_desc);
        TextView tvTime = item.findViewById(R.id.tv_notif_time);
        TextView btnAction = item.findViewById(R.id.btn_notif_action);
        TextView btnDismiss = item.findViewById(R.id.btn_notif_dismiss);

        if ("duel_invite".equals(type)) {
            tvIcon.setText("⚔️");
            tvTitle.setText("Duel Invitation");
            tvDesc.setText(message);
            btnAction.setText("Accept");
            btnDismiss.setText("Decline");
            
            String duelId = (String) notif.get("duelId");
            btnAction.setOnClickListener(v -> acceptDuel(key, duelId));
            btnDismiss.setOnClickListener(v -> declineDuel(key, duelId));
        } else if ("nudge".equals(type)) {
            tvIcon.setText("👋");
            tvTitle.setText("Friend Nudge");
            tvDesc.setText(message);
            btnAction.setText("Practice Now");
            btnAction.setOnClickListener(v -> {
                dismissNotification(key);
                dismiss();
                // Navigate to home practice
                Intent intent = new Intent(getActivity(), MainActivity.class);
                startActivity(intent);
            });
            btnDismiss.setOnClickListener(v -> dismissNotification(key));
        } else {
            tvIcon.setText("🔔");
            tvTitle.setText("Notification");
            tvDesc.setText(message);
            btnAction.setVisibility(View.GONE);
            btnDismiss.setOnClickListener(v -> dismissNotification(key));
        }

        tvTime.setText("just now");
        llNotificationsContainer.addView(item, 0);
    }

    private void addSimpleNotification(String key, String message) {
        View item = getLayoutInflater().inflate(R.layout.item_notification, llNotificationsContainer, false);
        ((TextView) item.findViewById(R.id.tv_notif_icon)).setText("🔔");
        ((TextView) item.findViewById(R.id.tv_notif_title)).setText("Alert");
        ((TextView) item.findViewById(R.id.tv_notif_desc)).setText(message);
        item.findViewById(R.id.btn_notif_action).setVisibility(View.GONE);
        item.findViewById(R.id.btn_notif_dismiss).setOnClickListener(v -> dismissNotification(key));
        llNotificationsContainer.addView(item, 0);
    }

    private void acceptDuel(String notifKey, String duelId) {
        mDatabase.child("duels").child(duelId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Duel duel = snapshot.getValue(Duel.class);
                if (duel != null) {
                    mDatabase.child("duels").child(duelId).child("status").setValue("active");
                    mDatabase.child("users").child(duel.getInitiatorUid()).child("activeDuels").child(duelId).setValue(true);
                    mDatabase.child("users").child(duel.getOpponentUid()).child("activeDuels").child(duelId).setValue(true);
                    
                    dismissNotification(notifKey);
                    Toast.makeText(getContext(), "Duel Started! ⚔️", Toast.LENGTH_SHORT).show();
                    
                    Intent intent = new Intent(getActivity(), ActiveDuelActivity.class);
                    intent.putExtra("duel_id", duelId);
                    startActivity(intent);
                    dismiss();
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void declineDuel(String notifKey, String duelId) {
        mDatabase.child("duels").child(duelId).removeValue();
        dismissNotification(notifKey);
    }

    private void dismissNotification(String key) {
        mDatabase.child("users").child(currentUid).child("notifications").child(key).removeValue();
    }

    private void markAllAsRead() {
        mDatabase.child("users").child(currentUid).child("notifications").removeValue();
    }

    private void showEmptyState() {
        TextView tv = new TextView(getContext());
        tv.setText("No new notifications");
        tv.setTextColor(0x88FFFFFF);
        tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        tv.setPadding(0, 50, 0, 50);
        llNotificationsContainer.addView(tv);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void applyHoverEffect(View view) {
        view.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) v.animate().scaleX(0.97f).scaleY(0.97f).setDuration(100).start();
            else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) 
                v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(150).start();
            return false;
        });
    }

    @Override
    public int getTheme() {
        return R.style.CustomBottomSheetDialogTheme;
    }
}
