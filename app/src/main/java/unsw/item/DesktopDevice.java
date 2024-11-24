package unsw.item;
import unsw.utils.Angle;

/**
 * desktop device
 */
public class DesktopDevice extends Device  {
    public DesktopDevice(String id, Angle position) {
        this(id, position, false);
    }

    public DesktopDevice(String id, Angle position, boolean isMoving) {
        super(id, 200_000, position, "DesktopDevice", isMoving ? 20 : 0);
    }
}

