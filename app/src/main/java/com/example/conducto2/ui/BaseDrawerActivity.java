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
 * Base activity for screens that include a navigation drawer.
 * It handles the drawer setup, header, and dynamic menu items for classes.
 * Activities that need this functionality should extend this class.
 */
public class BaseDrawerActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    /** The main layout that holds the navigation drawer. */
    protected DrawerLayout drawerLayout;
    /** The view that displays the navigation items. */
    protected NavigationView navigationView;
    protected FirestoreManager firestoreManager;
    /** A unique ID for the dynamically created class menu items. */
    private static final int DYNAMIC_CLASSES_GROUP_ID = 12345;
    /** A unique ID for the dynamically created recent lesson menu items. */
    private static final int DYNAMIC_RECENT_LESSONS_GROUP_ID = 12346;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        firestoreManager = new FirestoreManager();
    }

    /**
     * Sets up the content view for the activity, inflating the base drawer layout and embedding the specific activity's layout within it.
     * It also initializes the toolbar and drawer toggle.
     * @param layoutResID Resource ID to be inflated.
     */
    @Override
    public void setContentView(int layoutResID) {
        DrawerLayout fullView = (DrawerLayout) getLayoutInflater().inflate(R.layout.activity_base_drawer, null);
        FrameLayout activityContainer = fullView.findViewById(R.id.content_frame);
        getLayoutInflater().inflate(layoutResID, activityContainer, true);
        super.setContentView(fullView);

        drawerLayout = fullView.findViewById(R.id.drawer_layout);
        navigationView = fullView.findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        
        // Disable default tinting to allow manual colorization of dynamic items
        navigationView.setItemIconTintList(null);

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

    /**
     * Refreshes the drawer header and menu when the activity resumes to ensure data is up to date.
     */
    @Override
    protected void onResume() {
        super.onResume();
        // Refresh drawer in case user details or class list changed
        setupDrawerHeader();
        setupDrawerMenu();
    }

    /**
     * Sets up the navigation drawer header with the current user's information, including name, email, and status.
     */
    private void setupDrawerHeader() {
        if (!FirebaseComm.isUserSignedIn()) return;

        User user = DataManager.getUserInstance();
        if (user == null) {
            // User data not loaded yet, try to fetch it
            firestoreManager.getUser(fetchedUser -> {
                if (fetchedUser != null) {
                    DataManager.setUser(fetchedUser);
                    // Now that we have the user, setup the header again
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
     * Populates the navigation drawer with a list of the user's classes, fetched from Firestore.
     * Each class is a clickable menu item that navigates to the {@link ClassActivity}.
     */
    private void setupDrawerMenu() {
        if (!FirebaseComm.isUserSignedIn()) return;
        String email = FirebaseComm.authUserEmail();

        firestoreManager.getClassesForUser(email, classes -> {
            if (classes != null) {
                Menu menu = navigationView.getMenu();
                menu.removeGroup(DYNAMIC_CLASSES_GROUP_ID); // Clear previous dynamic items

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
     * Fetches and displays the most recently accessed lessons in the navigation drawer.
     * Includes lazy cleanup for deleted lessons.
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

            for (Lesson lessonLog : recentLessons) {
                firestoreManager.getLesson(lessonLog.getClassId(), lessonLog.getId(), actualLesson -> {
                    if (actualLesson != null) {
                        synchronized (validLessons) {
                            validLessons.add(Lesson.fromBase(actualLesson));
                        }
                    } else {
                        // Ghost entry detected, perform lazy cleanup
                        firestoreManager.deleteRecentLessonLog(email, lessonLog.getId());
                    }

                    if (count.incrementAndGet() == total) {
                        // All checks done, update menu on UI thread
                        runOnUiThread(() -> {
                            Menu menu = navigationView.getMenu();
                            menu.removeGroup(DYNAMIC_RECENT_LESSONS_GROUP_ID);
                            
                            // Re-sort to match the order of the original fetch (most recent first)
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

    private void addRecentLessonToMenu(Menu menu, Lesson lesson) {
        String genreLabel = lesson.getGenreLabel();
        String fullTitle = "[" + genreLabel + "] " + lesson.getTitle();
        int color = ContextCompat.getColor(this, lesson.getRecentLessonTintResId());

        SpannableString spannableTitle = new SpannableString(fullTitle);
        // Color and Bold the "[Genre]" prefix
        int prefixEnd = genreLabel.length() + 2;
        spannableTitle.setSpan(new ForegroundColorSpan(color), 0, prefixEnd, 0);
        spannableTitle.setSpan(new StyleSpan(Typeface.BOLD), 0, prefixEnd, 0);

        MenuItem item = menu.add(DYNAMIC_RECENT_LESSONS_GROUP_ID, Menu.NONE, Menu.NONE, spannableTitle)
                .setIcon(lesson.getGenreIconResId());
        
        // Apply polymorphic tint to the icon
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
     * Handles clicks on navigation drawer items, navigating to the corresponding activities.
     * @param item The selected item.
     * @return True if the event was handled, false otherwise.
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