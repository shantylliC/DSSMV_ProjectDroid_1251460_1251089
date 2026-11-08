package com.dicemania;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.material.switchmaterial.SwitchMaterial;

import androidx.appcompat.app.AppCompatActivity;

import com.dicemania.data.local.SharedPreferencePlayerRepository;
import com.dicemania.data.local.PlayerStatsRepository;
import com.dicemania.data.sound.GameFeedbackEffects;

import com.dicemania.domain.model.Player;
import com.google.android.material.bottomsheet.BottomSheetDialog;

public class HomeActivity extends AppCompatActivity {

    private Player player;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Recupera o jogador guardado quando fez login.
        SharedPreferencePlayerRepository playerRepository = new SharedPreferencePlayerRepository(this);
        player = playerRepository.getLocalPlayer();

        // Se não houver jogador guardado, regressa ao ecrã de login.
        if (player == null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }


        Button letsPlayButton = findViewById(R.id.letsPlayButton);
        ImageButton settingsButton = findViewById(R.id.settingsButton);

        // Começa uma partida para o jogador atual.
        letsPlayButton.setOnClickListener(view -> startGame());

        // Abre o painel com o perfil e os recordes.
        settingsButton.setOnClickListener(view -> showSettings());
    }

    // Inicia o jogo e envia a identidade do jogador para a GameActivity.
    private void startGame() {
        Intent intent = new Intent(this, GameActivity.class);
        intent.putExtra("player_id", player.getId());
        intent.putExtra("player_nickname", player.getNickname());
        startActivity(intent);
    }

    // Apresenta o painel inferior das definições.
    private void showSettings() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View panel = getLayoutInflater().inflate(
                R.layout.bottom_sheet_settings,
                null
        );

        TextView playerName = panel.findViewById(R.id.settingsPlayerName);
        TextView bestStreak = panel.findViewById(R.id.settingsBestStreak);
        TextView bestScore = panel.findViewById(R.id.settingsBestScore);
        SwitchMaterial soundSwitch = panel.findViewById(R.id.settingsSoundSwitch);
        SwitchMaterial vibrationSwitch =
                panel.findViewById(R.id.settingsVibrationSwitch);
        Button leaderboardButton =
                panel.findViewById(R.id.weeklyLeaderboardButton);

        // Mostra as opções guardadas sem gravar alterações durante a inicialização.
        soundSwitch.setChecked(GameFeedbackEffects.isSoundEnabled(this));
        vibrationSwitch.setChecked(GameFeedbackEffects.isVibrationEnabled(this));

        // Guarda cada opção imediatamente para ser aplicada na próxima partida.
        soundSwitch.setOnCheckedChangeListener((buttonView, isChecked) ->
                GameFeedbackEffects.setSoundEnabled(this, isChecked)
        );
        vibrationSwitch.setOnCheckedChangeListener((buttonView, isChecked) ->
                GameFeedbackEffects.setVibrationEnabled(this, isChecked)
        );

        playerName.setText(getString(
                R.string.nickname_value,
                player.getNickname()
        ));

        // Lê os recordes locais associados ao ID deste jogador.
        PlayerStatsRepository statsRepository = new PlayerStatsRepository(this);

        bestStreak.setText(
                getString(
                        R.string.best_streak_value,
                        statsRepository.getBestStreak(player.getId())
                )
        );

        bestScore.setText(
                getString(
                        R.string.best_score_value,
                        statsRepository.getBestScore(player.getId())
                )
        );

        // Mostra um aviso temporário enquanto a ligação ao leaderboard fica adiada.
        leaderboardButton.setOnClickListener(view ->
                Toast.makeText(
                        this,
                        R.string.leaderboard_unavailable,
                        Toast.LENGTH_SHORT
                ).show()
        );

        dialog.setContentView(panel);
        dialog.show();

    }
}
