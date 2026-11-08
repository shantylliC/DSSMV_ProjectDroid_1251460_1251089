package com.dicemania.data.sound;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;

import com.dicemania.R;

import java.io.IOException;

// Gere a música ambiente em loop e mantém esta opção separada dos efeitos sonoros.
public class BackgroundMusicManager {

    // Preferências privadas usadas pelo switch independente de música ambiente.
    private static final String PREFERENCES_NAME = "game_feedback_settings";
    private static final String KEY_MUSIC_ENABLED = "background_music_enabled";

    // Mantém o mesmo leitor de áudio entre as Activities para retomar do ponto pausado.
    private static BackgroundMusicManager instance;

    // Leitor do ficheiro WAV guardado em res/raw.
    private MediaPlayer mediaPlayer;

    // Guarda o contexto da aplicação, sem manter uma Activity viva na memória.
    private Context applicationContext;

    // Indica se o leitor terminou de preparar o ficheiro e pode começar a tocar.
    private boolean playerPrepared;

    // Memoriza se a música deve tocar quando a preparação assíncrona terminar.
    private boolean playbackRequested;

    // Devolve o gestor partilhado pelas Activities da aplicação.
    public static synchronized BackgroundMusicManager getInstance() {
        if (instance == null) {
            instance = new BackgroundMusicManager();
        }
        return instance;
    }

    // Construtor privado para impedir que cada ecrã crie um leitor de música diferente.
    private BackgroundMusicManager() {
    }

    // Lê se a música ambiente está ativa; por omissão, começa ligada.
    public static boolean isMusicEnabled(Context context) {
        return getPreferences(context).getBoolean(KEY_MUSIC_ENABLED, true);
    }

    // Guarda a escolha e aplica-a imediatamente no ecrã que está aberto.
    public static void setMusicEnabled(Context context, boolean enabled) {
        getPreferences(context).edit()
                .putBoolean(KEY_MUSIC_ENABLED, enabled)
                .apply();

        if (enabled) {
            getInstance().resumeIfEnabled(context);
        } else {
            getInstance().pause();
        }
    }

    // Inicia ou retoma a faixa quando uma Activity passa para primeiro plano.
    public void resumeIfEnabled(Context context) {
        applicationContext = context.getApplicationContext();

        if (!isMusicEnabled(applicationContext)) {
            pause();
            return;
        }

        playbackRequested = true;

        if (mediaPlayer == null) {
            preparePlayer();
        } else {
            startIfReady();
        }
    }

    // Prepara o ficheiro local em segundo plano para não atrasar o desenho do ecrã.
    private void preparePlayer() {
        AudioAttributes musicAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build();

        MediaPlayer newPlayer = new MediaPlayer();
        mediaPlayer = newPlayer;

        try {
            // Define o tipo de áudio e a fonte antes de iniciar a preparação assíncrona.
            newPlayer.setAudioAttributes(musicAttributes);
            Uri musicUri = Uri.parse(
                    "android.resource://"
                            + applicationContext.getPackageName()
                            + "/"
                            + R.raw.background_music
            );
            newPlayer.setDataSource(applicationContext, musicUri);
            newPlayer.setVolume(0.18f, 0.18f);
            newPlayer.setLooping(true);

            // Só começa depois de a descodificação estar pronta e se ainda estiver em primeiro plano.
            newPlayer.setOnPreparedListener(preparedPlayer -> {
                if (mediaPlayer != preparedPlayer) {
                    preparedPlayer.release();
                    return;
                }

                playerPrepared = true;
                startIfReady();
            });

            // Liberta o leitor se o dispositivo não conseguir descodificar o recurso.
            newPlayer.setOnErrorListener((failedPlayer, what, extra) -> {
                if (mediaPlayer == failedPlayer) {
                    releasePlayer();
                }
                return true;
            });

            newPlayer.prepareAsync();
        } catch (IOException | IllegalArgumentException | IllegalStateException error) {
            releasePlayer();
        }
    }

    // Começa a faixa apenas quando o leitor, a preferência e o estado do ecrã permitem.
    private void startIfReady() {
        if (mediaPlayer == null
                || !playerPrepared
                || !playbackRequested
                || !isMusicEnabled(applicationContext)
                || mediaPlayer.isPlaying()) {
            return;
        }

        mediaPlayer.start();
    }

    // Pausa quando a Activity sai de primeiro plano; a posição fica guardada para retomar.
    public void pause() {
        playbackRequested = false;

        if (mediaPlayer != null
                && playerPrepared
                && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        }
    }

    // Liberta o leitor se a preparação ou a descodificação falhar.
    private void releasePlayer() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }

        playerPrepared = false;
    }

    // Abre as preferências partilhadas com as restantes definições de áudio.
    private static SharedPreferences getPreferences(Context context) {
        return context.getApplicationContext().getSharedPreferences(
                PREFERENCES_NAME,
                Context.MODE_PRIVATE
        );
    }
}
