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

public class AndroidShakeSensor implements ShakeSensor, SensorEventListener {

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private ShakeCallback callback;


    private static final float SHAKE_THRESHOLD_GRAVITY = 2.7F;

    private static final int SHAKE_SLOP_TIME_MS = 500;
    private long shakeTimeStamp;

    public AndroidShakeSensor(Context context) {

        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);

        if (sensorManager != null){

            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        }

    }

    @Override
    public void startListening(ShakeCallback callback){

        this.callback = callback;

        if (accelerometer != null){

            sensorManager.registerListener(this, accelerometer, sensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    public void stopListening() {
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        callback = null;
    }

    @Override
    public void onSensorChanged(SensorEvent event){

        if (callback == null){

            return;
        }

        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];

        float gX = x / SensorManager.GRAVITY_EARTH;
        float gY = y / SensorManager.GRAVITY_EARTH;
        float gZ = z / SensorManager.GRAVITY_EARTH;

        float gForce = (float) Math.sqrt(gX * gX + gY * gY + gZ * gZ);




        if (gForce > SHAKE_THRESHOLD_GRAVITY) {

            final long now = System.currentTimeMillis();

            if(shakeTimeStamp + SHAKE_SLOP_TIME_MS > now){

                return;

            }

            shakeTimeStamp = now;
            callback.onShakeDetected();

        }
    }

    @Override
    public void onAccuracyChanged (Sensor sensor, int accuracy){

    }


}
