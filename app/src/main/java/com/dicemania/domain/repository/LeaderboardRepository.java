package com.dicemania.domain.repository;

import com.dicemania.domain.model.Player;
import java.util.List;

public interface LeaderboardRepository {
    // Regra para enviar os pontos no final
    void submitScore(String playerId, int finalScore);

    // Regra para receber o Top 10
    List<Player> getTop10();
}
