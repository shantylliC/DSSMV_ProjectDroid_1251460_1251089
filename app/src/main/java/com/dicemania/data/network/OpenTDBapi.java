package com.dicemania.data.network;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

    public interface OpenTDBapi { // temos uma INTERFASE que representa uma ficheiro header do C++, vao ser declarado o que é preciso ser feito e nao como

        @GET("api.php")//vamos buscar no link URL da API, o ficheiro em que tem as perguntas, que precisamos, guardadas

        Call <OpenTDBresponse> getQuestions(//Vamos chamar ao nosso ficheiro OpenTBDresponse e a açao que queremos realizar será getQuestions. Vamos procurar as perguntas, categorias, etc, enviadas ao OpenTDBresponse para demonstrar o que é preciso ser feito com estas informaçoes

            //Apartir de aqui, vamos ter de criar o nosso "proprio" link, colocando um formato em espcifico para que cada vez que o usurio começe um jogo, o sistema possa pedir o tipo de perguntas, tipo de respostas, etc a API em tempo real, dando-lhe certos valores no fim do link original para que posso buscar as coisas especificas que se pretendem. Ex: 3 perguntas, categoria: jogos, tipo: veradeiro/falso,...

            @Query("amount") int amount, // diz ao retorfit que o valor, que será recebido, ou seja, 3, pois sao questoes necessarias, que estiver dentro da variavel amount, deve ser colado no final do link da internet. ?amount=...

            @Query("category") int category,

            @Query("type") int type

                                            );

    }


