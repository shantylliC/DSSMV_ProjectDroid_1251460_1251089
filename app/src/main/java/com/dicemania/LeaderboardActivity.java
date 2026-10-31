package com.dicemania;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

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

        ArrayList<String> rows = new ArrayList<>();

        if (nicknames != null && scores != null) {
            int count = Math.min(10, Math.min(nicknames.size(), scores.length));

            for (int i = 0; i < count; i++) {
                rows.add((i + 1) + ".  " + nicknames.get(i)
                        + " — " + scores[i] + " pontos");
            }
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this, android.R.layout.simple_list_item_1, rows) {
            @Override
            public View getView(int position, View convertView,
                                android.view.ViewGroup parent) {
                TextView row = (TextView) super.getView(position, convertView, parent);
                row.setTextColor(getColor(R.color.castanho));
                row.setTextSize(18);
                row.setPadding(16, 20, 16, 20);
                return row;
            }
        };

        leaderboardList.setAdapter(adapter);
    }
}