package com.dicemania;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.dicemania.data.network.RetrofitLeaderboardRepository;
import com.dicemania.data.sound.BackgroundMusicManager;
import com.dicemania.domain.model.Player;
import com.dicemania.domain.repository.LeaderboardRepository;

import java.util.ArrayList;
import java.util.List;

// Carrega o Top 10 online e apresenta os jogadores pela ordem recebida da base de dados.
public class LeaderboardActivity extends AppCompatActivity {

    // Identificadores para os dados de resultado enviados pela GameActivity.
    private static final String EXTRA_GAME_RESULT = "leaderboard_game_result";
    private static final String EXTRA_PLAYER_ID = "leaderboard_player_id";
    private static final String EXTRA_PLAYER_NICKNAME = "leaderboard_player_nickname";
    private static final String EXTRA_FINAL_SCORE = "leaderboard_final_score";
    private static final String EXTRA_BEST_STREAK = "leaderboard_best_streak";
    private static final String EXTRA_GOD_MODE = "leaderboard_god_mode";

    // Guarda as linhas recebidas para o adaptador atualizar a lista sem a recriar.
    private final ArrayList<Player> topPlayers = new ArrayList<>();

    // Referências aos elementos visuais do layout do ranking.
    private ListView leaderboardList;
    private View loadingPanel;
    private TextView statusMessage;
    private TextView gameResultMessage;
    private Button retryButton;

    // Liga os objetos Player às linhas visuais do Top 10.
    private ArrayAdapter<Player> adapter;

    // Faz as chamadas de leitura e escrita do ranking Supabase.
    private RetrofitLeaderboardRepository leaderboardRepository;

    // Guarda os dados da partida para os enviar antes de pedir o Top 10.
    private boolean openedAfterGame;
    private boolean godModeResult;
    private boolean scoreSubmitted;
    private String resultPlayerId;
    private String resultNickname;
    private int resultScore;
    private int resultBestStreak;

    // Cria o Intent usado no fim da partida para mostrar o ranking e o resultado do jogador.
    public static Intent createGameResultIntent(
            Context context,
            Player player,
            int finalScore,
            int bestStreak,
            boolean godModeEnabled
    ) {
        Intent intent = new Intent(context, LeaderboardActivity.class);
        intent.putExtra(EXTRA_GAME_RESULT, true);
        intent.putExtra(EXTRA_PLAYER_ID, player.getId());
        intent.putExtra(EXTRA_PLAYER_NICKNAME, player.getNickname());
        intent.putExtra(EXTRA_FINAL_SCORE, finalScore);
        intent.putExtra(EXTRA_BEST_STREAK, bestStreak);
        intent.putExtra(EXTRA_GOD_MODE, godModeEnabled);
        return intent;
    }

    // Prepara a lista e inicia o pedido assíncrono ao Supabase.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leaderboard);

        leaderboardList = findViewById(R.id.leaderboardList);
        loadingPanel = findViewById(R.id.leaderboardLoadingPanel);
        statusMessage = findViewById(R.id.leaderboardStatus);
        gameResultMessage = findViewById(R.id.leaderboardGameResultMessage);
        retryButton = findViewById(R.id.leaderboardRetryButton);
        leaderboardRepository = RetrofitLeaderboardRepository.getInstance();

        // Lê o resultado opcional; ao abrir pelas definições estes extras não existem.
        openedAfterGame = getIntent().getBooleanExtra(EXTRA_GAME_RESULT, false);
        godModeResult = getIntent().getBooleanExtra(EXTRA_GOD_MODE, false);
        resultPlayerId = getIntent().getStringExtra(EXTRA_PLAYER_ID);
        resultNickname = getIntent().getStringExtra(EXTRA_PLAYER_NICKNAME);
        resultScore = getIntent().getIntExtra(EXTRA_FINAL_SCORE, 0);
        resultBestStreak = getIntent().getIntExtra(EXTRA_BEST_STREAK, 0);

        adapter = createLeaderboardAdapter();
        leaderboardList.setAdapter(adapter);

        // Permite repetir a consulta quando falhar por falta de rede.
        retryButton.setOnClickListener(view -> loadTop10());

        loadTop10();
    }

    // Retoma a faixa ambiente quando o ranking fica visível.
    @Override
    protected void onResume() {
        super.onResume();
        BackgroundMusicManager.getInstance().resumeIfEnabled(this);
    }

    // Pausa a música quando a Activity deixa de estar em primeiro plano.
    @Override
    protected void onPause() {
        BackgroundMusicManager.getInstance().pause();
        super.onPause();
    }

    // Pede os dez melhores jogadores sem bloquear a interface.
    private void loadTop10() {
        showLoading();

        // No fim de uma partida, espera a gravação antes de consultar o ranking atualizado.
        if (openedAfterGame && !godModeResult && !scoreSubmitted) {
            leaderboardRepository.submitScore(
                    resultPlayerId,
                    resultNickname,
                    resultScore,
                    resultBestStreak,
                    (successful, errorMessage) -> {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        if (!successful) {
                            showError(errorMessage);
                            return;
                        }

                        scoreSubmitted = true;
                        fetchTop10();
                    }
            );
            return;
        }

        fetchTop10();
    }

    // Consulta os dez primeiros apenas depois de concluir o envio, se veio do fim de jogo.
    private void fetchTop10() {
        leaderboardRepository.getTop10(new LeaderboardRepository.LeaderboardCallback() {
            @Override
            public void onSuccess(List<Player> players) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }

                topPlayers.clear();

                if (players != null) {
                    int count = Math.min(players.size(), 10);
                    topPlayers.addAll(players.subList(0, count));
                }

                adapter.notifyDataSetChanged();
                showResults();
                showGameResultMessage();
            }

            @Override
            public void onError(String errorMessage) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }

                showError(errorMessage);
            }
        });
    }

    // Mostra a mensagem de resultado só quando a consulta confirma a posição atual do jogador.
    private void showGameResultMessage() {
        if (!openedAfterGame || resultPlayerId == null) {
            gameResultMessage.setVisibility(View.GONE);
            return;
        }

        boolean playerIsInTop10 = false;
        for (Player player : topPlayers) {
            if (resultPlayerId.equals(player.getId())) {
                playerIsInTop10 = true;
                break;
            }
        }

        gameResultMessage.setText(playerIsInTop10
                ? R.string.leaderboard_player_is_dice_master
                : R.string.leaderboard_player_keep_rolling);
        gameResultMessage.setVisibility(View.VISIBLE);
    }

    // Mostra o indicador enquanto o pedido de rede está em curso.
    private void showLoading() {
        leaderboardList.setVisibility(View.GONE);
        statusMessage.setVisibility(View.GONE);
        loadingPanel.setVisibility(View.VISIBLE);
        retryButton.setVisibility(View.GONE);
    }

    // Mostra a tabela ou uma mensagem de lista vazia depois da resposta.
    private void showResults() {
        loadingPanel.setVisibility(View.GONE);
        retryButton.setVisibility(View.GONE);

        if (topPlayers.isEmpty()) {
            leaderboardList.setVisibility(View.GONE);
            statusMessage.setText(R.string.leaderboard_empty);
            statusMessage.setVisibility(View.VISIBLE);
        } else {
            statusMessage.setVisibility(View.GONE);
            leaderboardList.setVisibility(View.VISIBLE);
        }
    }

    // Apresenta o erro e deixa o jogador repetir o pedido.
    private void showError(String errorMessage) {
        loadingPanel.setVisibility(View.GONE);
        leaderboardList.setVisibility(View.GONE);
        // Mantém a mensagem pedida simples e consistente; os detalhes técnicos ficam no callback.
        statusMessage.setText(R.string.leaderboard_load_failed);
        statusMessage.setVisibility(View.VISIBLE);
        retryButton.setVisibility(View.VISIBLE);
    }

    // Cria cada linha do ranking com a posição, o nome, o traço e a pontuação.
    private ArrayAdapter<Player> createLeaderboardAdapter() {
        return new ArrayAdapter<Player>(this, 0, topPlayers) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View row = convertView;

                if (row == null) {
                    row = getLayoutInflater().inflate(
                            R.layout.item_leaderboard,
                            parent,
                            false
                    );
                }

                ImageView trophy = row.findViewById(R.id.leaderboardTrophy);
                TextView rank = row.findViewById(R.id.leaderboardRank);
                TextView nickname = row.findViewById(R.id.leaderboardNickname);
                TextView score = row.findViewById(R.id.leaderboardScore);

                Player player = getItem(position);
                if (player == null) {
                    return row;
                }

                // O objeto Player também conserva o ID e o streak recebido, mesmo que a linha só mostre nome e pontos.
                nickname.setText(player.getNickname());
                score.setText(getString(
                        R.string.leaderboard_score,
                        player.getPlayerScore()
                ));
                rank.setText(String.valueOf(position + 1));

                // Colore o troféu dos três primeiros lugares e reserva espaço nos restantes.
                if (position < 3) {
                    int medalColor;

                    if (position == 0) {
                        medalColor = R.color.gold3;
                    } else if (position == 1) {
                        medalColor = R.color.silver;
                    } else {
                        medalColor = R.color.bronze;
                    }

                    trophy.setVisibility(View.VISIBLE);
                    trophy.setContentDescription(getString(
                            R.string.leaderboard_trophy_description,
                            position + 1
                    ));
                    trophy.setImageTintList(
                            ColorStateList.valueOf(getColor(medalColor))
                    );
                } else {
                    trophy.setVisibility(View.INVISIBLE);
                }

                return row;
            }
        };
    }
}
