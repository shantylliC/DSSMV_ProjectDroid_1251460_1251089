package com.dicemania.domain.util;

public class AntiCheatTimer {
    private long backgroundTimestamp = 0;
    private static final long MAX_ALLOWED_TIME_MS = 10000; // 10 segundos de limite

    // Chamado quando o jogador minimiza a app
    public void markAppMinimized() {
        backgroundTimestamp = System.currentTimeMillis();
    }

    // Chamado quando o jogador volta, para ver se é penalizado
    public boolean isTimeoutPenalty() {
        if (backgroundTimestamp == 0) return false;

        long timeAway = System.currentTimeMillis() - backgroundTimestamp;
        backgroundTimestamp = 0; // Limpa o registo após verificar

        return timeAway > MAX_ALLOWED_TIME_MS;
    }
}

