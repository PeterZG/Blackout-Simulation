package unsw.item;

import unsw.utils.Angle;

/**
 * standard statellite
 */
public class StandardSatellite extends Satellite  {

    public StandardSatellite(String id, double height, Angle position) {
        super(id, 150_000, height, position, 2500, "StandardSatellite");
        getSupportTypes().add("HandheldDevice");
        getSupportTypes().add("LaptopDevice");

        setMaxFile(3);
        setMaxStore(80);
        setBandwidthSend(1);
        setBandwidthReceive(1);
    }
}
