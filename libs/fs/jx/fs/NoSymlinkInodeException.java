package jx.fs;

/**
 * Es wird versucht, eine Operation auf eine Datei oder ein Verzeichnis auszuf&uuml;hren, die nur bei einem symbolischen Link
 * Sinn macht bzw. erlaubt ist (z.B. <code>setSymlink</code>, <code>getSymlink</code>).
 */
public class NoSymlinkInodeException extends FSException {
    public NoSymlinkInodeException() {
	super();
    }

    public NoSymlinkInodeException(String msg) {
	super(msg);
    }
}
