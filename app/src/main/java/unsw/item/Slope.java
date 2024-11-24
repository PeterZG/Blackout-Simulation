package unsw.item;

import java.util.List;
import java.util.ArrayList;

/**
 * slope class
 */
public class Slope {
    /** start */
    private double start;

    /** end */
    private double end;

    /** gradient */
    private double gradient;

    private static List<Slope> slopes = new ArrayList<>();

    public Slope(int startAngle, int endAngle, int gradient) {
        this.start = startAngle;
        this.end = endAngle;
        this.gradient = gradient;
    }

    public static void createSlope(int startAngle, int endAngle, int gradient) {
        slopes.add(new Slope(startAngle, endAngle, gradient));
    }

    public static List<Slope> getSlopes() {
        return slopes;
    }

    public static Slope inSlope(double angle) {
        for (Slope slope : slopes) {
            if (angle >= slope.start && angle <= slope.end) {
                return slope;
            }
        }
        return null;
    }

    /** return start */
    public double getStart() {
        return start;
    }

    /** set start */
    public void setStart(double start) {
        this.start = start;
    }

    /** return end */
    public double getEnd() {
        return end;
    }

    /** set end */
    public void setEnd(double end) {
        this.end = end;
    }

    /** return gradient */
    public double getGradient() {
        return gradient;
    }

    /** set gradient */
    public void setGradient(double gradient) {
        this.gradient = gradient;
    }

}
