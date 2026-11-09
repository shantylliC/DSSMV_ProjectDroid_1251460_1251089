package com.dicemania;

import android.animation.Animator;
import android.animation.ArgbEvaluator;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Html;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.content.res.ColorStateList;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Gravity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.text.HtmlCompat;
import androidx.core.content.res.ResourcesCompat;
import com.google.android.material.button.MaterialButton;

import com.airbnb.lottie.LottieAnimationView;
import com.dicemania.data.network.RetrofitQuestionRepository;
import com.dicemania.data.network.RetrofitLeaderboardRepository;
import com.dicemania.data.sensor.AndroidShakeSensor;
import com.dicemania.domain.model.Category;
import com.dicemania.domain.model.GameSession;
import com.dicemania.domain.model.GameState;
import com.dicemania.domain.model.Player;
import com.dicemania.domain.model.Question;
import com.dicemania.domain.model.QuestionType;
import com.dicemania.domain.repository.GameSessionListener;
import com.dicemania.domain.repository.QuestionRepository;
import com.dicemania.domain.repository.ShakeSensor;
import com.dicemania.data.local.PlayerStatsRepository;
import com.dicemania.data.sound.BackgroundMusicManager;
import com.dicemania.data.sound.GameFeedbackEffects;
import com.dicemania.domain.util.DebugManager;

import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

// Controla os ecrãs da partida: abanão, resumo e pergunta. Mantém uma só Activity ativa e troca o layout apresentado.
public class GameActivity extends AppCompatActivity implements GameSessionListener {

    // Duração do resumo entre perguntas.
    private static final int SUMMARY_SECONDS = 7;

    // Tempo para responder. Altera quando o tempo oficial estiver decidido.
    private static final int QUESTION_TIME_SECONDS = 15;

    // O tom do fogo atinge a intensidade máxima após um streak de 15 acertos.
    private static final int MAX_STREAK_FLAME_LEVEL = 15;

    // Uma thread única faz os pedidos de rede sem bloquear o ecrã.
    private final ExecutorService networkExecutor =
            Executors.newSingleThreadExecutor();

    private final Random random = new Random();

    private GameSession gameSession;
    private QuestionRepository questionRepository;
    // Marca se esta partida segue o modo de teste ativado no logo.
    private boolean godModeEnabled;
    private CountDownTimer screenTimer;
    private CountDownTimer questionTimer;
    // Gere os bips, os sons das respostas e as vibrações desta partida.
    private GameFeedbackEffects feedbackEffects;

    private PlayerStatsRepository playerStatsRepository;

    private Category currentCategory;
    private QuestionType currentType;
    private List<Question> pendingQuestions;

    private boolean animationFinished;
    private boolean questionsLoaded;
    private int finishedAnimations;
    private int firstDiceNumber;
    private int secondDiceNumber;
    private int secondsRemaining;
    // Guarda o último segundo que já emitiu bip para evitar repetições.
    private int lastCountdownFeedbackSecond = -1;

    // Mantém as respostas coloridas durante este intervalo antes de avançar.
    private static final long ANSWER_FEEDBACK_DELAY_MS = 3000L;

    // Agenda a passagem ao ecrã seguinte depois do feedback visual.
    private final Handler feedbackHandler = new Handler(Looper.getMainLooper());

    // Lembra o jogador de lançar os dados depois de 15 segundos sem atividade.
    private static final long SHAKE_IDLE_PROMPT_DELAY_MS = 15000L;
    private final Handler idlePromptHandler = new Handler(Looper.getMainLooper());
    private Runnable idlePromptRunnable;

    // Identifica cada lançamento para descartar respostas de pedidos/animações antigos.
    private volatile long rollAttemptId;
    private Future<?> questionLoadTask;

    // A Activity só aceita resultados e animações enquanto está visível.
    private boolean activityResumed;
    private boolean lifecyclePaused;
    private boolean sensorNeedsResume;
    private long pauseStartedAtElapsedRealtime;

    // Impede que o jogador responda várias vezes à mesma pergunta.
    private boolean answerLocked;


    // Cria a sessão e prepara o ecrã inicial dos dados.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        questionRepository = new RetrofitQuestionRepository();
        godModeEnabled = new DebugManager(this).isGodModeEnabled();

        playerStatsRepository = new PlayerStatsRepository(this);
        // Carrega os efeitos curtos em segundo plano antes da primeira pergunta.
        feedbackEffects = new GameFeedbackEffects(getApplicationContext());

        // Primeiro mostra e prepara o layout; depois ativa o sensor.
        showRollScreen();
        gameSession = new GameSession(
                readPlayer(),
                new AndroidShakeSensor(this),
                RetrofitLeaderboardRepository.getInstance(),
                this,
                godModeEnabled
        );
    }

    // Lê o jogador enviado pelo menu. Usa um jogador de demonstração se a partida for aberta diretamente.
    private Player readPlayer() {
        String id = getIntent().getStringExtra("player_id");
        String nickname = getIntent().getStringExtra("player_nickname");

        if (id == null) {
            id = "guest";
        }
        if (nickname == null) {
            nickname = "Jogador";
        }

        return new Player(id, nickname, 0);
    }

    // Recebe o evento da GameSession depois de um abanão acima do limite.
    @Override
    public void onRollDetected() {
        // O sensor chama este callback na interface Android.
        if (!activityResumed || isFinishing() || isDestroyed()) {
            // Evita iniciar uma ronda se um evento do sensor chegar durante a transição do ecrã.
            if (gameSession != null) {
                gameSession.onBlockLoadFailed();
                sensorNeedsResume = true;
            }
            return;
        }
        beginRollAndLoadQuestions();
    }

    // Sorteia os resultados, inicia os dados e pede três perguntas.
    private void beginRollAndLoadQuestions() {
        cancelIdlePrompt();
        final long attemptId = ++rollAttemptId;
        animationFinished = false;
        questionsLoaded = false;
        finishedAnimations = 0;
        pendingQuestions = null;

        // O primeiro dado escolhe uma categoria entre as seis disponíveis.
        firstDiceNumber = random.nextInt(6) + 1;
        currentCategory = categoryForDice(firstDiceNumber);

        // O segundo dado escolhe o formato: três faces para cada formato.
        secondDiceNumber = random.nextInt(6) + 1;
        currentType = secondDiceNumber <= 3
                ? QuestionType.TRUE_FALSE
                : QuestionType.MULTIPLE_CHOICE;
        final Category requestedCategory = currentCategory;
        final QuestionType requestedType = currentType;

        TextView status = findViewById(R.id.instructionText);
        status.setText(R.string.loading_questions);

        LottieAnimationView leftDice =
                findViewById(R.id.lottieDiceAnimation);
        LottieAnimationView rightDice =
                findViewById(R.id.lottieDiceAnimationInvertido);

        // Espera as duas animações antes de abrir o resumo.
        addAnimationEndListener(leftDice, attemptId);
        addAnimationEndListener(rightDice, attemptId);

        leftDice.playAnimation();
        rightDice.playAnimation();
        // Mantém um som em loop durante a animação e pára quando os dois dados terminarem.
        feedbackEffects.playDiceRoll();

        // Reabre a API e executa execute() fora da UI, sem bloquear a animação.
        questionRepository.resumeRequests();
        questionLoadTask = networkExecutor.submit(() -> {
            if (attemptId != rollAttemptId || Thread.currentThread().isInterrupted()) {
                return;
            }

            List<Question> loadedQuestions;
            try {
                loadedQuestions = questionRepository.getQuestions(
                        requestedCategory,
                        requestedType,
                        3
                );
            } catch (RuntimeException error) {
                // Uma falha inesperada segue o mesmo caminho de uma resposta sem perguntas.
                loadedQuestions = null;
            }
            final List<Question> result = loadedQuestions;

            runOnUiThread(() -> {
                // O identificador impede uma resposta antiga de aparecer depois de um bloqueio.
                if (attemptId != rollAttemptId
                        || !activityResumed
                        || isFinishing()
                        || isDestroyed()
                        || gameSession == null
                        || gameSession.getState() != GameState.LOADING_BLOCK) {
                    return;
                }
                questionLoadTask = null;

                if (result == null || result.size() != 3) {
                    handleQuestionLoadFailure();
                    return;
                }

                pendingQuestions = result;
                questionsLoaded = true;
                showSummaryWhenReady();
            });
        });
    }

    // Soma os finais das duas animações sem aceitar eventos de um lançamento cancelado.
    private void addAnimationEndListener(LottieAnimationView view, long attemptId) {
        view.addAnimatorListener(new Animator.AnimatorListener() {
            private boolean alreadyCounted;

            @Override
            public void onAnimationStart(Animator animation) { }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (alreadyCounted
                        || attemptId != rollAttemptId
                        || !activityResumed
                        || gameSession == null
                        || gameSession.getState() != GameState.LOADING_BLOCK) {
                    return;
                }

                alreadyCounted = true;
                finishedAnimations++;

                if (finishedAnimations == 2) {
                    animationFinished = true;
                    feedbackEffects.stopDiceRoll();
                    showSummaryWhenReady();
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) { }

            @Override
            public void onAnimationRepeat(Animator animation) { }
        });
    }

    // Só abre o resumo quando a animação e o pedido à API terminaram.
    private void showSummaryWhenReady() {
        if (!animationFinished || !questionsLoaded || pendingQuestions == null) {
            return;
        }

        gameSession.startNewBlock(pendingQuestions);
        showSummary();
    }

    // Mostra uma mensagem e volta a ouvir o sensor se o pedido falhar.
    private void handleQuestionLoadFailure() {
        feedbackEffects.stopDiceRoll();
        gameSession.onBlockLoadFailed();
        showRollScreen();
        // Deixa a mensagem visível e permite começar imediatamente outro lançamento.
        TextView instruction = findViewById(R.id.instructionText);
        instruction.setText(R.string.question_load_failed);
        gameSession.listenForNextRoll();
    }

    // Apresenta durante sete segundos o resultado e o estado da partida.
    // Depois do resumo, abre o ecrã da pergunta.
    private void showSummary() {
        setContentView(R.layout.activity_game_summary);

        // Encontra os textos correspondentes aos dois dados.
        TextView questionDie = findViewById(R.id.summaryQuestionDie);
        TextView questionType = findViewById(R.id.summaryQuestionType);
        TextView answerDie = findViewById(R.id.summaryAnswerDie);
        TextView answerType = findViewById(R.id.summaryAnswerType);

        // Encontra o progresso e os valores atuais da partida.
        TextView questionNumber = findViewById(R.id.summaryQuestionNumber);
        ProgressBar progress = findViewById(R.id.summaryProgress);
        TextView streak = findViewById(R.id.summaryStreak);
        ImageView streakFlame = findViewById(R.id.summaryStreakFlame);
        TextView score = findViewById(R.id.summaryScore);

        // Mostra o número e a categoria escolhidos pelo dado Question.
        questionDie.setText(getString(
                R.string.summary_die_number,
                firstDiceNumber
        ));
        questionType.setText(categoryName(currentCategory));

        // Mostra o número e o formato escolhidos pelo dado Answer.
        answerDie.setText(getString(
                R.string.summary_die_number,
                secondDiceNumber
        ));
        answerType.setText(formatName(currentType));

        // Mostra o número da pergunta, a pontuação e o streak atuais.
        int currentQuestionNumber = gameSession.getCurrentQuestionIndex() + 1;
        int totalQuestionCount = gameSession.getTotalQuestionCount();
        questionNumber.setText(getString(
                R.string.summary_question_counter,
                currentQuestionNumber,
                totalQuestionCount
        ));
        progress.setMax(totalQuestionCount);
        progress.setProgress(currentQuestionNumber);
        progress.setContentDescription(getString(
                R.string.summary_progress_description,
                currentQuestionNumber,
                totalQuestionCount
        ));
        int currentStreak = gameSession.getCurrentStreak();
        streak.setText(getString(
                R.string.streak_value,
                currentStreak
        ));
        streakFlame.setImageTintList(ColorStateList.valueOf(
                getStreakFlameColor(currentStreak)
        ));
        score.setText(getString(
                R.string.score_value,
                gameSession.getTotalScore()
        ));

        // Reinicia o temporizador, se já houver um ativo.
        if (screenTimer != null) {
            screenTimer.cancel();
        }

        // Mantém a pausa de sete segundos, sem apresentar segundos no ecrã.
        screenTimer = new CountDownTimer(SUMMARY_SECONDS * 1000L, 1000L) {
            @Override
            public void onTick(long millisUntilFinished) {
                // O temporizador continua a contar, mas não atualiza nenhum texto.
            }

            @Override
            public void onFinish() {
                gameSession.setPlayingState();
                showQuestion();
            }
        }.start();
    }

    // Aumenta gradualmente a cor do fogo à medida que o streak cresce.
    private int getStreakFlameColor(int streak) {
        if (streak <= 0) {
            return getColor(R.color.streak_flame_idle);
        }

        float intensity = Math.min(streak, MAX_STREAK_FLAME_LEVEL)
                / (float) MAX_STREAK_FLAME_LEVEL;

        return (Integer) new ArgbEvaluator().evaluate(
                intensity,
                getColor(R.color.streak_flame_warm),
                getColor(R.color.streak_flame_hot)
        );
    }


    // Mostra a pergunta e distribui os botões pelo espaço disponível.
    private void showQuestion() {
        Question question = gameSession.getCurrentQuestion();

        if (question == null) {
            handleQuestionLoadFailure();
            return;
        }

        setContentView(R.layout.activity_game_question);

        TextView number = findViewById(R.id.questionNumber);
        TextView timer = findViewById(R.id.questionTimer);
        TextView text = findViewById(R.id.questionText);
        LinearLayout answers = findViewById(R.id.answersContainer);

        // Prepara a interface para receber uma resposta nova.
        answerLocked = false;
        answers.removeAllViews();

        number.setText(getString(
                R.string.question_number,
                gameSession.getCurrentQuestionIndex() + 1,
                gameSession.getTotalQuestionCount()
        ));
        text.setText(decodeHtml(question.getText()));

        String correctAnswer = decodeHtml(question.getCorrectAnswer());

        for (String rawAnswer : question.getRespostasMisturadas()) {
            String answer = decodeHtml(rawAnswer);

            MaterialButton answerButton = new MaterialButton(this);
            answerButton.setText(answer);
            answerButton.setContentDescription(answer);
            answerButton.setAllCaps(false);
            answerButton.setTextSize(20);
            answerButton.setMinHeight(getResources().getDimensionPixelSize(
                    R.dimen.minimum_touch_target
            ));
            answerButton.setCornerRadius(getResources().getDimensionPixelSize(
                    R.dimen.button_corner_radius
            ));
            answerButton.setTypeface(ResourcesCompat.getFont(
                    this,
                    R.font.montserrat_bold
            ));
            answerButton.setGravity(Gravity.CENTER);
            answerButton.setTextColor(getColor(R.color.black));
            answerButton.setBackgroundTintList(
                    ColorStateList.valueOf(getColor(R.color.pink))
            );
            int horizontalPadding = getResources().getDimensionPixelSize(
                    R.dimen.answer_button_padding_horizontal
            );
            int verticalPadding = getResources().getDimensionPixelSize(
                    R.dimen.answer_button_padding_vertical
            );
            answerButton.setPadding(
                    horizontalPadding,
                    verticalPadding,
                    horizontalPadding,
                    verticalPadding
            );

            // Os botões partilham o espaço livre, mas podem crescer quando o texto está ampliado.
            LinearLayout.LayoutParams params =
                    new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            1f
                    );

            int spacing = getResources()
                    .getDimensionPixelSize(R.dimen.answer_button_spacing);
            params.setMargins(0, spacing, 0, spacing);

            answers.addView(answerButton, params);

            answerButton.setOnClickListener(view ->
                    submitAnswer(answer, correctAnswer)
            );
        }

        startQuestionTimer(timer);
    }

    // Inicia o temporizador de resposta e atualiza o texto a cada segundo.
    private void startQuestionTimer(TextView timerView) {
        startQuestionTimer(timerView, QUESTION_TIME_SECONDS);
    }

    // Retoma o temporizador com o tempo restante depois de a Activity voltar ao primeiro plano.
    private void startQuestionTimer(TextView timerView, int startingSeconds) {
        secondsRemaining = startingSeconds;
        lastCountdownFeedbackSecond = -1;
        timerView.setText(getString(R.string.seconds_value, secondsRemaining));
        timerView.setContentDescription(getString(
                R.string.question_timer_description,
                secondsRemaining
        ));

        if (questionTimer != null) {
            questionTimer.cancel();
        }

        questionTimer = new CountDownTimer(
                startingSeconds * 1000L,
                1000L
        ) {
            @Override
            public void onTick(long millisUntilFinished) {
                secondsRemaining = (int) ((millisUntilFinished + 999) / 1000);
                timerView.setText(getString(
                        R.string.seconds_value,
                        secondsRemaining
                ));
                timerView.setContentDescription(getString(
                        R.string.question_timer_description,
                        secondsRemaining
                ));

                // Dá um bip e uma vibração curta apenas nos segundos 3, 2 e 1.
                if (secondsRemaining > 0
                        && secondsRemaining <= 3
                        && secondsRemaining != lastCountdownFeedbackSecond) {
                    lastCountdownFeedbackSecond = secondsRemaining;
                    feedbackEffects.playCountdownTick();
                }
            }

            @Override
            public void onFinish() {
                timerView.setContentDescription(getString(
                        R.string.question_timer_description,
                        0
                ));
                handleQuestionTimeout();
            }
        }.start();
    }

    // Aplica a mesma penalização e feedback quando o tempo termina em primeiro plano ou ao regressar.
    private void handleQuestionTimeout() {
        if (answerLocked
                || gameSession == null
                || gameSession.getState() != GameState.PLAYING_QUESTION) {
            return;
        }

        answerLocked = true;
        secondsRemaining = 0;
        TextView timer = findViewById(R.id.questionTimer);
        if (timer != null) {
            timer.setText(getString(R.string.seconds_value, 0));
            timer.setContentDescription(getString(
                    R.string.question_timer_description,
                    0
            ));
        }
        feedbackEffects.playTimeoutFeedback();

        Question question = gameSession.getCurrentQuestion();
        if (question == null) {
            continueAfterAnswer();
            return;
        }

        String correctAnswer = decodeHtml(question.getCorrectAnswer());
        gameSession.processTimeout();
        showAnswerFeedback(correctAnswer);
        feedbackHandler.postDelayed(
                this::continueAfterAnswer,
                ANSWER_FEEDBACK_DELAY_MS
        );
    }


    // Regista a resposta, revela as cores e avança depois da pausa visual.
    private void submitAnswer(String selectedAnswer, String correctAnswer) {
        if (answerLocked
                || gameSession.getState() != GameState.PLAYING_QUESTION) {
            return;
        }

        answerLocked = true;

        if (questionTimer != null) {
            questionTimer.cancel();
        }

        // Usa a mesma regra de acerto da sessão para escolher o efeito sonoro.
        boolean answerWasCorrect = correctAnswer.equals(selectedAnswer)
                && secondsRemaining > 0;

        gameSession.processAnswer(
                selectedAnswer,
                correctAnswer,
                secondsRemaining
        );

        // Toca palmas no acerto ou uma vaia curta quando a resposta está errada.
        feedbackEffects.playAnswerFeedback(answerWasCorrect);

        showAnswerFeedback(correctAnswer);

        feedbackHandler.postDelayed(
                this::continueAfterAnswer,
                ANSWER_FEEDBACK_DELAY_MS
        );
    }


    // Mostra a resposta certa a verde e todas as erradas a vermelho.
    private void showAnswerFeedback(String correctAnswer) {
        LinearLayout answers = findViewById(R.id.answersContainer);

        for (int index = 0; index < answers.getChildCount(); index++) {
            View child = answers.getChildAt(index);

            if (!(child instanceof Button)) {
                continue;
            }

            Button answerButton = (Button) child;
            boolean isCorrect =
                    answerButton.getText().toString().equals(correctAnswer);

            int color = isCorrect
                    ? getColor(R.color.answer_correct_accessible)
                    : getColor(R.color.answer_incorrect_accessible);

            answerButton.setEnabled(false);
            answerButton.setTextColor(getColor(R.color.white));
            answerButton.setBackgroundTintList(
                    ColorStateList.valueOf(color)
            );
        }
    }



    // Escolhe o próximo ecrã conforme o estado atualizado da sessão.
    private void continueAfterAnswer() {
        if (gameSession.getState() == GameState.FINISHED) {
            showFinishedMessage();
        } else if (gameSession.getState() == GameState.WAITING_FOR_ROLL) {
            showRollScreen();
            gameSession.listenForNextRoll();
        } else {
            // Ainda há perguntas neste bloco; mostra o resumo da seguinte.
            showSummary();
        }
    }

    // Guarda os recordes normais e encaminha o jogador diretamente para o leaderboard.
    private void showFinishedMessage() {
        Player player = gameSession.getPlayer();

        // Não grava os valores artificiais do modo Deus nos recordes reais do jogador.
        if (!gameSession.isGodModeEnabled()) {
            playerStatsRepository.saveBestStats(
                    player.getId(),
                    gameSession.getTotalScore(),
                    gameSession.getBestStreak()
            );
        }

        // Abre imediatamente o ranking; a Activity trata do envio e depois carrega o Top 10.
        startActivity(LeaderboardActivity.createGameResultIntent(
                this,
                player,
                gameSession.getTotalScore(),
                gameSession.getBestStreak(),
                gameSession.isGodModeEnabled()
        ));
        finish();
    }

    // Volta a apresentar o ecrã dos dados.
    private void showRollScreen() {
        setContentView(R.layout.activity_roll_dices);
        TextView instruction = findViewById(R.id.instructionText);
        instruction.setText(R.string.shake_instruction);
        scheduleIdlePrompt();
    }

    // Agenda a mensagem motivacional enquanto a sessão continua à espera de um abanão.
    private void scheduleIdlePrompt() {
        cancelIdlePrompt();
        idlePromptRunnable = () -> {
            if (!activityResumed
                    || isFinishing()
                    || isDestroyed()
                    || (gameSession != null
                    && gameSession.getState() != GameState.WAITING_FOR_ROLL)) {
                return;
            }

            TextView instruction = findViewById(R.id.instructionText);
            if (instruction != null) {
                instruction.setText(R.string.shake_idle_motivation);
            }
        };
        idlePromptHandler.postDelayed(idlePromptRunnable, SHAKE_IDLE_PROMPT_DELAY_MS);
    }

    // Remove a mensagem pendente assim que o jogador abana ou deixa este ecrã.
    private void cancelIdlePrompt() {
        if (idlePromptRunnable != null) {
            idlePromptHandler.removeCallbacks(idlePromptRunnable);
            idlePromptRunnable = null;
        }
    }

    // Cancela o lançamento parcial, incluindo o pedido HTTP, animações e som dos dados.
    private void cancelInterruptedRoll() {
        ++rollAttemptId;

        if (questionLoadTask != null) {
            questionLoadTask.cancel(true);
            questionLoadTask = null;
        }
        if (questionRepository != null) {
            questionRepository.cancelPendingRequests();
        }

        LottieAnimationView leftDice = findViewById(R.id.lottieDiceAnimation);
        LottieAnimationView rightDice = findViewById(R.id.lottieDiceAnimationInvertido);
        if (leftDice != null) {
            leftDice.cancelAnimation();
        }
        if (rightDice != null) {
            rightDice.cancelAnimation();
        }

        if (feedbackEffects != null) {
            feedbackEffects.stopDiceRoll();
        }
        animationFinished = false;
        questionsLoaded = false;
        finishedAnimations = 0;
        pendingQuestions = null;

        // O próximo abanão começa uma tentativa limpa com um novo par de dados.
        if (gameSession != null) {
            gameSession.onBlockLoadFailed();
        }
    }

    // Converte o número do primeiro dado numa das seis categorias do jogo.
    private Category categoryForDice(int number) {
        Category[] categories = {
                Category.ART,
                Category.SCIENCE,
                Category.HISTORY,
                Category.CINEMA,
                Category.VIDEOGAMES,
                Category.MUSIC
        };
        return categories[number - 1];
    }

    // Converte a categoria interna numa etiqueta legível em português.
    private String categoryName(Category category) {
        switch (category) {
            case ART: return getString(R.string.category_art);
            case SCIENCE: return getString(R.string.category_science);
            case HISTORY: return getString(R.string.category_history);
            case CINEMA: return getString(R.string.category_cinema);
            case VIDEOGAMES: return getString(R.string.category_games);
            case MUSIC: return getString(R.string.category_music);
            default: return getString(R.string.category_general);
        }
    }

    // Converte o formato interno numa etiqueta visível.
    private String formatName(QuestionType type) {
        return type == QuestionType.TRUE_FALSE
                ? getString(R.string.format_true_false)
                : getString(R.string.format_multiple_choice);
    }

    // Descobre entidades HTML que podem vir no texto da OpenTDB.
    private String decodeHtml(String value) {
        return HtmlCompat.fromHtml(
                value == null ? "" : value,
                HtmlCompat.FROM_HTML_MODE_LEGACY
        ).toString();
    }

    // Pausa os temporizadores e cancela qualquer lançamento que ainda esteja a carregar.
    @Override
    protected void onPause() {
        activityResumed = false;
        lifecyclePaused = true;
        cancelIdlePrompt();
        feedbackHandler.removeCallbacksAndMessages(null);

        if (screenTimer != null) {
            screenTimer.cancel();
            screenTimer = null;
        }

        GameState stateWhenPaused = gameSession == null
                ? null
                : gameSession.getState();

        if (questionTimer != null) {
            questionTimer.cancel();
            questionTimer = null;
        }
        pauseStartedAtElapsedRealtime = stateWhenPaused == GameState.PLAYING_QUESTION
                ? SystemClock.elapsedRealtime()
                : 0L;

        if (gameSession != null) {
            gameSession.notifyAppMinimized();
        }

        if (stateWhenPaused == GameState.LOADING_BLOCK) {
            // Um pedido ou animação interrompidos nunca podem concluir-se ao regressar.
            cancelInterruptedRoll();
        }

        BackgroundMusicManager.getInstance().pause();
        super.onPause();
    }

    // Retoma o estado seguro da sessão, sem reaproveitar resultados de um lançamento interrompido.
    @Override
    protected void onResume() {
        super.onResume();
        activityResumed = true;
        BackgroundMusicManager.getInstance().resumeIfEnabled(this);

        if (!lifecyclePaused || gameSession == null) {
            // Recupera um evento de sensor recebido durante a transição inicial da Activity.
            if (sensorNeedsResume && gameSession != null) {
                sensorNeedsResume = false;
                gameSession.listenForNextRoll();
            }
            return;
        }
        lifecyclePaused = false;

        GameState stateBeforeRestore = gameSession.getState();
        long awayDurationMs = pauseStartedAtElapsedRealtime == 0L
                ? 0L
                : SystemClock.elapsedRealtime() - pauseStartedAtElapsedRealtime;
        pauseStartedAtElapsedRealtime = 0L;

        // A saída prolongada durante uma pergunta continua a aplicar a regra anti-batota existente.
        gameSession.notifyAppRestored();
        GameState restoredState = gameSession.getState();

        if (restoredState == GameState.PLAYING_QUESTION) {
            if (stateBeforeRestore == GameState.PLAYING_QUESTION) {
                // O relógio não executa callbacks em segundo plano; desconta-se o tempo decorrido.
                int elapsedSeconds = (int) ((awayDurationMs + 999L) / 1000L);
                secondsRemaining = Math.max(0, secondsRemaining - elapsedSeconds);
                if (secondsRemaining == 0) {
                    handleQuestionTimeout();
                } else {
                    TextView timer = findViewById(R.id.questionTimer);
                    if (timer != null) {
                        startQuestionTimer(timer, secondsRemaining);
                    }
                }
            }
            return;
        }

        if (restoredState == GameState.WAITING_FOR_ROLL) {
            showRollScreen();
            gameSession.listenForNextRoll();
            sensorNeedsResume = false;
        } else if (restoredState == GameState.IN_BREAK) {
            // Recomeça o resumo completo; o tempo não avança enquanto o ecrã está bloqueado.
            showSummary();
        } else if (restoredState == GameState.FINISHED) {
            showFinishedMessage();
        }
    }

    // Cancela tarefas, efeitos e sensores para impedir callbacks depois de a Activity fechar.
    @Override
    protected void onDestroy() {
        activityResumed = false;
        ++rollAttemptId;
        cancelIdlePrompt();

        if (screenTimer != null) {
            screenTimer.cancel();
        }
        if (questionTimer != null) {
            questionTimer.cancel();
        }
        if (gameSession != null) {
            gameSession.dispose();
        }
        if (questionLoadTask != null) {
            questionLoadTask.cancel(true);
            questionLoadTask = null;
        }
        if (questionRepository != null) {
            questionRepository.cancelPendingRequests();
        }
        if (feedbackEffects != null) {
            feedbackEffects.release();
        }

        networkExecutor.shutdownNow();
        feedbackHandler.removeCallbacksAndMessages(null);
        idlePromptHandler.removeCallbacksAndMessages(null);

        super.onDestroy();
    }
}
