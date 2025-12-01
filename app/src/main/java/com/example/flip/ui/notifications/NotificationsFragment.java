package com.example.flip.ui.notifications;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.example.flip.MemoryMatchSetupActivity;
import com.example.flip.QuizActivity;
import com.example.flip.R;
import com.example.flip.StartQuizActivity;
import com.example.flip.TypeRaceMenuActivity;

public class NotificationsFragment extends Fragment {

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_games, container, false);


        CardView typeRaceCard = root.findViewById(R.id.typeRaceCard);
        typeRaceCard.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), TypeRaceMenuActivity.class);
            startActivity(intent);
        });


        CardView memoryMatchCard = root.findViewById(R.id.memoryMatchCard);
        memoryMatchCard.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), MemoryMatchSetupActivity.class);
            startActivity(intent);
        });


        CardView quizGeneratorCard = root.findViewById(R.id.quizGeneratorCard);
        quizGeneratorCard.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), StartQuizActivity.class);
            startActivity(intent);
        });

        return root;
    }
}