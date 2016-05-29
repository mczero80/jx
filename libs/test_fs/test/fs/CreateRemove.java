package test.fs;


import jx.fs.*;
import jx.zero.*;

/**
 * Create and remove files
 *
 * @author Michael Golm
 */

public class CreateRemove {
    private static final int MAXBUFFERSIZE   = 1*1024*1024;

    public static void main(String[] args) throws Exception {
	Memory buffer;
	Naming naming = InitialNaming.getInitialNaming();
	MemoryManager memMgr = (MemoryManager)InitialNaming.getInitialNaming().lookup("MemoryManager");
	buffer = memMgr.allocAligned(MAXBUFFERSIZE, 4);

	/*
	FSImpl fs = new FSImpl();
	//FS fsPortal = (FS) naming.promoteDEP(fs, "jx/fs/FS");
	naming.registerPortal(fs, "FS");
	

	fs.mountRoot(jfs, false); // 2. Parameter = read-only  //hda8

	fs.mkdir("lost+found", InodeImpl.S_IWUSR|InodeImpl.S_IRUGO|InodeImpl.S_IXUGO);
	*/
	
	FS fs = null ;
	while((fs=(FS)naming.lookup("FS")) == null) Thread.yield();

	
	fs.create("TEST0", InodeImpl.S_IWUSR|InodeImpl.S_IRUGO);
	fs.create("TEST1", InodeImpl.S_IWUSR|InodeImpl.S_IRUGO);
	

	jx.fs.FileSystem jfs = (jx.fs.FileSystem)naming.lookup("JavaFS");	
	if (jfs != null)
	    jfs.release();

	/*

	try {
	    for(int i=0; i<10; i++) {
		String filename = "test"+i;
		//RegularFile file = (RegularFile)fs.openRW(filename);
		RegularFile file = (RegularFile)fs.create(filename);
		if (file == null) throw new Error("Could not create "+filename);
		//    int ret=file.read(filePos, buffer, 0, reclen);
		//		    if (ret < 0) throw new Error();
		//	    filePos += reclen;
		file.close();
	    }	    
	} catch(Throwable e) {
	    fs.unmount();
	}
	*/
	Debug.out.println("Test finished.");
    }
    

    static void dotest(jx.fs.FileSystem fs) throws FSException {
	Inode ino =  fs.getRootInode();
	ino.create("bla1",InodeImpl.S_IWUSR|InodeImpl.S_IRUGO|InodeImpl.S_IXUGO);
	ino.create("bla0",InodeImpl.S_IWUSR|InodeImpl.S_IRUGO|InodeImpl.S_IXUGO);
	/*
	for(int i=0; i<10; i++) {
	    ino.create("test"+i,InodeImpl.S_IWUSR|InodeImpl.S_IRUGO|InodeImpl.S_IXUGO);
	}
	*/
	//	ino.mkdir("bla",InodeImpl.S_IWUSR|InodeImpl.S_IRUGO|InodeImpl.S_IXUGO);
    }
}
