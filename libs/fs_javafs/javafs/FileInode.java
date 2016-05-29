package javafs;

import java.util.Vector;
import jx.zero.Debug;
import jx.zero.Memory;
import jx.zero.ReadOnlyMemory;
import jx.zero.Clock;
import jx.fs.FSException;
import jx.fs.InodeIOException;
import jx.fs.NoDirectoryInodeException;
import jx.fs.NoSymlinkInodeException;
import jx.fs.NotExistException;
import jx.fs.PermissionException;
import jx.fs.InodeNotFoundException;
import jx.fs.FileExistsException;
import jx.fs.DirNotEmptyException;
import jx.fs.NoFileInodeException;
import jx.fs.NotSupportedException;

import jx.fs.buffercache.*;

/**
 * This class contains all methods to manipulate a file.
 * Methods that can not be applied to a file throw an exception.
 * (example: <code>mkdir</code>, <code>unlink</code>) throw <code>NoDirectoryInodeException</code>)
 */
public class FileInode extends InodeImpl {

    private static final boolean trace = false;
    private final int blockmask;

    public FileInode(FileSystem fileSystem, Super i_sb, int i_ino, InodeData i_data, BufferCache bufferCache, InodeCache inodeCache, Clock clock) {
	super(fileSystem, i_sb, i_ino, i_data, bufferCache, inodeCache, clock);
	blockmask = i_sb.s_blocksize - 1;
    }

    public boolean isSymlink() throws NotExistException {
	if (i_released) throw new NotExistException();
	return false;
    }

    public boolean isFile() throws NotExistException {
	if (i_released) throw new NotExistException();
	return true;
    }

    public boolean isDirectory() throws NotExistException {
	if (i_released) throw new NotExistException();
	return false;
    }

    public boolean isWritable() throws NotExistException {
	if (i_released) throw new NotExistException();
	return true;
    }

    public boolean isReadable() throws NotExistException {
	if (i_released) throw new NotExistException();
	return true;
    }

    public boolean isExecutable() throws NotExistException {
	if (i_released) throw new NotExistException();
	return true;
    }

    public String[] readdirNames() throws NoDirectoryInodeException, NotExistException {
	if (i_released)	throw new NotExistException();
	throw new NoDirectoryInodeException();
    }

    public jx.fs.Inode getInode(String name) throws InodeIOException, InodeNotFoundException, NoDirectoryInodeException, NotExistException, PermissionException {
	if (i_released)	throw new NotExistException();
	throw new NoDirectoryInodeException();
    }

    public jx.fs.Inode mkdir(String name, int mode) throws FileExistsException, InodeIOException, NoDirectoryInodeException, NotExistException, PermissionException {
	if (i_released)	throw new NotExistException();
	throw new NoDirectoryInodeException();
    }

    public void rmdir(String name) throws DirNotEmptyException, InodeIOException, InodeNotFoundException, NoDirectoryInodeException, NotExistException,PermissionException {
	if (i_released)	throw new NotExistException();
	throw new NoDirectoryInodeException();
    }

    public jx.fs.Inode create(String name, int mode) throws FileExistsException, InodeIOException, NoDirectoryInodeException, NotExistException, PermissionException {
	if (i_released)	throw new NotExistException();
	throw new NoDirectoryInodeException();    
    }

    public void unlink(String name) throws InodeIOException, InodeNotFoundException, NoDirectoryInodeException, NoFileInodeException, NotExistException,PermissionException {
	if (i_released) throw new NotExistException();
	throw new NoDirectoryInodeException();
    }

    public jx.fs.Inode symlink(String symname, String newname) throws FileExistsException, InodeIOException, NoDirectoryInodeException, NotExistException, NotSupportedException,PermissionException {
	if (i_released)	throw new NotExistException();
	throw new NoDirectoryInodeException();
    }

    public String getSymlink() throws InodeIOException, NoSymlinkInodeException, NotExistException, NotSupportedException, PermissionException {
	if (i_released)	throw new NotExistException();
	throw new NoSymlinkInodeException();
    }

    public void rename(String oldname, jx.fs.Inode new_dir, String newname) throws InodeIOException, InodeNotFoundException, NoDirectoryInodeException, NotExistException, PermissionException {
	if (i_released)	throw new NotExistException();
	throw new NoDirectoryInodeException();
    }

    /*
    public  int read(int pos, Memory mem, int bufoff, int len) throws InodeIOException, NoFileInodeException, NotExistException, PermissionException {
	if (i_released) throw new NotExistException();

	int block, offset, already_read, c;
	BufferHead bh;
		
	if (len == 0) return 0;
	
	if (pos > pos + len) // Int-Ueberlauf
	    throw new InodeIOException();
	
	if (! permission(MAY_READ))
	    throw new PermissionException();
	
	block = pos / i_sb.s_blocksize;
	offset = pos & blockmask;

	c = i_sb.s_blocksize - offset;
	already_read = 0;

	if (trace) Debug.out.println("FileInode.read(): pos = " + pos + ", block = " + block + ", offset = " + offset);

	int filesize = i_data.i_size();
	do {
	    if (c > len) c = len;

	    if (pos + c > filesize) // read request too large (larger as file)
		c = filesize - pos;
	    
	    if (c > 1024) throw new Error();
	    
	    if (c <= 0)
		break;
	    
	    bh = getBlk(block, true);

	    if (bh == null) {
		if (already_read == 0)
		    already_read = -1;
		break;
	    }
	    
	    bufferCache.updateBuffer(bh);
	    
	    bh.getData().copyToMemory(mem, offset, bufoff, c);
	    //bh.getData().copyToByteArray(b, bufoff, offset, c);
	    
	    pos += c;
	    already_read += c;
	    bufoff += c;
	    len -= c;
	    
	    bufferCache.brelse(bh);
	    
	    block++;
	    offset = 0;
	    c = i_sb.s_blocksize;
	} while (len > 0);
	
	i_data.i_atime(clock.getTimeInMillis());
	setDirty(true);
	
	if (already_read < 0)
	    throw new InodeIOException();
	
	return already_read;
    }
    */

    public  int read(int pos, Memory mem, int bufoff, int len) throws InodeIOException, NoFileInodeException, NotExistException, PermissionException {
	if (i_released) throw new NotExistException();

	int block, offset, already_read, c;
	BufferHead bh;

	// read request too large (larger as file)
	int filesize = i_data.i_size();
	if (pos > filesize) return 0;
	if (pos + len > filesize) len = filesize - pos;
	
	block = pos / i_sb.s_blocksize;
	offset = pos & blockmask;

	c = i_sb.s_blocksize - offset;
	already_read = 0;

	do {
	    if (c > len) c = len;

	    if (c <= 0) break;
	    
	    bh = getBlk(block, true);

	    if (bh == null) {
		if (already_read == 0)
		    already_read = -1;
		break;
	    }
	    
	    bufferCache.updateBuffer(bh);
	    
	    bh.getData().copyToMemory(mem, offset, bufoff, c);
	    
	    //pos += c;
	    already_read += c;
	    bufoff += c;
	    len -= c;
	    
	    bufferCache.brelse(bh);
	    
	    block++;
	    offset = 0;
	    c = i_sb.s_blocksize;
	} while (len > 0);
	
	i_data.i_atime(clock.getTimeInMillis());
	setDirty(true);
	
	if (already_read < 0)
	    throw new InodeIOException();
	
	return already_read;
    }    

    public  int read(Memory mem, int off, int len) throws InodeIOException, NoFileInodeException, NotExistException, PermissionException {
	if (i_released) throw new NotExistException();

	int pos, block, offset, already_read, c, bufoff;
	BufferHead bh;
	
	
	if (len == 0)
	    return 0;
	
	pos = off;
	
	if (pos > pos + len) // Ueberlauf
	    throw new InodeIOException();
	
	if (! permission(MAY_READ))
	    throw new PermissionException();
	
	block = pos / i_sb.s_blocksize;
	offset = pos & blockmask;
	c = i_sb.s_blocksize - offset;
	already_read = 0;
	bufoff = 0;
	if (trace) Debug.out.println("FileInode.read(): pos = " + pos + ", block = " + block + ", offset = " + offset);
	int filesize = i_data.i_size();
	do {
	    if (c > len)
		c = len;
	    if (pos + c > filesize) // read request too large (larger as file)
		c = filesize - pos;
	    
	    if (c > 1024) throw new Error();
	    
	    if (c <= 0)
		break;
	    
	    bh = getBlk(block, true);
	    if (bh == null) {
		if (already_read == 0)
		    already_read = -1;
		break;
	    }
	    
	    bufferCache.updateBuffer(bh);

	    bh.getData().copyToMemory(mem, offset, bufoff, c);
	    
	    pos += c;
	    already_read += c;
	    bufoff += c;
	    len -= c;
	    
	    bufferCache.brelse(bh);
	    
	    block++;
	    offset = 0;
	    c = i_sb.s_blocksize;
	} while (len > 0);
	
	i_data.i_atime(clock.getTimeInMillis());
	setDirty(true);
	
	if (already_read < 0)
	    throw new InodeIOException();
	
	return already_read;
    }

    public  ReadOnlyMemory readWeak(int off, int len) throws InodeIOException, NoFileInodeException, NotExistException, PermissionException {
	if (i_released) throw new NotExistException();

	int pos, block, offset, already_read, c, bufoff;
	BufferHead bh;
	
	
	if (len == 0)
	    return null;
	
	pos = off;
	
	if (pos > pos + len) // Ueberlauf
	    throw new InodeIOException();
	
	if (! permission(MAY_READ))
	    throw new PermissionException();
	
	block = pos / i_sb.s_blocksize;
	offset = pos & blockmask;
	c = i_sb.s_blocksize - offset;
	already_read = 0;
	bufoff = 0;
	if (trace) Debug.out.println("FileInode.read(): pos = " + pos + ", block = " + block + ", offset = " + offset);
	int filesize = i_data.i_size();
	do {
	    if (c > len)
		c = len;
	    if (pos + c > filesize) // read request too large (larger as file)
		c = filesize - pos;
	    
	    if (c > 1024) throw new Error();
	    
	    if (c <= 0)
		break;
	    
	    bh = getBlk(block, true);
	    if (bh == null) {
		if (already_read == 0)
		    already_read = -1;
		break;
	    }
	    
	    bufferCache.updateBuffer(bh);

	    // EXPERIMENTAL: DO NOT COPY
	    //bh.getData().copyToMemory(mem, offset, bufoff, c);
	    
	    pos += c;
	    already_read += c;
	    bufoff += c;
	    len -= c;
	    
	    bufferCache.brelse(bh);
	    
	    block++;
	    offset = 0;
	    c = i_sb.s_blocksize;
	} while (len > 0);
	
	i_data.i_atime(clock.getTimeInMillis());
	setDirty(true);
	
	if (already_read < 0)
	    throw new InodeIOException();
	
	return null; // return a chunked ROMemory here
    }

    public  int write(byte[] b, int off, int len) {throw new Error(); }

    public  int write(Memory mem, int off, int len) throws InodeIOException, NoFileInodeException, NotExistException, PermissionException {
	if (i_released) throw new NotExistException();

	//Bitmap.traceBitmap = true;

	int pos;
	int block;
	int offset, bufoff;
	int written, c;
	BufferHead bh;
	int i, write_error = 0;
	    
	if (len == 0)
	    return 0;
	    
	if (! permission(MAY_WRITE))
	    throw new PermissionException();
	    
	//if ((flags & O_APPEND) > 0)
	//    pos = i_data.i_size();
	//else {
	pos = off;
	//}
	    
	if (pos > pos + len) // Ueberlauf
	    throw new InodeIOException("Overflow: pos="+pos+", len="+len);
	    
	block = pos / i_sb.s_blocksize;
	offset = pos & (i_sb.s_blocksize - 1);
	c = i_sb.s_blocksize - offset;
	written = 0;
	bufoff = 0;
	do {
	    bh = getBlk(block, true, false);
	    if (bh == null) {
		if (written == 0) {
		    Debug.out.println("getBlk returned null and written was 0; block="+block);
		    written = -1;
		}
		break;
	    }
	    if (c > len)
		c = len;
		
	    if ((! bh.isUptodate()) && (! bh.isLocked()) && (c == i_sb.s_blocksize)) {
		// new buffer
		bh.lock();
		bh.getData().copyFromMemory(mem, bufoff, offset, c);
		bh.markUptodate();
		bh.unlock();
	    } else {
		// buffer is not up-to-date
		bufferCache.updateBuffer(bh);
		bh.getData().copyFromMemory(mem, bufoff, offset, c);
	    }
	    if (c == 0) {
		bufferCache.brelse(bh);
		throw new Error("EFAULT");
	    }
	    if (pos + c < 0) { // Ueberlauf
		bufferCache.bdwrite(bh);
		break;
	    }
	    pos += c;
	    written += c;
	    bufoff += c;
	    len -= c;
		
	    bufferCache.bdwrite(bh);
	    if (write_error > 0)
		break;
	    block++;
	    offset = 0;
	    c = i_sb.s_blocksize;
	} while (len > 0);
	    
	if (pos > i_data.i_size())
	    i_data.i_size(pos);
	int times = clock.getTimeInMillis();
	i_data.i_ctime(times);
	i_data.i_mtime(times);
	setDirty(true);
	    
	if (written < 0)
	    throw new InodeIOException("written<0");
	    
	//Bitmap.traceBitmap = false;
	return written;
    }

    public  int write(int pos, Memory mem, int bufoff, int len) throws InodeIOException, NoFileInodeException, NotExistException, PermissionException {
	if (i_released) throw new NotExistException();

	//int pos;
	int block;
	int offset; //bufoff;
	int written, c;
	BufferHead bh;
	int i, write_error = 0;
	    
	if (len == 0)
	    return 0;
	    
	if (! permission(MAY_WRITE))
	    throw new PermissionException();
	    
	if (pos > pos + len) // Ueberlauf
	    throw new InodeIOException("Overflow: pos="+pos+", len="+len);
	    
	block = pos / i_sb.s_blocksize;
	offset = pos & (i_sb.s_blocksize - 1);
	c = i_sb.s_blocksize - offset;
	written = 0;
	bufoff = 0;
	do {
	    bh = getBlk(block, true, false);
	    if (bh == null) {
		if (written == 0) {
		    Debug.out.println("getBlk returned null and written was 0; block="+block);
		    written = -1;
		}
		break;
	    }
	    if (c > len)
		c = len;
		
	    if ((! bh.isUptodate()) && (! bh.isLocked()) && (c == i_sb.s_blocksize)) {
		// new buffer
		bh.lock();
		bh.getData().copyFromMemory(mem, bufoff, offset, c);
		//bh.getData().copyFromByteArray(b, bufoff, offset, c);
		bh.markUptodate();
		bh.unlock();
	    } else {
		// buffer is not up-to-date
		bufferCache.updateBuffer(bh);
		bh.getData().copyFromMemory(mem, bufoff, offset, c);
		//bh.getData().copyFromByteArray(b, bufoff, offset, c);
	    }
	    if (c == 0) {
		bufferCache.brelse(bh);
		throw new Error("EFAULT");
	    }
	    if (pos + c < 0) { // Ueberlauf
		bufferCache.bdwrite(bh);
		break;
	    }
	    pos += c;
	    written += c;
	    bufoff += c;
	    len -= c;
		
	    bufferCache.bdwrite(bh);
	    if (write_error > 0)
		break;
	    block++;
	    offset = 0;
	    c = i_sb.s_blocksize;
	} while (len > 0);
	    
	if (pos > i_data.i_size())
	    i_data.i_size(pos);

	int times = clock.getTimeInMillis();
	i_data.i_ctime(times);
	i_data.i_mtime(times);
	setDirty(true);
	    
	if (written < 0)
	    throw new InodeIOException("written<0");
	    
	//Bitmap.traceBitmap = false;
	return written;
    }
}
