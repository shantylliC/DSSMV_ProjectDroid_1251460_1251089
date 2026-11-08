package com.dicemania.domain.repository;

//Permite à GameSession avisar a Activity de que houve um abanão válido.
 // A Activity trata então da animação e do pedido de perguntas.

public interface GameSessionListener {

    //Chamado uma vez quando o sensor deteta um abanão válido (Força > 2.7G).
    void onRollDetected();
}