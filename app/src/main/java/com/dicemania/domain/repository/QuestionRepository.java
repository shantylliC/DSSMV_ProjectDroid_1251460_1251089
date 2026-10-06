package com.dicemania.domain.repository;

import com.dicemania.domain.model.Category;
import com.dicemania.domain.model.Question;
import com.dicemania.domain.model.QuestionType;
import java.util.List;



public interface QuestionRepository {

    // Define a regra: quem implementar isto tem de devolver uma Lista de Perguntas
    List<Question> getQuestions(Category category, QuestionType type, int amount);

}
