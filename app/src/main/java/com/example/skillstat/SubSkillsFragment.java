package com.example.skillstat;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.skillstat.models.User;
import com.example.skillstat.utils.ForgeUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SubSkillsFragment extends Fragment implements SubSkillAdapter.OnSubSkillClickListener {

    private RecyclerView rvSubSkills;
    private View llEmptyState;
    private EditText etSearch;
    private TextView tvTotalSubsPill, tvMasteryPill, tvTitle;
    private DatabaseReference mDatabase;
    private String currentUid;
    private SubSkillAdapter adapter;
    private List<String> mainSkillsList = new ArrayList<>();
    private List<String> filteredMainSkillsList = new ArrayList<>();
    private Map<String, List<String>> subSkillsMap = new HashMap<>();
    private Map<String, Double> subSkillMasteryMap = new HashMap<>();
    private boolean hasAnimatedEntrance = false;
    private ValueEventListener userListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sub_skills, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mDatabase = FirebaseDatabase.getInstance().getReference();
        currentUid = FirebaseAuth.getInstance().getUid();

        rvSubSkills = view.findViewById(R.id.rv_sub_skills);
        llEmptyState = view.findViewById(R.id.ll_empty_state);
        etSearch = view.findViewById(R.id.et_search_sub);
        tvTotalSubsPill = view.findViewById(R.id.tv_total_subs_pill);
        tvMasteryPill = view.findViewById(R.id.tv_mastery_pill);
        tvTitle = view.findViewById(R.id.tv_title);

        if (tvTitle != null) tvTitle.setText("The Forge ⚒️");

        rvSubSkills.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new SubSkillAdapter(subSkillsMap, subSkillMasteryMap, filteredMainSkillsList, this);
        rvSubSkills.setAdapter(adapter);

        view.findViewById(R.id.btn_back).setOnClickListener(v -> getParentFragmentManager().popBackStack());

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { filterSkills(s.toString()); }
            @Override public void afterTextChanged(Editable s) {}
        });

        loadData();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mDatabase != null && userListener != null && currentUid != null) {
            mDatabase.child("users").child(currentUid).removeEventListener(userListener);
        }
    }

    private void loadData() {
        if (currentUid == null) return;
        userListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                User user = snapshot.getValue(User.class);
                if (user != null) {
                    mainSkillsList.clear();
                    mainSkillsList.addAll(user.getSkills());
                    subSkillsMap.clear();
                    subSkillsMap.putAll(user.getSubSkills());
                    subSkillMasteryMap.clear();
                    subSkillMasteryMap.putAll(user.getSubSkillMastery());
                    
                    updateForgeDashboard();
                    filterSkills(etSearch.getText().toString());
                    if (!hasAnimatedEntrance) animateEntrance();
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        };
        mDatabase.child("users").child(currentUid).addValueEventListener(userListener);
    }

    private void updateForgeDashboard() {
        int total = 0;
        for (List<String> list : subSkillsMap.values()) total += list.size();
        tvTotalSubsPill.setText(String.format(Locale.US, "⚒️ %d FORGED", total));

        double maxM = 0;
        for (double m : subSkillMasteryMap.values()) if (m > maxM) maxM = m;
        
        String currentRank = ForgeUtils.getLevelTitle(maxM);
        tvMasteryPill.setText(String.format(Locale.US, "🏆 RANK: %s", currentRank.toUpperCase()));
        tvMasteryPill.setTextColor(ForgeUtils.getTierColor(maxM));
    }

    private void filterSkills(String query) {
        filteredMainSkillsList.clear();
        if (TextUtils.isEmpty(query)) {
            filteredMainSkillsList.addAll(mainSkillsList);
        } else {
            String q = query.toLowerCase(Locale.US);
            for (String skill : mainSkillsList) {
                if (skill.toLowerCase(Locale.US).contains(q)) {
                    filteredMainSkillsList.add(skill);
                } else {
                    List<String> subs = subSkillsMap.get(skill);
                    if (subs != null) {
                        for (String sub : subs) {
                            if (sub.toLowerCase(Locale.US).contains(q)) {
                                filteredMainSkillsList.add(skill);
                                break;
                            }
                        }
                    }
                }
            }
        }
        updateUI();
    }

    private void updateUI() {
        if (filteredMainSkillsList.isEmpty()) {
            llEmptyState.setVisibility(View.VISIBLE);
            rvSubSkills.setVisibility(View.GONE);
        } else {
            llEmptyState.setVisibility(View.GONE);
            rvSubSkills.setVisibility(View.VISIBLE);
            adapter.notifyDataSetChanged(); // In real production, use DiffUtil here
        }
    }

    @Override
    public void onAddSubSkill(String mainSkill) {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_sub_skill, null);
        EditText etSubSkillName = dialogView.findViewById(R.id.et_sub_skill_name);

        new MaterialAlertDialogBuilder(getContext(), R.style.MaterialAlertDialog_Rounded)
                .setTitle("Forge New Sub-skill in " + mainSkill)
                .setView(dialogView)
                .setPositiveButton("Forge ⚒️", (dialog, which) -> {
                    String subName = etSubSkillName.getText().toString().trim();
                    if (!TextUtils.isEmpty(subName)) saveSubSkill(mainSkill, subName);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onSubSkillClick(String mainSkill, String subSkill) {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_manage_sub_skill, null);
        TextView tvTitleManage = dialogView.findViewById(R.id.tv_manage_title);
        SeekBar sbMastery = dialogView.findViewById(R.id.sb_mastery);
        TextView tvMasteryValue = dialogView.findViewById(R.id.tv_mastery_value);
        TextView btnDelete = dialogView.findViewById(R.id.btn_delete_sub);

        double currentM = subSkillMasteryMap.getOrDefault(subSkill, 0.0);
        tvTitleManage.setText(String.format("%s — %s", subSkill, ForgeUtils.getLevelTitle(currentM)));
        sbMastery.setProgress((int)currentM);
        tvMasteryValue.setText(String.format(Locale.US, "%d%% Mastery", (int)currentM));

        sbMastery.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvMasteryValue.setText(String.format(Locale.US, "%d%% — %s", progress, ForgeUtils.getLevelTitle(progress)));
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        AlertDialog dialog = new MaterialAlertDialogBuilder(getContext(), R.style.MaterialAlertDialog_Rounded)
                .setView(dialogView)
                .setPositiveButton("Update Mastery", (d, which) -> {
                    int newM = sbMastery.getProgress();
                    mDatabase.child("users").child(currentUid).child("subSkillMastery").child(subSkill).setValue((double)newM);
                    if (newM >= 100 && currentM < 100) unlockGrandmasterBadge(subSkill);
                })
                .setNegativeButton("Close", null)
                .create();

        btnDelete.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(getContext(), R.style.MaterialAlertDialog_Rounded)
                    .setTitle("Discard Sub-skill?")
                    .setMessage("This will remove all progress for '" + subSkill + "'.")
                    .setPositiveButton("Delete", (d2, w2) -> { deleteSubSkill(mainSkill, subSkill); dialog.dismiss(); })
                    .setNegativeButton("Cancel", null).show();
        });

        dialog.show();
    }

    private void unlockGrandmasterBadge(String subSkill) {
        mDatabase.child("users").child(currentUid).child("badges").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<String> badges = new ArrayList<>();
                for (DataSnapshot ds : snapshot.getChildren()) badges.add(ds.getValue(String.class));
                String name = "Grandmaster: " + subSkill;
                if (!badges.contains(name)) {
                    badges.add(name);
                    mDatabase.child("users").child(currentUid).child("badges").setValue(badges);
                    Toast.makeText(getContext(), "Epic Unlock: Grandmaster Badge Acquired! 👑", Toast.LENGTH_LONG).show();
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void deleteSubSkill(String mainSkill, String subSkill) {
        List<String> subs = subSkillsMap.get(mainSkill);
        if (subs != null) {
            subs.remove(subSkill);
            mDatabase.child("users").child(currentUid).child("subSkills").child(mainSkill).setValue(subs);
            mDatabase.child("users").child(currentUid).child("subSkillMastery").child(subSkill).removeValue();
        }
    }

    private void saveSubSkill(String mainSkill, String subName) {
        List<String> subs = subSkillsMap.get(mainSkill);
        if (subs == null) subs = new ArrayList<>();
        if (subs.contains(subName)) {
            Toast.makeText(getContext(), "Already forged!", Toast.LENGTH_SHORT).show();
            return;
        }
        subs.add(subName);
        mDatabase.child("users").child(currentUid).child("subSkills").child(mainSkill).setValue(subs);
        mDatabase.child("users").child(currentUid).child("subSkillMastery").child(subName).setValue(0.0);
    }

    private void animateEntrance() {
        if (getView() == null) return;
        hasAnimatedEntrance = true;
        View header = getView().findViewById(R.id.ll_sub_skills_header);
        header.animate().alpha(1).translationY(0).setDuration(400).setInterpolator(new DecelerateInterpolator()).start();
        View search = getView().findViewById(R.id.cv_search_sub);
        search.animate().alpha(1).translationY(0).setDuration(400).setStartDelay(100).start();
        rvSubSkills.setAlpha(0);
        rvSubSkills.animate().alpha(1).setDuration(600).setStartDelay(200).start();
    }
}
