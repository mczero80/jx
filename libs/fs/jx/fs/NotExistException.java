package jx.fs;

/**
 * Die Inode existiert nicht mehr (wurde geloescht).
 */
public class NotExistException extends FSException {
    public NotExistException() {
	super("Inode has been deleted.");
    }

    public NotExistException(String msg) {
	super(msg);
    }
}
