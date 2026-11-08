package com.dicemania.domain.repository;

import com.dicemania.domain.model.Player;
import java.util.List;

public interface LeaderboardRepository {
    // Regra para enviar os pontos no final, preservando chamadas antigas do projeto.
    void submitScore(String playerId, String nickname, int finalScore);

    // Envia também o streak máximo da partida para ficar guardado no perfil online.
    default void submitScore(
            String playerId,
            String nickname,
            int finalScore,
            int bestStreak
    ) {
        submitScore(playerId, nickname, finalScore);
    }

    // Regra para receber o Top 10
    void getTop10 (LeaderboardCallback callback);

    // é o que recebemos quando chamamos ao servidor. Se sucesso faz tal coisa, se error outra
    interface LeaderboardCallback {

        void onSuccess(List<Player> topPlayers);
        void onError(String errorMessage);

    }
}
