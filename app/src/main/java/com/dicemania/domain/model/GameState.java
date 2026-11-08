package com.dicemania.domain.model;

//Representa o ponto em que a partida se encontra.
 // A Activity consulta este estado para decidir que ecrã apresentar.

public enum GameState {
    WAITING_FOR_ROLL, // O jogador pode abanar o telemóvel.
    LOADING_BLOCK,    // A animação corre e a aplicação pede três perguntas.
    IN_BREAK,         // O resumo aparece durante cinco segundos.
    PLAYING_QUESTION, // O jogador está a responder.
    FINISHED          // As 15 perguntas terminaram.
}