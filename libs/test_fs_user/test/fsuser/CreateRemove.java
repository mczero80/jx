package test.fsuser;


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
	MemoryManager memMgr = (MemoryManager)InitialNaming.getInitialNaming().lookup("MemoryManager");
	buffer = memMgr.allocAligned(MAXBUFFERSIZE, 4);

	ExtendedFileSystemInterface fs = ExtendedFileSystemInterface.getExtFileSystemInterface("FSInterface");
	RegularFile file0 = (RegularFile)fs.create("TEST0");
	RegularFile file1 = (RegularFile)fs.create("TEST1");
	RegularFile file2 = (RegularFile)fs.create("TEST2");
	RegularFile file3 = (RegularFile)fs.create("TEST3");
	RegularFile file4 = (RegularFile)fs.create("TEST4");
	fs.unmount();
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
    
}
