package com.dicemania.data.network;

import com.dicemania.domain.model.Category;
import com.dicemania.domain.model.Question;
import com.dicemania.domain.model.QuestionType;
import com.dicemania.domain.repository.QuestionRepository;

import java.util.ArrayList;
import java.util.List;


public class RetrofitQuestionRepository implements QuestionRepository { //implements: assinatura de contrato. Quando temos uma interfase, na qual se coloca o que se é preciso fazer (certas tarefas, pedidos, etc ), a implements faz com que seja obrigatorio que esta classe faça, escreva o codigo para poder realizar estas tarefas/pedidos

    @Override

    public List<Question> getQuestions (Category category, QuestionType type, int amount){ // QUESTION: versao organizada e limpa de QUESTIONJSON, um molde pronto a ser enviado para o resto dos ficheiros. Construindo objetos puros, pois faz uma traduçao para que os restos dos ficheiros possam usar a sua informaçao


        //falta a logica de baralhamento das questoes



        List<Question> perguntasProntas = new ArrayList<>(); // vetor dinamico que será preenchido com os objetos em cima. nao pode guardar texto JSON, só o puro das QUESTION. Isto será feito para cada pergunta recebida pelo JSON
        return perguntasProntas;

    }
}
