package com.example.skillstat;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnticipateOvershootInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
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
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EditProfileFragment extends Fragment {

    private RecyclerView rvAvatars;
    private AvatarAdapter adapter;
    private ImageView ivAvatarPreview;
    private EditText etDisplayName, etBio;
    private ProgressBar pbLoading;
    private View btnSave;
    private String selectedAvatarName;
    private String originalUsername;
    private DatabaseReference mDatabase;
    private String currentUid;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_edit_profile, container, false);
        mDatabase = FirebaseDatabase.getInstance().getReference();
        currentUid = FirebaseAuth.getInstance().getUid();

        rvAvatars = view.findViewById(R.id.rv_edit_avatars);
        ivAvatarPreview = view.findViewById(R.id.iv_edit_avatar_preview);
        etDisplayName = view.findViewById(R.id.et_edit_display_name);
        etBio = view.findViewById(R.id.et_edit_bio);
        pbLoading = view.findViewById(R.id.pb_edit_profile);
        btnSave = view.findViewById(R.id.btn_save_changes);

        setupAvatarGrid();
        loadUserData();

        btnSave.setOnClickListener(v -> validateAndSave());
        view.findViewById(R.id.btn_back).setOnClickListener(v -> getParentFragmentManager().popBackStack());

        return view;
    }

    private void loadUserData() {
        if (currentUid == null) return;
        mDatabase.child("users").child(currentUid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                User user = snapshot.getValue(User.class);
                if (user != null) {
                    originalUsername = user.getUsername();
                    etDisplayName.setText(originalUsername);
                    etBio.setText(user.getBio());
                    selectedAvatarName = user.getAvatarUrl();
                    updateAvatarPreview(selectedAvatarName);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void validateAndSave() {
        String newName = etDisplayName.getText().toString().trim();
        if (TextUtils.isEmpty(newName)) { etDisplayName.setError("Name required"); return; }
        
        setLoading(true);

        // FIX 1: Check if username is taken by someone else
        if (newName.equalsIgnoreCase(originalUsername)) {
            performUpdate(newName);
        } else {
            Query query = mDatabase.child("users").orderByChild("username").equalTo(newName);
            query.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        setLoading(false);
                        etDisplayName.setError("Username already taken!");
                    } else {
                        performUpdate(newName);
                    }
                }
                @Override public void onCancelled(@NonNull DatabaseError error) { setLoading(false); }
            });
        }
    }

    private void performUpdate(String name) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("username", name);
        updates.put("bio", etBio.getText().toString().trim());
        updates.put("avatarUrl", selectedAvatarName);

        mDatabase.child("users").child(currentUid).updateChildren(updates).addOnSuccessListener(aVoid -> {
            if (isAdded()) {
                Toast.makeText(getContext(), "Profile updated! ✨", Toast.LENGTH_SHORT).show();
                getParentFragmentManager().popBackStack();
            }
        }).addOnFailureListener(e -> setLoading(false));
    }

    private void setLoading(boolean loading) {
        if (pbLoading != null) pbLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSave.setEnabled(!loading);
        btnSave.setAlpha(loading ? 0.5f : 1.0f);
    }

    private void setupAvatarGrid() {
        List<Integer> avatars = Arrays.asList(R.drawable.prof1, R.drawable.prof2, R.drawable.prof3, R.drawable.prof4, R.drawable.prof5, R.drawable.prof6, R.drawable.prof7, R.drawable.prof8, R.drawable.prof9, R.drawable.prof10);
        adapter = new AvatarAdapter(avatars, pos -> {
            selectedAvatarName = getResources().getResourceEntryName(avatars.get(pos));
            updateAvatarPreview(selectedAvatarName);
        });
        rvAvatars.setLayoutManager(new GridLayoutManager(getContext(), 5));
        rvAvatars.setAdapter(adapter);
    }

    private void updateAvatarPreview(String name) {
        int resId = getResources().getIdentifier(name, "drawable", getContext().getPackageName());
        ivAvatarPreview.setImageResource(resId != 0 ? resId : R.drawable.prof1);
    }
}
