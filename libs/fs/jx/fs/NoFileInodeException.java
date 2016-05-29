package jx.fs;

/**
 * Es wird versucht, eine Operation auf ein Verzeichnis oder einen symbolischen Link auszuf&uuml;hren, die nur bei einer Datei
 * Sinn macht bzw. erlaubt ist (z.B. <code>read</code> und <code>write</code>).
 */
public class NoFileInodeException extends FSException {
    public NoFileInodeException() {
	super();
    }

    public NoFileInodeException(String msg) {
	super(msg);
    }
}
