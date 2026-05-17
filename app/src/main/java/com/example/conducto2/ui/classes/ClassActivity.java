package com.example.conducto2.ui.classes;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.example.conducto2.R;
import com.example.conducto2.data.firebase.FirestoreManager;
import com.example.conducto2.data.manager.DataManager;
import com.example.conducto2.data.model.Lesson;
import com.example.conducto2.data.model.User;
import com.example.conducto2.ui.BaseDrawerActivity;
import com.example.conducto2.ui.classes.fragments.ScheduledFragment;
import com.example.conducto2.ui.classes.fragments.LiveFragment;
import com.example.conducto2.ui.classes.fragments.PeopleFragment;
import com.example.conducto2.ui.classes.fragments.HistoryFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import com.example.conducto2.data.model.Class;

/**
 * ClassActivity
 * 
 * The main classroom navigation hub. This activity coordinates the high-level 
 * user experience within a specific class, providing a multi-tab interface 
 * for scheduling, participant rosters, live lessons, and history.
 * 
 * Technical Architecture:
 * 1. Layout: Uses {@link ViewPager2} to host four distinct Fragments.
 * 2. Navigation: Integrates a {@link BottomNavigationView} that is synchronized 
 *    with the ViewPager.
 * 3. Real-time Status: Implements {@link FirestoreManager.LiveLessonListener} 
 *    to provide class-wide visual alerts when a lesson goes "Live."
 * 4. Custom Styling: Implements manual tinting for navigation items to support 
 *    dynamic state-based alerts (e.g., the red Live dot).
 */
public class ClassActivity extends BaseDrawerActivity implements FirestoreManager.LiveLessonListener {

    /** The container for swipeable fragments. */
    private ViewPager2 viewPager;

    /** The primary navigation bar for classroom sub-sections. */
    private BottomNavigationView bottomNavigationView;

    /** UI field displaying the class's Join Code for quick reference. */
    private TextView joinCodeTextView;

    /** 
     * Local state tracking whether any lesson in the class is currently active. 
     * Drives the color state of the "Live" navigation icon.
     */
    private boolean isLiveActive = false;

    /** Identifier for logging. */
    public static final String TAG = "ClassActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_class);

        initViews();
        setupViewPager();
        setupBottomNavigation();

        // Initialize UI styling for the navigation components.
        applyNavigationStyles();

        // Registration of the real-time listener to detect Live sessions.
        Class currentClass = DataManager.getCurClass();
        if (currentClass == null || currentClass.getId() == null) {
            Toast.makeText(this, "Error: Class data is missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        firestoreManager.listenForLiveLesson(currentClass.getId(), this);
        Log.d(TAG, "Listening for live lesson for class: " + currentClass.getId());

        // Navigation Resolution: Support direct deep-linking to specific fragments.
        if (getIntent().hasExtra("target_tab")) {
            int targetTab = getIntent().getIntExtra("target_tab", 0);
            viewPager.setCurrentItem(targetTab, false);
        }
    }

    /**
     * Initializes views and performs permanent overrides on BottomNavigationView 
     * to allow for manual state-based color control.
     */
    private void initViews() {
        viewPager = findViewById(R.id.view_pager);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        joinCodeTextView = findViewById(R.id.class_join_code);

        // Permanently disable global tints to allow per-item manual overrides 
        // in applyNavigationStyles(). This prevents default theme behaviors from 
        // overwriting the dynamic red "Live" indicator.
        bottomNavigationView.setItemIconTintList(null);
        bottomNavigationView.setItemTextColor(null);

        refreshClassInfo();
    }

    /**
     * Synchronizes the UI text and ActionBar title with the current DataManager state.
     */
    private void refreshClassInfo() {
        Class currentClass = DataManager.getCurClass();
        if (currentClass != null) {
            joinCodeTextView.setText("Code: " + currentClass.getJoinCode());
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(currentClass.getName());
            }
        }
    }

    /**
     * Configures the ViewPager2 adapter and implements bidirectional synchronization 
     * with the BottomNavigationView.
     */
    private void setupViewPager() {
        SectionsPagerAdapter pagerAdapter = new SectionsPagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);

        // ViewPager -> BottomNav sync: Update selected item when user swipes.
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                int itemId;
                switch (position) {
                    case 0: itemId = R.id.nav_scheduled; break;
                    case 1: itemId = R.id.nav_people; break;
                    case 2: itemId = R.id.nav_live; break;
                    case 3: itemId = R.id.nav_history; break;
                    default: itemId = R.id.nav_history; break;
                }
                if (bottomNavigationView.getSelectedItemId() != itemId) {
                    bottomNavigationView.setSelectedItemId(itemId);
                }
                // Refresh item tints based on the new selection state.
                applyNavigationStyles();
            }
        });
    }

    /**
     * Configures the BottomNav listeners to trigger ViewPager navigation.
     */
    private void setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            // BottomNav -> ViewPager sync: Update current page when user clicks a button.
            if (itemId == R.id.nav_scheduled) {
                if (viewPager.getCurrentItem() != 0) viewPager.setCurrentItem(0);
            } else if (itemId == R.id.nav_people) {
                if (viewPager.getCurrentItem() != 1) viewPager.setCurrentItem(1);
            } else if (itemId == R.id.nav_live) {
                if (viewPager.getCurrentItem() != 2) viewPager.setCurrentItem(2);
            } else if (itemId == R.id.nav_history) {
                if (viewPager.getCurrentItem() != 3) viewPager.setCurrentItem(3);
            }

            applyNavigationStyles();
            return true;
        });
    }

    /**
     * Callback from Firestore when a lesson's live state changes.
     * Triggers a full UI refresh of the navigation bar to alert the user.
     */
    @Override
    public void onLiveLessonChanged(Lesson lesson) {
        runOnUiThread(() -> {
            isLiveActive = (lesson != null);
            applyNavigationStyles();
        });
    }


    /**
     * Adapter for the ViewPager2. 
     * Manages the lifecycle of the four primary classroom sub-fragments.
     */
    private static class SectionsPagerAdapter extends FragmentStateAdapter {
        public SectionsPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
            super(fragmentActivity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0: return new ScheduledFragment();
                case 1: return new PeopleFragment();
                case 2: return new LiveFragment();
                case 3: return new HistoryFragment();
                default: return new HistoryFragment();
            }
        }

        @Override
        public int getItemCount() {
            return 4;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        applyNavigationStyles();
        refreshClassInfo();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        User user = DataManager.getUserInstance();
        // Permission Control: Only instructors can access the classroom settings edit screen.
        if (user != null && "teacher".equals(user.getUserType())) {
            MenuItem editItem = menu.add(Menu.NONE, 1001, Menu.NONE, "Edit");
            editItem.setIcon(R.drawable.ic_edit);
            editItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
            editItem.setOnMenuItemClickListener(v -> {
                Intent intent = new Intent(this, ClassEditActivity.class);
                intent.putExtra("class_obj", DataManager.getCurClass());
                startActivity(intent);
                return true;
            });
        }
        return super.onCreateOptionsMenu(menu);
    }

    /**
     * Manually applies colors and icons to BottomNavigationView items.
     * 
     * Logical Reasoning:
     * We disable the default XML-based tinting mechanism to allow for a 
     * non-binary state model. While standard items switch between "Selected" and 
     * "Default" colors, the 'Live' button must also account for the 'isLiveActive' 
     * state independently of whether it is currently the selected tab.
     */
    private void applyNavigationStyles() {
        Menu menu = bottomNavigationView.getMenu();
        int selectedId = bottomNavigationView.getSelectedItemId();

        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            int itemId = item.getItemId();
            boolean isSelected = itemId == selectedId;

            if (itemId == R.id.nav_live) {
                // Feature: Red Alert for Live Sessions.
                // Icon and Text color become high-contrast red if a lesson is live, 
                // overriding standard navigation colors.
                item.setIcon(isLiveActive ? R.drawable.ic_live_dot_red : R.drawable.ic_live_dot_white);
                item.setIconTintList(null);

                int color = ContextCompat.getColor(this, isLiveActive ? R.color.red_600 : (isSelected ? R.color.nav_icon_selected : R.color.nav_icon_default));
                SpannableString s = new SpannableString(item.getTitle());
                s.setSpan(new ForegroundColorSpan(color), 0, s.length(), 0);
                item.setTitle(s);
            } else {
                // Standard Tab Styling: Switch between selected and default tints.
                int color = ContextCompat.getColor(this, isSelected ? R.color.nav_icon_selected : R.color.nav_icon_default);

                Drawable icon = item.getIcon();
                if (icon != null) {
                    icon = icon.mutate();
                    DrawableCompat.setTint(icon, color);
                    item.setIcon(icon);
                }

                SpannableString s = new SpannableString(item.getTitle());
                s.setSpan(new ForegroundColorSpan(color), 0, s.length(), 0);
                item.setTitle(s);
            }
        }
    }
}