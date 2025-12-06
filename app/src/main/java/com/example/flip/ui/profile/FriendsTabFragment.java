package com.example.flip.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.flip.ChatActivity;
import com.example.flip.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class FriendsTabFragment extends Fragment {

    private LinearLayout friendsContainer;
    private ProgressBar loadingSpinner;
    private TextView emptyStateText;

    private FirebaseAuth auth;
    private FirebaseDatabase database;
    private String currentUserId;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_friends_tab, container, false);

        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();
        currentUserId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;

        friendsContainer = root.findViewById(R.id.friendsContainer);
        loadingSpinner = root.findViewById(R.id.loadingSpinner);
        emptyStateText = root.findViewById(R.id.emptyStateText);

        loadFriends();

        return root;
    }

    private void loadFriends() {
        if (currentUserId == null) return;

        loadingSpinner.setVisibility(View.VISIBLE);
        friendsContainer.removeAllViews();

        DatabaseReference friendshipsRef = database.getReference("friendships").child(currentUserId);
        friendshipsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<String> friendIds = new ArrayList<>();

                for (DataSnapshot friendSnapshot : snapshot.getChildren()) {
                    friendIds.add(friendSnapshot.getKey());
                }

                if (friendIds.isEmpty()) {
                    showEmptyState();
                } else {
                    displayFriends(friendIds);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                loadingSpinner.setVisibility(View.GONE);
                showEmptyState();
            }
        });
    }

    private void displayFriends(List<String> friendIds) {
        loadingSpinner.setVisibility(View.GONE);
        emptyStateText.setVisibility(View.GONE);
        friendsContainer.setVisibility(View.VISIBLE);
        friendsContainer.removeAllViews();

        for (String friendId : friendIds) {
            loadFriendAndAddItem(friendId);
        }
    }

    private void loadFriendAndAddItem(String friendId) {
        DatabaseReference userRef = database.getReference("users").child(friendId);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // Try both field names for username
                    String username = snapshot.child("Username").getValue(String.class);
                    if (username == null || username.isEmpty()) {
                        username = snapshot.child("username").getValue(String.class);
                    }
                    if (username == null || username.isEmpty()) {
                        // Fallback to email
                        String email = snapshot.child("email").getValue(String.class);
                        username = email != null ? email.split("@")[0] : "User";
                    }

                    Integer points = snapshot.child("points").getValue(Integer.class);
                    addFriendItem(friendId, username, points != null ? points : 0);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void addFriendItem(String friendId, String friendUsername, int points) {
        View itemView = LayoutInflater.from(getContext())
                .inflate(R.layout.item_friend, friendsContainer, false);

        ImageView messageButton = itemView.findViewById(R.id.messageButton);
        TextView usernameTV = itemView.findViewById(R.id.friendUsername);
        TextView pointsTV = itemView.findViewById(R.id.friendPoints);

        usernameTV.setText("@" + friendUsername);
        pointsTV.setText(points + " pts");

        messageButton.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), ChatActivity.class);
            intent.putExtra("FRIEND_ID", friendId);
            intent.putExtra("FRIEND_USERNAME", friendUsername);
            startActivity(intent);
        });

        friendsContainer.addView(itemView);
    }

    private void showEmptyState() {
        loadingSpinner.setVisibility(View.GONE);
        friendsContainer.setVisibility(View.GONE);
        emptyStateText.setVisibility(View.VISIBLE);
    }
}
