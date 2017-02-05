package com.example.andreucortes.notouchmcontrol;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private final String TAG = "MainActivity";

    private SeekBar seekBarSensibility;
    private TextView helloText;

    int sensibility;

    private GestureListenerService gestureListenerService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        helloText = (TextView) findViewById(R.id.hello);

        seekBarSensibility = (SeekBar) findViewById(R.id.sensibility_bar);
        seekBarSensibility.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                sensibility = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                helloText.setText("" + seekBarSensibility.getProgress() + "/" + seekBarSensibility.getMax());
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                helloText.setText("" + seekBarSensibility.getProgress() + "/" + seekBarSensibility.getMax());
            }
        });

        helloText.setText("" + seekBarSensibility.getProgress() + "/" + seekBarSensibility.getMax());
        Intent gestureListenerServiceIntent = new Intent(this, GestureListenerService.class);
        bindService(gestureListenerServiceIntent, serviceConnection, BIND_AUTO_CREATE);
        Log.d(TAG, "Finished MainActivity.onCreate()");
    }

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            gestureListenerService = ((GestureListenerService.LocalBinder) service).getService();
            Toast.makeText(getApplicationContext(), "Connected service", Toast.LENGTH_SHORT);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Toast.makeText(getApplicationContext(), "Disconnected service", Toast.LENGTH_SHORT);
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(serviceConnection);
    }
}
