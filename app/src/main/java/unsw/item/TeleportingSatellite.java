package unsw.item;

import unsw.utils.MathsHelper;
import unsw.utils.Angle;

/**
 * teleporting satellite
 */
public class TeleportingSatellite extends Satellite  {

    public TeleportingSatellite(String id, double height, Angle position) {
        super(id, 200_000, height, position, 1000, "TeleportingSatellite");

        setDirection(MathsHelper.ANTI_CLOCKWISE);

        setMaxFile(Integer.MAX_VALUE);
        setMaxStore(200);
        setBandwidthSend(10);
        setBandwidthReceive(15);
    }

    @Override
    public boolean canSupport(String type) {
        return true;
    }

    @Override
    public void move() {
        super.move();
        if (getPosition().toDegrees() >= 180) {
            setPosition(Angle.fromDegrees(0));
        }
    }
}
