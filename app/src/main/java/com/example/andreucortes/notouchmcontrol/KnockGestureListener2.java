package com.example.andreucortes.notouchmcontrol;

import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.speech.SpeechRecognizer;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class KnockGestureListener2 extends GestureListenerService {

    private final String TAG = "KnockGesturesListner";
    private Binder binder = new LocalBinder();
    private boolean disableService = false;

    private float movement = 0;
    private long  deltaTime = 0;
    private long delayBetweenRecognitions = 1000000;

    private float filter = 1.00E-6f;
    private float baseFilter = 1.00E-7f;
    private int sensibility = 0;
    private int zSensibility = 0;
    private MySensorEvent currentEvent, lastEvent;
    private int lastEventsListSize = 100;
    private ArrayList<MySensorEvent> lastEvents = new ArrayList<>(lastEventsListSize);

    // Second approach variables
    private float derivativeFactor1, derivativeFactor2;
    private float integralFactor1;
    private long accumulatedTime;

    // Third approach variables
    private IntegralFactor integralFactor;

    // Forth approach variables
    private long lastShakeFactor;
    private long SHAKE_TIME_TO_WAIT_MS = 700;

    // Fifth approach variables
    SpeechRecognizer speechRecognizer;
    Intent speechIntent;

    // Listener for accelerometer sensor.
    private SensorEventListener sensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            synchronized (this) {
                // get event
                currentEvent = new MySensorEvent(event);
                Log.d(TAG, "read event! delay = " + delayBetweenRecognitions + "; deltatime = " + deltaTime);

                // comment the evaluation you don't want to debug
                boolean result = evaluateEventFirstApproach();
                //evaluateEventSecondApproach();
                // TODO: acabar esto
                //evaluateEventThirdApproach();
                //evaluateEventForthApproach();
                //evaluateEventFifthApproach();

                if(result && delayBetweenRecognitions < 0){
                    Log.d(TAG, "Filter pass with: " + movement);
                    runAction(Gestures.TAP);
                    make_sound("before");
                    // listen();
                    delayBetweenRecognitions = deltaTime*lastEventsListSize;
                }
                else{
                    // count timeout
                    delayBetweenRecognitions -= deltaTime;
                }

                // store past events
                lastEvent = new MySensorEvent(currentEvent);
                lastEvents.add(0, currentEvent);
                if(lastEvents.size() > lastEventsListSize) lastEvents.remove(lastEventsListSize);
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };

    /**
     *  First approach to an algorithm that calculates a movement
     */
    private boolean evaluateEventFirstApproach(){
        // Get variations from current event against lastEvent
        float deltaX = currentEvent.values[0] - lastEvent.values[0];
        float deltaY = currentEvent.values[1] - lastEvent.values[1];
        float deltaZ = currentEvent.values[2] - lastEvent.values[2];
        deltaTime = currentEvent.timestamp - lastEvent.timestamp;

        // Apply a low pass filter adjustable
        Log.d(TAG, "deltaZ positive: " + (deltaZ > 0? "true": "false") + "; deltaZValue = " + deltaZ + "; deltaZsensibility = " + zSensibility);
        if(deltaZ > zSensibility){
            // Calculate movement if passed the filter
            movement = (deltaX + deltaY + deltaZ* zSensibility) / deltaTime;
        }
        else movement = 0;

        deltaTime = currentEvent.timestamp - lastEvent.timestamp;

        Log.d(TAG, "Mov: " + movement);
        if (movement > (filter + baseFilter * sensibility * 2) && deltaTime > 17000000) {
            return true;
        }

        return false;
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

    /**
     *  Forth approach.
     *  Not valid. Possibly we could use this approach mixed with the second or third ones.
     */
    private void evaluateEventFifthApproach(){
        if((currentEvent.timestamp - lastShakeFactor) > SHAKE_TIME_TO_WAIT_MS){
            lastShakeFactor = currentEvent.timestamp;

            float x = currentEvent.values[0] / SensorManager.GRAVITY_EARTH;
            float y = currentEvent.values[1] / SensorManager.GRAVITY_EARTH;
            float z = currentEvent.values[2] / SensorManager.GRAVITY_EARTH;

            double force = Math.sqrt(x*x+y*y+z*z);
            if(force > sensibility){
                runAction(Gestures.TAP);
                make_sound("before");
                // listen();
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

        /**
         speechRecognizer = SpeechRecognizer.createSpeechRecognizer(getApplicationContext());
         speechRecognizer.setRecognitionListener(new RecognitionListener() {
        @Override
        public void onReadyForSpeech(Bundle params) {

        }

        @Override
        public void onBeginningOfSpeech() {

        }

        @Override
        public void onRmsChanged(float rmsdB) {

        }

        @Override
        public void onBufferReceived(byte[] buffer) {

        }

        @Override
        public void onEndOfSpeech() {

        }

        @Override
        public void onError(int error) {
        if(error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT){
        // sound
        listen();
        }
        }

        @Override
        public void onResults(Bundle results) {
        ArrayList<String> result = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        Log.d(TAG, "onResults: results after listen are: " + result.toString());
        // search for yes and no
        if(result.contains("yes")) update_results();
        }

        @Override
        public void onPartialResults(Bundle partialResults) {

        }

        @Override
        public void onEvent(int eventType, Bundle params) {

        }
        });

         speechIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
         speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
         speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.ENGLISH);
         **/
        // Finished onBind
        Log.d(TAG, "Finished onBind()");
        return binder;
    }

    /**
     public void listen(){
     Log.d(TAG, "listen: listening for results!");
     Handler mainHandler = new Handler(Looper.getMainLooper());
     Runnable myRunnable = new Runnable() {
    @Override
    public void run() {
    Log.d(TAG, "listen: listening for results in the runnable!");
    speechRecognizer.startListening(speechIntent);
    }
    };
     mainHandler.post(myRunnable);
     Runnable stopRunnable = new Runnable() {
    @Override
    public void run() {
    Log.d(TAG, "listen: stop listening in runnable");
    speechRecognizer.stopListening();
    }
    };
     mainHandler.postAtTime(stopRunnable, 2000000);
     }
     **/

    public void make_sound(String when){
        Uri path = Uri.parse("android.resource://" + getPackageName() + "/raw/appointed.mp3");;
        switch(when){
            case "before":
                try {
                    path = Uri.parse("android.resource://" + getPackageName() + "/raw/appointed.mp3");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case "after":
                try {
                    path = Uri.parse("android.resource://" + getPackageName() + "/raw/case_closed.mp3");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case "ok":
                try {
                    path = Uri.parse("android.resource://" + getPackageName() + "/raw/job_done.mp3");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
        }
        Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), path);
        r.play();
    }

    public void update_results(){
        make_sound("after");
    }

    @Override
    public boolean onUnbind(Intent intent){
        Log.d(TAG, "onUnbind()");
        sensorManager.unregisterListener(sensorEventListener);
        super.onUnbind(intent);
        return false;
    }

    public class LocalBinder extends Binder {
        public KnockGestureListener2 getService(){
            return KnockGestureListener2.this;
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

class MySensorEvent2 {
    public float[] values;
    public long    timestamp;

    public MySensorEvent2(){
        values = new float[3];
        timestamp = 0;
    }

    public MySensorEvent2(SensorEvent sensorEvent){
        values = sensorEvent.values.clone();
        timestamp = sensorEvent.timestamp;
    }

    public MySensorEvent2(MySensorEvent sensorEvent){
        values = sensorEvent.values.clone();
        timestamp = sensorEvent.timestamp;
    }
}

class IntegralFactor2 {
    private Queue<Float> derivativeFactors;
    private Queue<Float> integralFactors;

    final int dimension = 10;

    public IntegralFactor2(){
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
