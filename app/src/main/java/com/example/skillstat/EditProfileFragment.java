package com.example.skillstat;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Arrays;
import java.util.List;

public class EditProfileFragment extends Fragment {

    private RecyclerView rvAvatars;
    private AvatarAdapter adapter;
    private TextView tvAvatarPreview;
    private EditText etDisplayName, etBio;
    private View btnBack, btnSave, btnCancel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_edit_profile, container, false);

        rvAvatars = view.findViewById(R.id.rv_edit_avatars);
        tvAvatarPreview = view.findViewById(R.id.tv_edit_avatar_emoji);
        etDisplayName = view.findViewById(R.id.et_edit_display_name);
        etBio = view.findViewById(R.id.et_edit_bio);
        btnBack = view.findViewById(R.id.btn_back);
        btnSave = view.findViewById(R.id.btn_save_changes);
        btnCancel = view.findViewById(R.id.btn_cancel_edit);

        setupAvatarGrid();

        btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());
        btnCancel.setOnClickListener(v -> getParentFragmentManager().popBackStack());
        
        btnSave.setOnClickListener(v -> {
            // Logic to save changes would go here
            getParentFragmentManager().popBackStack();
        });

        return view;
    }

    private void setupAvatarGrid() {
        List<String> emojiAvatars = Arrays.asList(
                "🧙‍♂️", "🦊", "🐉", "🦁", "🐺",
                "🦅", "🐸", "🤖", "👾", "🧜‍♀️",
                "🐨", "🦋", "🐬", "🦄", "🐯"
        );

        adapter = new AvatarAdapter(emojiAvatars, position -> {
            tvAvatarPreview.setText(emojiAvatars.get(position));
        });

        rvAvatars.setLayoutManager(new GridLayoutManager(getContext(), 6));
        rvAvatars.setAdapter(adapter);
    }
}