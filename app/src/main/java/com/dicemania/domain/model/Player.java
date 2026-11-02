package com.dicemania.domain.model;

// Este ficheiro é um objeto de java limpo. É com este ficheiro que o GameSession vai conversar
// vai ter todas as informaçoes já limpinhas pelos outros ficheiros. Só quando estiverem as informaçoes aqui é que o resto do jogo vai poder usá-las

public class Player {
    private String id;
    private String nickname;
    private int playerScore;

    public Player(String id, String nickname, int playerScore) {
        this.id = id;
        this.nickname = nickname;
        this.playerScore = playerScore;
    }

    // Getters para permitir a leitura dos dados
    public String getId() {
        return id;
    }
    public String getNickname() {

        return nickname;
    }
    public int getPlayerScore() {

        return playerScore;
    }

}



