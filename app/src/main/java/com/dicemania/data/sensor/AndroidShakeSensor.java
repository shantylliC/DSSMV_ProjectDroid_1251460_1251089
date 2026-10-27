package com.dicemania.data.sensor;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import com.dicemania.domain.repository.ShakeCallback;
import com.dicemania.domain.repository.ShakeSensor;

//este ficheiro vai estar ligado ao shakeSensor, para garantir que o programa sabe falar com ele, shakecallback que usa a campainha para dizer se o telemovel foi agitado e a nossa activity, GamesSession, para que possa ter as permissao de usar as peças físicas
//precismamos dele porque serve como filtro matematico. Se nao existisse, este pediria a cada milisengundos perguntas a API e estaria constantemente a detetar movimentos inecessários criando bug e crashando a aplicaçao
// deteta gravidade em 3 dimensoes, calcula força de impacto e tem um temporizador para ver so abanamento é prolongado

public class AndroidShakeSensor implements ShakeSensor, SensorEventListener { //Aqui assina-se, a classe concorda em cumprir com as regras definidas no ficheiro ShakeSensor e com o SensorEventListener, que vem da google, para permitir ler dados fisicos (obriga a criar a funçao onSensorChanged)

    private SensorManager sensorManager; // vem do contrato com a google, é a ele que vamos pedir as coisas para que o programa funcione
    private Sensor accelerometer; //peça de hardware específica que vamos usar
    private ShakeCallback callback; // a variavel que vem di ficheiro ShakeCallback que contém o aviso de se o telemovel foi abanado ou nao


    private static final float SHAKE_THRESHOLD_GRAVITY = 2.7F; // definimos a força de agitaçao necessaria para que o jogador possa começar o jogo
    // final: nunca mais pode ser alterado o valor desta constante, durante a compilaçao do código. Prevenimos assim bugs desnecessários
    //static final: guarda-se a variavel só uma vez para o jogo inteiro

    private static final int SHAKE_SLOP_TIME_MS = 500; // Aqui definimos os milisegundos que o sistema deve ignorar depois de o telemovel ser abanado, os movimentos que se detetem depois do primeiro serao ignorados
    private long shakeTimeStamp; // esta variavel funciona como uma memória do motor fisico. Guarda os milissegundos exatos em que o último abanao válido foi feito
    // É o nosso filtro anti-spam

    public AndroidShakeSensor(Context context) { // temos aqui o construtor desta classe
        // context: classe nativa do android. Acaba por ser como um passaporte, a ponte de comunicaçao entre o nosso código e o sistema operativo -- Representa o contexto, o estado atual da nossa aplicaçao perante o telemovel
        // precismaos do context, pois precisamos de autorizaçao para aceder as peças físicas do telemovel e para tal devemos pedir permissao oa sistema operativo. Como fazemos isso? usando a nossa identificaçao, context
        // com isto o motor fisico pode falar com o android

        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        // o nosso context pede ao android que quer usar um serviço do sistema, getSystemService, especificamente aquele que controla a parte dos sensores, SENSOR_SERVICE
        // (SensorManager): o android entrega o acesso aos sensores através de um pacote genérico. ou seja, pedimos que este acesso venha na variavel SensorManager, o nosso cast

        if (sensorManager != null){ // verificar se o recebido foi um gestor de sensores válidos

            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER); // caso o recebido seja um gestor de sensores
            // Pedimos que vai ás peças físicas do dispositivo e que nos entregue o controlo do sensor do tipo Acelerómetro, TYPE_ACCELEROMETER
            // accelerometer: agora, esta variavel, ficará com a ligaçao direta e fisico ao chip de movimento do telemovel, terá acesso ao acelerometro do dispositivo

        }

    }


    // É o interruptor que inicia o motor fisico. Recebe o pedido do jogo para começar a prestar atençao aos movimentos do telemovel
    @Override // Está a cumprir com o "contrato", o código definido no ficheiro ShakeSensor
    public void startListening(ShakeCallback callback){ // recebe o callback, a variavel que guarda se o telemovel foi agitado ou nao

        this.callback = callback; // guarda a campainha, na memoria do motor
        // this.callback: this referece a propria classe, á memoria permanente do ficheiro (variavel privada)
        // = callback: variavel que entra pelos parentesis

        if (accelerometer != null){ // codigo de segurança: só ligamos o sensor se tivermos a peça fisica a funcionar bem

            sensorManager.registerListener(this, accelerometer, sensorManager.SENSOR_DELAY_UI);
            //this: diz quem vai ficar a ouvir os dados, pedimos que se mandem os dados para a propria classe
            //accelerometer: especificamos qual é o chip fisico que queremos que ele use, acelerometro
            //SensorManager.SENSOR_DELAY_UI: é a taxa de atualizaçao, quantas vezes por segundo o android deve mandar os dados. UI é a velocidade média (existem tambem: NORMAL, GAME, FASTEST)

        }
    }

    @Override
    public void stopListening(){

        this.callback = callback;

        if (sensorManager != null){

            sensorManager.unregisterListener(this); // dorme o hardware. Para a leitura do sensor, assim o sistema nao passa todo o jogo a tentar ouvir ou ver se houve movimento ou nao
        }
    }


    @Override
    public void onSensorChanged(SensorEvent event){ // o nosso método matemático
        // event: vem do sistema android. o android manda todos os dados, a força e os milisegundos, do movimento nesta variavel do tipo SensorEvent
        // trás os numeros para podermos fazer as nossas contas

        if (callback == null){ // nao é preciso fazer o calculos dos movimentos caso o callback venha vazio, ou seja, que a campainha nao toque, vazio

            return;
        }

        // aqui estao as forças de movimentos, reconhecidos, num array de valores de cada um dos eixos de movimento. Vem em metros por segundo ao quadrado
        float x = event.values[0]; // movimento lateral
        float y = event.values[1]; // movimento vertical
        float z = event.values[2]; // movimento de profundidade
        // na posiçao 1 esta X, no 2 está Y e no 3 está Z

        // obtemos a força G de cada eixo
        float gX = x / SensorManager.GRAVITY_EARTH; // valor do X a dividir pela gravidade da terra, 9.8 m/s2
        float gY = y / SensorManager.GRAVITY_EARTH;
        float gZ = z / SensorManager.GRAVITY_EARTH;

        // temos o teorema de pitagoras para calcular a força G total de todos os 3 eixos
        float gForce = (float) Math.sqrt(gX * gX + gY * gY + gZ * gZ); // juntamos os 3 eixos num único vetor global



        if (gForce > SHAKE_THRESHOLD_GRAVITY) { // se a força vindo do telemovel for maior à força (> 2.7)

            final long now = System.currentTimeMillis(); // quando a gforce superara a força estabelecida, o sistema tira uma foto, guarda a hora exata de quando aconteceu, naquele milissegundo

            if(shakeTimeStamp + SHAKE_SLOP_TIME_MS > now){ // pega no último abanao que registamos na memória (shakeTimingStamp) e lhe vai ser somado (SHAKE_SLOP_TIME_MS).
                // Se esse tempo ainda for maior que o momento atual (now), significa que ainda nao passou meio segundo de descanso, enato, faz um return

                return;
            }

            // se chegamos aqui, significa que o abanao é válido e respeitou a janela de descanso de meio segundo
            shakeTimeStamp = now; // atualiza-se a mameoria com a hora do abanao atual (aquele feito agora mesmo) para assim bloquear novos disparos, campainhas do telemovel a avisar, no meio segundo seguinte
            callback.onShakeDetected(); //chamamos ao detetor de abanao, campainha
            //a fisica acaba aqui e o dominio do jogo entra em açao, ativando-se a maquina de estados para ligar o temporizador e descarregar as perguntas do api


        }
    }

    @Override
    public void onAccuracyChanged (Sensor sensor, int accuracy){ // a segunda funçao importada do android studio
        // usa-se esta funçao para avisar ao jogo de que um sensorr descalibrou ou alteoru a precissao
        // este diz: sensor - qual foi a peça fisica, accuracy - numero que representa o estado de saude do sensor (HIGH, MEDIUM, LOW, UNRELIABLE)

        //fica vazio porque independentemente do valor a matematica de culculo nao fica alterada, nao afeta a aplicaçao
    }


}
