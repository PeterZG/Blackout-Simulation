package unsw.item;

import unsw.utils.MathsHelper;
import unsw.utils.Angle;

/**
 * relay satellite
 */
public class RelaySatellite extends Satellite  {

    public RelaySatellite(String id, double height, Angle position) {
        super(id, 300_000, height, position, 1500, "RelaySatellite");

        setMaxFile(0);
        setMaxStore(0);
        setBandwidthSend(Integer.MAX_VALUE);
        setBandwidthReceive(Integer.MAX_VALUE);
    }

    @Override
    public boolean canSupport(String type) {
        return true;
    }

    @Override
    public void move() {
        super.move();
        if (getPosition().toDegrees() >= 190) {
            setDirection(MathsHelper.CLOCKWISE);
        }
        if (getPosition().toDegrees() <= 140) {
            setDirection(MathsHelper.ANTI_CLOCKWISE);
        }
    }
}
