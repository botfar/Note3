package com.example.todo;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
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

    EditText editTextTitle;
    EditText editTextNote;
    Button btnSave, btnGoBack;

    FirebaseUser user;
    FirebaseFirestore db;

    String noteId = null; // null = new note, otherwise editing existing

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_note);

        // Drawer setup
        drawerLayout = findViewById(R.id.drawer_layout_add);
        navigationView = findViewById(R.id.navigation_view_add);
        toolbar = findViewById(R.id.toolbar_add);

        toolbar.setNavigationOnClickListener(v ->
                drawerLayout.openDrawer(GravityCompat.START)
        );

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_notes || id == R.id.nav_home) {
                finish(); // no warning, just close
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

        // UI elements
        editTextTitle = findViewById(R.id.editTextTitle);
        editTextNote = findViewById(R.id.editTextNote);
        btnSave = findViewById(R.id.btnSaveNote);
        btnGoBack = findViewById(R.id.btnGoBack);

        db = FirebaseFirestore.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) finish();

        // Load existing note if editing
        Intent intent = getIntent();
        if (intent.hasExtra("noteId")) {
            noteId = intent.getStringExtra("noteId");
            String noteTitle = intent.getStringExtra("noteTitle");
            String noteText = intent.getStringExtra("noteText");

            editTextTitle.setText(noteTitle);
            editTextNote.setText(noteText);
        }

        btnSave.setOnClickListener(v -> saveNote());

        btnGoBack.setOnClickListener(v -> finish()); // go back immediately, no warning
    }

    private void saveNote() {
        String title = editTextTitle.getText().toString().trim();
        String text = editTextNote.getText().toString().trim();

        if (TextUtils.isEmpty(title)) {
            title = "Untitled Note";
        }
        if (TextUtils.isEmpty(text)) {
            Toast.makeText(this, "Note cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> note = new HashMap<>();
        note.put("title", title);
        note.put("text", text);
        note.put("timestamp", System.currentTimeMillis());

        if (noteId == null) {
            // New note
            db.collection("users")
                    .document(user.getUid())
                    .collection("notes")
                    .add(note)
                    .addOnSuccessListener(d -> {
                        Toast.makeText(this, "Note saved", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Save failed", Toast.LENGTH_SHORT).show()
                    );
        } else {
            // Edit existing note
            db.collection("users")
                    .document(user.getUid())
                    .collection("notes")
                    .document(noteId)
                    .update(note)
                    .addOnSuccessListener(a -> {
                        Toast.makeText(this, "Note updated", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Update failed", Toast.LENGTH_SHORT).show()
                    );
        }
    }


}