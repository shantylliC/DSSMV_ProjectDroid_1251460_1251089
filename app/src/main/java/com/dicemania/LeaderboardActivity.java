package com.dicemania;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.content.res.ColorStateList;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.dicemania.domain.model.Player;

import java.util.ArrayList;
import java.util.List;

public class LeaderboardActivity extends AppCompatActivity {

    public static final String EXTRA_NICKNAMES = "leaderboard_nicknames";
    public static final String EXTRA_SCORES = "leaderboard_scores";

    public static Intent createIntent(Context context, List<Player> topPlayers) {
        ArrayList<String> nicknames = new ArrayList<>();
        int count = Math.min(topPlayers.size(), 10);
        int[] scores = new int[count];

        for (int i = 0; i < count; i++) {
            Player player = topPlayers.get(i);
            nicknames.add(player.getNickname());
            scores[i] = player.getPlayerScore();
        }

        Intent intent = new Intent(context, LeaderboardActivity.class);
        intent.putStringArrayListExtra(EXTRA_NICKNAMES, nicknames);
        intent.putExtra(EXTRA_SCORES, scores);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leaderboard);

        ListView leaderboardList = findViewById(R.id.leaderboardList);
        TextView emptyMessage = findViewById(R.id.emptyMessage);
        leaderboardList.setEmptyView(emptyMessage);

        ArrayList<String> nicknames =
                getIntent().getStringArrayListExtra(EXTRA_NICKNAMES);
        int[] scores = getIntent().getIntArrayExtra(EXTRA_SCORES);

        ArrayList<Player> players = new ArrayList<>();

        if (nicknames != null && scores != null) {
            int count = Math.min(10, Math.min(nicknames.size(), scores.length));

            for (int index = 0; index < count; index++) {
                players.add(new Player(
                        String.valueOf(index + 1),
                        nicknames.get(index),
                        scores[index]
                ));
            }
        }

        ArrayAdapter<Player> adapter = new ArrayAdapter<Player>(
                this,
                0,
                players
        ) {
            @Override
            public View getView(
                    int position,
                    View convertView,
                    ViewGroup parent
            ) {
                View row = convertView;

                if (row == null) {
                    row = getLayoutInflater().inflate(
                            R.layout.item_leaderboard,
                            parent,
                            false
                    );
                }

                ImageView trophy = row.findViewById(R.id.leaderboardTrophy);
                TextView rank = row.findViewById(R.id.leaderboardRank);
                TextView nickname = row.findViewById(R.id.leaderboardNickname);
                TextView score = row.findViewById(R.id.leaderboardScore);

                Player player = getItem(position);

                nickname.setText(player.getNickname());
                score.setText(getString(
                        R.string.leaderboard_score,
                        player.getPlayerScore()
                ));
                rank.setVisibility(View.VISIBLE);
                rank.setText(String.valueOf(position + 1));

                if (position < 3) {
                    int medalColor;

                    if (position == 0) {
                        medalColor = R.color.gold3;
                    } else if (position == 1) {
                        medalColor = R.color.silver;
                    } else {
                        medalColor = R.color.bronze;
                    }

                    trophy.setVisibility(View.VISIBLE);
                    trophy.setContentDescription(getString(
                            R.string.leaderboard_trophy_description,
                            position + 1
                    ));
                    trophy.setImageTintList(
                            ColorStateList.valueOf(getColor(medalColor))
                    );
                } else {
                    // Reserva o espaço do troféu para alinhar todos os nomes.
                    trophy.setVisibility(View.INVISIBLE);
                }

                return row;
            }
        };

        leaderboardList.setAdapter(adapter);
    }
}
