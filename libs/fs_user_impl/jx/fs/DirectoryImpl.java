package jx.fs;

import jx.zero.Debug;

final public class DirectoryImpl extends FSObjectImpl implements jx.fs.Directory {

    public DirectoryImpl(FilesystemImpl impl, FileSystem fs, FSObjectImpl parent, Inode inode) {
	super(impl, fs, parent, inode);
    }
    
    public Directory getParent() { return parent; }

    public String[] list() throws Exception { 
	try {
	    String[] dirList = inode.readdirNames();
	    return dirList;
	} catch (Exception ex) {
	    return null;
	}
    }

    public int length() throws Exception { return 0; }

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
		return fs_impl.registerFSObj(new DirectoryImpl(fs_impl,fs,this,nInode));
	    } else if (nInode.isFile()) {
		return fs_impl.registerFSObj(new RegularFileImpl(fs_impl,fs,this,nInode));
	    } else {
		return null;
	    }
	} catch (Exception ex) {
	    Debug.verbose("exception caught (lookup)");
	    return null;
	}
    }

    public RegularFile create(Permission perm, String filename) throws Exception {
	try {
	    Inode nInode = inode.create(filename, InodeImpl.S_IWUSR|InodeImpl.S_IRUGO);
	    return new RegularFileImpl(fs_impl,fs,this,nInode);
	} catch (Exception ex) {
	    return null;
	}
    }

    public boolean    link(Permission perm, String filename, FSObject file) throws Exception { return false; }

    public boolean    unlink(String filename) throws Exception { return false; }

    public boolean    mkdir(Permission perm, String name) throws Exception {return false;}

    protected void finalize() throws Throwable {
	inode.decUseCount();
    }
}
