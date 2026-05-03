package com.example.skillstat;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class SkillsFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_skills, container, false);

        // Setup individual skill cards
        setupSkillCard(view.findViewById(R.id.card_java), "Java 💻");
        setupSkillCard(view.findViewById(R.id.card_guitar), "Guitar 🎸");
        setupSkillCard(view.findViewById(R.id.card_public_speaking), "Public Speaking 🎤");

        // Setup the "Add New Skill" button
        View btnAdd = view.findViewById(R.id.btn_add_new_skill);
        if (btnAdd != null) {
            btnAdd.setOnClickListener(v -> navigateToAddSkill());
        }

        // Setup the small header add button
        View btnAddSmall = view.findViewById(R.id.btn_add_small);
        if (btnAddSmall != null) {
            btnAddSmall.setOnClickListener(v -> navigateToAddSkill());
        }

        return view;
    }

    private void setupSkillCard(View cardView, String skillName) {
        if (cardView != null) {
            // Set the skill name if it's different from default
            TextView tvName = cardView.findViewById(R.id.tv_skill_name);
            if (tvName != null) {
                tvName.setText(skillName);
            }

            // Edit Button
            View editButton = cardView.findViewById(R.id.btn_edit);
            if (editButton != null) {
                editButton.setOnClickListener(v -> navigateToEditSkill());
            }

            // Practice Button
            View practiceButton = cardView.findViewById(R.id.btn_practice);
            if (practiceButton != null) {
                practiceButton.setOnClickListener(v -> navigateToPractice(skillName));
            }
        }
    }

    private void navigateToEditSkill() {
        if (getActivity() != null) {
            getActivity().getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(
                            R.anim.step_forward_enter,
                            R.anim.step_forward_exit,
                            R.anim.step_backward_enter,
                            R.anim.step_backward_exit
                    )
                    .replace(R.id.fragment_container, new EditSkillFragment())
                    .addToBackStack(null)
                    .commit();
        }
    }

    private void navigateToPractice(String skillName) {
        if (getActivity() != null) {
            PracticeFragment practiceFragment = new PracticeFragment();
            Bundle args = new Bundle();
            args.putString("skill_name", skillName);
            practiceFragment.setArguments(args);

            getActivity().getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(
                            R.anim.step_forward_enter,
                            R.anim.step_forward_exit,
                            R.anim.step_backward_enter,
                            R.anim.step_backward_exit
                    )
                    .replace(R.id.fragment_container, practiceFragment)
                    .addToBackStack(null)
                    .commit();
        }
    }

    private void navigateToAddSkill() {
        if (getActivity() != null) {
            getActivity().getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(
                            R.anim.step_forward_enter,
                            R.anim.step_forward_exit,
                            R.anim.step_backward_enter,
                            R.anim.step_backward_exit
                    )
                    .replace(R.id.fragment_container, new AddSkillFragment())
                    .addToBackStack(null)
                    .commit();
        }
    }
}