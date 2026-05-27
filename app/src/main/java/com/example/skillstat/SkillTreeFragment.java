package com.example.skillstat;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
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

public class SkillTreeFragment extends Fragment {

    private String skillName;
    private DatabaseReference mDatabase;
    private String currentUid;
    private GridLayout glSubNodes;
    private TextView tvRootNode, tvSkillName;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_skill_tree, container, false);
        
        if (getArguments() != null) {
            skillName = getArguments().getString("skill_name");
        }

        tvSkillName = view.findViewById(R.id.tv_skill_name);
        tvRootNode = view.findViewById(R.id.tv_root_node);
        glSubNodes = view.findViewById(R.id.gl_sub_nodes);
        
        if (skillName != null) tvSkillName.setText(skillName + " Tree");

        view.findViewById(R.id.btn_back).setOnClickListener(v -> getParentFragmentManager().popBackStack());

        mDatabase = FirebaseDatabase.getInstance().getReference();
        currentUid = FirebaseAuth.getInstance().getUid();

        loadTreeData();

        return view;
    }

    private void loadTreeData() {
        if (currentUid == null || skillName == null) return;

        mDatabase.child("users").child(currentUid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                User user = snapshot.getValue(User.class);
                if (user != null) {
                    renderTree(user);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void renderTree(User user) {
        glSubNodes.removeAllViews();
        
        Map<String, List<String>> subSkillsMap = user.getSubSkills();
        Map<String, Double> masteryMap = user.getSubSkillMastery();
        
        List<String> subs = subSkillsMap.get(skillName);
        
        // Root Node Progress (Avg of subs)
        double totalMastery = 0;
        if (subs != null && !subs.isEmpty()) {
            for (String sub : subs) {
                totalMastery += masteryMap.getOrDefault(sub, 0.0);
            }
            double avg = totalMastery / subs.size();
            tvRootNode.setBackgroundTintList(android.content.res.ColorStateList.valueOf(ForgeUtils.getTierColor(avg)));
            
            for (String sub : subs) {
                double subMastery = masteryMap.getOrDefault(sub, 0.0);
                addSubNode(sub, subMastery);
            }
        }
    }

    private void addSubNode(String name, double mastery) {
        LinearLayout nodeLayout = new LinearLayout(getContext());
        nodeLayout.setOrientation(LinearLayout.VERTICAL);
        nodeLayout.setGravity(Gravity.CENTER);
        nodeLayout.setPadding(20, 20, 20, 20);

        // Connector Line
        View line = new View(getContext());
        line.setLayoutParams(new LinearLayout.LayoutParams(4, 30));
        line.setBackgroundColor(Color.parseColor("#2D2D44"));
        nodeLayout.addView(line);

        // Node Circle
        TextView node = new TextView(getContext());
        node.setLayoutParams(new LinearLayout.LayoutParams(160, 160));
        node.setBackgroundResource(R.drawable.shape_avatar_bg);
        node.setBackgroundTintList(android.content.res.ColorStateList.valueOf(ForgeUtils.getTierColor(mastery)));
        node.setGravity(Gravity.CENTER);
        node.setText(String.valueOf((int)mastery) + "%");
        node.setTextColor(Color.WHITE);
        node.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        nodeLayout.addView(node);

        // Label
        TextView label = new TextView(getContext());
        label.setText(name);
        label.setTextColor(Color.WHITE);
        label.setTextSize(12);
        label.setGravity(Gravity.CENTER);
        nodeLayout.addView(label);

        glSubNodes.addView(nodeLayout);
    }
}
