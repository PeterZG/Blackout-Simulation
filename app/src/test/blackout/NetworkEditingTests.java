package blackout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import unsw.blackout.BlackoutController;
import unsw.utils.Angle;
import unsw.utils.MathsHelper;

/** Editing must preserve identity, transfer invariants and session isolation. */
public class NetworkEditingTests {
    private BlackoutController connectedNetwork() {
        BlackoutController controller = new BlackoutController();
        controller.createDevice("ground", "HandheldDevice", Angle.fromDegrees(0));
        controller.createSatellite("orbiter", "StandardSatellite", 80000, Angle.fromDegrees(0));
        controller.addFileToDevice("ground", "telemetry", "Hello Jupiter");
        return controller;
    }

    @Test
    public void editDeviceFileUpdatesStoredDataAndRejectsDuplicates() {
        BlackoutController controller = connectedNetwork();
        assertThrows(IllegalArgumentException.class,
                () -> controller.addFileToDevice("ground", "telemetry", "duplicate"));
        controller.updateDeviceFile("ground", "telemetry", "Updated");
        assertEquals("Updated", controller.getEntityById("ground").getFile("telemetry").getContent());
        assertEquals(7, controller.getEntityById("ground").getFile("telemetry").getSize());
        assertThrows(IllegalArgumentException.class,
                () -> controller.addFileToDevice("missing", "telemetry", "payload"));
    }

    @Test
    public void emptyFilesCanBeCreatedAndEditedWithoutLosingTheirCompletedState() {
        BlackoutController controller = connectedNetwork();
        controller.addFileToDevice("ground", "empty.txt", "");
        assertEquals("", controller.getInfo("ground").getFiles().get("empty.txt").getData());
        controller.updateDeviceFile("ground", "telemetry", "");
        assertEquals(0, controller.getEntityById("ground").getFile("telemetry").getSize());
        assertTrue(controller.getEntityById("ground").getFile("telemetry").getCompleted());
    }

    @Test
    public void activeSourceCannotBeEditedAndDeletingItRemovesPartialCopies() throws Exception {
        BlackoutController controller = connectedNetwork();
        controller.sendFile("telemetry", "ground", "orbiter");
        controller.simulate();
        assertThrows(IllegalArgumentException.class,
                () -> controller.updateDeviceFile("ground", "telemetry", "changed"));
        controller.removeFile("ground", "telemetry");
        controller.simulate();
        assertTrue(controller.getInfo("ground").getFiles().isEmpty());
        assertTrue(controller.getInfo("orbiter").getFiles().isEmpty());
    }

    @Test
    public void deletingPartialDestinationCancelsWithoutDeletingSource() throws Exception {
        BlackoutController controller = connectedNetwork();
        controller.sendFile("telemetry", "ground", "orbiter");
        controller.removeFile("orbiter", "telemetry");
        controller.simulate();
        assertNull(controller.getEntityById("orbiter").getFile("telemetry"));
        assertEquals("Hello Jupiter", controller.getEntityById("ground").getFile("telemetry").getContent());
    }

    @Test
    public void repositionCancelsTransfersButKeepsCompletedFilesAndAngularSpeed() throws Exception {
        BlackoutController controller = connectedNetwork();
        controller.sendFile("telemetry", "ground", "orbiter");
        controller.updateEntity("orbiter", Angle.fromDegrees(45), 90000);
        assertEquals(45, controller.getInfo("orbiter").getPosition().toDegrees(), 0.001);
        assertTrue(controller.getInfo("orbiter").getFiles().isEmpty());
        assertEquals("Hello Jupiter", controller.getEntityById("ground").getFile("telemetry").getContent());
        assertEquals(controller.getEntityById("orbiter").getVelocity() / 90000,
                controller.getEntityById("orbiter").getAngularVelocity());
    }

    @Test
    public void invalidEditIsAtomic() throws Exception {
        BlackoutController controller = connectedNetwork();
        controller.sendFile("telemetry", "ground", "orbiter");
        assertThrows(IllegalArgumentException.class,
                () -> controller.updateEntity("orbiter", Angle.fromDegrees(45), Double.NaN));
        assertEquals(0, controller.getInfo("orbiter").getPosition().toDegrees());
        assertEquals(1, controller.getInfo("orbiter").getFiles().size());
        controller.simulate();
        assertEquals("H", controller.getEntityById("orbiter").getFile("telemetry").getContent());
    }

    @Test
    public void malformedTypesHeightsAndFilesAreRejected() {
        BlackoutController controller = new BlackoutController();
        assertThrows(IllegalArgumentException.class,
                () -> controller.createDevice("ground", "Unknown", Angle.fromDegrees(0)));
        assertThrows(IllegalArgumentException.class,
                () -> controller.createSatellite("orbiter", "StandardSatellite", 1, Angle.fromDegrees(0)));
        assertThrows(IllegalArgumentException.class,
                () -> controller.createSatellite("orbiter", "Unknown", 80000, Angle.fromDegrees(0)));
        controller.createDevice("ground", "LaptopDevice", Angle.fromDegrees(0));
        assertThrows(IllegalArgumentException.class, () -> controller.addFileToDevice("ground", " ", "data"));
        assertThrows(IllegalArgumentException.class,
                () -> controller.addFileToDevice("ground", "large", "x".repeat(100001)));
    }

    @Test
    public void slopesBelongToOneSimulationNotAllBrowserSessions() {
        BlackoutController first = new BlackoutController();
        BlackoutController second = new BlackoutController();
        first.createDevice("moving", "HandheldDevice", Angle.fromDegrees(0), true);
        second.createDevice("moving", "HandheldDevice", Angle.fromDegrees(0), true);
        first.createSlope(0, 90, 100);
        first.simulate();
        second.simulate();
        assertTrue(first.getInfo("moving").getHeight() > MathsHelper.RADIUS_OF_JUPITER);
        assertEquals(MathsHelper.RADIUS_OF_JUPITER, second.getInfo("moving").getHeight());
        assertThrows(IllegalArgumentException.class, () -> first.createSlope(90, 0, 1));
    }
}
