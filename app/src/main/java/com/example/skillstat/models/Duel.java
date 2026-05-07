package com.example.skillstat.models;

public class Duel {
    private String duelId;
    private String initiatorUid;
    private String opponentUid;
    private String skillName;
    private int durationDays;
    private long startTime;
    private int initiatorStartMastery;
    private int opponentStartMastery;
    private int initiatorCurrentMastery;
    private int opponentCurrentMastery;
    private String status; // "active", "completed"

    public Duel() {
        // Required for Firebase
    }

    public Duel(String duelId, String initiatorUid, String opponentUid, String skillName, int durationDays) {
        this.duelId = duelId;
        this.initiatorUid = initiatorUid;
        this.opponentUid = opponentUid;
        this.skillName = skillName;
        this.durationDays = durationDays;
        this.startTime = System.currentTimeMillis();
        this.status = "active";
    }

    // Getters and Setters
    public String getDuelId() { return duelId; }
    public void setDuelId(String duelId) { this.duelId = duelId; }

    public String getInitiatorUid() { return initiatorUid; }
    public void setInitiatorUid(String initiatorUid) { this.initiatorUid = initiatorUid; }

    public String getOpponentUid() { return opponentUid; }
    public void setOpponentUid(String opponentUid) { this.opponentUid = opponentUid; }

    public String getSkillName() { return skillName; }
    public void setSkillName(String skillName) { this.skillName = skillName; }

    public int getDurationDays() { return durationDays; }
    public void setDurationDays(int durationDays) { this.durationDays = durationDays; }

    public long getStartTime() { return startTime; }
    public void setStartTime(long startTime) { this.startTime = startTime; }

    public int getInitiatorStartMastery() { return initiatorStartMastery; }
    public void setInitiatorStartMastery(int initiatorStartMastery) { this.initiatorStartMastery = initiatorStartMastery; }

    public int getOpponentStartMastery() { return opponentStartMastery; }
    public void setOpponentStartMastery(int opponentStartMastery) { this.opponentStartMastery = opponentStartMastery; }

    public int getInitiatorCurrentMastery() { return initiatorCurrentMastery; }
    public void setInitiatorCurrentMastery(int initiatorCurrentMastery) { this.initiatorCurrentMastery = initiatorCurrentMastery; }

    public int getOpponentCurrentMastery() { return opponentCurrentMastery; }
    public void setOpponentCurrentMastery(int opponentCurrentMastery) { this.opponentCurrentMastery = opponentCurrentMastery; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
