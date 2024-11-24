package unsw.blackout;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import unsw.blackout.FileTransferException.VirtualFileAlreadyExistsException;
import unsw.blackout.FileTransferException.VirtualFileNotFoundException;
import unsw.item.Device;
import unsw.item.Entity;
import unsw.item.FileItem;
import unsw.item.RelaySatellite;
import unsw.item.Satellite;
import unsw.item.Slope;
import unsw.response.models.EntityInfoResponse;
import unsw.utils.Angle;
import unsw.utils.MathsHelper;

/**
 * The controller for the Blackout system.
 *
 * WARNING: Do not move this file or modify any of the existing method
 * signatures
 */
public class BlackoutController {
    private List<Device> deviceList = new ArrayList<>();
    private List<Satellite> satelliteList = new ArrayList<>();
    private List<Transfer> transferList = new ArrayList<>();

    public void createDevice(String deviceId, String type, Angle position) {
        // TODO: Task 1a)
        deviceList.add(Device.createDevice(deviceId, type, position));
    }

    public void removeDevice(String deviceId) {
        // TODO: Task 1b)
        deviceList.removeIf(device -> device.getId() != null && device.getId().equals(deviceId));
    }

    public void createSatellite(String satelliteId, String type, double height, Angle position) {
        // TODO: Task 1c)
        satelliteList.add(Satellite.createSatellite(satelliteId, type, height, position));
    }

    public void removeSatellite(String satelliteId) {
        // TODO: Task 1d)
        satelliteList.removeIf(s -> s.getId() != null && s.getId().equals(satelliteId));
    }

    public List<String> listDeviceIds() {
        // TODO: Task 1e)
        return deviceList.stream().map(e -> e.getId()).collect(Collectors.toList());
    }

    public List<String> listSatelliteIds() {
        // TODO: Task 1f)
        return satelliteList.stream().map(e -> e.getId()).collect(Collectors.toList());
    }

    public void addFileToDevice(String deviceId, String filename, String content) {
        // TODO: Task 1g)
        deviceList.stream().filter(e -> e.getId().equals(deviceId)).forEach(e -> e.addFile(filename, content));
    }

    public EntityInfoResponse getInfo(String id) {
        // TODO: Task 1h)
        Entity entity = getEntityById(id);
        return entity == null ? null : entity.toInfoResponse();
    }

    public void simulate() {
        // TODO: Task 2a)
        // System.out.println("simulate ...2");
        satelliteList.forEach(s -> s.move());
        deviceList.forEach(s -> s.move());

        List<Transfer> completed = new ArrayList<>();

        for (Satellite s : satelliteList) {
            for (Transfer t : transferList) {
                if (t.isFrom(s.getId())) {
                    int transferBandwidth = s.getBandwidthSend() / getSendCount(s.getId());
                    transferFile(completed, t, transferBandwidth);
                }

                if (t.isTo(s.getId())) {
                    int transferBandwidth = s.getBandwidthSend() / getReceiveCount(s.getId());
                    transferFile(completed, t, transferBandwidth);
                }
            }
        }
        // remove completed transfer
        transferList.removeAll(completed);

        List<Transfer> outRange = new ArrayList<>();
        transferList.stream().filter(e -> !inRange(e.getFrom(), e.getTo())).forEach(e -> {
            if (!e.getDest().getCompleted()) {
                e.getTo().removeFile(e.getDest().getName());
            }
            outRange.add(e);
        });
        transferList.removeAll(outRange);
    }

    /**
     * transfer file
     * @param completed
     * @param t
     * @param transferBandwidth
     */
    private void transferFile(List<Transfer> completed, Transfer t, int transferBandwidth) {
        int size = t.getSrc().getSize();
        FileItem dest = t.getDest();

        int transfer = dest.getTransfered() + transferBandwidth;
        if (transfer >= size) {
            transfer = size;
        }
        dest.setTransfered(transfer);
        dest.setContent(t.getSrc().getContent().substring(0, transfer));
        if (dest.getTransfered() >= size) {
            dest.setCompleted(true);
            completed.add(t);
        }
    }

    /**
     * Simulate for the specified number of minutes. You shouldn't need to modify
     * this function.
     */
    public void simulate(int numberOfMinutes) {
        for (int i = 0; i < numberOfMinutes; i++) {
            simulate();
        }
    }

    public List<String> communicableEntitiesInRange(String id) {
        // TODO: Task 2 b)
        List<String> list = new ArrayList<>();
        if (id == null) {
            return list;
        }
        Optional<Device> device = deviceList.stream().filter(e -> e.getId().equals(id)).findFirst();
        if (device.isPresent()) {
            Device src = device.get();
            satelliteList.stream().filter(e -> e.canSupport(src.getType()) && inRange(src, e) && isVisible(e, src))
                    .forEach(e -> {
                        addIfNotExist(list, e.getId());

                        if (e instanceof RelaySatellite) {
                            satelliteList.stream().filter(e1 -> e1.canSupport(src.getType()) && isVisible(e1, e))
                                    .forEach(e1 -> {
                                        addIfNotExist(list, e1.getId());
                                    });
                        }
                    });
        } else {
            Optional<Satellite> sate = satelliteList.stream().filter(e -> e.getId().equals(id)).findFirst();
            sate.ifPresent(s -> {
                satelliteList.stream().filter(e -> !id.equals(e.getId()) && isVisible(s, e)).forEach(e -> {
                    addIfNotExist(list, e.getId());
                    if (e instanceof RelaySatellite) {
                        deviceList.stream().filter(d -> inRange(e, d) && isVisible(e, d) && s.canSupport(d.getType()))
                                .forEach(d -> addIfNotExist(list, d.getId()));
                    }
                });
                deviceList.stream().filter(e -> s.canSupport(e.getType()) && inRange(s, e) && isVisible(s, e))
                        .forEach(e -> addIfNotExist(list, e.getId()));
            });
        }
        return list;
    }

    public void sendFile(String fileName, String fromId, String toId) throws FileTransferException {
        // TODO: Task 2 c)
        if (fileName == null || fromId == null || toId == null) {
            throw new FileTransferException("Empty input");
        }
        Entity from = getEntityById(fromId);
        FileItem file = from.getCompleteFile(fileName);
        if (file == null) {
            throw new VirtualFileNotFoundException(fileName);
        }
        Entity to = getEntityById(toId);
        if (to.getFile(fileName) != null) {
            throw new VirtualFileAlreadyExistsException(fileName);
        }

        to.receiveFile(fileName, file.getContent());

        FileItem dest = new FileItem(fileName, "");
        dest.setSize(file.getSize());
        dest.setCompleted(false);

        transferList.add(new Transfer(from, to, file, dest));

        to.getFiles().add(dest);
    }

    public void createDevice(String deviceId, String type, Angle position, boolean isMoving) {
        // createDevice(deviceId, type, position);
        // TODO: Task 3
        deviceList.add(Device.createDevice(deviceId, type, position, isMoving));
    }

    public void createSlope(int startAngle, int endAngle, int gradient) {
        // TODO: Task 3
        // If you are not completing Task 3 you can leave this method blank :)
        Slope.createSlope(startAngle, endAngle, gradient);
    }

    public Entity getEntityById(String id) {
        Optional<Device> device = deviceList.stream().filter(e -> e.getId().equals(id)).findFirst();
        if (device.isPresent()) {
            return device.get();
        }
        Optional<Satellite> sate = satelliteList.stream().filter(e -> e.getId().equals(id)).findFirst();
        if (sate.isPresent()) {
            return sate.get();
        }
        return null;
    }

    private int getSendCount(String id) {
        return (int) transferList.stream().filter(e -> e.getFrom().getId().equals(id)).count();
    }

    private int getReceiveCount(String id) {
        return (int) transferList.stream().filter(e -> e.getTo().getId().equals(id)).count();
    }

    private boolean isVisible(Entity e1, Entity e2) {
        return MathsHelper.isVisible(e1.getHeight(), e1.getPosition(), e2.getHeight(), e2.getPosition());
    }

    private double getDistance(Entity e1, Entity e2) {
        return MathsHelper.getDistance(e1.getHeight(), e1.getPosition(), e2.getHeight(), e2.getPosition());
    }

    private void addIfNotExist(List<String> list, String id) {
        if (!list.contains(id)) {
            list.add(id);
        }
    }

    private boolean inRange(Entity e1, Entity e2) {
        return getDistance(e1, e2) <= e1.getRange();
    }
}
