package com.example.skillstat;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

public class CategoriesActivity extends AppCompatActivity {

    private LinearLayout llCategoriesList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_categories);

        FrameLayout btnBack = findViewById(R.id.btn_back_categories);
        btnBack.setOnClickListener(v -> finish());

        llCategoriesList = findViewById(R.id.ll_categories_list);

        populateCategories();
    }

    private void populateCategories() {
        llCategoriesList.removeAllViews();

        addCategoryItem("💻 Tech", 1, new String[]{"Java 💻"});
        addCategoryItem("🎵 Music", 1, new String[]{"Guitar 🎸"});
        addCategoryItem("🗣️ Language", 1, new String[]{"Public Speaking 🎤"});
        addCategoryItem("🏃 Sport", 0, null);
        addCategoryItem("🎨 Creative", 0, null);
        addCategoryItem("🧠 Mind", 0, null);
        addCategoryItem("🔍 Life", 0, null);
    }

    private void addCategoryItem(String name, int count, String[] skills) {
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

        if (skills != null && skills.length > 0) {
            tvEmpty.setVisibility(View.GONE);
            for (String skill : skills) {
                Chip chip = new Chip(this);
                chip.setText(skill);
                chip.setChipBackgroundColorResource(R.color.splash_bg_surface);
                chip.setTextColor(getResources().getColor(R.color.white));
                chip.setChipStrokeColorResource(R.color.splash_bg_mid);
                chip.setChipStrokeWidth(2f);
                chipGroup.addView(chip);
            }
        } else {
            tvEmpty.setVisibility(View.VISIBLE);
        }

        view.setOnClickListener(v -> {
            if (expandable.getVisibility() == View.VISIBLE) {
                expandable.setVisibility(View.GONE);
                tvArrow.setText("▼");
            } else {
                expandable.setVisibility(View.VISIBLE);
                tvArrow.setText("▲");
            }
        });

        btnAdd.setOnClickListener(v -> {
            // Handle add skill for this category
        });

        llCategoriesList.addView(view);
    }
}
