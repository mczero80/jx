package vfs;

import java.util.Vector;
import java.util.Hashtable;
import java.util.Enumeration;
import jx.zero.Debug;
import jx.fs.Inode;
import jx.fs.InodeImpl;
import jx.fs.*;

/**
 * Diese Klasse dient als Verwaltungsstelle und Zwischenspeicher f&uuml;r Verzeichniseintr&auml;ge, um h&auml;ufige
 * Festplattenzugriffe beim Zugriff auf Dateien und Verzeichnisse zu verhinden. Dieser Cache bildet Pfadnamen auf Inodes ab.
 */
public class DirEntryCache {
    private Hashtable dentry_hashtable;
    private final int max_dentries = 1024;

    private static DirEntryCache instance = new DirEntryCache();

    private DirEntryCache() {
	dentry_hashtable = new Hashtable();
    }

    public static DirEntryCache instance() {
	return instance;
    }

    /** 
     * F&uuml;gt dem Cache eine Inode hinzu.
     *
     * @param inode die Inode, die in den Cache aufgenommen werden soll
     */
    public synchronized void addEntry(String pfad, Inode inode) {
	if (dentry_hashtable.size() >= max_dentries)
	    invalidateEntries();
	dentry_hashtable.put(pfad, inode);
    }

    /**
     * Liefert die Inode zum angegebenen Pfad aus dem Cache. Falls die Inode im Cache nicht vorhanden ist, wird
     * <code>null</code> zur&uuml;ckgegeben.
     *
     * @param pfad der Pfad innerhalb des Dateisystems, der auf die gew&uuml;nschte Inode verweist
     * @return die gew&uuml;nschte Inode, falls im Cache vorhanden, ansonsten <code>null</code>
     */
    public synchronized Inode getEntry(String pfad) {
	Inode inode = (Inode)dentry_hashtable.get(pfad);
	if (inode != null)
	    inode.incUseCount();
	return inode;
    }

    /**
     * Teilt dem Cache mit, das der angegebene Entrag nicht mehr verwendet wird bzw. gel&ouml;scht wurde.
     *
     * @param pfad der Pfad des Eintrags, der aus dem Cache entfernt werden soll
     */
    public synchronized void removeEntry(String pfad) {
	Inode inode = (Inode)dentry_hashtable.remove(pfad);
	if (inode == null)
	    return;

	inode.decUseCount(); // <- DIE zentrale Methode (brelse(idata.bh), evtl. deleteInode())
    }

    /**
     * Benennt einen Verzeichniseintrag um.
     *
     * @param pfad       der alte Pfad des Eintrags
     * @param neuer_pfad der neue Pfad des Eintrags
     */
    public synchronized void moveEntry(String pfad, String neuer_pfad) {
	Inode inode = (Inode)dentry_hashtable.remove(pfad);
	if (inode != null)
	    dentry_hashtable.put(neuer_pfad, inode);
    }

    /**
     * Entfernt die Eintr&auml;ge des durch die angegebene Partition bezeichneten Dateisystems aus dem Cache.
     *
     */
    public synchronized void invalidateEntries() {
	boolean busy;
	Vector throw_away = new Vector();

	Enumeration enumEntries = dentry_hashtable.elements();
	Enumeration enumKeys    = dentry_hashtable.keys();
	while (enumEntries.hasMoreElements() && enumKeys.hasMoreElements()) {
	    Inode inode = (Inode)enumEntries.nextElement();
	    String pfad = (String)enumKeys.nextElement();
	    dentry_hashtable.remove(pfad);
	    inode.decUseCount();  // <- DIE zentrale Methode (brelse(idata.bh), evtl. deleteInode())
	}
    }

    /**
     * Schreibt die Eintr&auml;ge des durch die angegebene Partition bezeichneten Dateisystems, die als "dirty" markiert sind,
     * auf Festplatte.
     *
     */
    public synchronized void syncEntries() throws NotExistException, InodeIOException {
	boolean busy;
	Vector throw_away = new Vector();

	Enumeration enumEntries = dentry_hashtable.elements();
	Enumeration enumKeys    = dentry_hashtable.keys();
	while (enumEntries.hasMoreElements() && enumKeys.hasMoreElements()) {
	    Inode inode = (Inode)enumEntries.nextElement();
	    String pfad = (String)enumKeys.nextElement();
	    dentry_hashtable.remove(pfad);
	    //try {
	    if (inode.isDirty())
		inode.writeInode();
	    //} catch (InodeIOException e) {
	    //} catch (NotExistException e) {
	    //}
	}
    }

    /**
     * Gibt eine Statistik der Verzeichniseintr&auml;ge im Cache aus.
     */
    public synchronized void showDirEntries() {
        /*System.out*/Debug.out.println("DirEntryCache enthaelt " + dentry_hashtable.size() + " Elemente");
	Enumeration enum = dentry_hashtable.keys();
	while (enum.hasMoreElements())
	    /*System.out*/Debug.out.println((String)enum.nextElement());
    }
}
