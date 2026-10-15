package com.dicemania;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.util.Log;
import com.dicemania.domain.model.Category;
import com.dicemania.domain.model.Question;
import com.dicemania.data.network.RetrofitQuestionRepository;
import com.dicemania.domain.model.QuestionType;

import java.util.List;


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


                // --- DAQUI PARA BAIXO É O NOSSO TESTE ---
                Log.d("DiceManiaTest", "A arrancar a app. A iniciar o teste da API...");

                // Usamos uma Background Thread para não bloquear a Main Thread
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            // 1. Instanciar o teu repositório
                            RetrofitQuestionRepository repository = new RetrofitQuestionRepository();

                            // 2. Pedir 3 perguntas da API
                            // ATENÇÃO: Se a tua função getQuestions pedir parâmetros diferentes, ajusta aqui!
                            List<Question> perguntasLimpas = repository.getQuestions(Category.VIDEOGAMES , QuestionType.MULTIPLE_CHOICE, 3);

                            // 3. Imprimir o resultado na consola
                            for (Question q : perguntasLimpas) {
                                Log.d("DiceManiaTest", "PERGUNTA: " + q.getText());
                                Log.d("DiceManiaTest", "RESPOSTAS BARALHADAS: " + q.getRespostasMisturadas().toString());
                                Log.d("DiceManiaTest", "CERTA: " + q.getCorrectAnswer());
                                Log.d("DiceManiaTest", "--------------------------------------------------");
                            }

                        } catch (Exception e) {
                            // Se der erro (ex: falta de net), imprime o erro a vermelho
                            Log.e("DiceManiaTest", "ERRO FATAL NA API: " + e.getMessage());
                        }
                    }
                }).start();
                // --- FIM DO NOSSO TESTE ---

        }

    }
