package com.dicemania.domain.repository;

// existe porque precisamos de um processo assincrono. Serve para detetar se está em movimento ou nao o telemovel

//este ficheiro vai ligar-se ao androidShakeSensor, este vai cumprir as regras definidas aqui, vai cumprir o contrato; a nossa Activity, GameSession, pois quando o jogar abanar o telemovel vai ser posivel fazer o pedido a openTDB para obter as perguntas e o ShakeSensor sendo usado como parametro

public interface ShakeCallback { //Otra interface, contrato que se deve seguir para que os sensores funcionem
    //funciona apenas se mexe o telemovel

    void onShakeDetected();// devolve a detaçao de movimento
}