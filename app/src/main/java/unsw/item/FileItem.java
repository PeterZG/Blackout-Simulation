package unsw.item;

import unsw.response.models.FileInfoResponse;

/**
 * Text payload and transfer state. Sizes count Java UTF-16 characters, not encoded bytes.
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
     * The total number of characters in the file.
     */
    private int size;

    /**
     * Characters received so far while a transfer is incomplete.
     */
    private int transferredCharacters;

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
     * Returns the total character count of the file.
     *
     * @return The file size.
     */
    public int getSize() {
        return size;
    }

    /**
     * Sets the total size of the file in UTF-16 characters.
     *
     * @param size The new file size.
     */
    public void setSize(int size) {
        this.size = size;
    }

    /**
     * Returns received characters. The original public method name is retained for compatibility.
     *
     * @return The transferred size.
     */
    public int getTransfered() {
        return transferredCharacters;
    }

    /**
     * Sets the number of UTF-16 characters received so far.
     *
     * @param transferredCharacters The received character count.
     */
    public void setTransfered(int transferredCharacters) {
        this.transferredCharacters = transferredCharacters;
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
