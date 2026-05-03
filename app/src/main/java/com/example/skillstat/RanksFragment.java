package com.example.skillstat;

import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import java.util.ArrayList;
import java.util.List;

public class RanksFragment extends Fragment {

    private LinearLayout llLeaderboardList;
    private View podiumSection;
    private TextView tabGlobal, tabWeekly, tabSkills;
    
    // Header UI
    private TextView tvHeaderRank, tvHeaderRankLabel;
    
    // Podium UI
    private TextView tvPodium1Emoji, tvPodium1Name, tvPodium1Score;
    private TextView tvPodium2Emoji, tvPodium2Name, tvPodium2Score;
    private TextView tvPodium3Emoji, tvPodium3Name, tvPodium3Score;
    
    private List<TextView> tabs = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_ranks, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        llLeaderboardList = view.findViewById(R.id.ll_leaderboard_list);
        podiumSection = view.findViewById(R.id.ll_podium_section);
        
        tabGlobal = view.findViewById(R.id.tab_global);
        tabWeekly = view.findViewById(R.id.tab_weekly);
        tabSkills = view.findViewById(R.id.tab_skills_rank);

        tvHeaderRank = view.findViewById(R.id.tv_user_rank);
        tvHeaderRankLabel = view.findViewById(R.id.tv_user_rank_label);

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
        
        // Initial load
        selectTab(tabGlobal);
        loadGlobalLeaderboard();
    }

    private void setupTabs() {
        tabGlobal.setOnClickListener(v -> {
            selectTab(tabGlobal);
            loadGlobalLeaderboard();
        });
        tabWeekly.setOnClickListener(v -> {
            selectTab(tabWeekly);
            loadWeeklyLeaderboard();
        });
        tabSkills.setOnClickListener(v -> {
            selectTab(tabSkills);
            loadSkillsLeaderboard();
        });
    }

    private void selectTab(TextView selectedTab) {
        for (TextView tab : tabs) {
            if (tab == selectedTab) {
                tab.setSelected(true);
                tab.setBackgroundResource(R.drawable.shape_skill_chip_selector);
                tab.setTextColor(ContextCompat.getColor(requireContext(), R.color.splash_green));
            } else {
                tab.setSelected(false);
                tab.setBackground(null);
                tab.setTextColor(ContextCompat.getColor(requireContext(), R.color.splash_text_secondary));
            }
        }
    }

    private void updatePodium(String p1Emoji, String p1Name, String p1Score, 
                              String p2Emoji, String p2Name, String p2Score, 
                              String p3Emoji, String p3Name, String p3Score) {
        podiumSection.setVisibility(View.VISIBLE);
        tvPodium1Emoji.setText(p1Emoji);
        tvPodium1Name.setText(p1Name);
        tvPodium1Score.setText(p1Score);
        tvPodium2Emoji.setText(p2Emoji);
        tvPodium2Name.setText(p2Name);
        tvPodium2Score.setText(p2Score);
        tvPodium3Emoji.setText(p3Emoji);
        tvPodium3Name.setText(p3Name);
        tvPodium3Score.setText(p3Score);
    }

    private void loadGlobalLeaderboard() {
        tvHeaderRank.setText("#2");
        tvHeaderRankLabel.setText("Global Rank");
        updatePodium("🦊", "Alex", "4,210", "🧙‍♂️", "You", "3,135", "🐺", "Sam", "2,980");
        
        llLeaderboardList.removeAllViews();
        addLeaderboardItem(1, "🦊", "Alex", "🔥 14d • Java 💻, Guitar 🎸", "4,210", false);
        addLeaderboardItem(2, "🧙‍♂️", "You", "🔥 6d • Java 💻, Public Spe...", "3,135", true);
        addLeaderboardItem(3, "🐺", "Sam", "🔥 8d • Public Speaking 🎤", "2,980", false);
        addLeaderboardItem(4, "🧜‍♀️", "Maria", "🔥 4d • Python 🐍, Design 🎨", "2,100", false);
        addLeaderboardItem(5, "🦁", "Carlos", "🔥 6d • Math 📐, Chess ♟️", "1,850", false);
        addLeaderboardItem(6, "🐸", "Yuki", "🔥 3d • Piano 🎹", "1,600", false);
        addLeaderboardItem(7, "🦋", "Priya", "🔥 2d • Drawing 🖌️, Spanis...", "1,340", false);
    }

    private void loadWeeklyLeaderboard() {
        tvHeaderRank.setText("#1");
        tvHeaderRankLabel.setText("Weekly Rank");
        updatePodium("🧙‍♂️", "You", "340", "🐺", "Sam", "280", "🦊", "Alex", "210");
        
        llLeaderboardList.removeAllViews();
        addLeaderboardItem(1, "🧙‍♂️", "You", "🔥 6d • Java 💻", "340", true);
        addLeaderboardItem(2, "🐺", "Sam", "🔥 8d • Public Speaking 🎤", "280", false);
        addLeaderboardItem(3, "🦊", "Alex", "🔥 14d • Guitar 🎸", "210", false);
        addLeaderboardItem(4, "🧜‍♀️", "Maria", "🔥 4d • Python 🐍", "190", false);
        addLeaderboardItem(5, "🦁", "Carlos", "🔥 6d • Chess ♟️", "155", false);
    }

    private void loadSkillsLeaderboard() {
        tvHeaderRank.setText("#2");
        tvHeaderRankLabel.setText("Global Rank");
        podiumSection.setVisibility(View.GONE);
        
        llLeaderboardList.removeAllViews();
        addLeaderboardItem(1, "🦊", "Alex", "Guitar 🎸", "95%", false, "Advanced");
        addLeaderboardItem(2, "🦋", "Priya", "Drawing 🖌️", "91%", false, "Advanced");
        addLeaderboardItem(3, "🧙‍♂️", "You", "Public Speaking 🎤", "91%", true, "Advanced");
        addLeaderboardItem(4, "🐺", "Sam", "Public Speaking 🎤", "88%", false, "Advanced");
        addLeaderboardItem(5, "🦁", "Carlos", "Chess ♟️", "84%", false, "Advanced");
        addLeaderboardItem(6, "🧙‍♂️", "You", "Java 💻", "82%", true, "Advanced");
        addLeaderboardItem(7, "🧜‍♀️", "Maria", "Python 🐍", "60%", false, "Intermediate");
    }

    private void addLeaderboardItem(int rank, String emoji, String name, String subInfo, String score, boolean isYou) {
        addLeaderboardItem(rank, emoji, name, subInfo, score, isYou, null);
    }

    private void addLeaderboardItem(int rank, String emoji, String name, String subInfo, String score, boolean isYou, String level) {
        View item = getLayoutInflater().inflate(R.layout.item_leaderboard, llLeaderboardList, false);

        TextView tvRank = item.findViewById(R.id.tv_rank_number);
        TextView tvEmoji = item.findViewById(R.id.tv_avatar_emoji);
        TextView tvName = item.findViewById(R.id.tv_name);
        TextView tvSubInfo = item.findViewById(R.id.tv_sub_info);
        TextView tvScore = item.findViewById(R.id.tv_score_value);
        TextView tvScoreIcon = item.findViewById(R.id.tv_score_icon);
        TextView tvYouTag = item.findViewById(R.id.tv_you_tag);
        TextView tvLevel = item.findViewById(R.id.tv_skill_level);
        View avatarBorder = item.findViewById(R.id.v_avatar_border);

        if (rank == 1) tvRank.setText("🥇");
        else if (rank == 2) tvRank.setText("🥈");
        else if (rank == 3) tvRank.setText("🥉");
        else tvRank.setText(String.valueOf(rank));

        tvEmoji.setText(emoji);
        tvName.setText(name);
        tvSubInfo.setText(subInfo);
        tvScore.setText(score);

        if (level != null) {
            // Skill Tab Specific UI
            tvScoreIcon.setVisibility(View.GONE);
            tvLevel.setVisibility(View.VISIBLE);
            tvLevel.setText(level);
            tvScore.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        } else {
            tvScoreIcon.setVisibility(View.VISIBLE);
            tvLevel.setVisibility(View.GONE);
            tvScore.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        }

        if (isYou) {
            tvYouTag.setVisibility(View.VISIBLE);
            item.setBackgroundResource(R.drawable.shape_reward_skill_pill);
            avatarBorder.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.splash_green));
        }

        llLeaderboardList.addView(item);
    }
}
