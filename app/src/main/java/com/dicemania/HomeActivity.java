package com.dicemania;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.material.switchmaterial.SwitchMaterial;

import androidx.appcompat.app.AppCompatActivity;

import com.dicemania.data.local.SharedPreferencePlayerRepository;
import com.dicemania.data.local.PlayerStatsRepository;
import com.dicemania.data.network.RetrofitLeaderboardRepository;
import com.dicemania.data.sound.BackgroundMusicManager;
import com.dicemania.data.sound.GameFeedbackEffects;
import com.dicemania.domain.util.DebugManager;

import com.dicemania.domain.model.Player;
import com.google.android.material.bottomsheet.BottomSheetDialog;

public class HomeActivity extends AppCompatActivity {

    // Identidade local usada pela partida e pelo painel de definições.
    private Player player;

    // Recordes locais usados para mostrar e reenviar os valores ao servidor.
    private PlayerStatsRepository playerStatsRepository;

    // Repositório online partilhado com a partida e o ecrã do ranking.
    private RetrofitLeaderboardRepository leaderboardRepository;

    // Reconhece a sequência secreta de toques no logo da Home.
    private DebugManager debugManager;

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

        playerStatsRepository = new PlayerStatsRepository(this);
        leaderboardRepository = RetrofitLeaderboardRepository.getInstance();
        debugManager = new DebugManager(this);


        Button letsPlayButton = findViewById(R.id.letsPlayButton);
        ImageButton settingsButton = findViewById(R.id.settingsButton);
        configureDebugModeLogoTap();

        // Começa uma partida para o jogador atual.
        letsPlayButton.setOnClickListener(view -> startGame());

        // Abre o painel com o perfil e os recordes.
        settingsButton.setOnClickListener(view -> showSettings());
    }

    // Alterna o modo Deus depois de cinco toques rápidos no logo do jogo.
    private void configureDebugModeLogoTap() {
        ImageView gameLogo = findViewById(R.id.gameLogo);
        gameLogo.setOnClickListener(view -> {
            if (!debugManager.registerSecretTap()) {
                return;
            }

            boolean godModeEnabled = debugManager.toggleGodMode();
            Toast.makeText(
                    this,
                    godModeEnabled
                            ? R.string.debug_mode_activated
                            : R.string.debug_mode_deactivated,
                    Toast.LENGTH_SHORT
            ).show();
        });
    }

    // Reenvia os recordes locais ao regressar ao menu para recuperar envios falhados por falta de rede.
    @Override
    protected void onResume() {
        super.onResume();
        BackgroundMusicManager.getInstance().resumeIfEnabled(this);

        if (player == null || playerStatsRepository == null || leaderboardRepository == null) {
            return;
        }

        int bestScore = playerStatsRepository.getBestScore(player.getId());
        int bestStreak = playerStatsRepository.getBestStreak(player.getId());

        // Não cria linhas vazias no ranking antes de o jogador terminar uma partida.
        if (playerStatsRepository.hasFinishedGame(player.getId())
                || bestScore > 0
                || bestStreak > 0) {
            leaderboardRepository.submitScore(
                    player.getId(),
                    player.getNickname(),
                    bestScore,
                    bestStreak
            );
        }
    }

    // Pausa a música quando a Home deixa de estar visível.
    @Override
    protected void onPause() {
        BackgroundMusicManager.getInstance().pause();
        super.onPause();
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
        SwitchMaterial musicSwitch = panel.findViewById(R.id.settingsMusicSwitch);
        SwitchMaterial vibrationSwitch =
                panel.findViewById(R.id.settingsVibrationSwitch);
        Button leaderboardButton =
                panel.findViewById(R.id.weeklyLeaderboardButton);

        // Mostra as opções guardadas sem gravar alterações durante a inicialização.
        soundSwitch.setChecked(GameFeedbackEffects.isSoundEnabled(this));
        musicSwitch.setChecked(BackgroundMusicManager.isMusicEnabled(this));
        vibrationSwitch.setChecked(GameFeedbackEffects.isVibrationEnabled(this));

        // Guarda cada opção imediatamente para ser aplicada na próxima partida.
        soundSwitch.setOnCheckedChangeListener((buttonView, isChecked) ->
                GameFeedbackEffects.setSoundEnabled(this, isChecked)
        );
        musicSwitch.setOnCheckedChangeListener((buttonView, isChecked) ->
                BackgroundMusicManager.setMusicEnabled(this, isChecked)
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

        // Fecha as definições e abre o ecrã que carrega o Top 10 da base de dados.
        leaderboardButton.setOnClickListener(view -> {
            dialog.dismiss();
            startActivity(new Intent(this, LeaderboardActivity.class));
        });

        dialog.setContentView(panel);
        dialog.show();

    }
}
