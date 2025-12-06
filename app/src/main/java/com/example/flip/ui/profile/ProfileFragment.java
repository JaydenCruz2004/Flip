package com.example.flip.ui.profile;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.example.flip.R;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ProfileFragment extends Fragment {
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private ProfilePagerAdapter pagerAdapter;

    private ImageView profileImage;
    private TextView usernameText, pointsText, rankingText, gamesText;

    private FirebaseAuth auth;
    private FirebaseDatabase database;
    private String currentUserId;
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_profile_new, container, false);

        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();

        currentUserId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;

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

        loadUserProfile();

        return root;
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

                    android.util.Log.d("ProfileDebug", "User data exists");
                    android.util.Log.d("ProfileDebug", "Username: " + snapshot.child("Username").getValue());
                    android.util.Log.d("ProfileDebug", "username: " + snapshot.child("username").getValue());
                    android.util.Log.d("ProfileDebug", "points: " + snapshot.child("points").getValue());
                    android.util.Log.d("ProfileDebug", "gamesPlayed: " + snapshot.child("gamesPlayed").getValue());
                    android.util.Log.d("ProfileDebug", "All data: " + snapshot.getValue());


                    String username = snapshot.child("Username").getValue(String.class);
                    if (username == null || username.isEmpty()) {
                        username = snapshot.child("username").getValue(String.class);
                    }

                    Integer points = snapshot.child("points").getValue(Integer.class);
                    Integer ranking = snapshot.child("ranking").getValue(Integer.class);
                    Integer games = snapshot.child("gamesPlayed").getValue(Integer.class);

                    usernameText.setText("@" + (username != null ? username : "User"));
                    pointsText.setText(String.valueOf(points != null ? points : 0));
                    rankingText.setText(ranking != null && ranking > 0 ? getOrdinal(ranking) : "N/A");
                    gamesText.setText(String.valueOf(games != null ? games : 0));
                } else {
                    android.util.Log.d("ProfileDebug", "User data does NOT exist!");
                    Toast.makeText(getContext(), "Profile data not found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                android.util.Log.e("ProfileDebug", "Error: " + error.getMessage());
                Toast.makeText(getContext(), "Failed to load profile", Toast.LENGTH_SHORT).show();
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
}


