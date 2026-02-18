package com.example.todo;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class NotesActivity extends AppCompatActivity {

    DrawerLayout drawerLayout;
    NavigationView navigationView;
    MaterialToolbar toolbar;

    ListView listView;
    Button btnAdd;

    ArrayList<String> notes = new ArrayList<>();
    ArrayList<String> allNotes = new ArrayList<>();
    ArrayList<String> noteIds = new ArrayList<>();
    ArrayAdapter<String> adapter;

    FirebaseUser user;
    FirebaseFirestore db;

    private static final int REQUEST_ADD_NOTE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notes);

        // Drawer setup
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.navigation_view);
        toolbar = findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);

        toolbar.setNavigationOnClickListener(v ->
                drawerLayout.openDrawer(GravityCompat.START)
        );

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                startActivity(new Intent(this, MainActivity.class));
            }

            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

        // Notes UI
        listView = findViewById(R.id.listViewNotes);
        btnAdd = findViewById(R.id.btnAddNote);

        adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1,
                notes);
        listView.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // Open AddNoteActivity to add a new note
        btnAdd.setOnClickListener(v ->
                startActivityForResult(new Intent(this, AddNoteActivity.class), REQUEST_ADD_NOTE)
        );

        // Edit note: opens AddNoteActivity with noteId and text
        listView.setOnItemClickListener((p, v, pos, id) -> {
            Intent intent = new Intent(this, AddNoteActivity.class);
            intent.putExtra("noteId", noteIds.get(pos));
            intent.putExtra("noteText", notes.get(pos));
            startActivityForResult(intent, REQUEST_ADD_NOTE);
        });

        // Long click to delete
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

    // Refresh notes after returning from AddNoteActivity
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ADD_NOTE && resultCode == RESULT_OK) {
            loadNotes();
        }
    }

    // 🔹 Load notes from Firestore
    private void loadNotes() {
        db.collection("users")
                .document(user.getUid())
                .collection("notes")
                .orderBy("timestamp")
                .get()
                .addOnSuccessListener(result -> {

                    notes.clear();
                    allNotes.clear();
                    noteIds.clear();

                    for (QueryDocumentSnapshot doc : result) {
                        String text = doc.getString("text");

                        notes.add(text);
                        allNotes.add(text);
                        noteIds.add(doc.getId());
                    }

                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Failed to load notes",
                                Toast.LENGTH_SHORT).show()
                );
    }

    // 🔍 Toolbar Search
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_notes, menu);

        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();

        searchView.setQueryHint("Search notes...");

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterNotes(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterNotes(newText);
                return true;
            }
        });

        return true;
    }

    // 🔹 Filter notes locally
    private void filterNotes(String text) {
        notes.clear();

        if (text == null || text.isEmpty()) {
            notes.addAll(allNotes);
        } else {
            String lower = text.toLowerCase(Locale.getDefault());
            for (String note : allNotes) {
                if (note.toLowerCase(Locale.getDefault()).contains(lower)) {
                    notes.add(note);
                }
            }
        }

        adapter.notifyDataSetChanged();
    }

    // 🔹 Delete dialog (optimistic UI)
    private void showDeleteDialog(int position) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Note")
                .setMessage("Delete this note?")
                .setPositiveButton("Yes", (d, w) -> {
                    String docId = noteIds.get(position); // save before removal

                    // Remove immediately from UI
                    notes.remove(position);
                    allNotes.remove(position);
                    noteIds.remove(position);
                    adapter.notifyDataSetChanged();

                    // Delete from Firestore
                    db.collection("users")
                            .document(user.getUid())
                            .collection("notes")
                            .document(docId)
                            .delete()
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Delete failed", Toast.LENGTH_SHORT).show();
                                loadNotes(); // revert if failed
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
