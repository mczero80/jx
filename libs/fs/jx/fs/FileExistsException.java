package jx.fs;

/**
 * Die Inode existiert schon.
 */
public class FileExistsException extends FSException {
    public FileExistsException() {
	super("File already exists.");
    }

    public FileExistsException(String msg) {
	super(msg);
    }
}
