package unsw.blackout;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import unsw.blackout.FileTransferException.VirtualFileAlreadyExistsException;
import unsw.blackout.FileTransferException.VirtualFileNoBandwidthException;
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
    private final List<Device> deviceList = new ArrayList<>();
    private final List<Satellite> satelliteList = new ArrayList<>();
    private final List<Transfer> transferList = new ArrayList<>();
    private final List<Slope> slopes = new ArrayList<>();

    public void createDevice(String deviceId, String type, Angle position) {
        createDevice(deviceId, type, position, false);
    }

    public void removeDevice(String deviceId) {
        cancelTransfersFor(deviceId);
        deviceList.removeIf(device -> device.getId() != null && device.getId().equals(deviceId));
    }

    public void createSatellite(String satelliteId, String type, double height, Angle position) {
        ensureUniqueId(satelliteId);
        validatePosition(position);
        validateHeight(height);
        if (!List.of("StandardSatellite", "RelaySatellite", "TeleportingSatellite").contains(type)) {
            throw new IllegalArgumentException("Choose a supported satellite type");
        }
        satelliteList.add(Satellite.createSatellite(satelliteId, type, height, position));
    }

    public void removeSatellite(String satelliteId) {
        cancelTransfersFor(satelliteId);
        satelliteList.removeIf(s -> s.getId() != null && s.getId().equals(satelliteId));
    }

    public List<String> listDeviceIds() {
        return deviceList.stream().map(e -> e.getId()).collect(Collectors.toList());
    }

    public List<String> listSatelliteIds() {
        return satelliteList.stream().map(e -> e.getId()).collect(Collectors.toList());
    }

    public void addFileToDevice(String deviceId, String filename, String content) {
        Entity device = requireEntity(deviceId);
        validateFile(filename, content);
        if (!(device instanceof Device)) {
            throw new IllegalArgumentException("Files can only be added directly to a device");
        }
        if (device.getFile(filename) != null) {
            throw new IllegalArgumentException("This file already exists. Edit it or choose a different name");
        }
        device.addFile(filename, content);
    }

    public EntityInfoResponse getInfo(String id) {
        Entity entity = getEntityById(id);
        return entity == null ? null : entity.toInfoResponse();
    }

    public void simulate() {
        satelliteList.forEach(s -> s.move());
        deviceList.forEach(device -> device.move(slopes));

        List<Transfer> completed = new ArrayList<>();
        List<Transfer> interrupted = new ArrayList<>();

        for (Transfer transfer : transferList) {
            if (!canCommunicate(transfer.getFrom(), transfer.getTo())) {
                transfer.getTo().removeFile(transfer.getDest().getName());
                interrupted.add(transfer);
                continue;
            }

            int bandwidth = Integer.MAX_VALUE;
            if (transfer.getFrom() instanceof Satellite) {
                Satellite source = (Satellite) transfer.getFrom();
                bandwidth = Math.min(bandwidth,
                        source.getBandwidthSend() / getSendCount(source.getId()));
            }
            if (transfer.getTo() instanceof Satellite) {
                Satellite destination = (Satellite) transfer.getTo();
                bandwidth = Math.min(bandwidth,
                        destination.getBandwidthReceive() / getReceiveCount(destination.getId()));
            }
            if (bandwidth > 0) {
                advanceFileTransfer(completed, transfer, bandwidth);
            }
        }

        transferList.removeAll(completed);
        transferList.removeAll(interrupted);
    }

    /** Copy one minute's character allowance and defer removal until iteration finishes. */
    private void advanceFileTransfer(List<Transfer> completed, Transfer transfer, int charactersPerMinute) {
        int fileSize = transfer.getSrc().getSize();
        FileItem destination = transfer.getDest();
        int receivedCharacters = Math.min(fileSize, destination.getTransfered() + charactersPerMinute);
        destination.setTransfered(receivedCharacters);
        destination.setContent(transfer.getSrc().getContent().substring(0, receivedCharacters));
        if (receivedCharacters >= fileSize) {
            destination.setCompleted(true);
            completed.add(transfer);
        }
    }

    /** Advance the model by the requested number of whole simulation minutes. */
    public void simulate(int numberOfMinutes) {
        for (int i = 0; i < numberOfMinutes; i++) {
            simulate();
        }
    }

    public List<String> communicableEntitiesInRange(String id) {
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
                            satelliteList.stream().filter(e1 -> e1.canSupport(src.getType())
                                    && inRange(e, e1) && isVisible(e1, e))
                                    .forEach(e1 -> {
                                        addIfNotExist(list, e1.getId());
                                    });
                        }
                    });
        } else {
            Optional<Satellite> sate = satelliteList.stream().filter(e -> e.getId().equals(id)).findFirst();
            sate.ifPresent(s -> {
                satelliteList.stream().filter(e -> !id.equals(e.getId()) && inRange(s, e) && isVisible(s, e))
                        .forEach(e -> {
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
        if (fileName == null || fromId == null || toId == null) {
            throw new FileTransferException("Empty input");
        }
        Entity from = getEntityById(fromId);
        Entity to = getEntityById(toId);
        if (from == null || to == null) {
            throw new FileTransferException("Unknown source or destination entity");
        }
        if (from == to) {
            throw new FileTransferException("Source and destination must be different entities");
        }
        if (!canCommunicate(from, to)) {
            throw new FileTransferException("Entities are not currently communicable");
        }
        FileItem file = from.getCompleteFile(fileName);
        if (file == null) {
            throw new VirtualFileNotFoundException(fileName);
        }
        if (to.getFile(fileName) != null) {
            throw new VirtualFileAlreadyExistsException(fileName);
        }
        if (from instanceof Satellite
                && getSendCount(fromId) >= ((Satellite) from).getBandwidthSend()) {
            throw new VirtualFileNoBandwidthException(fromId);
        }

        to.receiveFile(fileName, file.getContent());

        FileItem dest = new FileItem(fileName, "");
        dest.setSize(file.getSize());
        dest.setCompleted(false);

        transferList.add(new Transfer(from, to, file, dest));

        to.getFiles().add(dest);
    }

    public void createDevice(String deviceId, String type, Angle position, boolean isMoving) {
        ensureUniqueId(deviceId);
        validatePosition(position);
        if (!List.of("HandheldDevice", "LaptopDevice", "DesktopDevice").contains(type)) {
            throw new IllegalArgumentException("Choose a supported device type");
        }
        deviceList.add(Device.createDevice(deviceId, type, position, isMoving));
    }

    public void createSlope(int startAngle, int endAngle, int gradient) {
        if (startAngle < 0 || endAngle > 359 || startAngle >= endAngle) {
            throw new IllegalArgumentException("Slope angles must satisfy 0 ≤ start < end ≤ 359");
        }
        slopes.add(new Slope(startAngle, endAngle, gradient));
    }

    /** Reposition without replacing an entity or losing its completed files. */
    public void updateEntity(String id, Angle position, double height) {
        Entity entity = requireEntity(id);
        validatePosition(position);
        if (entity instanceof Satellite) validateHeight(height);
        cancelTransfersFor(id);
        entity.setPosition(position);
        if (entity instanceof Satellite) {
            entity.setHeight(height);
            entity.setAngularVelocity(entity.getVelocity() / height);
        }
    }

    public void updateDeviceFile(String deviceId, String filename, String content) {
        Entity device = requireEntity(deviceId);
        validateFile(filename, content);
        if (!(device instanceof Device) || device.getCompleteFile(filename) == null) {
            throw new IllegalArgumentException("Only a completed device file can be edited");
        }
        if (transferList.stream().anyMatch(transfer -> transfer.isFrom(deviceId)
                && transfer.getSrc().getName().equals(filename))) {
            throw new IllegalArgumentException("Wait for this file's active transfers before editing");
        }
        FileItem file = device.getFile(filename);
        file.setContent(content);
        file.setSize(content.length());
        file.setTransfered(content.length());
    }

    /** Cancels this file's transfers and removes unfinished destination copies. */
    public void removeFile(String entityId, String filename) {
        Entity entity = requireEntity(entityId);
        if (entity.getFile(filename) == null) throw new IllegalArgumentException("File not found");
        List<Transfer> cancelled = transferList.stream().filter(transfer ->
                (transfer.isFrom(entityId) && transfer.getSrc().getName().equals(filename))
                || (transfer.isTo(entityId) && transfer.getDest().getName().equals(filename)))
                .collect(Collectors.toList());
        cancelled.forEach(transfer -> transfer.getTo().removeFile(transfer.getDest().getName()));
        transferList.removeAll(cancelled);
        entity.removeFile(filename);
    }

    private Entity requireEntity(String id) {
        Entity entity = getEntityById(id);
        if (entity == null) throw new IllegalArgumentException("Entity not found");
        return entity;
    }

    private void validatePosition(Angle position) {
        if (position == null || !Double.isFinite(position.toRadians())) {
            throw new IllegalArgumentException("Position must be a finite angle");
        }
    }

    private void validateHeight(double height) {
        if (!Double.isFinite(height) || height <= MathsHelper.RADIUS_OF_JUPITER) {
            throw new IllegalArgumentException("Orbital radius must be above Jupiter's surface");
        }
    }

    private void validateFile(String filename, String content) {
        if (filename == null || filename.isBlank() || filename.length() > 80) {
            throw new IllegalArgumentException("File name must contain 1–80 characters");
        }
        if (content == null || content.length() > 100000) {
            throw new IllegalArgumentException("File content must contain at most 100,000 characters");
        }
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

    private boolean canCommunicate(Entity from, Entity to) {
        return communicableEntitiesInRange(from.getId()).contains(to.getId());
    }

    private void ensureUniqueId(String id) {
        if (id == null || id.isBlank() || id.length() > 80) {
            throw new IllegalArgumentException("Entity ID must contain 1–80 characters");
        }
        if (getEntityById(id) != null) {
            throw new IllegalArgumentException("Entity ID already exists: " + id);
        }
    }

    private void cancelTransfersFor(String entityId) {
        List<Transfer> cancelled = transferList.stream()
                .filter(transfer -> transfer.isFrom(entityId) || transfer.isTo(entityId))
                .collect(Collectors.toList());
        cancelled.forEach(transfer -> transfer.getTo().removeFile(transfer.getDest().getName()));
        transferList.removeAll(cancelled);
    }
}
