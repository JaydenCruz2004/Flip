package com.example.flip.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.flip.MainActivity;
import com.example.flip.R;
import com.example.flip.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class HomeFragment extends Fragment {

    private TextView usernameText, streakBadge, rankingValue, pointsValue, gamesValue;
    private ImageView profileImage;
    private LinearLayout leaderboardContainer, activityContainer, scheduleContainer;

    private FirebaseAuth auth;
    private FirebaseDatabase database;
    private User currentUser;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_home, container, false);

        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();

        // Initialize views
        profileImage = root.findViewById(R.id.profileImage);
        usernameText = root.findViewById(R.id.usernameText);
        streakBadge = root.findViewById(R.id.streakBadge);
        rankingValue = root.findViewById(R.id.rankingValue);
        pointsValue = root.findViewById(R.id.pointsValue);
        gamesValue = root.findViewById(R.id.gamesValue);

        leaderboardContainer = root.findViewById(R.id.leaderboardContainer);
        activityContainer = root.findViewById(R.id.activityContainer);
        scheduleContainer = root.findViewById(R.id.scheduleContainer);

        // Load profile data (for now, using mock data)
        loadProfileData();

        // Populate sections
        populateLeaderboard();
//        populateRecentActivity();
//        populateSchedule();

        return root;
    }

    private void loadProfileData() {
        // Try to get user from MainActivity first
        if (getActivity() instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) getActivity();
            currentUser = mainActivity.getCurrentUser();

            if (currentUser != null) {
                displayUserData();
                return;
            }
        }

        // If not available, load from Firebase
        String uid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;

        if (uid != null) {
            DatabaseReference userRef = database.getReference("users").child(uid);
            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        currentUser = snapshot.getValue(User.class);
                        displayUserData();
                    } else {
                        Toast.makeText(getContext(), "User data not found. Using mock data.", Toast.LENGTH_SHORT).show();
                        // If user doesn't exist in database, use mock data
                        displayMockData();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    // On error, display mock data
                    displayMockData();
                }
            });
        } else {
            // No user logged in, use mock data
            displayMockData();
        }
    }

    private void displayMockData() {
        User user = new User();
        user.setUsername("Loganwins");
        user.setStreak(36);
        user.setRanking(5);
        user.setPoints(987);
        user.setGamesPlayed(105);

        usernameText.setText("@" + user.getUsername());
        streakBadge.setText("🔥 " + user.getStreak() + " Day Streak");
        rankingValue.setText(getOrdinal(user.getRanking()));
        pointsValue.setText(String.valueOf(user.getPoints()));
        gamesValue.setText(String.valueOf(user.getGamesPlayed()));
    }

    private void displayUserData() {
        if(currentUser != null) {
            String username = currentUser.getUsername();
            if (username != null && !username.isEmpty()) {
                usernameText.setText("@" + username);
            } else {
                usernameText.setText("@User");
            }

            streakBadge.setText("🔥 " + currentUser.getStreak() + " Day Streak");
            rankingValue.setText(getOrdinal(currentUser.getRanking()));
            pointsValue.setText(String.valueOf(currentUser.getPoints()));
            gamesValue.setText(String.valueOf(currentUser.getGamesPlayed()));
        }
    }

    private void populateLeaderboard() {
        // Mock leaderboard data
        addLeaderboardItem("TaliaShaw", 1436, 1);
        addLeaderboardItem("kysonbrown", 1592, 2);
        addLeaderboardItem("jo8945", 1034, 3);
    }

    private void addLeaderboardItem(String username, int points, int position) {
        //View itemView = LayoutInflater.from(getContext())
          //      .inflate(R.layout.item_leaderboard, leaderboardContainer, false);

        // Set up the leaderboard item views here
        // TextView usernameTV = itemView.findViewById(R.id.leaderboardUsername);
        // TextView pointsTV = itemView.findViewById(R.id.leaderboardPoints);
        // usernameTV.setText("@" + username);
        // pointsTV.setText(points + " pts");

        //leaderboardContainer.addView(itemView);
    }

//    private void populateRecentActivity() {
//        // Mock activity data
//        addActivityItem("KayBear35", "invited you to play InfoSystems Ch3", "30m");
//        addActivityItem("Micah25", "Requested to follow you", "2hr");
//        addActivityItem("Sam_Jones", "Requested to follow you", "1D");
//    }

//    private void addActivityItem(String username, String action, String time) {
//        View itemView = LayoutInflater.from(getContext())
//                .inflate(R.layout.item_activity, activityContainer, false);
//
//        // Set up the activity item views here
//
//        activityContainer.addView(itemView);
//    }

//    private void populateSchedule() {
//        // Mock schedule data
//        String[] times = {"9:00", "10:00", "11:00", "12:00", "1:00", "2:00", "3:00", "4:00"};
//
//        for (String time : times) {
//            addScheduleItem(time);
//        }
//    }

//    private void addScheduleItem(String time) {
//        View itemView = LayoutInflater.from(getContext())
//                .inflate(R.layout.item_schedule, scheduleContainer, false);
//
//        // Set up the schedule item views here
//
//        scheduleContainer.addView(itemView);
//    }

    private String getOrdinal(int number) {
        if (number == 0) return "N/A";
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