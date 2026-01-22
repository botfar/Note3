package com.example.todo;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class NotesActivity extends AppCompatActivity {

    DrawerLayout drawerLayout;
    NavigationView navigationView;
    MaterialToolbar toolbar;

    ListView listView;
    Button btnAdd;

    ArrayList<String> notes = new ArrayList<>();
    ArrayList<String> noteIds = new ArrayList<>();
    ArrayAdapter<String> adapter;

    FirebaseUser user;
    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notes);

        // Drawer setup
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.navigation_view);
        toolbar = findViewById(R.id.toolbar);

        toolbar.setNavigationOnClickListener(v ->
                drawerLayout.openDrawer(GravityCompat.START)
        );

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                startActivity(new Intent(this, MainActivity.class));
            }
            // nav_notes → already here, do nothing

            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

        // Notes UI
        listView = findViewById(R.id.listViewNotes);
        btnAdd = findViewById(R.id.btnAddNote);

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, notes);
        listView.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        btnAdd.setOnClickListener(v ->
                startActivity(new Intent(this, AddNoteActivity.class))
        );

        listView.setOnItemClickListener((p, v, pos, id) ->
                showEditDialog(pos)
        );

        listView.setOnItemLongClickListener((p, v, pos, id) -> {
            showDeleteDialog(pos);
            return true;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadNotes();
    }

    private void loadNotes() {
        db.collection("users")
                .document(user.getUid())
                .collection("notes")
                .orderBy("timestamp")
                .get()
                .addOnSuccessListener(result -> {
                    notes.clear();
                    noteIds.clear();
                    for (QueryDocumentSnapshot doc : result) {
                        notes.add(doc.getString("text"));
                        noteIds.add(doc.getId());
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load notes", Toast.LENGTH_SHORT).show()
                );
    }

    private void showDeleteDialog(int position) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Note")
                .setMessage("Delete this note?")
                .setPositiveButton("Yes", (d, w) ->
                        db.collection("users")
                                .document(user.getUid())
                                .collection("notes")
                                .document(noteIds.get(position))
                                .delete()
                                .addOnSuccessListener(a -> loadNotes())
                )
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showEditDialog(int position) {
        String docId = noteIds.get(position);

        android.widget.EditText input = new android.widget.EditText(this);
        input.setText(notes.get(position));

        new AlertDialog.Builder(this)
                .setTitle("Edit Note")
                .setView(input)
                .setPositiveButton("Save", (d, w) -> {
                    Map<String, Object> update = new HashMap<>();
                    update.put("text", input.getText().toString());

                    db.collection("users")
                            .document(user.getUid())
                            .collection("notes")
                            .document(docId)
                            .update(update)
                            .addOnSuccessListener(a -> loadNotes());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
