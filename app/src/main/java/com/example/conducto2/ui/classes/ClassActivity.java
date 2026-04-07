package com.example.conducto2.ui.classes;

import android.content.res.ColorStateList;
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
import com.example.conducto2.ui.BaseDrawerActivity;
import com.example.conducto2.ui.classes.fragments.HomeworkFragment;
import com.example.conducto2.ui.classes.fragments.LiveFragment;
import com.example.conducto2.ui.classes.fragments.PeopleFragment;
import com.example.conducto2.ui.classes.fragments.UpcomingFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Activity that displays class details with swipeable tabs for Upcoming, People, Live, and Homework.
 * Uses ViewPager2 and BottomNavigationView for navigation.
 */
public class ClassActivity extends BaseDrawerActivity implements FirestoreManager.DBResult {

    private ViewPager2 viewPager;
    private BottomNavigationView bottomNavigationView;
    private TextView joinCodeTextView;
    private boolean isLiveActive = false;

    public static final String TAG = "ClassActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_class);

        if (getIntent().hasExtra("class_id")) {
            String classId = getIntent().getStringExtra("class_id");
            DataManager.setCurClassID(classId);
        }

        initViews();
        setupViewPager();
        setupBottomNavigation();
        fetchClassDetails();
    }

    private void initViews() {
        viewPager = findViewById(R.id.view_pager);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        joinCodeTextView = findViewById(R.id.class_join_code);

        // Permanently disable global tints and active indicator to allow per-item manual overrides
        // for the live button. This avoids conflicts with default theme colors
        bottomNavigationView.setItemIconTintList(null);
        bottomNavigationView.setItemTextColor(null);
    }

    private void setupViewPager() {
        SectionsPagerAdapter pagerAdapter = new SectionsPagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);

        // Synchronize ViewPager swipe with BottomNavigationView selection
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                int itemId;
                switch (position) {
                    case 0: itemId = R.id.nav_upcoming; break;
                    case 1: itemId = R.id.nav_people; break;
                    case 2: itemId = R.id.nav_live; break;
                    case 3: itemId = R.id.nav_homework; break;
                    default: itemId = R.id.nav_upcoming; break;
                }
                if (bottomNavigationView.getSelectedItemId() != itemId) {
                    bottomNavigationView.setSelectedItemId(itemId);
                }
                applyNavigationStyles();
            }
        });
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_upcoming) {
                if (viewPager.getCurrentItem() != 0) viewPager.setCurrentItem(0);
            } else if (itemId == R.id.nav_people) {
                if (viewPager.getCurrentItem() != 1) viewPager.setCurrentItem(1);
            } else if (itemId == R.id.nav_live) {
                if (viewPager.getCurrentItem() != 2) viewPager.setCurrentItem(2);
            } else if (itemId == R.id.nav_homework) {
                if (viewPager.getCurrentItem() != 3) viewPager.setCurrentItem(3);
            }
            applyNavigationStyles();
            return true;
        });
    }

    private void fetchClassDetails() {
        String classId = DataManager.getCurClassID();
        if (classId == null) return;
        FirebaseFirestore.getInstance().collection("classes").document(classId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        com.example.conducto2.data.model.Class currentClass = documentSnapshot.toObject(com.example.conducto2.data.model.Class.class);
                        if (currentClass != null) {
                            joinCodeTextView.setText("Code: " + currentClass.getJoinCode());
                            if (getSupportActionBar() != null) {
                                getSupportActionBar().setTitle(currentClass.getName());
                            }
                        }
                    }
                });
    }

    /**
     * Adapter for the ViewPager2 to handle the 4 fragments.
     */
    private static class SectionsPagerAdapter extends FragmentStateAdapter {
        public SectionsPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
            super(fragmentActivity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0: return new UpcomingFragment();
                case 1: return new PeopleFragment();
                case 2: return new LiveFragment();
                case 3: return new HomeworkFragment();
                default: return new UpcomingFragment();
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
        firestoreManager.setDbResult(this);
        firestoreManager.isLiveLessonActive(DataManager.getCurClassID());
        Log.d(TAG, "onResume: isLiveActive=" + isLiveActive);
        applyNavigationStyles();
    }


    @Override
    public void uploadResult(boolean success) {
        this.isLiveActive = success;
        Log.d(TAG, "uploadResult: isLiveActive updated to " + success);
        applyNavigationStyles();
    }

    private void applyNavigationStyles() {
        Menu menu = bottomNavigationView.getMenu();
        int selectedId = bottomNavigationView.getSelectedItemId();

        /* A loop that sets the highlights the live button if necessary*/
        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            int itemId = item.getItemId();
            boolean isSelected = itemId == selectedId;

            if (itemId == R.id.nav_live) {
                // pre-colored icons for Live button to ensure reliability
                item.setIcon(isLiveActive ? R.drawable.ic_live_dot_red : R.drawable.ic_live_dot_white);
                item.setIconTintList(null);

                int color = ContextCompat.getColor(this, isLiveActive ? R.color.red_600 : R.color.white);
                // A string object used for android styling
                SpannableString s = new SpannableString(item.getTitle());
                s.setSpan(new ForegroundColorSpan(color), 0, s.length(), 0);
                item.setTitle(s);
            } else {
                // standard behavior for other items
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


    @Override
    public void displayMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
