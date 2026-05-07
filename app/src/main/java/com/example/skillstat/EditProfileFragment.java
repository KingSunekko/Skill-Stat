package com.example.skillstat;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.skillstat.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EditProfileFragment extends Fragment {

    private RecyclerView rvAvatars;
    private AvatarAdapter adapter;
    private TextView tvAvatarPreview;
    private EditText etDisplayName, etBio;
    private View btnBack, btnSave, btnCancel;
    private String selectedEmoji;
    private DatabaseReference mDatabase;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_edit_profile, container, false);

        mDatabase = FirebaseDatabase.getInstance().getReference();

        rvAvatars = view.findViewById(R.id.rv_edit_avatars);
        tvAvatarPreview = view.findViewById(R.id.tv_edit_avatar_emoji);
        etDisplayName = view.findViewById(R.id.et_edit_display_name);
        etBio = view.findViewById(R.id.et_edit_bio);
        btnBack = view.findViewById(R.id.btn_back);
        btnSave = view.findViewById(R.id.btn_save_changes);
        btnCancel = view.findViewById(R.id.btn_cancel_edit);

        setupAvatarGrid();
        loadUserData();

        btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());
        btnCancel.setOnClickListener(v -> getParentFragmentManager().popBackStack());
        
        btnSave.setOnClickListener(v -> saveChanges());

        return view;
    }

    private void loadUserData() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        mDatabase.child("users").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    User user = snapshot.getValue(User.class);
                    if (user != null) {
                        etDisplayName.setText(user.getUsername());
                        selectedEmoji = user.getAvatarUrl();
                        if (selectedEmoji != null && !selectedEmoji.isEmpty()) {
                            tvAvatarPreview.setText(selectedEmoji);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("EditProfileFragment", "Load failed", error.toException());
            }
        });
    }

    private void saveChanges() {
        String name = etDisplayName.getText().toString().trim();
        if (name.isEmpty()) {
            etDisplayName.setError("Name cannot be empty");
            return;
        }

        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        Map<String, Object> updates = new HashMap<>();
        updates.put("username", name);
        updates.put("avatarUrl", selectedEmoji);

        mDatabase.child("users").child(uid).updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Profile updated!", Toast.LENGTH_SHORT).show();
                    getParentFragmentManager().popBackStack();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Update failed", Toast.LENGTH_SHORT).show();
                });
    }

    private void setupAvatarGrid() {
        List<String> emojiAvatars = Arrays.asList(
                "🧙‍♂️", "🦊", "🐉", "🦁", "🐺",
                "🦅", "🐸", "🤖", "👾", "🧜‍♀️",
                "🐨", "🦋", "🐬", "🦄", "🐯"
        );

        adapter = new AvatarAdapter(emojiAvatars, position -> {
            selectedEmoji = emojiAvatars.get(position);
            tvAvatarPreview.setText(selectedEmoji);
        });

        rvAvatars.setLayoutManager(new GridLayoutManager(getContext(), 6));
        rvAvatars.setAdapter(adapter);
    }
}
