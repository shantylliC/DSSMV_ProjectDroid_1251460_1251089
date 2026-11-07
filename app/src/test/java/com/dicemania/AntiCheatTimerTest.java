package com.dicemania;

import org.junit.Test;
import static org.junit.Assert.*;
import com.dicemania.domain.util.AntiCheatTimer;

public class AntiCheatTimerTest {

    @Test
    public void testBatota_Ultrapassa10Segundos_AplicaPenalizacao() throws InterruptedException {
        // 1. Arrange: Instanciar a ferramenta
        AntiCheatTimer timer = new AntiCheatTimer();

        // 2. Act: Simular a minimização da app pelo jogador
        timer.markAppMinimized();

        // Simular o tempo a passar no background (10.050 milissegundos)
        // Usamos Thread.sleep para atrasar a execução do teste
        Thread.sleep(10050);

        // 3. Assert: Verificar se o sistema deteta a infração
        boolean isPenalty = timer.isTimeoutPenalty();

        assertTrue("O jogador deveria ser penalizado por demorar mais de 10 segundos", isPenalty);
    }
}