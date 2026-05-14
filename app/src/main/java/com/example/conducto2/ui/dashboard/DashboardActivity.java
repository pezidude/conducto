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
import java.util.List;

public class DashboardActivity extends BaseDrawerActivity implements com.example.conducto2.data.firebase.FirestoreManager.UserFetchListener {

    private static final String TAG = "DashboardActivity";

    MaterialButton btnLogout;
    TextView tvWelcomeMessage, tvUserTypeStatus;
    
    private TextView tvClassesCount, tvStudentsCount;
    private ImageView ivDashboardProfilePicture;
    private TextView tvAvatarInitials;
    private View flAvatar;
    
    // Live Lesson Shortcut Views
    private View llLiveLessonShortcut;
    private TextView tvLiveLessonTitle, tvLiveLessonClass;
    private MaterialButton btnJoinLiveLesson;
    
    // firestoreManager is inherited from BaseDrawerActivity

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        // Toolbar toolbar = findViewById(R.id.toolbar);
        // setSupportActionBar(toolbar);

        // firestoreManager is initialized in BaseDrawerActivity.onCreate

        initViews();
        firestoreManager.getUser(this); // pass the job to onUserFetched
    }

    private void initViews() {
        btnLogout = findViewById(R.id.btnLogout);
        tvWelcomeMessage = findViewById(R.id.tvWelcomeMessage);
        tvUserTypeStatus = findViewById(R.id.tvUserTypeStatus);
        
        tvClassesCount = findViewById(R.id.tvClassesCount);
        tvStudentsCount = findViewById(R.id.tvStudentsCount);

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
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.dashboard_menu, menu);
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

    @Override
    public void onUserFetched(User user) {
        //setupUserSpecificElements
        if (user != null) {
            DataManager.setUser(user);
            String welcomeMsg = "Welcome, " + user.getFname() + " " + user.getLname();
            tvWelcomeMessage.setText(welcomeMsg);
            tvUserTypeStatus.setText("User Type: " + user.getUserType());
            
            updateAvatar(user);
            updateStats(user);
            checkForLiveLessons(user);
        }
    }

    private void updateAvatar(User user) {
        // Set initials
        String initials = "";
        if (user.getFname() != null && !user.getFname().isEmpty()) {
            initials += user.getFname().substring(0, 1).toUpperCase();
        }
        if (user.getLname() != null && !user.getLname().isEmpty()) {
            initials += user.getLname().substring(0, 1).toUpperCase();
        }
        tvAvatarInitials.setText(initials);

        // Load profile picture if exists
        String base64Image = user.getProfilePictureBase64();
        if (base64Image != null && !base64Image.isEmpty()) {
            try {
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
            if (classes != null) {
                tvClassesCount.setText(String.valueOf(classes.size()));
                
                if ("teacher".equals(user.getUserType())) {
                    int totalStudents = 0;
                    for (Class cls : classes) {
                        if (cls.getMembers() != null) {
                            // Subtract 1 if the teacher is also in the members list
                            totalStudents += cls.getMembers().size() - 1;
                        }
                    }
                    tvStudentsCount.setText(String.valueOf(Math.max(0, totalStudents)));
                } else {
                    // For students, maybe show total number of fellow students? 
                    // Or just hide/change this stat.
                    findViewById(R.id.tvStudentsCount).getParent().requestLayout(); 
                }
            }
        });
    }

    private void checkForLiveLessons(User user) {
        firestoreManager.getClassesForUser(user.getEmail(), classes -> {
            if (classes != null) {
                for (Class cls : classes) {
                    firestoreManager.listenForLiveLesson(cls.getId(), lesson -> {
                        if (lesson != null) {
                            showLiveLessonShortcut(cls, lesson);
                        }
                    });
                }
            }
        });
    }

    private void showLiveLessonShortcut(Class cls, Lesson lesson) {
        tvLiveLessonTitle.setText(lesson.getTitle());
        tvLiveLessonClass.setText(cls.getName());
        llLiveLessonShortcut.setVisibility(View.VISIBLE);

        btnJoinLiveLesson.setOnClickListener(v -> {
            DataManager.setCurClass(cls);
            DataManager.setCurLesson(lesson);
            Intent intent = new Intent(this, ClassActivity.class);
            intent.putExtra("target_tab", 2); // 2 is for LiveFragment
            startActivity(intent);
        });
    }

    private void logout() {
        FirebaseComm.signOut();
        finish();
    }
}