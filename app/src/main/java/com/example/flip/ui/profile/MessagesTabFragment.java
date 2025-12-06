package com.example.flip.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MessagesTabFragment extends Fragment {

    private LinearLayout messagesContainer;
    private ProgressBar loadingSpinner;
    private TextView emptyStateText;

    private FirebaseAuth auth;
    private FirebaseDatabase database;
    private String currentUserId;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_messages_tab, container, false);

        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();
        currentUserId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;

        messagesContainer = root.findViewById(R.id.messagesContainer);
        loadingSpinner = root.findViewById(R.id.loadingSpinner);
        emptyStateText = root.findViewById(R.id.emptyStateText);

        loadConversations();

        return root;
    }

    private void loadConversations() {
        if (currentUserId == null) return;

        loadingSpinner.setVisibility(View.VISIBLE);
        messagesContainer.removeAllViews();

        DatabaseReference messagesRef = database.getReference("messages");
        messagesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Set<String> friendIds = new HashSet<>();

                for (DataSnapshot chatSnapshot : snapshot.getChildren()) {
                    String chatId = chatSnapshot.getKey();
                    if (chatId != null && chatId.contains(currentUserId)) {
                        // Extract friend ID from chatId
                        String[] ids = chatId.split("_");
                        String friendId = ids[0].equals(currentUserId) ? ids[1] : ids[0];
                        friendIds.add(friendId);
                    }
                }

                if (friendIds.isEmpty()) {
                    showEmptyState();
                } else {
                    displayConversations(new ArrayList<>(friendIds));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                loadingSpinner.setVisibility(View.GONE);
                showEmptyState();
            }
        });
    }

    private void displayConversations(List<String> friendIds) {
        loadingSpinner.setVisibility(View.GONE);
        emptyStateText.setVisibility(View.GONE);
        messagesContainer.setVisibility(View.VISIBLE);
        messagesContainer.removeAllViews();

        for (String friendId : friendIds) {
            loadFriendAndAddConversation(friendId);
        }
    }

    private void loadFriendAndAddConversation(String friendId) {
        DatabaseReference userRef = database.getReference("users").child(friendId);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // Try both field names
                    String username = snapshot.child("Username").getValue(String.class);
                    if (username == null || username.isEmpty()) {
                        username = snapshot.child("username").getValue(String.class);
                    }
                    if (username == null || username.isEmpty()) {
                        String email = snapshot.child("email").getValue(String.class);
                        username = email != null ? email.split("@")[0] : "User";
                    }

                    addConversationItem(friendId, username);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Ignore
            }
        });
    }

    private void addConversationItem(String friendId, String friendUsername) {
        View itemView = LayoutInflater.from(getContext())
                .inflate(R.layout.item_message_preview, messagesContainer, false);

        TextView usernameTV = itemView.findViewById(R.id.friendUsername);
        TextView lastMessageTV = itemView.findViewById(R.id.lastMessage);

        usernameTV.setText("@" + friendUsername);
        lastMessageTV.setText("Tap to open chat");

        itemView.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), ChatActivity.class);
            intent.putExtra("FRIEND_ID", friendId);
            intent.putExtra("FRIEND_USERNAME", friendUsername);
            startActivity(intent);
        });

        messagesContainer.addView(itemView);
    }

    private void showEmptyState() {
        loadingSpinner.setVisibility(View.GONE);
        messagesContainer.setVisibility(View.GONE);
        emptyStateText.setVisibility(View.VISIBLE);
    }
}
