package com.example.flip.ui.profile;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
public class ProfilePagerAdapter extends FragmentStateAdapter {

    public ProfilePagerAdapter(@NonNull Fragment fragment) {
        super(fragment);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new MessagesTabFragment();
            case 1:
                return new FriendsTabFragment();
            case 2:
                return new ActivitiesTabFragment();
            default:
                return new MessagesTabFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 3;
    }
}