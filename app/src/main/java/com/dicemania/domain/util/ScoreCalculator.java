package com.dicemania.domain.util;

public class ScoreCalculator {

    // Calcula os pontos recebendo o tempo que sobrou no relógio e o streak atual
    public static int calculate(int timeRemainingSeconds, int currentStreak) {
        if (timeRemainingSeconds <= 0) return 0; // Se o tempo acabou, 0 pontos

        int basePoints = 10;
        int timeBonus =  timeRemainingSeconds * 15;
        int streakBonus = currentStreak * 25; // Ganha 25 pontos extra por cada acerto consecutivo

        return basePoints + timeBonus + streakBonus;
    }
}
