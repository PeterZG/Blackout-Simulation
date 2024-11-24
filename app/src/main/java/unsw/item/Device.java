package unsw.item;

import unsw.utils.MathsHelper;

import unsw.utils.Angle;

/**
 * Base class of devices
 */
public class Device extends Entity {

    public Device(String id, double range, Angle position, String type, double velocity) {
        super(id, range, position, MathsHelper.RADIUS_OF_JUPITER, type, velocity);
        setDirection(MathsHelper.ANTI_CLOCKWISE);
    }

    public static Device createDevice(String deviceId, String type, Angle position) {
        return createDevice(deviceId, type, position, false);
    }

    public static Device createDevice(String deviceId, String type, Angle position, boolean isMoving) {
        if ("HandheldDevice".equals(type)) {
            return new HandheldDevice(deviceId, position, isMoving);
        } else if ("LaptopDevice".equals(type)) {
            return new LaptopDevice(deviceId, position, isMoving);
        } else if ("DesktopDevice".equals(type)) {
            return new DesktopDevice(deviceId, position, isMoving);
        } else {
            return new Device(deviceId, 0, position, type, 0);
        }
    }

    @Override
    public void move() {
        super.move();
        Slope slope = Slope.inSlope(getPosition().toDegrees());
        if (slope == null) {
            setHeight(MathsHelper.RADIUS_OF_JUPITER);
        } else {
            double newHeight = getHeight() + getAngularVelocity() * slope.getGradient();
            if (newHeight < MathsHelper.RADIUS_OF_JUPITER) {
                newHeight = MathsHelper.RADIUS_OF_JUPITER;
            }
            setHeight(newHeight);
        }
    }
}
