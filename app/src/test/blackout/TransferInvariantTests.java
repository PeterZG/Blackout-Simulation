package blackout;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static unsw.utils.MathsHelper.RADIUS_OF_JUPITER;

import org.junit.jupiter.api.Test;

import unsw.blackout.BlackoutController;
import unsw.blackout.FileTransferException;
import unsw.blackout.FileTransferException.VirtualFileNoStorageSpaceException;
import unsw.response.models.FileInfoResponse;
import unsw.utils.Angle;

public class TransferInvariantTests {
    @Test
    public void invalidEntitiesAreReportedAsTransferErrors() {
        BlackoutController controller = new BlackoutController();

        assertThrows(FileTransferException.class,
                () -> controller.sendFile("missing", "unknown", "also-unknown"));
    }

    @Test
    public void destinationCapacityIncludesTheIncomingFile() {
        BlackoutController controller = new BlackoutController();
        controller.createDevice("device", "HandheldDevice", Angle.fromDegrees(0));
        controller.createSatellite("satellite", "StandardSatellite",
                RADIUS_OF_JUPITER + 1000, Angle.fromDegrees(0));
        controller.addFileToDevice("device", "large", "x".repeat(81));

        assertThrows(VirtualFileNoStorageSpaceException.class,
                () -> controller.sendFile("large", "device", "satellite"));
    }

    @Test
    public void removingAnEndpointCancelsItsPartialTransfer() throws FileTransferException {
        BlackoutController controller = new BlackoutController();
        controller.createDevice("device", "HandheldDevice", Angle.fromDegrees(0));
        controller.createSatellite("satellite", "StandardSatellite",
                RADIUS_OF_JUPITER + 1000, Angle.fromDegrees(0));
        controller.addFileToDevice("device", "message", "partial");
        controller.sendFile("message", "device", "satellite");

        controller.removeDevice("device");

        assertFalse(controller.getInfo("satellite").getFiles().containsKey("message"));
    }

    @Test
    public void entityIdentifiersAreUniqueAcrossEntityTypes() {
        BlackoutController controller = new BlackoutController();
        controller.createDevice("duplicate", "HandheldDevice", Angle.fromDegrees(0));

        assertThrows(IllegalArgumentException.class,
                () -> controller.createSatellite("duplicate", "StandardSatellite",
                        RADIUS_OF_JUPITER + 1000, Angle.fromDegrees(0)));
    }

    @Test
    public void satelliteTransferProgressesOnceUsingTheSlowerEndpoint() throws FileTransferException {
        BlackoutController controller = new BlackoutController();
        controller.createDevice("device", "HandheldDevice", Angle.fromDegrees(0));
        controller.createSatellite("source", "TeleportingSatellite",
                RADIUS_OF_JUPITER + 10000, Angle.fromDegrees(0));
        controller.createSatellite("destination", "TeleportingSatellite",
                RADIUS_OF_JUPITER + 10000, Angle.fromDegrees(0));
        controller.addFileToDevice("device", "message", "x".repeat(30));
        controller.sendFile("message", "device", "source");
        controller.simulate(2);

        controller.sendFile("message", "source", "destination");
        controller.simulate();

        assertEquals(new FileInfoResponse("message", "x".repeat(10), 30, false),
                controller.getInfo("destination").getFiles().get("message"));
    }

    @Test
    public void orbitalMovementCancelsAnInterruptedTransfer() throws FileTransferException {
        BlackoutController controller = new BlackoutController();
        controller.createDevice("device", "HandheldDevice", Angle.fromDegrees(0));
        controller.createSatellite("satellite", "StandardSatellite",
                RADIUS_OF_JUPITER + 1000, Angle.fromDegrees(352));
        controller.addFileToDevice("device", "message", "partial");
        controller.sendFile("message", "device", "satellite");

        controller.simulate();

        assertFalse(controller.getInfo("satellite").getFiles().containsKey("message"));
    }

    @Test
    public void satelliteVisibilityDoesNotBypassTransmissionRange() {
        BlackoutController controller = new BlackoutController();
        controller.createSatellite("source", "StandardSatellite",
                RADIUS_OF_JUPITER + 1000000, Angle.fromDegrees(0));
        controller.createSatellite("destination", "StandardSatellite",
                RADIUS_OF_JUPITER + 1000000, Angle.fromDegrees(10));

        assertFalse(controller.communicableEntitiesInRange("source").contains("destination"));
    }
}
