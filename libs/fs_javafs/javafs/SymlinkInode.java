package javafs;

import java.util.Vector;
import jx.zero.Debug;
import jx.zero.Memory;
import jx.zero.Clock;
import jx.fs.FSException;
import jx.fs.InodeIOException;
import jx.fs.NoDirectoryInodeException;
import jx.fs.NoFileInodeException;
import jx.fs.NotExistException;
import jx.fs.PermissionException;

import jx.fs.buffercache.*;

/**
 * Diese Klasse enth&auml;lt alle Methoden, die f&uuml;r einen symbolischen Link von Bedeutung sind (<code>getSymlink</code> und
 * <code>setSymlink</code>). Methoden der Superklasse <code>Inode</code>, die f&uuml;r einen symbolischen Link keinen Sinn ergeben
 * bzw. nicht erlaubt sind (z.B. <code>mkdir</code> oder <code>read</code>), erzeugen beim Aufruf eine
 * <code>NoDirectoryInodeException</code> bzw. <code>NoFileInodeException</code>.
 */
public class SymlinkInode extends InodeImpl {

    public SymlinkInode(FileSystem fileSystem, Super i_sb, int i_ino, InodeData i_data, BufferCache bufferCache, InodeCache inodeCache, Clock clock) {
	super(fileSystem, i_sb, i_ino, i_data, bufferCache, inodeCache, clock);
    }

    public boolean isSymlink() { // throws NotExistException {
	//if (i_released)
	//throw new NotExistException();
	return true;
    }

    public boolean isFile() { // throws NotExistException {
	//if (i_released)
	//throw new NotExistException();
	return false;
    }

    public boolean isDirectory() {
	return false;
    }

    public boolean isWritable() { // throws NotExistException {
	//if (i_released)
	//throw new NotExistException();
	return true;
    }

    public boolean isReadable() { // throws NotExistException {
	//if (i_released)
	//throw new NotExistException();
	return true;
    }

    public boolean isExecutable() { // throws NotExistException {
	//if (i_released)
	//throw new NotExistException();
	return true;
    }

    public String[] readdirNames() {
    //throws NoDirectoryInodeException, NotExistException {
	try {
	    if (i_released)
		throw new NotExistException();
	    throw new NoDirectoryInodeException();
	} catch (FSException e) { } // alle Exceptions ignorieren
	return null;
    }

    public jx.fs.Inode getInode(String name) {
    //throws InodeIOException, InodeNotFoundException, NoDirectoryInodeException, NotExistException, PermissionException {
	try {
	    if (i_released)
		throw new NotExistException();
	    throw new NoDirectoryInodeException();
	} catch (FSException e) { } // alle Exceptions ignorieren
	return null;
    }

    public jx.fs.Inode mkdir(String name, int mode) {
    //throws FileExistsException, InodeIOException, NoDirectoryInodeException, NotExistException, PermissionException {
	try {
	    if (i_released)
		throw new NotExistException();
	    throw new NoDirectoryInodeException();
	} catch (FSException e) { } // alle Exceptions ignorieren
	return null;
    }

    public void rmdir(String name) {
    //throws DirNotEmptyException, InodeIOException, InodeNotFoundException, NoDirectoryInodeException, NotExistException,
    //PermissionException {
	try {
	    if (i_released)
		throw new NotExistException();
	    throw new NoDirectoryInodeException();
	} catch (FSException e) { } // alle Exceptions ignorieren
    }

    public jx.fs.Inode create(String name, int mode) {
    //throws FileExistsException, InodeIOException, NoDirectoryInodeException, NotExistException, PermissionException {
	try {
	    if (i_released)
		throw new NotExistException();
	    throw new NoDirectoryInodeException();    
	} catch (FSException e) { } // alle Exceptions ignorieren
	return null;
    }

    public void unlink(String name) {
    //throws InodeIOException, InodeNotFoundException, NoDirectoryInodeException, NoFileInodeException, NotExistException,
    //PermissionException {
	try {
	    if (i_released)
		throw new NotExistException();
	    throw new NoDirectoryInodeException();
	} catch (FSException e) { } // alle Exceptions ignorieren
    }

    public jx.fs.Inode symlink(String symname, String newname) {
    //throws FileExistsException, InodeIOException, NoDirectoryInodeException, NotExistException, NotSupportedException,
    //PermissionException {
	try {
	    if (i_released)
		throw new NotExistException();
	    throw new NoDirectoryInodeException();
	} catch (FSException e) { } // alle Exceptions ignorieren
	return null;
    }

    public void rename(String oldname, jx.fs.Inode new_dir, String newname) {
    //throws InodeIOException, InodeNotFoundException, NoDirectoryInodeException, NotExistException, PermissionException {
	try {
	    if (i_released)
		throw new NotExistException();
	    throw new NoDirectoryInodeException();
	} catch (FSException e) { } // alle Exceptions ignorieren
    }

    public int read(int pos, Memory b, int off, int len) throws InodeIOException, NoFileInodeException, NotExistException, PermissionException {
	if (i_released)
	    throw new NotExistException();
	throw new NoFileInodeException();
    }

    public int read(Memory b, int off, int len) throws InodeIOException, NoFileInodeException, NotExistException, PermissionException {
	if (i_released)
	    throw new NotExistException();
	throw new NoFileInodeException();
    }

    public int write(int pos, Memory b, int off, int len) throws InodeIOException, NoFileInodeException, NotExistException, PermissionException {
	if (i_released)
	    throw new NotExistException();
	throw new NoFileInodeException();
    }

    public int write(Memory b, int off, int len) throws InodeIOException, NoFileInodeException, NotExistException, PermissionException {
	if (i_released)
	    throw new NotExistException();
	throw new NoFileInodeException();
    }

    /**
     * Setzt den Pfad der Inode, auf die der symbolische Link verweisen soll. Ist der Pfad klein genug, wird er innerhalb der
     * Zeiger auf die direkten und indirekten Bl&ouml;cke abgelegt, ansonsten wird ein neuer Block angefordert, der den
     * Pfadnamen aufnimmt.
     *
     * @param     symname             der Pfadname, auf den verwiesen werden soll
     * @exception InodeIOException    falls bei der Ein-/Ausgabe ein Fehler auftritt
     * @exception PermissionException falls die Zugriffsrechte des symbolischen Links die Operation nicht erlauben oder das
     *                                Dateisystem als nur lesbar angemeldet wurde
     */
    public  void setSymlink(String symname) {
    //throws InodeIOException, NotExistException, PermissionException {
	try {
	    SymlinkBlockData symlink_data;
	    BufferHead name_block;
	    int len;
	    
	    if (i_released)
		throw new NotExistException();
	    
	    if (! permission(MAY_WRITE))
		throw new PermissionException();
	    
	    len = symname.length();
	    if (len > i_sb.s_blocksize-1)
		len = i_sb.s_blocksize-1;
	    if (len >= 60) {
		//Debug.out.println("len = " + len + ", normal symlink");
		name_block = bread(0, true);
		if (name_block == null) {
		    setDirty(true);
		    decUseCount();
		    throw new InodeIOException();
		}
		symlink_data = new SymlinkBlockData(name_block, 0);
		symlink_data.i_symlink(symname);
		bufferCache.bdwrite(name_block);
	    } else {
		//Debug.out.println("len = " + len + ", fast symlink");
		i_data.i_symlink(symname);
	    }
	    
	    i_data.i_size(len);
	    i_data.i_links_count((short)(i_data.i_links_count()-1));
	    setDirty(true);
	} catch (FSException e) { } // alle Exceptions ignorieren
    }
	
    public  String getSymlink() {
    //throws InodeIOException, NoSymlinkInodeException, NotExistException, NotSupportedException, PermissionException {
	try {
	    SymlinkBlockData symlink_data;
	    BufferHead name_block;
	    String symname;
	    int len;

	    if (i_released)
		throw new NotExistException();
	    
	    if (! permission(MAY_WRITE))
		throw new PermissionException();

	    len = i_data.i_size();
	    if (len >= 60) {
		Debug.out.println("len = " + len + ", normal symlink");
		name_block = bread(0, true);
		if (name_block == null) {
		    setDirty(true);
		    decUseCount();
		    throw new InodeIOException();
		}
		symlink_data = new SymlinkBlockData(name_block, 0);
		symname = symlink_data.i_symlink(len);
		bufferCache.brelse(name_block);
	    } else {
		//Debug.out.println("len = " + len + ", fast symlink");
		symname = i_data.i_symlink(len);
	    }
	    
	    return symname;
	} catch (FSException e) { } // alle Exceptions ignorieren
	return null;
    }
}
