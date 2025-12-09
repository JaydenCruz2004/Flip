package com.example.flip.ui.home;


import java.util.Calendar;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
        populateRecentActivity();
        populateSchedule();

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh data when fragment becomes visible
        loadProfileData();
        populateLeaderboard();
        populateRecentActivity();
        populateSchedule();
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
        usernameText.setText(R.string.user);
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
                usernameText.setText(R.string.user);
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

    private void populateRecentActivity() {
        activityContainer.removeAllViews();

        String uid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (uid == null) {
            addEmptyActivityMessage();
            return;
        }
        database.getReference("friendRequests").child(uid)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        activityContainer.removeAllViews();
                        int activityCount = 0;

                        // Add friend requests
                        for (DataSnapshot requestSnapshot : snapshot.getChildren()) {
                            String senderId = requestSnapshot.getKey();
                            String senderUsername = requestSnapshot.child("senderUsername").getValue(String.class);
                            String status = requestSnapshot.child("status").getValue(String.class);
                            Long timestamp = requestSnapshot.child("timestamp").getValue(Long.class);

                            if ("pending".equals(status)) {
                                addFriendRequestActivity(senderUsername, timestamp != null ? timestamp : System.currentTimeMillis());
                                activityCount++;
                            }
                        }

                        // Load user activities
                        loadUserActivities(activityCount);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        addEmptyActivityMessage();
                    }
                });
    }

    private void loadUserActivities(int existingCount) {
        String uid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (uid == null) return;

        database.getReference("users").child(uid).child("recentActivities")
                .limitToLast(5)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot activitySnapshot : snapshot.getChildren()) {
                            String activityText = activitySnapshot.child("text").getValue(String.class);
                            Long timestamp = activitySnapshot.child("timestamp").getValue(Long.class);

                            if (activityText != null) {
                                addGeneralActivity(activityText, timestamp != null ? timestamp : System.currentTimeMillis());
                            }
                        }

                        if (existingCount == 0 && !snapshot.exists()) {
                            addEmptyActivityMessage();
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        // Ignore
                    }
                });
    }

    private void addFriendRequestActivity(String username, long timestamp) {
                        View itemView = LayoutInflater.from(getContext())
                                .inflate(R.layout.item_activity_item, activityContainer, false);

                        TextView activityIcon = itemView.findViewById(R.id.activityIcon);
                        TextView activityText = itemView.findViewById(R.id.activityText);
                        TextView activityTime = itemView.findViewById(R.id.activityTime);

                        activityIcon.setText("👥");
                        activityText.setText("@" + username + " wants to be friends");
                        activityTime.setText(getTimeAgo(timestamp));

                        activityContainer.addView(itemView);
                    }
                    private void addGeneralActivity(String text, long timestamp) {
                        View itemView = LayoutInflater.from(getContext())
                                .inflate(R.layout.item_activity_item, activityContainer, false);

                        TextView activityIcon = itemView.findViewById(R.id.activityIcon);
                        TextView activityText = itemView.findViewById(R.id.activityText);
                        TextView activityTime = itemView.findViewById(R.id.activityTime);

                        activityIcon.setText("🎮");
                        activityText.setText(text);
                        activityTime.setText(getTimeAgo(timestamp));

                        activityContainer.addView(itemView);
                    }

                    private void addEmptyActivityMessage() {
                        TextView emptyMessage = new TextView(getContext());
                        emptyMessage.setText("No recent activity");
                        emptyMessage.setTextColor(getResources().getColor(R.color.flip_mid));
                        emptyMessage.setTextSize(14);
                        emptyMessage.setPadding(16, 16, 16, 16);
                        emptyMessage.setGravity(android.view.Gravity.CENTER);

                        activityContainer.addView(emptyMessage);
                    }


    private void populateSchedule() {
        scheduleContainer.removeAllViews();

        String uid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (uid == null) {
            addEmptyScheduleWithButton();
            return;
        }

        // Load study times from Firebase
        database.getReference("users").child(uid).child("studyTimes")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        scheduleContainer.removeAllViews();

                        if (!snapshot.exists()) {
                            addEmptyScheduleWithButton();
                            return;
                        }

                        for (DataSnapshot timeSnapshot : snapshot.getChildren()) {
                            String title = timeSnapshot.child("title").getValue(String.class);
                            Long startTime = timeSnapshot.child("startTime").getValue(Long.class);
                            Long endTime = timeSnapshot.child("endTime").getValue(Long.class);

                            if (title != null && startTime != null && endTime != null) {
                                addScheduleItem(title, startTime, endTime);
                            }
                        }

                        addAddStudyTimeButton();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        addEmptyScheduleWithButton();
                    }
                });
    }



    private void addEmptyScheduleWithButton() {
        // Create a centered container for empty state
        LinearLayout emptyContainer = new LinearLayout(getContext());
        emptyContainer.setOrientation(LinearLayout.VERTICAL);
        emptyContainer.setGravity(android.view.Gravity.CENTER);
        emptyContainer.setPadding(16, 32, 16, 32);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        emptyContainer.setLayoutParams(params);

        // Empty message
        TextView emptyMessage = new TextView(getContext());
        emptyMessage.setText(R.string.no_study_sessions);
        emptyMessage.setTextColor(getResources().getColor(R.color.flip_mid));
        emptyMessage.setTextSize(14);
        emptyMessage.setGravity(android.view.Gravity.CENTER);
        emptyContainer.addView(emptyMessage);

        // Add button centered below message
        View buttonView = LayoutInflater.from(getContext())
                .inflate(R.layout.item_add_study_time_button, null, false);

        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                120,  // width in dp
                120   // height in dp
        );
        buttonParams.topMargin = 16;
        buttonParams.gravity = android.view.Gravity.CENTER;
        buttonView.setLayoutParams(buttonParams);

        buttonView.setOnClickListener(v -> showAddStudyTimeDialog());

        emptyContainer.addView(buttonView);
        scheduleContainer.addView(emptyContainer);
    }

                    private void addScheduleItem(String title, long startTime, long endTime) {
                        View itemView = LayoutInflater.from(getContext())
                                .inflate(R.layout.item_schedule_card, scheduleContainer, false);

                        TextView titleText = itemView.findViewById(R.id.scheduleTitle);
                        TextView timeText = itemView.findViewById(R.id.scheduleTime);
                        TextView dateText = itemView.findViewById(R.id.scheduleDate);

                        SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.US);
                        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, MMM d", Locale.US);

                        titleText.setText(title);
                        timeText.setText(timeFormat.format(startTime) + " - " + timeFormat.format(endTime));
                        dateText.setText(dateFormat.format(startTime));

                        scheduleContainer.addView(itemView);
                    }
    private void addAddStudyTimeButton() {
        View buttonView = LayoutInflater.from(getContext())
                .inflate(R.layout.item_add_study_time_button, scheduleContainer, false);

        buttonView.setOnClickListener(v -> showAddStudyTimeDialog());

        scheduleContainer.addView(buttonView);
    }
    private void showAddStudyTimeDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_study_time, null);

        android.widget.EditText titleInput = dialogView.findViewById(R.id.studyTitleInput);
        android.widget.Button selectDateButton = dialogView.findViewById(R.id.selectDateButton);
        android.widget.Button selectStartTimeButton = dialogView.findViewById(R.id.selectStartTimeButton);
        android.widget.Button selectEndTimeButton = dialogView.findViewById(R.id.selectEndTimeButton);

        final Calendar selectedCalendar = Calendar.getInstance();
        final long[] startTimeMillis = {0};
        final long[] endTimeMillis = {0};

        selectDateButton.setOnClickListener(v -> {
            DatePickerDialog datePicker = new DatePickerDialog(
                    requireContext(),
                    (view, year, month, dayOfMonth) -> {
                        selectedCalendar.set(Calendar.YEAR, year);
                        selectedCalendar.set(Calendar.MONTH, month);
                        selectedCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy", Locale.US);
                        selectDateButton.setText(dateFormat.format(selectedCalendar.getTime()));
                    },
                    selectedCalendar.get(Calendar.YEAR),
                    selectedCalendar.get(Calendar.MONTH),
                    selectedCalendar.get(Calendar.DAY_OF_MONTH)
            );
            datePicker.show();
        });

        selectStartTimeButton.setOnClickListener(v -> {
            TimePickerDialog timePicker = new TimePickerDialog(
                    requireContext(),
                    (view, hourOfDay, minute) -> {
                        Calendar startCal = (Calendar) selectedCalendar.clone();
                        startCal.set(Calendar.HOUR_OF_DAY, hourOfDay);
                        startCal.set(Calendar.MINUTE, minute);
                        startTimeMillis[0] = startCal.getTimeInMillis();

                        SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.US);
                        selectStartTimeButton.setText(timeFormat.format(startCal.getTime()));
                    },
                    selectedCalendar.get(Calendar.HOUR_OF_DAY),
                    selectedCalendar.get(Calendar.MINUTE),
                    false
            );
            timePicker.show();
        });

        selectEndTimeButton.setOnClickListener(v -> {
            TimePickerDialog timePicker = new TimePickerDialog(
                    requireContext(),
                    (view, hourOfDay, minute) -> {
                        Calendar endCal = (Calendar) selectedCalendar.clone();
                        endCal.set(Calendar.HOUR_OF_DAY, hourOfDay);
                        endCal.set(Calendar.MINUTE, minute);
                        endTimeMillis[0] = endCal.getTimeInMillis();

                        SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.US);
                        selectEndTimeButton.setText(timeFormat.format(endCal.getTime()));
                    },
                    selectedCalendar.get(Calendar.HOUR_OF_DAY),
                    selectedCalendar.get(Calendar.MINUTE),
                    false
            );
            timePicker.show();
        });

        builder.setView(dialogView)
                .setTitle("Add Study Time")
                .setPositiveButton("Add", (dialog, which) -> {
                    String title = titleInput.getText().toString().trim();

                    if (title.isEmpty()) {
                        Toast.makeText(getContext(), "Please enter a title", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (startTimeMillis[0] == 0 || endTimeMillis[0] == 0) {
                        Toast.makeText(getContext(), "Please select start and end times", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    saveStudyTime(title, startTimeMillis[0], endTimeMillis[0]);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }


    private void saveStudyTime(String title, long startTime, long endTime) {
        String uid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (uid == null) return;

        DatabaseReference studyTimesRef = database.getReference("users").child(uid).child("studyTimes");
        String studyTimeId = studyTimesRef.push().getKey();

        if (studyTimeId == null) return;

        Map<String, Object> studyTimeData = new HashMap<>();
        studyTimeData.put("title", title);
        studyTimeData.put("startTime", startTime);
        studyTimeData.put("endTime", endTime);
        studyTimeData.put("createdAt", System.currentTimeMillis());

        studyTimesRef.child(studyTimeId).setValue(studyTimeData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Study time added!", Toast.LENGTH_SHORT).show();
                    populateSchedule();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to add study time", Toast.LENGTH_SHORT).show();
                });
    }

    private String getTimeAgo(long timestamp) {
        long now = System.currentTimeMillis();
        long diff = now - timestamp;

        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
            return days + "d ago";
        } else if (hours > 0) {
            return hours + "h ago";
        } else if (minutes > 0) {
            return minutes + "m ago";
        } else {
            return "Just now";
        }
    }
    private void addEmptyScheduleMessage() {
        TextView emptyMessage = new TextView(getContext());
        emptyMessage.setText(R.string.no_study_sessions);
        emptyMessage.setTextColor(getResources().getColor(R.color.flip_mid));
        emptyMessage.setTextSize(14);
        emptyMessage.setPadding(16, 16, 16, 16);
        emptyMessage.setGravity(android.view.Gravity.CENTER);

        scheduleContainer.addView(emptyMessage);
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