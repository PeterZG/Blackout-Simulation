package unsw.item;

import java.util.List;
import java.util.stream.Collectors;
import java.util.ArrayList;

import unsw.blackout.FileTransferException;
import unsw.blackout.FileTransferException.VirtualFileNoBandwidthException;
import unsw.blackout.FileTransferException.VirtualFileNoStorageSpaceException;
import unsw.utils.Angle;

/**
 * Satellite infomation
 */
public class Satellite extends Entity  {
    /** support device types */
    private List<String> supportTypes;

    /** max storage file count */
    private int maxFile;

    /** max storage bytes */
    private int maxStore;

    /** send byte count per minute */
    private int bandwidthSend;

    /** receive byte count per minute */
    private int bandwidthReceive;



    public Satellite(String id, double range, double height, Angle position, double velocity, String type) {
        super(id, range, position, height, type, velocity);
        supportTypes = new ArrayList<>();
    }

    public static Satellite createSatellite(String satelliteId, String type, double height, Angle position) {

        if ("StandardSatellite".equals(type)) {
            return new StandardSatellite(satelliteId, height, position);
        } else if ("TeleportingSatellite".equals(type)) {
            return new TeleportingSatellite(satelliteId, height, position);
        } else if ("RelaySatellite".equals(type)) {
            return new RelaySatellite(satelliteId, height, position);
        } else {
            return new StandardSatellite(satelliteId, height, position);
        }
    }

    public boolean canSupport(String type) {
        return supportTypes.contains(type);
    }

    @Override
    public void receiveFile(String filename, String content) throws FileTransferException {

        int transferCount = getFiles().stream().filter(e -> !e.getCompleted()).collect(Collectors.toList()).size();
        if (transferCount == getBandwidthReceive()) {
            throw new VirtualFileNoBandwidthException(getId());
        }

        if (getFiles().size() == getMaxFile()) {
            throw new VirtualFileNoStorageSpaceException("Max Files Reached");
        }

        int totalSize = 0;
        for (FileItem file : getFiles()) {
            totalSize += file.getTransfered();
        }

        if (totalSize >= getMaxStore()) {
            throw new VirtualFileNoStorageSpaceException("Max Storage Reached");
        }

    }



    /** return support device types */
    public List<String> getSupportTypes() {
        return supportTypes;
    }

    /** set support device types */
    public void setSupportTypes(List<String> supportTypes) {
        this.supportTypes = supportTypes;
    }

    /** return max storage file count */
    public int getMaxFile() {
        return maxFile;
    }

    /** set max storage file count */
    public void setMaxFile(int maxFile) {
        this.maxFile = maxFile;
    }

    /** return max storage bytes */
    public int getMaxStore() {
        return maxStore;
    }

    /** set max storage bytes */
    public void setMaxStore(int maxStore) {
        this.maxStore = maxStore;
    }

    /** return send byte count per minute */
    public int getBandwidthSend() {
        return bandwidthSend;
    }

    /** set send byte count per minute */
    public void setBandwidthSend(int bandwidthSend) {
        this.bandwidthSend = bandwidthSend;
    }

    /** return receive byte count per minute */
    public int getBandwidthReceive() {
        return bandwidthReceive;
    }

    /** set receive byte count per minute */
    public void setBandwidthReceive(int bandwidthReceive) {
        this.bandwidthReceive = bandwidthReceive;
    }


}
