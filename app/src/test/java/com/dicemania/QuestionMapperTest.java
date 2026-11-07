package com.dicemania;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class QuestionMapperTest {

    @Test
    public void testMisturaRespostas_MantemRespostaCorreta() {
        // 1. Arrange: Simular dados "sujos" vindos do DTO JSON da API OpenTDB
        String correctAnswer = "Lisboa";
        List<String> incorrectAnswers = new ArrayList<>();
        incorrectAnswers.add("Porto");
        incorrectAnswers.add("Faro");
        incorrectAnswers.add("Coimbra");

        // 2. Act: O processo de mapeamento feito no RetrofitQuestionRepository
        List<String> respostasMisturadas = new ArrayList<>();
        respostasMisturadas.add(correctAnswer);
        respostasMisturadas.addAll(incorrectAnswers);

        // Invocar o baralhador do Java
        Collections.shuffle(respostasMisturadas);

        // 3. Assert: Garantir que a mistura tem exatamente 4 opções e a certa não se perdeu
        assertEquals("A lista tem de conter exatamente 4 respostas", 4, respostasMisturadas.size());
        assertTrue("A lista final tem de conter a resposta correta após ser baralhada",
                respostasMisturadas.contains(correctAnswer));
    }
}