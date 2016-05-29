package test.fsmulti;

import jx.fs.*;
import jx.zero.*;
import jx.zero.debug.*;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.DataInputStream;

class Main {
    
    FS fs;
    Naming naming = InitialNaming.getInitialNaming();
    Memory buffer;

    public static void main (String[] args) throws Exception {
	new Main(args);
    }

     public Main(String[] args) throws Exception {
	 String fsname=args[0];
	    fs = (FS)LookupHelper.waitUntilPortalAvailable(naming, fsname);
	    FileSystem jfs = (jx.fs.FileSystem)LookupHelper.waitUntilPortalAvailable(naming, "JavaFS");
	    if (jfs==null) throw new Error("no fs");

	    // create buffer
	    MemoryManager memoryManager = (MemoryManager)InitialNaming.getInitialNaming().lookup("MemoryManager");
	    buffer = memoryManager.alloc(1024);

	    test1();

	    jfs.release();

     }

    void test0() throws Exception {
	    for(int i=0; i<10; i++) {
		createFile(fs, "index"+i+".html", 
			   "<html><head><title>JX index.html Testseite (FILEXX)</title><body bgcolor=ffffff>\n\n" +
			   "<center><h2>Herzlich willkommen auf der JX-FILE-Testseite</h2></center><br><br>\n" +
			   "Link zur <a href=page2.html>zweiten</a> Seite</body></html>\n");
	    }
    }

    void test1() throws Exception {
	Thread threads[] = new Thread[10];
	for(int i=0; i<10; i++) {
	    final int n=i;
	    threads[i] = new Thread() {
		    public void run() {
			try {
			    String name = "F"+n;
			    fs.create(name, InodeImpl.S_IWUSR|InodeImpl.S_IRUGO);
			    Inode inode = (Inode)fs.lookup(name);
			    inode.write(buffer, 0, 10);
			    for(int j=0; j<10; j++) {
				inode.read(buffer, 0, 10);
			    }
			    inode.decUseCount();
			} catch(Exception e) { throw new Error(); }
		    }
		};
	    threads[i].start();
	}
	for(int i=0; i<10; i++) {
	    threads[i].join();
	}

    }

    private final void createFile(FS fs, String name, String contents) throws Exception {
	fs.create(name, InodeImpl.S_IWUSR|InodeImpl.S_IRUGO);
	
	Inode inode = (Inode)fs.lookup(name);
	byte[] b = contents.getBytes();
	buffer.copyFromByteArray(b, 0, 0, b.length);
	inode.write(buffer, 0, b.length);
	inode.decUseCount();
	
    }

}
