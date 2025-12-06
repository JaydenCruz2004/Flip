package com.example.flip;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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

public class ChatActivity extends AppCompatActivity {

    private RecyclerView messagesRecyclerView;
    private EditText messageInput;
    private ImageButton sendButton;
    private TextView friendUsernameText;

    private ChatAdapter chatAdapter;
    private List<ChatMessage> messages = new ArrayList<>();

    private FirebaseAuth auth;
    private FirebaseDatabase database;
    private String currentUserId;
    private String friendId;
    private String chatId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();
        currentUserId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;

        friendId = getIntent().getStringExtra("FRIEND_ID");
        String friendUsername = getIntent().getStringExtra("FRIEND_USERNAME");

        if (currentUserId == null || friendId == null) {
            Toast.makeText(this, "Error loading chat", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        chatId = currentUserId.compareTo(friendId) < 0
                ? currentUserId + "_" + friendId
                : friendId + "_" + currentUserId;

        friendUsernameText = findViewById(R.id.friendUsernameText);
        messagesRecyclerView = findViewById(R.id.messagesRecyclerView);
        messageInput = findViewById(R.id.messageInput);
        sendButton = findViewById(R.id.sendButton);

        friendUsernameText.setText("@" + friendUsername);

        // Setup RecyclerView
        chatAdapter = new ChatAdapter(messages, currentUserId);
        messagesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        messagesRecyclerView.setAdapter(chatAdapter);

        // Load messages
        loadMessages();

        // Send button
        sendButton.setOnClickListener(v -> sendMessage());

        // Back button
        findViewById(R.id.backButton).setOnClickListener(v -> finish());
    }

    private void loadMessages() {
        DatabaseReference messagesRef = database.getReference("messages").child(chatId);
        messagesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                messages.clear();

                for (DataSnapshot messageSnapshot : snapshot.getChildren()) {
                    ChatMessage message = messageSnapshot.getValue(ChatMessage.class);
                    if (message != null) {
                        messages.add(message);
                    }
                }

                chatAdapter.notifyDataSetChanged();
                if (!messages.isEmpty()) {
                    messagesRecyclerView.scrollToPosition(messages.size() - 1);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ChatActivity.this, "Failed to load messages", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendMessage() {
        String messageText = messageInput.getText().toString().trim();
        if (messageText.isEmpty()) return;

        DatabaseReference messagesRef = database.getReference("messages").child(chatId);
        String messageId = messagesRef.push().getKey();

        if (messageId == null) return;

        Map<String, Object> messageData = new HashMap<>();
        messageData.put("senderId", currentUserId);
        messageData.put("receiverId", friendId);
        messageData.put("message", messageText);
        messageData.put("timestamp", System.currentTimeMillis());
        messageData.put("read", false);

        messagesRef.child(messageId).setValue(messageData)
                .addOnSuccessListener(aVoid -> {
                    messageInput.setText("");
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ChatActivity.this, "Failed to send message", Toast.LENGTH_SHORT).show();
                });
    }

    public static class ChatMessage {
        public String senderId;
        public String receiverId;
        public String message;
        public long timestamp;
        public boolean read;

        public ChatMessage() {}

        public ChatMessage(String senderId, String receiverId, String message, long timestamp, boolean read) {
            this.senderId = senderId;
            this.receiverId = receiverId;
            this.message = message;
            this.timestamp = timestamp;
            this.read = read;
        }
    }


}
