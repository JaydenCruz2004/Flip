package com.example.flip;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.fragment.NavHostFragment;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class StartQuizActivity extends AppCompatActivity {
    private EditText notesInput;
    private Button generateButton;
    private Button backButton;
    private ProgressBar progressBar;
    private String API_KEY;

    private OkHttpClient client = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz_start);

        notesInput = findViewById(R.id.notesInput);
        generateButton = findViewById(R.id.generateButton);
        backButton = findViewById(R.id.backButton);
        progressBar = findViewById(R.id.progressBar);
        API_KEY = getString(R.string.api_key);


        backButton.setOnClickListener(v -> finish());

        generateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String notes = notesInput.getText().toString().trim();
                if (notes.isEmpty()) {
                    Toast.makeText(StartQuizActivity.this, "Please enter your notes", Toast.LENGTH_SHORT).show();
                    return;
                }
                generateQuiz(notes);
            }
        });
    }

    private void generateQuiz(String notes) {
        progressBar.setVisibility(View.VISIBLE);
        generateButton.setEnabled(false);

        // get this error Can not extract resource from com.android.aaptcompiler.ParsedResource@72e66fdb. when i try to
        // make the prompt a string resource Can not extract resource from com.android.aaptcompiler.ParsedResource@70aafed5.
        String prompt = "Convert the following notes into exactly 5 quiz questions. Format each question EXACTLY as shown below. Do not add any introduction, explanation, or extra text. Just output the questions in this exact format:\n\n" +
                "Question: [question text]\n" +
                "a. [option a]\n" +
                "b. [option b]\n" +
                "c. [option c]\n" +
                "d. [option d]\n" +
                "Correct: [a/b/c/d]\n\n" +
                "Separate each question with a blank line. Do not number the questions. Start directly with 'Question:'\n\n" +
                "Notes:\n" + notes;

        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("model", "gpt-4o-mini");

            JSONArray messages = new JSONArray();
            JSONObject message = new JSONObject();
            message.put("role", "user");
            message.put("content", prompt);
            messages.put(message);

            jsonBody.put("messages", messages);
            jsonBody.put("max_tokens", 1000);
            jsonBody.put("temperature", 0.7);

            RequestBody body = RequestBody.create(
                    jsonBody.toString(),
                    MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                    .url("https://api.openai.com/v1/chat/completions")
                    .addHeader("Authorization", "Bearer " + API_KEY)
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressBar.setVisibility(View.GONE);
                            generateButton.setEnabled(true);
                            Toast.makeText(StartQuizActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    final String responseBody = response.body().string();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressBar.setVisibility(View.GONE);
                            generateButton.setEnabled(true);

                            try {
                                JSONObject jsonResponse = new JSONObject(responseBody);

                                // Check for API errors
                                if (jsonResponse.has("error")) {
                                    String errorMsg = jsonResponse.getJSONObject("error").getString("message");
                                    Toast.makeText(StartQuizActivity.this, "API Error: " + errorMsg, Toast.LENGTH_LONG).show();
                                    return;
                                }

                                String quizText = jsonResponse.getJSONArray("choices")
                                        .getJSONObject(0)
                                        .getJSONObject("message")
                                        .getString("content");

                                // Debug: Show the response
                                android.util.Log.d("QuizApp", "AI Response: " + quizText);

                                Intent intent = new Intent(StartQuizActivity.this, QuizActivity.class);
                                intent.putExtra("quizText", quizText);
                                startActivity(intent);

                            } catch (JSONException e) {
                                Toast.makeText(StartQuizActivity.this, "Error parsing response: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                android.util.Log.e("QuizApp", "Parse error", e);
                                android.util.Log.e("QuizApp", "Response: " + responseBody);
                            }
                        }
                    });
                }
            });

        } catch (JSONException e) {
            progressBar.setVisibility(View.GONE);
            generateButton.setEnabled(true);
            Toast.makeText(this, "Error creating request", Toast.LENGTH_SHORT).show();
        }
    }
}