package jx.fs;

import jx.zero.Debug;

final public class ReadOnlyDirectoryImpl extends FSObjectImpl implements jx.fs.ReadOnlyDirectory {

    public ReadOnlyDirectoryImpl(FilesystemImpl impl, FileSystem fs, FSObjectImpl parent, Inode inode) {
	super(impl, fs, parent, inode);
    }
    
    public Directory  getParent() { return (Directory)parent; }

    public String[] list() throws Exception { 
	String[] dirList = inode.readdirNames();
	return inode.readdirNames();
    }

    public void close() throws Exception {
	inode.decUseCount();
	inode=null;
    }

    public int length() throws Exception {return 0;}

    public FSObject openRO(String filename) throws Exception {
	try {
	    Inode nInode = inode.lookup(filename);
	    if (nInode.isDirectory()) {
		return fs_impl.registerFSObj(new ReadOnlyDirectoryImpl(fs_impl,fs,this,nInode));
	    } else if (nInode.isFile()) {
		return fs_impl.registerFSObj(new ReadOnlyRegularFileImpl(fs_impl,fs,this,nInode));
	    } else {
		return null;
	    }
	} catch (Exception ex) {
	    Debug.verbose("exception caught (lookup)");
	    return null;
	}
    }
    
    public FSObject openRW(String filename) throws Exception {
	try {
	    Inode nInode = inode.lookup(filename);
	    if (nInode.isDirectory()) {
		return new DirectoryImpl(fs_impl,fs,this,nInode);
	    } else if (nInode.isFile()) {
		return new RegularFileImpl(fs_impl,fs,this,nInode);
	    } else {
		return null;
	    }
	} catch (Exception ex) {
	    Debug.verbose("exception caught (lookup)");
	    return null;
	}
    }

    protected void finalize() throws Throwable {
	inode.decUseCount();
    }
}
