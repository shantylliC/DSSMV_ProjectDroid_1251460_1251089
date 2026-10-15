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


    // O Construtor: Prepara o jogo assim que o jogador entra, instanciando estas variaveis
    public GameSession(Player player) {
        this.player = player;
        this.currentQuestionIndex = 0;
        this.currentStreak = 0;
        this.totalScore = 0;
        this.state = GameState.WAITING_FOR_ROLL; // Passo 2: À espera que agite o telemóvel

    }

    // Chamado quando o jogador agita o telemovel, o ShakeSensor dispara e o Question devolve 3 perguntas
    // estas 3 perguntas seram guardadas na variavel chamada de current block
    public void startNewBlock(List<Question> questions) {
        this.currentBlock = questions; // no currentBlock, variavel temporal, estao as 3 perguntas, e as suas respostas, que o jogador tem de responder
        this.state = GameState.IN_BREAK; // Quando currentBlock tiver as questoes, este vai mudar o estado do jogo para in_break, o que permitirá entrar a aplicaçao naquele descanso de 5 segundos entre o ecra de laçamento dos dados e aquela de responder as questoes, ou seja, acabará por ser o ecra em que se vai ver o numero da questao e o tipo de pergunta que vamos fazer

    } // este bloco sempre volta a funcionar cada vez que o jogador lança os dados

    // Processa a resposta quando o jogador clica num botão
    public void processAnswer(String selectedAnswer, String correctAnswer, int timeRemainingSeconds) { // recebe a variavel selectedAnswer do ficheiro GameActivity, correctAnswer do ficheiro Question, que vem da variavel currentBlock; e o timeRemainingSeconds do cronometro doe cra, countdowntimer
        // esta funçao é chamada quando o jogador toca em algum dos botoes para a resposta

        if (correctAnswer.equals(selectedAnswer) && timeRemainingSeconds > 0) { // se a resposta selecionada pelo jogador é igual a resposta correta e os tempo de responder a resposta é superior a

            currentStreak++; // streak aumenta com cada resposta feita
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


    // Lógica privada para controlar o Fluxo das 15 perguntas (Sincronização Parcial)
    private void advanceGame() { //chamada sempre que se responda a uma questao, NAO A UM BLOCO, só quando se respondem as questoes

        currentQuestionIndex++; // aumento constante até atingir a pergunta 15 (verdadeiramente até o 14, pois vai de 0 a 14)

        if (currentQuestionIndex >= 15) {// fazemos >= 15 como segurança, caso haja um bug e o jogador conseguir fazer com uqe exista um 16,17,.. questao, o programa nao crasha

            state = GameState.FINISHED; // coloca logo o estado de: Acabou o jogo, ficando pronto para que esta partida seja enviada para a base de dados (SupaBase)

        } else if (currentQuestionIndex % 3 == 0) { // Com isto o jogo sabe quando se passaram 3 perguntas, ou seja, um bloco de questoes.
            // o % serve para calcular o resto duma divisao, o que significa que para sabermos que um bloco de questoes passou, temos de dividir o numero de questoes atuais entre 3 e dessa divisao devmos ter o resto 0. Exemplo: 1% 3 nao é zero - avança, 2 % 3 nao é zero - avança, 3 % 3 é zero modamos de estado e assim sucessivamente até serem atinguidos os 5 blocos pretendidos

            state = GameState.WAITING_FOR_ROLL; // Quando for encontrado um bloco, o estado muda para WAITNG_FOR_ROLL. Fica a espera que o telemovel seja agitado novamente, ou seja, que sejam lançados os dados

        } else {
            state = GameState.IN_BREAK; // Em caso de que o resto da divisao (% 3) seja diferente de zero, continua-se no mesmo bloco, vai para a pausa de 5s e continua a o processo novamente no topo da funçao até atinguir outro bloco ou se acabar a partida

        }
    }

    // --- MODO DESENVOLVEDOR (UC04 / RNF17) ---
    public void debugForceEndGame() { // modo DEUS, temos a maxima pontuaçao e mostra a final da partida e o leaderboard
        this.totalScore += 15000;
        this.currentQuestionIndex = 15;
        this.state = GameState.FINISHED;
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
}
