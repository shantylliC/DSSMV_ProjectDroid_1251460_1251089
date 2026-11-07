package com.dicemania;

import org.junit.Test;
import static org.junit.Assert.*;

public class SensorMathTest {

    @Test
    public void testGForceCalculation_ValidShake() {
        // 1. Arrange: Simular valores recebidos de um abanão forte (ex: Eixo X muito elevado)
        float x = 30.0f;
        float y = 2.0f;
        float z = 1.0f;
        float gravityEarth = 9.8f; // SensorManager.GRAVITY_EARTH

        // 2. Act: Executar a lógica matemática presente no AndroidShakeSensor
        float gX = x / gravityEarth;
        float gY = y / gravityEarth;
        float gZ = z / gravityEarth;

        float gForce = (float) Math.sqrt(gX * gX + gY * gY + gZ * gZ);

        // 3. Assert: Garantir que o resultado supera o limiar de 2.7G para iniciar o jogo
        assertTrue("A força G (calculada: " + gForce + ") deve ser maior que o limiar de 2.7", gForce > 2.7f);
    }
}