package com.example.andreucortes.notouchmcontrol;

import android.app.Service;
import android.content.Intent;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.os.IBinder;
import android.util.Log;
import android.view.KeyEvent;

import java.util.Timer;
import java.util.TimerTask;

public abstract class GestureListenerService extends Service {

    // Service variables
    private String TAG = "GestureListenerService";

    protected AudioManager audioManager;
    protected SensorManager sensorManager;

    // Implementation variables
    public boolean pauseMusic;


    // Parameters
    public enum Parameters {SENSIBILITY, OTHER};

    // Different Gestures recognized
    public enum Gestures {TAP, DOUBLE_TAP, TRIPLE_TAP}

    /**
    public class NTCSensorEvent {
        public float[] values;
        public long    timestamp;

        public NTCSensorEvent(){
            values = new float[3];

        }
    } **/

    public IBinder onBind(Intent intent){
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        audioManager  = (AudioManager)  getSystemService(AUDIO_SERVICE);

        return null;
    }

    /*
     *      Maybe runAction needs to evolve to a switch to do different actions as Listeners recognize
     *      more and more gestures.
     */
    public void runAction(Gestures gestures){
        toggleMusic();
    }

    public void setToggleType(boolean pause){
        pauseMusic = pause;
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


}
