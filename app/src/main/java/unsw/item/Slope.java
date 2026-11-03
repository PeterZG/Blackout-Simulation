package unsw.item;

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

    public Slope(int startAngle, int endAngle, int gradient) {
        this.start = startAngle;
        this.end = endAngle;
        this.gradient = gradient;
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
