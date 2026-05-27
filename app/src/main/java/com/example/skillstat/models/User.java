package com.example.skillstat.models;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@IgnoreExtraProperties
public class User {
    private String uid, username, email, avatarUrl, bio, lastWeeklyResetWeek;
    private List<String> skills, badges;
    private Map<String, List<String>> subSkills;
    private Map<String, Double> subSkillMastery, skillMastery, skillMultipliers;
    private Map<String, Integer> skillDecaySettings, skillDailyGoals;
    private Map<String, Long> skillLastPractice;
    private Map<String, Object> activeDuels, completedDuels, friends, friendRequests, sentRequests, notifications, dailyChallenges, activeQuests, recentChats;
    private Map<String, PracticeSession> sessions, personalBests;
    private Map<String, Map<String, Double>> history;

    private int totalPoints, weeklyPoints, streak, bestStreak, totalDays, dailyGoalMinutes, currentDailyMinutes, wins, losses, challengeXpToday, streakFreezes;
    private long lastPracticeTimestamp, lastOnlineTimestamp, lastNotificationTimestamp;
    private boolean online, dailyRemindersEnabled = true, decayAlertsEnabled = true, smartSchedulingEnabled = true;

    public User() {}

    public User(String uid, String username, String email) {
        this.uid = uid; this.username = username; this.email = email;
        this.avatarUrl = "prof1"; this.bio = "None";
        this.skills = new ArrayList<>(); 
        this.subSkills = new HashMap<>();
        this.subSkillMastery = new HashMap<>(); 
        this.skillMastery = new HashMap<>();
        this.skillDecaySettings = new HashMap<>();
        this.skillDailyGoals = new HashMap<>();
        this.skillLastPractice = new HashMap<>();
        this.personalBests = new HashMap<>(); 
        this.history = new HashMap<>();
        this.recentChats = new HashMap<>();
        this.dailyGoalMinutes = 30;
    }

    // Standard Getters and Setters
    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getAvatarUrl() { return avatarUrl != null ? avatarUrl : "prof1"; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
    public String getLastWeeklyResetWeek() { return lastWeeklyResetWeek; }
    public void setLastWeeklyResetWeek(String l) { this.lastWeeklyResetWeek = l; }

    public List<String> getSkills() { 
        if (skills == null) skills = new ArrayList<>();
        return skills; 
    }
    public void setSkills(List<String> skills) { this.skills = skills; }
    public List<String> getBadges() { 
        if (badges == null) badges = new ArrayList<>();
        return badges; 
    }
    public void setBadges(List<String> badges) { this.badges = badges; }

    public Map<String, List<String>> getSubSkills() { 
        if (subSkills == null) subSkills = new HashMap<>();
        return subSkills; 
    }
    public void setSubSkills(Map<String, List<String>> s) { this.subSkills = s; }
    public Map<String, Double> getSubSkillMastery() { 
        if (subSkillMastery == null) subSkillMastery = new HashMap<>();
        return subSkillMastery; 
    }
    public void setSubSkillMastery(Map<String, Double> s) { this.subSkillMastery = s; }
    public Map<String, Double> getSkillMastery() { 
        if (skillMastery == null) skillMastery = new HashMap<>();
        return skillMastery; 
    }
    public void setSkillMastery(Map<String, Double> skillMastery) { this.skillMastery = skillMastery; }
    public Map<String, Double> getSkillMultipliers() { 
        if (skillMultipliers == null) skillMultipliers = new HashMap<>();
        return skillMultipliers; 
    }
    public void setSkillMultipliers(Map<String, Double> s) { this.skillMultipliers = s; }

    public Map<String, Integer> getSkillDecaySettings() { 
        if (skillDecaySettings == null) skillDecaySettings = new HashMap<>();
        return skillDecaySettings; 
    }
    public void setSkillDecaySettings(Map<String, Integer> s) { this.skillDecaySettings = s; }
    public Map<String, Long> getSkillLastPractice() { 
        if (skillLastPractice == null) skillLastPractice = new HashMap<>();
        return skillLastPractice; 
    }
    public void setSkillLastPractice(Map<String, Long> s) { this.skillLastPractice = s; }
    public Map<String, Integer> getSkillDailyGoals() { 
        if (skillDailyGoals == null) skillDailyGoals = new HashMap<>();
        return skillDailyGoals; 
    }
    public void setSkillDailyGoals(Map<String, Integer> s) { this.skillDailyGoals = s; }

    public Map<String, Object> getActiveDuels() { return activeDuels; }
    public void setActiveDuels(Map<String, Object> a) { this.activeDuels = a; }
    public Map<String, Object> getCompletedDuels() { return completedDuels; }
    public void setCompletedDuels(Map<String, Object> c) { this.completedDuels = c; }
    public Map<String, Object> getFriends() { return friends; }
    public void setFriends(Map<String, Object> f) { this.friends = f; }
    public Map<String, Object> getFriendRequests() { return friendRequests; }
    public void setFriendRequests(Map<String, Object> f) { this.friendRequests = f; }
    public Map<String, Object> getSentRequests() { return sentRequests; }
    public void setSentRequests(Map<String, Object> s) { this.sentRequests = s; }
    public Map<String, Object> getNotifications() { return notifications; }
    public void setNotifications(Map<String, Object> n) { this.notifications = n; }
    public Map<String, Object> getDailyChallenges() { return dailyChallenges; }
    public void setDailyChallenges(Map<String, Object> d) { this.dailyChallenges = d; }
    public Map<String, Object> getActiveQuests() { return activeQuests; }
    public void setActiveQuests(Map<String, Object> q) { this.activeQuests = q; }
    public Map<String, Object> getRecentChats() { 
        if (recentChats == null) recentChats = new HashMap<>();
        return recentChats; 
    }
    public void setRecentChats(Map<String, Object> r) { this.recentChats = r; }

    public Map<String, PracticeSession> getSessions() { 
        if (sessions == null) sessions = new HashMap<>();
        return sessions; 
    }
    public void setSessions(Map<String, PracticeSession> sessions) { this.sessions = sessions; }
    public Map<String, PracticeSession> getPersonalBests() { 
        if (personalBests == null) personalBests = new HashMap<>();
        return personalBests; 
    }
    public void setPersonalBests(Map<String, PracticeSession> p) { this.personalBests = p; }
    public Map<String, Map<String, Double>> getHistory() { 
        if (history == null) history = new HashMap<>();
        return history; 
    }
    public void setHistory(Map<String, Map<String, Double>> history) { this.history = history; }

    public int getTotalPoints() { return totalPoints; }
    public void setTotalPoints(int totalPoints) { this.totalPoints = totalPoints; }
    public int getWeeklyPoints() { return weeklyPoints; }
    public void setWeeklyPoints(int weeklyPoints) { this.weeklyPoints = weeklyPoints; }
    public int getStreak() { return streak; }
    public void setStreak(int streak) { this.streak = streak; }
    public int getBestStreak() { return bestStreak; }
    public void setBestStreak(int b) { this.bestStreak = b; }
    public int getTotalDays() { return totalDays; }
    public void setTotalDays(int totalDays) { this.totalDays = totalDays; }
    public int getDailyGoalMinutes() { return dailyGoalMinutes; }
    public void setDailyGoalMinutes(int d) { this.dailyGoalMinutes = d; }
    public int getCurrentDailyMinutes() { return currentDailyMinutes; }
    public void setCurrentDailyMinutes(int c) { this.currentDailyMinutes = c; }
    public int getWins() { return wins; }
    public void setWins(int wins) { this.wins = wins; }
    public int getLosses() { return losses; }
    public void setLosses(int losses) { this.losses = losses; }
    public int getChallengeXpToday() { return challengeXpToday; }
    public void setChallengeXpToday(int challengeXpToday) { this.challengeXpToday = challengeXpToday; }
    public int getStreakFreezes() { return streakFreezes; }
    public void setStreakFreezes(int s) { this.streakFreezes = s; }

    public long getLastPracticeTimestamp() { return lastPracticeTimestamp; }
    public void setLastPracticeTimestamp(long lastPracticeTimestamp) { this.lastPracticeTimestamp = lastPracticeTimestamp; }
    public long getLastOnlineTimestamp() { return lastOnlineTimestamp; }
    public void setLastOnlineTimestamp(long l) { this.lastOnlineTimestamp = l; }
    public long getLastNotificationTimestamp() { return lastNotificationTimestamp; }
    public void setLastNotificationTimestamp(long l) { this.lastNotificationTimestamp = l; }

    public boolean isOnline() { return online; }
    public void setOnline(boolean online) { this.online = online; }
    public boolean isDailyRemindersEnabled() { return dailyRemindersEnabled; }
    public void setDailyRemindersEnabled(boolean d) { this.dailyRemindersEnabled = d; }
    public boolean isDecayAlertsEnabled() { return decayAlertsEnabled; }
    public void setDailyRemindersEnabled(boolean d, boolean unused) { this.decayAlertsEnabled = d; } // Placeholder for older calls
    public void setDecayAlertsEnabled(boolean d) { this.decayAlertsEnabled = d; }
    public boolean isSmartSchedulingEnabled() { return smartSchedulingEnabled; }
    public void setSmartSchedulingEnabled(boolean s) { this.smartSchedulingEnabled = s; }

    @Exclude
    public double getWinRate() {
        int total = wins + losses;
        return total == 0 ? 0 : (double) wins / total * 100;
    }

    @Exclude
    public int getLevel() { return (totalPoints / 500) + 1; }

    @Exclude
    public String getRankName() {
        if (totalPoints < 1000) return "Bronze";
        if (totalPoints < 3000) return "Silver";
        if (totalPoints < 6000) return "Gold";
        return "Platinum";
    }

    @Exclude
    public String getRankEmoji() {
        if (totalPoints < 1000) return "🥉";
        if (totalPoints < 3000) return "🥈";
        if (totalPoints < 6000) return "🥇";
        return "💎";
    }

    @Exclude
    public String getAvatarEmoji() {
        String url = getAvatarUrl();
        if (url == null) return "👤";
        switch (url) {
            case "prof1": return "👦";
            case "prof2": return "👧";
            case "prof3": return "👨";
            case "prof4": return "👩";
            case "prof5": return "👴";
            case "prof6": return "👵";
            case "prof7": return "👨‍🎓";
            case "prof8": return "👩‍🎓";
            case "prof9": return "👨‍🏫";
            case "prof10": return "👩‍🏫";
            case "prof11": return "👨‍💻";
            case "prof12": return "👩‍💻";
            case "prof13": return "👨‍🎨";
            case "prof14": return "👩‍🎨";
            case "prof15": return "🕵️";
            default: return url.length() <= 2 ? url : "👤";
        }
    }

    @Exclude
    public int getTotalSessionsCount() { return sessions != null ? sessions.size() : 0; }

    @Exclude
    public boolean isChallengeCompleted(String key) {
        if (dailyChallenges == null) return false;
        Object val = dailyChallenges.get(key);
        return (val instanceof Boolean && (Boolean) val) || (val instanceof String && "true".equalsIgnoreCase((String) val));
    }

    @Exclude
    public int getCompletedChallengesCount() {
        if (dailyChallenges == null) return 0;
        int count = 0;
        for (Object v : dailyChallenges.values()) if (isChallengeCompleted(v)) count++;
        return count;
    }

    private boolean isChallengeCompleted(Object val) {
        return (val instanceof Boolean && (Boolean) val) || (val instanceof String && "true".equalsIgnoreCase((String) val));
    }
}
