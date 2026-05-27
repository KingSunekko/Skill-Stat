package com.example.skillstat.models;

public class PracticeSession {
    private String userId;
    private String skillName;
    private int minutes;
    private int seconds;
    private int xpEarned;
    private double effortPoints;
    private long timestamp;
    private String note;
    private double masteryAfter = -1;
    private boolean zenMode = false;

    public PracticeSession() {}

    // Updated Constructor
    public PracticeSession(String skillName, int seconds, int xpEarned, long timestamp, String note, double masteryAfter, boolean zenMode) {
        this.skillName = skillName;
        this.seconds = seconds;
        this.minutes = Math.max(1, seconds / 60);
        this.xpEarned = xpEarned;
        this.timestamp = timestamp;
        this.note = note;
        this.masteryAfter = masteryAfter;
        this.effortPoints = xpEarned;
        this.zenMode = zenMode;
    }

    public PracticeSession(String userId, String skillName, int seconds, int xpEarned, long timestamp) {
        this.userId = userId;
        this.skillName = skillName;
        this.seconds = seconds;
        this.minutes = Math.max(1, seconds / 60);
        this.xpEarned = xpEarned;
        this.timestamp = timestamp;
        this.effortPoints = xpEarned;
    }

    // Getters and Setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getSkillName() { return skillName; }
    public void setSkillName(String skillName) { this.skillName = skillName; }
    public int getMinutes() { return minutes; }
    public void setMinutes(int minutes) { this.minutes = minutes; }
    public int getSeconds() { return seconds; }
    public void setSeconds(int seconds) { this.seconds = seconds; }
    public int getXpEarned() { return xpEarned; }
    public void setXpEarned(int xpEarned) { this.xpEarned = xpEarned; }
    public double getEffortPoints() { return effortPoints; }
    public void setEffortPoints(double effortPoints) { this.effortPoints = effortPoints; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public double getMasteryAfter() { return masteryAfter; }
    public void setMasteryAfter(double masteryAfter) { this.masteryAfter = masteryAfter; }
    public boolean isZenMode() { return zenMode; }
    public void setZenMode(boolean zenMode) { this.zenMode = zenMode; }
}
