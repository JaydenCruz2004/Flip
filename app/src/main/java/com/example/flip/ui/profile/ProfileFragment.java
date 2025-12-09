package com.example.flip.ui.profile;


import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.example.flip.R;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

public class ProfileFragment extends Fragment {
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private ProfilePagerAdapter pagerAdapter;

    private ImageView profileImage;
    private TextView usernameText, pointsText, rankingText, gamesText;

    private FirebaseAuth auth;
    private FirebaseDatabase database;
    private FirebaseStorage storage;
    private String currentUserId;
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_profile_new, container, false);

        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();
        storage = FirebaseStorage.getInstance();

        currentUserId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null) {
                            uploadProfilePicture(imageUri);
                        }
                    }
                });
        // Initialize views
        profileImage = root.findViewById(R.id.profileImage);
        usernameText = root.findViewById(R.id.usernameText);
        pointsText = root.findViewById(R.id.pointsText);
        rankingText = root.findViewById(R.id.rankingText);
        gamesText = root.findViewById(R.id.gamesText);

        tabLayout = root.findViewById(R.id.tabLayout);
        viewPager = root.findViewById(R.id.viewPager);

        // Setup ViewPager with tabs
        pagerAdapter = new ProfilePagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);


        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText("Messages");
                    break;
                case 1:
                    tab.setText("Friends");
                    break;
                case 2:
                    tab.setText("Activities");
                    break;
            }
        }).attach();

        profileImage.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Tap to change profile picture", Toast.LENGTH_SHORT).show();
            openImagePicker();
        });

        loadUserProfile();
        calculateAndUpdateRanking();

        return root;
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    private void uploadProfilePicture(Uri imageUri) {
        if (currentUserId == null) return;

        Toast.makeText(getContext(), "Uploading...", Toast.LENGTH_SHORT).show();

        StorageReference profilePicRef = storage.getReference()
                .child("profile_pictures")
                .child(currentUserId + ".jpg");

        profilePicRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    profilePicRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        String imageUrl = uri.toString();

                        // Save URL to database
                        database.getReference("users")
                                .child(currentUserId)
                                .child("profileImageUrl")
                                .setValue(imageUrl)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(getContext(), "✓ Profile picture updated!", Toast.LENGTH_SHORT).show();
                                    loadProfileImage(imageUrl);
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(getContext(), "Failed to save image URL", Toast.LENGTH_SHORT).show();
                                });
                    });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void loadUserProfile() {
        if (currentUserId == null) {
            Toast.makeText(getContext(), "No user logged in!", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference userRef = database.getReference("users").child(currentUserId);
        userRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String username = snapshot.child("Username").getValue(String.class);
                    if (username == null || username.isEmpty()) {
                        username = snapshot.child("username").getValue(String.class);
                    }

                    Integer points = snapshot.child("points").getValue(Integer.class);
                    Integer ranking = snapshot.child("ranking").getValue(Integer.class);
                    Integer games = snapshot.child("gamesPlayed").getValue(Integer.class);
                    String profileImageUrl = snapshot.child("profileImageUrl").getValue(String.class);


                    usernameText.setText("@" + (username != null ? username : "User"));
                    pointsText.setText(String.valueOf(points != null ? points : 0));
                    rankingText.setText(ranking != null && ranking > 0 ? getOrdinal(ranking) : "N/A");
                    gamesText.setText(String.valueOf(games != null ? games : 0));
                    if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                        loadProfileImage(profileImageUrl);
                    } else {
                        // Show default icon
                        profileImage.setImageResource(R.drawable.ic_user);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                android.util.Log.e("ProfileDebug", "Error: " + error.getMessage());
                Toast.makeText(getContext(), "Failed to load profile", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadProfileImage(String imageUrl) {
        if (getContext() != null && imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(this)
                    .load(imageUrl)
                    .apply(new RequestOptions()
                            .circleCrop()
                            .placeholder(R.drawable.ic_user)
                            .error(R.drawable.ic_user)
                            .diskCacheStrategy(DiskCacheStrategy.ALL))
                    .into(profileImage);
        }
    }


    private void calculateAndUpdateRanking() {
        if (currentUserId == null) return;

        DatabaseReference usersRef = database.getReference("users");
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<UserRanking> allUsers = new ArrayList<>();

                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    String uid = userSnapshot.getKey();
                    Integer points = userSnapshot.child("points").getValue(Integer.class);
                    Integer gamesPlayed = userSnapshot.child("gamesPlayed").getValue(Integer.class);

                    if (uid != null && points != null && (points > 0 || (gamesPlayed != null && gamesPlayed > 0))) {
                        UserRanking user = new UserRanking();
                        user.uid = uid;
                        user.points = points;
                        allUsers.add(user);
                    }
                }

                Collections.sort(allUsers, new Comparator<UserRanking>() {
                    @Override
                    public int compare(UserRanking u1, UserRanking u2) {
                        return Integer.compare(u2.points, u1.points);
                    }
                });

                // Update rankings in database
                Map<String, Object> updates = new HashMap<>();
                for (int i = 0; i < allUsers.size(); i++) {
                    UserRanking user = allUsers.get(i);
                    int rank = i + 1;
                    updates.put("users/" + user.uid + "/ranking", rank);
                }

                // Update each user individually (avoids root-level permission issues)
                if (!updates.isEmpty()) {
                    for (Map.Entry<String, Object> entry : updates.entrySet()) {
                        String path = entry.getKey();
                        Object value = entry.getValue();
                        database.getReference(path).setValue(value);
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Ignore
            }
        });
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
        // Refresh ranking when returning to profile
        calculateAndUpdateRanking();
    }

    private static class UserRanking {
        String uid;
        int points;
    }

}


