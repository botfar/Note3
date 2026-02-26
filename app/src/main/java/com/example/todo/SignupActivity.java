package com.example.todo;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;

public class SignupActivity extends BaseActivity {

    FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        // Setup drawer/menu from BaseActivity
        setupDrawer();

        // Firebase
        auth = FirebaseAuth.getInstance();

        // Sign up logic
        EditText nameEditText = findViewById(R.id.nameEditText);
        EditText emailEditText = findViewById(R.id.emailEditText);
        EditText passwordEditText = findViewById(R.id.passwordEditText);
        EditText confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText);
        Button signupButton = findViewById(R.id.signupButton);
        TextView loginRedirectText = findViewById(R.id.loginRedirectText);

        signupButton.setOnClickListener(v -> {

            String name = nameEditText.getText().toString().trim();
            String email = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();
            String confirm = confirmPasswordEditText.getText().toString().trim();

            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!password.equals(confirm)) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(this, "Sign up successful", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(SignupActivity.this, LoginActivity.class));
                            finish();
                        } else {
                            Toast.makeText(this,
                                    "Sign up failed: " + task.getException().getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    });
        });

        // Redirect to login
        loginRedirectText.setOnClickListener(v ->
                startActivity(new Intent(SignupActivity.this, LoginActivity.class))
        );
    }
}