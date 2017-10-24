package com.example.andreucortes.notouchmcontrol;

import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.List;

import static java.lang.Math.abs;

public class MagneticGesturesListener extends GestureListenerService {

    private Binder binder = new LocalBinder();

    public class LocalBinder extends Binder {
        public MagneticGesturesListener getService() { return MagneticGesturesListener.this; }
    }

    String TAG = "MagneticGesturesListner";
    MySensorEvent currentEvent, lastEvent;
    int magneticSensitivity = 0;
    int magneticFactor = 100000;
    int delay = 5;

    SensorEventListener sensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            synchronized (this) {
                currentEvent = new MySensorEvent(event);
                Log.d(TAG, "read event!");

                evaluate();

                lastEvent = new MySensorEvent(currentEvent);
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };

    public void evaluate(){
        float delta = abs(currentEvent.values[0]) - abs(lastEvent.values[0]); //+ abs(currentEvent.values[1]) - abs(lastEvent.values[1]) + abs(currentEvent.values[2]) - abs(lastEvent.values[2]);
        float deltaTime = (float) ((currentEvent.timestamp - lastEvent.timestamp)/1E8);

        Log.d(TAG, "deltaMagn: " + delta + "; deltaTime: " + deltaTime + "; magneticSensitivity: " + magneticSensitivity);
        if(delay > 0) delay--;
        Log.d(TAG, "delay: " + delay);
        if((delta/deltaTime) > magneticSensitivity/deltaTime && delay == 0){
            Log.d(TAG, "runningAction");
            Log.d(TAG, "decision: " + (delta/deltaTime > magneticSensitivity*magneticFactor/deltaTime));
            Log.d(TAG, "decision: deltaMagn: " + delta + "; deltaTime: " + deltaTime + "; division: "+ delta/deltaTime + "; magneticSensitivity: " + magneticSensitivity);
            runAction(Gestures.TAP);
            delay = 5;
        }
    }

    public void changeMagneticSensitivity(int magneticSensitivity){
        this.magneticSensitivity = magneticSensitivity;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        super.onBind(intent);

        // Get here the sensors you need for your implementation
        List<Sensor> sensors = sensorManager.getSensorList(Sensor.TYPE_MAGNETIC_FIELD);
        sensorManager.registerListener(sensorEventListener, sensors.get(0), SensorManager.SENSOR_DELAY_NORMAL);
        lastEvent = new MySensorEvent();

        // Apply your needs


        // Finished onBind
        Log.d(TAG, "Finished onBind()");
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent){
        Log.d(TAG, "onUnbind()");
        sensorManager.unregisterListener(sensorEventListener);
        super.onUnbind(intent);
        return false;
    }
}
