package com.example.skillstat;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class SplashScreen extends AppCompatActivity {

    private static final int SPLASH_DURATION_MS = 3000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash_screen);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        View     logoContainer = findViewById(R.id.logo_container);
        TextView  tvAppName     = findViewById(R.id.tv_app_name);
        TextView  tvTagline     = findViewById(R.id.tv_tagline);
        View      btnScience    = findViewById(R.id.btn_powered_by_science);
        View      dotsContainer = findViewById(R.id.dots_container);
        View      dot1          = findViewById(R.id.dot1);
        View      dot2          = findViewById(R.id.dot2);
        View      dot3          = findViewById(R.id.dot3);

        // Show all elements immediately
        logoContainer.setVisibility(View.VISIBLE);
        tvAppName.setVisibility(View.VISIBLE);
        tvTagline.setVisibility(View.VISIBLE);
        btnScience.setVisibility(View.VISIBLE);
        dotsContainer.setVisibility(View.VISIBLE);

        // ── Professional "SkillStat" Logo Pop ──
        // Combining high-energy scale, organic squash/stretch, and a rotation jiggle
        logoContainer.setScaleX(0f);
        logoContainer.setScaleY(0f);
        logoContainer.setRotation(-15f); // Start with a professional tilt
        
        // Organic bouncy keyframes
        PropertyValuesHolder pvhScaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 0f, 1.3f, 0.85f, 1.1f, 1f);
        PropertyValuesHolder pvhScaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 0f, 0.7f, 1.2f, 0.95f, 1f);
        PropertyValuesHolder pvhRotation = PropertyValuesHolder.ofFloat(View.ROTATION, -15f, 10f, -5f, 2f, 0f);
        
        ObjectAnimator popAnim = ObjectAnimator.ofPropertyValuesHolder(logoContainer, pvhScaleX, pvhScaleY, pvhRotation);
        popAnim.setDuration(1200);
        popAnim.setInterpolator(new AccelerateDecelerateInterpolator());
        popAnim.start();

        // "Arriving" slide-up effect with its own overshoot
        ObjectAnimator liftAnim = ObjectAnimator.ofFloat(logoContainer, View.TRANSLATION_Y, 120f, 0f);
        liftAnim.setDuration(1000);
        liftAnim.setInterpolator(new OvershootInterpolator(1.4f));
        liftAnim.start();

        // ── Rhythmic Wave Animation for dots ──────────────────────────────────
        animateDot(dot1, 0);
        animateDot(dot2, 150);
        animateDot(dot3, 300);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent intent = new Intent(SplashScreen.this, LoginActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        }, SPLASH_DURATION_MS);
    }

    private void animateDot(View view, long delay) {
        // Translation: Bounces up
        PropertyValuesHolder pvhY = PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, 0f, -28f, 0f);
        // Squish/Stretch: Professional organic feel
        PropertyValuesHolder pvhScaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 0.9f, 1.1f, 1f);
        PropertyValuesHolder pvhScaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.1f, 0.9f, 1f);

        ObjectAnimator animator = ObjectAnimator.ofPropertyValuesHolder(view, pvhY, pvhScaleX, pvhScaleY);
        animator.setDuration(900);
        animator.setStartDelay(delay);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setRepeatMode(ValueAnimator.RESTART);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.start();
    }
}