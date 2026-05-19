package com.example.conducto2.ui;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Base64;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.example.conducto2.R;
import com.example.conducto2.data.firebase.FirebaseComm;
import com.example.conducto2.data.firebase.FirestoreManager;
import com.example.conducto2.data.manager.DataManager;
import com.example.conducto2.data.model.Class;
import com.example.conducto2.data.model.Lesson;
import com.example.conducto2.data.model.User;
import com.example.conducto2.ui.classes.ClassListActivity;
import com.example.conducto2.ui.dashboard.DashboardActivity;
import com.example.conducto2.ui.classes.ClassActivity;
import com.example.conducto2.ui.lessons.LessonDetailsActivity;
import com.google.android.material.navigation.NavigationView;

import java.util.List;

/**
 * BaseDrawerActivity
 * 
 * An abstract architectural foundation for all primary application screens that require 
 * a Navigation Drawer. This class handles the complex orchestration of the drawer lifecycle, 
 * dynamic menu generation, and user profile synchronization.
 * 
 * Key Roles:
 * 1. Base UI Hub: Implements a common DrawerLayout/NavigationView structure.
 * 2. Dynamic Roster: Automatically populates the drawer with the user's enrolled classes.
 * 3. Intelligent History: Fetches and maintains a "Recent Lessons" menu with lazy cleanup.
 * 4. User Context: Synchronizes the drawer header with real-time Firebase user data.
 */
public class BaseDrawerActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    /** The root layout component providing the sliding side menu functionality. */
    protected DrawerLayout drawerLayout;

    /** The view responsible for rendering the navigation menu items and the user header. */
    protected NavigationView navigationView;

    /** The database manager inherited by all subclasses forFirestore interactions. */
    protected FirestoreManager firestoreManager;

    /** identifier for the dynamically generated group of Classroom menu items. */
    private static final int DYNAMIC_CLASSES_GROUP_ID = 12345;

    /** identifier for the dynamically generated group of Recent Lesson menu items. */
    private static final int DYNAMIC_RECENT_LESSONS_GROUP_ID = 12346;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        firestoreManager = new FirestoreManager();
    }

    /**
     * Overrides the standard content setter to inject the requested layout into 
     * a standardized drawer frame.
     * 
     * Sequential Logic:
     * 1. Inflate the base drawer container.
     * 2. Locate the central content frame.
     * 3. Nest the specific activity's layout inside that frame.
     * 4. Initialize navigation listeners and the Toolbar toggle.
     * 
     * @param layoutResID The resource ID of the specific screen's layout.
     */
    @Override
    public void setContentView(int layoutResID) {
        // Step 1: Inflate the parent drawer structure.
        DrawerLayout fullView = (DrawerLayout) getLayoutInflater().inflate(R.layout.activity_base_drawer, null);
        FrameLayout activityContainer = fullView.findViewById(R.id.content_frame);
        
        // Step 2: Inject the child layout provided by the subclass into the central FrameLayout.
        getLayoutInflater().inflate(layoutResID, activityContainer, true);
        super.setContentView(fullView);

        drawerLayout = fullView.findViewById(R.id.drawer_layout);
        navigationView = fullView.findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        
        // Disable default coloring to allow custom tints for genre icons.
        navigationView.setItemIconTintList(null);

        // Step 3: Link the system Toolbar to the Drawer state.
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                    this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
            drawerLayout.addDrawerListener(toggle);
            toggle.syncState();
        }

        setupDrawerHeader();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reactive Updates: Ensure user data and menu lists are fresh when the screen returns.
        setupDrawerHeader();
        setupUserClassesMenu();
    }

    /**
     * Populates the top section of the drawer with the authenticated user's profile.
     * Handles async data fetching if the local user instance is currently null.
     */
    private void setupDrawerHeader() {
        if (!FirebaseComm.isUserSignedIn()) return;

        User user = DataManager.getUserInstance();
        if (user == null) {
            // Lazy Loading: If user isn't in cache, fetch and recurse.
            firestoreManager.getUser(fetchedUser -> {
                if (fetchedUser != null) {
                    DataManager.setUser(fetchedUser);
                    setupDrawerHeader();
                }
            });
            return;
        }

        View headerView = navigationView.getHeaderView(0);
        TextView status = headerView.findViewById(R.id.nav_user_status);
        TextView name = headerView.findViewById(R.id.nav_user_name);
        TextView email = headerView.findViewById(R.id.nav_user_email);
        ImageView image = headerView.findViewById(R.id.nav_user_image);
        TextView tvInitials = headerView.findViewById(R.id.nav_avatar_initials);

        if (status != null) status.setText(user.getUserType() != null ? user.getUserType() : "Student");
        if (name != null) name.setText(user.getFname() + " " + user.getLname());
        if (email != null) email.setText(user.getEmail());

        // Avatar Logic: Generate initials as a fallback for missing images.
        if (tvInitials != null) {
            String initials = "";
            if (user.getFname() != null && !user.getFname().isEmpty()) {
                initials += user.getFname().substring(0, 1).toUpperCase();
            }
            if (user.getLname() != null && !user.getLname().isEmpty()) {
                initials += user.getLname().substring(0, 1).toUpperCase();
            }
            tvInitials.setText(initials);
        }

        // Image Processing: Decode Base64 profile picture.
        if (image != null) {
            String base64Image = user.getProfilePictureBase64();
            if (base64Image != null && !base64Image.isEmpty()) {
                try {
                    byte[] decodedString = Base64.decode(base64Image, Base64.DEFAULT);
                    Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                    image.setImageBitmap(decodedByte);
                    image.setVisibility(View.VISIBLE);
                    if (tvInitials != null) tvInitials.setVisibility(View.GONE);
                } catch (Exception e) {
                    image.setVisibility(View.GONE);
                    if (tvInitials != null) tvInitials.setVisibility(View.VISIBLE);
                }
            } else {
                image.setVisibility(View.GONE);
                if (tvInitials != null) tvInitials.setVisibility(View.VISIBLE);
            }
        }

        headerView.setOnClickListener(v -> {
            startActivity(new Intent(BaseDrawerActivity.this, ProfileActivity.class));
            drawerLayout.closeDrawer(GravityCompat.START);
        });
    }

    /**
     * Synchronizes the navigation menu with the user's current classroom list from Firestore.
     */
    private void setupUserClassesMenu() {
        if (!FirebaseComm.isUserSignedIn()) return;
        String email = FirebaseComm.authUserEmail();

        firestoreManager.getClassesForUser(email, classes -> {
            if (classes != null) {
                Menu menu = navigationView.getMenu();
                // State Management: Clear old items before adding fresh data to prevent duplicates.
                menu.removeGroup(DYNAMIC_CLASSES_GROUP_ID); 

                if (!classes.isEmpty()) {
                    for (Class cls : classes) {
                         menu.add(DYNAMIC_CLASSES_GROUP_ID, Menu.NONE, Menu.NONE, cls.getName())
                                .setOnMenuItemClickListener(menuItem -> {
                                    Intent intent = new Intent(BaseDrawerActivity.this, ClassActivity.class);
                                    DataManager.setCurClass(cls);
                                    startActivity(intent);
                                    drawerLayout.closeDrawer(GravityCompat.START);
                                    return true;
                                }).setIcon(R.drawable.ic_lessons);
                    }
                }
            }
        });

        setupRecentLessonsMenu();
    }

    /**
     * Orchestrates the population of the "Recent Lessons" menu.
     * Implements an asynchronous verification logic:
     * 1. Fetches recent access logs.
     * 2. Verifies that the referenced lessons still exist in the database.
     * 3. Lazy Cleanup: Automatically deletes log entries for "ghost" lessons.
     * 4. Multi-Threaded Sync: Uses Atomic counters to wait for all async checks 
     *    before re-ordering and rendering the menu on the UI thread.
     */
    private void setupRecentLessonsMenu() {
        if (!FirebaseComm.isUserSignedIn()) return;
        String email = FirebaseComm.authUserEmail();

        firestoreManager.getRecentLessons(email, recentLessons -> {
            if (recentLessons == null || recentLessons.isEmpty()) {
                runOnUiThread(() -> navigationView.getMenu().removeGroup(DYNAMIC_RECENT_LESSONS_GROUP_ID));
                return;
            }

            final List<Lesson> validLessons = new java.util.ArrayList<>();
            final int total = recentLessons.size();
            final java.util.concurrent.atomic.AtomicInteger count = new java.util.concurrent.atomic.AtomicInteger(0);

            // Sequential Parallelism: Iterate through logs and fetch full lesson details for each.
            for (Lesson lessonLog : recentLessons) {
                firestoreManager.getLesson(lessonLog.getClassId(), lessonLog.getId(), actualLesson -> {
                    if (actualLesson != null) {
                        synchronized (validLessons) {
                            validLessons.add(actualLesson);
                        }
                    } else {
                        // Integrity Fix: Entry exists in history but lesson document is gone.
                        firestoreManager.deleteRecentLessonLog(email, lessonLog.getId());
                    }

                    // Synchronization Point: Check if all asynchronous fetches are complete.
                    if (count.incrementAndGet() == total) {
                        runOnUiThread(() -> {
                            Menu menu = navigationView.getMenu();
                            menu.removeGroup(DYNAMIC_RECENT_LESSONS_GROUP_ID);
                            
                            // Re-Ordering logic: Restore the time-based sequence after parallel fetching.
                            synchronized (validLessons) {
                                for (Lesson log : recentLessons) {
                                    for (Lesson valid : validLessons) {
                                        if (valid.getId().equals(log.getId())) {
                                            addRecentLessonToMenu(menu, valid);
                                            break;
                                        }
                                    }
                                }
                            }
                        });
                    }
                });
            }
        });
    }

    /**
     * Generates a polymorphically styled menu item for a recent lesson.
     * Applies Spannable tints and genre-specific icons.
     * 
     * @param menu The parent NavigationView menu.
     * @param lesson The lesson model to render.
     */
    private void addRecentLessonToMenu(Menu menu, Lesson lesson) {
        String genreLabel = lesson.getGenreLabel();
        String fullTitle = "[" + genreLabel + "] " + lesson.getTitle();
        int color = ContextCompat.getColor(this, lesson.getRecentLessonTintResId());

        // Rich Formatting: Apply bold style and genre color to the prefix.
        SpannableString spannableTitle = new SpannableString(fullTitle);
        int prefixEnd = genreLabel.length() + 2;
        spannableTitle.setSpan(new ForegroundColorSpan(color), 0, prefixEnd, 0);
        spannableTitle.setSpan(new StyleSpan(Typeface.BOLD), 0, prefixEnd, 0);

        MenuItem item = menu.add(DYNAMIC_RECENT_LESSONS_GROUP_ID, Menu.NONE, Menu.NONE, spannableTitle)
                .setIcon(lesson.getGenreIconResId());
        
        // Polymorphic Styling: Mutate the menu icon tint to match the genre theme.
        Drawable icon = item.getIcon();
        if (icon != null) {
            icon.mutate().setTint(color);
        }

        item.setOnMenuItemClickListener(menuItem -> {
            DataManager.setCurLesson(lesson);
                    firestoreManager.getClassById(lesson.getClassId(), cls -> {
                        if (cls != null) {
                            DataManager.setCurClass(cls);
                        }
                        Intent intent = new Intent(BaseDrawerActivity.this, LessonDetailsActivity.class);
                        startActivity(intent);
                        drawerLayout.closeDrawer(GravityCompat.START);
                    });
                    return true;
                });
    }

    /**
     * Handles selection of static top-level navigation items.
     */
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.nav_dashboard) {
            startActivity(new Intent(this, DashboardActivity.class));
        } else if (id == R.id.nav_my_classes) {
            startActivity(new Intent(this, ClassListActivity.class));
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }
}