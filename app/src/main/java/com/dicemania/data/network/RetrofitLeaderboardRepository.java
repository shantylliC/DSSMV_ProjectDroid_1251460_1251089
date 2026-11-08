package com.dicemania.data.network;

import com.dicemania.domain.model.Player;
import com.dicemania.domain.repository.LeaderboardRepository;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

// Este ficheiro é quem conduz todo o ciclo de processamento deste servidor. É o coraçao da camada de dados
// este ficheiro faz: abertura de portas de rede, converter ficheiros JSON , lidar com falhas de wifi, traduz o que vem do servidor (PlayerJson) para jogadores normais (Player) para que possa ser usado pelo ecra

// este ficheiro vai ligar-se ao LeaderboardRepository, pois o RetrofitLeaderboardRepository deve cumprir todas as regras, métodos lá contidos. assina o contrato para que sempre se cumpram
// também ao SupaBaseApi já que este é o menú dos pedidos de HTTP, permite que este se liga ao servidor
// Ao PlayerJson que é o formato em que as informaçoes devem vir e ir ao servidor
// e por último ao Player que acabará por ser o formato limpo que se vai entregar a aplicaçao para que possa ser usada

public class RetrofitLeaderboardRepository implements LeaderboardRepository { // contrato com o LeaderboardRepository, vai cumprir o que estiver dentro do ficheiro

    // Devolve o resultado do envio para permitir carregar o Top 10 só depois de gravar.
    public interface ScoreSubmissionCallback {
        void onComplete(boolean successful, String errorMessage);
    }

    // Mantém uma única configuração de Retrofit partilhada pelo jogo, menu e leaderboard.
    private static volatile RetrofitLeaderboardRepository instance;

    private SupaBaseApi supaBaseApi; // criamos um objeto do tipo SupaBaseApi para podermos usar as ferramentas de comunicaçao com o servidor mais facilmente ao longo da classe

    // Devolve a instância partilhada para evitar recriar o cliente de rede em cada ecrã.
    public static RetrofitLeaderboardRepository getInstance() {
        if (instance == null) {
            synchronized (RetrofitLeaderboardRepository.class) {
                if (instance == null) {
                    instance = new RetrofitLeaderboardRepository();
                }
            }
        }
        return instance;
    }

    public RetrofitLeaderboardRepository() { // temos o construtor, apenas arranque a aplicaçao vai ser criado este repositório. Vai criar o telefone e o tradutor que vao ligar a nossa aplicaçao ao servidor Supabase

        Retrofit retrofit = new Retrofit.Builder() // criamos uma varivel retrofit que vai ter um objeto dinamico. Este vai criar o nosso tal telefone, aquele que vai chamar ao servidor para poder pedir e receber o que pretende (Builder())

                .baseUrl("https://cgxabecnerfihjygossw.supabase.co/") // colocamos aqui a endereço de redr, o URL, do servidor para que o Retrofit possa saber a onde deve ir buscar e receber as informaçoes. Com o ficheiro SupaBAseApi este vai pegar nas pequenas partes dos endereçoes escritos lá e vai colá-los em frente deste para assim ir buscar o que precisa (seja pontuaçao, id ou nickname)

                .addConverterFactory(GsonConverterFactory.create()) // temos aqui o nosso tradutor automatico (como aparece no RetrofitQuestionRepository)
                // esta alinha pega na funçao addConverterFactory e invoca a funçao GsonConverterFactory que, quando receba as mensagens do servidor, vai fazer a traduçao de todo o texto, que vem no formato JSON, e vai traduzi-lo e separá-lo para os objetos que o nosso programa, o retrofit, vai poder usar e possa entender, ou seja, no formato predefinfo no PlayerJson
                // .create(): cria logo o nosso chip de traduçao com as definiçoes padrao todas feitas. É uma ferramenta pre definida pela google para isto. Usamos o create em vez do new
                // quando for pedido as informaçoes ao servidor, será chamado diretamente esta funçao

                .build(); // aqui cria-se o objeto completo (retrofit), tendo o URL e o tradutor incluido, guardando tudo isto

        supaBaseApi = retrofit.create(SupaBaseApi.class); // acaba por ser uma maquina que vai imprimir e pedir tudo o que vier desde o servidor, score, id, nickename (obtém-se estes links através dao ficheiro SupaBaseApi
        // retrofit.create: precisamos de criar algo com as informaçoes que temos va variavel retrofit
        // SupaBaseApi: vamos criar algo com base nas informaçoes deste ficheiro. Pedimos para que crie uma maquina que saiba fazer os pedidos que estao explicitos aí
        // .class: como a SupaBaseApi é um ficheiro de interfase, este pode ser considerado como um ficheiro imaginário, já que é um contrato de regras, logo nao tem corpo físico. Isto leva a que nao seja possivel criarmos um objeto a partir dele, entao, precisamos utilizar o operador .class que diz ao Java que nao está a dar um objeto real e vivo, está a dar uma definiçao estrutural deste ficheiro (Type reference). o Retrofit pega nisto e faz um Reflection, onde o código consegue ler-se a si próprio e analisa todas as anotaçoes do ficheiro

    }

    @Override // contrato a cumprir com o ficheiro LeaderboardRepository

    public void submitScore(String playerId, String nickname, int finalScore) {
        submitScore(playerId, nickname, finalScore, 0);
    }

    // Envia também o streak máximo para acompanhar a pontuação do jogador.
    @Override
    public void submitScore(
            String playerId,
            String nickname,
            int finalScore,
            int bestStreak
    ) {
        submitScore(playerId, nickname, finalScore, bestStreak, null);
    }

    // Envia o resultado e avisa quem chamou quando o servidor responder.
    public void submitScore(
            String playerId,
            String nickname,
            int finalScore,
            int bestStreak,
            ScoreSubmissionCallback submissionCallback
    ) {

        PlayerJson playerJson = new PlayerJson(
                playerId,
                nickname,
                finalScore,
                bestStreak
        ); // Formata os dados enviados para a tabela players.

        supaBaseApi.submitPlayerScore(playerJson).enqueue(new Callback<Void>() { //
            // supaBaseApi: precisamos dele para saber o endereço do servidor. É o único que sabe falar e tem a chave do servidor. Com ele abrimos a ligaçao com a SupaBase
            // .submitPlayerScore(playerJson): chamamos a funçao do ficheiro LeaderboardRepository que contém a variavel playerJson preenchida com as parametros de playerId, nickname, finalScore.
            // .enqueue: pega no que está atrás dele (supaBaseApi.submitPlayerScore(playerJson)), a chamada que temos de fazer ao servidor, e vai enviá-lo de forma assíncrona. Devemos faze-lo assim para que a main thread nao encrave e dê erro de a aplicaçao nao está a responder, e possa continuar a fazer outras tarefas enquanto se faz o pedido e receçao das informaçoes ao servidor (é uma especie de thread secundária)
            // new Callback<Void>(): serve como aviso, campainha, alarme, resposta da internet, que vai tocar quando a operaçao de contacto com o servidor seja realizada. Precisamos disto porque ao usar o .enqueue o código continua a executar pela frente e isto permite obter a informaçao de um código já executado

            // temos de escrever dois escenários que podem acontecer caso consigamos contactor ou nao com o servidor

            @Override
            public void onResponse(Call<Void> call, Response<Void> response) { // funçao que pertence à interfase Callback da biblioteca retrofit em caso se receba uma resposta do servidor (acesso ao servidor SupaBase)
                // Call<Void> call: É a nossa chamada original. O registo de chamadas, pedidos ao servidor. O retrofit entrega isto caso se queira inspecionar o que foi enviado (oo pedido feito. EX: se pode calcelar)
                // Response<Void> response: é a resposta do servidor (200 - success, 401 - Unauthorized, 500 - Server error)

                if (response.isSuccessful()) { // verificaçao se o servidor aceirou o pedido - 200

                    android.util.Log.d("SUPABASE", "Pontuaçao gravada com sucesso");
                    if (submissionCallback != null) {
                        submissionCallback.onComplete(true, null);
                    }

                } else {
                    String errorMessage = "HTTP " + response.code();
                    android.util.Log.e(
                            "SUPABASE",
                            "Erro ao gravar pontuação. Código HTTP: " + response.code()
                    );
                    if (submissionCallback != null) {
                        submissionCallback.onComplete(false, errorMessage);
                    }
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) { // funçao que pertence à interfase Callback da biblioteca retrofit em caso se receba uma resposta do servidor (acesso ao servidor SupaBase)
                // Throwable t: é o relatório do acidente, do proque houve um erro

                android.util.Log.d("SUPABASE", "Erro ao gravar pontuaçao: " + t.getMessage());
                if (submissionCallback != null) {
                    submissionCallback.onComplete(false, t.getMessage());
                }

            }
        });
    }

    @Override // contrato com o ficheiro LeaderboardRepository

    public void getTop10(LeaderboardCallback callback) { // para mostrarmos a tabela do top 10 chamamos a esta funçao
        // espera a que recebamos a informaçao do servidor
        // callback: o nosso estafeta de comunicaçao. Aquele que se vai comunicar com os restantes ficheiros

        supaBaseApi.getTop10Players().enqueue(new Callback<List<PlayerJson>>() { // supaBaseApi: precisamos dele para saber o endereço do servidor. É o único que sabe falar e tem a chave do servidor. Com ele abrimos a ligaçao com a SupaBase
            // .getTop10Player(): chamamos a funçao do ficheiro LeaderboardRepository
            // .enqueue: pega no que está atrás dele (supaBaseApi.getTop10Players()), a chamada que temos de fazer ao servidor, e vai enviá-lo de forma assíncrona. Devemos faze-lo assim para que a main thread nao encrave e dê erro de a aplicaçao nao está a responder, e possa continuar a fazer outras tarefas enquanto se faz o pedido e receçao das informaçoes ao servidor (é uma especie de thread secundária)
            // new Callback<List<PlayerJson>>(): serve como aviso, alarme, campainha, que vai avisar e guardar quando receber uma Lista de objetos do tipo PlayerJson. Vem o formato cru, sem traduzir, em formato JSON

            @Override
            public void onResponse(Call<List<PlayerJson>> call, Response<List<PlayerJson>> response) { // funçao que pertence a interfase Callback da biblioteca Retrofit em que vai receber o que for enviado do servidor
                // Call<List<PlayerJson>> call: Registo da chamada. É a nossa chamada original em que pedimos a lista dos 10 melhores jogadores
                // Response<List<PlayerJson>> response: É a resposta do servidor perante esse pedido de obter a lista do top 10 jogadores

                if (response.isSuccessful() && response.body() != null) { // se a resposta der sucesso, ou seja, o pedido foi corretamente feito e se a resposta nao veio vazia

                    List<Player> top10 = new ArrayList<>(); //criamos lista dinamica do tipo Player chamado top10. Preparamos já o recipiente limpo para meter os jogadores limpos (Player)
                    // a lista esta em response.body (suja)

                    for (PlayerJson playerJson : response.body()) { // por cada response.body() recebido dá-lhe o nome de playerJson

                        top10.add(new Player(
                                playerJson.getId(),
                                playerJson.getNickname(),
                                playerJson.getScore(),
                                playerJson.getBestStreak()
                        ));
                        // top10.add: adicionar na lista dinamica chamada de top10, os 3 dados puros
                        // para cada variavel do tipo Player vai ao objeto playerJson e retira-lhe o id, nickname e o score
                        //new Player: desses 3 dados puras constroi um Player limpo

                    }

                    callback.onSuccess(top10); // aqui já a lista de playerJson é ignorada e a lista de Player está cheia
                    // damos ao mensageiro, quem avisou e guardou o que tinhamos recebido, através da funçao isSuccess a lista do top10. Ao colocá-lo no isSuccess apertamos um "botao" em que o callback ja pode transportar, passar a lista dos top 10
                    // este vai levar isto ao ecra
                    // devolvemos isto através de um callback para pouparmos tempo. O return tem de devolver informaçao no instante e como o servidor pode demorar mis tempo em mandar a informaçao, para nao haver erros, bugs ou relentizaçao, é melhor o uso do callback, que espera a que toda a informaçao chegue e depois pasa tudo a outras funçoes

                }

                else {

                    callback.onError("Erro ao ler ranking: Código " + response.code()); // se o servidor recusaou o pedido, o código devolve uma mensagem de erro
                }
            }

            @Override
            public void onFailure(Call<List<PlayerJson>> call, Throwable t) { // se nao recebemos nada mandamos um erro

                callback.onError("Erro de internet: " + t.getMessage());

            }
        });
    }
}
