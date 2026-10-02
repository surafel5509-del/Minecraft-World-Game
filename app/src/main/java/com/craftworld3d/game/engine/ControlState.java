package com.craftworld3d.game.engine;

/**
 * Input snapshot shared between the UI thread (writes) and the tick thread
 * (reads). Primitive fields only; races are benign.
 */
public final class ControlState {
    // movement joystick, -1..1
    public volatile float moveX;
    public volatile float moveZ;

    public volatile boolean jump;
    public volatile boolean sprint;
    public volatile boolean crouch;      // toggled by the UI button

    /** Break button held down. */
    public volatile boolean breaking;

    // vehicle controls (used while mounted)
    public volatile float vehicleThrottle; // -1..1
    public volatile float vehicleSteer;    // -1..1
    public volatile float vehiclePitch;    // -1..1 (planes/helicopter altitude)
    public volatile boolean horn;
    public volatile boolean headlights;

    public void reset() {
        moveX = moveZ = 0;
        jump = sprint = false;
        breaking = false;
        vehicleThrottle = vehicleSteer = vehiclePitch = 0;
        horn = false;
    }
}
