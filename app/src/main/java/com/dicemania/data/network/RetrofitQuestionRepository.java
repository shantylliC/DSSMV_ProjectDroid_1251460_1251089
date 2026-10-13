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

    // 1. CONSTRUTOR: Arrancar o Motor do Retrofit e definir a Morada Principal
    public RetrofitQuestionRepository() { // cria-se um construtor para que cada variavel api tenha o formato e conteudos necessários para que o ficheiro gameSession tenha tudo o necessario para funcionar, quando este for passado para lá

        Retrofit retrofit = new Retrofit.Builder() //utilizamos uma variável do tipo retrofit (da propria biblioteca retrofit) e vamos criar uma variável dinâmica que será uma especie de construtor que vai permitir montar passo a passo cada parte, texto, que vamos precisar do API

                .baseUrl("https://opentdb.com/") // aqui é colocado a maior parte do link da API, esta funçao serve para guardar a morada principal e fixa do servidor. O retrofit precisa desta morada inicial para poder começar a procurar – o destino final deste link esta na OpentTDBapi (openTBD.hbh)

                .addConverterFactory(GsonConverterFactory.create()) // esta alinha pega na funçao addConverterFactory e cria a funçao GsonConverterFactory que, quando receba as mensagens da API, vai fazer a traduçao de todo o texto, que vem no formato Gson, e vai traduzi-lo e separá-lo para os objetos que o nosso programa vai usar e possa entender (category, type,…)

                .build(); // constroi tudo a partir das coisas ja feitas atrás

        api = retrofit.create(OpenTDBapi.class);  //esta variavel API vai guardar ao nosso construtor acima instanciado, vai guardar o produto final que a máquina do Retrofit acabou de construir . Vai criar um objeto do tipo OpenTDBapi e vai fazer o que foi explícito nesse ficheiro, o tal contrato feito com a interfase, este vai ter a responsabilidade de fazer o que está lá a acontecer. É este que vai fazer o pedido diretamente ao servidor, trazendo tudo o que for preciso. É como o telefone do programa, os ficheiros vão pedir que contacte com o servidor e este devolverá o que lhe foi dito pelo mesmo.
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


            Response<OpenTDBresponse> resposta = chamada.execute(); // A ação de execução da chamada, telemóvel para buscar a informaçao, vai ser executado pela variável resposta
            //Execute() faz com que o código bloqueie. Até que o servidor nao mande tudo, as variáveis todas, ele nao vai passar a linha de código seguinte
            //Response<OpenTdbResponse>: Response acaba por ser uma caixa de correios da Retrofit, pois nao entrega apenas o texto do servidor, mas também o estado ou se correu bem ou nao esta execução, apresenta um relatório. Este texto todo vai ter de estar no formato instanciado no OpenTdbResponse.


            // 4. SE A INTERNET DEVOLVER SUCESSO (Código 200)
            if (resposta.isSuccessful() && resposta.body() != null) { //resposta.isSuccessful(): pergunta, com a funçao isSuccessful(), se a execução da chamada correu bem
               // resposta.body() != null: comprova que o recebido pela openTDB não esteja vazio. Entre na funçao body(), definido no ficheiro build.gradle. É a chave para abrir os pacotes recebidos da net e aceder aos conteúdos reais.



                // 5. PERCORRER CADA PERGUNTA SUJA (JSON) E LIMPAR
                for (OpenTDBresponse.QuestionJson Json : resposta.body().results) { // resposta.body().results: chama-se de method chaining, com isto é possível viajar por várias camadas de codigo sem termos de criar váras variaveis temporais
                    //resposta: a varaivel de cima que contem os dados ja enviados pela API, a tal chamada
                    //.body(): acede aos conteudos que recebeu a variavel resposta da net
                    //.results: Ao abrir o objeto da openTDBresponse e vai diretemeente a variavel results que está definido como o nome da List<QuestionJson> definida no ficheiro openTDBresponse

                    //QuestionJson: é a propria classe que criamos no ficheiro openTDBresponse, sendo esta o molde de como devem vir as peeguntas sem formataçao, sujas, da net (com as tais etiquetas @SerializedName correctAnswer,... )
                    //for: por cada pergunta do tipo Json faz o seguinte


                    // Juntar todas as opções
                    List<String> respostasMisturadas = new ArrayList<>(); //criamos uma nova lista dinamica de objetos do tipo string chamada de todasAsRespostas
                    //serve para guardar todas as respostas, vindas da net alocadas no Json, independentemente da ordem das mesmas ou se sao verdadeiras ou falsas. Todo entra no mesmo saco
                    // como esta no ciclo for, o java criará uma lista nova e limpa para cada uma das perguntas

                    respostasMisturadas.add(Json.correctAnswer); // Usa-se a lista vazia em cima e adiciona-se, vai ser inserida, e a resposta correta que vem contida no objeto Json (que tem tudo, pergunta e respostas), que tem comomolde a funçao correctAnswer do ficheiro openTDBresponse


                    if (Json.incorrectAnswers != null) { // se o que está contido no objeto Json for as respostas incorretas
                        respostasMisturadas.addAll(Json.incorrectAnswers); // vao ser adicionadas a lista das respostasMisturadas as respostas incorretas contidas no onjeto Json no formato incorrectAnswer do ficheiro openTDBresponse
                    }



                    // 6. O BARALHADOR: Troca a posição da resposta certa aleatoriamente!
                    Collections.shuffle(respostasMisturadas); // o Collections e o shuffle vem da propria biblioteca do java (java.util)
                    //Collectiosn:è como uma caixa de ferramentas. Ele só guarda comandos para a manipulaçao das listas, por exemplo tem ferramentas especificas como ordenar alfabeticamente (sort()), virar ao contrário (reverse()), maior valor (max()), baralhar (shuffle()), ...
                    // .shuffle: é uma das ferramentas do Collections que vai baralhar todo o que está dentro da lista das respostasMisturadas



                    // 7. CONSTRUIR O MOLDE PURO DO TEU JOGO
                    // (Nota: Ajusta esta linha se o construtor da tua classe Question for diferente)
                    Question perguntaLimpa = new Question( // Question: é a classe do ficheiro Question (molde perfeito que mostra como uma pergunta deve ser dentro do universo), um texto, lista de opçoes e apresentar a certa.
                            // cria-se um objeto do tipo Question chamado perguntaLimpa e esta vai chamar ao construtor criado no ficheiro Question para que crie uma questao com os parametros qeu tiver nos parentesis

                            category,

                            type,

                            Json.question, //tira-se só o texto da questao

                            Json.correctAnswer, // Passamos a resposta certa para o jogo saber quem ganha

                            respostasMisturadas // tem todas as respostas já baralhadas

                    );

                    perguntasProntas.add(perguntaLimpa); // pegamos na lista que fizemos em cima, antes de começarmos com o loop, e vamos colocar-lhe o pacote de perguntas completo, perguntas, opçoes e a resposta correta
                }
            }
        } catch (Exception e) {
            // Se o telemóvel estiver sem internet ou o servidor falhar, o erro cai aqui.
            // A lista volta vazia e o jogo não "crasha".
            e.printStackTrace();
        }

        return perguntasProntas; // devolvem-se todas as perguntas
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
