package com.example.skillstat.models;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;
import java.util.concurrent.TimeUnit;

@IgnoreExtraProperties
public class JointQuest {
    private String questId;
    private String creatorUid;
    private String partnerUid;
    private String skillName;
    private int goalMinutes;
    private double creatorMinutes;
    private double partnerMinutes;
    private long startTime;
    private int durationDays;
    private String status; // "pending", "active", "completed", "failed"

    public JointQuest() {
        // Required for Firebase
    }

    public JointQuest(String questId, String creatorUid, String partnerUid, String skillName, int goalMinutes, int durationDays) {
        this.questId = questId;
        this.creatorUid = creatorUid;
        this.partnerUid = partnerUid;
        this.skillName = skillName;
        this.goalMinutes = goalMinutes;
        this.durationDays = durationDays;
        this.startTime = System.currentTimeMillis();
        this.status = "active";
        this.creatorMinutes = 0;
        this.partnerMinutes = 0;
    }

    public String getQuestId() { return questId; }
    public void setQuestId(String questId) { this.questId = questId; }

    public String getCreatorUid() { return creatorUid; }
    public void setCreatorUid(String creatorUid) { this.creatorUid = creatorUid; }

    public String getPartnerUid() { return partnerUid; }
    public void setPartnerUid(String partnerUid) { this.partnerUid = partnerUid; }

    public String getSkillName() { return skillName; }
    public void setSkillName(String skillName) { this.skillName = skillName; }

    public int getGoalMinutes() { return goalMinutes; }
    public void setGoalMinutes(int goalMinutes) { this.goalMinutes = goalMinutes; }

    public double getCreatorMinutes() { return creatorMinutes; }
    public void setCreatorMinutes(double creatorMinutes) { this.creatorMinutes = creatorMinutes; }

    public double getPartnerMinutes() { return partnerMinutes; }
    public void setPartnerMinutes(double partnerMinutes) { this.partnerMinutes = partnerMinutes; }

    public long getStartTime() { return startTime; }
    public void setStartTime(long startTime) { this.startTime = startTime; }

    public int getDurationDays() { return durationDays; }
    public void setDurationDays(int durationDays) { this.durationDays = durationDays; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Exclude
    public long getEndTime() {
        return startTime + TimeUnit.DAYS.toMillis(durationDays);
    }

    @Exclude
    public double getTotalMinutes() {
        return creatorMinutes + partnerMinutes;
    }

    @Exclude
    public float getProgress() {
        if (goalMinutes <= 0) return 0;
        return (float) (getTotalMinutes() / goalMinutes);
    }
}
