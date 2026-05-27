package com.example.skillstat;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.skillstat.models.User;
import com.example.skillstat.utils.ForgeUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MasterTreeFragment extends Fragment {

    private DatabaseReference mDatabase;
    private String currentUid;
    private LinearLayout llSkillsTree, treeContainer;
    private ImageView ivUserAvatar;
    private View userNodeCard, mainTrunk;
    private TextView tvUserName, tvUserRank;
    private final String LINE_COLOR = "#4A4A6A";
    private final int LINE_THICKNESS_DP = 2;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_master_tree, container, false);

        treeContainer = view.findViewById(R.id.tree_container);
        userNodeCard = view.findViewById(R.id.user_node_card);
        ivUserAvatar = view.findViewById(R.id.iv_user_avatar);
        tvUserName = view.findViewById(R.id.tv_user_name);
        tvUserRank = view.findViewById(R.id.tv_user_rank);
        llSkillsTree = view.findViewById(R.id.ll_skills_tree);
        mainTrunk = view.findViewById(R.id.main_trunk);

        view.findViewById(R.id.btn_back).setOnClickListener(v -> getParentFragmentManager().popBackStack());

        mDatabase = FirebaseDatabase.getInstance().getReference();
        currentUid = FirebaseAuth.getInstance().getUid();

        loadMasterTreeData();

        return view;
    }

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    private void loadMasterTreeData() {
        if (currentUid == null) return;
        mDatabase.child("users").child(currentUid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                User user = snapshot.getValue(User.class);
                if (user != null) renderMasterTree(user);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void renderMasterTree(User user) {
        if (llSkillsTree == null) return;
        llSkillsTree.removeAllViews();
        View oldBar = treeContainer.findViewWithTag("hor_bar");
        if (oldBar != null) treeContainer.removeView(oldBar);

        // Update Avatar and User Info
        String avatarName = user.getAvatarUrl();
        int resId = getResources().getIdentifier(avatarName, "drawable", getContext().getPackageName());
        if (resId != 0) ivUserAvatar.setImageResource(resId);
        
        tvUserName.setText(user.getUsername() != null ? user.getUsername().toUpperCase() : "YOU");
        tvUserRank.setText("LEVEL " + user.getLevel() + " • " + user.getRankName().toUpperCase());

        Map<String, Double> skillMastery = user.getSkillMastery();
        if (!skillMastery.isEmpty()) {
            double total = 0;
            for (double m : skillMastery.values()) total += m;
            userNodeCard.setBackgroundTintList(ColorStateList.valueOf(ForgeUtils.getTierColor(total / skillMastery.size())));
        }

        List<String> skills = user.getSkills();
        Map<String, List<String>> subSkillsMap = user.getSubSkills();
        Map<String, Double> subMasteryMap = user.getSubSkillMastery();

        for (int i = 0; i < skills.size(); i++) {
            String skill = skills.get(i);
            addSkillBranch(skill, skillMastery.getOrDefault(skill.replace(".", "_"), 0.0), subSkillsMap.get(skill), subMasteryMap, i);
        }

        // Add Horizontal Bar (Energy Path)
        llSkillsTree.post(() -> {
            if (!isAdded() || skills.size() <= 1) return;
            int childCount = llSkillsTree.getChildCount();
            View first = llSkillsTree.getChildAt(0);
            View last = llSkillsTree.getChildAt(childCount - 1);

            int barWidth = (last.getLeft() + last.getWidth()/2) - (first.getLeft() + first.getWidth()/2);
            View bar = new View(getContext());
            bar.setTag("hor_bar");
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(barWidth, dpToPx(LINE_THICKNESS_DP));
            bar.setLayoutParams(lp);
            bar.setBackgroundColor(Color.parseColor(LINE_COLOR));
            treeContainer.addView(bar, treeContainer.indexOfChild(mainTrunk) + 1);
        });
    }

    private void addSkillBranch(String skillName, double mastery, List<String> subs, Map<String, Double> subMasteryMap, int index) {
        LinearLayout branch = new LinearLayout(getContext());
        branch.setOrientation(LinearLayout.VERTICAL);
        branch.setGravity(Gravity.CENTER_HORIZONTAL);
        branch.setPadding(dpToPx(40), 0, dpToPx(40), 0);

        // Vertical Line
        View line = new View(getContext());
        line.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(LINE_THICKNESS_DP), dpToPx(40)));
        line.setBackgroundColor(Color.parseColor(LINE_COLOR));
        branch.addView(line);

        // Skill Gem Node
        FrameLayout gemWrap = new FrameLayout(getContext());
        gemWrap.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(80), dpToPx(80)));
        
        TextView gem = new TextView(getContext());
        FrameLayout.LayoutParams gemLp = new FrameLayout.LayoutParams(dpToPx(70), dpToPx(70), Gravity.CENTER);
        gem.setLayoutParams(gemLp);
        gem.setBackgroundResource(R.drawable.shape_avatar_bg);
        gem.setBackgroundTintList(ColorStateList.valueOf(ForgeUtils.getTierColor(mastery)));
        gem.setGravity(Gravity.CENTER);
        gem.setElevation(dpToPx(8));
        
        String symbol = extractEmoji(skillName);
        if (symbol.isEmpty()) {
            symbol = skillName.substring(0, Math.min(2, skillName.length())).toUpperCase();
            gem.setTextSize(14);
        } else {
            gem.setTextSize(24);
        }
        gem.setText(symbol);
        gem.setTextColor(Color.WHITE);
        gem.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        
        gemWrap.addView(gem);
        branch.addView(gemWrap);

        // Label Pill
        TextView title = new TextView(getContext());
        title.setText(skillName.replaceAll("[\\p{So}\\p{Cn}]", "").trim());
        title.setTextColor(Color.WHITE);
        title.setTextSize(12);
        title.setPadding(dpToPx(15), dpToPx(6), dpToPx(15), dpToPx(6));
        title.setGravity(Gravity.CENTER);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        title.setBackgroundResource(R.drawable.shape_pill_dark_3d);
        
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(-2, -2);
        titleLp.topMargin = dpToPx(8);
        title.setLayoutParams(titleLp);
        branch.addView(title);

        // Sub-skills
        if (subs != null && !subs.isEmpty()) {
            View subConnector = new View(getContext());
            subConnector.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(LINE_THICKNESS_DP / 2 + 1), dpToPx(20)));
            subConnector.setBackgroundColor(Color.parseColor("#333344"));
            branch.addView(subConnector);
            for (String sub : subs) addSubOrb(branch, sub, subMasteryMap.getOrDefault(sub, 0.0));
        }

        // Animation
        branch.setAlpha(0); branch.setScaleX(0.5f); branch.setScaleY(0.5f);
        branch.animate().alpha(1).scaleX(1).scaleY(1).setDuration(600)
                .setStartDelay(index * 100L).setInterpolator(new OvershootInterpolator(1.2f)).start();

        llSkillsTree.addView(branch);
    }

    private void addSubOrb(LinearLayout parent, String name, double mastery) {
        LinearLayout subLayout = new LinearLayout(getContext());
        subLayout.setOrientation(LinearLayout.VERTICAL);
        subLayout.setGravity(Gravity.CENTER_HORIZONTAL);
        subLayout.setPadding(0, dpToPx(8), 0, dpToPx(8));

        TextView orb = new TextView(getContext());
        orb.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(45), dpToPx(45)));
        orb.setBackgroundResource(R.drawable.shape_avatar_circle_green);
        orb.setBackgroundTintList(ColorStateList.valueOf(ForgeUtils.getTierColor(mastery)));
        orb.setGravity(Gravity.CENTER);
        orb.setText((int)mastery + "%");
        orb.setTextSize(9); orb.setTextColor(Color.WHITE);
        orb.setTypeface(Typeface.DEFAULT_BOLD);
        subLayout.addView(orb);

        TextView subLabel = new TextView(getContext());
        subLabel.setText(name);
        subLabel.setTextColor(Color.parseColor("#BBBBCC"));
        subLabel.setTextSize(9);
        subLabel.setGravity(Gravity.CENTER);
        subLayout.addView(subLabel);

        parent.addView(subLayout);
    }

    private String extractEmoji(String str) {
        Pattern p = Pattern.compile("[\\p{So}\\p{Cn}]", Pattern.UNICODE_CASE | Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(str);
        return m.find() ? m.group() : "";
    }
}
