package com.example.campuseventdiscoverysystem.activities;

import android.os.Bundle;
import android.widget.TextView;

import com.example.campuseventdiscoverysystem.R;

/**
 * Shows only the societies the current user follows.
 * Reuses SocietiesActivity entirely — just flips followOnly = true.
 */
public class MySocietiesActivity extends SocietiesActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        followOnly = true; // must be set before super.onCreate calls loadFollowsThenDisplay
        super.onCreate(savedInstanceState);

        // Update header title
        TextView tvHeader = null;
        // Find the header TextView — it's the second child of the header row
        android.widget.LinearLayout header = findViewById(R.id.headerRow);
        if (header != null) {
            for (int i = 0; i < header.getChildCount(); i++) {
                android.view.View v = header.getChildAt(i);
                if (v instanceof TextView) {
                    ((TextView) v).setText("My Societies");
                    break;
                }
            }
        }
    }
}