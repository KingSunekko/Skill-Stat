package com.example.skillstat;

import android.animation.ValueAnimator;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnticipateOvershootInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.example.skillstat.models.User;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.slider.Slider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AddSkillFragment extends Fragment {

    private EditText etSkillName;
    private ChipGroup cgPopularSkills;
    private View llBeginner, llIntermediate, llAdvanced;
    private View ivBeginnerCheck, ivIntermediateCheck, ivAdvancedCheck;
    private AppCompatButton btnAddSkill;

    private TextView tvDecayTimerTitle, tvDecayStatusIcon, tvDecayStatusText, tvSectionTitle;
    private Slider sliderDecay;
    private View llDecayStatus;

    private TextView tvGoalValue, tvWeeklyTotal;
    private View btnMinusGoal, btnPlusGoal;
    private TextView[] tvPresets = new TextView[6];
    private int dailyGoalMinutes = 10;

    private String selectedLevel = "Beginner";
    private DatabaseReference mDatabase;
    private String currentUid;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_skill, container, false);

        mDatabase = FirebaseDatabase.getInstance().getReference();
        currentUid = FirebaseAuth.getInstance().getUid();

        initViews(view);
        setupListeners();
        handleInsets(view);
        
        selectLevel("Beginner");
        updateDecayStatus(1);
        updateGoalUI();
        
        // Handle passed category from CategoriesActivity or set default "Popular" suggestions
        String category = "Popular";
        if (getArguments() != null && getArguments().containsKey("selected_category")) {
            category = getArguments().getString("selected_category");
        }
        setupCategorySuggestions(category);

        animateEntrance(view);

        return view;
    }

    private void setupCategorySuggestions(String category) {
        if (cgPopularSkills == null || getContext() == null) return;
        cgPopularSkills.removeAllViews();
        
        if (tvSectionTitle != null) {
            if ("Popular".equalsIgnoreCase(category)) {
                tvSectionTitle.setText("POPULAR SKILLS");
            } else {
                tvSectionTitle.setText("SUGGESTIONS FOR " + (category != null ? category.toUpperCase() : "SKILLS"));
            }
        }

        String[] suggestions;
        String cat = (category != null) ? category.toLowerCase() : "popular";
        
        if (cat.contains("tech")) {
            suggestions = new String[]{"Java 💻", "Python 🐍", "Web Dev 🌐", "React ⚛️", "C++ 👾", "Android 🤖", "Kotlin 🎯", "JavaScript 📜", "Swift 🍎", "Cybersecurity 🔒", "Data Science 📊", "AI/ML 🧠", "SQL 🗄️", "DevOps ♾️", "Cloud Computing ☁️"};
        } else if (cat.contains("music")) {
            suggestions = new String[]{"Guitar 🎸", "Piano 🎹", "Drums 🥁", "Singing 🎤", "Violin 🎻", "Bass 🎸", "Saxophone 🎷", "Trumpet 🎺", "Flute 🎶", "Music Theory 🎼", "Beatmaking 🎹", "Ukulele 🏖️", "Cello 🎻", "Songwriting 📝", "DJing 🎧"};
        } else if (cat.contains("language")) {
            suggestions = new String[]{"English 🇺🇸", "Spanish 🇪🇸", "Japanese 🇯🇵", "French 🇫🇷", "Public Speaking 🗣️", "Korean 🇰🇷", "German 🇩🇪", "Chinese 🇨🇳", "Italian 🇮🇹", "Sign Language 🤟", "Debating 🤝", "Storytelling 📖", "Negotiation 💼", "Writing ✍️", "Presentation 📽️"};
        } else if (cat.contains("sport")) {
            suggestions = new String[]{"Basketball 🏀", "Football ⚽", "Swimming 🏊", "Gym 🏋️", "Yoga 🧘", "Running 🏃", "Cycling 🚲", "Tennis 🎾", "Martial Arts 🥋", "Boxing 🥊", "Pilates 🤸", "Volleyball 🏐", "Hiking 🥾", "Badminton 🏸", "Cricket 🏏"};
        } else if (cat.contains("creative")) {
            suggestions = new String[]{"Drawing 🎨", "Photoshop 🖌️", "Writing ✍️", "Photography 📷", "Video Editing 🎬", "UI/UX Design 🎨", "Animation 🎞️", "Sculpture 🗿", "Fashion Design 👗", "Interior Design 🏠", "Calligraphy 🖋️", "Pottery 🏺", "Origami 📄", "Graphic Design 🖌️", "Illustration ✍️"};
        } else if (cat.contains("mind")) {
            suggestions = new String[]{"Chess ♟️", "Math 📐", "Meditation 🧠", "Logic 🧩", "Memory 💡", "Speed Reading 📖", "Critical Thinking 🧐", "Astronomy 🔭", "History 🏛️", "Philosophy 🧘‍♂️", "Coding ⌨️", "Physics ⚛️", "Chemistry 🧪", "Biology 🧬", "Geography 🌍"};
        } else {
            suggestions = new String[]{"Python 🐍", "Spanish 🇪🇸", "Piano 🎹", "Drawing 🎨", "Cooking 🍳", "Math 📐", "Java 💻", "Guitar 🎸", "English 🇺🇸", "Basketball 🏀", "Yoga 🧘", "Finance 💰", "Chess ♟️", "Driving 🚗", "First Aid 🚑"};
        }

        for (String s : suggestions) {
            if (getContext() == null) break;
            Chip chip = new Chip(requireContext());
            chip.setText(s);
            chip.setCheckable(true);
            chip.setClickable(true);
            chip.setChipBackgroundColorResource(R.color.splash_bg_surface);
            chip.setTextColor(Color.WHITE);
            chip.setChipStrokeColorResource(R.color.splash_bg_mid);
            chip.setChipStrokeWidth(2f);
            chip.setChipCornerRadius(32f);
            
            Typeface typeface = ResourcesCompat.getFont(requireContext(), R.font.nunito_semibold);
            if (typeface != null) chip.setTypeface(typeface);
            chip.setTextSize(13f);
            
            chip.setOnClickListener(v -> {
                etSkillName.setText(s);
                applyClickEffect(chip);
            });
            cgPopularSkills.addView(chip);
        }
    }

    private void initViews(View view) {
        etSkillName = view.findViewById(R.id.et_skill_name);
        cgPopularSkills = view.findViewById(R.id.cg_popular_skills);
        tvSectionTitle = view.findViewById(R.id.tv_popular_label);
        llBeginner = view.findViewById(R.id.ll_beginner);
        llIntermediate = view.findViewById(R.id.ll_intermediate);
        llAdvanced = view.findViewById(R.id.ll_advanced);
        ivBeginnerCheck = view.findViewById(R.id.iv_beginner_check);
        ivIntermediateCheck = view.findViewById(R.id.iv_intermediate_check);
        ivAdvancedCheck = view.findViewById(R.id.iv_advanced_check);
        btnAddSkill = view.findViewById(R.id.btn_add_skill);

        tvDecayTimerTitle = view.findViewById(R.id.tv_decay_timer_title);
        sliderDecay = view.findViewById(R.id.slider_decay);
        llDecayStatus = view.findViewById(R.id.ll_decay_status);
        tvDecayStatusIcon = view.findViewById(R.id.tv_decay_status_icon);
        tvDecayStatusText = view.findViewById(R.id.tv_decay_status_text);

        tvGoalValue = view.findViewById(R.id.tv_goal_value);
        tvWeeklyTotal = view.findViewById(R.id.tv_weekly_total);
        btnMinusGoal = view.findViewById(R.id.btn_minus_goal);
        btnPlusGoal = view.findViewById(R.id.btn_plus_goal);

        tvPresets[0] = view.findViewById(R.id.tv_preset_5);
        tvPresets[1] = view.findViewById(R.id.tv_preset_10);
        tvPresets[2] = view.findViewById(R.id.tv_preset_15);
        tvPresets[3] = view.findViewById(R.id.tv_preset_20);
        tvPresets[4] = view.findViewById(R.id.tv_preset_30);
        tvPresets[5] = view.findViewById(R.id.tv_preset_45);

        View back = view.findViewById(R.id.btn_back);
        if (back != null) back.setOnClickListener(v -> { if (getActivity() != null) getActivity().getSupportFragmentManager().popBackStack(); });

        View cancel = view.findViewById(R.id.btn_cancel);
        if (cancel != null) cancel.setOnClickListener(v -> { if (getActivity() != null) getActivity().getSupportFragmentManager().popBackStack(); });
    }

    private void setupListeners() {
        if (etSkillName != null) {
            etSkillName.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    boolean hasText = s.toString().trim().length() > 0;
                    if (btnAddSkill != null) {
                        btnAddSkill.setEnabled(hasText);
                        btnAddSkill.animate().alpha(hasText ? 1.0f : 0.5f).setDuration(200).start();
                    }
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }

        if (sliderDecay != null) {
            sliderDecay.addOnChangeListener((slider, value, fromUser) -> {
                int days = (int) value;
                if (tvDecayTimerTitle != null) tvDecayTimerTitle.setText("DECAY TIMER — " + days + " DAYS");
                updateDecayStatus(days);
            });
        }

        if (llBeginner != null) llBeginner.setOnClickListener(v -> selectLevel("Beginner"));
        if (llIntermediate != null) llIntermediate.setOnClickListener(v -> selectLevel("Intermediate"));
        if (llAdvanced != null) llAdvanced.setOnClickListener(v -> selectLevel("Advanced"));

        if (btnMinusGoal != null) btnMinusGoal.setOnClickListener(v -> { applyClickEffect(v); if (dailyGoalMinutes > 1) { dailyGoalMinutes--; updateGoalUI(); } });
        if (btnPlusGoal != null) btnPlusGoal.setOnClickListener(v -> { applyClickEffect(v); if (dailyGoalMinutes < 1440) { dailyGoalMinutes++; updateGoalUI(); } });

        for (int i = 0; i < tvPresets.length; i++) {
            final int index = i;
            if (tvPresets[i] != null) {
                tvPresets[i].setOnClickListener(v -> {
                    applyClickEffect(v);
                    String text = tvPresets[index].getText().toString().replace(" min", "");
                    try {
                        dailyGoalMinutes = Integer.parseInt(text);
                        updateGoalUI();
                    } catch (Exception ignored) {}
                });
            }
        }

        if (btnAddSkill != null) btnAddSkill.setOnClickListener(v -> { applyClickEffect(v); saveSkillToDatabase(); });
    }

    private void applyClickEffect(View v) {
        if (v != null) v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(100).start()).start();
    }

    private void updateGoalUI() {
        if (tvGoalValue != null) tvGoalValue.setText(String.valueOf(dailyGoalMinutes));
        if (tvWeeklyTotal != null) tvWeeklyTotal.setText((dailyGoalMinutes * 7) + " min");

        for (TextView preset : tvPresets) {
            if (preset == null) continue;
            String val = preset.getText().toString().replace(" min", "");
            try {
                if (Integer.parseInt(val) == dailyGoalMinutes) {
                    preset.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#1A58CC02")));
                    preset.setTextColor(Color.parseColor("#58CC02"));
                } else {
                    preset.setBackgroundTintList(null);
                    preset.setTextColor(Color.WHITE);
                }
            } catch (Exception ignored) {}
        }
    }

    private void saveSkillToDatabase() {
        if (etSkillName == null || currentUid == null) return;
        String skillName = etSkillName.getText().toString().trim();
        if (TextUtils.isEmpty(skillName)) return;
        
        int decayDays = (int) (sliderDecay != null ? sliderDecay.getValue() : 7);
        // FIX: Sanitize skillKey to remove characters forbidden by Firebase (., #, $, [, ], /)
        String skillKey = skillName.replaceAll("[.#$\\[\\]/]", "_");

        mDatabase.child("users").child(currentUid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                User user = snapshot.getValue(User.class);
                if (user != null) {
                    List<String> skills = user.getSkills();
                    if (skills == null) skills = new ArrayList<>();
                    
                    if (skills.contains(skillName)) {
                        Toast.makeText(getContext(), "You already have this skill!", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    skills.add(skillName);
                    user.setSkills(skills);

                    Map<String, Double> mastery = user.getSkillMastery();
                    double initialMastery = 0.0;
                    if ("Intermediate".equals(selectedLevel)) initialMastery = 30.0;
                    else if ("Advanced".equals(selectedLevel)) initialMastery = 60.0;
                    mastery.put(skillKey, initialMastery);
                    
                    user.getSkillDecaySettings().put(skillKey, decayDays);
                    user.getSkillDailyGoals().put(skillKey, dailyGoalMinutes);
                    user.getSkillLastPractice().put(skillKey, System.currentTimeMillis());
                    
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    String today = sdf.format(new Date());
                    Map<String, Map<String, Double>> history = user.getHistory();
                    if (!history.containsKey(today)) history.put(today, new HashMap<>());
                    Map<String, Double> dayMap = history.get(today);
                    if (dayMap != null) dayMap.put(skillKey, initialMastery);

                    mDatabase.child("users").child(currentUid).setValue(user)
                            .addOnSuccessListener(aVoid -> {
                                if (isAdded()) {
                                    Toast.makeText(getContext(), "Skill added successfully!", Toast.LENGTH_SHORT).show();
                                    if (getActivity() != null) getActivity().getSupportFragmentManager().popBackStack();
                                }
                            })
                            .addOnFailureListener(e -> { if (isAdded()) Toast.makeText(getContext(), "Failed to add skill", Toast.LENGTH_SHORT).show(); });
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void updateDecayStatus(int days) {
        if (getContext() == null) return;
        int color; String statusText; String icon; int bgTint;

        if (days <= 1) { color = Color.parseColor("#FF4B4B"); statusText = "At Risk"; icon = "🛑"; bgTint = Color.parseColor("#26FF4B4B"); }
        else if (days <= 4) { color = Color.parseColor("#FF9600"); statusText = "Fading"; icon = "🟠"; bgTint = Color.parseColor("#26FF9600"); }
        else if (days <= 9) { color = ContextCompat.getColor(getContext(), R.color.splash_green); statusText = "Sharp"; icon = "🟢"; bgTint = Color.parseColor("#2658CC02"); }
        else { color = Color.parseColor("#00BCD4"); statusText = "Mastered"; icon = "💎"; bgTint = Color.parseColor("#2600BCD4"); }

        if (tvDecayStatusIcon != null) tvDecayStatusIcon.setText(icon);
        if (tvDecayStatusText != null) {
            tvDecayStatusText.setText(statusText);
            tvDecayStatusText.setTextColor(color);
        }
        if (llDecayStatus != null) llDecayStatus.setBackgroundTintList(ColorStateList.valueOf(bgTint));
        if (sliderDecay != null) {
            sliderDecay.setThumbTintList(ColorStateList.valueOf(color));
            sliderDecay.setTrackActiveTintList(ColorStateList.valueOf(color));
        }
    }

    private void selectLevel(String level) {
        selectedLevel = level;
        if (llBeginner != null) llBeginner.setBackgroundTintList(null);
        if (llIntermediate != null) llIntermediate.setBackgroundTintList(null);
        if (llAdvanced != null) llAdvanced.setBackgroundTintList(null);
        if (ivBeginnerCheck != null) ivBeginnerCheck.setVisibility(View.GONE);
        if (ivIntermediateCheck != null) ivIntermediateCheck.setVisibility(View.GONE);
        if (ivAdvancedCheck != null) ivAdvancedCheck.setVisibility(View.GONE);

        int selectedColor = Color.parseColor("#1A58CC02");
        View target = null; View check = null;
        switch (level) {
            case "Beginner": if (llBeginner != null) { llBeginner.setBackgroundTintList(ColorStateList.valueOf(selectedColor)); if (ivBeginnerCheck != null) ivBeginnerCheck.setVisibility(View.VISIBLE); target = llBeginner; check = ivBeginnerCheck; } break;
            case "Intermediate": if (llIntermediate != null) { llIntermediate.setBackgroundTintList(ColorStateList.valueOf(selectedColor)); if (ivIntermediateCheck != null) ivIntermediateCheck.setVisibility(View.VISIBLE); target = llIntermediate; check = ivIntermediateCheck; } break;
            case "Advanced": if (llAdvanced != null) { llAdvanced.setBackgroundTintList(ColorStateList.valueOf(selectedColor)); if (ivAdvancedCheck != null) ivAdvancedCheck.setVisibility(View.VISIBLE); target = llAdvanced; check = ivAdvancedCheck; } break;
        }
        
        if (target != null) {
            target.setScaleX(0.95f); target.setScaleY(0.95f);
            target.animate().scaleX(1.0f).scaleY(1.0f).setDuration(200).setInterpolator(new AnticipateOvershootInterpolator()).start();
        }
        if (check != null) {
            check.setScaleX(0f); check.setScaleY(0f);
            check.animate().scaleX(1.0f).scaleY(1.0f).setDuration(300).setInterpolator(new AnticipateOvershootInterpolator()).start();
        }
    }

    private void animateEntrance(View view) {
        View header = view.findViewById(R.id.ll_add_skill_header);
        if (header != null) {
            header.setAlpha(0);
            header.setTranslationY(-20);
            header.animate().alpha(1).translationY(0).setDuration(400).setInterpolator(new DecelerateInterpolator()).start();
        }

        View scroll = view.findViewById(R.id.nsv_add_skill);
        if (scroll instanceof ViewGroup) {
            View child0 = ((ViewGroup) scroll).getChildAt(0);
            if (child0 instanceof ViewGroup) {
                ViewGroup content = (ViewGroup) child0;
                for (int i = 0; i < content.getChildCount(); i++) {
                    View child = content.getChildAt(i);
                    child.setAlpha(0);
                    child.setTranslationY(30);
                    child.animate()
                            .alpha(1)
                            .translationY(0)
                            .setDuration(500)
                            .setStartDelay(100 + (i * 50L))
                            .setInterpolator(new DecelerateInterpolator())
                            .start();
                }
            }
        }
    }

    private void handleInsets(View view) {
        if (getContext() == null) return;
        float density = getResources().getDisplayMetrics().density;
        View header = view.findViewById(R.id.ll_add_skill_header);
        View formContainer = view.findViewById(R.id.ll_form_container);

        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            
            if (header != null) {
                int paddingTop = (int) (20 * density) + systemBars.top;
                header.setPadding(header.getPaddingLeft(), paddingTop, header.getPaddingRight(), header.getPaddingBottom());
            }
            
            if (formContainer != null) {
                int bottomInset = Math.max(systemBars.bottom, ime.bottom);
                int paddingBottom = (int) (40 * density) + bottomInset;
                formContainer.setPadding(formContainer.getPaddingLeft(), formContainer.getPaddingTop(), formContainer.getPaddingRight(), paddingBottom);
            }

            return insets;
        });
    }
}
