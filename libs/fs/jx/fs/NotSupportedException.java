package jx.fs;

/**
 * Das Dateisystem "versteht" diese Operation nicht
 */
public class NotSupportedException extends FSException {
    public NotSupportedException() {
	super();
    }

    public NotSupportedException(String msg) {
	super(msg);
    }
}
