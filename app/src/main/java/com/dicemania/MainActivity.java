package com.dicemania;

import android.os.Bundle;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

// --- IMPORTS Perguntas e Sensor ---
import com.dicemania.domain.model.Category;
import com.dicemania.domain.model.Question;
import com.dicemania.data.network.RetrofitQuestionRepository;
import com.dicemania.domain.model.QuestionType;
import com.dicemania.data.sensor.AndroidShakeSensor;
import com.dicemania.domain.model.GameSession;
import com.dicemania.domain.model.Player;

// --- NOVOS IMPORTS SharedPreferences e Supabase ---
import com.dicemania.data.local.SharedPreferencePlayerRespository;
import com.dicemania.data.network.RetrofitLeaderboardRepository;
import com.dicemania.domain.repository.LeaderboardRepository;

import java.util.List;
import java.util.UUID; // Para gerar um ID falso no teste

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // A tag unificada para veres tudo de seguida no Logcat
        String TAG = "TESTE_DICEMANIA";


        // ==========================================================
        // TESTE 1: SHARED PREFERENCES (MEMÓRIA LOCAL DO TELEMÓVEL)
        // ==========================================================
        Log.d(TAG, "--- 1. A iniciar Teste Local (Memória) ---");
        SharedPreferencePlayerRespository localRepo = new SharedPreferencePlayerRespository(this);

        // 1.A. Criar um jogador de teste e guardar no telemóvel
        Player jogadorMemoria = new Player(UUID.randomUUID().toString(), "MestreDosDados", 0);
        localRepo.saveLocalPlayer(jogadorMemoria);
        Log.d(TAG, "Jogador guardado no telemóvel.");

        // 1.B. Ler o jogador do telemóvel para ver se gravou bem
        Player jogadorLido = localRepo.getLocalPlayer();
        if (jogadorLido != null) {
            Log.d(TAG, "SUCESSO LOCAL! Lido: " + jogadorLido.getNickname() + " | ID: " + jogadorLido.getId());
        } else {
            Log.d(TAG, "ERRO LOCAL: O jogador retornou null.");
        }


        // ==========================================================
        // TESTE 2: SENSOR DE ABANÃO (MOTOR FÍSICO)
        // ==========================================================
        Log.d(TAG, "--- 2. A iniciar Teste do Sensor e GameSession ---");

        // Em vez de criarmos o "Visitante", vamos usar o jogadorLido do Teste 1! Fica mais realista.
        Player jogadorTeste = (jogadorLido != null) ? jogadorLido : new Player("1", "Visitante", 0);

        AndroidShakeSensor motorFisico = new AndroidShakeSensor(this);

        Log.d(TAG, "--- 3. A iniciar Teste do Supabase (Leaderboard) ---");
        RetrofitLeaderboardRepository redeRepo = new RetrofitLeaderboardRepository();
        GameSession sessaoTeste = new GameSession(jogadorTeste, motorFisico, redeRepo);
        Log.d(TAG, "Motor físico foi ligado para o jogador: " + jogadorTeste.getNickname());


        // ==========================================================
        // TESTE 3: SUPABASE (REDE - LEADERBOARD)
        // ==========================================================


        // 3.A. Submeter uma pontuação de teste (Ex: 120 pontos)
        redeRepo.submitScore(jogadorTeste.getId(), jogadorTeste.getNickname(), 120);
        Log.d(TAG, "Pedido de submissão enviado. A aguardar o Supabase...");

        // 3.B. Pedir o Top 10
        redeRepo.getTop10(new LeaderboardRepository.LeaderboardCallback() {
            @Override
            public void onSuccess(List<Player> top10) {
                Log.d(TAG, "SUCESSO SUPABASE! Recebido Top 10:");
                for (int i = 0; i < top10.size(); i++) {
                    Player p = top10.get(i);
                    Log.d(TAG, (i+1) + "º Lugar: " + p.getNickname() + " - Score: " + p.getPlayerScore());
                }
            }

            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "ERRO NO SUPABASE: " + errorMessage);
            }
        });


        // ==========================================================
        // TESTE 4: OPENTDB API (PERGUNTAS)
        // ==========================================================
        Log.d(TAG, "--- 4. A iniciar Teste da API de Perguntas (OpenTDB) ---");

        // Mantemos a Thread aqui em baixo para não travar os processos acima
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    RetrofitQuestionRepository repository = new RetrofitQuestionRepository();
                    List<Question> perguntasLimpas = repository.getQuestions(Category.VIDEOGAMES, QuestionType.MULTIPLE_CHOICE, 3);

                    for (Question q : perguntasLimpas) {
                        Log.d(TAG, "PERGUNTA: " + q.getText());
                        Log.d(TAG, "RESPOSTAS BARALHADAS: " + q.getRespostasMisturadas().toString());
                        Log.d(TAG, "CERTA: " + q.getCorrectAnswer());
                        Log.d(TAG, "--------------------------------------------------");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "ERRO FATAL NA API DE PERGUNTAS: " + e.getMessage());
                }
            }
        }).start();

    }
}