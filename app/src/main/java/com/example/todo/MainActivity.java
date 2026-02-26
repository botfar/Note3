package com.example.todo;

import android.content.Intent;
import android.os.Bundle;

public class MainActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize drawer & toolbar from BaseActivity
        setupDrawer();

        // Optional: your other UI setup goes here
    }
}