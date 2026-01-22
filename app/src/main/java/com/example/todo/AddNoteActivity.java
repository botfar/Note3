package com.example.todo;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AddNoteActivity extends AppCompatActivity {

    DrawerLayout drawerLayout;
    NavigationView navigationView;
    MaterialToolbar toolbar;

    EditText editTextNote;
    Button btnSave;

    FirebaseUser user;
    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_note);

        drawerLayout = findViewById(R.id.drawer_layout_add);
        navigationView = findViewById(R.id.navigation_view_add);
        toolbar = findViewById(R.id.toolbar_add);

        toolbar.setNavigationOnClickListener(v ->
                drawerLayout.openDrawer(GravityCompat.START)
        );

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_notes) {
                finish(); // 🔥 GO BACK
            }
            else if (id == R.id.nav_home) {
                startActivity(new Intent(this, MainActivity.class));
            }

            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

        editTextNote = findViewById(R.id.editTextNote);
        btnSave = findViewById(R.id.btnSaveNote);

        db = FirebaseFirestore.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            finish();
            return;
        }

        btnSave.setOnClickListener(v -> saveNote());
    }

    private void saveNote() {
        String text = editTextNote.getText().toString().trim();

        if (text.isEmpty()) {
            Toast.makeText(this, "Note cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> note = new HashMap<>();
        note.put("text", text);
        note.put("timestamp", System.currentTimeMillis());

        db.collection("users")
                .document(user.getUid())
                .collection("notes")
                .add(note)
                .addOnSuccessListener(d -> {
                    Toast.makeText(this, "Note saved", Toast.LENGTH_SHORT).show();
                    finish(); // ✅ RETURNS TO NotesActivity
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Save failed", Toast.LENGTH_SHORT).show()
                );
    }
}
