package com.dicemania;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputFilter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.dicemania.data.local.SharedPreferencePlayerRepository;
import com.dicemania.data.sound.BackgroundMusicManager;
import com.dicemania.domain.util.DebugManager;
import com.dicemania.domain.model.Player;

import java.util.UUID;


// Mostra o login a quem ainda não tem jogador guardado e encaminha os restantes para HomeActivity.
public class MainActivity extends AppCompatActivity {

    // Limite para o nickname caber inteiro em cada linha do leaderboard.
    private static final int MAX_NICKNAME_LENGTH = 9;

    private EditText usernameInput;
    private SharedPreferencePlayerRepository playerRepository;
    private DebugManager debugManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        playerRepository = new SharedPreferencePlayerRepository(this);

        // Se já houver um jogador guardado, abre diretamente o ecrã inicial.
        if (playerRepository.getLocalPlayer() != null) {
            openHome();
            return;
        }

        // Apresenta o formulário apenas quando ainda não existe jogador guardado.
        setContentView(R.layout.activity_main);

        debugManager = new DebugManager(this);
        configureDebugModeLogoTap();

        usernameInput = findViewById(R.id.usernameInput);
        usernameInput.setFilters(new InputFilter[] {
                new InputFilter.LengthFilter(MAX_NICKNAME_LENGTH)
        });

        Button loginButton = findViewById(R.id.loginButton);
        loginButton.setOnClickListener(view -> savePlayerAndOpenHome());
    }

    // Liga cinco toques rápidos no logo à alternância do modo Deus.
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

    // Inicia ou retoma a música ambiente quando o ecrã inicial fica visível.
    @Override
    protected void onResume() {
        super.onResume();
        BackgroundMusicManager.getInstance().resumeIfEnabled(this);
    }

    // Pausa a faixa quando a aplicação deixa de estar em primeiro plano.
    @Override
    protected void onPause() {
        BackgroundMusicManager.getInstance().pause();
        super.onPause();
    }

    // Valida e guarda o nickname antes de abrir o ecrã inicial.
    private void savePlayerAndOpenHome() {
        String nickname = usernameInput.getText().toString().trim();

        if (nickname.isEmpty()) {
            usernameInput.setError(getString(R.string.enter_nickname_error));
            return;
        }

        Player savedPlayer = playerRepository.getLocalPlayer();

        // Mantém o ID se o nickname for o mesmo; cria outro ID se for diferente.
        String playerId;
        if (savedPlayer != null
                && nickname.equals(savedPlayer.getNickname())) {
            playerId = savedPlayer.getId();
        } else {
            playerId = UUID.randomUUID().toString();
        }

        Player player = new Player(playerId, nickname, 0);
        playerRepository.saveLocalPlayer(player);

        // O botão Let's Play da HomeActivity é que inicia a partida.
        openHome();
    }

    // Abre o ecrã inicial do jogador.
    private void openHome() {
        startActivity(new Intent(this, HomeActivity.class));
        finish();
    }
}
