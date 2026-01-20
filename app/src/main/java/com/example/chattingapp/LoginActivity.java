package com.example.chattingapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.chattingapp.utils.SupabaseAuthHelper;
import com.example.chattingapp.utils.UserManager;

public class LoginActivity extends AppCompatActivity {

    private EditText editName;
    private com.google.android.material.textfield.TextInputLayout inputLayout;
    private Button btnContinue;
    private ProgressBar progressBar;
    private UserManager userManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        userManager = new UserManager(this);

        // If user is already logged in, go to Main Activity directly
        if (userManager.isUserLoggedIn()) {
            navigateToMain();
            return;
        }

        editName = findViewById(R.id.edit_name);
        inputLayout = findViewById(R.id.input_layout);
        btnContinue = findViewById(R.id.btn_continue);
        progressBar = findViewById(R.id.progressBar);

        btnContinue.setOnClickListener(v -> handleLogin());
    }

    private void handleLogin() {
        String name = editName.getText().toString().trim();
        inputLayout.setError(null); // Clear previous error

        if (name.isEmpty()) {
            inputLayout.setError("Name is required");
            return;
        }

        if (name.length() > 30) {
            inputLayout.setError("Name cannot exceed 30 characters");
            return;
        }

        // Check for symbols/numbers (letters and spaces only)
        if (!name.matches("^[a-zA-Z\\s]+$")) {
            inputLayout.setError("Name must contain only letters");
            return;
        }

        // Check for spam (repeating characters 4 or more times, e.g., "ssss")
        if (hasRepeatingChars(name, 4)) {
            inputLayout.setError("Please enter a valid name");
            return;
        }

        setLoading(true);

        // Call Kotlin Helper ensure init
        SupabaseClientHelper.init();

        SupabaseAuthHelper.INSTANCE.signInAndSaveProfile(name, new SupabaseAuthHelper.AuthCallback() {
            @Override
            public void onSuccess(String userId) {
                setLoading(false);
                // Save locally
                userManager.saveUser(userId, name);
                // Go to App
                navigateToMain();
            }

            @Override
            public void onError(String message) {
                setLoading(false);
                Toast.makeText(LoginActivity.this, "Error: " + message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setLoading(boolean isLoading) {
        if (isLoading) {
            progressBar.setVisibility(View.VISIBLE);
            btnContinue.setEnabled(false);
            btnContinue.setText("Setting up...");
        } else {
            progressBar.setVisibility(View.GONE);
            btnContinue.setEnabled(true);
            btnContinue.setText("Start Chatting");
        }
    }

    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish(); // Prevent going back to login
    }

    private boolean hasRepeatingChars(String str, int limit) {
        if (str == null || str.length() < limit) return false;
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("(.)\\1{" + (limit - 1) + ",}");
        return p.matcher(str).find();
    }
}
