package com.dicemania.domain.model;

import com.dicemania.domain.util.ScoreCalculator;
import java.util.List;

public class GameSession { // variaveis privadas, intocáveis pelos restantes ficheiros sem ter autorizaçao
    private Player player; // guarda a identidade de quem joga (ID e nome). Vem do ficheiro Player e é criado atrvés da inserçao do layout, as credenciais do jogador
    private int currentQuestionIndex; // contador das questoes, vai de 0 a 14 (verdadeiramente do 1 ao 15) // Nasce dentro do proprio construtor gameSession, garantindo que o jogo começe limpo
    private int currentStreak; // Quantas perguntas o jogodor tem acertados de seguida // Nasce dentro do proprio construtor gameSession, garantindo que o jogo começe limpo
    private int totalScore; // pontuaçao total acumulada durante a partida // Nasce dentro do proprio construtor gameSession, garantindo que o jogo começe limpo
    private GameState state; //Guarda o estado atual do jogo // Vem do ficheiro GameState, instancia-se diretamente no construtor, começando no Waiting_for_roll
    private List<Question> currentBlock; // a lista que contém as 3 perguntas, e as suas respetivas respostas, dependendo do seu tipo
    // IMPORTANTE!!!: esta lista é a mesma que aparece no ficheiro RetrofitQuestionRepository, tendo o molde criado no ficheiro Question, o tal perguntasLimpas que está no fim e que retorna o ficheiro


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
    public Player getPlayer() {
        return player;
    }
    public int getCurrentQuestionIndex() {
        return currentQuestionIndex;
    }
    public int getCurrentStreak() {
        return currentStreak;
    }
    public int getTotalScore() {
        return totalScore; }
    public GameState getState() {
        return state; }
    public List<Question> getCurrentBlock() {
        return currentBlock; }
}
