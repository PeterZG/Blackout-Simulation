package unsw.item;

import unsw.response.models.FileInfoResponse;

/**
 * Represents a file item, which encapsulates the details of a file .
 */
public class FileItem {
    /**
     * The name of the file.
     */
    private String name;

    /**
     * The content of the file.
     */
    private String content;

    /**
     * The total size of the file in bytes.
     */
    private int size;

    /**
     * The size of the file that has been transferred in bytes.
     */
    private int transfered;

    /**
     * Indicates whether the file transfer is completed.
     */
    private boolean completed;

    /**
     * Constructs a new FileItem with the specified filename and content.
     *
     * @param filename The name of the file.
     * @param content  The content of the file.
     */
    public FileItem(String filename, String content) {
        this.name = filename;
        this.content = content;
        this.size = content == null ? 0 : content.length();
        this.completed = true;
    }

    /**
     * Converts the FileItem to a FileInfoResponse object.
     * @return The FileInfoResponse object.
     */
    public FileInfoResponse toInfoResponse() {
        return new FileInfoResponse(name, content, size, completed);
    }


    /**
     * Returns the name of the file.
     *
     * @return The file name.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the file.
     *
     * @param name The new file name.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the content of the file.
     *
     * @return The file content.
     */
    public String getContent() {
        return content;
    }

    /**
     * Sets the content of the file.
     *
     * @param content The new file content.
     */
    public void setContent(String content) {
        this.content = content;
    }

    /**
     * Returns the total size of the file in bytes.
     *
     * @return The file size.
     */
    public int getSize() {
        return size;
    }

    /**
     * Sets the total size of the file in bytes.
     *
     * @param size The new file size.
     */
    public void setSize(int size) {
        this.size = size;
    }

    /**
     * Returns the size of the file that has been transferred in bytes.
     *
     * @return The transferred size.
     */
    public int getTransfered() {
        return transfered;
    }

    /**
     * Sets the size of the file that has been transferred in bytes.
     *
     * @param transfered The new transferred size.
     */
    public void setTransfered(int transfered) {
        this.transfered = transfered;
    }

    /**
     * Returns the completion status of the file transfer.
     *
     * @return true if the file transfer is completed
     */
    public boolean getCompleted() {
        return completed;
    }

    /**
     * Sets the completion status of the file transfer.
     *
     * @param completed The new completion status.
     */
    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

}
