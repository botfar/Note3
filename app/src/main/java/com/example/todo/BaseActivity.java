package com.example.todo;

import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class BaseActivity extends AppCompatActivity {

    protected DrawerLayout drawerLayout;
    protected NavigationView navigationView;
    protected MaterialToolbar toolbar;

    /**
     * Call this method in child activities AFTER setContentView()
     * to initialize the drawer, toolbar, and menu
     */
    protected void setupDrawer() {
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.navigation_view);
        toolbar = findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);

        // Hamburger menu click opens drawer
        toolbar.setNavigationOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        // Show/hide menu items based on user login status
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        Menu navMenu = navigationView.getMenu();

        navMenu.findItem(R.id.nav_login).setVisible(currentUser == null);
        navMenu.findItem(R.id.nav_signup).setVisible(currentUser == null);
        navMenu.findItem(R.id.nav_logout).setVisible(currentUser != null);

        // Handle navigation clicks
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                startActivity(new Intent(this, MainActivity.class));
            } else if (id == R.id.nav_notes) {
                startActivity(new Intent(this, NotesActivity.class));
            } else if (id == R.id.nav_login) {
                startActivity(new Intent(this, LoginActivity.class));
            } else if (id == R.id.nav_signup) {
                startActivity(new Intent(this, SignupActivity.class));
            } else if (id == R.id.nav_logout) {
                FirebaseAuth.getInstance().signOut();
                Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show();

                // Refresh menu items after logout
                navMenu.findItem(R.id.nav_login).setVisible(true);
                navMenu.findItem(R.id.nav_signup).setVisible(true);
                navMenu.findItem(R.id.nav_logout).setVisible(false);

                // Optional: redirect to Home after logout
                startActivity(new Intent(this, MainActivity.class));
                finish();
            }

            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
    }
}