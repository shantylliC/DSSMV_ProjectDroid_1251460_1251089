package com.dicemania.domain.repository;

import com.dicemania.domain.model.Player;

// vai definir as regras de guardado das informaçoes do jogador no telemovel. A  sua implementaçao esta no ficheiro SharedPreferencePlayerRespository

public interface PlayerRepository {
    // Regra para guardar o jogador no SharedPreferences
    void saveLocalPlayer(Player player);

    // Regra para carregar o jogador quando a app abre
    Player getLocalPlayer(); //Vai retornar null caso nao seja a primeira vez que o jogador entra no na aplicaçao
}

