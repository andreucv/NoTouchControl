package com.example.andreucortes.notouchmcontrol;

import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.KeyEvent;

import com.example.andreucortes.notouchmcontrol.GestureState.State;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class GestureListenerService extends Service {

    private final String TAG = "GestureListenerService";

    private float currentX,  currentY,  currentZ;
    private float previousX, previousY, previousZ;
    private float movement = 0;
    private long currentTime = 0;
    private long lastUpdate  = 0;
    private float filter = 1.00E-6f;
    private float baseFilter = 1.00E-7f;
    private int sensibility = 0;
    private int zsensibility = 0;
    private float limitIntraGestures = 1000000000;
    private long timeDifference = 0;
    private State currentState;

    private SensorManager sensorManager;
    private GestureRecognizer gestureRecognizer;

    private Binder binder = new LocalBinder();

    private SensorEventListener sensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            synchronized (this) {
                currentTime = event.timestamp;
                currentX = event.values[0];
                currentY = event.values[1];
                currentZ = event.values[2];

                calculateMovement();

                timeDifference = currentTime - lastUpdate;
                Log.d(TAG, "Mov: " + movement);
                if (movement > (filter + baseFilter * sensibility * 2) && timeDifference > 17000000) {
                    Log.d(TAG, "Filter pass with: " + movement);
                    currentState = State.TAP;
                    runAction();
                }

                if (currentTime > limitIntraGestures) currentState = State.NONE;

                previousX = currentX;
                previousY = currentY;
                previousZ = currentZ;
                lastUpdate = currentTime;
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };

    private void calculateMovement(){
        float deltaX = currentX - previousX;
        float deltaY = currentY - previousY;
        float deltaZ = currentZ - previousZ;
        long  deltaTime = currentTime - lastUpdate;
        if(deltaZ > zsensibility){
            Log.d(TAG, "deltaZ positive: " + (deltaZ > 0? "true": "false") + " deltaZValue = " + deltaZ + "deltaZsensibility = " + zsensibility);
            movement = (deltaX + deltaY + deltaZ*zsensibility) / deltaTime;
        }
        else movement = 0;
    }

    private void recognizeGesture(){
        float [] values = new float[]{0,0,0};
        Log.d(TAG, "entered in recognizeGesture()");
        State gestureRecognized = gestureRecognizer.determineGesture(values, currentTime);
        currentState = finiteStateMachine(gestureRecognized);
        runAction();
        Log.d(TAG, "finished recognizeGesture()");
    }

    private State finiteStateMachine(State previousResult){
        State result = previousResult;
        if(currentState == State.TAP && previousResult == State.TAP){
            result = State.DOUBLE_TAP;
        }
        else if(currentState == State.DOUBLE_TAP && previousResult == State.TAP){
            result = State.TRIPLE_TAP;
        }
        return result;
    }

    private void runAction(){
        if(currentState == State.TAP){
            Log.d(TAG, "Toggling Music @:" + currentTime + " with sensibility: " + sensibility);
            toggleMusic();
        }
    }

    private void toggleMusic(){
        Intent mediaEvent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        KeyEvent event = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
        mediaEvent.putExtra(Intent.EXTRA_KEY_EVENT, event);
        getApplicationContext().sendBroadcast(mediaEvent);

        (new Timer()).schedule(new TimerTask() {
            @Override
            public void run() {
                Intent mediaEvent = new Intent(Intent.ACTION_MEDIA_BUTTON);
                KeyEvent event = new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
                mediaEvent.putExtra(Intent.EXTRA_KEY_EVENT, event);
                getApplicationContext().sendBroadcast(mediaEvent);
            }
        }, 100);
    }

    /***
    private Runnable runRecognizeGesture = new Runnable() {
        public void run() {
            currentState = gestureRecognizer.determineGesture(values, currentTime);
        }
    };***/

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        sensorManager = (SensorManager) getApplicationContext().getSystemService(SENSOR_SERVICE);
        List<Sensor> sensors = sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(sensorEventListener, sensors.get(0), SensorManager.SENSOR_DELAY_NORMAL);
        currentTime = System.currentTimeMillis();
        sensibility = intent.getIntExtra("sensibility", 0);
        gestureRecognizer = new GestureRecognizer(sensibility);
        currentState = State.NONE;
        Log.d(TAG, "Finished onBind()");
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent){
        return super.onUnbind(intent);
    }

    public class LocalBinder extends Binder {
        public GestureListenerService getService(){
            return GestureListenerService.this;
        }
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        sensorManager = (SensorManager) getApplicationContext().getSystemService(SENSOR_SERVICE);
        List<Sensor> sensors = sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER);
        boolean result = sensorManager.registerListener(sensorEventListener, sensors.get(0), SensorManager.SENSOR_DELAY_NORMAL);
        currentTime = System.currentTimeMillis();
        gestureRecognizer = new GestureRecognizer(intent.getIntExtra("sensibility", 10));
        currentState = State.NONE;
        return START_STICKY;
    }

    public void changeSensibility(int sensibility){
        this.sensibility = sensibility;
    }

    public void changeZSensibility(int zsensibility) {
        this.zsensibility = zsensibility;
    }
}
