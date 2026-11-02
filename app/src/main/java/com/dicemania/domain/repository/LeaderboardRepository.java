package com.dicemania.domain.repository;

import com.dicemania.domain.model.Player;
import java.util.List;

public interface LeaderboardRepository {
    // Regra para enviar os pontos no final
    void submitScore(String playerId, String nickname, int finalScore);

    // Regra para receber o Top 10
    void getTop10 (LeaderboardCallback callback);

    // é o que recebemos quando chamamos ao servidor. Se sucesso faz tal coisa, se error outra
    interface LeaderboardCallback {

        void onSuccess(List <Player> topPLayer);
        void onError(String errorMessage);

    }
}
