package com.example.flip;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class TypeRaceMenuActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_type_race_menu);

        Button mathButton = findViewById(R.id.mathQuizButton);
        Button geographyButton = findViewById(R.id.geographyQuizButton);
        Button scienceButton = findViewById(R.id.scienceQuizButton);

        mathButton.setOnClickListener(v -> startQuiz("MATH"));
        geographyButton.setOnClickListener(v -> startQuiz("GEOGRAPHY"));
        scienceButton.setOnClickListener(v -> startQuiz("SCIENCE"));
    }

    private void startQuiz(String quizType) {
        Intent intent = new Intent(this, TypeRaceActivity.class);
        intent.putExtra("QUIZ_TYPE", quizType);
        startActivity(intent);
    }
}