package javafs;

import java.util.Vector;
import java.util.Hashtable;
import java.util.Enumeration;
import jx.zero.Debug;
import jx.fs.InodeIOException;
import jx.fs.NotExistException;

/**
 * Cache for inodes.
 */
public class InodeCache {
    private Vector    inode_in_use;
    private Vector    inode_dirty;
    private Hashtable inode_hashtable;
    private final int max_inodes = 16384;
    private int nr_inodes = 0;
    private static final boolean trace = false;

    InodeCache() {
	if (trace) Debug.out.println("Initialising InodeCache");
	inode_in_use    = new Vector(100);
	inode_dirty     = new Vector(100);
	inode_hashtable = new Hashtable();
    }

    /**
     * Shows caches statistics.
     */
    public  void showInodes() {
        Debug.out.println("InodeCache enthaelt " + inode_hashtable.size() + " Elemente");
	Enumeration enum = inode_hashtable.elements();
	while (enum.hasMoreElements()) {
	    InodeImpl inode = (InodeImpl)enum.nextElement();
	    Debug.out.print("inode " + inode.i_ino + ", hashkey " + inode.i_hashkey.hashCode());
	    Debug.out.print(" (count=" + inode.i_count + ", i_blocks=" + inode.i_data.i_blocks() + ")");
	    if (inode.isDirty())
		Debug.out.println(", dirty");
	    else
		Debug.out.println(", clean");
	}
	for (int i = 0; i < inode_dirty.size(); i++) {
	    InodeImpl inode = (InodeImpl)inode_dirty.elementAt(i);
	    Debug.out.println("dirty-list: " + inode.i_ino);
	}
	for (int i = 0; i < inode_in_use.size(); i++) {
	    InodeImpl inode = (InodeImpl)inode_in_use.elementAt(i);
	    Debug.out.println("in-use-list: " + inode.i_ino);
	}
    }

    private  void listDel(InodeImpl inode) {
	if (inode.i_list != null) {
	    inode.i_list.removeElement(inode); // inode.i_list hat auf jeden Fall einen gueltigen Wert
	    inode.i_list = null;
	}
    }

    private  void listAdd(InodeImpl inode, Vector vec) {
	vec.addElement(inode);
	inode.i_list = vec;
    }

    /**
     * Marks inode as dirty.
     *
     * @param inode Inode that is dirty
     */
    public  void markInodeDirty(InodeImpl inode) {
	/* Only add valid (ie hashed) inodes to the dirty list */
	if (inode.i_ishashed) {
	    listDel(inode);
	    listAdd(inode, inode_dirty);
	    if (trace) Debug.out.println("markInodeDirty: marking inode " + inode.i_ino + " dirty");
	}
	else if (trace) Debug.out.println("markInodeDirty: inode " + inode.i_ino + " not hashed");
    }

    private  void waitOnInode(InodeImpl inode) {
	//while (true) {
	    //Debug.out.println("waitOnInode()");
	    //if (inode.isisLocked())
	    //{ //inode.sleep();
	    //    while (inode.isLocked()) ;
	    //}
	    //else
	//return;
	//}
    }

    private  void syncOne(InodeImpl inode) {
	Debug.out.println("InodeCache.syncOne("+inode.i_ino+")");

	/*	
        if (inode.isLocked()) {
	    //waitOnInode(inode);
	    throw new Error();
	}
	*/
	listDel(inode);
	listAdd(inode, inode_in_use);
	//inode.setLocked(true); // set I_LOCK
	inode.setDirty(false); // reset I_DIRTY
	try {
	    inode.writeInode();
	} catch (InodeIOException e) {
	} catch (NotExistException e) { }
	//inode.setLocked(false);
	//inode.wakeUp();
    }

    /**
     * Schreibt die Verwaltungsdaten aller Inodes, die als "dirty" markiert sind und dem Dateisystem angeh&ouml;ren, das
     * sich auf der angegebenen Partition befindet, auf die Festplatte.
     *
     */
    public  void syncInodes() {
	InodeImpl inode;

	//Debug.out.println("syncing Inodes of device " + device + ", size of dirty list: " + inode_dirty.size());

	Vector dirty_clone = inode_dirty;
	for (int i = 0; i < dirty_clone.size(); i++) {
	    inode = (InodeImpl)dirty_clone.elementAt(i);
	    syncOne(inode);
	}
    }

    /**
     * Die Inode wird auf Festplatte geschrieben, sofern sie als "dirty" markiert ist.
     *
     * @param inode die Inode, die geschrieben werden soll
     */
    public  void writeInodeNow(InodeImpl inode) {
	while (inode.isDirty())
	    syncOne(inode);
    }

    private  void disposeList(Vector vec) {
	InodeImpl inode;
        int count = vec.size();

	for (int i = 0; i < count; i++) {
	    inode = (InodeImpl)vec.firstElement();
	    //Debug.out.println("disposeList: disposing inode " + inode.i_ino);
	    waitOnInode(inode);
	    vec.removeElementAt(0);
	    inode.i_list = null;
	    try {
		inode.putInode();
	    } catch (NotExistException e) { }
	    nr_inodes--;
	}
    }

    private  boolean invalidateList(Vector vec, Vector dispose) {
        int i = 0;
	boolean busy = false;
	InodeImpl inode;

	while (i < vec.size()) {
	    inode = (InodeImpl)vec.elementAt(i);
	    //Debug.out.println("invalidateList: examing inode " + inode.i_ino + " [" + metaxa.os.devices.ide.IDEDevice.deviceToName(inode.i_device) + "]");
	    i++;
	    /*
	    if (inode.i_count == 0) {
		//Debug.out.println("invalidateList: removing inode " + inode.i_ino + " [" + metaxa.os.devices.ide.IDEDevice.deviceToName(inode.i_device) + "]");
		removeInodeHash(inode);
		listDel(inode);
		listAdd(inode, dispose);
		//vec.removeElementAt(i);
		continue;
	    }
	    i++;
	    //Debug.out.println("invalidateList: Inode " + inode.i_ino + " is busy (" + inode.i_count + ")");
	    busy = true;
	    */
        }
        return busy;
    }

    /**
     * Entfernt die Inodes des durch die angegebene Partition bezeichneten Dateisystems aus dem Cache. Vorher sollte
     * <code>syncInodes</code> aufgerufen werden, damit &auml;nderungen nicht verloren gehen.
     *
     * @param die Partition, die das Dateisystem mit den nicht mehr ben&ouml;tigten Inodes enth&auml;lt
     */
    public  boolean invalidateInodes() {
	boolean busy;
	Vector throw_away = new Vector(inode_in_use.size() + inode_dirty.size());

	///*System.out*/Debug.out.println("invalidateInodes(" + metaxa.os.devices.ide.IDEDevice.deviceToName(device) + ")");

        busy = invalidateList(inode_in_use, throw_away);
	if (invalidateList(inode_dirty, throw_away))
	    busy =  true;
        disposeList(throw_away);

	///*System.out*/Debug.out.println("invalidateInodes - Ende");

        return busy;
    }

    private  boolean freeInodes() {
        Vector freeable = new Vector();
	InodeImpl inode;

	int i = 0;
	while (i < inode_in_use.size()) {
	    inode = (InodeImpl)inode_in_use.elementAt(i);
	    if (inode.i_count != 0) {
		i++;
		continue;
	    }

	    listDel(inode);
	    removeInodeHash(inode);
	    freeable.addElement(inode);
        }
	disposeList(freeable);

        return (freeable.size() > 0);
    }

    private InodeImpl findInode(int ino_nr) {
	InodeImpl inode = (InodeImpl)inode_hashtable.get(new InodeHashKey(ino_nr));
	if (inode != null)
	    inode.i_count++;
	return inode;
    }

    /** 
     * F&uuml;gt dem Cache eine Inode hinzu.
     *
     * @param inode die Inode, die in den Cache aufgenommen werden soll
     */
    public  void addInode(InodeImpl inode) {
	if (nr_inodes > max_inodes) {
	    if (freeInodes() == false) {
		syncInodes();       // alle Inodes ausschreiben
		invalidateInodes(); // und aus dem Cache entfernen
	    }
	}
	listAdd(inode, inode_in_use);
	insertInodeHash(inode);
	//inode.wakeUp();
	inode.i_count = 1;
	nr_inodes++;
    }

    /**
     * Tries to find the inode with the given number in the cache.
     * If the inode is not in the cache null is returned.
     *
     * @param ino    inode number
     * @return       the inode
     */
    public  InodeImpl iget(int ino) {
	InodeImpl inode;

	inode = findInode(ino);
        if (inode != null) {
	    //Debug.out.println("iget: getting " + ino + ", count = " + inode.i_count);
	    waitOnInode(inode);
	    return inode;
        }

	//Debug.out.println("iget: inode " + ino + " not found");
	return null;
    }

    private  void insertInodeHash(InodeImpl inode) {
	inode.i_hashkey = new InodeHashKey(inode.i_ino);
	if (inode_hashtable.put(inode.i_hashkey, inode) != null)
	    Debug.out.println("Kollision!");
	inode.i_ishashed = true;
    }

    private  void removeInodeHash(InodeImpl inode) {
	if (inode.i_hashkey == null)
	    return;
	inode_hashtable.remove(inode.i_hashkey);
	inode.i_ishashed = false;
    }

    /**
     * Teilt dem Cache mit, das die angegebene Inode nicht mehr verwendet wird. Der Nutzungsz&auml;hler der Inode wird
     * erniedrigt. Falls auf die Inode nicht mehr verwiesen wird (<code>inode.i_nlinks</code>), wird die Inode gel&ouml;scht.
     *
     * @param inode die freizugebende Inode
     */
    public  void iput(InodeImpl inode) {
        if (inode == null) return;
	
	inode.i_count--;
	
	if (trace) Debug.out.println("iput: " + inode.i_ino + ", count = " + inode.i_count);
	
	if (inode.i_count == 0) {
	    try {
		if (inode.i_nlinks() == 0) {
		    removeInodeHash(inode);
		    inode.deleteInode();
		} // (inode.i_ishashed == false) trifft zu
		if (inode.i_ishashed == false) {
		    listDel(inode);
		    inode.putInode();
		    nr_inodes--;
		} else if (inode.isDirty() == false) {
		    listDel(inode);
		    listAdd(inode, inode_in_use);
		}
	    } catch(NotExistException e) { Debug.out.println("Not Exist!");
	    } catch(InodeIOException e) {Debug.out.println("InodeIO!"); }
	}
	if (inode.i_count < 0) {
	    Debug.out.println("iput: Fehler: inode " + inode.i_ino + ", i_count = " + inode.i_count);
	    throw new Error("inode refcount < 0");
	}
	if (trace) if (inode.i_count > 0) Debug.out.println("iput: inode " + inode.i_ino + " count changed, count = " + inode.i_count);
	
    }
}
