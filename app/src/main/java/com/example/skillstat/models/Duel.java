package com.example.skillstat.models;

import com.google.firebase.database.IgnoreExtraProperties;
import java.util.concurrent.TimeUnit;

@IgnoreExtraProperties
public class Duel {
    private String duelId;
    private String initiatorUid;
    private String opponentUid;
    private String skillName;
    private int durationDays;
    private long startTime;
    private double initiatorStartMastery;
    private double opponentStartMastery;
    private double initiatorCurrentMastery;
    private double opponentCurrentMastery;
    private double initiatorEffort;
    private double opponentEffort;
    private String status; // "active", "completed", "pending"
    private int wagerAmount;
    private String winnerUid; // "draw" if it's a tie

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
        this.wagerAmount = 0;
        this.initiatorEffort = 0.0;
        this.opponentEffort = 0.0;
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

    public long getEndTime() {
        return startTime + TimeUnit.DAYS.toMillis(durationDays);
    }

    public double getInitiatorStartMastery() { return initiatorStartMastery; }
    public void setInitiatorStartMastery(double initiatorStartMastery) { this.initiatorStartMastery = initiatorStartMastery; }

    public double getOpponentStartMastery() { return opponentStartMastery; }
    public void setOpponentStartMastery(double opponentStartMastery) { this.opponentStartMastery = opponentStartMastery; }

    public double getInitiatorCurrentMastery() { return initiatorCurrentMastery; }
    public void setInitiatorCurrentMastery(double initiatorCurrentMastery) { this.initiatorCurrentMastery = initiatorCurrentMastery; }

    public double getOpponentCurrentMastery() { return opponentCurrentMastery; }
    public void setOpponentCurrentMastery(double opponentCurrentMastery) { this.opponentCurrentMastery = opponentCurrentMastery; }

    public double getInitiatorEffort() { return initiatorEffort; }
    public void setInitiatorEffort(double initiatorEffort) { this.initiatorEffort = initiatorEffort; }

    public double getOpponentEffort() { return opponentEffort; }
    public void setOpponentEffort(double opponentEffort) { this.opponentEffort = opponentEffort; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getWagerAmount() { return wagerAmount; }
    public void setWagerAmount(int wagerAmount) { this.wagerAmount = wagerAmount; }

    public String getWinnerUid() { return winnerUid; }
    public void setWinnerUid(String winnerUid) { this.winnerUid = winnerUid; }
}
