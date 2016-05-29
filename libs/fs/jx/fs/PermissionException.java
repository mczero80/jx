package jx.fs;

/**
 * Die gew&uuml;schte Operation ist in diesem Zusammenhang nicht erlaubt.
 */
public class PermissionException extends FSException {
    public PermissionException() {
	super("Permission denied.");
    }

    public PermissionException(String msg) {
	super(msg);
    }
}
