package com.example.andreucortes.notouchmcontrol;

/**
 * Created by andreucortes on 03/02/2017.
 */
public class GestureState {
    public enum State {
        NONE(0), TAP(1), DOUBLE_TAP(2), TRIPLE_TAP(3), VERTICAL_TAP(8);

        public final int num;

        private State(int num) {
            this.num = num;
        }

        public int getNextState(){
            return this.num + 1;
        }

        public int getInt(){
            return this.num;
        }
    }
}
