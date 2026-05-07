package com.example.skillstat;

import android.os.Bundle;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.skillstat.models.PracticeSession;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PracticeHistoryActivity extends AppCompatActivity {

    private LinearLayout llHistoryList;
    private DatabaseReference mDatabase;
    private String currentUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_practice_history);

        mDatabase = FirebaseDatabase.getInstance().getReference();
        currentUid = FirebaseAuth.getInstance().getUid();

        FrameLayout btnBack = findViewById(R.id.btn_back_history);
        btnBack.setOnClickListener(v -> finish());

        llHistoryList = findViewById(R.id.ll_history_list);

        loadPracticeHistory();
    }

    private void loadPracticeHistory() {
        if (currentUid == null) return;

        mDatabase.child("users").child(currentUid).child("sessions")
                .orderByChild("timestamp")
                .limitToLast(50)
                .addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                llHistoryList.removeAllViews();
                List<PracticeSession> sessions = new ArrayList<>();
                
                for (DataSnapshot ds : snapshot.getChildren()) {
                    PracticeSession session = ds.getValue(PracticeSession.class);
                    if (session != null) sessions.add(session);
                }

                // Show newest first
                Collections.reverse(sessions);

                if (sessions.isEmpty()) {
                    showEmptyPlaceholder();
                } else {
                    for (PracticeSession session : sessions) {
                        addHistoryItem(session);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("History", "Load failed", error.toException());
            }
        });
    }

    private void showEmptyPlaceholder() {
        TextView tv = new TextView(this);
        tv.setText("No practice sessions yet. Start your first one today!");
        tv.setTextColor(0x88FFFFFF);
        tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        tv.setPadding(0, 100, 0, 0);
        llHistoryList.addView(tv);
    }

    private void addHistoryItem(PracticeSession session) {
        View view = getLayoutInflater().inflate(R.layout.item_practice_history, llHistoryList, false);

        TextView tvEmoji = view.findViewById(R.id.tv_skill_emoji);
        TextView tvName = view.findViewById(R.id.tv_skill_name);
        TextView tvInfo = view.findViewById(R.id.tv_session_info);
        TextView tvArrow = view.findViewById(R.id.tv_expand_arrow);
        View expandableSection = view.findViewById(R.id.ll_expandable_note);
        TextView tvNoteContent = view.findViewById(R.id.tv_note_content);

        // Get emoji from name (simple logic or default)
        String emoji = session.getSkillName().contains(" ") ? session.getSkillName().split(" ")[1] : "⚡";
        tvEmoji.setText(emoji);
        tvName.setText(session.getSkillName());

        CharSequence timeAgo = DateUtils.getRelativeTimeSpanString(session.getTimestamp(), System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS);
        tvInfo.setText(timeAgo + " · " + session.getMinutes() + " min · +" + session.getXpEarned() + " XP");

        // Notes implementation could be added to the Practice screen later
        tvNoteContent.setText("Session successfully completed!");

        view.setOnClickListener(v -> {
            boolean isVisible = expandableSection.getVisibility() == View.VISIBLE;
            expandableSection.setVisibility(isVisible ? View.GONE : View.VISIBLE);
            tvArrow.setText(isVisible ? "▼" : "▲");
        });

        llHistoryList.addView(view);
    }
}
