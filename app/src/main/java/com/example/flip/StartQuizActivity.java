package com.example.flip;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;

public class StartQuizActivity extends AppCompatActivity {
    private EditText notesInput;
    private Button generateButton;
    private ProgressBar progressBar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz_start);

        notesInput = findViewById(R.id.notesInput);
        generateButton = findViewById(R.id.generateButton);
        progressBar = findViewById(R.id.progressBar);


        };

}
