package com.example.andreucortes.notouchmcontrol;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class MainActivity extends Activity {

    private final String TAG = "MainActivity";

    private enum RecognizeType {ACCELEROMETER, MAGNETIC_FIELD};

    RecognizeType currentRecognizeType = RecognizeType.ACCELEROMETER;
    private TextView currentRecognizeTypeText;

    // present here your GestureListeners
    private KnockGesturesListener       knockGesturesListener;
    private MagneticGesturesListener    magneticGesturesListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final SeekBar overallSensibilitySeekBar;
        final TextView overallSensitivityTextValue;
        final SeekBar zAxisSensitivitySeekBar;
        final TextView zAxisSensitivityTextValue;
        final SeekBar delayBetweenEventsSeekBar;
        final TextView delayBetweenEventsTextValue;
        final SeekBar magneticSensitivitySeekBar;
        final TextView magneticSensitivityTextValue;

        ToggleButton toggleMusicButton;
        Button accelerometerButton, magneticFieldButton;

        overallSensitivityTextValue = (TextView) findViewById(R.id.overall_sensitivity_text_value);
        overallSensibilitySeekBar = (SeekBar) findViewById(R.id.overall_sensitivity_seekbar);
        overallSensibilitySeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                overallSensitivityTextValue.setText("" + seekBar.getProgress() + "/" + seekBar.getMax());
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                overallSensitivityTextValue.setText("" + seekBar.getProgress() + "/" + seekBar.getMax());
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                knockGesturesListener.changeSensibility(seekBar.getProgress());
            }
        });

        zAxisSensitivityTextValue = (TextView) findViewById(R.id.z_axis_sensitivity_text_value);
        zAxisSensitivitySeekBar = (SeekBar) findViewById(R.id.z_axis_sensitivity_seekbar);
        zAxisSensitivitySeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                zAxisSensitivityTextValue.setText(seekBar.getProgress() + "/" + seekBar.getMax());
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                zAxisSensitivityTextValue.setText(seekBar.getProgress() + "/" + seekBar.getMax());
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                knockGesturesListener.changeZSensibility(seekBar.getProgress());
            }
        });

        delayBetweenEventsTextValue = (TextView) findViewById(R.id.delay_between_events_text_value);
        delayBetweenEventsSeekBar = (SeekBar) findViewById(R.id.delay_between_events_seekbar);
        delayBetweenEventsSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                delayBetweenEventsTextValue.setText(seekBar.getProgress() + "/" + seekBar.getMax());
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                delayBetweenEventsTextValue.setText(seekBar.getProgress() + "/" + seekBar.getMax());
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                knockGesturesListener.changeDelay(seekBar.getProgress());
            }
        });


        magneticSensitivityTextValue = (TextView) findViewById(R.id.magnetic_sensitivity_text_value);
        magneticSensitivitySeekBar = (SeekBar) findViewById(R.id.magnetic_sensitivity_seekbar);
        magneticSensitivitySeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                magneticSensitivityTextValue.setText(seekBar.getProgress() + "/" + seekBar.getMax());
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                magneticSensitivityTextValue.setText(seekBar.getProgress() + "/" + seekBar.getMax());
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                magneticGesturesListener.changeMagneticSensitivity(seekBar.getProgress());
            }
        });


        overallSensitivityTextValue.setText("" + overallSensibilitySeekBar.getProgress() + "/" + overallSensibilitySeekBar.getMax());
        zAxisSensitivityTextValue.setText(zAxisSensitivitySeekBar.getProgress() + "/" + zAxisSensitivitySeekBar.getMax());
        magneticSensitivityTextValue.setText(magneticSensitivitySeekBar.getProgress() + "/" + magneticSensitivitySeekBar.getMax());

        toggleMusicButton = (ToggleButton) findViewById(R.id.toggleMusicButton);
        toggleMusicButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isMyServiceRunning(KnockGesturesListener.class)) knockGesturesListener.setToggleType(isChecked);
                if(isMyServiceRunning(MagneticGesturesListener.class)) magneticGesturesListener.setToggleType(isChecked);
            }
        });

        accelerometerButton = (Button) findViewById(R.id.buttonAccelerometer);
        magneticFieldButton = (Button) findViewById(R.id.buttonMagneticField);

        accelerometerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                disconnectActualServices();
                currentRecognizeType = RecognizeType.ACCELEROMETER;
                currentRecognizeTypeText.setText(currentRecognizeType.toString());
                bindService(new Intent(getApplicationContext(), KnockGesturesListener.class), knockServiceConnection, BIND_AUTO_CREATE);
            }
        });
        magneticFieldButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                disconnectActualServices();
                currentRecognizeType = RecognizeType.MAGNETIC_FIELD;
                currentRecognizeTypeText.setText(currentRecognizeType.toString());
                bindService(new Intent(getApplicationContext(), MagneticGesturesListener.class), magneticServiceConnection, BIND_AUTO_CREATE);
            }
        });

        currentRecognizeTypeText = (TextView) findViewById(R.id.currentRecognizeTypeText);
        currentRecognizeTypeText.setText(currentRecognizeType.toString());

        bindService(new Intent(getApplicationContext(), KnockGesturesListener.class), knockServiceConnection, BIND_AUTO_CREATE);
        bindService(new Intent(getApplicationContext(), MagneticGesturesListener.class), magneticServiceConnection, BIND_AUTO_CREATE);
        disconnectActualServices();

        Log.d(TAG, "Finished onCreate()");
    }

    private final ServiceConnection knockServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            knockGesturesListener = ((KnockGesturesListener.LocalBinder) service).getService();
            Toast.makeText(getApplicationContext(), "Knock Service Connected", Toast.LENGTH_SHORT);
            Log.d(TAG, "Connected");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Toast.makeText(getApplicationContext(), "Knock Service Disconnected", Toast.LENGTH_SHORT);
            Log.d(TAG, "Disconnected");
        }
    };

    private final ServiceConnection magneticServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            magneticGesturesListener = ((MagneticGesturesListener.LocalBinder) service).getService();
            Toast.makeText(getApplicationContext(), "Magnetic Service Connected", Toast.LENGTH_SHORT);
            Log.d(TAG, "Connected");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Toast.makeText(getApplicationContext(), "Magnetic Service Disconnected", Toast.LENGTH_SHORT);
            Log.d(TAG, "Disconnected");
        }
    };

    private void disconnectActualServices(){
        if(isMyServiceRunning(KnockGesturesListener.class)) {
            unbindService(knockServiceConnection);
            stopService(new Intent(this, KnockGesturesListener.class));
        }
        if(isMyServiceRunning(MagneticGesturesListener.class)) {
            unbindService(magneticServiceConnection);
        }
    }

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
        disconnectActualServices();
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for(ActivityManager.RunningServiceInfo serviceInfo : manager.getRunningServices(Integer.MAX_VALUE)){
            if(serviceClass.getName().equals(serviceInfo.service.getClassName())){
               return true;
            }
        }
        return false;
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data){
        super.onActivityResult(requestCode, resultCode, data);
        if(String.valueOf(requestCode) == KnockGesturesListener.ACTION_HANDLE_DATA && resultCode == RESULT_OK){
            notifyService(data);
        }
    }

    private void notifyService(final Intent data){
        final Intent intent = new Intent(this, KnockGesturesListener.class);
        intent.setAction(KnockGesturesListener.ACTION_HANDLE_DATA);
        intent.putExtra(KnockGesturesListener.EXTRA_DATA, data);
        startService(intent);
    }
}
