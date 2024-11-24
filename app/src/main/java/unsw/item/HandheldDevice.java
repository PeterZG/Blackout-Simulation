package unsw.item;

import unsw.utils.Angle;

/**
 * handheld device
 */
public class HandheldDevice extends Device  {

    public HandheldDevice(String id, Angle position) {
        this(id, position, false);
    }

    public HandheldDevice(String id, Angle position, boolean isMoving) {
        super(id, 50_000, position, "HandheldDevice", isMoving ? 50 : 0);
    }

}
