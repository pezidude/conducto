package com.example.conducto2.ui.dashboard;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.conducto2.R;
import com.example.conducto2.data.firebase.FirebaseComm;
import com.example.conducto2.data.manager.DataManager;
import com.example.conducto2.data.model.User;
import com.example.conducto2.ui.BaseDrawerActivity;
import com.example.conducto2.ui.ProfileActivity;
import com.example.conducto2.ui.classes.ClassActivity;
import com.example.conducto2.ui.classes.ClassListActivity;
import com.google.android.material.button.MaterialButton;

import com.example.conducto2.data.model.Class;
import com.example.conducto2.data.model.Lesson;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

/**
 * DashboardActivity
 * 
 * The primary home screen and user portal of the application. 
 * Inheriting from {@link BaseDrawerActivity}, it provides a personalized overview 
 * of the user's musical activities, statistics, and real-time alerts.
 * 
 * Key Responsibilities:
 * 1. User Synchronization: Fetches and displays the logged-in user's profile and avatar.
 * 2. Summary Statistics: Calculates and renders metrics like classroom count and 
 *    total student reach (for teachers).
 * 3. Reactive Alerting: Monitors all of the user's classrooms in parallel to 
 *    display immediate shortcuts for active "Live Lessons."
 */
public class DashboardActivity extends BaseDrawerActivity implements com.example.conducto2.data.firebase.FirestoreManager.UserFetchListener {

    /** Identifier for logging. */
    private static final String TAG = "DashboardActivity";

    /** UI component for initiating the logout process. */
    private MaterialButton btnLogout;

    /** Header labels for personalized greetings and role status. */
    private TextView tvWelcomeMessage, tvUserTypeStatus;
    
    /** Counters for displaying aggregate classroom and student data. */
    private TextView tvClassesCount, tvStudentsCount;
    private View cardClassesCount, cardStudentsCount;

    /** Binary profile image component. */
    private ImageView ivDashboardProfilePicture;

    /** UI placeholder for user initials when an image is unavailable. */
    private TextView tvAvatarInitials;

    /** Frame container for the avatar UI, serving as a navigation link to Profile. */
    private View flAvatar;
    
    /** Dynamic container shown only when a live lesson is detected in one of the user's classes. */
    private View llLiveLessonShortcut;

    /** Details for the currently active live lesson alert. */
    private TextView tvLiveLessonTitle, tvLiveLessonClass;

    /** Interaction button to jump directly into the live sheet music session. */
    private MaterialButton btnJoinLiveLesson;

    /** Real-time listeners for live lesson status across all user classes. */
    private final List<ListenerRegistration> liveLessonListeners = new ArrayList<>();
    
    /** Tracking variables for a single active live lesson and its class. */
    private Lesson activeLiveLesson = null;
    private Class activeLiveClass = null;
    
    /** Flag to prevent registering new listeners when the activity is not active. */
    private boolean isPaused = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        initViews();
        // Step 1: Trigger the user profile sync chain.
        firestoreManager.getUser(this); 
    }

    @Override
    protected void onResume() {
        super.onResume();
        isPaused = false;
        // Reactive UI Sync: Ensure cached user data from DataManager is reflected 
        // after returning from the ProfileActivity.
        User user = DataManager.getUserInstance();
        if (user != null) {
            String welcomeMsg = "Welcome, " + user.getFname() + " " + user.getLname();
            tvWelcomeMessage.setText(welcomeMsg);
            tvUserTypeStatus.setText("User Type: " + user.getUserType());
            updateAvatar(user);
            startListeningForLiveLessons(user);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        isPaused = true;
        stopListeningForLiveLessons();
    }

    /**
     * Binds UI components to their XML identifiers and registers interaction listeners.
     */
    private void initViews() {
        btnLogout = findViewById(R.id.btnLogout);
        tvWelcomeMessage = findViewById(R.id.tvWelcomeMessage);
        tvUserTypeStatus = findViewById(R.id.tvUserTypeStatus);
        tvClassesCount = findViewById(R.id.tvClassesCount);
        tvStudentsCount = findViewById(R.id.tvStudentsCount);
        cardClassesCount = findViewById(R.id.cardClassesCount);
        cardStudentsCount = findViewById(R.id.cardStudentsCount);
        ivDashboardProfilePicture = findViewById(R.id.ivDashboardProfilePicture);
        tvAvatarInitials = findViewById(R.id.tvAvatarInitials);
        flAvatar = findViewById(R.id.flAvatar);
        llLiveLessonShortcut = findViewById(R.id.llLiveLessonShortcut);
        tvLiveLessonTitle = findViewById(R.id.tvLiveLessonTitle);
        tvLiveLessonClass = findViewById(R.id.tvLiveLessonClass);
        btnJoinLiveLesson = findViewById(R.id.btnJoinLiveLesson);

        btnLogout.setOnClickListener(v -> logout());
        flAvatar.setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.dashboard_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_my_classes) {
            startActivity(new Intent(this, ClassListActivity.class));
            return true;
        } else if (itemId == R.id.menu_todo) {
            Toast.makeText(this, "TODO clicked", Toast.LENGTH_SHORT).show();
            return true;
        } else if (itemId == R.id.menu_recent_lessons) {
            Toast.makeText(this, "Recent Lessons clicked", Toast.LENGTH_SHORT).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Callback triggered when the user's profile is retrieved from Firestore.
     * Initiates the cascading dashboard population (Stats and Live Alerts).
     */
    @Override
    public void onUserFetched(User user) {
        if (user != null) {
            DataManager.setUser(user); // Cache globally.
            String welcomeMsg = "Welcome, " + user.getFname() + " " + user.getLname();
            tvWelcomeMessage.setText(welcomeMsg);
            tvUserTypeStatus.setText("User Type: " + user.getUserType());
            
            updateAvatar(user);
            updateStats(user);
            startListeningForLiveLessons(user);
        }
    }

    /**
     * Handles the complex rendering logic for the user's avatar.
     * Decodes Base64 data if available, otherwise generates a high-contrast initial-based icon.
     */
    private void updateAvatar(User user) {
        String initials = "";
        if (user.getFname() != null && !user.getFname().isEmpty()) {
            initials += user.getFname().substring(0, 1).toUpperCase();
        }
        if (user.getLname() != null && !user.getLname().isEmpty()) {
            initials += user.getLname().substring(0, 1).toUpperCase();
        }
        tvAvatarInitials.setText(initials);

        String base64Image = user.getProfilePictureBase64();
        if (base64Image != null && !base64Image.isEmpty()) {
            try {
                // Binary Processing: Decode the String representation into a native Android Bitmap.
                byte[] decodedString = Base64.decode(base64Image, Base64.DEFAULT);
                Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                ivDashboardProfilePicture.setImageBitmap(decodedByte);
                ivDashboardProfilePicture.setVisibility(View.VISIBLE);
                tvAvatarInitials.setVisibility(View.GONE);
            } catch (Exception e) {
                Log.e(TAG, "Error decoding base64 image", e);
                ivDashboardProfilePicture.setVisibility(View.GONE);
                tvAvatarInitials.setVisibility(View.VISIBLE);
            }
        } else {
            ivDashboardProfilePicture.setVisibility(View.GONE);
            tvAvatarInitials.setVisibility(View.VISIBLE);
        }
    }

    private void updateStats(User user) {
        firestoreManager.getClassesForUser(user.getEmail(), classes -> {
            if (isPaused) return;
            if (classes != null) {
                tvClassesCount.setText(String.valueOf(classes.size()));
                
                if ("teacher".equals(user.getUserType())) {
                    cardStudentsCount.setVisibility(View.VISIBLE);
                    // Reset classes card weight if it was changed
                    LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) cardClassesCount.getLayoutParams();
                    params.weight = 1;
                    params.rightMargin = getResources().getDimensionPixelSize(R.dimen.spacing_xs);
                    cardClassesCount.setLayoutParams(params);

                    int totalStudents = 0;
                    // Aggregate Logic: Sum the membership sizes of all classrooms.
                    for (Class cls : classes) {
                        if (cls.getMembers() != null) {
                            // Filter Rule: Subtract the teacher (owner) if they are in the list.
                            totalStudents += cls.getMembers().size() - 1;
                        }
                    }
                    tvStudentsCount.setText(String.valueOf(Math.max(0, totalStudents)));
                } else {
                    cardStudentsCount.setVisibility(View.GONE);
                    // Make classes card full width for students
                    LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) cardClassesCount.getLayoutParams();
                    params.weight = 2; // Take full weightSum
                    params.rightMargin = 0;
                    cardClassesCount.setLayoutParams(params);
                }
            }
        });
    }

    /**
     * Implements a parallel monitoring strategy for Live Lessons.
     * Iterates through every classroom the user belongs to and registers a real-time 
     * listener for any lesson with 'isLive == true'.
     */
    private void startListeningForLiveLessons(User user) {
        stopListeningForLiveLessons(); // Clean up existing listeners if any
        
        firestoreManager.getClassesForUser(user.getEmail(), classes -> {
            if (isPaused) return;
            if (classes != null) {
                for (Class cls : classes) {
                    // Logic: Register a scoped snapshot listener for each classroom.
                    ListenerRegistration registration = firestoreManager.listenForLiveLesson(cls.getId(), lesson -> {
                        if (isPaused) return;
                        if (lesson != null) {
                            activeLiveLesson = lesson;
                            activeLiveClass = cls;
                        } else if (activeLiveClass != null && activeLiveClass.getId().equals(cls.getId())) {
                            // The current active lesson has ended.
                            activeLiveLesson = null;
                            activeLiveClass = null;
                        }
                        updateLiveLessonUI();
                    });
                    liveLessonListeners.add(registration);
                }
            }
        });
    }

    /**
     * Terminates all active Firestore snapshot listeners and clears the tracked lesson data.
     */
    private void stopListeningForLiveLessons() {
        for (ListenerRegistration registration : liveLessonListeners) {
            registration.remove();
        }
        liveLessonListeners.clear();
        activeLiveLesson = null;
        activeLiveClass = null;
        updateLiveLessonUI();
    }

    /**
     * Synchronizes the Live Lesson shortcut UI with the current state of active lessons.
     */
    private void updateLiveLessonUI() {
        if (activeLiveLesson == null || activeLiveClass == null) {
            llLiveLessonShortcut.setVisibility(View.GONE);
        } else {
            showLiveLessonShortcut(activeLiveClass, activeLiveLesson);
        }
    }

    /**
     * Configures the dynamic alert UI for a discovered Live Lesson.
     * Provides a direct deep-link into the class's live tab.
     */
    private void showLiveLessonShortcut(Class cls, Lesson lesson) {
        tvLiveLessonTitle.setText(lesson.getTitle());
        tvLiveLessonClass.setText(cls.getName());
        llLiveLessonShortcut.setVisibility(View.VISIBLE);

        btnJoinLiveLesson.setOnClickListener(v -> {
            // Context Transition: Populate the global cache before navigating.
            DataManager.setCurClass(cls);
            DataManager.setCurLesson(lesson);
            Intent intent = new Intent(this, ClassActivity.class);
            // 2 is the index of LiveFragment in the hub's ViewPager.
            intent.putExtra("target_tab", 2); 
            startActivity(intent);
        });
    }

    private void logout() {
        FirebaseComm.signOut();
        finish();
    }
}