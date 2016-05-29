package jx.fs;

abstract public class FSObjectImpl implements jx.fs.FSObject, jx.zero.Service {

    protected FilesystemImpl fs_impl;
    protected FileSystem     fs;
    protected DirectoryImpl  parent;
    protected Inode          inode;

    protected EXT2Permission perm = null;
    protected EXT2Attribute  attr = null;

    public abstract int length() throws Exception;

    public FSObjectImpl(FilesystemImpl fs_impl, FileSystem fs, FSObjectImpl parent, Inode inode) {
	this.fs_impl = fs_impl;
	this.fs      = fs;
	this.inode   = inode;
	// HACK
	this.perm    = (EXT2Permission)fs_impl.getDefaultPermission();
	this.attr   = new EXT2Attribute(1,perm);
	// =====
    }

    public void close() throws Exception {
	sync();
	inode.decUseCount();
	inode=null;
    }

    public void sync() throws Exception {}

    final public FilesystemInterface getFileSystem() {
	return fs_impl;
    }

    final public boolean isFile() throws Exception {
	return inode.isFile();
    }

    final public boolean isDirectory() throws Exception {
	return inode.isDirectory();
    }

    final public boolean isExecuteable() throws Exception {
	return inode.isExecutable();
    }

    final public Permission getPermission() throws Exception {	
	return perm;
    }

    final public FSAttribute getAttribute() throws Exception {
	if (attr==null) {attr = new EXT2Attribute(1,perm);}
	return attr;
    }

    public int hashCode() {
	try {
	    return inode.getIdentifier();
	} catch (Exception ex) {
	    return super.hashCode();
	}
    }
}
