package unsw.item;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import unsw.blackout.FileTransferException;
import unsw.response.models.EntityInfoResponse;
import unsw.response.models.FileInfoResponse;
import unsw.utils.Angle;
import unsw.utils.MathsHelper;

/**
 * Represents the base class for entities within the system.
 */
public class Entity {
    /** The unique identifier for the entity. */
    private String id;

    /** The maximum distance for connection */
    private double range;

    /** The height of the entity above ground level. */
    private double height;

    /** The current position of the entity in terms of angle. */
    private Angle position;

    /** The type of the entity (e.g., satellite, device). */
    private String type;

    /** A list of files associated with the entity. */
    private List<FileItem> files;

    /** Linear velocity in kilometers per minute. */
    private double velocity;

    /** Angular velocity in radians per minute. */
    private double angularVelocity;

    /** Direction indicator where clockwise is -1. */
    private int direction;

    /**
     * Constructs an Entity with specified properties and initializes its state.
     *
     * @param id       Unique identifier for the entity.
     * @param range    Maximum connection range in meters.
     * @param position Initial position as an Angle.
     * @param height   Height of the entity above ground.
     * @param type     Type of the entity.
     * @param velocity Linear velocity in km/min.
     */
    public Entity(String id, double range, Angle position, double height, String type, double velocity) {
        this.id = id;
        this.range = range;
        this.position = position;
        this.type = type;
        this.height = height;
        this.files = new ArrayList<>();
        this.velocity = velocity;
        this.angularVelocity = velocity / height; // Assuming conversion from linear to angular velocity
        this.direction = MathsHelper.CLOCKWISE;
    }

    /**
     * Adds a new file to the entity's file list.
     *
     * @param filename The name of the file.
     * @param content  The content of the file.
     */
    public void addFile(String filename, String content) {
        FileItem file = new FileItem(filename, content);
        files.add(file);
    }

    /**
     * Retrieves a complete file by its name.
     *
     * @param filename The name of the file to retrieve.
     * @return The complete FileItem or null if not found.
     */
    public FileItem getCompleteFile(String filename) {
        for (FileItem file : files) {
            if (file.getName().equals(filename) && file.getCompleted()) {
                return file;
            }
        }
        return null;
    }

    /**
     * Retrieves a file by its name regardless of completion status.
     *
     * @param filename The name of the file to retrieve.
     * @return The FileItem or null if not found.
     */
    public FileItem getFile(String filename) {
        for (FileItem file : files) {
            if (file.getName().equals(filename)) {
                return file;
            }
        }
        return null;
    }

    /**
     * Removes a file from the entity's file list by its name.
     *
     * @param filename The name of the file to remove.
     */
    public void removeFile(String filename) {
        files.removeIf(file -> file.getName().equals(filename));
    }

    /**
     * Attempts to receive a file transfer.
     *
     * @param filename The name of the file being transferred.
     * @param content  The content of the file being transferred.
     * @throws FileTransferException If the file transfer cannot be completed.
     */
    public void receiveFile(String filename, String content) throws FileTransferException {
        // subclass implemnetation
    }

    /**
     * Convert entity to EntityInfoResponse
     * @return EntityInfoResponse
     */
    public EntityInfoResponse toInfoResponse() {
        Map<String, FileInfoResponse> map = new HashMap<>();
        this.files.stream().forEach(file -> map.put(file.getName(), file.toInfoResponse()));
        return new EntityInfoResponse(id, position, height, type, map);
    }

    /**
     * Updates the entity's position based on its angular velocity and direction.
     */
    public void move() {
        double pos = getPosition().toRadians() + angularVelocity * direction;

        while (pos >= Math.PI * 2) {
            pos = pos - Math.PI * 2;
        }
        while (pos < 0) {
            pos = pos + Math.PI * 2;
        }
        setPosition(Angle.fromRadians(pos));
    }

    /**
     * return entiry id
     */
    public String getId() {
        return id;
    }

    /**
     * set entiry id
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * return maximum range to connect (unit: m)
     */
    public double getRange() {
        return range;
    }

    /**
     * set maximum range to connect (unit: m)
     */
    public void setRange(double range) {
        this.range = range;
    }

    /**
     * return height
     */
    public double getHeight() {
        return height;
    }

    /**
     * set height
     */
    public void setHeight(double height) {
        this.height = height;
    }

    /**
     * return entiry position
     */
    public Angle getPosition() {
        return position;
    }

    /**
     * set entiry position
     */
    public void setPosition(Angle position) {
        this.position = position;
    }

    /**
     * return entiry type
     */
    public String getType() {
        return type;
    }

    /**
     * set entiry type
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * return files
     */
    public List<FileItem> getFiles() {
        return files;
    }

    /**
     * set files
     */
    public void setFiles(List<FileItem> files) {
        this.files = files;
    }

    /**
     * return linear velocity (km/min)
     */
    public double getVelocity() {
        return velocity;
    }

    /**
     * set linear velocity (km/min)
     */
    public void setVelocity(double velocity) {
        this.velocity = velocity;
    }

    /**
     * return angular velocity (radius/min)
     */
    public double getAngularVelocity() {
        return angularVelocity;
    }

    /**
     * set angular velocity (radius/min)
     */
    public void setAngularVelocity(double angularVelocity) {
        this.angularVelocity = angularVelocity;
    }

    /**
     * return closkwise is -1
     */
    public int getDirection() {
        return direction;
    }

    /**
     * set closkwise is -1
     */
    public void setDirection(int direction) {
        this.direction = direction;
    }

}
