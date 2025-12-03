package com.example.flip;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;

public class MemoryMatchSetupActivity extends AppCompatActivity {

    private LinearLayout pairsContainer;
    private Button addPairButton, startGameButton, backButton;
    private ArrayList<String> terms = new ArrayList<>();
    private ArrayList<String> definitions = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_memory_match_setup);

        pairsContainer = findViewById(R.id.pairsContainer);
        addPairButton = findViewById(R.id.addPairButton);
        startGameButton = findViewById(R.id.startGameButton);
        backButton = findViewById(R.id.backButton);

        backButton.setOnClickListener(v -> finish());


        for (int i = 0; i < 4; i++) {
            addPairInputFields();
        }

        addPairButton.setOnClickListener(v -> {
            if (pairsContainer.getChildCount() < 8) {
                addPairInputFields();
            } else {
                Toast.makeText(this, "Maximum 8 pairs allowed", Toast.LENGTH_SHORT).show();
            }
        });

        startGameButton.setOnClickListener(v -> startGame());
    }

    private void addPairInputFields() {
        View pairView = LayoutInflater.from(this)
                .inflate(R.layout.item_memory_pair_input, pairsContainer, false);

        pairsContainer.addView(pairView);
    }

    private void startGame() {
        terms.clear();
        definitions.clear();
        for (int i = 0; i < pairsContainer.getChildCount(); i++) {
            View pairView = pairsContainer.getChildAt(i);
            EditText termInput = pairView.findViewById(R.id.termInput);
            EditText definitionInput = pairView.findViewById(R.id.definitionInput);

            String term = termInput.getText().toString().trim();
            String definition = definitionInput.getText().toString().trim();

            if (!term.isEmpty() && !definition.isEmpty()) {
                terms.add(term);
                definitions.add(definition);
            }
        }

        if (terms.size() < 4) {
            Toast.makeText(this, "Please enter at least 4 pairs", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, MemoryMatchGameActivity.class);
        intent.putStringArrayListExtra("TERMS", terms);
        intent.putStringArrayListExtra("DEFINITIONS", definitions);
        startActivity(intent);
    }
}