package jx.fs;

import jx.zero.Memory;
import jx.zero.Debug;

final public class RegularFileImpl extends FSObjectImpl implements jx.fs.RegularFile {
        
    public RegularFileImpl(FilesystemImpl impl, FileSystem fs, FSObjectImpl parent, Inode inode) {
	super(impl, fs, parent,inode);
    }

    /**
     * Reads up to b.length bytes of data from this file into an array of bytes.
     */

    public int read(int pos, Memory mem, int off, int len) throws Exception {
	return inode.read(pos,mem,off,len);
    }

    /**
     * Writes len bytes from the specified byte array starting at offset off to this file.
     */

    public int write(int pos, Memory mem, int off, int len) throws Exception {
	return inode.write(pos,mem,off,len);
    }
    
    /**
     * Appends len bytes from the specified byte array starting at offset off to this file.
     */

    public int append(Memory mem, int off, int len) throws Exception {	
	return inode.write((int)length(),mem,off,len);
    }

    /**
     * Sets the length of this file.
     */

    public int setLength(long newLength) throws Exception {
	throw new Error("not implemented yet");
    }

    public int length() throws Exception {
	return inode.getLength();
    }

    protected void finalize() throws Throwable {
	inode.decUseCount();
    }
}
