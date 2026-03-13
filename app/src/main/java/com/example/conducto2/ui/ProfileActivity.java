package com.example.conducto2.ui;

import android.os.Bundle;
import com.example.conducto2.R;

/**
 * This activity displays the user's profile information.
 * It extends {@link BaseDrawerActivity} to include the navigation drawer.
 */
public class ProfileActivity extends BaseDrawerActivity {

    /**
     * Initializes the activity.
     * Sets up the content view from the layout resource {@code R.layout.activity_profile}.
     * @param savedInstanceState If the activity is being re-initialized after previously being shut down then this Bundle contains the data it most recently supplied in onSaveInstanceState(Bundle).
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
    }
}