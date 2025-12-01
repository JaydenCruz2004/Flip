package com.example.flip;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MemoryMatchGameActivity extends AppCompatActivity {

    private GridLayout gameGrid;
    private TextView movesText, pairsFoundText, timerText;

    private List<MemoryCard> cards = new ArrayList<>();
    private MemoryCard firstCard = null;
    private MemoryCard secondCard = null;
    private boolean isProcessing = false;

    private int moves = 0;
    private int pairsFound = 0;
    private int totalPairs = 0;
    private long startTime;

    private Handler timerHandler = new Handler();
    private Runnable timerRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_memory_match_game);

        gameGrid = findViewById(R.id.gameGrid);
        movesText = findViewById(R.id.movesText);
        pairsFoundText = findViewById(R.id.pairsFoundText);
        timerText = findViewById(R.id.timerText);


        ArrayList<String> terms = getIntent().getStringArrayListExtra("TERMS");
        ArrayList<String> definitions = getIntent().getStringArrayListExtra("DEFINITIONS");

        if (terms == null || definitions == null || terms.size() != definitions.size()) {
            Toast.makeText(this, "Error loading game data", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        totalPairs = terms.size();
        setupGame(terms, definitions);
        startTimer();
    }

    private void setupGame(ArrayList<String> terms, ArrayList<String> definitions) {

        for (int i = 0; i < terms.size(); i++) {
            cards.add(new MemoryCard(terms.get(i), i, true));
            cards.add(new MemoryCard(definitions.get(i), i, false));
        }


        Collections.shuffle(cards);

        int columns = (cards.size() <= 8) ? 2 : (cards.size() <= 12) ? 3 : 4;
        gameGrid.setColumnCount(columns);

        for (int i = 0; i < cards.size(); i++) {
            final MemoryCard card = cards.get(i);
            final int position = i;

            View cardContainer = getLayoutInflater().inflate(R.layout.item_memory_card, gameGrid, false);
            CardView cardView = (CardView) cardContainer;
            TextView cardText = cardView.findViewById(R.id.cardText);

            card.cardView = cardView;
            card.textView = cardText;

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = GridLayout.LayoutParams.WRAP_CONTENT;
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.setMargins(8, 8, 8, 8);
            cardView.setLayoutParams(params);

            cardView.setOnClickListener(v -> onCardClicked(card));
            gameGrid.addView(cardView);
        }

        updateStats();
    }

    private void onCardClicked(MemoryCard card) {
        if (isProcessing || card.isMatched || card.isFlipped) {
            return;
        }

        flipCard(card, true);

        if (firstCard == null) {
            firstCard = card;
        } else if (secondCard == null) {
            secondCard = card;
            moves++;
            updateStats();
            checkForMatch();
        }
    }

    private void flipCard(MemoryCard card, boolean faceUp) {
        card.isFlipped = faceUp;
        if (faceUp) {
            card.textView.setText(card.content);
            card.cardView.setCardBackgroundColor(Color.parseColor("#2D2C58"));
        } else {
            card.textView.setText("?");
            card.cardView.setCardBackgroundColor(Color.parseColor("#55458F"));
        }
    }

    private void checkForMatch() {
        isProcessing = true;

        if (firstCard.pairId == secondCard.pairId) {
            new Handler().postDelayed(() -> {
                firstCard.isMatched = true;
                secondCard.isMatched = true;

                firstCard.cardView.setCardBackgroundColor(Color.parseColor("#4CAF50"));
                secondCard.cardView.setCardBackgroundColor(Color.parseColor("#4CAF50"));

                pairsFound++;
                updateStats();
                resetSelection();
                if (pairsFound == totalPairs) {
                    gameComplete();
                }
            }, 500);
        } else {
            new Handler().postDelayed(() -> {
                flipCard(firstCard, false);
                flipCard(secondCard, false);
                resetSelection();
            }, 1000);
        }
    }

    private void resetSelection() {
        firstCard = null;
        secondCard = null;
        isProcessing = false;
    }

    private void updateStats() {
        movesText.setText("Moves: " + moves);
        pairsFoundText.setText("Pairs: " + pairsFound + "/" + totalPairs);
    }

    private void startTimer() {
        startTime = System.currentTimeMillis();
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                long elapsed = System.currentTimeMillis() - startTime;
                int seconds = (int) (elapsed / 1000);
                int minutes = seconds / 60;
                seconds = seconds % 60;

                timerText.setText(String.format("%02d:%02d", minutes, seconds));
                timerHandler.postDelayed(this, 1000);
            }
        };
        timerHandler.post(timerRunnable);
    }

    private void gameComplete() {
        timerHandler.removeCallbacks(timerRunnable);

        String time = timerText.getText().toString();
        String message = String.format("🎉 Congratulations!\n\nTime: %s\nMoves: %d\n\nTap anywhere to return",
                time, moves);

        Toast.makeText(this, message, Toast.LENGTH_LONG).show();

        new Handler().postDelayed(() -> {
            findViewById(android.R.id.content).setOnClickListener(v -> finish());
        }, 2000);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timerHandler != null) {
            timerHandler.removeCallbacks(timerRunnable);
        }
    }

    private static class MemoryCard {
        String content;
        int pairId;
        boolean isTerm;
        boolean isFlipped = false;
        boolean isMatched = false;
        CardView cardView;
        TextView textView;

        MemoryCard(String content, int pairId, boolean isTerm) {
            this.content = content;
            this.pairId = pairId;
            this.isTerm = isTerm;
        }
    }
}