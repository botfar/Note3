package com.example.todo;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.widget.SearchView;
import androidx.appcompat.app.AlertDialog;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Locale;

public class NotesActivity extends BaseActivity {

    ListView listView;
    Button btnAdd;

    ArrayList<String> noteTitles = new ArrayList<>();
    ArrayList<String> allTitles = new ArrayList<>();
    ArrayList<String> noteIds = new ArrayList<>();

    ArrayAdapter<String> adapter;

    FirebaseUser user;
    FirebaseFirestore db;

    private static final int REQUEST_ADD_NOTE = 1;

    // 🔥 Current sort field (default = time)
    private String currentSortField = "timestamp";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notes);

        setupDrawer(); // from BaseActivity: sets toolbar, drawer, nav listener

        listView = findViewById(R.id.listViewNotes);
        btnAdd = findViewById(R.id.btnAddNote);

        adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                noteTitles
        );

        listView.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        btnAdd.setOnClickListener(v ->
                startActivityForResult(
                        new Intent(this, AddNoteActivity.class),
                        REQUEST_ADD_NOTE
                )
        );

        listView.setOnItemClickListener((p, v, pos, id) -> {
            Intent intent = new Intent(this, AddNoteActivity.class);
            String docId = noteIds.get(pos);

            db.collection("users")
                    .document(user.getUid())
                    .collection("notes")
                    .document(docId)
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            intent.putExtra("noteId", docId);
                            intent.putExtra("noteText", doc.getString("text"));
                            intent.putExtra("noteTitle", doc.getString("title"));
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

    private void loadNotes() {
        Query.Direction direction =
                currentSortField.equals("timestamp")
                        ? Query.Direction.DESCENDING
                        : Query.Direction.ASCENDING;

        db.collection("users")
                .document(user.getUid())
                .collection("notes")
                .orderBy(currentSortField, direction)
                .get()
                .addOnSuccessListener(result -> {
                    noteTitles.clear();
                    allTitles.clear();
                    noteIds.clear();

                    for (QueryDocumentSnapshot doc : result) {
                        String title = doc.getString("title");
                        String dateTime = doc.getString("dateTime");

                        if (title == null || title.trim().isEmpty()) {
                            title = "Untitled Note";
                        }
                        if (dateTime == null) {
                            dateTime = "";
                        }

                        String displayText = title + "\n" + dateTime;
                        noteTitles.add(displayText);
                        allTitles.add(displayText);
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
        // Use BaseActivity's menu handling and add search functionality here
        super.onCreateOptionsMenu(menu);
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.action_sort_time) {
            currentSortField = "timestamp";
            loadNotes();
            Toast.makeText(this, "Sorted by time", Toast.LENGTH_SHORT).show();
            return true;
        }

        if (id == R.id.action_sort_title) {
            currentSortField = "title";
            loadNotes();
            Toast.makeText(this, "Sorted by title", Toast.LENGTH_SHORT).show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void filterNotes(String text) {

        noteTitles.clear();

        if (text == null || text.isEmpty()) {
            noteTitles.addAll(allTitles);
        } else {
            String lower = text.toLowerCase(Locale.getDefault());
            for (String item : allTitles) {
                if (item.toLowerCase(Locale.getDefault()).contains(lower)) {
                    noteTitles.add(item);
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
                                Toast.makeText(this,
                                        "Delete failed",
                                        Toast.LENGTH_SHORT).show();
                                loadNotes();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}