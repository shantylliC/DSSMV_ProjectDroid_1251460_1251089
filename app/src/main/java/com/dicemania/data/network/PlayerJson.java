package com.dicemania.data.network;

import com.google.gson.annotations.SerializedName;

// DTO (Data Transfer Object): Este ficheiro serve como molde, regra, envelope de correio, que se deve cumprir para que se possa aceder corretamente à internet
// Estrutura a informaçao para que o retrofit consiga traduzir os dados de linguaguem java para formato de texo JSON

public class PlayerJson {

    @SerializedName("id") // Método GSON.
    //Diz que independentemente do nome que tenha esta variavel no código, que seja guardado com o nome id no servidor
    private String id; // declaraçao da variavel a usar

    @SerializedName("nickname")
    private String nickname;

    @SerializedName("score")
    private int score;

    // Nome da coluna best_streak devolvida e aceite pela tabela players.
    @SerializedName("best_streak")
    private int bestStreak;

    // Permite ao Gson criar o objeto antes de preencher os campos lidos do JSON.
    public PlayerJson() {
    }

    public PlayerJson (String id, String nickname, int score) { // É o nosso construtor
        this(id, nickname, score, 0);
    }

    // Constrói o JSON completo enviado para a tabela players do Supabase.
    public PlayerJson(String id, String nickname, int score, int bestStreak) {
        // Usa-se par poder instanciar um objeto físico ba memória do telemovel

        this.id = id; // pega no id recebido como argumento desta funçao e guarda-o no id principal desta classe (this.id)
        this.nickname = nickname;
        this.score = score;
        this.bestStreak = bestStreak;

    }

    // temos aqui os nosso Getters. Permitem que outros ficheiros saibam, sem poder alterar, qual é o id, nickname e score do jogador
    public String getId(){

        return id;

    }

    public String getNickname(){

        return nickname;

    }

    public int getScore(){

        return score;

    }

    // Devolve o streak máximo recebido da base de dados.
    public int getBestStreak() {
        return bestStreak;
    }
}
