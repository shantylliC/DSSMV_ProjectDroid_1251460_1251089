package com.dicemania.data.network;

import com.dicemania.domain.model.Category;
import com.dicemania.domain.model.Question;
import com.dicemania.domain.model.QuestionType;
import com.google.gson.Gson;

import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Testa a conversão do DTO da OpenTDB para Question sem fazer pedidos de rede.
 */
public class RetrofitQuestionRepositoryTest {

    private final Gson gson = new Gson();

    @Test
    public void mapsMultipleChoiceDtoAndPreservesAllFourOptions() {
        String json = "{"
                + "\"question\":\"What is 2 + 2?\","
                + "\"correct_answer\":\"4\","
                + "\"incorrect_answers\":[\"1\",\"2\",\"3\"]"
                + "}";

        OpenTDBresponse.QuestionJson dto = gson.fromJson(
                json,
                OpenTDBresponse.QuestionJson.class
        );

        Question pergunta = RetrofitQuestionRepository.mapQuestion(
                Category.ART,
                QuestionType.MULTIPLE_CHOICE,
                dto
        );

        assertEquals(Category.ART, pergunta.getCategory());
        assertEquals(QuestionType.MULTIPLE_CHOICE, pergunta.getType());
        assertEquals("What is 2 + 2?", pergunta.getText());
        assertEquals("4", pergunta.getCorrectAnswer());

        List<String> respostasMisturadas = pergunta.getRespostasMisturadas();
        assertEquals(4, respostasMisturadas.size());
        assertOccursOnce(respostasMisturadas, "4");
        assertOccursOnce(respostasMisturadas, "1");
        assertOccursOnce(respostasMisturadas, "2");
        assertOccursOnce(respostasMisturadas, "3");
    }

    @Test
    public void mapsTrueFalseDtoAndKeepsBothOptions() {
        String json = "{"
                + "\"question\":\"The sky can appear blue?\","
                + "\"correct_answer\":\"True\","
                + "\"incorrect_answers\":[\"False\"]"
                + "}";

        OpenTDBresponse.QuestionJson dto = gson.fromJson(
                json,
                OpenTDBresponse.QuestionJson.class
        );

        Question pergunta = RetrofitQuestionRepository.mapQuestion(
                Category.SCIENCE,
                QuestionType.TRUE_FALSE,
                dto
        );

        List<String> respostasMisturadas = pergunta.getRespostasMisturadas();
        assertEquals(2, respostasMisturadas.size());
        assertEquals("True", pergunta.getCorrectAnswer());
        assertOccursOnce(respostasMisturadas, "True");
        assertOccursOnce(respostasMisturadas, "False");
    }

    @Test
    public void mapsDtoWhenIncorrectAnswersAreNull() {
        String json = "{"
                + "\"question\":\"Question without incorrect options\","
                + "\"correct_answer\":\"Correct\","
                + "\"incorrect_answers\":null"
                + "}";

        OpenTDBresponse.QuestionJson dto = gson.fromJson(
                json,
                OpenTDBresponse.QuestionJson.class
        );

        Question pergunta = RetrofitQuestionRepository.mapQuestion(
                Category.HISTORY,
                QuestionType.MULTIPLE_CHOICE,
                dto
        );

        assertEquals(1, pergunta.getRespostasMisturadas().size());
        assertEquals(
                Collections.singletonList("Correct"),
                pergunta.getRespostasMisturadas()
        );
    }

    // Confirma que cada opção esperada existe exatamente uma vez, sem depender da ordem aleatória.
    private void assertOccursOnce(List<String> respostas, String respostaEsperada) {
        assertEquals(1, Collections.frequency(respostas, respostaEsperada));
    }
}
