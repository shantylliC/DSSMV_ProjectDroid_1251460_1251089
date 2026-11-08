package com.dicemania.data.network;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;

//este ficheiro vai estar ligado ao PlayerJson, porque precisa de saber o formato exato em que os dados vêm da net
// vai ser usado pelo RetrofitLeaderboardRepository, pois o SupabaseApi nao faz nada sozinho por si só, o repositorio vai ser o cliente, quem vai chamar os métodos deste menu e vai pedir ao retrofit para os executar

// É como um menú. Este ficheiro existe para fazer a traduçao dos pedidos HTTP, vindos da pagina web da Supabase, com a biblioteca retrofit, numa linguagem que o nosso programa possa entender
// colocamos o que queremos em java e o retrofit vai manda-lo no formato para uma linguagem HTTP, ou seja, num pedido de rede

public interface SupaBaseApi { // contrato, a tal regra que se dev cumprir sempre que se quer fazer um pedido à página da Supabase

    String API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImNneGFiZWNuZXJmaWhqeWdvc3N3Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTMxMzM4NTAsImV4cCI6MjEwODcwOTg1MH0.pV_btWeQrLhi6-HfSc6WmWkxS3qjoilTohP3FBNbScw";
    // a nossa chave, cartao de acesso ao Supabase, é o código do nosso projeto. Dá permissao a nossa aplicaçao que podemos entrar no servidor

    @Headers ({ // é uma mensagem que mandamos juntamente com o pedido. Serve para poder comprovar que somos nós, e nao uns desconhecidos, a entrar no servidor
            // serve como cartao de cidadao e comprovativo de autenticidade

            "apikey: " + API_KEY, // regra especifica de quem criou o Supabase
            //o servidor pergunta qual é o nosso projeto, a qual queremos aceder e nós enviamos a nossa API_KEY

            "Authorization: Bearer " + API_KEY // é o formato standard de segurança da internet. Uma autorizaçao legítima
            // isto avisa ao servidor que lhe será entregue uma prova de identidade, uma verificaçao
            // permite o acesso ao servidor quem tiver a API_KEY
    })


    @GET ("rest/v1/players?select=id,nickname,score,best_streak&order=score.desc,best_streak.desc,nickname.asc&limit=10") // o método GET lê os dez melhores perfis sem alterar a tabela
    // rest/v1/players: ir atabela de Players
    // select: devolve ID, nickname, score e streak máximo para a aplicação.
    // order: põe a pontuação maior primeiro e usa streak/nickname para desempatar.
    // &limit=10: traz apenas os 10 primeiros

    Call<List<PlayerJson>> getTop10Players(); // chamada assincrona, vai mandar as coisas no momento que for necessário
    // quando forem recebidos os dados do servidor, a tal chamada, estes seram guardados numa lista do tipo PlayerJson, esta lista terá o nome de getTop10Players
    // obtemos 10 jogadores diferentes

    @Headers ({ // Esta mensagem permite inserir um jogador ou atualizar o registo já existente com o mesmo ID.

            "apikey: " + API_KEY,
            "Authorization: Bearer " + API_KEY,
            "Content-Type: application/json", // explica que o que vamos a mandar é um ficheiro de dados JSON estruturado como aparece no PLayerJson
            "Prefer: resolution=merge-duplicates,return=minimal" // Com on_conflict=id, atualiza a linha existente e não devolve um corpo desnecessário.
    })

    @POST ("rest/v1/players?on_conflict=id") // O ID identifica unicamente o jogador para o upsert não criar duplicados.

    Call<Void> submitPlayerScore(@Body PlayerJson player); // fazemos uma chamada a espera de que o servidor atenda e faça o que pedimos
    // @Body: diz ao retrofit para que pegue no objeto PLayerJson e o converte em linguagem JSON
    // player: o jogador que estiver a jogar no dispositivo naquele momento
    // É isto que se vai ativar mal o jogador acabe a partida. O servidor vai receber as suas informaçoes no formato já preescrito no PlayerJson
}
