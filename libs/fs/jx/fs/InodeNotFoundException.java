package jx.fs;

/**
 * Die angesprochene Inode kann nicht gefunden werden
 */
public class InodeNotFoundException extends FSException {
    public InodeNotFoundException() {
	super("Inode not found.");
    }

    public InodeNotFoundException(String msg) {
	super(msg);
    }
}
