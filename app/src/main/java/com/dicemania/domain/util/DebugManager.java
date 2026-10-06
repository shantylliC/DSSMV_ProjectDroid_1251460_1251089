package com.dicemania.domain.util;

public class DebugManager {
    private int tapCount = 0;
    private long lastTapTime = 0;
    private static final long MAX_DELAY_MS = 500; // Meio segundo máximo entre cada toque

    public boolean registerSecretTap() {
        long currentTime = System.currentTimeMillis();

        // Se demorou demasiado tempo desde o último toque, recomeça a contagem
        if (currentTime - lastTapTime > MAX_DELAY_MS) {
            tapCount = 1;
        } else {
            tapCount++;
        }

        lastTapTime = currentTime;

        // Se acertou os 5 toques rápidos, devolve verdadeiro e limpa a contagem
        if (tapCount == 5) {
            tapCount = 0;
            return true;
        }

        return false;
    }
}


