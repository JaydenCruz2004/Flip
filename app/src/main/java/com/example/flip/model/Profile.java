package com.example.flip.model;

public class Profile {
    private String Username;
    private String profileImageUrl;
    private int points;
    private int streak;

    public Profile() {
    }
    public Profile(String username, String profileImageUrl, int points, int streak) {
        Username = username;
        this.profileImageUrl = profileImageUrl;
        this.points = points;
        this.streak = streak;
    }

    public String getUsername() {
        return Username;
    }
    public void setUsername(String username) {
        Username = username;
    }
    public String getProfileImageUrl() {
        return profileImageUrl;
    }
    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }
    public int getPoints() {
        return points;
    }
    public void setPoints(int points) {
        this.points = points;
    }
    public int getStreak() {
        return streak;
    }
    public void setStreak(int streak) {
        this.streak = streak;
    }

}
