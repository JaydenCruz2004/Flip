package com.example.flip;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QuizActivity extends AppCompatActivity {

    private TextView questionText, questionNumber, scoreText;
    private RadioGroup optionsGroup;
    private RadioButton optionA, optionB, optionC, optionD;
    private Button submitButton, nextButton, backButton;

    private List<Question> questions;
    private int currentQuestion = 0;
    private int score = 0;

    // Firebase
    private FirebaseAuth auth;
    private FirebaseDatabase db;
    private String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);

        // Firebase init
        auth = FirebaseAuth.getInstance();
        db = FirebaseDatabase.getInstance();
        uid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;

        questionText = findViewById(R.id.questionText);
        questionNumber = findViewById(R.id.questionNumber);
        scoreText = findViewById(R.id.scoreText);
        optionsGroup = findViewById(R.id.optionsGroup);
        optionA = findViewById(R.id.optionA);
        optionB = findViewById(R.id.optionB);
        optionC = findViewById(R.id.optionC);
        optionD = findViewById(R.id.optionD);
        submitButton = findViewById(R.id.submitButton);
        nextButton = findViewById(R.id.nextButton);
        backButton = findViewById(R.id.backButton);
        backButton.setVisibility(View.INVISIBLE);

        String quizText = getIntent().getStringExtra("quizText");
        questions = parseQuestions(quizText);

        if (questions.isEmpty()) {
            Toast.makeText(this, "Error loading quiz", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadQuestion();

        backButton.setOnClickListener(v -> finish());

        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkAnswer();
            }
        });

        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentQuestion++;
                if (currentQuestion < questions.size()) {
                    loadQuestion();
                    nextButton.setVisibility(View.GONE);
                    submitButton.setEnabled(true);
                    optionsGroup.clearCheck();
                } else {
                    showFinalScore();
                }
            }
        });
    }

    private List<Question> parseQuestions(String quizText) {
        List<Question> questionList = new ArrayList<>();


        String[] blocks = quizText.split("(?i)Question:");

        for (int i = 0; i < blocks.length; i++) {
            String block = blocks[i].trim();
            if (block.isEmpty()) continue;

            try {
                String[] lines = block.split("\n");
                String q = "";
                String a = "", b = "", c = "", d = "";
                String correct = "";

                for (String line : lines) {
                    line = line.trim();
                    if (line.isEmpty()) continue;

                    // Extract question text (first non-option line)
                    if (!line.matches("^[a-d]\\..*") &&
                            !line.toLowerCase().startsWith("correct") &&
                            q.isEmpty()) {
                        q = line;
                    }
                    // Extract options
                    else if (line.matches("^a\\..*")) {
                        a = line.substring(2).trim();
                    } else if (line.matches("^b\\..*")) {
                        b = line.substring(2).trim();
                    } else if (line.matches("^c\\..*")) {
                        c = line.substring(2).trim();
                    } else if (line.matches("^d\\..*")) {
                        d = line.substring(2).trim();
                    }
                    // Extract correct answer
                    else if (line.toLowerCase().startsWith("correct")) {
                        // Extract just the letter (a, b, c, or d)
                        correct = line.toLowerCase()
                                .replaceAll("correct:?\\s*", "")
                                .replaceAll("[^a-d]", "")
                                .trim();
                        if (correct.length() > 0) {
                            correct = String.valueOf(correct.charAt(0));
                        }
                    }
                }

                // check for all required parts
                if (!q.isEmpty() && !a.isEmpty() && !b.isEmpty() &&
                        !c.isEmpty() && !d.isEmpty() && !correct.isEmpty()) {
                    questionList.add(new Question(q, a, b, c, d, correct));
                }
            } catch (Exception ignored) {}
        }
        return questionList;

    }


    private void loadQuestion() {
        Question q = questions.get(currentQuestion);
        questionNumber.setText("Question " + (currentQuestion + 1) + "/" + questions.size());
        questionText.setText(q.question);
        optionA.setText("a. " + q.optionA);
        optionB.setText("b. " + q.optionB);
        optionC.setText("c. " + q.optionC);
        optionD.setText("d. " + q.optionD);
        scoreText.setText("Score: " + score + "/" + questions.size());
    }

    private void checkAnswer() {
        int selectedId = optionsGroup.getCheckedRadioButtonId();
        if (selectedId == -1) {
            Toast.makeText(this, "Please select an answer", Toast.LENGTH_SHORT).show();
            return;
        }

        String userAnswer = "";
        if (selectedId == R.id.optionA) userAnswer = "a";
        else if (selectedId == R.id.optionB) userAnswer = "b";
        else if (selectedId == R.id.optionC) userAnswer = "c";
        else if (selectedId == R.id.optionD) userAnswer = "d";

        Question q = questions.get(currentQuestion);
        if (userAnswer.equals(q.correctAnswer)) {
            score++;
            Toast.makeText(this, "Correct!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Wrong! Correct answer: " + q.correctAnswer, Toast.LENGTH_SHORT).show();
        }

        scoreText.setText("Score: " + score + "/" + questions.size());
        submitButton.setEnabled(false);
        nextButton.setVisibility(View.VISIBLE);
    }

    private void showFinalScore() {
        questionText.setText("Quiz Complete!");
        optionsGroup.setVisibility(View.GONE);
        submitButton.setVisibility(View.GONE);
        nextButton.setVisibility(View.GONE);

        double percentage = (score * 100.0) / questions.size();
        questionNumber.setText("Final Score: " + score + "/" + questions.size() +
                " (" + percentage + "%)");

        backButton.setVisibility(View.VISIBLE);

        updateUserScoreInFirebase((int) percentage);
    }

    // Implement Firebase score update
    private void updateUserScoreInFirebase(int percentageScore) {
        if (uid == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        int earnedPoints = percentageScore / 10;

        DatabaseReference ref = db.getReference("users").child(uid);

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                Integer oldPoints = snapshot.child("points").getValue(Integer.class);
                Integer games = snapshot.child("gamesPlayed").getValue(Integer.class);

                int newPoints = (oldPoints != null ? oldPoints : 0) + earnedPoints;
                int newGames = (games != null ? games : 0) + 1;

                Map<String, Object> update = new HashMap<>();
                update.put("points", newPoints);
                update.put("gamesPlayed", newGames);

                ref.updateChildren(update).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(QuizActivity.this,
                                "Saved! +" + earnedPoints + " points",
                                Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(QuizActivity.this,
                                "Error saving score", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(QuizActivity.this,
                        "Database error: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private static class Question {
        String question, optionA, optionB, optionC, optionD, correctAnswer;

        Question(String q, String a, String b, String c, String d, String correct) {
            this.question = q;
            this.optionA = a;
            this.optionB = b;
            this.optionC = c;
            this.optionD = d;
            this.correctAnswer = correct;
        }
    }
}
