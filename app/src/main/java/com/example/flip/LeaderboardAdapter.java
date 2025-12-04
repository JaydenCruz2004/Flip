package com.example.flip;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.flip.model.User;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class LeaderboardAdapter extends RecyclerView.Adapter<LeaderboardAdapter.LeaderboardViewHolder> {

    private List<User> userList = new ArrayList<>();
    private String currentUserId;

    public LeaderboardAdapter(String currentUserId)
    {
        this.currentUserId = currentUserId;
    }

    public void setUserList(List<User> users)
    {
        this.userList = users;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public LeaderboardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        int layoutId = (viewType == 1) ?
                R.layout.item_leaderboard_entry_highlighted :
                R.layout.item_leaderboard_entry;

        View view = LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false);
        return new LeaderboardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LeaderboardViewHolder holder, int position)
    {
        User user = userList.get(position);
        int rank = position + 1;

        // Set rank
        holder.rankBadge.setText(String.valueOf(rank));

        // Set username
        String username = user.getUsername();
        if (username == null || username.isEmpty()) {
            username = user.getEmail() != null ? user.getEmail().split("@")[0] : "User";
        }
        holder.usernameText.setText("@" + username);

        // Set games played
        holder.gamesPlayedText.setText(user.getGamesPlayed() + " games played");

        // Set points with formatting (e.g., 1,234)
        NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.US);
        holder.pointsText.setText(numberFormat.format(user.getPoints()));

        // Show trophy for top 3
        if (rank <= 3) {
            holder.trophyIcon.setVisibility(View.VISIBLE);

            // Different colors for gold, silver, bronze
            switch (rank) {
                case 1:
                    holder.trophyIcon.setColorFilter(0xFFFFD700); // Gold
                    break;
                case 2:
                    holder.trophyIcon.setColorFilter(0xFFC0C0C0); // Silver
                    break;
                case 3:
                    holder.trophyIcon.setColorFilter(0xFFCD7F32); // Bronze
                    break;
            }
        } else {
            holder.trophyIcon.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    @Override
    public int getItemViewType(int position) {
        // Return 1 for current user (highlighted layout), 0 for others
        User user = userList.get(position);
        return (user.getUid() != null && user.getUid().equals(currentUserId)) ? 1 : 0;
    }

    static class LeaderboardViewHolder extends RecyclerView.ViewHolder {
        TextView rankBadge;
        ImageView profileImage;
        TextView usernameText;
        TextView gamesPlayedText;
        TextView pointsText;
        ImageView trophyIcon;

        public LeaderboardViewHolder(@NonNull View itemView) {
            super(itemView);
            rankBadge = itemView.findViewById(R.id.rankBadge);
            profileImage = itemView.findViewById(R.id.profileImage);
            usernameText = itemView.findViewById(R.id.usernameText);
            gamesPlayedText = itemView.findViewById(R.id.gamesPlayedText);
            pointsText = itemView.findViewById(R.id.pointsText);
            trophyIcon = itemView.findViewById(R.id.trophyIcon);
        }
    }
}




