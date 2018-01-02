package com.example.andreucortes.notouchmcontrol;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.KeyEvent;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public abstract class GestureListenerService extends Service {

    // Service variables
    private String TAG = "GestureListenerService";

    protected AudioManager audioManager;
    protected SensorManager sensorManager;

    // Service notifying variables
    final public static String ACTION_HANDLE_DATA = "39910301";
    final public static String EXTRA_DATA = "GestureListenerServiceDataInput";

    // Implementation variables
    public boolean pauseMusic;


    // Parameters
    public enum Parameters {SENSIBILITY, OTHER};

    // Different Gestures recognized
    public enum Gestures {TAP, DOUBLE_TAP, TRIPLE_TAP}

    public int lastEventsListSize = 100;
    public MySensorEvent currentEvent, lastEvent = new MySensorEvent();
    public ArrayList<MySensorEvent> lastEvents = new ArrayList<>(lastEventsListSize);

    public long  deltaTime = 0;
    public long delayBetweenRecognitions = 1000000;

    // Listener for accelerometer sensor.
    public SensorEventListener sensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            synchronized (this) {
                // get event
                currentEvent = new MySensorEvent(event);
                deltaTime = currentEvent.timestamp - lastEvent.timestamp;

                Log.d(TAG, "read event! Remaining delay = " + delayBetweenRecognitions + "; DeltaTime = " + deltaTime);

                boolean result = evaluateEvent();

                if (result && delayBetweenRecognitions < 0) {
                    Log.d(TAG, "onSensorChanged: Event recognized!");
                    make_sound("before");
                    runAction(Gestures.TAP);
                    delayBetweenRecognitions = deltaTime * lastEventsListSize;
                    //listen();
                    //save_last_event();
                } else {
                    // count timeout
                    delayBetweenRecognitions -= deltaTime;
                }

                // store past events
                lastEvent = new MySensorEvent(currentEvent);
                lastEvents.add(0, currentEvent);
                if (lastEvents.size() > lastEventsListSize) lastEvents.remove(lastEventsListSize);
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };

    public boolean evaluateEvent(){
        // Nothing to do here: please create your own Listener/Recognizer implementing evaluateEvent()
        return false;
    }

    public IBinder onBind(Intent intent){
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        audioManager  = (AudioManager)  getSystemService(Context.AUDIO_SERVICE);
        sensorManager.unregisterListener(sensorEventListener);
        return null;
    }

    public void changeDelay(int delay) {
        this.lastEventsListSize = delay;
    }

    public void setToggleType(boolean pause){
        pauseMusic = pause;
    }

    /***
     *  Maybe runAction needs to evolve to a switch to do different
     *  actions as Listeners recognize more and more gestures.
     */
    public void runAction(Gestures gestures){
        toggleMusic();
    }

    private void toggleMusic(){
        if(pauseMusic) {
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
            }, 50);
        }
        else {
            Log.d(TAG, "Into toggle music mute music stream...");
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_TOGGLE_MUTE, 0);
        }
    }

    public void update_results(){
        make_sound("after");
    }

    public void make_sound(String when){
        int sound = R.raw.appointed;
        switch(when){
            case "before":
                sound = R.raw.appointed;
                break;
            case "after":
                sound = R.raw.case_closed;
                break;
            case "ok":
                sound = R.raw.job_done;
                break;
        }
        //TODO: make it sound correctly. interrump streams, play sound, release streams
        int lastMusicVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        final MediaPlayer mediaPlayer = MediaPlayer.create(this, sound);

        /*
        int result = audioManager.requestAudioFocus(audioFocusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
        Log.d(TAG, "make_sound: requested audio focus is: " + result + " and granted is: " + AudioManager.AUDIOFOCUS_REQUEST_GRANTED);
        if(result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED){
            //audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 1, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);

            //MediaPlayer mediaPlayer = new MediaPlayer();
            //mediaPlayer.setAudioStreamType(AudioManager.STREAM_NOTIFICATION);
            /*try {

                //R.raw.appointed
                //mediaPlayer.setDataSource(getApplicationContext(), Uri.parse("android.resource://" + getPackageName()));
                //mediaPlayer.setDataSource(getApplicationContext(), Uri.parse("android.resource://" + getPackageName() + "/res/raw/appointed.mp3"));
                mediaPlayer.setDataSource(getApplicationContext(), Uri.parse("/data/app/" + getPackageName() + "-1" + "/raw/appointed.mp3"));
                //mediaPlayer.setDataSource(getApplicationContext(), Uri.parse("android.resource://" + getPackageName() + "/raw/appointed.mp3"));
                mediaPlayer.prepare();
            } catch (IOException e) {
                e.printStackTrace();
            }*/
        /*
            Log.d(TAG, "make_sound: sounding now?");

        }
        audioManager.abandonAudioFocus(audioFocusChangeListener);
        //audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, lastMusicVolume, 0);
        */
        mediaPlayer.start();
        mediaPlayer.release();
        Log.d(TAG, "make_sound: lastMusicVolume was : " + lastMusicVolume);
    }

    public void listen(){
        Log.d(TAG, "listen: listening for results!");
        Intent speechIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechIntent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Voice being recognized...");
        //startActivity(speechIntent, Integer.valueOf(ACTION_HANDLE_DATA));
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            final String action = intent.getAction();
            if (action != null) {
                switch (action) {
                    case ACTION_HANDLE_DATA:
                        handleData(intent.getStringArrayListExtra(EXTRA_DATA));
                        // Implement your handleData method. Remember not to confuse Intents, or even better make your own Parcelable
                        break;
                }
            }
        }
        return START_NOT_STICKY;
    }

    public void handleData(ArrayList data){
        Log.d(TAG, "handleData: data is: " + data.toString());
    }

    AudioManager.OnAudioFocusChangeListener audioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(int focusChange) {
            if(focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT){
                Log.d(TAG, "onAudioFocusChange: Pause playback");
            } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN){
                Log.d(TAG, "onAudioFocusChange: Resume playback");
            } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS){
                audioManager.abandonAudioFocus(audioFocusChangeListener);
            }
        }
    };
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
