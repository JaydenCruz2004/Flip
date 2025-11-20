package com.example.flip;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.flip.model.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText emailET;
    private TextInputEditText passwordET;
    private MaterialButton signInButton;

    FirebaseDatabase database;
    FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Match IDs from XML
        emailET = findViewById(R.id.emailET);
        passwordET = findViewById(R.id.passwordET);
        signInButton = findViewById(R.id.signInButton);

        // Login button
        signInButton.setOnClickListener(loginListener);

        // Create Account link
        findViewById(R.id.createAccountTV).setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, SignUpActivity.class));
        });

        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();

        // Window insets: use correct root ID
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private final View.OnClickListener loginListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            final String username = emailET.getText().toString().trim();
            final String password = passwordET.getText().toString().trim();

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(LoginActivity.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            // Firebase Authentication
            auth.signInWithEmailAndPassword(username, password)
                    .addOnCompleteListener(LoginActivity.this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {

                            if (task.isSuccessful()) {

                                DatabaseReference refUsers = database.getReference("users");

                                refUsers.orderByChild("email")
                                        .equalTo(username)
                                        .limitToFirst(1)
                                        .addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot snapshot) {


                                                Intent gotoMain = new Intent(LoginActivity.this, MainActivity.class);

                                                if (snapshot.exists()) {
                                                    User current = snapshot.getChildren()
                                                            .iterator().next()
                                                            .getValue(User.class);

                                                    gotoMain.putExtra(User.E_KEY, current.getEmail());
                                                    gotoMain.putExtra(User.P_KEY, current.getPass());
                                                } else {
                                                    gotoMain.putExtra(User.E_KEY, username);
                                                }

                                                startActivity(gotoMain);
                                            }

                                            @Override
                                            public void onCancelled(@NonNull DatabaseError error) {
                                                Toast.makeText(LoginActivity.this,
                                                        "DB Error: " + error.getMessage(),
                                                        Toast.LENGTH_SHORT).show();
                                            }
                                        });

                            } else {
                                Toast.makeText(LoginActivity.this,
                                        "Login failed: " + task.getException().getMessage(),
                                        Toast.LENGTH_LONG).show();
                            }
                        }
                    });
        }
    };
}
