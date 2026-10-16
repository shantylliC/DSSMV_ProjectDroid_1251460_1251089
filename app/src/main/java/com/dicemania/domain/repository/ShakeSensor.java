package com.dicemania.domain.repository;


//Vai ligar-se ao ficheiro do AndoridShakeSensor (o matematico que se comunica com o hardware), a nossa Activity (GameSession) que fará o pedido ao sensor quando o estado for WAITNF_FOR_ROLL e com o ShakeCallback que será quem avisa, a campainha, quando o sensor detetar movimento
public interface ShakeSensor { // o contrato de interface pura, a base arquitetural do hardware, sensor
    // dita as regras que o sensor deve seguir


    //FUNÇOES CRIADAS AQUI
    void startListening(ShakeCallback callback);
    //startListening: a ordem dada ao hardware para que o telemovel fique a "ouvir", a espera de que o telemovel seja abanado
    //(ShakeCallback callback): cada vez que a funçao deteta que o telemovel foi abanado, este aviso vai ser guardado na variavel callback

    void stopListening(); // faz com que se deixe de "ouvir", esperar para que o telemovel seja abanado
    // garante poupança de energia e jogadores que fazem batota, pois assim nao se ativa enquanto se estao a responder as perguntas, passando ao estado PLAYING_QUESTION
}
