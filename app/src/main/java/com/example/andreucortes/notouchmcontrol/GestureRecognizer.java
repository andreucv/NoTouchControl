package com.example.andreucortes.notouchmcontrol;

/**
 * Created by andreucortes on 31/01/2017.
 */
public class GestureRecognizer implements Runnable{

    public enum Axis{
        X(0), Y(1), Z(2), MAX_AXIS(3);

        public final int num;

        private Axis(int num){ this.num = num; }

        private int getAxis(Axis axis){
            return (int) axis.num;
        }
    }

    private long lastUpdate = 0;
    private float[] currentValues, previousValues;
    private float minimumMovement = 1.00E-6f;
    private int sensibility = 10;
    private int [] axisSensibility;
    private long limitIntraGesture = 1000000000;

    public GestureRecognizer(int sensibility)
    {
        this.sensibility = sensibility;
        previousValues   = new float[]{0, 0, 0};
        currentValues    = new float[]{0, 0, 0};
        lastUpdate       = 0;
        axisSensibility  = new int  []{1, 3, 1};
    }

    public GestureRecognizer(int sensibility, float minimumMovement)
    {
        this.sensibility     = sensibility;
        this.minimumMovement = minimumMovement;
        previousValues       = new float[]{0, 0, 0};
        currentValues        = new float[]{0, 0, 0};
        lastUpdate           = 0;
        axisSensibility      = new int  []{1, 3, 1};
    }

    public void changeSensibility(int sensibility){
        this.sensibility = sensibility;
    }

    public void changeMinimumMovement(float minimumMovement){
        this.minimumMovement = minimumMovement;
    }

    public void changeAxisSensibility(int [] axisSensibility){
        this.axisSensibility = axisSensibility;
    }

    @Override
    public void run(){

    }

    public GestureState.State determineGesture(float [] values, long currentTime){
        // Here goes the algorithm
        GestureState.State result = GestureState.State.NONE;

        if(lastUpdate == 0){
            previousValues = values;
            lastUpdate = currentTime;
        }

        long time_difference = currentTime - lastUpdate;
        if(time_difference > 0) {
            float movement = 0;
            for (int i = 0; i < Axis.MAX_AXIS.num; i++) {
                movement += Math.abs(currentValues[i] * axisSensibility[i] - previousValues[i] * axisSensibility[i]) / time_difference;
            }

            if (movement > minimumMovement) {
                if (currentTime - lastUpdate >= limitIntraGesture) {
                    result = GestureState.State.TAP;
                }
            }
        }

        return result;
    }

    /***
    if (prevX == 0 && prevY == 0 && prevZ == 0) {
        last_update = current_time;
        last_movement = current_time;
        prevX = currX;
        prevY = currY;
        prevZ = currZ;
    }


    if (time_difference > 0) {
        float movement = Math.abs((currY*3+currZ) - (prevY*3 + prevZ)) / time_difference;
        int limit = 1500;
        float min_movement = 1.00E-6f + sensibility * 2E-8f;
        if (movement > min_movement) {
            if (current_time - last_movement >= limit){
                moved += "100 ";
            }
            last_movement = current_time;
        }
        prevX = currX;
        prevY = currY;
        prevZ = currZ;
        last_update = current_time;
    }

    if(moved.compareTo("100 100 ") == 0){
        Toast.makeText(getApplicationContext(), "Gesto play/pause", Toast.LENGTH_SHORT).show();
        moved = "";
        if(sounding) audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
        else audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_TOGGLE_MUTE, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
        sounding = !sounding;
    }

    int limit = 1000000000;
    if((current_time - last_movement) > limit){
        moved = "";
    }***/
}
