package com.dicemania.data.network;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class OpenTDBresponse {

    @SerializedName("results") //tradutor Gson da palavra result da API
    public List<QuestionJson> results; // Vetor que vai guardar as 3 perguntas pedidas da API

    public static class QuestionJson  { // vai servir como sub-molde, ou seja, como molde para o formato das questoes

        public String category; // Molde para cada parte das perguntas vindas da API
        public String type;
        public String question;

        @SerializedName("correct_answer") // traduto Gson da palavra correct_answer da API
        public String correctAnswer; // variavel que vai guardar as respostas corretas vindas da API

        @SerializedName ("incorrect_answers")  // traduto Gson da palavra incorrect_answers da API
        public List <String> incorrectAnswers; //variavel que vai guardar as respostas incorretas vindas da API

    }

}
