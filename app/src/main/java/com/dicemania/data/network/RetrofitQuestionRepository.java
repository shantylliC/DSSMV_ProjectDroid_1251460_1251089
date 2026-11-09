package com.dicemania.data.network;

import com.dicemania.domain.model.Category;
import com.dicemania.domain.model.Question;
import com.dicemania.domain.model.QuestionType;
import com.dicemania.domain.repository.QuestionRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitQuestionRepository implements QuestionRepository {

    private OpenTDBapi api; // é uma varivel do ficheiro OpenTDBapi que vai guardar a ligaçao á internet já configurada com a interface - ver ficheiro openTDBapi
    // Mantém referência ao pedido síncrono para o poder cancelar ao bloquear o ecrã.
    private volatile Call<OpenTDBresponse> activeQuestionCall;
    // Impede que uma tarefa atrasada inicie um novo pedido depois de ser cancelada.
    private boolean requestsCancelled;

    // 1. CONSTRUTOR: Arrancar o Motor do Retrofit e definir a Morada Principal
    public RetrofitQuestionRepository() { // cria-se um construtor para que cada variavel api tenha o formato e conteudos necessários para que o ficheiro gameSession tenha tudo o necessario para funcionar, quando este for passado para lá

        Retrofit retrofit = new Retrofit.Builder() //utilizamos uma variável do tipo retrofit (da propria biblioteca retrofit) e vamos criar uma variável dinâmica que será uma especie de construtor que vai permitir montar passo a passo cada parte, texto, que vamos precisar do API

                .baseUrl("https://opentdb.com/") // aqui é colocado a maior parte do link da API, esta funçao serve para guardar a morada principal e fixa do servidor. O retrofit precisa desta morada inicial para poder começar a procurar – o destino final deste link esta na OpentTDBapi (openTBD.hbh)

                .addConverterFactory(GsonConverterFactory.create()) // esta alinha pega na funçao addConverterFactory e invoca a funçao GsonConverterFactory que, quando receba as mensagens da API, vai fazer a traduçao de todo o texto, que vem no formato JSON, e vai traduzi-lo e separá-lo para os objetos que o nosso programa vai usar e possa entender (category, type,…)

                .build(); // constroi tudo a partir das coisas ja feitas atrás

        api = retrofit.create(OpenTDBapi.class);  //esta variavel API vai guardar ao nosso construtor acima instanciado, vai guardar o produto final que a máquina do Retrofit acabou de construir . Vai criar um objeto do tipo OpenTDBapi e vai fazer o que foi explícito nesse ficheiro, o tal contrato feito com a interfase, este vai ter a responsabilidade de fazer o que está lá a acontecer. É este que vai fazer o pedido diretamente ao servidor, trazendo tudo o que for preciso. É como o telefone do programa, os ficheiros vão pedir que contacte com o servidor e este devolverá o que lhe foi dito pelo mesmo.
        // acaba por ser uma maquina que vai imprimir e pedir tudo o que vier desde o servidor, perguntas e respostas (obtém-se estes links através do ficheiro OpenTDBapi)
        // retrofit.create: precisamos de criar algo com as informaçoes que temos va variavel retrofit
        // OpenTDBapi: vamos criar algo com base nas informaçoes deste ficheiro. Pedimos para que crie uma maquina que saiba fazer os pedidos que estao explicitos aí
        // .class: como a SupaBaseApi é um ficheiro de interfase, este pode ser considerado como um ficheiro imaginário, já que é um contrato de regras, logo nao tem corpo físico. Isto leva a que nao seja possivel criarmos um objeto a partir dele, entao, precisamos utilizar o operador .class que diz ao Java que nao está a dar um objeto real e vivo, está a dar uma definiçao estrutural deste ficheiro (Type reference). o Retrofit pega nisto e faz um Reflection, onde o c+odigo consegue ler-se a si próprio e analisa todas as anotaçoes do ficheiro

    }

    @Override // pega na funçao, contrato, explicito no ficheiro openTDBapi e pode ser usada neste ficheiro

    public List<Question> getQuestions(Category category, QuestionType type, int amount) { // List<Question>: a mesma lista criada no ficheiro openTDBapi. Daqui, quando a funçao acabe o seu trabalho, vai sair uma lista de objetos puros do tipo Question. SÓ DEVOLVE AS PERGUNTAS FINAIS DO JOGO, NADA FORA DISTO.
        //getQuestions: o nome dado a esta tarefa no ficheiro, contrato, openTDBapi.
        //(Category category, QuestionType type, int amount): Nesta funçao vai entrar os ingredientes, objetos puros, que a funçao vai precisar para armar as questoes. Fica a espera até que a pessoa agita os dados. Objetos: categoria, tipo de pergunta e quantidade desejada (3)


        List<Question> perguntasProntas = new ArrayList<>(); // List<Question>: Formato que se deve seguir para receber as perguntas
        //perguntasProntas: o nome dado a List<Question>
        //new ArrayList<>(): Aqui fabrica-se a lista como tal, a caixa onde estará tudo. <>: aqui dentro deveria estar a palavra Question, mas nao é necessário pela existência deste operador


        // 2. TRADUÇÃO: Converter as tuas classes para os textos que a API entende
        int categoryId = traduzirCategoriaParaId(category); // usa-se a funçao do final do ficheiro, o dicionário das categorias, para fazer a tradução do número recebido para ver a que palavra corresponde
        //Timelap: O ficheiro Category.java (lá na tua pasta domain/model) foi onde tu criaste as palavras: SPORTS, HISTORY, etc.
                //O teu GameSession (que vamos criar no futuro) vê o jogador lançar o dado. Calha o lado do Desporto.
               // O GameSession grita para o Repositório: "Ei, executa a função getQuestions e toma lá a Categoria SPORTS!"
        //A palavra SPORTS aterra na variável category (na porta de entrada da função).
                //Tu agarras nessa variável e atiras logo para dentro do teu tradutor: traduzirCategoriaParaId(category).
                //O tradutor lê SPORTS, devolve o número 21, e guarda-o no teu categoryId temporário.


        String typeString = (type == QuestionType.MULTIPLE_CHOICE) ? "multiple" : "boolean"; // vai buscar o ficheiro ENUM do QuestionType, que tem as opções dos tipos de perguntas que podem existir
//? …  :  … - Um atalho para fazer um if e um else:
        //String typeString;
      //  if (type == QuestionType.MULTIPLE_CHOICE) {
           // typeString = "multiple";
      //  } else {
            //typeString = "boolean";
        //}
        //"multiple" : "boolean" - colocamos o multiple e o boolean, porque no manual da openTDB aparece que os pedidos do tipo de questões devem ser feitas desta forma, obrigatoriamente

        try {
            // 3. PREPARAR E DISPARAR O PEDIDO
            Call<OpenTDBresponse> chamada = api.getQuestions(amount, categoryId, typeString); // Call<OpenTdbResponse>: Chama a funçao do Retrofit que contacta com o servidor, o telefone, com o formato já predefinido pelo ficheiro OpenTdbResponse, o servidor tem de mandar exatamente na forma como está nesse ficheiro
            //chamada: nome da tarefa que a chamada vai realizar
            //Api: a variavel responsável por fazer o que se pediu no ficheiro openTDBrapi. A gaveat, telemóvel,  para guardar tudo isto
            //Api.getQuestions: a variavel vai executar a fucnçao getQuestions, do ficheiro openTDBapi
            //(amount, categoryId, typeString): sao os valores recebidos pelo jogo, pelo usuário quando joga. Aqui o retrofit meterá tudo disto no link e vai procurar na pagina da OpenTDB, na morada, para poder buscar cada uma dessas variáveis.

            synchronized (this) {
                // A tarefa pode ter começado ao mesmo tempo que o ecrã foi bloqueado.
                if (requestsCancelled) {
                    return perguntasProntas;
                }
                activeQuestionCall = chamada;
            }

            Response<OpenTDBresponse> resposta;
            try {
                resposta = chamada.execute(); // Aguarda a resposta nesta thread de trabalho.
            } finally {
                // Evita que um pedido antigo apague a referência de um pedido mais recente.
                synchronized (this) {
                    if (activeQuestionCall == chamada) {
                        activeQuestionCall = null;
                    }
                }
            }
            // O execute() é síncrono; a GameActivity chama este método num ExecutorService para não bloquear a UI.
            //Response<OpenTdbResponse>: Response acaba por ser uma caixa de correios da Retrofit, pois nao entrega apenas o texto do servidor, mas também o estado ou se correu bem ou nao esta execução, apresenta um relatório. Este texto todo vai ter de estar no formato instanciado no OpenTdbResponse.


            // 4. SE A INTERNET DEVOLVER SUCESSO (Código 200)
            if (resposta.isSuccessful() && resposta.body() != null) { //resposta.isSuccessful(): pergunta, com a funçao isSuccessful(), se a execução da chamada correu bem
               // resposta.body() != null: comprova que o recebido pela openTDB não esteja vazio. Entre na funçao body(), definido no ficheiro build.gradle. É a chave para abrir os pacotes recebidos da net e aceder aos conteúdos reais.



                // 5. PERCORRER CADA PERGUNTA DO DTO E CONVERTÊ-LA EM MODELO DO DOMÍNIO
                for (OpenTDBresponse.QuestionJson Json : resposta.body().results) {
                    // A função testada também compõe e baralha as opções da pergunta.
                    Question perguntaLimpa = mapQuestion(category, type, Json);
                    perguntasProntas.add(perguntaLimpa);
                }
            }
        } catch (Exception e) {
            // Se o telemóvel estiver sem internet ou o servidor falhar, o erro cai aqui.
            // A lista volta vazia e o jogo não "crasha".
            e.printStackTrace();
        }

        return perguntasProntas; // devolvem-se todas as perguntas
    }

    // Interrompe a ligação HTTP ativa quando o lançamento é suspenso pelo ciclo de vida Android.
    @Override
    public void cancelPendingRequests() {
        Call<OpenTDBresponse> call;
        synchronized (this) {
            requestsCancelled = true;
            call = activeQuestionCall;
        }
        if (call != null) {
            call.cancel();
        }
    }

    // Reabre o acesso à API quando o jogador regressa e pode iniciar outro lançamento.
    @Override
    public synchronized void resumeRequests() {
        requestsCancelled = false;
    }

    // Converte o DTO recebido da OpenTDB num modelo do domínio e baralha as opções.
    // O acesso package-private permite testar esta transformação sem aceder à Internet.
    static Question mapQuestion(
            Category category,
            QuestionType type,
            OpenTDBresponse.QuestionJson Json
    ) {
        List<String> respostasMisturadas = new ArrayList<>();
        respostasMisturadas.add(Json.correctAnswer);

        // Algumas respostas podem não incluir uma lista de opções incorretas.
        if (Json.incorrectAnswers != null) {
            respostasMisturadas.addAll(Json.incorrectAnswers);
        }

        // A ordem aleatória evita que a resposta correta fique sempre na mesma posição.
        Collections.shuffle(respostasMisturadas);

        return new Question(
                category,
                type,
                Json.question,
                Json.correctAnswer,
                respostasMisturadas
        );
    }

    // Método auxiliar (Alfândega) para converter os Enums do jogo nos números da OpenTDB
    private int traduzirCategoriaParaId(Category category) { // os tipos de questoes que aparecem no ficheiro Category

        // Cada categoria tem o seu numero associado. Vemos isto na pagina oficial da openTDB
        switch (category) {
            case ART:
                return 25;
            case SCIENCE:
                return 17;
            case HISTORY:
                return 23;
            case CINEMA:
                return 11;
            case VIDEOGAMES:
                return 15;
            case MUSIC:
                return 12;
            default:
                return 9; // 9 = Conhecimentos Gerais (Caso de segurança)
        }
    }
}
