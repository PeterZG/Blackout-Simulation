package unsw.blackout;

import unsw.item.Entity;
import unsw.item.FileItem;

/**
 * Represents a file transfer process between two entities.
 */
public class Transfer {
    /** The source entity */
    private Entity from;

    /** The destination entity */
    private Entity to;

    /** The source file item to be transferred */
    private FileItem src;

    /** The destination file item where the file is to be transferred */
    private FileItem dest;

    /**
     * Constructs a Transfer object with specified source and destination entities,
     *
     * @param from the source entity
     * @param to   the destination entity
     * @param src  the source file item to be transferred
     * @param dest the destination file item
     */
    public Transfer(Entity from, Entity to, FileItem src, FileItem dest) {
        this.from = from;
        this.to = to;
        this.src = src;
        this.dest = dest;
    }

    /**
     * Checks if the given entity is the source entity.
     * @param fromId the id of the entity
     * @return true if the given entity is the source entity, false otherwise
     */
    public boolean isFrom(String fromId) {
        return from.getId().equals(fromId);
    }
    public boolean isTo(String toId) {
        return to.getId().equals(toId);
    }

    /**
     * Returns the source entity
     *
     * @return the source entity
     */
    public Entity getFrom() {
        return from;
    }

    /**
     * Sets the source entity
     *
     * @param from the source entity to set
     */
    public void setFrom(Entity from) {
        this.from = from;
    }

    /**
     * Returns the destination entity
     *
     * @return the destination entity
     */
    public Entity getTo() {
        return to;
    }

    /**
     * Sets the destination entity receiving the transfer.
     *
     * @param to the destination entity to set
     */
    public void setTo(Entity to) {
        this.to = to;
    }

    /**
     * Returns the source file item to be transferred.
     *
     * @return the source file item
     */
    public FileItem getSrc() {
        return src;
    }

    /**
     * Sets the source file item to be transferred.
     *
     * @param src the source file item to set
     */
    public void setSrc(FileItem src) {
        this.src = src;
    }

    /**
     * Returns the destination file item
     *
     * @return the destination file item
     */
    public FileItem getDest() {
        return dest;
    }

    /**
     * Sets the destination file item
     *
     * @param dest the destination file item to set
     */
    public void setDest(FileItem dest) {
        this.dest = dest;
    }

}
