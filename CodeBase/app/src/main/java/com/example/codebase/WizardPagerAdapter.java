package com.example.codebase;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class WizardPagerAdapter extends FragmentStateAdapter {
    public WizardPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 1: return new CreateScheduleFragment();
            case 2: return new CreateSettingsFragment();
            case 3: return new CreateQrFragment();
            default: return new CreateEventFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 4;
    }
}