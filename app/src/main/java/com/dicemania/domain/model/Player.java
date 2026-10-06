package com.dicemania.domain.model;

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
    public String getId() { return id; }
    public String getNickname() { return nickname; }
    public int getPlayerScore() { return playerScore; }

    // Setter para podermos atualizar a pontuação no final do jogo
    public void setPlayerScore(int score) { this.playerScore = score; }
}
