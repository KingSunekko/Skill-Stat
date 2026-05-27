package com.example.skillstat;

import android.app.Application;
import com.google.firebase.database.FirebaseDatabase;

public class SkillStatApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Enable Firebase Offline Persistence
        // This allows the app to work without internet by caching data locally
        try {
            FirebaseDatabase.getInstance().setPersistenceEnabled(true);
        } catch (Exception e) {
            // Persistence must be set before any other usage of FirebaseDatabase
            e.printStackTrace();
        }
    }
}
