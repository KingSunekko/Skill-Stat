package com.example.skillstat;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
    private String selectedEmoji = "🧙‍♂️"; // Default
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

        btnContinue.setOnClickListener(v -> saveAndContinue());

        return view;
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

        Map<String, Object> updates = new HashMap<>();
        updates.put("username", name);
        updates.put("avatarUrl", selectedEmoji);

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
        btnContinue.setAlpha(isEnabled ? 1.0f : 0.5f);
    }

    private void setupRecyclerView() {
        List<String> emojiAvatars = Arrays.asList(
                "🧙‍♂️", "🦊", "🐉", "🦁", "🐺",
                "🦅", "🐸", "🤖", "👾", "🧜‍♀️",
                "🐨", "🦋", "🐬", "🦄", "🐯"
        );

        adapter = new AvatarAdapter(emojiAvatars, position -> {
            selectedEmoji = emojiAvatars.get(position);
        });

        rvAvatars.setLayoutManager(new GridLayoutManager(getContext(), 5));
        rvAvatars.setAdapter(adapter);
    }
}
