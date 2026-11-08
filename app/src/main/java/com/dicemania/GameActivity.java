package com.dicemania;

import android.animation.Animator;
import android.animation.ArgbEvaluator;
import android.app.AlertDialog;
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
import android.widget.Toast;
import android.content.res.ColorStateList;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.text.HtmlCompat;
import androidx.core.content.res.ResourcesCompat;
import com.google.android.material.button.MaterialButton;

import com.airbnb.lottie.LottieAnimationView;
import com.dicemania.data.network.RetrofitQuestionRepository;
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
import com.dicemania.data.sound.GameFeedbackEffects;

import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

    // Impede que o jogador responda várias vezes à mesma pergunta.
    private boolean answerLocked;


    // Cria a sessão e prepara o ecrã inicial dos dados.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        questionRepository = new RetrofitQuestionRepository();

        playerStatsRepository = new PlayerStatsRepository(this);
        // Carrega os efeitos curtos em segundo plano antes da primeira pergunta.
        feedbackEffects = new GameFeedbackEffects(getApplicationContext());

        // Primeiro mostra e prepara o layout; depois ativa o sensor.
        showRollScreen();
        gameSession = new GameSession(
                readPlayer(),
                new AndroidShakeSensor(this),
                null,
                this
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
        beginRollAndLoadQuestions();
    }

    //Sorteia os resultados, inicia os dados e pede três perguntas.
    private void beginRollAndLoadQuestions() {
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

        TextView status = findViewById(R.id.instructionText);
        status.setText(R.string.loading_questions);

        LottieAnimationView leftDice =
                findViewById(R.id.lottieDiceAnimation);
        LottieAnimationView rightDice =
                findViewById(R.id.lottieDiceAnimationInvertido);

        // Espera as duas animações antes de abrir o resumo.
        addAnimationEndListener(leftDice);
        addAnimationEndListener(rightDice);

        leftDice.playAnimation();
        rightDice.playAnimation();

        // RetrofitQuestionRepository usa execute(), por isso corre fora da UI.
        networkExecutor.execute(() -> {
            List<Question> result = questionRepository.getQuestions(
                    currentCategory,
                    currentType,
                    3
            );

            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }

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

    // Soma os finais das duas animações. O loop tem de estar desligado no XML para este evento ocorrer.
    private void addAnimationEndListener(LottieAnimationView view) {
        view.addAnimatorListener(new Animator.AnimatorListener() {
            private boolean alreadyCounted;

            @Override
            public void onAnimationStart(Animator animation) { }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (alreadyCounted) {
                    return;
                }

                alreadyCounted = true;
                finishedAnimations++;

                if (finishedAnimations == 2) {
                    animationFinished = true;
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
        Toast.makeText(
                this,
                R.string.question_load_failed,
                Toast.LENGTH_LONG
        ).show();

        gameSession.onBlockLoadFailed();
        showRollScreen();
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
        questionNumber.setText(getString(
                R.string.summary_question_counter,
                gameSession.getCurrentQuestionIndex() + 1,
                15
        ));
        progress.setMax(15);
        progress.setProgress(gameSession.getCurrentQuestionIndex() + 1);
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
                gameSession.getCurrentQuestionIndex() + 1
        ));
        text.setText(decodeHtml(question.getText()));

        String correctAnswer = decodeHtml(question.getCorrectAnswer());

        for (String rawAnswer : question.getRespostasMisturadas()) {
            String answer = decodeHtml(rawAnswer);

            MaterialButton answerButton = new MaterialButton(this);
            answerButton.setText(answer);
            answerButton.setAllCaps(false);
            answerButton.setTextSize(20);
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
            answerButton.setPadding(20, 12, 20, 12);

            // Altura zero e peso um: todos os botões dividem igualmente o espaço.
            LinearLayout.LayoutParams params =
                    new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            0,
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
        secondsRemaining = QUESTION_TIME_SECONDS;
        lastCountdownFeedbackSecond = -1;

        if (questionTimer != null) {
            questionTimer.cancel();
        }

        questionTimer = new CountDownTimer(
                QUESTION_TIME_SECONDS * 1000L,
                1000L
        ) {
            @Override
            public void onTick(long millisUntilFinished) {
                secondsRemaining = (int) ((millisUntilFinished + 999) / 1000);
                timerView.setText(getString(
                        R.string.seconds_value,
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
                if (answerLocked
                        || gameSession.getState() != GameState.PLAYING_QUESTION) {
                    return;
                }

                answerLocked = true;
                secondsRemaining = 0;

                // Assinala o fim do tempo com um alarme curto e uma vibração longa.
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
                        GameActivity.this::continueAfterAnswer,
                        ANSWER_FEEDBACK_DELAY_MS
                );
            }
        }.start();
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
                    ? getColor(R.color.green2)
                    : getColor(R.color.red);

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

    // Mostra o resultado final da partida.
    private void showFinishedMessage() {

        // Guarda os recordes desta partida associados ao ID do jogador.
        Player player = gameSession.getPlayer();
        playerStatsRepository.saveBestStats(
                player.getId(),
                gameSession.getTotalScore(),
                gameSession.getBestStreak()
        );

        new AlertDialog.Builder(this)
                .setTitle(R.string.game_finished)
                .setMessage(getString(
                        R.string.final_score,
                        gameSession.getTotalScore()
                ))
                .setCancelable(false)
                .setPositiveButton(R.string.close, (dialog, which) -> finish())
                .show();
    }

    // Volta a apresentar o ecrã dos dados.
    private void showRollScreen() {
        setContentView(R.layout.activity_roll_dices);
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

    // Avisa a GameSession quando a aplicação vai para segundo plano.
    @Override
    protected void onPause() {
        if (gameSession != null) {
            gameSession.notifyAppMinimized();
        }
        super.onPause();
    }

    // Reavalia a penalização anti-batota ao regressar à aplicação.
    @Override
    protected void onResume() {
        super.onResume();

        if (gameSession != null) {
            GameState before = gameSession.getState();
            gameSession.notifyAppRestored();

            if (before == GameState.PLAYING_QUESTION
                    && gameSession.getState() != GameState.PLAYING_QUESTION) {
                if (questionTimer != null) {
                    questionTimer.cancel();
                }
                continueAfterAnswer();
            }
        }
    }

    // Liberta o sensor, os temporizadores e a thread de rede ao fechar.
    @Override
    protected void onDestroy() {
        if (screenTimer != null) {
            screenTimer.cancel();
        }
        if (questionTimer != null) {
            questionTimer.cancel();
        }
        if (gameSession != null) {
            gameSession.dispose();
        }
        if (feedbackEffects != null) {
            feedbackEffects.release();
        }

        networkExecutor.shutdownNow();
        feedbackHandler.removeCallbacksAndMessages(null);

        super.onDestroy();
    }
}
