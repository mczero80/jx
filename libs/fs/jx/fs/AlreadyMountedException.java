package jx.fs;

public class AlreadyMountedException extends FSException {
    public AlreadyMountedException() {
	super();
    }

    public AlreadyMountedException(String msg) {
	super(msg);
    }
}
