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
import java.util.Locale;

public class NotesActivity extends AppCompatActivity {

    DrawerLayout drawerLayout;
    NavigationView navigationView;
    MaterialToolbar toolbar;

    ListView listView;
    Button btnAdd;

    ArrayList<String> noteTitles = new ArrayList<>(); // Displayed titles
    ArrayList<String> allTitles = new ArrayList<>();  // For search
    ArrayList<String> noteIds = new ArrayList<>();
    ArrayAdapter<String> adapter;

    FirebaseUser user;
    FirebaseFirestore db;

    private static final int REQUEST_ADD_NOTE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notes);

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

        listView = findViewById(R.id.listViewNotes);
        btnAdd = findViewById(R.id.btnAddNote);

        adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1,
                noteTitles);
        listView.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        btnAdd.setOnClickListener(v ->
                startActivityForResult(new Intent(this, AddNoteActivity.class), REQUEST_ADD_NOTE)
        );

        listView.setOnItemClickListener((p, v, pos, id) -> {
            Intent intent = new Intent(this, AddNoteActivity.class);
            String docId = noteIds.get(pos);

            db.collection("users").document(user.getUid())
                    .collection("notes").document(docId)
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            String text = doc.getString("text");
                            String title = doc.getString("title");
                            intent.putExtra("noteId", docId);
                            intent.putExtra("noteText", text);
                            intent.putExtra("noteTitle", title);
                            startActivityForResult(intent, REQUEST_ADD_NOTE);
                        }
                    });
        });

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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ADD_NOTE && resultCode == RESULT_OK) {
            loadNotes();
        }
    }

    private void loadNotes() {
        db.collection("users")
                .document(user.getUid())
                .collection("notes")
                .orderBy("timestamp")
                .get()
                .addOnSuccessListener(result -> {
                    noteTitles.clear();
                    allTitles.clear();
                    noteIds.clear();

                    for (QueryDocumentSnapshot doc : result) {
                        String title = doc.getString("title");
                        if (title == null || title.trim().isEmpty()) {
                            title = "Untitled Note";
                        }

                        noteTitles.add(title);
                        allTitles.add(title);
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

    private void filterNotes(String text) {
        noteTitles.clear();

        if (text == null || text.isEmpty()) {
            noteTitles.addAll(allTitles);
        } else {
            String lower = text.toLowerCase(Locale.getDefault());
            for (String title : allTitles) {
                if (title.toLowerCase(Locale.getDefault()).contains(lower)) {
                    noteTitles.add(title);
                }
            }
        }

        adapter.notifyDataSetChanged();
    }

    private void showDeleteDialog(int position) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Note")
                .setMessage("Delete this note?")
                .setPositiveButton("Yes", (d, w) -> {
                    String docId = noteIds.get(position);

                    noteTitles.remove(position);
                    allTitles.remove(position);
                    noteIds.remove(position);
                    adapter.notifyDataSetChanged();

                    db.collection("users")
                            .document(user.getUid())
                            .collection("notes")
                            .document(docId)
                            .delete()
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Delete failed", Toast.LENGTH_SHORT).show();
                                loadNotes();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}