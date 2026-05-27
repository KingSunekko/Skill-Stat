package com.example.skillstat;

import android.os.Bundle;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.skillstat.models.PracticeSession;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class PracticeHistoryActivity extends AppCompatActivity {

    private LinearLayout llHistoryList;
    private DatabaseReference mDatabase;
    private String currentUid;
    private boolean hasAnimated = false;

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
                        List<DataSnapshot> sessionSnapshots = new ArrayList<>();

                        for (DataSnapshot ds : snapshot.getChildren()) {
                            sessionSnapshots.add(ds);
                        }

                        Collections.reverse(sessionSnapshots);

                        if (sessionSnapshots.isEmpty()) {
                            showEmptyPlaceholder();
                        } else {
                            int index = 0;
                            for (DataSnapshot ds : sessionSnapshots) {
                                PracticeSession session = ds.getValue(PracticeSession.class);
                                if (session != null) {
                                    addHistoryItem(session, ds.getKey(), index++);
                                }
                            }
                            hasAnimated = true;
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

    private void addHistoryItem(PracticeSession session, String sessionKey, int index) {
        View view = getLayoutInflater().inflate(R.layout.item_practice_history, llHistoryList, false);

        TextView tvEmoji = view.findViewById(R.id.tv_skill_emoji);
        TextView tvName = view.findViewById(R.id.tv_skill_name);
        TextView tvInfo = view.findViewById(R.id.tv_session_info);
        TextView tvArrow = view.findViewById(R.id.tv_expand_arrow);
        TextView tvZenIndicator = view.findViewById(R.id.tv_zen_indicator);
        View expandableSection = view.findViewById(R.id.ll_expandable_note);
        TextView tvNoteContent = view.findViewById(R.id.tv_note_content);
        TextView btnAddEditNote = view.findViewById(R.id.btn_add_edit_note);

        if (session.isZenMode()) {
            tvZenIndicator.setVisibility(View.VISIBLE);
        } else {
            tvZenIndicator.setVisibility(View.GONE);
        }

        // --- FIXED EMOJI & NAME LOGIC ---
        String fullSkillName = session.getSkillName();
        String displayName = fullSkillName;
        String emoji = "🎯"; // Default icon

        if (fullSkillName != null) {
            if (fullSkillName.equalsIgnoreCase("General Practice")) {
                emoji = "🛠️";
            } else if (fullSkillName.contains(" ")) {
                int lastSpaceIndex = fullSkillName.lastIndexOf(" ");
                String lastPart = fullSkillName.substring(lastSpaceIndex + 1).trim();

                if (isEmoji(lastPart)) {
                    emoji = lastPart;
                    displayName = fullSkillName.substring(0, lastSpaceIndex).trim();
                } else {
                    emoji = "⚡"; // Fallback if no emoji at the end
                }
            }
        }

        if (session.getMasteryAfter() >= 100) {
            emoji = "💎";
            tvName.setTextColor(0xFFD700);
        }

        tvEmoji.setText(emoji);
        tvName.setText(displayName);

        // --- FIXED PERCENTAGE FORMATTING (1 Decimal Place) ---
        CharSequence timeAgo = DateUtils.getRelativeTimeSpanString(session.getTimestamp(), System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS);
        String masteryText = "";
        if (session.getMasteryAfter() >= 0) {
            masteryText = String.format(Locale.getDefault(), " · %.1f%%", session.getMasteryAfter());
        }
        tvInfo.setText(timeAgo + " · " + session.getMinutes() + " min" + masteryText + " · +" + session.getXpEarned() + " XP");

        if (session.getNote() != null && !session.getNote().isEmpty()) {
            tvNoteContent.setText(session.getNote());
            view.findViewById(R.id.iv_note_indicator).setVisibility(View.VISIBLE);
            btnAddEditNote.setText("📝 EDIT NOTE");
        } else {
            tvNoteContent.setText("Session successfully completed!");
            btnAddEditNote.setText("📝 ADD NOTE");
        }

        view.setOnClickListener(v -> {
            boolean isVisible = expandableSection.getVisibility() == View.VISIBLE;
            if (!isVisible) {
                expandableSection.setVisibility(View.VISIBLE);
                expandableSection.setAlpha(0);
                expandableSection.setTranslationY(-20);
                expandableSection.animate().alpha(1).translationY(0).setDuration(300).start();
            } else {
                expandableSection.setVisibility(View.GONE);
            }
            tvArrow.setText(isVisible ? "▼" : "▲");
        });

        btnAddEditNote.setOnClickListener(v -> showEditNoteDialog(sessionKey, session.getNote()));

        if (!hasAnimated) {
            view.setAlpha(0);
            view.setTranslationX(50);
            view.animate().alpha(1).translationX(0).setDuration(400).setStartDelay(index * 50L).start();
        }

        llHistoryList.addView(view);
    }

    // Helper to check if a string is actually an emoji
    private boolean isEmoji(String str) {
        if (str == null || str.isEmpty()) return false;
        for (int i = 0; i < str.length(); ) {
            int codePoint = str.codePointAt(i);
            int type = Character.getType(codePoint);
            // If it's a letter or digit, it's not a standalone emoji icon
            if (type == Character.UPPERCASE_LETTER || type == Character.LOWERCASE_LETTER || type == Character.DECIMAL_DIGIT_NUMBER) {
                return false;
            }
            i += Character.charCount(codePoint);
        }
        return true;
    }

    private void showEditNoteDialog(String sessionKey, String currentNote) {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (20 * getResources().getDisplayMetrics().density);
        container.setPadding(padding, padding / 2, padding, 0);

        EditText etNote = new EditText(this);
        etNote.setText(currentNote);
        etNote.setHint("What did you achieve in this session?");
        etNote.setTextColor(getResources().getColor(R.color.white));
        etNote.setHintTextColor(0x88FFFFFF);
        container.addView(etNote);

        new MaterialAlertDialogBuilder(this, R.style.MaterialAlertDialog_Rounded)
                .setTitle("Session Note")
                .setView(container)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newNote = etNote.getText().toString().trim();
                    if (currentUid != null && sessionKey != null) {
                        mDatabase.child("users").child(currentUid).child("sessions").child(sessionKey).child("note")
                                .setValue(newNote)
                                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Note updated! ✨", Toast.LENGTH_SHORT).show());
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
