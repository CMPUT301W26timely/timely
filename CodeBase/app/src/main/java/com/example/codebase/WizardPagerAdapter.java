package com.example.codebase;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

/**
 * {@link FragmentStateAdapter} that drives the event-creation wizard's
 * {@link androidx.viewpager2.widget.ViewPager2}.
 *
 * <p>The wizard is composed of four sequential steps:
 * <ol>
 *   <li>(position 0) {@link CreateEventFragment} — basic event details.</li>
 *   <li>(position 1) {@link CreateScheduleFragment} — dates and schedule.</li>
 *   <li>(position 2) {@link CreateSettingsFragment} — lottery and capacity settings.</li>
 *   <li>(position 3) {@link CreateQrFragment} — QR code generation and review.</li>
 * </ol>
 */
public class WizardPagerAdapter extends FragmentStateAdapter {

    /**
     * Constructs a new {@code WizardPagerAdapter} bound to the given
     * {@link FragmentActivity}'s lifecycle.
     *
     * @param fragmentActivity The host {@link FragmentActivity}; must not be {@code null}.
     */
    public WizardPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    /**
     * Creates the {@link Fragment} for the requested wizard step.
     *
     * @param position The zero-based page index.
     * @return The corresponding wizard step fragment:
     *         {@link CreateScheduleFragment} for position 1,
     *         {@link CreateSettingsFragment} for position 2,
     *         {@link CreateQrFragment} for position 3, or
     *         {@link CreateEventFragment} for position 0 (and any unexpected value).
     */
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

    /**
     * Returns the total number of pages in the wizard.
     *
     * @return {@code 4}, one for each creation step.
     */
    @Override
    public int getItemCount() {
        return 4;
    }
}