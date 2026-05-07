package com.example.skillstat.models;

public class PracticeSession {
    private String skillName;
    private int minutes;
    private int xpEarned;
    private long timestamp;

    public PracticeSession() {
        // Required for Firebase
    }

    public PracticeSession(String skillName, int minutes, int xpEarned, long timestamp) {
        this.skillName = skillName;
        this.minutes = minutes;
        this.xpEarned = xpEarned;
        this.timestamp = timestamp;
    }

    // Getters and Setters
    public String getSkillName() { return skillName; }
    public void setSkillName(String skillName) { this.skillName = skillName; }

    public int getMinutes() { return minutes; }
    public void setMinutes(int minutes) { this.minutes = minutes; }

    public int getXpEarned() { return xpEarned; }
    public void setXpEarned(int xpEarned) { this.xpEarned = xpEarned; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
