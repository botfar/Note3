package com.example.todo;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AddNoteActivity extends BaseActivity {

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

        // Initialize drawer & toolbar from BaseActivity
        setupDrawer();

        // UI elements
        editTextTitle = findViewById(R.id.editTextTitle);
        editTextNote = findViewById(R.id.editTextNote);
        btnSave = findViewById(R.id.btnSaveNote);
        btnGoBack = findViewById(R.id.btnGoBack);

        db = FirebaseFirestore.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) finish(); // ensure user is logged in

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
        btnGoBack.setOnClickListener(v -> finish());
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

        // Get current date and time
        Date now = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        String formattedDate = sdf.format(now);

        Map<String, Object> note = new HashMap<>();
        note.put("title", title);
        note.put("text", text);
        note.put("timestamp", System.currentTimeMillis()); // for sorting
        note.put("dateTime", formattedDate);               // readable date & hour

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