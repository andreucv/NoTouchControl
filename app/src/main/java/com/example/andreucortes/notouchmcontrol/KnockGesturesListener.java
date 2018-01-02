package com.example.andreucortes.notouchmcontrol;

import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import static java.lang.Math.abs;

public class KnockGesturesListener extends GestureListenerService {

    private final String TAG = "KnockGesturesListner";
    private Binder binder = new LocalBinder();

    private float movement = 0;

    private float filter = 1.00E-6f;
    private float baseFilter = 1.00E-7f;
    private int sensibility = 0;
    private int zSensibility = 0;

    public void changeSensibility(int sensibility){
        this.sensibility = sensibility;
    }

    public void changeZSensibility(int zSensibility) {
        this.zSensibility = zSensibility;
    }

    /**
     *  First approach to an algorithm that calculates a movement
     */
    @Override
    public boolean evaluateEvent() {
        // Get variations from current event against lastEvent
        float deltaX = currentEvent.values[0] - lastEvent.values[0];
        float deltaY = currentEvent.values[1] - lastEvent.values[1];
        float deltaZ = currentEvent.values[2] - lastEvent.values[2];

        // Apply a low pass filter adjustable
        Log.d(TAG, "deltaZ positive: " + (deltaZ > 0 ? "true" : "false") + "; deltaZValue = " + deltaZ + "; deltaZsensibility = " + zSensibility);
        if (abs(deltaZ) > zSensibility) {
            // Calculate movement if passed the filter
            movement = (abs(deltaX) + abs(deltaY) + abs(deltaZ) * zSensibility) / deltaTime;
        } else movement = 0;

        if (movement > (filter + baseFilter * sensibility * 2) && deltaTime > 17000000) {
            Log.d(TAG, "Mov: " + movement);
            return true;
        }

        return false;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        super.onBind(intent);

        // Get here the sensors you need for your implementation
        List<Sensor> sensors = sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(sensorEventListener, sensors.get(0), SensorManager.SENSOR_DELAY_NORMAL);
        lastEvent = new MySensorEvent();

        // Apply your needs
        sensibility = intent.getIntExtra("sensibility", 0);

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

    public class LocalBinder extends Binder {
        public KnockGesturesListener getService(){
            return KnockGesturesListener.this;
        }
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }
}

class IntegralFactor {
    private Queue<Float> derivativeFactors;
    private Queue<Float> integralFactors;

    final int dimension = 10;

    public IntegralFactor(){
        derivativeFactors = new LinkedList<>();
        integralFactors = new LinkedList<>();
    }

    public float addDerivativeFactor(float derivativeFactor){
        if(derivativeFactors.size() < dimension){
            derivativeFactors.add(derivativeFactor);
        }
        else{
            derivativeFactors.poll();
            derivativeFactors.add(derivativeFactor);
        }
        return updateMaxIntegralFactor();
    }

    public float updateMaxIntegralFactor(){
        Iterator iterator = derivativeFactors.iterator();
        while(iterator.hasNext()){
            //integralFactors.add((float)iterator.next()-((float)iterator.next());
        }
        if(integralFactors.size() > 0) return Collections.max(integralFactors);
        else return 0;
    }

}
