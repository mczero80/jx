package jx.fs;

import jx.zero.Debug;
import jx.zero.Service;

import java.util.Vector;

public class FilesystemImpl implements FilesystemInterface, Service {

    private FileSystem     fs;
    private EXT2Permission defaultPermission = new EXT2Permission(EXT2Permission.RWX,0,0);
    private Inode          rInode;
    private Vector         fsobjList = new Vector();
    private boolean        umounted = true;

    public FilesystemImpl(FileSystem fs) {
	this.fs = fs;
	mount();
    }

    public String getName() {
	return fs.name();
    }

    public FSObject openRootDirectoryRO() {
	rInode.incUseCount();
	return registerFSObj(new ReadOnlyDirectoryImpl(this, fs, null, rInode));
    }

    public FSObject openRootDirectoryRW() {
	rInode.incUseCount();
	return registerFSObj(new DirectoryImpl(this, fs, null, rInode));
    }

    public Permission getDefaultPermission() {
	return defaultPermission;
    }

    public void mount() {
	if (!umounted) return;
	fs.init(true);
	rInode = fs.getRootInode();
	umounted = false;
    }

    public void unmount() {
	umounted = true;
	try {
	    for (int i=0;i<fsobjList.size();i++) {
		try {
		    FSObjectImpl fsobj = (FSObjectImpl)fsobjList.elementAt(i);
		    fsobj.close();
		} catch (Exception ex) {ex.printStackTrace();}
	    }
	    fsobjList.removeAllElements();
	    rInode.decUseCount();
	} catch (Exception ex) {ex.printStackTrace();}
	rInode=null;
	fs.release();
    }

    FSObjectImpl registerFSObj(FSObjectImpl fsobj) {
	if (umounted) {
	    try {fsobj.close();} catch (Exception ex) {ex.printStackTrace();}
	    return null;
	}
	fsobjList.addElement(fsobj);
	return fsobj;
    }

    protected void finalize() throws Throwable {
	if (!umounted) unmount();
    }
}
