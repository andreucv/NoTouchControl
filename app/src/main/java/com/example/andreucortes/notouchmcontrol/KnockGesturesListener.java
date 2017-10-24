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

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class KnockGesturesListener extends GestureListenerService {

    private final String TAG = "KnockGesturesListner";
    private Binder binder = new LocalBinder();
    private boolean disableService = false;

    private float movement = 0;
    private long  deltaTime = 0;

    private float filter = 1.00E-6f;
    private float baseFilter = 1.00E-7f;
    private int sensibility = 0;
    private int zSensibility = 0;

    private MySensorEvent currentEvent, lastEvent;

    // Second approach variables
    private float derivativeFactor1, derivativeFactor2;
    private float integralFactor1;
    private long accumulatedTime;

    // Third approach variables
    private IntegralFactor integralFactor;

    // Forth approach variables
    private long lastShakeFactor;
    private long SHAKE_TIME_TO_WAIT_MS = 700;

    // Listener for accelerometer sensor.
    private SensorEventListener sensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            synchronized (this) {
                // get event
                currentEvent = new MySensorEvent(event);
                Log.d(TAG, "read event!");

                // comment the evaluation you don't want to debug
                evaluateEventFirstApproach();
                //evaluateEventSecondApproach();
                // TODO: acabar esto
                //evaluateEventThirdApproach();
                //evaluateEventForthApproach();

                // store past event
                lastEvent = new MySensorEvent(currentEvent);
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };

    /**
     *  First approach to an algorithm that calculates a movement
     */
    private void evaluateEventFirstApproach(){
        // Get variations from current event against lastEvent
        float deltaX = currentEvent.values[0] - lastEvent.values[0];
        float deltaY = currentEvent.values[1] - lastEvent.values[1];
        float deltaZ = currentEvent.values[2] - lastEvent.values[2];
        deltaTime = currentEvent.timestamp - lastEvent.timestamp;

        // Apply a low pass filter adjustable
        Log.d(TAG, "deltaZ positive: " + (deltaZ > 0? "true": "false") + " deltaZValue = " + deltaZ + "deltaZsensibility = " + zSensibility);
        if(deltaZ > zSensibility){
            // Calculate movement if passed the filter
            movement = (deltaX + deltaY + deltaZ* zSensibility) / deltaTime;
        }
        else movement = 0;

        deltaTime = currentEvent.timestamp - lastEvent.timestamp;

        Log.d(TAG, "Mov: " + movement);
        if (movement > (filter + baseFilter * sensibility * 2) && deltaTime > 17000000) {
            Log.d(TAG, "Filter pass with: " + movement);
            runAction(Gestures.TAP);
        }
    }

    /**
     *  Second approach to an algorithm that calculates a tap
     *  It uses the peak identification in the integral vector of sensor registered movement.
     *  In this approach I only investigate on the Z axis.
     */
    private void evaluateEventSecondApproach(){
        derivativeFactor1 = currentEvent.values[2] - lastEvent.values[2];
        integralFactor1 = derivativeFactor1 - derivativeFactor2;
        Log.d(TAG, "derivativeFactor1 = "+derivativeFactor1);
        Log.d(TAG, "derivativeFactor2 = "+derivativeFactor2);
        Log.d(TAG, "integralFactor1   = "+integralFactor1 + "; zSensibility = "+zSensibility);
        if(integralFactor1 > zSensibility){
            movement = 100;

        }
        else movement = 0;

        accumulatedTime = 17000001;

        Log.d(TAG, "Mov: " + movement);
        if (movement > (filter + baseFilter * sensibility * 2) && accumulatedTime > 17000000) {
            Log.d(TAG, "Filter pass with: " + movement);
            runAction(Gestures.TAP);
        }

        derivativeFactor2 = derivativeFactor1;
    }

    private void evaluateEventThirdApproach(){
        float maxIntegral = integralFactor.addDerivativeFactor(currentEvent.values[2] - lastEvent.values[2]);

        if(maxIntegral > zSensibility){
            runAction(Gestures.TAP);
        }
    }

    /**
     *  Forth approach.
     *  Not valid. Possibly we could use this approach mixed with the second or third ones.
     */
    private void evaluateEventForthApproach(){
        if((currentEvent.timestamp - lastShakeFactor) > SHAKE_TIME_TO_WAIT_MS){
            lastShakeFactor = currentEvent.timestamp;

            float x = currentEvent.values[0] / SensorManager.GRAVITY_EARTH;
            float y = currentEvent.values[1] / SensorManager.GRAVITY_EARTH;
            float z = currentEvent.values[2] / SensorManager.GRAVITY_EARTH;

            double force = Math.sqrt(x*x+y*y+z*z);
            if(force > sensibility){
                runAction(Gestures.TAP);
            }
        }
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
        integralFactor = new IntegralFactor();

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
        return START_STICKY;
    }

    public void changeSensibility(int sensibility){
        this.sensibility = sensibility;
    }

    public void changeZSensibility(int zsensibility) {
        this.zSensibility = zsensibility;
    }

    public void setDisableService(){
        disableService = true;
        stopSelf();
    }
}

class MySensorEvent {
    public float[] values;
    public long    timestamp;

    public MySensorEvent(){
        values = new float[3];
        timestamp = 0;
    }

    public MySensorEvent(SensorEvent sensorEvent){
        values = sensorEvent.values.clone();
        timestamp = sensorEvent.timestamp;
    }

    public MySensorEvent(MySensorEvent sensorEvent){
        values = sensorEvent.values.clone();
        timestamp = sensorEvent.timestamp;
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
