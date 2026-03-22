package com.example.conducto2.ui.classes;

import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.example.conducto2.R;
import com.example.conducto2.data.manager.DataManager;
import com.example.conducto2.ui.BaseDrawerActivity;
import com.example.conducto2.ui.classes.fragments.HomeworkFragment;
import com.example.conducto2.ui.classes.fragments.LiveFragment;
import com.example.conducto2.ui.classes.fragments.PeopleFragment;
import com.example.conducto2.ui.classes.fragments.ScheduledFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Activity that displays class details with swipeable tabs for Homework, People, Live, and Scheduled.
 * Uses ViewPager2 and BottomNavigationView for navigation.
 */
public class ClassActivity extends BaseDrawerActivity {

    // ViewPager2 is a modern alt. to ViewPager
    private ViewPager2 viewPager;
    private BottomNavigationView bottomNavigationView;
    private TextView joinCodeTextView;

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
                    case 0: itemId = R.id.nav_homework; break;
                    case 1: itemId = R.id.nav_people; break;
                    case 2: itemId = R.id.nav_live; break;
                    case 3: itemId = R.id.nav_scheduled; break;
                    default: itemId = R.id.nav_homework; break;
                }
                if (bottomNavigationView.getSelectedItemId() != itemId) {
                    bottomNavigationView.setSelectedItemId(itemId);
                }
            }
        });
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_homework) {
                if (viewPager.getCurrentItem() != 0) viewPager.setCurrentItem(0);
                return true;
            } else if (itemId == R.id.nav_people) {
                if (viewPager.getCurrentItem() != 1) viewPager.setCurrentItem(1);
                return true;
            } else if (itemId == R.id.nav_live) {
                if (viewPager.getCurrentItem() != 2) viewPager.setCurrentItem(2);
                return true;
            } else if (itemId == R.id.nav_scheduled) {
                if (viewPager.getCurrentItem() != 3) viewPager.setCurrentItem(3);
                return true;
            }
            return false;
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
                case 0: return new HomeworkFragment();
                case 1: return new PeopleFragment();
                case 2: return new LiveFragment();
                case 3: return new ScheduledFragment();
                default: return new HomeworkFragment();
            }
        }

        @Override
        public int getItemCount() {
            return 4;
        }
    }
}
