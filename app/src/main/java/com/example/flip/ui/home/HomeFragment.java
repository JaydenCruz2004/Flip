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

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

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

        // Load profile data
        loadProfileData();

        // Populate sections
        populateLeaderboard();

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh data when fragment becomes visible
        loadProfileData();
        populateLeaderboard();
    }

    private void loadProfileData() {
        String uid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;

        if (uid != null) {
            DatabaseReference userRef = database.getReference("users").child(uid);
            userRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        currentUser = snapshot.getValue(User.class);
                        if (currentUser != null) {
                            currentUser.setUid(uid);
                            // Calculate ranking among all users
                            calculateUserRanking(uid);
                        }
                        displayUserData();
                    } else {
                        // If user doesn't exist in database, use default values
                        displayDefaultData();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    // On error, display default data
                    displayDefaultData();
                }
            });
        } else {
            // No user logged in, use default data
            displayDefaultData();
        }
    }

    private void calculateUserRanking(String userId) {
        DatabaseReference usersRef = database.getReference("users");
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<User> allUsers = new ArrayList<>();

                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    User user = userSnapshot.getValue(User.class);
                    if (user != null && (user.getPoints() > 0 || user.getGamesPlayed() > 0)) {
                        user.setUid(userSnapshot.getKey());
                        allUsers.add(user);
                    }
                }

                // Sort by points
                Collections.sort(allUsers, new Comparator<User>() {
                    @Override
                    public int compare(User u1, User u2) {
                        return Integer.compare(u2.getPoints(), u1.getPoints());
                    }
                });

                // Find current user's rank
                for (int i = 0; i < allUsers.size(); i++) {
                    if (allUsers.get(i).getUid() != null &&
                            allUsers.get(i).getUid().equals(userId)) {
                        if (currentUser != null) {
                            currentUser.setRanking(i + 1);
                            displayUserData();
                        }
                        break;
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Ignore ranking calculation errors
            }
        });
    }

    private void displayDefaultData() {
        usernameText.setText("@User");
        streakBadge.setText("🔥 0 Day Streak");
        rankingValue.setText("N/A");
        pointsValue.setText("0");
        gamesValue.setText("0");
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

            // Format points with commas
            NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.US);
            pointsValue.setText(numberFormat.format(currentUser.getPoints()));

            gamesValue.setText(String.valueOf(currentUser.getGamesPlayed()));
        }
    }

    private void populateLeaderboard() {
        // Clear existing leaderboard items
        leaderboardContainer.removeAllViews();

        // Load top 3 users from Firebase
        DatabaseReference usersRef = database.getReference("users");
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<User> allUsers = new ArrayList<>();

                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    User user = userSnapshot.getValue(User.class);
                    if (user != null) {
                        user.setUid(userSnapshot.getKey());

                        // Only include users with points > 0
                        if (user.getPoints() > 0 || user.getGamesPlayed() > 0) {
                            allUsers.add(user);
                        }
                    }
                }

                // Sort by points (highest first)
                Collections.sort(allUsers, new Comparator<User>() {
                    @Override
                    public int compare(User u1, User u2) {
                        return Integer.compare(u2.getPoints(), u1.getPoints());
                    }
                });

                // Display top 3
                int limit = Math.min(3, allUsers.size());
                for (int i = 0; i < limit; i++) {
                    User user = allUsers.get(i);
                    addLeaderboardItem(user, i + 1);
                }

                // If no users, show placeholder
                if (allUsers.isEmpty()) {
                    addEmptyLeaderboardMessage();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(),
                        "Failed to load leaderboard",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addLeaderboardItem(User user, int position) {
        View itemView = LayoutInflater.from(getContext())
                .inflate(R.layout.item_leaderboard_mini, leaderboardContainer, false);

        TextView rankBadge = itemView.findViewById(R.id.rankBadgeMini);
        TextView usernameTV = itemView.findViewById(R.id.usernameMini);
        TextView pointsTV = itemView.findViewById(R.id.pointsMini);
        ImageView trophyIcon = itemView.findViewById(R.id.trophyIconMini);

        // Set rank
        rankBadge.setText(String.valueOf(position));

        // Set username
        String username = user.getUsername();
        if (username == null || username.isEmpty()) {
            username = user.getEmail() != null ? user.getEmail().split("@")[0] : "User";
        }
        usernameTV.setText("@" + username);

        // Set points with formatting
        NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.US);
        pointsTV.setText(numberFormat.format(user.getPoints()) + " pts");

        // Show trophy for top 3 with different colors
        trophyIcon.setVisibility(View.VISIBLE);
        switch (position) {
            case 1:
                trophyIcon.setColorFilter(0xFFFFD700); // Gold
                break;
            case 2:
                trophyIcon.setColorFilter(0xFFC0C0C0); // Silver
                break;
            case 3:
                trophyIcon.setColorFilter(0xFFCD7F32); // Bronze
                break;
        }

        leaderboardContainer.addView(itemView);
    }

    private void addEmptyLeaderboardMessage() {
        TextView emptyMessage = new TextView(getContext());
        emptyMessage.setText("No players yet. Be the first!");
        emptyMessage.setTextColor(getResources().getColor(R.color.flip_mid));
        emptyMessage.setTextSize(14);
        emptyMessage.setPadding(16, 16, 16, 16);
        emptyMessage.setGravity(android.view.Gravity.CENTER);

        leaderboardContainer.addView(emptyMessage);
    }

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