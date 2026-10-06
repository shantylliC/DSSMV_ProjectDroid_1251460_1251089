package com.dicemania.domain.repository;

import com.dicemania.domain.model.Player;

public interface PlayerRepository {
    // Regra para guardar o jogador no SharedPreferences
    void saveLocalPlayer(Player player);

    // Regra para carregar o jogador quando a app abre
    Player getLocalPlayer();
}

