package jx.fs;

/**
 * Interner Fehler bei einer Inodeoperation (Inodedaten ung&uuml;tig oder Fehler bei der Ein-/Ausgabe).
 */
public class InodeIOException extends FSException {
    public InodeIOException() {
	super("Internal inode handling error.");
    }

    public InodeIOException(String msg) {
	super(msg);
    }
}
