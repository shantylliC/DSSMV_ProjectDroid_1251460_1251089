package com.dicemania.domain.repository;

public interface ShakeSensor {

    // Um mini-contrato para avisar o jogo quando o abanão acontece
    interface Callback {
        void onShake();
    }

    void startListening(Callback callback);
    void stopListening();
}
