package unsw;

import unsw.utils.Angle;
import unsw.utils.MathsHelper;

/** Parses HTTP values without changing the simulation's underlying unit conventions. */
final class SimulationInput {
    private static final double MAX_ORBITAL_RADIUS_KM = 10000000;
    private static final int MAX_MINUTES_PER_REQUEST = 120;

    private SimulationInput() {
    }

    static boolean movingDevice(String value) {
        if (value == null) return false;
        if ("true".equalsIgnoreCase(value.trim())) return true;
        if ("false".equalsIgnoreCase(value.trim())) return false;
        throw new IllegalArgumentException("Moving device must be true or false");
    }

    static Angle position(String value) {
        String message = "Position must be a number between 0° and 360° (exclusive)";
        double radians = finiteNumber(value, message);
        if (radians < 0 || radians >= Math.PI * 2) throw new IllegalArgumentException(message);
        return Angle.fromRadians(radians);
    }

    static double orbitalRadius(String value) {
        String message = "Orbital radius must be above 69,911 and at most 10,000,000 km";
        double radius = finiteNumber(value, message);
        if (radius <= MathsHelper.RADIUS_OF_JUPITER || radius > MAX_ORBITAL_RADIUS_KM) {
            throw new IllegalArgumentException(message);
        }
        return radius;
    }

    static int minutes(String value) {
        String message = "Advance between 1 and 120 whole minutes per request";
        try {
            int minutes = value == null ? 1 : Integer.parseInt(value.trim());
            if (minutes < 1 || minutes > MAX_MINUTES_PER_REQUEST) throw new IllegalArgumentException(message);
            return minutes;
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException(message);
        }
    }

    private static double finiteNumber(String value, String message) {
        try {
            double number = Double.parseDouble(value == null ? "" : value.trim());
            if (!Double.isFinite(number)) throw new IllegalArgumentException(message);
            return number;
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException(message);
        }
    }
}
