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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
                Map<String, ChatInfo> chatsMap = new HashMap<>();

                for (DataSnapshot chatSnapshot : snapshot.getChildren()) {
                    String chatId = chatSnapshot.getKey();
                    if (chatId != null && chatId.contains(currentUserId)) {
                        // Extract friend ID from chatId
                        String[] ids = chatId.split("_");
                        String friendId = ids[0].equals(currentUserId) ? ids[1] : ids[0];

                        DataSnapshot lastMessageSnapshot = null;
                        long latestTimestamp = 0;

                        for (DataSnapshot messageSnapshot : chatSnapshot.getChildren()) {
                            Long timestamp = messageSnapshot.child("timestamp").getValue(Long.class);
                            if (timestamp != null && timestamp > latestTimestamp) {
                                latestTimestamp = timestamp;
                                lastMessageSnapshot = messageSnapshot;
                            }
                        }

                        if (lastMessageSnapshot != null) {
                            String message = lastMessageSnapshot.child("message").getValue(String.class);
                            String senderId = lastMessageSnapshot.child("senderId").getValue(String.class);

                            ChatInfo chatInfo = new ChatInfo();
                            chatInfo.friendId = friendId;
                            chatInfo.lastMessage = message != null ? message : "";
                            chatInfo.timestamp = latestTimestamp;
                            chatInfo.sentByMe = currentUserId.equals(senderId);

                            chatsMap.put(friendId, chatInfo);
                        }
                    }
                }

                if (chatsMap.isEmpty()) {
                    showEmptyState();
                } else {
                    displayConversations(new ArrayList<>(chatsMap.values()));
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                loadingSpinner.setVisibility(View.GONE);
                showEmptyState();
            }
        });
    }

    private void displayConversations(List<ChatInfo> chats) {
        loadingSpinner.setVisibility(View.GONE);
        emptyStateText.setVisibility(View.GONE);
        messagesContainer.setVisibility(View.VISIBLE);
        messagesContainer.removeAllViews();

        for (ChatInfo chat : chats) {
            loadFriendAndAddConversation(chat);
        }
    }


    private void loadFriendAndAddConversation(ChatInfo chatInfo) {
        DatabaseReference userRef = database.getReference("users").child(chatInfo.friendId);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String username = snapshot.child("Username").getValue(String.class);
                    if (username == null || username.isEmpty()) {
                        username = snapshot.child("username").getValue(String.class);
                    }
                    if (username == null || username.isEmpty()) {
                        String email = snapshot.child("email").getValue(String.class);
                        username = email != null ? email.split("@")[0] : "User";
                    }

                    addConversationItem(chatInfo.friendId, username, chatInfo.lastMessage, chatInfo.sentByMe);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Ignore
            }
        });
    }

    private void addConversationItem(String friendId, String friendUsername, String lastMessage, boolean sentByMe) {
        View itemView = LayoutInflater.from(getContext())
                .inflate(R.layout.item_message_preview, messagesContainer, false);

        TextView usernameTV = itemView.findViewById(R.id.friendUsername);
        TextView lastMessageTV = itemView.findViewById(R.id.lastMessage);

        usernameTV.setText("@" + friendUsername);

        // Show preview with sender info
        String preview;
        if (sentByMe) {
            preview = "You: " + lastMessage;
        } else {
            preview = lastMessage;
        }

        // Limit preview length
        if (preview.length() > 50) {
            preview = preview.substring(0, 47) + "...";
        }

        lastMessageTV.setText(preview);

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

    private static class ChatInfo {
        String friendId;
        String lastMessage;
        long timestamp;
        boolean sentByMe;
    }
}
