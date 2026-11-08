package com.dicemania.domain.model;

// Este ficheiro é um objeto de java limpo. É com este ficheiro que o GameSession vai conversar
// vai ter todas as informaçoes já limpinhas pelos outros ficheiros. Só quando estiverem as informaçoes aqui é que o resto do jogo vai poder usá-las

public class Player {
    private String id;
    private String nickname;
    private int playerScore;
    // Recorde de respostas certas seguidas devolvido pelo leaderboard.
    private int bestStreak;

    // Mantém compatibilidade com os jogadores locais e chamadas já existentes.
    public Player(String id, String nickname, int playerScore) {
        this(id, nickname, playerScore, 0);
    }

    // Cria um jogador completo, incluindo o recorde de streak vindo da base de dados.
    public Player(String id, String nickname, int playerScore, int bestStreak) {
        this.id = id;
        this.nickname = nickname;
        this.playerScore = playerScore;
        this.bestStreak = bestStreak;
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

    // Devolve o maior streak guardado para este jogador.
    public int getBestStreak() {
        return bestStreak;
    }

}



