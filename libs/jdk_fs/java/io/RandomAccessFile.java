package java.io;

import java.io.DataInputStream;

import jx.fs.*;
import jx.zero.*;

public class RandomAccessFile implements DataInput, DataOutput
{

    private final int BUF_SIZE = 1024;
    private Memory io_mem;
    private byte[] io_buffer;

    private FileDescriptor fd = new FileDescriptor();

    private File                file;
    private ExtendedFileSystemInterface fs;
    private MemoryManager       mm;    
    private FSObject fsobj;

    private int  filePos;

    private volatile boolean closing;

    public RandomAccessFile(String path, String mode) throws IOException {
	if (!mode.equals("r") && !mode.equals("rw"))
	    throw new IllegalArgumentException();
	
	
	Naming ns = (Naming) InitialNaming.getInitialNaming();
	try {
	    fs = ExtendedFileSystemInterface.getExtFileSystemInterface("FSInterface");
	} catch (Exception ex) {
	    throw new IOException("Can`t access file system interface");
	}
	mm = (jx.zero.MemoryManager) ns.lookup("MemoryManager");

	io_mem = mm.alloc(BUF_SIZE);
	io_buffer = new byte[BUF_SIZE];

	fs.setSeparator(File.separatorChar);

	try {
	    if (mode.equals("rw")) {fsobj = fs.openRW(path);} else {fsobj = fs.openRO(path);}
	    if (fsobj==null) {
		//Permission perm = new EXT2Permission(EXT2Permission.RW,0,0);
		//fsobj = fs.create(perm,path);
		fsobj = fs.create(path);
	    }
	} catch (Exception ex) {
	    ex.printStackTrace();
	    throw new Error();
	}
    }
    
    public RandomAccessFile(File file, String mode) throws IOException {
	this(file.getPath(), mode);
    }
        
    public  FileDescriptor getFD() throws IOException {
	return fd;
    }
  
    public synchronized void close() throws IOException  {
	if (closing) return;
	closing = true;
	try {
	    if (fsobj!=null) fsobj.close();
	} catch (Exception ex) {}
	fd = null;
	fsobj = null;
	closing = false;
    }
      
    public synchronized long getFilePointer() throws IOException {
	if (fsobj == null) throw new IOException();
	return filePos;
    }

    /*
    public synchronized void seek(long pos) throws IOException {
	if (fsobj == null) throw new IOException();
	filePos = pos;
    }
    */

    public synchronized void seek(int pos) throws IOException {
	if (fsobj == null) {
	    Debug.message("fsobj==null");
	    throw new IOException();
	}
	filePos = pos;
    }

    /*
    public synchronized long length() throws IOException {
	try {
	    return (long)fsobj.length();
	} catch(Exception ex) { 
	    throw new IOException();
	}
    }
    */

    public synchronized long length() throws IOException {
	try {
	    return (int)fsobj.length();
	} catch(Exception ex) { 
	    throw new IOException();
	}
    }

    public synchronized int skipBytes(int n) throws IOException {
	if (fsobj==null) throw new IOException();
	filePos += n;
	return n;
    }
  
    public synchronized int read() throws IOException {
	int ret;
	if (fsobj==null) throw new IOException();
	if ((ret=read(io_buffer, 0, 1))<1) return -1;	
	return ((int)io_buffer[0]) & 0xff;
	/*
	try {
	    if ((ret=fsobj.read(filePos,io_mem,0,1))<1) return -1;
	    int value = ((int)io_mem.get8(0)) & 0xFF;
	    filePos++;
	    return value;
	} catch (Exception ex) {
	    throw new IOException(ex.getMessage());
	}
	*/
    }

    public synchronized int read(byte[] b) throws IOException {
	return read(b, 0, b.length);
    }

    public synchronized int read(byte[] b, int off, int len) throws IOException {
	int ret;
	Memory mem;
       
	if (fsobj==null) throw new IOException();

	try {
	    if (len>io_mem.size()) {io_mem = mem = mm.alloc(len);} else {mem = io_mem;}
	    if (fsobj instanceof ReadOnlyRegularFile) {
		if ((ret=((ReadOnlyRegularFile)fsobj).read(filePos,mem,0,len))<0) return -1;
	    } else {
		if ((ret=((RegularFile)fsobj).read(filePos,mem,0,len))<0) return -1;
	    }
	    mem.copyToByteArray(b, off, 0, len);
	} catch(Exception e) {
	    throw new IOException(e.getMessage());
	}

	filePos+=ret;
	return ret;
    }

    public synchronized void write(int c) throws IOException {
	io_buffer[0] = (byte) c;
	write(io_buffer, 0 , 1);
    }
  
    public synchronized void write(byte[] b) throws IOException {
	write(b, 0, b.length);
    }

    public synchronized void write(byte[] b, int off, int len) throws IOException {
	int ret=-1;
	Memory mem;
	
	if (fsobj==null) throw new IOException();
	if (!(fsobj instanceof RegularFile)) throw new IOException();
	
	try {
	    if (len>io_mem.size()) {io_mem = mem = mm.alloc(len);} else {mem = io_mem;}	    
	    mem.copyFromByteArray(b, off, 0, len);
	    ret=((RegularFile)fsobj).write(filePos,mem,0,len);
	} catch(Exception e) {
	    throw new IOException(e.getMessage());
	}

	filePos+=ret;
	if (ret<len && ret<b.length) throw new IOException();	
    }

    /*** Interface DataInput ***/

    public synchronized void readFully(byte[] b) throws IOException
    {
	readFully(b, 0, b.length);
    }

    public synchronized  void readFully(byte[] b, int off, int len) throws IOException
    {
	while (len > 0)
	    {
		int n = read(b, off, len);
		if (n < 0) throw new EOFException();
		off += n;
		len -= n;
	    }
    }

    public synchronized boolean readBoolean() throws IOException
    {
	return read() != 0;
    }

    public synchronized byte readByte() throws IOException
    {
	return (byte) read();
    }

    public synchronized int readUnsignedByte() throws IOException
    {
	return read();
    }

    public synchronized char readChar() throws IOException
    {	
	if (read(io_buffer,0,2) < 0) throw new EOFException();
	int n = io_buffer[0];
	n <<= 8; n += io_buffer[1];
	return (char) n;
    }

    public synchronized short readShort() throws IOException
    {
	if (read(io_buffer,0,2) < 0) throw new EOFException();
	int n = io_buffer[0];
	n <<= 8; n += io_buffer[1];
	return (short) n;
    }

    public synchronized int readUnsignedShort() throws IOException
    {
	if (read(io_buffer,0,2) < 0) throw new EOFException();
	int n = io_buffer[0];
	n <<= 8; n += io_buffer[1];
	return n;
    }

    public synchronized int readInt() throws IOException
    {
	if (read(io_buffer,0,4) < 0) throw new EOFException();
	int n = io_buffer[0];
	n <<= 8; n += io_buffer[1];
	n <<= 8; n += io_buffer[2];
	n <<= 8; n += io_buffer[3];
	return n;
    }

    public synchronized long readLong() throws IOException
    {
	if (read(io_buffer,0,8) < 0) throw new EOFException();
	long n = io_buffer[0];
	n <<= 8; n += io_buffer[1];
	n <<= 8; n += io_buffer[2];
	n <<= 8; n += io_buffer[3];
	n <<= 8; n += io_buffer[4];
	n <<= 8; n += io_buffer[5];
	n <<= 8; n += io_buffer[6];
	n <<= 8; n += io_buffer[7];
	return n;
    }

    public synchronized  float readFloat() throws IOException
    {
	throw new IOException("not implemented");
	/*
	int n = readInt();
	return Float.intBitsToFloat(n);
	*/
    }

    public synchronized  double readDouble() throws IOException
    {
	throw new IOException("not implemented");
	/*
	long n = readLong();
	return Double.longBitsToDouble(n);
	*/
    }

    public  synchronized String readLine() throws IOException {
	StringBuffer buff = new StringBuffer();
	
	while (true) {
	    int c = read();
	    
	    if (c == '\n')
		break;
	    
	    if (c == '\r') {
		c = read();
		if (c != '\n') filePos--;
		break;
	    }
	    buff.append((char) c);
	    
	}
	
	return buff.toString();
    }

    public synchronized  String readUTF() throws IOException
    {
	return DataInputStream.readUTF(this);
    }

    /*** Interface DataOutput ***/ 

    public synchronized final void writeBoolean(boolean v) throws IOException {
	write(v ? 1 : 0);
    }

    public synchronized final void writeByte(int v) throws IOException {
	write(v);
    }

    public synchronized final void writeShort(int v) throws IOException {
	write((v >>> 8) & 0xFF);
	write((v >>> 0) & 0xFF);
    }

    public synchronized final void writeChar(int v) throws IOException {
	write((v >>> 8) & 0xFF);
	write((v >>> 0) & 0xFF);
    }

    public synchronized final void writeInt(int v) throws IOException {
	write((v >>> 24) & 0xFF);
	write((v >>> 16) & 0xFF);
	write((v >>>  8) & 0xFF);
	write((v >>>  0) & 0xFF);
    }

    public synchronized final void writeLong(long v) throws IOException {
	write((int)(v >>> 56) & 0xFF);
	write((int)(v >>> 48) & 0xFF);
	write((int)(v >>> 40) & 0xFF);
	write((int)(v >>> 32) & 0xFF);
	write((int)(v >>> 24) & 0xFF);
	write((int)(v >>> 16) & 0xFF);
	write((int)(v >>>  8) & 0xFF);
	write((int)(v >>>  0) & 0xFF);
    }

    public synchronized final void writeFloat(float v) throws IOException {
	writeInt(Float.floatToIntBits(v));
    }

    public synchronized final void writeDouble(double v) throws IOException {
	writeLong(Double.doubleToLongBits(v));
    }

    public synchronized final void writeBytes(String s) throws IOException {
	int len = s.length();
	for (int i = 0 ; i < len ; i++) {
	    write((byte)s.charAt(i));
	}
    }

    public synchronized final void writeChars(String s) throws IOException {
	int len = s.length();
	for (int i = 0 ; i < len ; i++) {
	    int v = s.charAt(i);
	    write((v >>> 8) & 0xFF);
	    write((v >>> 0) & 0xFF);
	}
    }

    public synchronized final void writeUTF(String str) throws IOException {
	//OutputStream out = this;
	int strlen = str.length();
	int utflen = 0;

	for (int i = 0 ; i < strlen ; i++) {
	    int c = str.charAt(i);
	    if ((c >= 0x0001) && (c <= 0x007F)) {
		utflen++;
	    } else if (c > 0x07FF) {
		utflen += 3;
	    } else {
		utflen += 2;
	    }
	}

	if (utflen > 65535)
	    throw new UTFDataFormatException();		  

	write((utflen >>> 8) & 0xFF);
	write((utflen >>> 0) & 0xFF);
	for (int i = 0 ; i < strlen ; i++) {
	    int c = str.charAt(i);
	    if ((c >= 0x0001) && (c <= 0x007F)) {
		write(c);
	    } else if (c > 0x07FF) {
		write(0xE0 | ((c >> 12) & 0x0F));
		write(0x80 | ((c >>  6) & 0x3F));
		write(0x80 | ((c >>  0) & 0x3F));
	    } else {
		write(0xC0 | ((c >>  6) & 0x1F));
		write(0x80 | ((c >>  0) & 0x3F));
	    }
	}
    }
}

