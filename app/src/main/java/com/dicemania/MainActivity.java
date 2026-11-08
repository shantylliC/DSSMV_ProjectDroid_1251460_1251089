package com.dicemania;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.dicemania.data.local.SharedPreferencePlayerRepository;
import com.dicemania.domain.model.Player;

import java.util.UUID;


// Mostra o login a quem ainda não tem jogador guardado e encaminha os restantes para HomeActivity.
public class MainActivity extends AppCompatActivity {

    private EditText usernameInput;
    private SharedPreferencePlayerRepository playerRepository;

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

        usernameInput = findViewById(R.id.usernameInput);

        Button loginButton = findViewById(R.id.loginButton);
        loginButton.setOnClickListener(view -> savePlayerAndOpenHome());
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
