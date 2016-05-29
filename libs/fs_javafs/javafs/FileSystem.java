package javafs;

import jx.zero.Debug;
import jx.zero.Service;
import jx.zero.Clock;
import jx.fs.DeviceNaming;
import jx.bio.BlockIO;
import jx.fs.Inode;
import jx.fs.*;

import jx.fs.buffercache.*;

/**
 * Instances of this class are used by VFS to operate on a jafafs as a whole.
 * This includes creating, repairing, mounting, and unmounting the file system. 
 */
public class FileSystem implements jx.fs.FileSystem, Service {
    private Super       root;
    private InodeImpl   rootInode;
    private boolean     inited = false;
    private BufferCache bufferCache;
    private InodeCache  inodeCache;
    private Tools       tools;
    private Clock       clock;
    private Integer     deviceID;

    public FileSystem() {
	inodeCache = new InodeCache();
	deviceID = new Integer(1);
    }

    public void init(BlockIO blockDevice, BufferCache bufferCache, Clock clock) {
	if (inited) throw new Error("FS already initialized");
	this.clock = clock;
	this.bufferCache = bufferCache;
	tools = new Tools(blockDevice, bufferCache, inodeCache, clock);
    }

    public void init(boolean read_only) {
	if (inited)
	    return;
	
	
	inited = true;
	Debug.out.println("INIT JavaFS");
	root = new Super(this, read_only, bufferCache, inodeCache, clock);
	root.dump();
	rootInode = root.getRootInode();
    }

    public String name() {
	return new String("JavaFS");
    }

    public jx.fs.Inode getRootInode() {
	if (! inited) throw new Error("FS not initialized");
	rootInode.incUseCount();
	return (jx.fs.Inode)rootInode;
    }

    public void release() {
	if (! inited) throw new Error("FS not initialized");
	Debug.out.println("releasing filesystem ");
	rootInode.decUseCount();
	root.writeSuper();
	root.putSuper();
	// direntrycache.invalidateEntries(device); -> jetzt im VFS!
	/*
	inodeCache.syncInodes();
	bufferCache.syncDevice(false);
	*/
	inodeCache.syncInodes();
	bufferCache.syncDevice(true); //wait
	inodeCache.invalidateInodes();
	bufferCache.flushCache();
	//bufferCache.stopDemon();
    }

    public void build(String name, int blocksize) {
	inited = false;
	tools.makeFS(name, blocksize);
	init(false);
    }

    public void check() {
	if (! inited) throw new Error("FS not initialized");
	tools.checkFS(null); /* pass answermachine */
    }

    public void printStatistics() {
	if (! inited) throw new Error("FS not initialized");
	inodeCache.showInodes();
	bufferCache.showBuffers();

    }

    public jx.fs.Inode getInode(int inodeNumber) throws FSException  {
	DirEntryData de_data;
	InodeImpl inode;
	
	//	System.out.println("getInode aufgerufen!");
	inode = (InodeImpl)inodeCache.iget(inodeNumber);
	if (inode != null) {
	    //bufferCache.brelse(de_data.bh);
	    //System.out.println("im Cache");
	    return inode;
	}
	//System.out.println("nicht im Cache");
	
	InodeData i_data = root.getInodeData(inodeNumber);
	
	// Falls die Inodenummer nicht existiert, wird auch kein InodeData-Objekt 
	// zurückgeliefert.
	if (i_data == null) throw new NotExistException();

	if (i_data.i_size() < 0) { // Ueberlauf
	    Debug.out.println("getInode(): Inode zu gross (Ueberlauf)");
	    throw new InodeIOException();
	}

	
	if ((i_data.i_mode() & InodeImpl.S_IFMT) == InodeImpl.S_IFREG) {
	    //Debug.out.println("getInode(): erzeuge FileInode");
	    inode = new FileInode(this, root, inodeNumber, i_data, bufferCache, inodeCache, clock);
	}
	if ((i_data.i_mode() & InodeImpl.S_IFMT) == InodeImpl.S_IFDIR) {
	    //Debug.out.println("getInode(): erzeuge DirInode");
	    inode = new DirInode(this, root, inodeNumber, i_data, bufferCache, inodeCache, clock);
	}
	if ((i_data.i_mode() & InodeImpl.S_IFMT) == InodeImpl.S_IFLNK) {
	    //Debug.out.println("getInode(): erzeuge SymlinkInode");
	    inode = new SymlinkInode(this, root, inodeNumber, i_data, bufferCache, inodeCache, clock);
	}
	//bufferCache.brelse(de_data.bh);
	
	if (inode == null) {
	    /*System.out*/Debug.out.println("getInode(): invalid inode type: " + (i_data.i_mode() & InodeImpl.S_IFMT));
	    throw new InodeIOException();
	}
	//inode.setParent(this);
	inodeCache.addInode(inode);
	return inode;
    }
    

    public Integer getDeviceID() {
	return deviceID; /* FIXME */
    }

    public Tools getTools() {
	return tools;
    }


}
