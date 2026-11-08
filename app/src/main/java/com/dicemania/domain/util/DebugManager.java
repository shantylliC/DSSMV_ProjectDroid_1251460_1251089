package com.dicemania.domain.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemClock;

// Reconhece cinco toques rápidos no logo e guarda o estado do modo Deus.
public class DebugManager {

    // Preferências locais que mantêm o modo ativo ao passar de ecrã ou reiniciar a app.
    private static final String PREFERENCES_NAME = "dice_mania_debug_settings";
    private static final String KEY_GOD_MODE_ENABLED = "god_mode_enabled";

    // O intervalo máximo entre dois toques seguidos no logo.
    private static final long MAX_DELAY_MS = 900L;

    // Guarda as preferências desta instalação da aplicação.
    private final SharedPreferences preferences;

    // Conta os toques seguidos e mede quando ocorreu o último.
    private int tapCount;
    private long lastTapTime;

    // Abre as preferências privadas usadas para manter o modo Deus.
    public DebugManager(Context context) {
        preferences = context.getApplicationContext().getSharedPreferences(
                PREFERENCES_NAME,
                Context.MODE_PRIVATE
        );
    }

    // Devolve verdadeiro uma vez a cada sequência de cinco toques rápidos.
    public boolean registerSecretTap() {
        long currentTime = SystemClock.elapsedRealtime();

        // Se a pausa foi longa, começa uma nova sequência no primeiro toque.
        if (currentTime - lastTapTime > MAX_DELAY_MS) {
            tapCount = 1;
        } else {
            tapCount++;
        }

        lastTapTime = currentTime;

        // Reinicia a contagem depois do quinto toque para permitir alternar o modo.
        if (tapCount >= 5) {
            tapCount = 0;
            return true;
        }

        return false;
    }

    // Devolve se o modo Deus está ligado nesta instalação.
    public boolean isGodModeEnabled() {
        return preferences.getBoolean(KEY_GOD_MODE_ENABLED, false);
    }

    // Alterna o modo Deus e devolve o novo estado para a mensagem do ecrã.
    public boolean toggleGodMode() {
        boolean newState = !isGodModeEnabled();
        preferences.edit()
                .putBoolean(KEY_GOD_MODE_ENABLED, newState)
                .apply();
        return newState;
    }
}
