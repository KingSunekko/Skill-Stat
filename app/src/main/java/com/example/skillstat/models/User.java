package com.example.skillstat.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class User {
    private String uid;
    private String username;
    private String email;
    private String avatarUrl;
    private List<String> skills;
    private Map<String, Integer> skillMastery;
    private List<String> badges;
    private Map<String, Boolean> activeDuels; // Map of duelId -> true
    private int totalPoints;
    private int streak;
    private int totalDays;
    private int dailyGoalMinutes;
    private int currentDailyMinutes;
    private long lastPracticeTimestamp;

    public User() {
        // Required for Firebase
    }

    public User(String uid, String username, String email) {
        this.uid = uid;
        this.username = username;
        this.email = email;
        this.avatarUrl = "🧙‍♂️";
        this.skills = new ArrayList<>();
        this.skillMastery = new HashMap<>();
        this.badges = new ArrayList<>();
        this.activeDuels = new HashMap<>();
        this.totalPoints = 0;
        this.streak = 0;
        this.totalDays = 0;
        this.dailyGoalMinutes = 10;
        this.currentDailyMinutes = 0;
        this.lastPracticeTimestamp = 0;
    }

    // Getters and Setters
    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public List<String> getSkills() { return skills; }
    public void setSkills(List<String> skills) { this.skills = skills; }

    public Map<String, Integer> getSkillMastery() { 
        if (skillMastery == null) skillMastery = new HashMap<>();
        return skillMastery; 
    }
    public void setSkillMastery(Map<String, Integer> skillMastery) { this.skillMastery = skillMastery; }

    public List<String> getBadges() { 
        if (badges == null) badges = new ArrayList<>();
        return badges; 
    }
    public void setBadges(List<String> badges) { this.badges = badges; }

    public Map<String, Boolean> getActiveDuels() {
        if (activeDuels == null) activeDuels = new HashMap<>();
        return activeDuels;
    }
    public void setActiveDuels(Map<String, Boolean> activeDuels) { this.activeDuels = activeDuels; }

    public int getTotalPoints() { return totalPoints; }
    public void setTotalPoints(int totalPoints) { this.totalPoints = totalPoints; }

    public int getStreak() { return streak; }
    public void setStreak(int streak) { this.streak = streak; }

    public int getTotalDays() { return totalDays; }
    public void setTotalDays(int totalDays) { this.totalDays = totalDays; }

    public int getDailyGoalMinutes() { return dailyGoalMinutes; }
    public void setDailyGoalMinutes(int dailyGoalMinutes) { this.dailyGoalMinutes = dailyGoalMinutes; }

    public int getCurrentDailyMinutes() { return currentDailyMinutes; }
    public void setCurrentDailyMinutes(int currentDailyMinutes) { this.currentDailyMinutes = currentDailyMinutes; }

    public long getLastPracticeTimestamp() { return lastPracticeTimestamp; }
    public void setLastPracticeTimestamp(long lastPracticeTimestamp) { this.lastPracticeTimestamp = lastPracticeTimestamp; }
}
