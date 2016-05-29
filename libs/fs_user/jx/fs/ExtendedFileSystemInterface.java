package jx.fs;

import jx.zero.Debug;
import jx.zero.InitialNaming;
import jx.zero.Naming;
import jx.zero.LookupHelper;

final public class ExtendedFileSystemInterface {

    private FilesystemInterface ifs;
    private FSObject cwd;
    private char separator;

    public ExtendedFileSystemInterface(FilesystemInterface fs) throws Exception {
	this.ifs = fs;
	this.cwd = fs.openRootDirectoryRW();
	this.separator = '/';
    }

    final public static ExtendedFileSystemInterface getExtFileSystemInterface(String name) throws Exception {
	Naming ns = (Naming)InitialNaming.getInitialNaming();
	FilesystemInterface fs=(FilesystemInterface)LookupHelper.waitUntilPortalAvailable(ns, name);
	return new ExtendedFileSystemInterface(fs);
    }

    final public void setSeparator(char separator) {
	this.separator = separator;
    }

    final public FSObject openRO(String path) throws Exception {
	ReadOnlyDirectory dir = locate(path);
	if (dir==null) return null;
	return dir.openRO(filename(path));
    }

    final public FSObject openRW(String path) throws Exception {
	ReadOnlyDirectory dir = locate(path);
	if (dir==null) return null;
	return dir.openRW(filename(path));
    }

    final public FSObject create(String path) throws Exception {
	return create(ifs.getDefaultPermission(),path);
    }

    final public FSObject create(Permission perm, String path) throws Exception {
	int n = path.lastIndexOf(separator);
	if (n>0) {
	    String rpath = path.substring(0,n);
	    ReadOnlyDirectory dir = locate(rpath);
	    Directory odir = (Directory)dir.openRW(filename(rpath));	    
	    if (odir==null) return null;
	    return odir.create(perm,filename(path));
	} 
	return ((Directory)cwd).create(perm,filename(path));
    }

    final public String filename(String path) {
	int n = path.lastIndexOf(separator);
	if (n<0) return path;
	return path.substring(n+1);
    }

    final public ReadOnlyDirectory locate(String path) throws Exception {
	return locate(cwd,path);
    }

    final public ReadOnlyDirectory locate(FSObject dir, String path) throws Exception {
	String    rpath = path;
	String    name  = path;
	int       n = rpath.indexOf(separator);

	ReadOnlyDirectory cFSObj = (ReadOnlyDirectory)dir;

	if (n==0) {
	    rpath = rpath.substring(1);
	    n = rpath.indexOf(separator);
	}
	
	while (n>0) {
	    try {
		name  = rpath.substring(0,n);
		rpath = rpath.substring(n+1);
		if ((cFSObj=(ReadOnlyDirectory)cFSObj.openRO(name))==null) return null;
		n = rpath.indexOf(separator);
	    } catch (Exception ex) {
		Debug.message("file not fount ("+path+")");
		return null;
	    }
	}

	return cFSObj;
    }

    public void   mount() throws Exception {
	ifs.mount();
    }

    public void   unmount() throws Exception {
	ifs.unmount();
    }

}
