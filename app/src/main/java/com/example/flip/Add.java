package com.example.flip;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.flip.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Add extends Fragment {

    private EditText searchInput;
    private LinearLayout searchResultsContainer;
    private LinearLayout friendRequestsContainer;
    private ProgressBar searchProgressBar;
    private TextView noResultsText;
    private TextView requestsCountBadge;

    private FirebaseAuth auth;
    private FirebaseDatabase database;
    private String currentUserId;
    private String currentUsername;

    public Add() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_add, container, false);

        // Initialize Firebase
        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();
        currentUserId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;

        // Initialize views
        searchInput = root.findViewById(R.id.searchInput);
        searchResultsContainer = root.findViewById(R.id.searchResultsContainer);
        friendRequestsContainer = root.findViewById(R.id.friendRequestsContainer);
        searchProgressBar = root.findViewById(R.id.searchProgressBar);
        noResultsText = root.findViewById(R.id.noResultsText);
        requestsCountBadge = root.findViewById(R.id.requestsCountBadge);

        // Load current username
        loadCurrentUsername();

        // Setup search functionality
        setupSearch();

        // Load friend requests
        loadFriendRequests();

        return root;
    }

    private void loadCurrentUsername() {
        if (currentUserId != null) {
            database.getReference("users").child(currentUserId)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            User user = snapshot.getValue(User.class);
                            if (user != null) {
                                currentUsername = user.getUsername();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            // Ignore
                        }
                    });
        }
    }

    private void setupSearch() {
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();
                if (query.length() >= 2) {
                    searchUsers(query);
                } else {
                    searchResultsContainer.removeAllViews();
                    noResultsText.setVisibility(View.GONE);
                }
            }
            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void searchUsers(String query) {
        searchProgressBar.setVisibility(View.VISIBLE);
        noResultsText.setVisibility(View.GONE);
        searchResultsContainer.removeAllViews();

        DatabaseReference usersRef = database.getReference("users");
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<User> results = new ArrayList<>();
                String lowerQuery = query.toLowerCase();

                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    User user = userSnapshot.getValue(User.class);
                    if (user != null) {
                        user.setUid(userSnapshot.getKey());
                        String username = user.getUsername();

                        // Don't show current user
                        if (user.getUid().equals(currentUserId)) {
                            continue;
                        }

                        // Search by username
                        if (username != null && username.toLowerCase().contains(lowerQuery)) {
                            results.add(user);
                        }
                    }
                }

                searchProgressBar.setVisibility(View.GONE);

                if (results.isEmpty()) {
                    noResultsText.setVisibility(View.VISIBLE);
                } else {
                    displaySearchResults(results);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                searchProgressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Search error: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void displaySearchResults(List<User> results) {
        searchResultsContainer.removeAllViews();

        for (User user : results) {
            View itemView = LayoutInflater.from(getContext())
                    .inflate(R.layout.item_user_search_result, searchResultsContainer, false);

            TextView usernameText = itemView.findViewById(R.id.usernameText);
            TextView statusText = itemView.findViewById(R.id.statusText);
            View addButton = itemView.findViewById(R.id.addFriendButton);
            View pendingButton = itemView.findViewById(R.id.pendingButton);

            usernameText.setText("@" + user.getUsername());

            // Check friendship status
            checkFriendshipStatus(user.getUid(), status -> {
                switch (status) {
                    case "friends":
                        addButton.setVisibility(View.GONE);
                        pendingButton.setVisibility(View.GONE);
                        statusText.setText("✓ Friends");
                        statusText.setVisibility(View.VISIBLE);
                        break;
                    case "pending_sent":
                        addButton.setVisibility(View.GONE);
                        pendingButton.setVisibility(View.VISIBLE);
                        break;
                    case "pending_received":
                        addButton.setVisibility(View.GONE);
                        pendingButton.setVisibility(View.GONE);
                        statusText.setText("Wants to be friends");
                        statusText.setVisibility(View.VISIBLE);
                        break;
                    default:
                        addButton.setVisibility(View.VISIBLE);
                        pendingButton.setVisibility(View.GONE);
                        statusText.setVisibility(View.GONE);
                        break;
                }
            });

            addButton.setOnClickListener(v -> sendFriendRequest(user));
            itemView.setOnClickListener(v -> {
                // Could navigate to user profile here
            });

            searchResultsContainer.addView(itemView);
        }
    }

    private void checkFriendshipStatus(String userId, FriendshipStatusCallback callback) {
        // Check if already friends
        database.getReference("friendships").child(currentUserId).child(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            callback.onResult("friends");
                            return;
                        }

                        // Check if request sent
                        database.getReference("friendRequests").child(userId).child(currentUserId)
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        if (snapshot.exists()) {
                                            callback.onResult("pending_sent");
                                            return;
                                        }

                                        // Check if request received
                                        database.getReference("friendRequests").child(currentUserId).child(userId)
                                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                                    @Override
                                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                        if (snapshot.exists()) {
                                                            callback.onResult("pending_received");
                                                        } else {
                                                            callback.onResult("none");
                                                        }
                                                    }

                                                    @Override
                                                    public void onCancelled(@NonNull DatabaseError error) {
                                                        callback.onResult("none");
                                                    }
                                                });
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                        callback.onResult("none");
                                    }
                                });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.onResult("none");
                    }
                });
    }

    private void sendFriendRequest(User recipient) {
        if (currentUserId == null || currentUsername == null) {
            Toast.makeText(getContext(), "Error: Not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> requestData = new HashMap<>();
        requestData.put("senderUsername", currentUsername);
        requestData.put("timestamp", System.currentTimeMillis());
        requestData.put("status", "pending");

        database.getReference("friendRequests")
                .child(recipient.getUid())
                .child(currentUserId)
                .setValue(requestData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(),
                            "Friend request sent to @" + recipient.getUsername(),
                            Toast.LENGTH_SHORT).show();
                    // Refresh search results
                    searchUsers(searchInput.getText().toString());
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to send request", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadFriendRequests() {
        if (currentUserId == null) return;

        database.getReference("friendRequests").child(currentUserId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        friendRequestsContainer.removeAllViews();
                        int requestCount = 0;

                        for (DataSnapshot requestSnapshot : snapshot.getChildren()) {
                            String senderId = requestSnapshot.getKey();
                            String senderUsername = requestSnapshot.child("senderUsername").getValue(String.class);
                            String status = requestSnapshot.child("status").getValue(String.class);

                            if ("pending".equals(status)) {
                                requestCount++;
                                addFriendRequestItem(senderId, senderUsername);
                            }
                        }

                        // Update badge
                        if (requestCount > 0) {
                            requestsCountBadge.setText(String.valueOf(requestCount));
                            requestsCountBadge.setVisibility(View.VISIBLE);
                        } else {
                            requestsCountBadge.setVisibility(View.GONE);
                        }

                        // Show empty state if no requests
                        if (friendRequestsContainer.getChildCount() == 0) {
                            TextView emptyText = new TextView(getContext());
                            emptyText.setText("No friend requests");
                            emptyText.setTextColor(getResources().getColor(R.color.flip_mid));
                            emptyText.setPadding(16, 16, 16, 16);
                            friendRequestsContainer.addView(emptyText);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(getContext(), "Failed to load requests", Toast.LENGTH_SHORT).show();
                    }
                });
    }
    private void addFriendRequestItem(String senderId, String senderUsername) {
        View itemView = LayoutInflater.from(getContext())
                .inflate(R.layout.item_friend_request, friendRequestsContainer, false);

        TextView usernameText = itemView.findViewById(R.id.requestUsername);
        View acceptButton = itemView.findViewById(R.id.acceptButton);
        View declineButton = itemView.findViewById(R.id.declineButton);

        usernameText.setText("@" + senderUsername);

        acceptButton.setOnClickListener(v -> acceptFriendRequest(senderId, senderUsername));
        declineButton.setOnClickListener(v -> declineFriendRequest(senderId));

        friendRequestsContainer.addView(itemView);
    }

    private void acceptFriendRequest(String senderId, String senderUsername) {
        // Create friendship both ways
        Map<String, Object> updates = new HashMap<>();
        updates.put("friendships/" + currentUserId + "/" + senderId, true);
        updates.put("friendships/" + senderId + "/" + currentUserId, true);
        updates.put("friendRequests/" + currentUserId + "/" + senderId + "/status", "accepted");

        database.getReference().updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(),
                            "You are now friends with @" + senderUsername,
                            Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to accept request", Toast.LENGTH_SHORT).show();
                });
    }

    private void declineFriendRequest(String senderId) {
        database.getReference("friendRequests")
                .child(currentUserId)
                .child(senderId)
                .removeValue()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Request declined", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to decline request", Toast.LENGTH_SHORT).show();
                });
    }

    interface FriendshipStatusCallback {
        void onResult(String status);
    }



}