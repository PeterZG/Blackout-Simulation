package blackout;

import static blackout.TestHelpers.assertListAreEqualIgnoringOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static unsw.utils.MathsHelper.RADIUS_OF_JUPITER;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import unsw.blackout.BlackoutController;
import unsw.response.models.EntityInfoResponse;
import unsw.response.models.FileInfoResponse;
import unsw.utils.Angle;

public class MyTests {
    // TODO: Add your own tests here

    @Test
    public void testDelete() {
        // Task 1
        BlackoutController controller = new BlackoutController();

        // Creates 1 satellite and 3 devices and deletes them
        controller.createSatellite("S1", "StandardSatellite", 100 + RADIUS_OF_JUPITER, Angle.fromDegrees(340));
        controller.createDevice("D1", "HandheldDevice", Angle.fromDegrees(30));
        controller.createDevice("D2", "LaptopDevice", Angle.fromDegrees(180));
        controller.createDevice("D3", "DesktopDevice", Angle.fromDegrees(330));

        assertListAreEqualIgnoringOrder(Arrays.asList("S1"), controller.listSatelliteIds());

        controller.removeDevice("D1");
        assertListAreEqualIgnoringOrder(Arrays.asList("D2", "D3"), controller.listDeviceIds());
        controller.removeDevice("D2");
        assertListAreEqualIgnoringOrder(Arrays.asList("D3"), controller.listDeviceIds());
        controller.removeDevice("D3");
        controller.removeSatellite("S1");
        assertListAreEqualIgnoringOrder(new ArrayList<String>(), controller.listSatelliteIds());
        assertListAreEqualIgnoringOrder(new ArrayList<String>(), controller.listDeviceIds());
    }

    @Test
    public void testAddFile() {
        BlackoutController controller = new BlackoutController();
        controller.createSatellite("S1", "RelaySatellite", 100 + RADIUS_OF_JUPITER, Angle.fromDegrees(100));
        controller.createSatellite("S2", "StandardSatellite", 100 + RADIUS_OF_JUPITER, Angle.fromDegrees(100));
        controller.createSatellite("S3", "TeleportingSatellite", 100 + RADIUS_OF_JUPITER, Angle.fromDegrees(100));
        controller.createDevice("DeviceA", "DesktopDevice", Angle.fromDegrees(100));

        // test DesktopDevice addFile
        controller.addFileToDevice("DeviceA", "File001", "Hello World!");
        Map<String, FileInfoResponse> expected = new HashMap<>();
        expected.put("File001", new FileInfoResponse("File001", "Hello World!", "Hello World!".length(), true));

        assertEquals(
                new EntityInfoResponse("DeviceA", Angle.fromDegrees(100), RADIUS_OF_JUPITER, "DesktopDevice", expected),
                controller.getInfo("DeviceA"));

        controller.addFileToDevice("DeviceA", "File002", "Hello World!");
        expected.put("File002", new FileInfoResponse("File002", "Hello World!", "Hello World!".length(), true));
        assertEquals(
                new EntityInfoResponse("DeviceA", Angle.fromDegrees(100), RADIUS_OF_JUPITER, "DesktopDevice", expected),
                controller.getInfo("DeviceA"));

    }


    @Test
    public void testListEntityInRange() {
        BlackoutController controller = new BlackoutController();
        controller.createSatellite("S1", "StandardSatellite", 10000 + RADIUS_OF_JUPITER, Angle.fromDegrees(100));
        controller.createSatellite("S2", "TeleportingSatellite", 10000 + RADIUS_OF_JUPITER, Angle.fromDegrees(100));
        controller.createSatellite("S3", "RelaySatellite", 10000 + RADIUS_OF_JUPITER, Angle.fromDegrees(100));
        controller.createDevice("D1", "HandheldDevice", Angle.fromDegrees(100));
        controller.createDevice("D2", "LaptopDevice", Angle.fromDegrees(100));
        controller.createDevice("D3", "DesktopDevice", Angle.fromDegrees(100));

        assertListAreEqualIgnoringOrder(Arrays.asList("S1", "S2", "S3"), controller.communicableEntitiesInRange("D1"));
        assertListAreEqualIgnoringOrder(Arrays.asList("S1", "S2", "S3"), controller.communicableEntitiesInRange("D2"));
        assertListAreEqualIgnoringOrder(Arrays.asList("S2", "S3"), controller.communicableEntitiesInRange("D3"));
    }

    @Test
    public void testListEntityInRange2() {
        BlackoutController controller = new BlackoutController();
        controller.createSatellite("S1", "StandardSatellite", 200000 + RADIUS_OF_JUPITER, Angle.fromDegrees(100));
        controller.createSatellite("S2", "TeleportingSatellite", 200000 + RADIUS_OF_JUPITER, Angle.fromDegrees(100));
        controller.createSatellite("S3", "RelaySatellite", 200000 + RADIUS_OF_JUPITER, Angle.fromDegrees(100));
        controller.createDevice("D1", "HandheldDevice", Angle.fromDegrees(100));
        controller.createDevice("D2", "LaptopDevice", Angle.fromDegrees(100));
        controller.createDevice("D3", "DesktopDevice", Angle.fromDegrees(100));

        assertListAreEqualIgnoringOrder(new ArrayList<String>(), controller.communicableEntitiesInRange("D1"));
        assertListAreEqualIgnoringOrder(new ArrayList<String>(), controller.communicableEntitiesInRange("D2"));
        assertListAreEqualIgnoringOrder(Arrays.asList("S2", "S3"), controller.communicableEntitiesInRange("D3"));
    }

    @Test
    public void testListEntityInRange3() {
        BlackoutController controller = new BlackoutController();
        controller.createSatellite("S1", "StandardSatellite", 150000 + RADIUS_OF_JUPITER, Angle.fromDegrees(100));
        controller.createSatellite("S2", "TeleportingSatellite", 150000 + RADIUS_OF_JUPITER, Angle.fromDegrees(100));
        controller.createSatellite("S3", "RelaySatellite", 150000 + RADIUS_OF_JUPITER, Angle.fromDegrees(100));
        controller.createDevice("D1", "HandheldDevice", Angle.fromDegrees(100));
        controller.createDevice("D2", "LaptopDevice", Angle.fromDegrees(100));
        controller.createDevice("D3", "DesktopDevice", Angle.fromDegrees(100));

        assertListAreEqualIgnoringOrder(Arrays.asList("D1", "D2", "S2", "S3"),
                controller.communicableEntitiesInRange("S1"));
        assertListAreEqualIgnoringOrder(Arrays.asList("D1", "D2", "D3", "S1", "S3"),
                controller.communicableEntitiesInRange("S2"));
        assertListAreEqualIgnoringOrder(Arrays.asList("D1", "D2", "D3", "S2", "S1"),
                controller.communicableEntitiesInRange("S3"));
    }

    @Test
    public void testListEntityInRange4() {
        BlackoutController controller = new BlackoutController();
        controller.createSatellite("S1", "StandardSatellite", 200000 + RADIUS_OF_JUPITER, Angle.fromDegrees(100));
        controller.createSatellite("S2", "TeleportingSatellite", 200000 + RADIUS_OF_JUPITER, Angle.fromDegrees(100));
        controller.createSatellite("S3", "RelaySatellite", 200000 + RADIUS_OF_JUPITER, Angle.fromDegrees(100));
        controller.createDevice("D1", "HandheldDevice", Angle.fromDegrees(100));
        controller.createDevice("D2", "LaptopDevice", Angle.fromDegrees(100));
        controller.createDevice("D3", "DesktopDevice", Angle.fromDegrees(100));

        assertListAreEqualIgnoringOrder(Arrays.asList("D1", "D2", "S2", "S3"),
                controller.communicableEntitiesInRange("S1"));
        assertListAreEqualIgnoringOrder(Arrays.asList("D1", "D2", "D3", "S1", "S3"),
                controller.communicableEntitiesInRange("S2"));
        assertListAreEqualIgnoringOrder(Arrays.asList("D1", "D2", "D3", "S2", "S1"),
                controller.communicableEntitiesInRange("S3"));
    }

    @Test
    public void testListEntityInRange5() {
        BlackoutController controller = new BlackoutController();
        controller.createSatellite("S1", "StandardSatellite", 300001 + RADIUS_OF_JUPITER, Angle.fromDegrees(100));
        controller.createSatellite("S2", "TeleportingSatellite", 300001 + RADIUS_OF_JUPITER, Angle.fromDegrees(100));
        controller.createSatellite("S3", "RelaySatellite", 300001 + RADIUS_OF_JUPITER, Angle.fromDegrees(100));
        controller.createDevice("D1", "HandheldDevice", Angle.fromDegrees(100));
        controller.createDevice("D2", "LaptopDevice", Angle.fromDegrees(100));
        controller.createDevice("D3", "DesktopDevice", Angle.fromDegrees(100));

        assertListAreEqualIgnoringOrder(Arrays.asList("S2", "S3"), controller.communicableEntitiesInRange("S1"));
        assertListAreEqualIgnoringOrder(Arrays.asList("S1", "S3"), controller.communicableEntitiesInRange("S2"));
        assertListAreEqualIgnoringOrder(Arrays.asList("S2", "S1"), controller.communicableEntitiesInRange("S3"));
    }

    private EntityInfoResponse info(String id, double degree, double height, String type) {
        return new EntityInfoResponse(id, Angle.fromDegrees(degree), height + RADIUS_OF_JUPITER, type);
    }

    @Test
    public void testSimulation() {
        BlackoutController controller = new BlackoutController();
        controller.createSatellite("S1", "StandardSatellite", 1000 + RADIUS_OF_JUPITER, Angle.fromDegrees(0));
        controller.simulate();
        assertEquals(info("S1", 357.98, 1000, "StandardSatellite"), controller.getInfo("S1"));
        controller.simulate(25);
        assertEquals(info("S1", 307.48, 1000, "StandardSatellite"), controller.getInfo("S1"));
        controller.simulate(120);
        assertEquals(info("S1", 65.08, 1000, "StandardSatellite"), controller.getInfo("S1"));
    }

    @Test
    public void testSimulation2() {
        BlackoutController controller = new BlackoutController();
        controller.createSatellite("S1", "TeleportingSatellite", 1000 + RADIUS_OF_JUPITER, Angle.fromDegrees(0));
        controller.simulate();
        assertEquals(
                new EntityInfoResponse("S1", Angle.fromDegrees(0.81), 1000 + RADIUS_OF_JUPITER, "TeleportingSatellite"),
                controller.getInfo("S1"));
        controller.simulate(24);
        assertEquals(info("S1", 20.2, 1000, "TeleportingSatellite"), controller.getInfo("S1"));
        controller.simulate(25);
        assertEquals(info("S1", 40.4, 1000, "TeleportingSatellite"), controller.getInfo("S1"));
        controller.simulate(200);
        assertEquals(new EntityInfoResponse("S1", Angle.fromDegrees(21.81), 1000 + RADIUS_OF_JUPITER,
                "TeleportingSatellite"), controller.getInfo("S1"));
    }

    @Test
    public void testSimulation3() {
        BlackoutController controller = new BlackoutController();
        controller.createSatellite("S1", "RelaySatellite", 1000 + RADIUS_OF_JUPITER, Angle.fromDegrees(150));
        controller.simulate();
        assertEquals(info("S1", 148.79, 1000, "RelaySatellite"), controller.getInfo("S1"));
        controller.simulate(20);
        assertEquals(info("S1", 153.64, 1000, "RelaySatellite"), controller.getInfo("S1"));
        controller.simulate(20);
        assertEquals(info("S1", 177.88, 1000, "RelaySatellite"), controller.getInfo("S1"));
        controller.simulate(20);
        assertEquals(info("S1", 180.30, 1000, "RelaySatellite"), controller.getInfo("S1"));
        controller.simulate(20);
        assertEquals(info("S1", 156.06, 1000, "RelaySatellite"), controller.getInfo("S1"));
    }
}
