package com.dicemania.data.local;

import android.content.Context;
import android.content.SharedPreferences;

// Guarda os recordes de cada jogador no telemóvel.
public class PlayerStatsRepository {

    private static final String PREFERENCES_NAME = "DiceManiaPlayerStats";
    private static final String KEY_BEST_SCORE = "best_score_";
    private static final String KEY_BEST_STREAK = "best_streak_";
    // Marca partidas terminadas, incluindo resultados em que score e streak são zero.
    private static final String KEY_HAS_FINISHED_GAME = "has_finished_game_";

    private final SharedPreferences preferences;

    // Abre as preferências privadas usadas para guardar os recordes.
    public PlayerStatsRepository(Context context) {
        preferences = context.getSharedPreferences(
                PREFERENCES_NAME,
                Context.MODE_PRIVATE
        );
    }

    // Devolve a maior pontuação guardada para este jogador.
    public int getBestScore(String playerId) {
        return preferences.getInt(KEY_BEST_SCORE + playerId, 0);
    }

    // Devolve o maior streak guardado para este jogador.
    public int getBestStreak(String playerId) {
        return preferences.getInt(KEY_BEST_STREAK + playerId, 0);
    }

    // Indica se este jogador já terminou pelo menos uma partida, mesmo com pontuação zero.
    public boolean hasFinishedGame(String playerId) {
        return preferences.getBoolean(KEY_HAS_FINISHED_GAME + playerId, false);
    }

    // Atualiza cada recorde apenas quando o novo valor é superior.
    public void saveBestStats(String playerId, int score, int streak) {
        int previousBestScore = getBestScore(playerId);
        int previousBestStreak = getBestStreak(playerId);

        SharedPreferences.Editor editor = preferences.edit();

        if (score > previousBestScore) {
            editor.putInt(KEY_BEST_SCORE + playerId, score);
        }

        if (streak > previousBestStreak) {
            editor.putInt(KEY_BEST_STREAK + playerId, streak);
        }

        // Regista a conclusão para permitir reenviar resultados se a rede falhar.
        editor.putBoolean(KEY_HAS_FINISHED_GAME + playerId, true);
        editor.apply();
    }
}
