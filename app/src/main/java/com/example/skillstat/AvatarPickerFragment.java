package com.example.skillstat;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AvatarPickerFragment extends Fragment {

    private RecyclerView rvAvatars;
    private AvatarAdapter adapter;
    private EditText etDisplayName;
    private Button btnContinue;
    private int selectedAvatarResId = R.drawable.prof1; // Default
    private DatabaseReference mDatabase;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_avatar_picker, container, false);

        mDatabase = FirebaseDatabase.getInstance().getReference();

        rvAvatars = view.findViewById(R.id.rv_avatars);
        etDisplayName = view.findViewById(R.id.et_display_name);
        btnContinue = view.findViewById(R.id.btn_continue);

        setupRecyclerView();
        setupValidation();

        btnContinue.setOnClickListener(v -> {
            applyClickEffect(v);
            saveAndContinue();
        });

        animateEntrance(view);

        return view;
    }

    private void animateEntrance(View view) {
        View header = view.findViewById(R.id.tv_picker_title);
        View sub = view.findViewById(R.id.tv_picker_subtitle);
        
        if (header != null) {
            header.setAlpha(0);
            header.setTranslationY(20);
            header.animate().alpha(1).translationY(0).setDuration(500).setInterpolator(new DecelerateInterpolator()).start();
        }
        if (sub != null) {
            sub.setAlpha(0);
            sub.setTranslationY(20);
            sub.animate().alpha(1).translationY(0).setDuration(500).setStartDelay(100).setInterpolator(new DecelerateInterpolator()).start();
        }
        
        etDisplayName.setAlpha(0);
        etDisplayName.setTranslationY(20);
        etDisplayName.animate().alpha(1).translationY(0).setDuration(500).setStartDelay(200).start();
        
        rvAvatars.setAlpha(0);
        rvAvatars.setTranslationY(30);
        rvAvatars.animate().alpha(1).translationY(0).setDuration(600).setStartDelay(300).start();
        
        btnContinue.setAlpha(0);
        btnContinue.setTranslationY(20);
        btnContinue.animate().alpha(1).translationY(0).setDuration(500).setStartDelay(400).start();
    }

    private void applyClickEffect(View v) {
        v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(100).start()).start();
    }

    private void saveAndContinue() {
        String name = etDisplayName.getText().toString().trim();
        if (TextUtils.isEmpty(name)) {
            etDisplayName.setError("Please enter a display name");
            etDisplayName.requestFocus();
            return;
        }

        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        // Map the resource ID back to a string name for storage
        String avatarName = getResources().getResourceEntryName(selectedAvatarResId);

        Map<String, Object> updates = new HashMap<>();
        updates.put("username", name);
        updates.put("avatarUrl", avatarName); // Now storing "prof1", "prof2", etc.

        mDatabase.child("users").child(uid).updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    if (getActivity() != null) {
                        getActivity().getSupportFragmentManager().beginTransaction()
                                .setCustomAnimations(
                                        R.anim.step_forward_enter,
                                        R.anim.step_forward_exit,
                                        R.anim.step_backward_enter,
                                        R.anim.step_backward_exit
                                )
                                .replace(R.id.onboarding_container, new SkillsPickerFragment())
                                .addToBackStack(null)
                                .commit();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to save profile", Toast.LENGTH_SHORT).show();
                });
    }

    private void setupValidation() {
        updateButtonState();
        etDisplayName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateButtonState();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void updateButtonState() {
        boolean isEnabled = !TextUtils.isEmpty(etDisplayName.getText().toString().trim());
        btnContinue.setEnabled(isEnabled);
        btnContinue.animate().alpha(isEnabled ? 1.0f : 0.5f).setDuration(200).start();
    }

    private void setupRecyclerView() {
        List<Integer> imageAvatars = Arrays.asList(
                R.drawable.prof1, R.drawable.prof2, R.drawable.prof3, R.drawable.prof4, R.drawable.prof5,
                R.drawable.prof6, R.drawable.prof7, R.drawable.prof8, R.drawable.prof9, R.drawable.prof10,
                R.drawable.prof11, R.drawable.prof12, R.drawable.prof13, R.drawable.prof14, R.drawable.prof15
        );

        adapter = new AvatarAdapter(imageAvatars, position -> {
            selectedAvatarResId = imageAvatars.get(position);
        });

        rvAvatars.setLayoutManager(new GridLayoutManager(getContext(), 5));
        rvAvatars.setAdapter(adapter);
    }
}
