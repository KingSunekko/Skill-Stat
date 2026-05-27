package com.example.skillstat;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.example.skillstat.models.User;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CategoriesActivity extends AppCompatActivity {

    private LinearLayout llCategoriesList;
    private DatabaseReference mDatabase;
    private String currentUid;
    private boolean hasAnimatedEntrance = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_categories);

        mDatabase = FirebaseDatabase.getInstance().getReference();
        currentUid = FirebaseAuth.getInstance().getUid();

        FrameLayout btnBack = findViewById(R.id.btn_back_categories);
        btnBack.setOnClickListener(v -> finish());

        llCategoriesList = findViewById(R.id.ll_categories_list);

        loadUserSkillsAndPopulate();
        animateEntrance();
    }

    private void animateEntrance() {
        View header = findViewById(R.id.ll_categories_header);
        if (header != null) {
            header.setAlpha(0);
            header.setTranslationY(-20);
            header.animate().alpha(1).translationY(0).setDuration(400).setInterpolator(new DecelerateInterpolator()).start();
        }
    }

    private void loadUserSkillsAndPopulate() {
        if (currentUid == null) return;
        mDatabase.child("users").child(currentUid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                if (user != null) {
                    Map<String, List<String>> catMap = groupSkillsByCategory(user.getSkills());
                    populateCategories(catMap);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private Map<String, List<String>> groupSkillsByCategory(List<String> skills) {
        Map<String, List<String>> map = new HashMap<>();
        String[] cats = {"💻 Tech", "🎵 Music", "🗣️ Language", "🏃 Sport", "🎨 Creative", "🧠 Mind", "🔍 Life"};
        for (String c : cats) map.put(c, new ArrayList<>());

        if (skills != null) {
            for (String s : skills) {
                String sLower = s.toLowerCase();
                // Tech Keywords
                if (sLower.contains("java") || sLower.contains("python") || sLower.contains("code") || sLower.contains("web") || 
                    sLower.contains("react") || sLower.contains("data") || sLower.contains("science") || sLower.contains("ai") || 
                    sLower.contains("ml") || sLower.contains("sql") || sLower.contains("devops") || sLower.contains("android") || 
                    sLower.contains("kotlin") || sLower.contains("swift") || sLower.contains("tech") || sLower.contains("computing") || 
                    sLower.contains("c++") || sLower.contains("script") || sLower.contains("cyber")) {
                    map.get("💻 Tech").add(s);
                } 
                // Music Keywords
                else if (sLower.contains("guitar") || sLower.contains("piano") || sLower.contains("music") || sLower.contains("sing") || 
                         sLower.contains("drum") || sLower.contains("violin") || sLower.contains("bass") || sLower.contains("sax") || 
                         sLower.contains("trumpet") || sLower.contains("flute") || sLower.contains("song") || sLower.contains("beatmaking") || 
                         sLower.contains("ukulele") || sLower.contains("cello") || sLower.contains("dj") || sLower.contains("violin")) {
                    map.get("🎵 Music").add(s);
                } 
                // Language Keywords
                else if (sLower.contains("speak") || sLower.contains("english") || sLower.contains("japanese") || sLower.contains("spanish") || 
                         sLower.contains("french") || sLower.contains("korean") || sLower.contains("german") || sLower.contains("chinese") || 
                         sLower.contains("italian") || sLower.contains("sign") || sLower.contains("debate") || sLower.contains("storytelling") || 
                         sLower.contains("writing")) {
                    map.get("🗣️ Language").add(s);
                } 
                // Sport Keywords
                else if (sLower.contains("gym") || sLower.contains("run") || sLower.contains("yoga") || sLower.contains("basket") || 
                         sLower.contains("foot") || sLower.contains("swim") || sLower.contains("cycle") || sLower.contains("tennis") || 
                         sLower.contains("martial") || sLower.contains("boxing") || sLower.contains("pilates") || sLower.contains("volley") || 
                         sLower.contains("hike") || sLower.contains("badminton") || sLower.contains("cricket") || sLower.contains("sport")) {
                    map.get("🏃 Sport").add(s);
                } 
                // Creative Keywords
                else if (sLower.contains("draw") || sLower.contains("design") || sLower.contains("photo") || sLower.contains("paint") || 
                         sLower.contains("video") || sLower.contains("animation") || sLower.contains("sculpture") || sLower.contains("fashion") || 
                         sLower.contains("interior") || sLower.contains("callig") || sLower.contains("pottery") || sLower.contains("origami") || 
                         sLower.contains("illustrat")) {
                    map.get("🎨 Creative").add(s);
                } 
                // Mind Keywords
                else if (sLower.contains("chess") || sLower.contains("math") || sLower.contains("meditation") || sLower.contains("logic") || 
                         sLower.contains("memory") || sLower.contains("reading") || sLower.contains("history") || sLower.contains("philosophy") || 
                         sLower.contains("physics") || sLower.contains("chemistry") || sLower.contains("biology") || sLower.contains("geography") || 
                         sLower.contains("mind")) {
                    map.get("🧠 Mind").add(s);
                } 
                // Everything else
                else {
                    map.get("🔍 Life").add(s);
                }
            }
        }
        return map;
    }

    private void populateCategories(Map<String, List<String>> catMap) {
        llCategoriesList.removeAllViews();
        String[] cats = {"💻 Tech", "🎵 Music", "🗣️ Language", "🏃 Sport", "🎨 Creative", "🧠 Mind", "🔍 Life"};
        int idx = 0;
        for (String name : cats) {
            List<String> skills = catMap.get(name);
            addCategoryItem(name, skills != null ? skills.size() : 0, skills, idx++);
        }
        hasAnimatedEntrance = true;
    }

    private void addCategoryItem(String name, int count, List<String> skills, int index) {
        View view = getLayoutInflater().inflate(R.layout.item_category, llCategoriesList, false);

        TextView tvName = view.findViewById(R.id.tv_category_icon_name);
        TextView tvCount = view.findViewById(R.id.tv_category_count);
        TextView tvArrow = view.findViewById(R.id.tv_category_expand_arrow);
        LinearLayout expandable = view.findViewById(R.id.ll_category_expandable);
        ChipGroup chipGroup = view.findViewById(R.id.cg_category_skills);
        TextView tvEmpty = view.findViewById(R.id.tv_empty_category);
        TextView btnAdd = view.findViewById(R.id.btn_add_skill_category);

        tvName.setText(name);
        tvCount.setText(String.valueOf(count));

        if (skills != null && !skills.isEmpty()) {
            tvEmpty.setVisibility(View.GONE);
            for (String skill : skills) {
                Chip chip = new Chip(this);
                chip.setText(skill);
                chip.setChipBackgroundColorResource(R.color.splash_bg_surface);
                chip.setTextColor(getResources().getColor(R.color.white));
                chipGroup.addView(chip);
            }
        } else {
            tvEmpty.setVisibility(View.VISIBLE);
        }

        view.setOnClickListener(v -> {
            boolean visible = expandable.getVisibility() == View.VISIBLE;
            expandable.setVisibility(visible ? View.GONE : View.VISIBLE);
            tvArrow.setText(visible ? "▼" : "▲");
            if (!visible) {
                expandable.setAlpha(0);
                expandable.animate().alpha(1).setDuration(300).start();
            }
        });

        btnAdd.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("ACTION", "OPEN_ADD_SKILL");
            intent.putExtra("CATEGORY_NAME", name);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        if (!hasAnimatedEntrance) {
            view.setAlpha(0);
            view.setTranslationX(30);
            view.animate().alpha(1).translationX(0).setDuration(400).setStartDelay(200 + (index * 50L)).start();
        }

        llCategoriesList.addView(view);
    }
}
