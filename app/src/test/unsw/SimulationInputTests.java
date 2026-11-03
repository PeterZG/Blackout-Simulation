package unsw;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Malformed HTTP values must fail before any controller mutation. */
public class SimulationInputTests {
    @Test
    public void optionalMovingFlagAcceptsOnlyBooleanValues() {
        assertFalse(SimulationInput.movingDevice(null));
        assertFalse(SimulationInput.movingDevice("false"));
        assertTrue(SimulationInput.movingDevice(" TRUE "));
        for (String invalid : new String[] {"", " ", "yes", "1", "false-false"}) {
            assertThrows(IllegalArgumentException.class, () -> SimulationInput.movingDevice(invalid));
        }
    }

    @Test
    public void positionUsesRadiansAndRejectsNonFiniteOrOutOfRangeValues() {
        assertEquals(Math.PI, SimulationInput.position(" " + Math.PI + " ").toRadians());
        assertEquals(0, SimulationInput.position("0").toRadians());
        for (String invalid : new String[] {"", "NaN", "Infinity", "-0.1", "6.283185307179586", "east"}) {
            assertThrows(IllegalArgumentException.class, () -> SimulationInput.position(invalid));
        }
    }

    @Test
    public void orbitalRadiusBoundsHaveReadableErrors() {
        assertEquals(69912, SimulationInput.orbitalRadius("69912"));
        assertEquals(10000000, SimulationInput.orbitalRadius("10000000"));
        for (String invalid : new String[] {"69911", "10000001", "NaN", "Infinity", "orbit"}) {
            IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                    () -> SimulationInput.orbitalRadius(invalid));
            assertTrue(failure.getMessage().contains("Orbital radius"));
        }
    }

    @Test
    public void simulationMinutesAreWholeBoundedNumbers() {
        assertEquals(1, SimulationInput.minutes(null));
        assertEquals(120, SimulationInput.minutes("120"));
        for (String invalid : new String[] {"0", "121", "1.5", "", "2147483648", "later"}) {
            IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                    () -> SimulationInput.minutes(invalid));
            assertTrue(failure.getMessage().contains("whole minutes"));
        }
    }
}
