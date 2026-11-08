package com.dicemania.domain.model;

import com.dicemania.domain.repository.GameSessionListener;

import com.dicemania.domain.repository.ShakeCallback;
import com.dicemania.domain.util.AntiCheatTimer;
import com.dicemania.domain.repository.LeaderboardRepository;
import com.dicemania.domain.repository.ShakeSensor;
import com.dicemania.domain.util.ScoreCalculator;
import java.util.List;

public class GameSession implements ShakeCallback { // variaveis privadas, intocáveis pelos restantes ficheiros sem ter autorizaçao
    private Player player; // guarda a identidade de quem joga (ID e nome). Vem do ficheiro Player e é criado atrvés da inserçao do layout, as credenciais do jogador
    private int currentQuestionIndex; // contador das questoes, vai de 0 a 14 (verdadeiramente do 1 ao 15) // Nasce dentro do proprio construtor gameSession, garantindo que o jogo começe limpo
    private int currentStreak; // Quantas perguntas o jogodor tem acertados de seguida // Nasce dentro do proprio construtor gameSession, garantindo que o jogo começe limpo
    private int bestStreak;
    private int totalScore; // pontuaçao total acumulada durante a partida // Nasce dentro do proprio construtor gameSession, garantindo que o jogo começe limpo
    private GameState state; //Guarda o estado atual do jogo // Vem do ficheiro GameState, instancia-se diretamente no construtor, começando no Waiting_for_roll
    private List<Question> currentBlock; // a lista que contém as 3 perguntas, e as suas respetivas respostas, dependendo do seu tipo
    // IMPORTANTE!!!: esta lista é a mesma que aparece no ficheiro RetrofitQuestionRepository, tendo o molde criado no ficheiro Question, o tal perguntasLimpas que está no fim e que retorna o ficheiro

    private ShakeSensor shakeSensor;
    private LeaderboardRepository leaderboardRepository;
    private AntiCheatTimer antiCheatTimer;

    // Guarda o canal pelo qual a sessão avisa a Activity de um abanão válido.
    private GameSessionListener gameSessionListener;



    // O Construtor: Prepara o jogo assim que o jogador entra, instanciando estas variaveis
     // Construtor compatível com código existente que não precisa de receber eventos do sensor na interface.

    public GameSession( // preserva as chamadas antigas de teste
            Player player,
            ShakeSensor shakeSensor,
            LeaderboardRepository leaderboardRepository
    ) {
        this(player, shakeSensor, leaderboardRepository, null);
    }





    // Reabre a sessão com os valores atuais ao passar para a pergunta seguinte.
    //Prepara uma nova partida e começa a ouvir o acelerómetro.
    public GameSession(
            Player player,
            ShakeSensor shakeSensor,
            LeaderboardRepository leaderboardRepository,
            GameSessionListener listener
    ) {
        this.player = player;
        this.currentQuestionIndex = 0;
        this.currentStreak = 0;
        this.bestStreak = 0;
        this.totalScore = 0;
        this.state = GameState.WAITING_FOR_ROLL;

        this.shakeSensor = shakeSensor;
        this.leaderboardRepository = leaderboardRepository;
        this.gameSessionListener = listener;
        this.antiCheatTimer = new AntiCheatTimer();

        // A própria GameSession recebe o callback do sensor.
        this.shakeSensor.startListening(this);
    }


    //Recebe o evento do acelerómetro e impede que o mesmo abanão inicie vários pedidos consecutivos.
    @Override
    public void onShakeDetected() {
        if (state != GameState.WAITING_FOR_ROLL) {
            return;
        }

        // Bloqueia novos abanões enquanto a animação e o pedido decorrem.
        state = GameState.LOADING_BLOCK;
        shakeSensor.stopListening();

        // A Activity inicia a animação e pede as perguntas à API.
        if (gameSessionListener != null) {
            gameSessionListener.onRollDetected();
        }
    }



    // Chamado quando o jogador agita o telemovel, o ShakeSensor dispara e o Question devolve 3 perguntas
    // estas 3 perguntas seram guardadas na variavel chamada de current block
    public void startNewBlock(List<Question> questions) {

        if (questions == null || questions.size() != 3) {
            throw new IllegalArgumentException(
                    "A partida precisa de receber exatamente três perguntas."
            );
        }

        this.currentBlock = questions; // no currentBlock, variavel temporal, estao as 3 perguntas, e as suas respostas, que o jogador tem de responder
        this.state = GameState.IN_BREAK; // Quando currentBlock tiver as questoes, este vai mudar o estado do jogo para in_break, o que permitirá entrar a aplicaçao naquele descanso de 5 segundos entre o ecra de laçamento dos dados e aquela de responder as questoes, ou seja, acabará por ser o ecra em que se vai ver o numero da questao e o tipo de pergunta que vamos fazer

    } // este bloco sempre volta a funcionar cada vez que o jogador lança os dados

    // Processa a resposta quando o jogador clica num botão
    public void processAnswer(String selectedAnswer, String correctAnswer, int timeRemainingSeconds) { // recebe a variavel selectedAnswer do ficheiro GameActivity, correctAnswer do ficheiro Question, que vem da variavel currentBlock; e o timeRemainingSeconds do cronometro doe cra, countdowntimer
        // esta funçao é chamada quando o jogador toca em algum dos botoes para a resposta

        if (correctAnswer.equals(selectedAnswer) && timeRemainingSeconds > 0) { // se a resposta selecionada pelo jogador é igual a resposta correta e os tempo de responder a resposta é superior a

            currentStreak++; // streak aumenta com cada resposta feita

            bestStreak = Math.max(bestStreak, currentStreak);

            totalScore += ScoreCalculator.calculate(timeRemainingSeconds, currentStreak); // a pontuçao aumenta ou permanece igual dependendo das respostas do jogador
            // o ScoreCalculator vem do ficheiro ScoreCalculator. sao passados os parametros para calculo

        } else {
            currentStreak = 0; // o jogador Errou, perde o streak de acertos
        }

        advanceGame();
    }

    // Funçao que aparece se o tempo chegar ao zero ou o AntiCheatTimer detetar batota
    public void processTimeout() { // esta funçao é chamda caso o jogador nao responder ou o tempo acabar

        currentStreak = 0; // Penalização imediata, logo a zero
        advanceGame();
    }
    // porque usamos estas duas funçoes?: teriamos de colocar mensagens de que o jogador nao respondeu nada ao processAnswer, logo o codigo ficaria sujo. É considerado uma má prática


    //Regista quando o jogador deixa a aplicação durante uma pergunta.
    public void notifyAppMinimized() {
        if (state == GameState.PLAYING_QUESTION) {
            antiCheatTimer.markAppMinimized();
        }
    }

    //Verifica se o jogador excedeu o limite permitido fora da aplicação.
    public void notifyAppRestored() {
        if (state == GameState.PLAYING_QUESTION
                && antiCheatTimer.isTimeoutPenalty()) {
            processTimeout();
        }
    }


    // Lógica privada para controlar o Fluxo das 15 perguntas (Sincronização Parcial)
    // Avança para a pergunta seguinte e decide se a partida continua, se é preciso lançar os dados novamente ou se terminou.
    private void advanceGame() {
        currentQuestionIndex++;

        if (currentQuestionIndex >= 15) {
            state = GameState.FINISHED;

            // Garante que o acelerómetro fica desligado no fim da partida.
            shakeSensor.stopListening();

            // Envia a pontuação final, se houver um repositório configurado.
            if (leaderboardRepository != null) {
                leaderboardRepository.submitScore(
                        player.getId(),
                        totalScore
                );
            }

        } else if (currentQuestionIndex % 3 == 0) {
            // Depois de cada bloco de três perguntas, espera um novo lançamento.
            state = GameState.WAITING_FOR_ROLL;

        } else {
            // Ainda há perguntas no bloco atual.
            state = GameState.IN_BREAK;
        }
    }

    // --- MODO DESENVOLVEDOR (UC04 / RNF17) ---
    public void debugForceEndGame() { // modo DEUS, temos a maxima pontuaçao e mostra a final da partida e o leaderboard
        this.totalScore += 15000;
        this.currentQuestionIndex = 15;
        this.state = GameState.FINISHED;
    }


    // ZONA DE FALHAS (ERROS)

    // Devolve a pergunta atual dentro do bloco de três.
    public Question getCurrentQuestion() {
        if (currentBlock == null || currentBlock.isEmpty()) {
            return null;
        }

        // O índice geral da partida vai de 0 a 14; o bloco usa índices de 0 a 2.
        int questionInBlock = currentQuestionIndex % 3;
        return currentBlock.get(questionInBlock);
    }

    //Volta a permitir abanões depois de a API não conseguir carregar um bloco de perguntas.
    public void onBlockLoadFailed() {
        if (state == GameState.LOADING_BLOCK) {
            state = GameState.WAITING_FOR_ROLL;
        }
    }

    //Reativa o sensor quando a partida precisa de outro bloco.
    public void listenForNextRoll() {
        if (state == GameState.WAITING_FOR_ROLL) {
            shakeSensor.startListening(this);
        }
    }

    //Desliga o sensor quando a Activity termina.
    public void dispose() {
        if (shakeSensor != null) {
            shakeSensor.stopListening();
        }
    }


    // Marca que o jogador começou a responder à pergunta.
    public void setPlayingState() {
        if (state == GameState.IN_BREAK && currentBlock != null) {
            state = GameState.PLAYING_QUESTION;
        }
    }


    // --- Getters para os ecrãs do Android poderem ler a informação --- Sao a encapsulaçao, podem ver mas nao podem tocar/alterar
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
        return totalScore;
    }
    public GameState getState() {
        return state;
    }
    public List<Question> getCurrentBlock() {
        return currentBlock;
    }

    // Devolve o maior streak conseguido nesta partida.
    public int getBestStreak() {
        return bestStreak;
    }

}

