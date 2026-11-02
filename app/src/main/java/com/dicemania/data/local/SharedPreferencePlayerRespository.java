package com.dicemania.data.local;

import android.content.Context;
import android.content.SharedPreferences;

import com.dicemania.domain.model.Player;
import com.dicemania.domain.repository.PlayerRepository;

// Este ficheiro serve como um bloco de notas, o armazem das informaçoes do jogador que serao armazenadas no telemovel. Cada vez que este entre na aplicaçao nao deverá de colocar o seu nome e fazer login
// Acaba por ser uma ferramenta do android, SharedPreferences, que cria um pequeno ficheiro de texto (XML) invisivel no telemovel, para poder usar a informaçoes pqeunas e rapidas contida nele

// Este ficheiro vai ligar-se ao PlayerRepository, pois este fichiero (SharedPreferencePlayerRespository) em que se realiza a implementaçao fisica do contrato, das regras, definidas no PLayerRepository
// também ao ficheiro PLayer. Recebe um obejto do tipo PLayer, para poder retirar os dados do jogador e poder guarda-los, e caso seja preciso os mesmos, constroi um novo obejto do tipo Player quando for ler os dados na memória do telemovel
public class SharedPreferencePlayerRespository implements PlayerRepository{ // esteficheiro assina o contrato escrito em PLayerRepository. Vai cumprir as regras e métodos lá escritos, vai ter a responsabilidade de fazer o que está lá

    // a ferramanete SharedPreferences guarda os dados num sistema de Chave-Valor (uma dada chave deve ter sempre um unico e determinado valor)

    private static final String PREF_NAME = "DicaManiaPreference"; // Definimos aqui o nome do ficheiro a usar para guardar a informaçoes do jogador no telemovel. Será o nome do bloco de notas
    private static final String KEY_ID = "player_id"; // Sao os nomes das páginas desse ficheiro, do bloco de notas do telemovel, esta terá e guardará o ID do jogador
    private static final String KEY_NICKNAME = "player_nickname"; // aqui, nesta página, ficará guardado o nome do jogador
    // se nao fizermos isto, ao longo do código teriamos de estar a escrever constantemente o nome do ficheiro e das suas páginas, o que ocasionaria bugs e relentizaçoes do sistema

    private SharedPreferences preferences; // criamos um objeto do tipo SharedPrefereneces para guardar as ferramentas de leitura e escrita do android para podermos usá-las mais facilmente ao longo da classe

    public SharedPreferencePlayerRespository (Context context){ // aqui temos o nosso construtor
        // mal o programa começe vai ser construido este repositório e lhes vai ser passado um context
        // context: Diz que ninguem pode criar este repositório sem entregar um cartao de acesso ao Android, o passe VIP das informaçoes. Cada ficheiro entregará o seu context para que possam usar este construtor
        // É a ponte entre o código e o cérebro do telemovel

        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE); // o context tem imensas ferramentas, como ligar acamara, ler GPS e até para ler a memória, que é o que vamos usar
        // context.getSharedPreferences: pedimos ao context para usar a ferramenta getSharedPreferences, que vai entrar na memória do nosso telemovel e vai buscar o ficheiro, o tal bloco de notas, que contem as informaçoes do jogador
        // (PREF_NAME: damos o nome do ficheiro, bloco de notas, que queremos que seja aberto, que seja procurada ou criado caso nao exista, quando pedirmos ao sistema
        // Context.MODE_PRIVATE): è o cadiado de segurança da aplicaçao. Fazemos com que este ficheiro, contendo todas as informaçoes do jogador, só se possa ler e escrever pela nossa aplicaçao. Caso outra aplicaçao tente, vai-lhe ser negada a entrada

    }

    @Override
    public void saveLocalPlayer (Player player){ // método chamdo mal o jogador faça login pela primeira vez, registo no primeiro ecra

        // todas estas funçoes sao da classe SharedPreferences
        // o Editor, é como a caneta, é uma classe que pertence ao SharedPreferences e tem como ferramentas o putString, Apply, etc

        SharedPreferences.Editor editor = preferences.edit(); // pedimos para abrir o ficheiro, bloco de notas, do telemovel e pegamos no editor, na tal caneta (preferences.edit())
        // o acesso e ediçao deste ficheiro, que está contido no telemovel será guardado na variavel editor do tipo Editor

        editor.putString(KEY_ID, player.getId()); // pegamos na variavel editor e lhe pedimos que acrescente na página Eo KEY_ID o id do joador recebido pela classe Player
        // pega-se o que se tem na variavel new player... que está na RAM e colocamos os eu conteudo na página do KWY_ID

        editor.putString(KEY_NICKNAME, player.getNickname()); // Fazemos o mesmo mas com a página do KEY_NICKNAME

        editor.apply(); // Fazemos com que as alteraçoes sejam guardadas. Sem este guardado, tudo o escrito anteriormente será totalmente apagado

    }

    @Override
    public Player getLocalPlayer (){ // método a chamar cada vez que a nossa aplicaçao seja aberta

        // estas variaveis vao ler o que está escrito no ficheiro, bloco de notas, e vao devolver o que estiver lá dentro

        String id = preferences.getString(KEY_ID, null); // obtemos o id do jogador. Caso nao a encontrarmos deve ser devolvido null
        String nickname = preferences.getString(KEY_NICKNAME,null); // obtemos o nickname

        if (id == null || nickname == null){ // caso nao tenhamos o id ou o nickname retorna-se null, logo a aplicaçao deve mostrar o ecra de registo, ou seja, a funçao em cima:  public void saveLocalPlayer (Player player)

           return null;

        }

        return new Player(id, nickname, 0); // Se os dados existem, pega nos dados do ficheiro XML e vai colocá-los, sem apagá-los, num objeto dinamico para que estas informaçoes possam ser usadas pelo resto da aplicaçao
        // a pontuaçao está aqui a zero proque ou o jogador ainda nao realizou nenhuma partida ou ainda nao começou a atual

    }

}
