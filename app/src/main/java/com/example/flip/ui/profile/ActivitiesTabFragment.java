package com.example.flip.ui.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.flip.R;
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

public class ActivitiesTabFragment extends Fragment {

    private LinearLayout activitiesContainer;
    private ProgressBar loadingSpinner;
    private TextView emptyStateText;
    private Button addActivityButton;

    private FirebaseAuth auth;
    private FirebaseDatabase database;
    private String currentUserId;
    private List<String> activities = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_activities_tab, container, false);

        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();
        currentUserId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;

        activitiesContainer = root.findViewById(R.id.activitiesContainer);
        loadingSpinner = root.findViewById(R.id.loadingSpinner);
        emptyStateText = root.findViewById(R.id.emptyStateText);
        addActivityButton = root.findViewById(R.id.addActivityButton);

        addActivityButton.setOnClickListener(v -> showAddActivityDialog());

        loadActivities();

        return root;
    }

    private void loadActivities() {
        if (currentUserId == null) return;

        loadingSpinner.setVisibility(View.VISIBLE);
        activitiesContainer.removeAllViews();

        DatabaseReference userRef = database.getReference("users").child(currentUserId).child("activities");
        userRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                activities.clear();

                for (DataSnapshot activitySnapshot : snapshot.getChildren()) {
                    String activity = activitySnapshot.getValue(String.class);
                    if (activity != null) {
                        activities.add(activity);
                    }
                }

                displayActivities();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                loadingSpinner.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Failed to load activities", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayActivities() {
        loadingSpinner.setVisibility(View.GONE);
        activitiesContainer.removeAllViews();

        if (activities.isEmpty()) {
            emptyStateText.setVisibility(View.VISIBLE);
            activitiesContainer.setVisibility(View.GONE);
        } else {
            emptyStateText.setVisibility(View.GONE);
            activitiesContainer.setVisibility(View.VISIBLE);

            for (int i = 0; i < activities.size(); i++) {
                addActivityItem(activities.get(i), i);
            }
        }
    }

    private void addActivityItem(String activity, int index) {
        View itemView = LayoutInflater.from(getContext())
                .inflate(R.layout.item_activity, activitiesContainer, false);

        TextView activityText = itemView.findViewById(R.id.activityText);
        ImageView editButton = itemView.findViewById(R.id.editActivityButton);
        ImageView deleteButton = itemView.findViewById(R.id.deleteActivityButton);

        activityText.setText(activity);

        editButton.setOnClickListener(v -> showEditActivityDialog(activity, index));
        deleteButton.setOnClickListener(v -> deleteActivity(index));

        activitiesContainer.addView(itemView);
    }
    private void showAddActivityDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_activity, null);

        EditText activityInput = dialogView.findViewById(R.id.activityInput);

        builder.setView(dialogView)
                .setTitle("Add New Activity")
                .setPositiveButton("Add", (dialog, which) -> {
                    String newActivity = activityInput.getText().toString().trim();
                    if (!newActivity.isEmpty()) {
                        addActivity(newActivity);
                    } else {
                        Toast.makeText(getContext(), "Activity cannot be empty", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showEditActivityDialog(String currentActivity, int index) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_activity, null);

        EditText activityInput = dialogView.findViewById(R.id.activityInput);
        activityInput.setText(currentActivity);

        builder.setView(dialogView)
                .setTitle("Edit Activity")
                .setPositiveButton("Save", (dialog, which) -> {
                    String updatedActivity = activityInput.getText().toString().trim();
                    if (!updatedActivity.isEmpty()) {
                        editActivity(updatedActivity, index);
                    } else {
                        Toast.makeText(getContext(), "Activity cannot be empty", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void addActivity(String activity) {
        if (currentUserId == null) return;

        activities.add(activity);
        saveActivitiesToFirebase();
    }

    private void editActivity(String newActivity, int index) {
        if (currentUserId == null || index >= activities.size()) return;

        activities.set(index, newActivity);
        saveActivitiesToFirebase();
    }

    private void deleteActivity(int index) {
        if (currentUserId == null || index >= activities.size()) return;

        activities.remove(index);
        saveActivitiesToFirebase();
    }

    private void saveActivitiesToFirebase() {
        DatabaseReference userRef = database.getReference("users").child(currentUserId);
        userRef.child("activities").setValue(activities)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Activities updated!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to update activities", Toast.LENGTH_SHORT).show();
                });
    }



}
