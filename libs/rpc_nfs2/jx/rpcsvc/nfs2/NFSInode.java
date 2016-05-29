package jx.rpcsvc.nfs2;

import jx.rpc.*;
import java.util.*;
import jx.fs.*;
import jx.zero.*;

import jx.rpcsvc.mount1.MountProc;
import jx.rpcsvc.mount1.FHStatus;
import jx.rpcsvc.mount1.FHStatusOK;
import jx.net.IPAddress;

public class NFSInode extends InodeImpl {
    static final boolean dumpAll = true;
    private FAttr attr;
    private FHandle fileHandle;
    private NFSProc nfs;
    private String myName;

    private Vector names,children;
    private RPC rpc;

    private NFSInode(NFSProc nfs, Inode parent, FHandle handle) {
	super(parent);
	this.nfs = nfs;
	this.fileHandle = handle;
    }

    /** 
     * create root-nfs inode
     * init with filename
     * store filename but do not query nfs for information about this node
     */
    private NFSInode(NFSProc nfs, Inode parent, String name) {
	super(parent);
	this.nfs = nfs;
	myName = name;
    }

    private NFSInode(RPC rpc, NFSProc nfs, Inode parent, FHandle handle, FAttr attr) {
	super(parent);
	this.rpc = rpc;
	this.nfs = nfs;
	this.fileHandle = handle;
	this.attr = attr;
    }


    private NFSInode(RPC rpc, IPAddress host, String path) throws NFSException {
	super(null);
	this.rpc = rpc;
	MountProc mount = (MountProc) InitialNaming.getInitialNaming().lookup("Mounter"); //new MountProc_Stub(host.getHostName(),rpc);
	FHStatus m = mount.mnt(new DirPath(path));
	if (m.status != FHStatus.SWITCH_FHStatusOK) {
	    throw new NFSException("Could not mount " + m.status);
	    }
	fileHandle = ((FHStatusOK)m).directory;
	
	nfs = new NFSProc_Stub(rpc, host);
	Auth a = new AuthUnix(rpc.getLocalAddress().getHostName(), 10412, 10430, new int[] {10424});
	Auth c = new AuthNone();
	((NFSProc_Stub)nfs).setAuth(a,c);
    }


    private NFSInode(Inode parent) throws NFSException {
	super(parent);
    }

    private void initFromNFS(String fileName)throws NFSException {
	if (! ( parent instanceof NFSInode)) {
	    throw new NFSException("non-nfs parent, cannot init");
	}
	FHandle ph = ((NFSInode)parent).getFileHandle();
	DirOpRes res = nfs.lookup(ph, new Name(fileName));
	if (! (res instanceof DirOpResOK)) {
	    throw new NFSException("lookupError: "+fileName);
	}

	fileHandle = ((DirOpResOK)res).file;
	attr = ((DirOpResOK)res).attributes;
    }

    private void initAttrFromNFS() throws NFSException {
	AttrStat a = nfs.getattr(fileHandle);
	if (! (a instanceof AttrStatOK)) {
	    throw new NFSException("getattr Error ");
	}
	attr = ((AttrStatOK)a).attributes;
    }

    private FHandle getFileHandle() throws NFSException {
	if (fileHandle==null) {
	    if (myName != null) {
		initFromNFS(myName);
		return fileHandle;
	    }
	    throw new NFSException("no fh ");
	}
	return fileHandle;
    }

    /*
      public Vector readdir() {
      if (children != null) return children;
      synchronized(this) {
      if (children != null) return children;
      try {
      children = new Vector();
      names = new Vector();
      int count = 1024;
      NFSCookie cookie = new NFSCookie();
      cookie.data = 0;
      ReadDirRes res = nfs.readdir(getFileHandle(), cookie, count);
      if (! (res instanceof ReadDirResOK)) {
      return null;
      }
      Entries entries = ((ReadDirResOK)res).entries;
      for(Entry e = entries.node; e != null;) {
      if (dumpAll) {
      System.out.println("fileid: " + e.fileid);
      System.out.println("name: " + e.name.data);
      System.out.println("cookie: " + e.cookie.data);
      }
      if (! e.name.data.equals(".")
      && ! e.name.data.equals("..")) {
      children.addElement(new NFSInode(nfs, this, e.name.data));
      names.addElement(e.name.data);
      }
      Entries ee = e.next;
      if (ee==null) break;
      e = ee.node;
      } 
      } catch(Exception e) {
      children = null;
      names = null;
      return null;
      }
      }
      return children;
      }
    */

    public String[] readdirNames() {
	throw new Error("String[] statt Vector zurueckgeben!");
	/*
	if (names != null) return names;
	synchronized(this) {
	    try {
		if (names != null) return names;
		names = new Vector();
		children = new Vector();
		int count = 1024;
		NFSCookie cookie = new NFSCookie();
		cookie.data = 0;
		ReadDirRes res = nfs.readdir(getFileHandle(), cookie, count);
		if (! (res instanceof ReadDirResOK)) {
		    return null;
		}
		Entries entries = ((ReadDirResOK)res).entries;
		for(Entry e = entries.node; e != null;) {
		    if (dumpAll){
			Debug.out.println("fileid: " + e.fileid);
			Debug.out.println("name: " + e.name.data);
			Debug.out.println("cookie: " + e.cookie.data);
		    }
		    if (! e.name.data.equals(".")
			&& ! e.name.data.equals("..")) {
			children.addElement(new NFSInode(nfs, this, e.name.data));
			names.addElement(e.name.data);
		    }
		    Entries ee = e.next;
		    if (ee==null) break;
		    e = ee.node;
		} 
	    } catch(Exception e) {
		children = null;
		names = null;
		return null;
	    }
	}
	return names;
	*/
    }

    private FAttr getAttr() throws NFSException {
	if (attr==null) {
	    if (fileHandle==null) {
		if (myName != null) {
		    initFromNFS(myName);
		} else {
		    throw new NFSException("no name and no handle?");
		}
	    }
	    initAttrFromNFS();
	}
	return attr;
    }

    public boolean isDirectory() {
	try {
	    return getAttr().type.ftype == FType.ftype_NFDIR;
	} catch(NFSException e) {
	    e.printStackTrace();
	    return false;
	}
    }
    public boolean isReadable() { 
	try {
	    return (getAttr().mode & FAttr.MODE_ROWNER) != 0;
	} catch(NFSException e) {
	    e.printStackTrace();
	    return false;
	}
    }
  
    public boolean isWritable() {
	try{
	    return (getAttr().mode & FAttr.MODE_WOWNER) != 0;
	} catch(NFSException e) {
	    e.printStackTrace();
	    return false;
	}
    }

    public boolean isExecutable() {
	try{
	    return (getAttr().mode & FAttr.MODE_XOWNER) != 0;
	} catch(NFSException e) {
	    e.printStackTrace();
	    return false;
	}
    }

    public boolean isFile() {
	try {
	    return getAttr().type.ftype == FType.ftype_NFREG;
	} catch(NFSException e) {
	    e.printStackTrace();
	    return false;
	}
    }
    public long mtime() {
	try {
	    return getAttr().mtime.seconds; 
	} catch(NFSException e) {
	    e.printStackTrace();
	    return -1;
	}
    }


    public synchronized Inode mknode(String name) {
	SAttr attributes = new SAttr(FAttr.MODE_RWOWNER);
	DirOpRes  res =   nfs.create(fileHandle, new Name(name), attributes);
	if (! (res instanceof DirOpResOK)) {
	    return null;
	}
	FHandle fh  = ((DirOpResOK)res).file;
	FAttr a  = ((DirOpResOK)res).attributes;
	NFSInode inode = new NFSInode(rpc, nfs, this, fh, a);
	if (children != null) {
	    children.addElement(inode);
	}
	if (names != null) {
	    names.addElement(name);
	}
	return inode;
    }

    public Inode mkdir(String name) {
	// send NFS mkdir request
	return null;
    }

    public int getLength() { 
	try{
	    return getAttr().size;
	} catch(NFSException e) {
	    e.printStackTrace();
	    return -1;
	}
    }


    public int read(long fp, byte[] buf, int ofs, int len) throws NFSException {
	int n;
	int offset;
	int chunk_size;
	int bytes_read = 0;
	final int NFS_READ_SIZE	= 1024;

	getFileHandle();

	offset = (int)fp;
	int size = len;
	while (size > 0) {
	    chunk_size = Math.min(NFS_READ_SIZE, size);
	    ReadRes res = nfs.read(fileHandle, offset, chunk_size, 0);
	    if (! (res instanceof ReadResOK)) {
		System.out.println("Read error " + res);
		System.exit(1);
	    }
	    NFSData data = ((ReadResOK)res).data;
	    n = data.data.length;
	    if (n < 0) {
		System.out.println("Unable to read text");
		System.exit(1);
	    }
	    if (n == 0)
		break;		/* hit eof */

	    System.arraycopy(data.data, 0, buf, ofs, n);

	    offset += n;
	    ofs += n;
	    size -= n;
	    bytes_read += n; 
	}
	return bytes_read;
    }

    public int write(long fp, byte[] buf, int ofs, int len) throws NFSException {
	int n;
	int offset;
	int chunk_size;
	int bytes_written = 0;
	final int NFS_WRITE_SIZE	= 1024;

	getFileHandle();

	offset = (int)fp;
	int size = len;
	NFSData data = new NFSData();
	while (size > 0) {
	    chunk_size = Math.min(NFS_WRITE_SIZE, size);
	    if ((data.data == null) || (data.data.length != chunk_size)) {
		data.data = new byte[chunk_size];
	    }
	    System.arraycopy(buf, ofs, data.data, 0, chunk_size);

	    AttrStat res = nfs.write(fileHandle, 0, offset, 0, data);
	    if (! (res instanceof AttrStatOK)) {
		System.out.println("Write error " + res);
		System.exit(1);
	    }
	    this.attr = ((AttrStatOK)res).attributes;


	    offset += chunk_size;
	    ofs += chunk_size;
	    size -= chunk_size;
	    bytes_written += chunk_size; 
	}
	return bytes_written;
    }
  
    public int available() {
	return 0;
    }

    public static Inode getRoot(RPC rpc, IPAddress host, String path) throws NFSException {
	return new NFSInode(rpc, host, path);
    }


    // TODO: implement these methods
    
    public void finalize() {}


    public Inode getParent() {return parent;}

    public void setParent(Inode parent) {}
    
    public boolean isDirty() {return false;}

    public void setDirty(boolean value) {}

    public void incUseCount() {}

    public void decUseCount() {}
    public int   i_nlinks() {return 0;}
    public void  deleteInode() throws InodeIOException, NotExistException {}
    public void  writeInode() throws InodeIOException, NotExistException {}
    public void  putInode() throws NotExistException {}
    public void overlay(Inode newChild, String name) throws InodeIOException, InodeNotFoundException, NoDirectoryInodeException, NotExistException, PermissionException {}
    public void removeOverlay(Inode child) throws InodeNotFoundException, NoDirectoryInodeException, NotExistException {}
    public void removeAllOverlays() throws NoDirectoryInodeException, NotExistException {}
    public boolean isOverlayed(String name) throws NoDirectoryInodeException, NotExistException { return false;}
    public Inode lookup(String name) throws InodeIOException, InodeNotFoundException, NoDirectoryInodeException, NotExistException, PermissionException {return null;}
    public boolean isSymlink() throws NotExistException {return false;}
    public int    lastModified() throws NotExistException {return 0;}
    public Inode   getInode(String name) throws InodeIOException, InodeNotFoundException, NoDirectoryInodeException, NotExistException, PermissionException {return null;}
    public Inode   mkdir(String name, int mode) throws FileExistsException, InodeIOException, NoDirectoryInodeException, NotExistException, PermissionException { return null;}
    public void    rmdir(String name) throws DirNotEmptyException, InodeIOException, InodeNotFoundException, NoDirectoryInodeException, NotExistException,PermissionException {}
    public Inode   create(String name, int mode) throws FileExistsException, InodeIOException, NoDirectoryInodeException, NotExistException, PermissionException {return null;}
    public void    unlink(String name) throws InodeIOException, InodeNotFoundException, NoDirectoryInodeException, NoFileInodeException, NotExistException,PermissionException {}
    public Inode   symlink(String symname, String newname) throws FileExistsException, InodeIOException, NoDirectoryInodeException, NotExistException, NotSupportedException,PermissionException {return null;}
    public String  getSymlink() throws InodeIOException, NoSymlinkInodeException, NotExistException, NotSupportedException, PermissionException {return null;}
    public void    rename(String oldname, Inode new_dir, String newname) throws InodeIOException, InodeNotFoundException, NoDirectoryInodeException, NotExistException, PermissionException {}
    public int     read(Memory mem, int off, int len) throws InodeIOException, NoFileInodeException, NotExistException, PermissionException {return 0;}
    public int     read(int pos, Memory mem, int off, int len) throws InodeIOException, NoFileInodeException, NotExistException, PermissionException {return 0;}
    public int     write(Memory mem, int off, int len) throws InodeIOException, NoFileInodeException, NotExistException, PermissionException {return 0;}
    public int     write(int pos, Memory mem, int off, int len) throws InodeIOException, NoFileInodeException, NotExistException, PermissionException {return 0;}

    public int    lastAccessed() throws NotExistException {return 0;}
    public int    lastChanged() throws NotExistException {return 0;}
    public void setLastModified(int time) throws NotExistException {}
    public void setLastAccessed(int time) throws NotExistException {}

    public int getIdentifier() throws NotExistException  {return 0;}

    public int getVersion() throws NotExistException  {return 0;}

    public FileSystem getFileSystem() throws NotExistException  {return null;}

    public StatFS getStatFS() {return null;}

}
