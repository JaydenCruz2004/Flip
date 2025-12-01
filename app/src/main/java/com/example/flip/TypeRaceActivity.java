package com.example.flip;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class TypeRaceActivity extends AppCompatActivity {

    private TextView questionText, questionNumberText, scoreText, feedbackText, timerText;
    private EditText answerInput;
    private Button submitButton;

    private List<QuizQuestion> questions;
    private int currentQuestionIndex = 0;
    private int totalScore = 0;
    private long questionStartTime;
    private String quizType;

    // Timing constants
    private static final int MAX_POINTS = 1000;
    private static final int OPTIMAL_TIME_MS = 2000; // 2 seconds for max points
    private static final int MAX_TIME_MS = 15000; // 15 seconds before points hit zero

    private Handler timerHandler = new Handler();
    private Runnable timerRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_type_race);

        // Get quiz type from intent
        quizType = getIntent().getStringExtra("QUIZ_TYPE");

        // Initialize views
        questionText = findViewById(R.id.questionText);
        questionNumberText = findViewById(R.id.questionNumber);
        scoreText = findViewById(R.id.scoreText);
        feedbackText = findViewById(R.id.feedbackText);
        timerText = findViewById(R.id.timerText);
        answerInput = findViewById(R.id.answerInput);
        submitButton = findViewById(R.id.submitButton);

        // Load questions based on quiz type
        loadQuestions(quizType);

        // Set up submit button
        submitButton.setOnClickListener(v -> checkAnswer());

        // Allow Enter key to submit
        answerInput.setOnEditorActionListener((v, actionId, event) -> {
            checkAnswer();
            return true;
        });

        // Start first question
        displayQuestion();
    }

    private void loadQuestions(String type) {
        questions = new ArrayList<>();

        switch (type) {
            case "MATH":
                questions.add(new QuizQuestion("What is 7 × 8?", "56"));
                questions.add(new QuizQuestion("What is 15 + 27?", "42"));
                questions.add(new QuizQuestion("What is 100 - 37?", "63"));
                questions.add(new QuizQuestion("What is 144 ÷ 12?", "12"));
                questions.add(new QuizQuestion("What is 9²?", "81"));
                questions.add(new QuizQuestion("What is 25 × 4?", "100"));
                questions.add(new QuizQuestion("What is 50% of 200?", "100"));
                questions.add(new QuizQuestion("What is 3³?", "27"));
                questions.add(new QuizQuestion("What is 13 + 29?", "42"));
                questions.add(new QuizQuestion("What is 72 ÷ 8?", "9"));
                break;

            case "GEOGRAPHY":
                questions.add(new QuizQuestion("Capital of France?", "Paris"));
                questions.add(new QuizQuestion("Capital of Japan?", "Tokyo"));
                questions.add(new QuizQuestion("Largest country by area?", "Russia"));
                questions.add(new QuizQuestion("Capital of Australia?", "Canberra"));
                questions.add(new QuizQuestion("Longest river in the world?", "Nile"));
                questions.add(new QuizQuestion("Capital of Canada?", "Ottawa"));
                questions.add(new QuizQuestion("Largest ocean?", "Pacific"));
                questions.add(new QuizQuestion("Capital of Italy?", "Rome"));
                questions.add(new QuizQuestion("Tallest mountain?", "Everest"));
                questions.add(new QuizQuestion("Capital of Spain?", "Madrid"));
                break;

            case "SCIENCE":
                questions.add(new QuizQuestion("Chemical symbol for water?", "H2O"));
                questions.add(new QuizQuestion("Planet closest to the sun?", "Mercury"));
                questions.add(new QuizQuestion("Speed of light in m/s? (3×10^8)", "300000000"));
                questions.add(new QuizQuestion("Number of bones in human body?", "206"));
                questions.add(new QuizQuestion("Chemical symbol for gold?", "Au"));
                questions.add(new QuizQuestion("Largest planet in solar system?", "Jupiter"));
                questions.add(new QuizQuestion("Powerhouse of the cell?", "Mitochondria"));
                questions.add(new QuizQuestion("Boiling point of water (°C)?", "100"));
                questions.add(new QuizQuestion("Chemical symbol for oxygen?", "O2"));
                questions.add(new QuizQuestion("How many planets in solar system?", "8"));
                break;
        }
    }

    private void displayQuestion() {
        if (currentQuestionIndex >= questions.size()) {
            showFinalScore();
            return;
        }

        QuizQuestion question = questions.get(currentQuestionIndex);

        questionText.setText(question.getQuestion());
        questionNumberText.setText("Question " + (currentQuestionIndex + 1) + "/" + questions.size());
        scoreText.setText("Score: " + totalScore);

        answerInput.setText("");
        answerInput.setEnabled(true);
        answerInput.requestFocus();

        feedbackText.setVisibility(View.INVISIBLE);

        // Start timing
        questionStartTime = System.currentTimeMillis();
        startTimer();
    }

    private void startTimer() {
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                long elapsedTime = System.currentTimeMillis() - questionStartTime;
                double seconds = elapsedTime / 1000.0;
                timerText.setText(String.format("%.1fs", seconds));

                // Update timer color based on time
                if (elapsedTime < OPTIMAL_TIME_MS) {
                    timerText.setTextColor(Color.parseColor("#4CAF50")); // Green
                } else if (elapsedTime < MAX_TIME_MS / 2) {
                    timerText.setTextColor(Color.parseColor("#FFC107")); // Yellow
                } else {
                    timerText.setTextColor(Color.parseColor("#F44336")); // Red
                }

                timerHandler.postDelayed(this, 100);
            }
        };
        timerHandler.post(timerRunnable);
    }

    private void stopTimer() {
        timerHandler.removeCallbacks(timerRunnable);
    }

    private void checkAnswer() {
        String userAnswer = answerInput.getText().toString().trim();

        if (userAnswer.isEmpty()) {
            Toast.makeText(this, "Please enter an answer", Toast.LENGTH_SHORT).show();
            return;
        }

        stopTimer();
        answerInput.setEnabled(false);

        long timeTaken = System.currentTimeMillis() - questionStartTime;
        QuizQuestion question = questions.get(currentQuestionIndex);

        boolean isCorrect = isAnswerCorrect(userAnswer, question.getAnswer());

        if (isCorrect) {
            int pointsEarned = calculatePoints(timeTaken);
            totalScore += pointsEarned;
            showFeedback(true, pointsEarned);
        } else {
            showFeedback(false, 0);
        }
    }

    private boolean isAnswerCorrect(String userAnswer, String correctAnswer) {
        // Normalize answers (lowercase, remove extra spaces)
        String normalizedUser = userAnswer.toLowerCase().replaceAll("\\s+", "");
        String normalizedCorrect = correctAnswer.toLowerCase().replaceAll("\\s+", "");

        // Exact match
        if (normalizedUser.equals(normalizedCorrect)) {
            return true;
        }

        // Check for close spelling (Levenshtein distance)
        int distance = levenshteinDistance(normalizedUser, normalizedCorrect);
        int threshold = Math.max(1, correctAnswer.length() / 5); // Allow 20% error

        return distance <= threshold;
    }

    private int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];

        for (int i = 0; i <= s1.length(); i++) {
            dp[i][0] = i;
        }

        for (int j = 0; j <= s2.length(); j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = 1 + Math.min(dp[i - 1][j - 1],
                            Math.min(dp[i - 1][j], dp[i][j - 1]));
                }
            }
        }

        return dp[s1.length()][s2.length()];
    }

    private int calculatePoints(long timeTakenMs) {
        if (timeTakenMs <= OPTIMAL_TIME_MS) {
            return MAX_POINTS;
        }

        if (timeTakenMs >= MAX_TIME_MS) {
            return 50; // Minimum points for correct answer
        }

        // Linear depreciation from OPTIMAL_TIME to MAX_TIME
        double ratio = (double)(MAX_TIME_MS - timeTakenMs) / (MAX_TIME_MS - OPTIMAL_TIME_MS);
        int points = (int)(50 + (MAX_POINTS - 50) * ratio);

        return Math.max(50, Math.min(MAX_POINTS, points));
    }

    private void showFeedback(boolean correct, int points) {
        feedbackText.setVisibility(View.VISIBLE);

        if (correct) {
            feedbackText.setText("✓ CORRECT! +" + points + " pts");
            feedbackText.setTextColor(Color.parseColor("#4CAF50")); // Green
        } else {
            QuizQuestion question = questions.get(currentQuestionIndex);
            feedbackText.setText("✗ INCORRECT\nCorrect: " + question.getAnswer());
            feedbackText.setTextColor(Color.parseColor("#F44336")); // Red
        }

        // Fade in animation
        AlphaAnimation fadeIn = new AlphaAnimation(0.0f, 1.0f);
        fadeIn.setDuration(200);
        feedbackText.startAnimation(fadeIn);

        // Move to next question after delay
        new Handler().postDelayed(() -> {
            currentQuestionIndex++;
            displayQuestion();
        }, 1500);
    }

    private void showFinalScore() {
        questionText.setText("Quiz Complete!");
        questionNumberText.setText("");
        answerInput.setVisibility(View.GONE);
        submitButton.setVisibility(View.GONE);
        timerText.setVisibility(View.GONE);

        int maxPossibleScore = questions.size() * MAX_POINTS;
        double percentage = (totalScore * 100.0) / maxPossibleScore;

        feedbackText.setVisibility(View.VISIBLE);
        feedbackText.setText(String.format("Final Score: %d / %d\n(%.1f%%)\n\nTap anywhere to return",
                totalScore, maxPossibleScore, percentage));
        feedbackText.setTextColor(Color.parseColor("#D594F2"));

        findViewById(android.R.id.content).setOnClickListener(v -> finish());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopTimer();
    }

    // Inner class for questions
    private static class QuizQuestion {
        private String question;
        private String answer;

        public QuizQuestion(String question, String answer) {
            this.question = question;
            this.answer = answer;
        }

        public String getQuestion() {
            return question;
        }

        public String getAnswer() {
            return answer;
        }
    }
}