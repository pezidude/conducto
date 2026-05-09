package com.example.conducto2.ui.dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.conducto2.R;
import com.example.conducto2.data.firebase.FirebaseComm;
import com.example.conducto2.data.manager.DataManager;
import com.example.conducto2.data.model.User;
import com.example.conducto2.ui.BaseDrawerActivity;
import com.example.conducto2.ui.classes.ClassActivity;
import com.example.conducto2.ui.classes.ClassListActivity;
import com.google.android.material.button.MaterialButton;

import com.example.conducto2.data.model.Class;
import com.example.conducto2.data.model.Lesson;
import com.example.conducto2.ui.player.SMPlayerActivity;
import com.example.conducto2.ui.lessons.LessonDetailsActivity;
import android.view.View;
import android.widget.ImageView;
import java.util.List;

public class DashboardActivity extends BaseDrawerActivity implements com.example.conducto2.data.firebase.FirestoreManager.UserFetchListener {

    MaterialButton btnLogout;
    TextView tvWelcomeMessage, tvUserTypeStatus;
    
    private TextView tvClassesCount, tvStudentsCount;
    
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

        llLiveLessonShortcut = findViewById(R.id.llLiveLessonShortcut);
        tvLiveLessonTitle = findViewById(R.id.tvLiveLessonTitle);
        tvLiveLessonClass = findViewById(R.id.tvLiveLessonClass);
        btnJoinLiveLesson = findViewById(R.id.btnJoinLiveLesson);

        btnLogout.setOnClickListener(v -> logout());
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
            
            updateStats(user);
            checkForLiveLessons(user);
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