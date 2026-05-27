package com.example.skillstat.utils;

public class ForgeUtils {

    public static String getLevelTitle(double mastery) {
        if (mastery >= 100) return "Grandmaster 👑";
        if (mastery >= 75) return "Master 🔥";
        if (mastery >= 50) return "Adept ✨";
        if (mastery >= 25) return "Apprentice 🛡️";
        return "Novice 🛠️";
    }

    public static int getRankLevel(double mastery) {
        if (mastery >= 100) return 4;
        if (mastery >= 75) return 3;
        if (mastery >= 50) return 2;
        if (mastery >= 25) return 1;
        return 0;
    }

    public static String getRankIcon(int level) {
        switch (level) {
            case 4: return "👑";
            case 3: return "🔥";
            case 2: return "✨";
            case 1: return "🛡️";
            default: return "🛠️";
        }
    }

    public static int getTierColor(double mastery) {
        if (mastery >= 100) return 0xFFFFD700; // Gold
        if (mastery >= 75) return 0xFFFF4500; // OrangeRed
        if (mastery >= 50) return 0xFF00BFFF; // DeepSkyBlue
        if (mastery >= 25) return 0xFF32CD32; // LimeGreen
        return 0xFFA9A9A9; // Gray
    }

    public static String getAvatarEmoji(String avatarUrl) {
        if (avatarUrl == null) return "👤";
        switch (avatarUrl) {
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
            default: return avatarUrl.length() <= 2 ? avatarUrl : "👤";
        }
    }
}
