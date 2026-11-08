package com.dicemania.data.sound;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.content.SharedPreferences;

import com.dicemania.R;

/**
 * Centraliza os sons e as vibrações curtas usados durante uma partida.
 * Os MP3 de aplausos e vaias e o WAV contínuo dos dados ficam em res/raw.
 */
public class GameFeedbackEffects {

    // Guarda as preferências de som e vibração para continuarem após fechar a aplicação.
    private static final String PREFERENCES_NAME = "game_feedback_settings";
    private static final String KEY_SOUND_ENABLED = "sound_enabled";
    private static final String KEY_VIBRATION_ENABLED = "vibration_enabled";

    // Duração do bip e da vibração que assinalam cada um dos últimos segundos.
    private static final int COUNTDOWN_BEEP_DURATION_MS = 100;
    private static final long COUNTDOWN_VIBRATION_DURATION_MS = 55L;

    // Duração do alarme e da vibração mais forte quando o tempo chega a zero.
    private static final int TIMEOUT_ALARM_DURATION_MS = 850;
    private static final long TIMEOUT_VIBRATION_DURATION_MS = 550L;

    // Gere os três efeitos de áudio da partida sem bloquear a interface.
    private final SoundPool soundPool;

    // Emite os bips do relógio sem precisar de ficheiros de áudio adicionais.
    private final ToneGenerator toneGenerator;

    // Controla a vibração do telemóvel, se o dispositivo tiver esse hardware.
    private final Vibrator vibrator;

    // Mantém as opções escolhidas no ecrã de definições durante esta partida.
    private final boolean soundEnabled;
    private final boolean vibrationEnabled;

    // IDs atribuídos pelo SoundPool aos efeitos guardados em res/raw.
    private int applauseSoundId;
    private int booSoundId;
    private int diceRollSoundId;
    private int diceRollStreamId;

    // Os sons só podem tocar depois de o SoundPool terminar de os carregar.
    private volatile boolean applauseLoaded;
    private volatile boolean booLoaded;
    private volatile boolean diceRollLoaded;

    // Permite começar o som assim que carregar, mesmo que a animação já tenha começado.
    private volatile boolean diceRollRequested;

    // Impede chamadas a recursos nativos depois de a Activity ser destruída.
    private volatile boolean released;

    /**
     * Prepara o gerador de tons, o vibrador e os efeitos de resposta em memória.
     */
    public GameFeedbackEffects(Context context) {
        Context applicationContext = context.getApplicationContext();

        // Lê as opções guardadas antes de preparar os efeitos desta partida.
        soundEnabled = isSoundEnabled(applicationContext);
        vibrationEnabled = isVibrationEnabled(applicationContext);

        vibrator = (Vibrator) applicationContext.getSystemService(
                Context.VIBRATOR_SERVICE
        );

        if (soundEnabled) {
            // Usa o volume de media do telemóvel para os bips do relógio.
            ToneGenerator generator;
            try {
                generator = new ToneGenerator(AudioManager.STREAM_MUSIC, 80);
            } catch (RuntimeException audioUnavailable) {
                generator = null;
            }
            toneGenerator = generator;

            // Os efeitos curtos ficam em memória para poderem tocar sem atraso.
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();

            soundPool = new SoundPool.Builder()
                    .setAudioAttributes(audioAttributes)
                    .setMaxStreams(3)
                    .build();

            // Marca cada efeito como pronto quando a descodificação assíncrona termina.
            soundPool.setOnLoadCompleteListener((pool, sampleId, status) -> {
                if (status != 0) {
                    return;
                }

                if (sampleId == applauseSoundId) {
                    applauseLoaded = true;
                } else if (sampleId == booSoundId) {
                    booLoaded = true;
                } else if (sampleId == diceRollSoundId) {
                    diceRollLoaded = true;
                    startDiceRollIfReady();
                }
            });

            // Fonte: SFXMint CC0 1.0 — https://sfxmint.com/sounds/growth-41f04b2be34295a1012ef2ff-retry
            applauseSoundId = soundPool.load(
                    applicationContext,
                    R.raw.correct_applause,
                    1
            );

            // Fonte: SFXMint CC0 1.0 — https://sfxmint.com/sounds/crowd-boo-09
            booSoundId = soundPool.load(
                    applicationContext,
                    R.raw.wrong_boo,
                    1
            );

            // O som de dados em loop acompanha a duração da animação Lottie.
            diceRollSoundId = soundPool.load(
                    applicationContext,
                    R.raw.dice_roll,
                    1
            );
        } else {
            // Não cria nem carrega áudio se o jogador desligou o som nas definições.
            toneGenerator = null;
            soundPool = null;
        }
    }

    /** Devolve se os efeitos sonoros estão ligados nas definições. */
    public static boolean isSoundEnabled(Context context) {
        return getPreferences(context).getBoolean(KEY_SOUND_ENABLED, true);
    }

    /** Guarda a opção escolhida para os efeitos sonoros. */
    public static void setSoundEnabled(Context context, boolean enabled) {
        getPreferences(context).edit()
                .putBoolean(KEY_SOUND_ENABLED, enabled)
                .apply();
    }

    /** Devolve se a vibração está ligada nas definições. */
    public static boolean isVibrationEnabled(Context context) {
        return getPreferences(context).getBoolean(KEY_VIBRATION_ENABLED, true);
    }

    /** Guarda a opção escolhida para a vibração. */
    public static void setVibrationEnabled(Context context, boolean enabled) {
        getPreferences(context).edit()
                .putBoolean(KEY_VIBRATION_ENABLED, enabled)
                .apply();
    }

    // Centraliza o acesso às preferências partilhadas pela HomeActivity e pelo jogo.
    private static SharedPreferences getPreferences(Context context) {
        return context.getApplicationContext().getSharedPreferences(
                PREFERENCES_NAME,
                Context.MODE_PRIVATE
        );
    }

    /**
     * Emite um bip curto e uma vibração leve num dos últimos três segundos.
     */
    public void playCountdownTick() {
        if (released) {
            return;
        }

        if (soundEnabled && toneGenerator != null) {
            toneGenerator.startTone(
                    ToneGenerator.TONE_PROP_BEEP,
                    COUNTDOWN_BEEP_DURATION_MS
            );
        }

        vibrate(COUNTDOWN_VIBRATION_DURATION_MS);
    }

    /**
     * Emite um alarme curto e uma vibração mais longa quando o tempo termina.
     */
    public void playTimeoutFeedback() {
        if (released) {
            return;
        }

        if (soundEnabled && toneGenerator != null) {
            toneGenerator.startTone(
                    ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD,
                    TIMEOUT_ALARM_DURATION_MS
            );
        }

        vibrate(TIMEOUT_VIBRATION_DURATION_MS);
    }

    /** Inicia o som contínuo de dados quando a animação começa. */
    public void playDiceRoll() {
        if (released || !soundEnabled || soundPool == null) {
            return;
        }

        diceRollRequested = true;
        startDiceRollIfReady();
    }

    // Só inicia o loop depois de o ficheiro estar pronto e se a animação ainda estiver ativa.
    private void startDiceRollIfReady() {
        if (released
                || !diceRollRequested
                || !diceRollLoaded
                || soundPool == null
                || diceRollStreamId != 0) {
            return;
        }

        diceRollStreamId = soundPool.play(
                diceRollSoundId,
                0.75f,
                0.75f,
                1,
                -1,
                1f
        );
    }

    /** Pára o som de dados assim que as duas animações terminam. */
    public void stopDiceRoll() {
        diceRollRequested = false;

        if (soundPool != null && diceRollStreamId != 0) {
            soundPool.stop(diceRollStreamId);
            diceRollStreamId = 0;
        }
    }

    /**
     * Toca palmas se a resposta estiver certa ou uma vaia se estiver errada.
     */
    public void playAnswerFeedback(boolean answerWasCorrect) {
        if (released) {
            return;
        }

        if (soundPool == null) {
            return;
        }

        if (answerWasCorrect && applauseLoaded) {
            soundPool.play(applauseSoundId, 1f, 1f, 1, 0, 1f);
        } else if (!answerWasCorrect && booLoaded) {
            soundPool.play(booSoundId, 1f, 1f, 1, 0, 1f);
        }
    }

    /**
     * Vibra durante o período pedido, com suporte para Android anterior à API 26.
     */
    private void vibrate(long durationMs) {
        if (!vibrationEnabled || vibrator == null || !vibrator.hasVibrator()) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(
                    durationMs,
                    VibrationEffect.DEFAULT_AMPLITUDE
            ));
        } else {
            // A API antiga só aceita a duração em milissegundos.
            vibrator.vibrate(durationMs);
        }
    }

    /**
     * Liberta os recursos de áudio e interrompe qualquer vibração desta partida.
     */
    public void release() {
        if (released) {
            return;
        }

        released = true;

        // Garante que o loop não continua quando a Activity fecha.
        stopDiceRoll();

        if (toneGenerator != null) {
            toneGenerator.stopTone();
            toneGenerator.release();
        }

        if (soundPool != null) {
            soundPool.release();
        }

        if (vibrator != null && vibrator.hasVibrator()) {
            vibrator.cancel();
        }
    }
}
