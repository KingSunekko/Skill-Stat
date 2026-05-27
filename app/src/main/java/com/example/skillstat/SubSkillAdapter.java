package com.example.skillstat;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.skillstat.utils.ForgeUtils;
import com.google.android.material.chip.ChipGroup;
import java.util.List;
import java.util.Map;

public class SubSkillAdapter extends RecyclerView.Adapter<SubSkillAdapter.ViewHolder> {

    private Map<String, List<String>> subSkillsMap;
    private List<String> mainSkills;
    private Map<String, Double> subSkillMastery;
    private OnSubSkillClickListener listener;

    public interface OnSubSkillClickListener {
        void onAddSubSkill(String mainSkill);
        void onSubSkillClick(String mainSkill, String subSkill);
    }

    public SubSkillAdapter(Map<String, List<String>> subSkillsMap, Map<String, Double> subSkillMastery, List<String> mainSkills, OnSubSkillClickListener listener) {
        this.subSkillsMap = subSkillsMap;
        this.subSkillMastery = subSkillMastery;
        this.mainSkills = mainSkills;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_sub_skill_group, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String mainSkill = mainSkills.get(position);
        List<String> subs = subSkillsMap.get(mainSkill);

        holder.tvMainSkill.setText(mainSkill);
        int count = (subs != null) ? subs.size() : 0;
        holder.tvCount.setText(count + " Sub-skills");

        holder.cgSubSkills.removeAllViews();
        if (subs != null) {
            for (String sub : subs) {
                View pill = LayoutInflater.from(holder.itemView.getContext()).inflate(R.layout.layout_sub_skill_pill, holder.cgSubSkills, false);
                TextView tvPill = (TextView) pill;

                double mastery = (subSkillMastery != null) ? subSkillMastery.getOrDefault(sub, 0.0) : 0.0;

                // RPG Leveling Display
                String title = ForgeUtils.getLevelTitle(mastery);
                tvPill.setText(sub + " • " + title + " (" + (int)mastery + "%)");

                // Set color based on tier
                tvPill.setTextColor(ForgeUtils.getTierColor(mastery));

                tvPill.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onSubSkillClick(mainSkill, sub);
                    }
                });

                holder.cgSubSkills.addView(pill);
            }
        }

        holder.btnAddSub.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAddSubSkill(mainSkill);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mainSkills.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvMainSkill, tvCount, btnAddSub;
        ChipGroup cgSubSkills;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMainSkill = itemView.findViewById(R.id.tv_main_skill_name);
            tvCount = itemView.findViewById(R.id.tv_sub_skill_count);
            cgSubSkills = itemView.findViewById(R.id.cg_sub_skills);
            btnAddSub = itemView.findViewById(R.id.btn_add_sub_skill);
        }
    }
}
