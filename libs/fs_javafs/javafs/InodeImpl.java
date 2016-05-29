package javafs;

import java.util.Vector;
import jx.zero.Debug;
import jx.zero.Clock;
import jx.fs.FSException;
import jx.fs.InodeIOException;
import jx.fs.InodeNotFoundException;
import jx.fs.PermissionException;
import jx.fs.NotExistException;
import jx.fs.FileExistsException;
import jx.fs.DirNotEmptyException;
import jx.fs.NoFileInodeException;
import jx.fs.NoSymlinkInodeException;
import jx.fs.NoDirectoryInodeException;
import jx.fs.NotSupportedException;

import jx.fs.buffercache.*;

/**
 * This is the baseclass for all inodes.
 * Subclasses are DirInode, FileInode, SymlinkInode.
 */
public abstract class InodeImpl extends jx.fs.InodeImpl {
    private final static boolean trace = false;
    private final static boolean dumpDetails = false;
    private final static boolean dumpBlockAlloc=false;
    private   int         i_next_alloc_block, i_next_alloc_goal;
    protected int         i_ino; // oder nach unten
    protected InodeData   i_data;
    protected Super       i_sb;
    protected BufferCache bufferCache;
    protected InodeCache  inodeCache;
    /** used by InodeCache */
    public    InodeHashKey i_hashkey;
    /** used by InodeCache */
    public    Vector       i_list;
    /** used by InodeCache */
    public    boolean      i_ishashed;
    /** used by InodeCache */
    public    int          i_count;

    protected static final int MAY_READ  = S_IROTH;
    protected static final int MAY_WRITE = S_IWOTH;
    protected static final int MAY_EXEC  = S_IXOTH;

  /**
   * Should safety checks should be performed at our interface?
   * No means: we trust the invoker to supply "good" parameters
   * Yes means: dont trust them
   */
  private static final boolean safety = false;

    // these variables are only needed in getblk but
    // it is faster to compute them once and store them
    // in the object
    private /*final*/ int addr_per_block;
  private /*final*/ int addr_per_block_bits;
  private /*final*/ int addr;
    
    private /*final*/ int blocksize;

    /* speed up hack for blockGetBlk */
    private InodeBlockData global_bd_slot = null;

    private int maxblock;

    protected Clock clock;

    
    protected FileSystem fileSystem;
   

    public InodeImpl(FileSystem fileSystem, Super i_sb, int i_ino, InodeData i_data, BufferCache bufferCache, InodeCache inodeCache, Clock clock) {
	this.fileSystem = fileSystem;
	this.i_sb = i_sb;
	this.i_ino = i_ino;
	this.i_data = i_data;
	this.bufferCache = bufferCache;
	this.inodeCache = inodeCache;
	i_list = null; i_ishashed = false;
	i_count = 1;
	parent = this;
	i_released = false;

	blocksize = i_sb.s_blocksize;
	this.clock = clock;

	int addr_per_block_bits_tmp = 0;
	int addr_tmp = i_sb.s_blocksize >> 2; // Laenge eines Int

	// init stuff (this was originally done in getblk, I moved it out of the fast path) mg 
	addr_per_block = addr_tmp; // Laenge eines Int

	while (addr_tmp != 1 && addr_tmp > 0) {
	    addr_tmp = addr_tmp >> 1;
	    addr_per_block_bits_tmp++;
	}
	addr = addr_tmp;
	addr_per_block_bits = addr_per_block_bits_tmp;


	maxblock = 12 + addr_per_block + (1 << (addr_per_block_bits << 1)) + ((1 << (addr_per_block_bits << 1)) << addr_per_block_bits);

	global_bd_slot = new InodeBlockData();

    }



    /**
     * Indicate that inode was changed and has
     * to be written to disk.
     */
    final public void setDirty(boolean value) {
	i_dirty = value;
	if (i_dirty)
	    inodeCache.markInodeDirty(this);
    }

    final public void incUseCount() {
	i_count++;
	//Debug.out.println("inc: i_ino = " + i_ino + ", i_count = " + i_count);
	//i_released = false;
    }

    final public void decUseCount() {
	inodeCache.iput(this);
	//Debug.out.println("dec: i_ino = " + i_ino + ", i_count = " + i_count);
	//if (i_count == 0)
	//    i_released = true;
    }


    final public int available()  throws NotExistException {
        //if (i_released)
        //throw new NotExistException();
	return i_sb.available();
    }

    final public int lastModified() throws NotExistException {
	//if (i_released)
	//throw new NotExistException();
	return i_data.i_mtime();
    }

    final public int lastAccessed() throws NotExistException {
	//if (i_released)
	//throw new NotExistException();
	return i_data.i_atime();
    }

    final public int lastChanged() throws NotExistException {
	//if (i_released)
	//throw new NotExistException();
	return i_data.i_ctime();
    }

    final public void setLastModified(int time) throws NotExistException {
	i_data.i_mtime(time);
    }
    final public void setLastAccessed(int time) throws NotExistException {
	i_data.i_atime(time);
    }


    final public int getLength() throws NotExistException {
	//if (i_released)
	//throw new NotExistException();
	return i_data.i_size();
    }

    final public int i_nlinks()  throws NotExistException {
	//if (i_released)
	//throw new NotExistException();
	return i_data.i_links_count();
    }

    final public  void writeInode()  throws InodeIOException, NotExistException {
	updateInode(false);
    }

    final public  void syncInode()  throws InodeIOException, NotExistException {
        updateInode(true);
    }

    public  void syncFile() throws InodeIOException {
	inodeCache.writeInodeNow(this);
	i_sb.writeSuper();
    }

    private  void updateInode(boolean do_sync)  throws InodeIOException, NotExistException {
	BufferHead bh = i_data.bh;
	//Debug.out.println("updateInode: " + bh.b_block);
	if (i_released)
	    throw new NotExistException();
	if (do_sync) {
	    bufferCache.bwrite(bh);
	} else {
	    bufferCache.bdwrite(bh);
	}
    }

    public  void putInode()  throws NotExistException {
	if (i_released) throw new NotExistException();
	bufferCache.brelse(i_data.bh);
    }

    // Wird in iput() aufgerufen, falls i_data.i_links_count() == 0
    public  void deleteInode() throws InodeIOException, NotExistException {
	//Debug.out.println("deleteInode");
	if (i_released)
	    throw new NotExistException();
	i_data.i_dtime(clock.getTimeInMillis());
	setDirty(true);
	updateInode(false);
	i_data.i_size(0);
	try {
	    if (i_data.i_blocks() > 0)
		truncate();
	    i_sb.freeInode(this);
	} catch (BufferIOException e) {
	    throw new InodeIOException();
	}
    }

    final protected  boolean permission(int mask) {
        int mode = i_data.i_mode();

        if (((mask & S_IWOTH) > 0) && i_sb.isReadonly())
	    return false;  // Dateisystem read-only
        if ((mode & mask & S_IRWXO) == mask)
	    return true;
	// UID und GID pruefen
        return true; // return false;
    }

    protected  boolean checkSticky() {
        if ((i_data.i_mode() & S_ISVTX) == 0)
	    return true;
	// inode.i_data.i_uid und dir.i_data.i_uid pruefen
	return true;
    }


    /**
     * Liest den Inhalt des Blocks mit der Nummer <code>blocknr</code>. Der Block wird mit <code>getBlk</code> angefordert
     * (vergleichbar mit der gleichnamigen Funktion im BufferCache).
     *
     * @see #getBlk(int block, boolean create)
     * @see fs.BufferCache#bread(int device, int block, int size)
     * @param block  die Nummer des gew&uuml;nschten Blocks
     * @param create Falls <code>true</code>, wird der Block erzeugt, falls er noch nicht vorhanden ist (jenseits der
     *               Inodegr&ouml;o&szlig;e liegt.
     */
    public  BufferHead bread(int blocknr, boolean create) {
	BufferHead bh;
        
        if (dumpDetails) Debug.out.println("bread: " + blocknr + "/" + i_data.i_blocks());
        bh = getBlk(blocknr, create);
        if (bh == null)
	    return null;

	bufferCache.updateBuffer(bh);
	return bh;
    }

    private  void checkBlockEmpty(BufferHead bh, int ipos, InodeBlockData p, BufferHead ind_bh)
	throws BufferIOException {
        int addr_per_block = i_sb.s_blocksize / 4;
	InodeBlockData iblockdata = new InodeBlockData(bh, ipos);
        int i;
	
        /* Make sure both buffers are unlocked */
	bh.waitUntilUnlocked();
	ind_bh.waitUntilUnlocked();
	
        for (i = 0; i < addr_per_block; i++) {
	    iblockdata.init(i);
	    if (iblockdata.bd_entry() > 0) {
		bufferCache.brelse(bh);
		return;
	    }
	}

	int tmp;
	if (p == null) {
	    tmp = i_data.i_block(ipos);
	    i_data.i_block(ipos, 0);
	} else {
	    tmp = p.bd_entry();
	    p.bd_entry(0);
	}
	i_data.i_blocks(i_data.i_blocks() - i_sb.s_blocksize / 512);
	setDirty(true);

	//Wir "vergessen" den Buffer und setzen ind_bh auf dirty
	bufferCache.bforget(bh);
	if (ind_bh != null)
	    ind_bh.markDirty();
	i_sb.freeBlocks(tmp, 1);
    }

    private  BufferHead inodeGetBlk(int nr, boolean create, int new_block) {
	return inodeGetBlk(nr, create, new_block, true);
    }
  /**
   * Allocate new block for this inode.
   * This method should only be called to allocate direct blocks!
   */
    private  BufferHead inodeGetBlk(int nr, boolean create, int new_block, boolean clear) {
        int p;
        int goal = 0;
        BufferHead result;

	if (dumpDetails) 
	    Debug.out.println("inodeGetBlk(" + nr + ", " + create + "):  " 
			      + i_data.i_blocks() + " " + i_data.bh.getBlock() + " "
			      + i_data.i_version());

	int blocknr;
	if ((blocknr = i_data.i_block(nr)) > 0) {
	    return bufferCache.getblk(blocknr);
	}

	if (create == false)
	    throw new Error("not allowed to create");
	
	if (dumpBlockAlloc) Debug.out.println("inodeGetBlk(" + nr + "):  " + i_data.i_blocks() + " " + i_data.bh.getBlock() + " --> alloc new block" );
	if (i_next_alloc_block == new_block)
	    goal = i_next_alloc_goal;
	
	if (dumpBlockAlloc) Debug.out.println("hint = " + goal + ",");
	
	if (goal == 0) {
	    for (p = nr - 1; p >= 0; p--) {
		if (i_data.i_block(p) > 0) {
		    goal = i_data.i_block(p);
		    break; // inneres Break
		}
	    }
	    if (goal == 0)
		goal = i_sb.groupStart(i_ino);
	}
	
	if (dumpBlockAlloc) Debug.out.println("goal = " + goal);
	
	try {
	    p = i_sb.newBlock(goal,clear);
	} catch (BufferIOException e) {
	    throw new Error();
	}
	if (p == 0)
	    throw new Error();

	result = bufferCache.getblk(p);
	if (dumpBlockAlloc) {
	    if (nr >= 12) {
		Debug.out.println("inodeGetBlk: getting indirect ("+nr+")block " + result.getBlock());	
		//throw new Error();
	    }
	}
	i_data.i_block(nr, p);
	
        i_next_alloc_block = new_block;
        i_next_alloc_goal = p;
        i_data.i_ctime(clock.getTimeInMillis());
        i_data.i_blocks(i_data.i_blocks() + i_sb.s_blocksize/512);
        //if (i_sb.isSynchronous())
	//    try {
	//	syncInode();
	//    } catch (InodeIOException e) { }
        //else
	setDirty(true);
        return result;
    }

  /**
   * Get data block.
   * Also used to indirect block.
   * Increment inode's block counter (i_blocks) by blocksize/512
   * @param bh block that contains list of block numbers of indirect blocks
   * @param nr index of block in this indirect block list
   * @param create create block if it does not exist
   * @param blocksize always 1024
   * @param new_block
   */
    private  BufferHead blockGetBlk(BufferHead bh, int nr, boolean create, int blocksize, int new_block) {
        int tmp, goal = 0;
        int p;
        BufferHead result;
        int limit;
	InodeBlockData bd_slot;

        bufferCache.updateBuffer(bh);
	
	/* remove new for speed up
	   bd_slot = new InodeBlockData(bh, nr*4);
	*/
	bd_slot = global_bd_slot;
	bd_slot.bd_init(bh,nr*4);

	if (trace) Debug.out.println("blockGetBlk: " + bh.getBlock());

        tmp = bd_slot.bd_entry();
        if (tmp > 0) {
	  // block already exists
	    result = bufferCache.getblk(tmp);
	    bufferCache.brelse(bh);
	    return result;
        }

        if (create == false) {
	    bufferCache.brelse(bh);
	    throw new Error("must alloc new block, but create=false"); //return null;
        }

	if (dumpBlockAlloc) Debug.out.println("blockGetBlk: " + bh.getBlock()+", alloc new block");

        if (i_next_alloc_block == new_block)
	    goal = i_next_alloc_goal;
        if (goal == 0) {
	    for (tmp = nr - 1; tmp >= 0; tmp--) {
		bd_slot.init(tmp*4);
		if (bd_slot.bd_entry() > 0) {
		    goal = bd_slot.bd_entry();
		    break;
		}
	    }
	    if (goal == 0)
		goal = bh.getBlock();  // ???
        }
	try {
	    tmp = i_sb.newBlock(goal);
	} catch (BufferIOException e) {
	    bufferCache.brelse(bh);
	    throw new Error("BufferIOException");
	}
        if (tmp == 0) {
	    bufferCache.brelse(bh);
	    throw new Error("no new block created");
        }
        result = bufferCache.getblk(tmp);
	if (dumpBlockAlloc) Debug.out.println("blockgetblk: " + bh.getBlock() + " setting " + (nr*4) + " to " + tmp);
	bd_slot.init(nr*4);
	bd_slot.bd_entry(tmp);
        i_data.i_ctime(clock.getTimeInMillis());
	if ((i_data.i_blocks() + i_sb.s_blocksize/512) < 0) // Ueberlauf
	    throw new Error("Overflow");
        i_data.i_blocks(i_data.i_blocks() + i_sb.s_blocksize/512);
        setDirty(true);
        i_next_alloc_block = new_block;
        i_next_alloc_goal = tmp;
        bufferCache.bwrite(bh);
        return result;
    }

    final  BufferHead getBlk(int block, boolean create) {
	return getBlk(block, create, true);
    }
    /**
     * Liefert den Block mit der Nummer <code>block</code> innerhalb der Inode.
     * Die Zaehlung beginnt bei 0. Fuer die
     * ersten zw&ouml;lf Bl&ouml;cke wird auf die direkten Bl&ouml;cke zur&uuml;ckgegriffen,
     * f&uuml;r die n&auml;chsten
     * Bl&ouml;cke auf die indirekten, usw. Der Schalter <code>create</code> 
     * veranla&szlig;t die Methode, den Block zu erzeugen
     * (allozieren), falls er die Inodegr&ouml;o&szlig;e &uuml;berschreitet.
     * Bei einer Blockgr&ouml;&szlig;e von 1024 Byte
     * verweisen die direkten Bl&ouml;cke auf Block 0 bis 11, die indirekten auf 12 bis 267,
     * die doppelt indirekten auf 268 bis
     * 65803 und die dreifach indirekten auf Block 65804 bis 16843019.
     *
     * @param block  die Nummer des gew&uuml;nschten Blocks
     * @param create Falls <code>true</code>, wird der Block erzeugt, wenn er noch nicht vorhanden ist (jenseits der Inodegr&ouml;&szlig;e liegt).
     * @param clear clear buffer contents (not necessary when buffer is written immediately)
     */
     final BufferHead getBlk(int block, boolean create, boolean clear) {
	BufferHead bh;
	int b;

	
	if (trace) Debug.out.println("getBlk():   addr_per_block: " + addr_per_block + ", bits: " + addr_per_block_bits);

	if (safety) {
	  if (block < 0) {
	    /*System.out*/Debug.out.println("Inode.getBlk(): block < 0");
	    throw new Error();
	  }
	  
	  if (block > maxblock) {
	    Debug.out.println("Inode.getBlk(): block > big");
	    throw new Error();
	  }
	}

        /* Falls die Bloecke sequentiell angefordert werden, setzen wir
	   next_alloc_block auch sequentiell hoch, damit alle angeforderten
	   indirekten Bloecke und Datenbloecke in derselben Gruppe liegen */
	if (trace) Debug.out.println("block " + block + ", next " + i_next_alloc_block + ", goal " + i_next_alloc_goal);

        if (block == i_next_alloc_block + 1) {
	    i_next_alloc_block++;
	    i_next_alloc_goal++;
        }

        if (block < 12)
	    return inodeGetBlk(block, create, block);
	// indirect block
        b = block;
        block -= 12;
        if (block < addr_per_block) {
	    bh = inodeGetBlk(12, create, b, clear);
	    if (! bh.isUsedOnlyByMe()) {
		Debug.out.println("Buffer not excl. used??: "+bh.getBlock());
		throw new Error();
	    }
	    BufferHead bh_res = blockGetBlk(bh, block, create, i_sb.s_blocksize, b);
	    //Debug.out.println("Block count (i_blocks) of inode "+i_ino+" now: "+i_data.i_blocks()); 
	    if (bh.isUsed()) {
		Debug.out.println("Buffer not released: "+bh.getBlock());
		throw new Error();
	    }
	    return bh_res;
        }
        block -= addr_per_block;
        if (block < (1 << (addr_per_block_bits << 1))) {
	    bh = inodeGetBlk(13, create, b);
	    bh = blockGetBlk(bh, block >> addr_per_block_bits, create, i_sb.s_blocksize, b);
	    return blockGetBlk(bh, block & (addr_per_block - 1), create, i_sb.s_blocksize, b);
        }
        block -= (1 << (addr_per_block_bits << 1));
        bh = inodeGetBlk(14, create, b);
        bh = blockGetBlk(bh, block >> (addr_per_block_bits << 1), create, i_sb.s_blocksize, b);
        bh = blockGetBlk(bh, (block >> addr_per_block_bits) & (addr_per_block - 1), create, i_sb.s_blocksize, b);
        return blockGetBlk(bh, block & (addr_per_block - 1), create, i_sb.s_blocksize, b);
    }

    private  void truncDirect() throws BufferIOException {
	BufferHead bh;
        int block_to_free = 0, free_count = 0;
        int blocks = i_sb.s_blocksize / 512;
        int direct_block = (i_data.i_size() + i_sb.s_blocksize - 1) / i_sb.s_blocksize;

	//Debug.out.println("truncDirekt()");

        for (int i = direct_block; i < 12; i++) {
	    int tmp = i_data.i_block(i);
	    if (tmp == 0)
		continue;

	    bh = bufferCache.findBuffer(tmp);
	    if (bh != null)
		bh.waitUntilUnlocked();

	    i_data.i_block(i, 0);
	    i_data.i_blocks(i_data.i_blocks() - blocks);
	    setDirty(true);
	    bufferCache.bforget(bh);

	    // wir sammeln freizugebenden Bloecke, die hintereinander liegen
	    if (free_count == 0) {
		block_to_free = tmp;
		free_count = 1;
	    } else if (block_to_free == tmp - free_count)
		free_count++;
	    else {
		i_sb.freeBlocks(block_to_free, free_count);
		block_to_free = tmp;
		free_count = 1;
	    }
        }
        if (free_count > 0)
	    i_sb.freeBlocks(block_to_free, free_count);
    }

    private  void truncIndirect(int offset, int ipos, InodeBlockData p, BufferHead dind_bh)
	throws BufferIOException {
        BufferHead ind_bh;
	InodeBlockData iblockdata;
        int tmp;
	int block_to_free = 0, free_count = 0;
        int indirect_block, addr_per_block, blocks;

	//Debug.out.println("truncIndirekt()");

	if (p == null)
	    tmp = i_data.i_block(ipos);
	else
	    tmp = p.bd_entry();
        if (tmp == 0)
	    return;
        ind_bh = bufferCache.bread(tmp);
	//Debug.out.println("truncIndirekt: fetching indirect block " + ind_bh.b_block);

        // Im Falle eines Lesefehlers Eintrag auf 0 setzen
        if (ind_bh == null) {
	    /*System.out*/Debug.out.println("truncIndirect(): Read failure, inode = " + i_ino + ", block = " + tmp);
	    if (p == null) {
		i_data.i_block(ipos, 0);
	    } else {
		p.bd_entry(0);
	    }
	    if (dind_bh != null)
		dind_bh.markDirty();
	    else
		setDirty(true);
	    return;
        }

        blocks = i_sb.s_blocksize / 512;
        addr_per_block = i_sb.s_blocksize / 4;
        indirect_block = (i_data.i_size() + i_sb.s_blocksize-1)/i_sb.s_blocksize - offset;

        if (indirect_block < 0)
	    indirect_block = 0;

	iblockdata = new InodeBlockData(ind_bh, indirect_block);
	//Debug.out.println("truncIndirekt: Schleife von " + indirect_block + " bis " + addr_per_block);
        for (int i = indirect_block; i < addr_per_block; i++) {
	    BufferHead bh;
	    iblockdata.init(i*4);
	    int ind;

	    ind_bh.waitOn();
	    ind = iblockdata.bd_entry();
	    if (ind == 0)
		continue;
	    // Wir rufen findBuffer (nicht getblk) direkt auf, um nicht zu blockieren
	    bh = bufferCache.findBuffer(ind);
	    if (bh != null) {
		bh.waitOn();
		//if (bh.b_count != 1 || bh.isLocked()) {
		//    bufferCache.brelse(bh);
		//    retry = true;
		//    continue;
		//}
	    }
	    
	    iblockdata.bd_entry(0);
	    i_data.i_blocks(i_data.i_blocks() - blocks);
	    setDirty(true);
	    bufferCache.bforget(bh);
	    ind_bh.markDirty();

	    // wir sammeln freizugebenden Bloecke, die hintereinander liegen
	    if (free_count == 0) {
		block_to_free = ind;
		free_count = 1;
	    } else if (block_to_free == ind - free_count)
		free_count++;
	    else {
		i_sb.freeBlocks(block_to_free, free_count);
		block_to_free = ind;
		free_count = 1;
	    }
        }
        if (free_count > 0)
	    i_sb.freeBlocks(block_to_free, free_count);

	// wir ueberpruefen den Block und geben den Buffer frei, falls er leer ist
	checkBlockEmpty(ind_bh, ipos, p, dind_bh);
    }
	
    private  void truncDindirect(int offset, int ipos, InodeBlockData p, BufferHead tind_bh)
	throws BufferIOException {
        BufferHead dind_bh;
	InodeBlockData iblockdata;
        int tmp;
        int dindirect_block, addr_per_block;

	//Debug.out.println("truncDindirekt()");

	if (p == null)
	    tmp = i_data.i_block(ipos);
	else
	    tmp = p.bd_entry();
        if (tmp == 0)
	    return;
        dind_bh = bufferCache.bread(tmp);

        // Im Falle eines Lesefehlers Eintrag auf 0 setzen
        if (dind_bh == null) {
	    /*System.out*/Debug.out.println("truncDindirect(): Read failure, inode = " + i_ino + ", block = " + tmp);
	    if (p == null) {
		i_data.i_block(ipos, 0);
	    } else {
		p.bd_entry(0);
	    }
	    if (tind_bh != null)
		tind_bh.markDirty();
	    else
		setDirty(true);
	    return;
        }

        addr_per_block = i_sb.s_blocksize / 4;
        dindirect_block = ((i_data.i_size() + i_sb.s_blocksize-1)/i_sb.s_blocksize - offset)/addr_per_block;

        if (dindirect_block < 0)
	    dindirect_block = 0;

	iblockdata = new InodeBlockData(dind_bh, dindirect_block);
	for (int i = dindirect_block; i < addr_per_block; i++) {
	    iblockdata.init(i*4);

	    if (iblockdata.bd_entry() > 0)
		truncIndirect(offset + (i * addr_per_block), 0, iblockdata, dind_bh);
        }

        // wir ueberpruefen den Block und geben den Buffer frei, falls er leer ist
	checkBlockEmpty(dind_bh, ipos, p, tind_bh);
    }

    private  void truncTindirect() throws BufferIOException {
        BufferHead tind_bh;
	InodeBlockData iblockdata;
        int tmp;
        int tindirect_block, addr_per_block, offset;

	//Debug.out.println("truncTindirekt()");

        tmp = i_data.i_block(14); // TIND_BLOCK
        if (tmp == 0)
	    return;

        tind_bh = bufferCache.bread(tmp);
	// Im Falle eines Lesefehlers Eintrag auf 0 setzen
        if (tind_bh == null) {
	    /*System.out*/Debug.out.println("truncTindirect(): Read failure, inode = " + i_ino + ", block = " + tmp);
	    i_data.i_block(14, 0);
	    setDirty(true);
	    return;
        }

        addr_per_block = i_sb.s_blocksize / 4;
	offset = 12 + addr_per_block + (addr_per_block * addr_per_block);
        tindirect_block = ((i_data.i_size() + i_sb.s_blocksize-1)/i_sb.s_blocksize - offset)/
	    (addr_per_block*addr_per_block);

        if (tindirect_block < 0)
	    tindirect_block = 0;

	iblockdata = new InodeBlockData(tind_bh, tindirect_block);
	for (int i = tindirect_block; i < addr_per_block; i++) {
	    iblockdata.init(i*4);

	    if (iblockdata.bd_entry() > 0) {
		truncDindirect(offset + (i * addr_per_block * addr_per_block), 0, iblockdata, tind_bh);
	    }
        }

        // wir ueberpruefen den Block und geben den Buffer frei, falls er leer ist
        checkBlockEmpty(tind_bh, 14, null, null);
    }

    /**
     * Gibt den Inhalt (die belegten Bl&ouml;cke) der Inode frei. Die Variable <code>i_data.i_size</code> mu&szlig; dazu vorher
     * auf den gew&uuml;nschten Wert gesetzt werden (die Gr&ouml;&szlig;e der Datei nach dem Verkleinern), da dieser Wert von
     * <code>truncate</code> zum Ermitteln der freizugebenden Bl&ouml;cke verwendet wird. Es werden zuerst die direkten, danach
     * die indirekten, die doppelt-indirekten und schlie&szlig;lich die dreifach-indirekten Bl&ouml;cke freigegeben. Falls der
     * letzte verwendete Block nicht vollst&auml;ndig belegt ist, wird der Rest dieses Blocks auf 0 gesetzt.
     *
     * @exception BufferIOException Falls bei der Ein-/Ausgabe ein Fehler auftritt.
     */
    private  void truncate() throws BufferIOException {
	truncDirect();
	truncIndirect(12, 12, null, null);
	truncDindirect((12 + i_sb.s_blocksize / 4), 13, null, null);
	truncTindirect();

	// falls der letzte verwendete Block nicht vollstaendig belegt wird, den Rest des
	// Blocks auf 0 setzen.
        int offset = i_data.i_size() & (i_sb.s_blocksize - 1);
        if (offset > 0) {
	    BufferHead bh = bread(i_data.i_size() / i_sb.s_blocksize, false);
	    if (bh != null) {
		bh.clear(offset, i_sb.s_blocksize - offset);
		bh.markClean();
		bufferCache.brelse(bh);
	    }
        }
	int times = clock.getTimeInMillis();
        i_data.i_mtime(times);
	i_data.i_ctime(times);
        setDirty(true);
    }

    public int getIdentifier() {
	return i_ino;
    }

    public int getVersion() {
	return i_data.i_version();
    }
    public jx.fs.FileSystem getFileSystem() { 
	return fileSystem;
    }

    public jx.fs.StatFS getStatFS() {
	jx.fs.StatFS sfs = new jx.fs.StatFS();
	sfs.bsize = i_sb.s_blocksize;
	sfs.blocks = i_sb.getNumberOfBlocks();
	sfs.bfree = i_sb.getNumberOfFreeBlocks();
	//TODO: Inhalt von sfs.bavail: the number of blocks available to non-privileged users??
	//sfs.bavail = ??
	  
	return sfs;
    }
}
