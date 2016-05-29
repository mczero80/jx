package javafs;

import java.util.Vector;
import jx.zero.Debug;
import jx.zero.Memory;
import jx.zero.Clock;
import jx.fs.DirNotEmptyException;
import jx.fs.FileExistsException;
import jx.fs.FSException;
import jx.fs.InodeIOException;
import jx.fs.InodeNotFoundException;
import jx.fs.NoFileInodeException;
import jx.fs.NoDirectoryInodeException;
import jx.fs.NoSymlinkInodeException;
import jx.fs.NotExistException;
import jx.fs.PermissionException;

import jx.fs.buffercache.*;

/**
 * This class represents a directory. There are some methods inherited from the superclass that make
 * no sense for directories, like read and write. Calling these methods creates a
 * NoFileInodeException or NoSymlinkInodeException.
 */
public class DirInode extends InodeImpl {

    private static final boolean dumpDetails = false;

    public DirInode(FileSystem fileSystem, Super i_sb, int i_ino, InodeData i_data, BufferCache bufferCache, InodeCache inodeCache, Clock clock) {
	super(fileSystem, i_sb, i_ino, i_data, bufferCache, inodeCache, clock);
    }

    public boolean isSymlink() throws NotExistException {
	if (i_released)
	    throw new NotExistException();
	return false;
    }

    public boolean isFile() throws NotExistException {
	if (i_released)
	    throw new NotExistException();
	return false;
    }

    public boolean isDirectory() throws NotExistException {
	if (i_released)
	    throw new NotExistException();
	return true;
    }

    public boolean isWritable() throws NotExistException {
	if (i_released)
	    throw new NotExistException();
	return true;
    }

    public boolean isReadable() throws NotExistException {
	if (i_released)
	    throw new NotExistException();
	return true;
    }

    public boolean isExecutable() throws NotExistException {
	if (i_released)
	    throw new NotExistException();
	return true;
    }

    public String[] readdirNames() throws NoDirectoryInodeException, NotExistException  {
	Vector dirs = readdirNames0();
	// copy into String array
	String[] str = new String[dirs.size()];
	dirs.copyInto(str);
	/*
	String[] str = new String[dirs.size()];
	for(int i=0; i<dirs.size(); i++) {
	    str[i] = (String)dirs.elementAt(i);
	}
	*/
	return str;
    }
    private Vector readdirNames0() throws NoDirectoryInodeException, NotExistException  {
	    int error = 0;
	    int offset = 0, blk, pos = 0, num;
	    BufferHead bh = null, tmp, bha[] = new BufferHead[16];
	    DirEntryData de_data;
	    Vector dirs = new Vector();
	    
	    if (i_released)
		throw new NotExistException();
	    
	    if (dumpDetails) Debug.out.println("Inode " + i_ino + " (block " + i_data.bh.getBlock() + ", offset " + i_data.offset + ")");

	    if (isDirectory() == false) {
	        /*System.out*/Debug.out.println("Inode " + i_ino + " (block " + i_data.bh.getBlock() + ", offset " + i_data.offset + ") != directory");
	        throw new NoDirectoryInodeException();
	    }
	    
	    if (dumpDetails) {
		Debug.out.println("vor readahead: size="+ i_data.i_size()+", mode="+ i_data.i_mode());
	    }

	    while (error == 0 && pos < i_data.i_size()) {
		blk = pos / i_sb.s_blocksize;

		if (dumpDetails) {
		    Debug.out.println("size="+ i_data.i_size()+", mode="+ i_data.i_mode()
				      +", pos="+pos+", blk size="+i_sb.s_blocksize+", blk="+blk);
		}

		if ((bh = bread(blk, false)) == null) {
		    /*System.out*/Debug.out.println("readDir: directory #" + i_ino + " contains a hole at offset " + pos);
		    pos += i_sb.s_blocksize - offset;
		    continue;
		}
		
		boolean READAHEAD = false;
		if (READAHEAD) {
		if (offset == 0) { // readahead
		    num = 0;
		    for (int i = 16 >> (i_sb.s_blocksize / 1024 - 1); i > 0; i--) {
			tmp = getBlk(++blk, false);
			if (tmp == null) throw new Error();
			if (! tmp.isUptodate() && ! tmp.isLocked())
			    bha[num++] = tmp;
			else
			    bufferCache.brelse(tmp);
		    }
		    if (num != 0) {
			for (int k = 0; k < num; k++) {
			    //bufferCache.readAhead(bha[k]); TODO: breadn benutzen
			}
		    }
		}
		}
		if (dumpDetails) Debug.out.println("nach readahead: size="+ i_data.i_size());
		
		de_data = new DirEntryData();
		while (error == 0 && pos < i_data.i_size() && offset < i_sb.s_blocksize) {
		    de_data.init(bh, offset);
		    if (checkDirEntry(de_data) == false) {
			pos = (pos & (i_sb.s_blocksize - 1)) + i_sb.s_blocksize; // On error, skip the pos to the next block.
			bufferCache.brelse(bh);
			return dirs;
		    }
		    offset += de_data.rec_len();
		    if (de_data.inode() != 0)
			dirs.addElement(de_data.name());
		    
		    pos += de_data.rec_len();
		}
		offset = 0;
		bufferCache.brelse(bh);
	    }
	    
	    if (i_sb.isReadonly() == false) {
		i_data.i_atime(clock.getTimeInMillis());
		setDirty(true);
	    }
	    return dirs;
    }

    /**
     * Liefert die Inode, die dem Verzeichniseintrag mit dem angegebenen Namen zugeordnet ist, sofern vorhanden. Zuerst wird
     * die Nummer der Inode ermittelt, indem nach dem Eintrag in der aktuellen Inode gesucht wird. Falls erfolgreich, wird
     * versucht, die Inode im InodeCache zu finden. Falls sie dort nicht verzeichnet ist, wird eine neue Inode angelegt (je
     * nach Typ <code>FileInode</code>, <code>DirInode</code> oder <code>SymlinkeInode</code>), und im Cache verzeichnet.
     *
     * @param     name der Name des Verzeichniseintrags, dessen Inode ermittelt werden soll
     * @return    die dem Verzeichniseintrag zugeordnete Inode
     * @exception InodeIOException          falls ein Fehler bei der Ein-/Ausgabe auftritt
     * @exception InodeNotFoundException    falls die gew&uuml;nschte Inode nicht gefunden werden kann
     * @exception NoDirectoryInodeException falls es sich nicht um ein Verzeichnis handelt
     * @exception PermissionException       falls die Zugriffsrechte des Verzeichnisses die Operation nicht erlauben
     */
    public  jx.fs.Inode getInode(String name) throws InodeIOException, InodeNotFoundException, NoDirectoryInodeException, NotExistException, PermissionException {
	DirEntryData de_data;
	InodeImpl inode;
	
	if (i_released)
	    throw new NotExistException();
	
	if (! permission(MAY_EXEC))
	    throw new PermissionException();
	
	de_data = findDirEntry(name);
	if (de_data == null) {
	    return null; //throw new InodeNotFoundException("Could not find inode for \""+name+"\"");
	}
	
	inode = (InodeImpl)inodeCache.iget(de_data.inode());
	if (inode != null) {
	    bufferCache.brelse(de_data.bh);
	    return inode;
	}
	
	InodeData i_data = i_sb.getInodeData(de_data.inode());
	if (i_data.i_size() < 0) { // Ueberlauf
	    /*System.out*/Debug.out.println("getInode(): Inode zu gross (Ueberlauf)");
	    throw new InodeIOException();
	}
	
	if ((i_data.i_mode() & S_IFMT) == S_IFREG) {
	    //Debug.out.println("getInode(): erzeuge FileInode");
	    inode = new FileInode(fileSystem, i_sb, de_data.inode(), i_data, bufferCache, inodeCache, clock);
	}
	if ((i_data.i_mode() & S_IFMT) == S_IFDIR) {
	    //Debug.out.println("getInode(): erzeuge DirInode");
	    inode = new DirInode(fileSystem, i_sb, de_data.inode(), i_data, bufferCache, inodeCache, clock);
	}
	if ((i_data.i_mode() & S_IFMT) == S_IFLNK) {
	    //Debug.out.println("getInode(): erzeuge SymlinkInode");
	    inode = new SymlinkInode(fileSystem, i_sb, de_data.inode(), i_data, bufferCache, inodeCache, clock);
	}
	bufferCache.brelse(de_data.bh);
	
	if (inode == null) {
	    /*System.out*/Debug.out.println("getInode(): invalid inode type: " + (i_data.i_mode() & S_IFMT));
	    throw new InodeIOException();
	}
	inode.setParent(this);
	inodeCache.addInode(inode);
	return inode;
    }
    
    public  jx.fs.Inode mkdir(String name, int mode) throws FileExistsException, InodeIOException, NoDirectoryInodeException, NotExistException, PermissionException {
	InodeImpl inode;
	BufferHead bh, dir_block;
	DirEntryData de_data;
	
	//Debug.out.println("DirInode.mkdir(" + name + ", " + mode + ")");
	
	if (i_released)
	    throw new NotExistException();
	
	if ((de_data = findDirEntry(name)) != null) {
	    bufferCache.brelse(de_data.bh);
	    throw new FileExistsException();
	}
	
	if (i_data.i_links_count() >= 32000)
	    throw new InodeIOException();
	
	if (! permission(MAY_WRITE | MAY_EXEC))
	    throw new PermissionException();
	
	if ((inode = i_sb.newInode(this, S_IFDIR)) == null)
	    throw new InodeIOException();
	
	if (addDirEntry(name, inode.i_ino) == false) {
	    inode.i_data.i_links_count((short)0);
	    inode.setDirty(true);
	    inode.decUseCount();
	    throw new InodeIOException();
	}
	
	i_data.i_links_count((short)(i_data.i_links_count()+1));
	setDirty(true);
	
	// Inode fuellen, DirEntryDatas fuer "." und ".." erzeugen
	inode.setParent(this);
	inode.i_data.i_size(i_sb.s_blocksize);
	inode.i_data.i_blocks(0);
	
	dir_block = inode.bread(0, true); // neuen Block erzeugen
	if (dir_block == null) {
	    /*System.out*/Debug.out.println("dir_block == null");
	    i_data.i_links_count((short)0);
	    inode.setDirty(true);
	    inode.decUseCount();
	    throw new InodeIOException();
	}
	
	de_data = new DirEntryData(dir_block, 0);
	de_data.inode(inode.i_ino);
	de_data.name_len((short)1);
	de_data.rec_len((short)12);
	de_data.name(".");
	
	de_data.init(de_data.rec_len());
	de_data.inode(((DirInode)parent).i_ino);
	de_data.rec_len((short)(((Super)inode.i_sb).s_blocksize - 12));
	de_data.name_len((short)2);
	de_data.name("..");
	
	inode.i_data.i_links_count((short)2);
	bufferCache.bdwrite(dir_block);
	//inode.i_data.i_mode((short)(S_IFDIR | (mode & (S_IRWXUGO|S_ISVTX) & ~Current.umask)));
	inode.i_data.i_mode((short)(S_IFDIR | (mode & (S_IRWXUGO|S_ISVTX))));
	if ((i_data.i_mode() & S_ISGID) > 0)
	    inode.i_data.i_mode((short)(inode.i_data.i_mode() | S_ISGID));
	inode.setDirty(true);
	
	return inode;
    }

    public  void rmdir(String name) throws DirNotEmptyException, InodeIOException, InodeNotFoundException, NoDirectoryInodeException, NotExistException, PermissionException {
	int time;
	DirEntryData de_data;
	jx.fs.Inode tmp_inode;
	DirInode inode;
	
	if (i_released)
	    throw new NotExistException();
	
	tmp_inode = getInode(name);
	if (tmp_inode.isDirectory() == false) {
	    tmp_inode.decUseCount();
	    throw new NoDirectoryInodeException();
	}
	
	if (! permission(MAY_WRITE | MAY_EXEC) || ! checkSticky())
	    throw new PermissionException();
	
	inode = (DirInode)tmp_inode;
	if (inode.emptyDir() == false) {
	    inode.decUseCount();
	    throw new DirNotEmptyException();
	}
	
	de_data = findDirEntry(name);
	// InodeNotFoundException und InodeIOException werden nicht abgefangen, waeren schon in getInode aufgetreten
	deleteDirEntry(de_data);
	// InodeNotFoundException duerfte nicht auftreten (wg. findDirEntry())
	
	if (inode.i_data.i_links_count() != 2)
	    /*System.out*/Debug.out.println("rmdir: empty directory has links count != 2 (" + inode.i_data.i_links_count() + ")");
	inode.i_data.i_links_count((short)0);
	inode.i_data.i_size(0);
	time = clock.getTimeInMillis();
	inode.i_data.i_ctime(time);
	inode.setDirty(true);
	inode.decUseCount();
	i_data.i_links_count((short)(i_data.i_links_count()-1));
	i_data.i_ctime(time);
	i_data.i_mtime(time);
	setDirty(true);
    }
    
public  jx.fs.Inode create(String name, int mode) throws FileExistsException, InodeIOException, NoDirectoryInodeException, NotExistException, PermissionException {
    InodeImpl inode;
    DirEntryData de_data;
    
	if (i_released)
	    throw new NotExistException();
	
	if ((de_data = findDirEntry(name)) != null) {
	    bufferCache.brelse(de_data.bh);
	    Debug.out.println("File exists.");
	    throw new FileExistsException();
	}
	
	if (! permission(MAY_WRITE | MAY_EXEC))
	    throw new PermissionException();
	
	if ((inode = i_sb.newInode(this, S_IFREG | mode)) == null)
	    throw new InodeIOException();
	inode.setParent(this);
	inode.setDirty(true);
	
	if (addDirEntry(name, inode.i_ino) == false) {
	    inode.i_data.i_links_count((short)0);
	    inode.setDirty(true);
	    inode.decUseCount();
	    throw new InodeIOException();
	}
	
	return (jx.fs.Inode)inode;
    }

    public  void unlink(String name) throws InodeIOException, InodeNotFoundException, NoDirectoryInodeException, NoFileInodeException, NotExistException, PermissionException {
	DirEntryData de_data;
	InodeImpl inode;
	
	if (i_released)
	    throw new NotExistException();
	
	inode = (InodeImpl)getInode(name);
	if (inode.isDirectory()) {  // Symlinks sind erlaubt
	    inode.decUseCount();
	    throw new NoFileInodeException();
	}
	
	if (! permission(MAY_WRITE | MAY_EXEC) || ! checkSticky())
	    throw new PermissionException();
	
	if (inode.i_data.i_links_count() == 0) {
	    /*System.out*/Debug.out.println("unlink: Deleting nonexistent file (" + inode.i_ino + "), " + inode.i_data.i_links_count());
	    inode.i_data.i_links_count((short)1);
	}
	
	de_data = findDirEntry(name);
	// InodeNotFoundException und InodeIOException werden nicht abgefangen, waeren schon in getInode aufgetreten
	deleteDirEntry(de_data);
	// InodeNotFoundException duerfte nicht auftreten (wg. findDirEntry())
	int times = clock.getTimeInMillis();
	i_data.i_ctime(times);
	i_data.i_mtime(times);
	setDirty(true);
	inode.i_data.i_links_count((short)(inode.i_data.i_links_count()-1));
	inode.setDirty(true);
	inode.decUseCount();
    }
    
    /**
     * Erzeugt einen "symbolischen Link", einen Verweis auf einen Verzeichniseintrag. Nach au&szlig;en hin ist zwischen Verweis
     * und urspr&uuml;nglichen Eintrag kein Unterschied festzustellen. Im Zielverzeichnis wird der Verweis unter dem angegebenen
     * Namen angelegt; dazu wird eine spezielle Inode <code>SymlinkInode</code> erzeugt.
     *
     * @param symname der Pfad, auf den ein Verweis angelegt werden soll (der Eintrag muss nicht existieren)
     * @param newname der Name des Verweises
     * @exception FileExistsException       falls bereits ein Eintrag mit angegebenen Namen (<code>newname</code>)
     *                                      existiert
     * @exception InodeIOException          falls bei der Ein-/Ausgabe ein Fehler auftritt
     * @exception NoDirectoryInodeException falls es sich nicht um ein Verzeichnis handelt
     * @exception NotSupportedException     falls das Dateisystem diese Operation nicht unterst&uuml;tzt
     * @exception PermissionException       falls die Zugriffsrechte des Verzeichnisses die Operation nicht erlauben oder das
     *                                      Dateisystem als nur lesbar angemeldet wurde
     */
    public  jx.fs.Inode symlink(String symname, String newname) throws FileExistsException, InodeIOException, NoDirectoryInodeException, NotExistException, PermissionException {
	SymlinkInode symlinkinode;
	DirEntryData de_data;
	boolean success;
	
	if (i_released)
	    throw new NotExistException();
	
	de_data = findDirEntry(newname); // wirft u.U. InodeIOException
	if (de_data != null) // Eintrag existiert bereits
	    throw new FileExistsException();
	
	if (! permission(MAY_WRITE | MAY_EXEC))
	    throw new PermissionException();
	
	if ((symlinkinode = (SymlinkInode)i_sb.newInode(this, (S_IFLNK | S_IRWXUGO))) == null)
	    throw new InodeIOException();
	
	symlinkinode.setSymlink(symname);
	
	success = addDirEntry(newname, symlinkinode.i_ino);
	symlinkinode.i_data.i_links_count((short)2);  // wieso das? UEBERPRUEFEN
	symlinkinode.setDirty(true);
	//symlinkinode.decUseCount();
	if (success == false)
	    throw new InodeIOException();
	return symlinkinode;
    }
	
    public String getSymlink() throws InodeIOException, NoSymlinkInodeException, NotExistException, PermissionException {
	if (i_released)
	    throw new NotExistException();
	throw new NoSymlinkInodeException();
    }

    /**
     * Verschiebt die Inode des angegebenen Verzeichniseintrags bzw. &auml;ndert deren Namen. Der alte Eintrag wird aus der
     * aktuellen Inode gel&ouml;scht und in der angegebenen Inode unter dem Namen <code>newname</code> erzeugt, wobei sich Quell-
     * und Zielinode nicht unterscheiden m&uuml;ssen. Dazu wird die Methode <code>addInode</code> aufgerufen, die einen neuen
     * Verzeichniseintrag erstellt, im Gegensatz zu <code>create</code> aber keine neue Inode erzeugt.
     *
     * @param oldname der Name des Verzeichniseintrags, der verschoben bzw. umbenannt werden soll
     * @param new_dir die Inode des Verzeichnisses, das den zu verschiebenden Verzeichniseintrag aufnehmen soll
     * @param newname der neue Name des Verzeichniseintrags
     * @exception InodeIOException          falls bei der Ein-/Ausgabe ein Fehler auftritt
     * @exception InodeNotFoundException    falls die zu verschiebende Inode nicht gefunden werden kann
     * @exception NoDirectoryInodeException falls es sich nicht um ein Verzeichnis handelt
     * @exception PermissionException       falls der Verzeichniseintrag im Zielverzeichnis nicht angelegt werden darf
     *                                      Daf&uuml;r kann es folgende Gr&uuml;nde geben:
     *                                      1. <code>newname</code> existiert und ist ein Verzeichnis, <code>oldname</code> nicht
     *                                      2. <code>newname</code> existiert und ist kein Verzeichnis, <code>oldname</code> schon
     *                                      3. <code>newname</code> und <code>oldname</code> sind nicht im selben Dateisystem
     *                                      4. <code>newname</code> ist ein Verzeichnis und ist nicht leer
     *                                      5. <code>newname</code> ist ein Unterverzeichnis von <code>oldname</code>
     *                                      6. das Verzeichnis, das <code>newname</code> aufnehmen soll, hat bereits die
     *                                         Maximalzahl an Links
     *                                      7. die Zugriffsrechte des Verzeichnisses erlauben die Operation nicht
     *                                      8. das Dateisystem ist als nur lesbar angemeldet
     */
    public  void rename(String oldname, jx.fs.Inode new_dir, String newname) throws InodeIOException, InodeNotFoundException, NoDirectoryInodeException, NotExistException, PermissionException {
	DirEntryData de_data;
	InodeImpl inode_to_move;
	DirInode new_dirinode;
	
	if (i_released)
	    throw new NotExistException();
	
	if (new_dir == null)
	    new_dir = this;
	
	/* Verzeichnis der zu verschiebenden Inode und Zielverzeichnis muessen demselben Dateisystem
	   angehoeren.
	*/
	if (!(new_dir instanceof DirInode))
	    throw new PermissionException();
	new_dirinode = (DirInode)new_dir;
	
	if (oldname.equals(newname) && new_dir.equals(this))
	    return;
	
	if (! permission(MAY_WRITE | MAY_EXEC) || ! checkSticky())
	    throw new PermissionException();
	
	inode_to_move = (InodeImpl)getInode(oldname);
	// wirft u.U. DirNotEmptyException, InodeIOException, NoDirectoryInodeException, PermissionException
	new_dirinode.renameAddInode(inode_to_move, newname);
	
	de_data = findDirEntry(oldname);
	// InodeNotFoundException und InodeIOException werden nicht abgefangen, waeren schon in getInode aufgetreten
	deleteDirEntry(de_data);
	// InodeNotFoundException duerfte nicht auftreten (wg. findDirEntry())
	    
	int time = clock.getTimeInMillis();
	inode_to_move.i_data.i_ctime(time);
	inode_to_move.i_data.i_mtime(time);
	inode_to_move.setDirty(true);
	if (inode_to_move.isDirectory())
	    i_data.i_links_count((short)(i_data.i_links_count()-1));
	inode_to_move.decUseCount();
	i_data.i_ctime(time);
	i_data.i_mtime(time);
	setDirty(true);
    }

    /**
     * F&uuml;gt die angegebene Inode dem Verzeichnis hinzu. Diese Methode wird von <code>rename</code> im Zielverzeichnis
     * aufgerufen.
     *
     * @param new_inode die Inode, die hinzugef&uuml;gt werden soll
     * @param newname   der Name, der zusammen mit der Inode einen neuen Verzeichniseintrag bilden soll
     * @param check     falls <code>true</code>, wird &uuml;berpr&uuml;ft, ob die Inode nicht schon einem
     *                  &uuml;bergeordneten Verzeichnis zugewiesen wurde.
     * @exception InodeIOException          falls bei der Ein-/Ausgabe ein Fehler auftritt
     * @exception NoDirectoryInodeException falls es sich nicht um ein Verzeichnis handelt
     * @exception PermissionException       falls der Verzeichniseintrag im Zielverzeichnis nicht angelegt werden darf
     *                                      (s. rename)
     */
    public  void renameAddInode(InodeImpl new_inode, String newname) throws InodeIOException, NoDirectoryInodeException, NotExistException, PermissionException {
	DirEntryData de_data;
	
	InodeImpl old_inode = null;
	try {
	    old_inode = (InodeImpl)getInode(newname);
	} catch (InodeIOException e) {
	    new_inode.decUseCount();
	    throw e;
	} catch (InodeNotFoundException e) { /* not an error here */ }
	if (old_inode != null) {
	    if (! permission(MAY_WRITE | MAY_EXEC) || ! checkSticky())
		throw new PermissionException();
	    try {
		if (old_inode.isDirectory()) {
		    if (new_inode.isDirectory()) {
			DirInode old_dirinode = (DirInode)old_inode;
			/* Zielverzeichnis existiert und ist nicht leer oder das Verzeichnis,
			   das die Inode aufnehmen soll (this) hat bereits maximale Linkanzahl.
			*/
			if ((old_dirinode.emptyDir() == false) || (i_data.i_links_count() > 32000))
			    throw new PermissionException();
		    } else {
			/* Verzeichnis mit existierender Datei zu ueberschreiben ist
			   nicht erlaubt.
			*/
			throw new PermissionException();
		    }
		    
		    InodeImpl new_tmp = new_inode;	   
		    while (true) { // new_inode darf nicht schon Unterverzeichnis von old_inode sein
			if (new_tmp.equals(old_inode) == false) { // oder ...equals(this)... ?
			    if (new_tmp.equals(new_tmp.getParent()))
				    break; // wir sind am Root-Verzeichnis angelangt
			    new_tmp = (InodeImpl)new_tmp.getParent();
			    continue;
			}
			throw new PermissionException();
		    }
		} else {
		    if (new_inode.isDirectory())
			throw new PermissionException();
		}
	    } catch (PermissionException e) {
		old_inode.decUseCount();
		new_inode.decUseCount();
		throw e;
	    }
	    old_inode.i_data.i_links_count((short)(old_inode.i_data.i_links_count()-1));
	    old_inode.i_data.i_ctime(clock.getTimeInMillis());
	    old_inode.setDirty(true);
	    old_inode.decUseCount();
	} else {
	    if (! permission(MAY_WRITE | MAY_EXEC))
		throw new PermissionException();
	}
	
	try {
	    de_data = findDirEntry(newname);
	} catch (InodeIOException e) {
	    new_inode.decUseCount();
	    throw e;
	}
	if (de_data != null) {
	    try {
		deleteDirEntry(de_data);
	    } catch (InodeNotFoundException e) { /* wird nie geworfen (s. findDirEntry oben) */ }
	}
	addDirEntry(newname, new_inode.i_ino);
	
	i_data.i_ctime(clock.getTimeInMillis());
	if (new_inode.isDirectory())
	    i_data.i_links_count((short)(i_data.i_links_count()+1));
	setDirty(true);
    }
	
    public int read(Memory b, int off, int len) throws InodeIOException, NoFileInodeException, NotExistException, PermissionException {
	if (i_released)
	    throw new NotExistException();
	throw new NoFileInodeException();
    }

    public int read(int pos, Memory b, int off, int len) throws InodeIOException, NoFileInodeException, NotExistException, PermissionException {
	if (i_released)
	    throw new NotExistException();
	throw new NoFileInodeException();
    }

    public int write(Memory b, int off, int len) throws InodeIOException, NoFileInodeException, NotExistException, PermissionException {
	if (i_released)
	    throw new NotExistException();
	throw new NoFileInodeException();
    }

    public int write(int pos,Memory b, int off, int len) throws InodeIOException, NoFileInodeException, NotExistException, PermissionException {
	if (i_released)
	    throw new NotExistException();
	throw new NoFileInodeException();
    }

    /**
     * Durchsucht das durch diese Inode repr&auml;sentierte Verzeichnis nach einem Eintrag mit dem angegebenen Namen und
     * liefert den Eintrag zur&uuml;ck.
     *
     * @param name der Name des Eintrags, der gesucht werden soll
     * @return falls der Eintrag gefunden werden konnte, wird er als <code>DirEntryData</code>-Objekt zur&uuml;ckgeliefert,
     *         ansonsten ist der R&uuml;ckgabewert <code>null</code>
     */
    public  DirEntryData findDirEntry(String name) throws InodeIOException {
	BufferHead bh = null;
	int bcount, block, offset, de_len;
	DirEntryData de_data;
	if (name.length() > 255)
	    return null;
	
	de_data = new DirEntryData();
	//Debug.out.println("i_size: " + i_data.i_size());
	bcount = i_data.i_size() / i_sb.s_blocksize;
	if (i_data.i_size() % i_sb.s_blocksize != 0)
	    bcount++;
	for (block = 0; block < bcount; block++) {
	    bh = getBlk(block, false); // jeden DirEntry-Block laden
	    if (bh == null) {
		/*System.out*/Debug.out.println("findDirEntry: directory #" + i_ino + " contains a hole at block " + block);
		continue;
	    }
	    bufferCache.updateBuffer(bh);
	    
	    de_data.init(bh, 0);
	    for (offset = 0; offset < i_sb.s_blocksize; offset += de_len) {
		de_data.init(offset);
		if (offset + name.length() <= i_sb.s_blocksize && (name.compareTo(de_data.name()) == 0) && (de_data.inode() != 0)) {
		    // passender Eintrag gefunden
		    if (checkDirEntry(de_data) == false) { // ueberpruefen
			bufferCache.brelse(bh);
			throw new InodeIOException();
		    }
		    return de_data;
		}
		// Endlosschleife (wegen ungueltiger Laenge 0) verhindern
		de_len = de_data.rec_len();
		if (de_len <= 0) {
		    bufferCache.brelse(bh);
		    throw new InodeIOException();
		}
	    }
	    
	    bufferCache.brelse(bh);
	}
	return null;
    }
	
    /**
     * F&uuml;gt dem Verzeichnis unter dem angegebenen Namen einen neuen Eintrag hinzu, wobei dieser mit der angegebenen Inode
     * verkn&uuml;pft wird. Es wird versucht, einen Eintrag zu finden, der gro&szlig; genug ist, um sich und den neuen Eintrag
     * aufzunehmen. (Die L&auml;nge des Eintrags wird zus&auml;tzlich gespeichert, um sie nicht von L&auml;nge des Namens
     * abh&auml;ngig zu machen. Dadurch wird das schnelle Einf&uuml;gen und L&ouml;schen von Eintr&auml;gen ohne aufwendiges
     * Verschieben erm&ouml;glicht.) Falls kein passender Eintrag gefunden wurde, wird ein neuer Block angelegt (die
     * Eintr&auml;ge enden stets an Blockgrenzen). Dieser Eintrag erh&auml;lt die maximale L&auml;nge (Gr&ouml;&szlig;e des
     * Blocks) und wird bei weiteren <code>addDirEntry</code>-Aufrufen nach und nach aufgeteilt.
     *
     * @param name der Name, den der Eintrag bekommen soll
     * @param ino  die Nummer der Inode, die mit dem Eintrag verkn&uuml;pft werden soll
     * @return falls Operation erfolgreich, <code>true</code>, im Falle eines Fehlers oder falls der Eintrag schon existiert,
     *         <code>false</code>
     */
    public  boolean addDirEntry(String name, int i_ino) {
        int offset, in_block_offset;
        short rec_len;
        BufferHead bh;
	DirEntryData de_data;

        // Inodedaten ungueltig
        if ((i_data.i_links_count() == 0) || (name.length() == 0) || (i_data.i_size() == 0)) {
	    /*System.out*/Debug.out.println("addDirEntry: 0" + name.length() + " " + i_data.i_links_count() + " " + i_data.i_size());
	    return false;
	}

        if ((bh = bread(0, false)) == null) {
	    /*System.out*/Debug.out.println("addDirEntry: 1");
	    return false;
	}

        rec_len = (short)((name.length() + 11) & ~3);
        offset = 0;
	in_block_offset = 0;
	
        de_data = new DirEntryData(bh, 0);

        while (true) {
	    if (in_block_offset >= i_sb.s_blocksize) { // naechsten Block laden
		in_block_offset = 0;
		bufferCache.brelse(bh);
		if ((bh = bread(offset / i_sb.s_blocksize, true)) == null) {
		    /*System.out*/Debug.out.println("addDirEntry: 2");
		    return false;
		}

		if (i_data.i_size() <= offset) { // neuer Block ist leer
		    if (i_data.i_size() == 0) {
			/*System.out*/Debug.out.println("addDirEntry: 3");
			return false;
		    }
		    
		    //Debug.out.println("erzeuge neuen Block");
		    
		    de_data.init(bh, 0);
		    de_data.inode(0);
		    de_data.rec_len((short)i_sb.s_blocksize);
		    i_data.i_size(offset + i_sb.s_blocksize); // Verzeichnisgroesse aendern
		    setDirty(true);
		} else {
		    //Debug.out.println("lade naechsten Block");
		    de_data.init(bh, 0);
		}
	    }
	    if (checkDirEntry(de_data) == false) {
		bufferCache.brelse(bh);
		/*System.out*/Debug.out.println("addDirEntry: 4");
		return false;
	    }

	    if ((de_data.inode() != 0) && (name.compareTo(de_data.name()) == 0)) {
		// Eintrag mit diesem Namen existiert bereits
		bufferCache.brelse(bh);
		/*System.out*/Debug.out.println("addDirEntry: 5");
		return false;
	    }

	    if ((de_data.inode() == 0 && de_data.rec_len() >= rec_len) ||   // ungueltiger Eintrag, oder:
		(de_data.rec_len() >= ((de_data.name_len() + 11) & ~3) + rec_len)) {  // Eintrag, der gross genug 
		//offset += de_data.rec_len();
		//in_block_offset += de_data.rec_len();
		if (de_data.inode() > 0) {  // DirEntry-Eintrag ist gross genug fuer beide Eintraege -> aufteilen
		    //Debug.out.println("aufteilen");
		    short tmp_rec_len = de_data.rec_len();
		    short tmp_name_len = de_data.name_len();
		    int tmp_offset = de_data.offset;
		    de_data.rec_len((short)((de_data.name_len() + 11) & ~3));
		    de_data.init(tmp_offset + ((de_data.name_len() + 11) & ~3));
		    de_data.rec_len((short)(tmp_rec_len - ((tmp_name_len + 11) & ~3)));
		    //de_data.init(tmp_offset);
		}
		de_data.inode(i_ino);
		de_data.name_len((short)name.length());
		de_data.name(name);

		i_data.i_mtime(clock.getTimeInMillis());
		i_data.i_ctime(i_data.i_mtime());
		setDirty(true);
		bufferCache.bdwrite(bh);
		//Debug.out.println("returning de_data " + de_data.bh.b_block);
		return true;
	    }
	    offset += de_data.rec_len();
	    in_block_offset += de_data.rec_len();
	    de_data.offset = in_block_offset;
        }
    }

    /**
     * L&ouml;scht den angegebenen Eintrag aus dem Verzeichnis. Steht der Eintrag am Anfang eines Blocks, wird seine Inodenummer
     * auf 0 gesetzt, um ihn als ung&uuml;ltig zu markieren. In der Mitte des Blocks wird der vorangehende Eintrag um die
     * L&auml;nge des zu l&ouml;schenden Eintrags vergr&ouml;&szlig;ert.
     *
     * @param de_data der Verzeichniseintrag, der gel&ouml;scht werden soll
     * @exception InodeNotFoundException falls der zu l&ouml;schende Eintrag nicht gefunden werden kann
     */
    public  void deleteDirEntry(DirEntryData de_data) throws InodeNotFoundException {
	DirEntryData pde_data;
	int i, pde_offset;
	String name = de_data.name();
	
	i = 0;
	pde_offset = -1;
	pde_data = new DirEntryData(de_data.bh, 0);
	while (i < i_sb.s_blocksize) { // vorherigen Eintrag im Block suchen
	    //Debug.out.println("delete: trying block " + de_data.bh.b_block + ", offset " + pde_data.offset + " " + pde_data.name());
	    if (checkDirEntry(pde_data) == false)
		break; // -EIO
	    if (pde_data.offset == de_data.offset) {
		if (pde_offset != -1) {
		    //Debug.out.println("delete: deleting in the middle of the block");
		    // vorherigen Eintrag um die Laenge des zu loeschenden Eintrags erweitern
		    pde_data.init(pde_offset);
		    pde_data.rec_len((short)(pde_data.rec_len() + de_data.rec_len()));
		    pde_data.init(pde_data.offset + pde_data.rec_len());
		} else {
		    //Debug.out.println("delete: deleting first entry");
		    // Der zu loeschende Eintrag ist der erste im Block; durch Setzen der
		    // Inode auf 0 wird er als ungueltig gekennzeichnet.
		    de_data.inode(0);
		}
		bufferCache.bdwrite(de_data.bh);
		return;
	    }
	    i += pde_data.rec_len();
		pde_data.offset += pde_data.rec_len();
	}
	bufferCache.brelse(de_data.bh);
	throw new InodeNotFoundException();
    }

    /**
     * Ermittelt, ob das Verzeichnis, das durch diese Inode repr&auml;sentiert wird, leer ist, d.h. keine
     * Verzeichniseintr&auml;ge enth&auml;lt. Gleichzeitig wird das Verzeichnis &uuml;berpr&uuml;ft auf das Vorhandensein der
     * Eintr&auml;ge "." und ".." und auf Fehler bzw. ung&uuml;ltige Eintr&auml;ge.
     *
     * @return falls das Verzeichnis leer ist und keine fehlerhaften Eintr&auml;ge enth&auml;lt, <code>true</code>, ansonsten
     *         <code>false</code>
     * @exception InodeIOException falls bei der Ein-/Ausgabe ein Fehler auftritt, falls das Verzeichnis fehlerhafte
     *                             Eintr&auml;ge oder keinen "."- oder ".."-Eintrag besitzt
     */
    public  boolean emptyDir()  throws InodeIOException {
	int offset;
	BufferHead bh;
	DirEntryData de_data;
	int err;
	
	if ((i_data.i_size() < 12 + 12) || ((bh = bread(0, false)) == null)) {
	    /*System.out*/Debug.out.println("emptyDir: bad directory (dir #" + i_ino + ") - no data block");
	    throw new InodeIOException();
	}
	de_data = new DirEntryData(bh, 0);
	if (de_data.inode() != i_ino || de_data.name().equals(".") == false) {
	    /*System.out*/Debug.out.println("emptyDir: bad directory (dir #" + i_ino + ") - no `.'");
	    bufferCache.brelse(bh);
	    throw new InodeIOException();
	}
	offset = de_data.rec_len();
	de_data.init(offset);
	if (de_data.inode() == 0 || de_data.name().equals("..") == false) {
	    /*System.out*/Debug.out.println("emptyDir: bad directory (dir #" + i_ino + ") - no `..'");
	    bufferCache.brelse(bh);
	    throw new InodeIOException();
	}
	offset += de_data.rec_len();
	de_data.init(offset);
	while (offset < i_data.i_size()) {
	    if (bh == null || offset >= i_sb.s_blocksize) {
		bufferCache.brelse(bh);
		bh = bread(offset / i_sb.s_blocksize, true);
		if (bh == null) {
		    /*System.out*/Debug.out.println("emptyDir: directory #" + i_ino + " contains a hole at offset " + offset);
		    offset += i_sb.s_blocksize;
		    continue;
		}
		de_data.init(bh, 0);
	    }
	    if (checkDirEntry(de_data) == false) {
		bufferCache.brelse(bh);
		throw new InodeIOException();
	    }
	    if (de_data.inode() > 0) {
		bufferCache.brelse(bh);
		return false;
	    }
	    offset += de_data.rec_len();
	    de_data.init(offset);
	}
	bufferCache.brelse(bh);
	
	return true;
    }

    /**
     * &Uuml;berpr&uuml;ft den angegebenen Verzeichniseintrag. Der Eintrag mu&szlig; eine minimale L&auml;nge von 12 Byte
     * aufweisen (8 Byte Inodenummer, L&auml;nge des Eintrags und L&auml;nge des Namens + mind. 1 Byte f&uuml;r den Namen,
     * Vielfaches von 4). Die L&auml;nge des Eintrags (<code>rec_len</code>) mu&szlig; durch 4 teilbar sein, darf nicht kleiner
     * als die L&auml;nge des Namens + 8 sein und die Gr&ouml;&szlig;e eines Blocks nicht &uuml;berschreiten. Schlie&szlig;lich
     * mu&szlig; die Nummer der Inode kleiner sein als die Anzahl Inodes (<code>s_inodes_count</code> im Superblock).
     *
     * @param de_data der Verzeichniseintrag, der &uuml;berpr&uuml;ft werden soll
     */
    public  boolean checkDirEntry(DirEntryData de_data) {
        String error_msg = new String();

	//Debug.out.println("checkDirEntry: " + de_data.bh.b_block);

        if (de_data.rec_len() < 12) // Eintrag ist zu klein
	    error_msg = "rec_len is smaller than minimal";
	else if (de_data.rec_len() % 4 != 0)
	    error_msg = "rec_len % 4 != 0";
	else if (de_data.rec_len() < ((de_data.name_len() + 11) & ~3)) // Eintrag ist zu klein fuer Namen
	    error_msg = "rec_len is too small for name_len";
        else if (de_data.offset + de_data.rec_len() > i_sb.s_blocksize)
	    error_msg = "directory entry across blocks"; // Laenge des Eintrags ueberschreitet Blockgrenze
        else if (de_data.inode() > i_sb.s_inodes_count) // Nummer der Inode ungueltig
	    error_msg = "inode out of bounds";

        if (error_msg.length() > 0) {
	    /*System.out*/Debug.out.println("bad entry in directory #" + i_ino + ": " + error_msg + " - offset=" +
			       de_data.offset + ", inode=" + de_data.inode() + ", rec_len=" +
			       de_data.rec_len() + ", name_len=" + de_data.name_len());
	    /*System.out*/Debug.out.println("rec_len="+de_data.rec_len() + ", name_len=" + de_data.name_len() + ", vergleich=" + (((de_data.name_len() + 11) & ~3))); // neu
	    return false;
	}
        return true;
    }
}
