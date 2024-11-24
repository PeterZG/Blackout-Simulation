package unsw.item;
import unsw.utils.Angle;

/**
 * laptop device
 */
public class LaptopDevice extends Device  {

    public LaptopDevice(String id, Angle position) {
        this(id, position, false);
    }

    public LaptopDevice(String id, Angle position, boolean isMoving) {
        super(id, 100_000, position, "LaptopDevice", isMoving ? 30 : 0);
    }
}
