package com.example.flip.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.flip.R;
import com.example.flip.model.Profile;

public class HomeFragment extends Fragment {

    private TextView usernameText, streakBadge, rankingValue, pointsValue, gamesValue;
    private ImageView profileImage;
    private LinearLayout leaderboardContainer, activityContainer, scheduleContainer;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_home, container, false);

        // Initialize views
        profileImage = root.findViewById(R.id.profileImage);
        usernameText = root.findViewById(R.id.usernameText);
        streakBadge = root.findViewById(R.id.streakBadge);
        pointsValue = root.findViewById(R.id.pointsValue);

        leaderboardContainer = root.findViewById(R.id.leaderboardContainer);
        activityContainer = root.findViewById(R.id.activityContainer);
        scheduleContainer = root.findViewById(R.id.scheduleContainer);

        // Load profile data (for now, using mock data)
        loadProfileData();

        // Populate sections
        populateLeaderboard();
        populateRecentActivity();
        populateSchedule();

        return root;
    }

    private void loadProfileData() {
        // Mock data - later you'll load this from Firebase
        Profile profile = new Profile("kokoOnTop", "placeholder_url", 1500, 3);

        usernameText.setText("@" + profile.getUsername());
        streakBadge.setText("🔥 " + profile.getStreak() + " Day Streak");
        pointsValue.setText(String.valueOf(profile.getPoints()));
    }

    private void populateLeaderboard() {
        // Mock leaderboard data
        addLeaderboardItem("TaliaShaw", 1436, 1);
        addLeaderboardItem("kysonbrown", 1592, 2);
        addLeaderboardItem("jo8945", 1034, 3);
    }

    private void addLeaderboardItem(String username, int points, int position) {
        View itemView = LayoutInflater.from(getContext())
                .inflate(R.layout.item_leaderboard, leaderboardContainer, false);

        // Set up the leaderboard item views here
        // TextView usernameTV = itemView.findViewById(R.id.leaderboardUsername);
        // TextView pointsTV = itemView.findViewById(R.id.leaderboardPoints);
        // usernameTV.setText("@" + username);
        // pointsTV.setText(points + " pts");

        leaderboardContainer.addView(itemView);
    }

    private void populateRecentActivity() {
        // Mock activity data
        addActivityItem("KayBear35", "invited you to play InfoSystems Ch3", "30m");
        addActivityItem("Micah25", "Requested to follow you", "2hr");
        addActivityItem("Sam_Jones", "Requested to follow you", "1D");
    }

    private void addActivityItem(String username, String action, String time) {
        View itemView = LayoutInflater.from(getContext())
                .inflate(R.layout.item_activity, activityContainer, false);

        // Set up the activity item views here

        activityContainer.addView(itemView);
    }

    private void populateSchedule() {
        // Mock schedule data
        String[] times = {"9:00", "10:00", "11:00", "12:00", "1:00", "2:00", "3:00", "4:00"};

        for (String time : times) {
            addScheduleItem(time);
        }
    }

    private void addScheduleItem(String time) {
        View itemView = LayoutInflater.from(getContext())
                .inflate(R.layout.item_schedule, scheduleContainer, false);

        // Set up the schedule item views here

        scheduleContainer.addView(itemView);
    }

    private String getOrdinal(int number) {
        String[] suffixes = {"TH", "ST", "ND", "RD", "TH", "TH", "TH", "TH", "TH", "TH"};

        switch (number % 100) {
            case 11:
            case 12:
            case 13:
                return number + "TH";
            default:
                return number + suffixes[number % 10];
        }
    }
}