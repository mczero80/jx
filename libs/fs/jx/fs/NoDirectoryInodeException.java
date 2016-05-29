package jx.fs;

/**
 * Es wird versucht, eine Operation auf eine Datei oder einen symbolischen Link auszuf&uuml;hren, die nur bei einem Verzeichnis
 * Sinn macht bzw. erlaubt ist (z.B. <code>mknode</code>, <code>mkdir</code>).
 */
public class NoDirectoryInodeException extends FSException {
    public NoDirectoryInodeException() {
	super("Not a directory");
    }

    public NoDirectoryInodeException(String msg) {
	super(msg);
    }
}
