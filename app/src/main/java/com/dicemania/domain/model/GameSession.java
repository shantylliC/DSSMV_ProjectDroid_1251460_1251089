package com.dicemania.domain.model;

import com.dicemania.domain.util.ScoreCalculator;
import java.util.List;

public class GameSession {
    private Player player;
    private int currentQuestionIndex; // Vai de 0 a 15
    private int currentStreak;
    private int totalScore;
    private GameState state;
    private List<Question> currentBlock;

    // O Construtor: Prepara o jogo assim que o jogador entra
    public GameSession(Player player) {
        this.player = player;
        this.currentQuestionIndex = 0;
        this.currentStreak = 0;
        this.totalScore = 0;
        this.state = GameState.WAITING_FOR_ROLL; // Passo 2: À espera que agite o telemóvel
    }

    // Chamado quando o ShakeSensor dispara e o QuestionRepository devolve 3 perguntas
    public void startNewBlock(List<Question> questions) {
        this.currentBlock = questions;
        this.state = GameState.IN_BREAK; // Vai para o compasso de espera de 5 segundos
    }

    // Processa a resposta quando o jogador clica num botão
    public void processAnswer(String selectedAnswer, String correctAnswer, int timeRemainingSeconds) {
        if (correctAnswer.equals(selectedAnswer) && timeRemainingSeconds > 0) {
            currentStreak++;
            totalScore += ScoreCalculator.calculate(timeRemainingSeconds, currentStreak);
        } else {
            currentStreak = 0; // Errou, perde a série de acertos
        }
        advanceGame();
    }

    // Chamado se o tempo chegar ao zero ou o AntiCheatTimer detetar batota
    public void processTimeout() {
        currentStreak = 0; // Penalização imediata
        advanceGame();
    }

    // Lógica privada para controlar o Fluxo das 15 perguntas (Sincronização Parcial)
    private void advanceGame() {
        currentQuestionIndex++;

        if (currentQuestionIndex >= 15) {
            state = GameState.FINISHED; // Acabou o jogo, pronto para enviar ao Supabase
        } else if (currentQuestionIndex % 3 == 0) {
            state = GameState.WAITING_FOR_ROLL; // Fez 3 perguntas. Precisa agitar novamente!
        } else {
            state = GameState.IN_BREAK; // Continua no mesmo bloco, vai para a pausa de 5s
        }
    }

    // --- MODO DESENVOLVEDOR (UC04 / RNF17) ---
    public void debugForceEndGame() {
        this.totalScore += 15000;
        this.currentQuestionIndex = 15;
        this.state = GameState.FINISHED;
    }

    // --- Getters para os ecrãs do Android poderem ler a informação ---
    public Player getPlayer() { return player; }
    public int getCurrentQuestionIndex() { return currentQuestionIndex; }
    public int getCurrentStreak() { return currentStreak; }
    public int getTotalScore() { return totalScore; }
    public GameState getState() { return state; }
    public List<Question> getCurrentBlock() { return currentBlock; }
}
