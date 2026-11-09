package com.dicemania.domain.repository;

import com.dicemania.domain.model.Category;
import com.dicemania.domain.model.Question;
import com.dicemania.domain.model.QuestionType;
import java.util.List;

// quem terá todas as perguntas e respostas já limpinhas do RetrofitQuestionRepository. Só quando cheguem aqui é que poderam ser usadas

public interface QuestionRepository {

    // Define a regra: quem implementar isto tem de devolver uma Lista de Perguntas
    List<Question> getQuestions(Category category, QuestionType type, int amount);

    // Permite cancelar um pedido em curso quando a Activity sai de primeiro plano.
    // Implementações que não têm pedidos canceláveis podem manter esta operação vazia.
    default void cancelPendingRequests() {
    }

    // Reabilita pedidos depois de a Activity regressar ao primeiro plano.
    default void resumeRequests() {
    }

}
