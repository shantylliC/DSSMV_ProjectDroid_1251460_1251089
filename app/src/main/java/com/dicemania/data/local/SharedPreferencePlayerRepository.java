package com.dicemania.data.local;

import android.content.Context;
import android.content.SharedPreferences;

import com.dicemania.domain.model.Player;
import com.dicemania.domain.repository.PlayerRepository;

/**
 * Guarda e recupera localmente o ID e o nickname do jogador.
 */
public class SharedPreferencePlayerRepository implements PlayerRepository {

    private static final String PREF_NAME = "DicaManiaPreference";
    private static final String KEY_ID = "player_id";
    private static final String KEY_NICKNAME = "player_nickname";

    private final SharedPreferences preferences;

    /**
     * Abre as preferências privadas da aplicação.
     */
    public SharedPreferencePlayerRepository(Context context) {
        preferences = context.getSharedPreferences(
                PREF_NAME,
                Context.MODE_PRIVATE
        );
    }

    /**
     * Guarda o ID e o nickname do jogador no telemóvel.
     */
    @Override
    public void saveLocalPlayer(Player player) {
        preferences.edit()
                .putString(KEY_ID, player.getId())
                .putString(KEY_NICKNAME, player.getNickname())
                .apply();
    }

    /**
     * Recupera o jogador guardado, ou null se ainda não existir.
     */
    @Override
    public Player getLocalPlayer() {
        String id = preferences.getString(KEY_ID, null);
        String nickname = preferences.getString(KEY_NICKNAME, null);

        if (id == null || nickname == null) {
            return null;
        }

        return new Player(id, nickname, 0);
    }
}