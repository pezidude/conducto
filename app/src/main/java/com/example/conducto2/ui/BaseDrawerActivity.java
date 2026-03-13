package com.example.conducto2.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.example.conducto2.R;
import com.example.conducto2.data.firebase.FirebaseComm;
import com.example.conducto2.data.firebase.FirestoreManager;
import com.example.conducto2.data.manager.DataManager;
import com.example.conducto2.data.model.Class;
import com.example.conducto2.data.model.User;
import com.example.conducto2.ui.classes.ClassEditActivity;
import com.example.conducto2.ui.classes.ClassListActivity;
import com.example.conducto2.ui.dashboard.DashboardActivity;
import com.example.conducto2.ui.lessons.ClassActivity;
import com.google.android.material.navigation.NavigationView;
import java.util.List;

/**
 * Base activity for screens that include a navigation drawer.
 * It handles the drawer setup, header, and dynamic menu items for classes.
 */
public class BaseDrawerActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    /** The main layout that holds the navigation drawer. */
    protected DrawerLayout drawerLayout;
    /** The view that displays the navigation items. */
    protected NavigationView navigationView;
    protected FirestoreManager firestoreManager;
    /** A unique ID for the dynamically created class menu items. */
    private static final int DYNAMIC_CLASSES_GROUP_ID = 12345;

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

        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                    this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
            drawerLayout.addDrawerListener(toggle);
            toggle.syncState();
        }

        setupDrawerHeader();
        setupDrawerMenu();
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
     * It also adjusts the "Homework" menu item text based on the user type (teacher or student).
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

        if (status != null) status.setText(user.getUserType() != null ? user.getUserType() : "Student");
        if (name != null) name.setText(user.getFname() + " " + user.getLname());
        if (email != null) email.setText(user.getEmail());

        headerView.setOnClickListener(v -> {
            startActivity(new Intent(BaseDrawerActivity.this, ProfileActivity.class));
            drawerLayout.closeDrawer(GravityCompat.START);
        });

        MenuItem homeworkItem = navigationView.getMenu().findItem(R.id.nav_homework);
        if (homeworkItem != null) {
            if ("teacher".equals(user.getUserType())) {
                homeworkItem.setTitle("Homework Review");
            } else {
                homeworkItem.setTitle("Homework");
            }
        }
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
                                    intent.putExtra("class_id", cls.getId());
                                    startActivity(intent);
                                    drawerLayout.closeDrawer(GravityCompat.START);
                                    return true;
                                }).setIcon(R.drawable.ic_launcher_foreground);
                    }
                }
            }
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
        } else if (id == R.id.nav_homework) {
            startActivity(new Intent(this, HomeworkActivity.class));
        } else if (id == R.id.nav_my_classes) {
            startActivity(new Intent(this, ClassListActivity.class));
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }
}