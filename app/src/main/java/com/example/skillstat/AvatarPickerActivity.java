package com.example.skillstat;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.Arrays;
import java.util.List;

public class AvatarPickerActivity extends AppCompatActivity {

    private RecyclerView rvAvatars;
    private AvatarAdapter adapter;
    private EditText etDisplayName;
    private Button btnContinue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_avatar_picker);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        rvAvatars = findViewById(R.id.rv_avatars);
        etDisplayName = findViewById(R.id.et_display_name);
        btnContinue = findViewById(R.id.btn_continue);

        setupRecyclerView();

        btnContinue.setOnClickListener(v -> {
            // Navigate to MainActivity after avatar selection
            Intent intent = new Intent(AvatarPickerActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_up_enter, R.anim.gentle_fade_out);
            finish();
        });
    }

    private void setupRecyclerView() {
        List<String> emojiAvatars = Arrays.asList(
                "🧙‍♂️", "🦊", "🐉", "🦁", "🐺",
                "🦅", "🐸", "🤖", "👾", "🧜‍♀️",
                "🐨", "🦋", "🐬", "🦄", "🐯"
        );

        adapter = new AvatarAdapter(emojiAvatars, position -> {
            // Optional: handle selection changes
        });

        rvAvatars.setLayoutManager(new GridLayoutManager(this, 5));
        rvAvatars.setAdapter(adapter);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.gentle_fade_in, R.anim.slide_down_exit);
    }
}