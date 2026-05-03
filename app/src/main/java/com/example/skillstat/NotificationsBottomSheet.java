package com.example.skillstat;

import android.annotation.SuppressLint;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class NotificationsBottomSheet extends BottomSheetDialogFragment {

    private LinearLayout llNotificationsContainer;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.layout_notifications_sheet, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        llNotificationsContainer = view.findViewById(R.id.ll_notifications_container);
        View btnMarkAllRead = view.findViewById(R.id.btn_mark_all_read);
        
        btnMarkAllRead.setOnClickListener(v -> {
            // Hide all unread dots with a simple fade animation
            for (int i = 0; i < llNotificationsContainer.getChildCount(); i++) {
                View item = llNotificationsContainer.getChildAt(i);
                View dot = item.findViewById(R.id.v_notif_dot);
                if (dot != null && dot.getVisibility() == View.VISIBLE) {
                    dot.animate().alpha(0f).setDuration(200).withEndAction(() -> dot.setVisibility(View.GONE)).start();
                }
            }
            
            // Clear the notification badge on the Home screen
            if (getActivity() != null) {
                View badge = getActivity().findViewById(R.id.tv_notif_badge);
                if (badge != null) {
                    badge.animate().scaleX(0f).scaleY(0f).setDuration(200).withEndAction(() -> badge.setVisibility(View.GONE)).start();
                }
            }
        });
        applyHoverEffect(btnMarkAllRead);

        populateNotifications();
    }

    private void populateNotifications() {
        llNotificationsContainer.removeAllViews();

        addNotificationItem("🚨", "Welcome back! Your skills miss you 👋", 
                "Java 💻 is at risk of decaying. Tap to practice now!", "now", "critical", true);

        addNotificationItem("🚨", "Skills Decaying Now!", 
                "Java 💻 is at critical risk. Practice now before you lose progress!", "now", "critical", true);

        addNotificationItem("💪", "Daily Practice Reminder", 
                "You haven't practiced today. Even 5 minutes keeps your skills sharp!", "5 min ago", "reminder", false);

        addNotificationItem("🧠", "Consistency Beats Intensity", 
                "Short daily sessions are 3x more effective than long weekly ones. Open SkillStat and practice now!", "1 hr ago", "info", false);
    }

    private void addNotificationItem(String icon, String title, String desc, String time, String type, boolean showAction) {
        View item = getLayoutInflater().inflate(R.layout.item_notification, llNotificationsContainer, false);

        TextView tvIcon = item.findViewById(R.id.tv_notif_icon);
        TextView tvTitle = item.findViewById(R.id.tv_notif_title);
        TextView tvDesc = item.findViewById(R.id.tv_notif_desc);
        TextView tvTime = item.findViewById(R.id.tv_notif_time);
        View vDot = item.findViewById(R.id.v_notif_dot);
        View btnAction = item.findViewById(R.id.btn_notif_action);
        View btnDismiss = item.findViewById(R.id.btn_notif_dismiss);

        tvIcon.setText(icon);
        tvTitle.setText(title);
        tvDesc.setText(desc);
        tvTime.setText(time);

        if ("critical".equals(type)) {
            item.setBackgroundResource(R.drawable.shape_notification_red);
            vDot.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FF4B4B")));
        } else if ("reminder".equals(type)) {
            item.setBackgroundResource(R.drawable.shape_notification_green);
            vDot.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#58CC02")));
        } else if ("info".equals(type)) {
            item.setBackgroundResource(R.drawable.shape_notification_blue);
            vDot.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#21B1FF")));
        }

        if (showAction) {
            btnAction.setVisibility(View.VISIBLE);
        } else {
            btnAction.setVisibility(View.GONE);
        }

        btnDismiss.setOnClickListener(v -> {
            item.animate().alpha(0f).translationX(100f).setDuration(200).withEndAction(() -> llNotificationsContainer.removeView(item)).start();
        });
        
        applyHoverEffect(item);
        applyHoverEffect(btnAction);
        applyHoverEffect(btnDismiss);

        llNotificationsContainer.addView(item);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void applyHoverEffect(View view) {
        view.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.animate().scaleX(0.97f).scaleY(0.97f).setDuration(100)
                            .setInterpolator(new DecelerateInterpolator()).start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(150)
                            .setInterpolator(new AccelerateInterpolator()).start();
                    break;
            }
            return false;
        });
    }

    @Override
    public int getTheme() {
        return R.style.CustomBottomSheetDialogTheme;
    }
}
