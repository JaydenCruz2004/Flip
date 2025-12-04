package com.example.flip;

import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.flip.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;


public class Leaderboard extends Fragment {

    private RecyclerView leaderboardRecyclerView;
    private LeaderboardAdapter adapter;
    private ProgressBar loadingSpinner;
    private TextView emptyStateText;

    private ImageView refreshButton;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView lastUpdatedText;

    // Current user info display
    private TextView currentUserRank;
    private TextView currentUsername;
    private TextView currentUserGames;
    private TextView currentUserPoints;

    // Firebase
    private DatabaseReference usersRef;
    private FirebaseAuth auth;
    private String currentUserId;
    private Handler autoRefreshHandler;
    private Runnable autoRefreshRunnable;
    private static final long AUTO_REFRESH_INTERVAL = 30000;


    public Leaderboard() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_leaderboard, container, false);

        // Initialize Firebase
        auth = FirebaseAuth.getInstance();
        usersRef = FirebaseDatabase.getInstance().getReference("users");
        currentUserId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;

        // Initialize views
        leaderboardRecyclerView = root.findViewById(R.id.leaderboardRecyclerView);
        loadingSpinner = root.findViewById(R.id.loadingSpinner);
        emptyStateText = root.findViewById(R.id.emptyStateText);
        refreshButton = root.findViewById(R.id.refreshButton);
        swipeRefreshLayout = root.findViewById(R.id.swipeRefreshLayout);
        lastUpdatedText = root.findViewById(R.id.lastUpdatedText);

        currentUserRank = root.findViewById(R.id.currentUserRank);
        currentUsername = root.findViewById(R.id.currentUsername);
        currentUserGames = root.findViewById(R.id.currentUserGames);
        currentUserPoints = root.findViewById(R.id.currentUserPoints);

        // Setup RecyclerView
        leaderboardRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new LeaderboardAdapter(currentUserId);
        leaderboardRecyclerView.setAdapter(adapter);

        swipeRefreshLayout.setColorSchemeColors(
                getResources().getColor(R.color.flip_accent),
                getResources().getColor(R.color.flip_primary),
                getResources().getColor(R.color.flip_mid)
        );
        swipeRefreshLayout.setOnRefreshListener(() -> {
            loadLeaderboard();
        });


        refreshButton.setOnClickListener(v -> {
            loadLeaderboard();
            v.animate().rotation(v.getRotation() + 360f).setDuration(500).start();
        });

        // Load data
        loadLeaderboard();
        setupAutoRefresh();

        return root;
    }

    private void setupAutoRefresh() {
        autoRefreshHandler = new Handler();
        autoRefreshRunnable = new Runnable() {
            @Override
            public void run() {
                loadLeaderboard();
                autoRefreshHandler.postDelayed(this, AUTO_REFRESH_INTERVAL);
            }
        };
    }

    private void startAutoRefresh() {
        if (autoRefreshHandler != null && autoRefreshRunnable != null) {
            autoRefreshHandler.postDelayed(autoRefreshRunnable, AUTO_REFRESH_INTERVAL);
        }
    }

    private void stopAutoRefresh() {
        if (autoRefreshHandler != null && autoRefreshRunnable != null) {
            autoRefreshHandler.removeCallbacks(autoRefreshRunnable);
        }
    }

    private void loadLeaderboard() {
        showLoading(true);

        // Query all users from Firebase
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<User> allUsers = new ArrayList<>();

                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    User user = userSnapshot.getValue(User.class);
                    if (user != null) {
                        user.setUid(userSnapshot.getKey());

                        // Only include users with points > 0 or games played > 0
                        if (user.getPoints() > 0 || user.getGamesPlayed() > 0) {
                            allUsers.add(user);
                        }
                    }
                }


                Collections.sort(allUsers, new Comparator<User>() {
                    @Override
                    public int compare(User u1, User u2) {
                        return Integer.compare(u2.getPoints(), u1.getPoints());
                    }
                });

                // Calculate rankings
                for (int i = 0; i < allUsers.size(); i++) {
                    allUsers.get(i).setRanking(i + 1);
                }

                // Update UI
                if (allUsers.isEmpty()) {
                    showEmptyState();
                } else {
                    showLeaderboard(allUsers);
                }

                // Update current user display
                updateCurrentUserDisplay(allUsers);

                updateLastUpdatedTime();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showLoading(false);
                if (swipeRefreshLayout != null) {
                    swipeRefreshLayout.setRefreshing(false);
                }

                Toast.makeText(getContext(),
                        "Error loading leaderboard: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showLeaderboard(List<User> users) {
        showLoading(false);
        leaderboardRecyclerView.setVisibility(View.VISIBLE);
        emptyStateText.setVisibility(View.GONE);
        adapter.setUserList(users);
    }

    private void showEmptyState() {
        showLoading(false);
        leaderboardRecyclerView.setVisibility(View.GONE);
        emptyStateText.setVisibility(View.VISIBLE);
    }

    private void showLoading(boolean loading) {
        if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
            loadingSpinner.setVisibility(View.GONE);
            return;
        }

        loadingSpinner.setVisibility(loading ? View.VISIBLE : View.GONE);
        if (loading) {
            leaderboardRecyclerView.setVisibility(View.GONE);
            emptyStateText.setVisibility(View.GONE);
        }
    }

    private void updateCurrentUserDisplay(List<User> allUsers) {
        if (currentUserId == null) {
            currentUserRank.setText("—");
            currentUsername.setText("Guest");
            currentUserGames.setText("0 games played");
            currentUserPoints.setText("0");
            return;
        }

        User currentUser = null;
        int rank = -1;

        for (int i = 0; i < allUsers.size(); i++) {
            if (allUsers.get(i).getUid() != null && allUsers.get(i).getUid().equals(currentUserId)) {
                currentUser = allUsers.get(i);
                rank = i + 1;
                break;
            }
        }

        if (currentUser != null) {
            currentUserRank.setText(getOrdinal(rank));

            String username = currentUser.getUsername();
            if (username == null || username.isEmpty()) {
                username = currentUser.getEmail() != null ? currentUser.getEmail().split("@")[0] : "User";
            }
            currentUsername.setText("@" + username);

            currentUserGames.setText(currentUser.getGamesPlayed() + " games played");

            NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.US);
            currentUserPoints.setText(numberFormat.format(currentUser.getPoints()));
        } else {
            currentUserRank.setText("—");


            usersRef.child(currentUserId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    User user = snapshot.getValue(User.class);
                    if (user != null) {
                        String username = user.getUsername();
                        if (username == null || username.isEmpty()) {
                            username = user.getEmail() != null ? user.getEmail().split("@")[0] : "User";
                        }
                        currentUsername.setText("@" + username);
                    } else {
                        currentUsername.setText("@user");
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    currentUsername.setText("@user");
                }
            });

            currentUserGames.setText("0 games played");
            currentUserPoints.setText("0");
        }
    }

    private void updateLastUpdatedTime() {
        if (lastUpdatedText != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.US);
            String currentTime = sdf.format(new Date());
            lastUpdatedText.setText("Updated " + currentTime);
            lastUpdatedText.setVisibility(View.VISIBLE);
        }
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

    @Override
    public void onResume() {
        super.onResume();
        loadLeaderboard();
        startAutoRefresh();
    }

    @Override
    public void onPause() {
        super.onPause();
        stopAutoRefresh();
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopAutoRefresh();  // Clean up
    }

}




