package test.fs;

import jx.fs.*;
import jx.zero.*;
import jx.bio.*;
import jx.zero.debug.*;

import javafs.*;
import vfs.*;

import jx.fs.InodeImpl;

import jx.zero.debug.DebugPrintStream;
import jx.zero.debug.DebugOutputStream;

class CreateRemoveSingle {
    static final int BUFFERCACHE_NUMBER_FSBLOCKS = 1 * 1024; /*  kBytes */
    static final int BUFFERCACHE_MAXNUMBER_FSBLOCKS = 1 * 1024; /* kBytes */
    static final int BUFFERCACHE_INCNUMBER_FSBLOCKS = 0; /* 0 MBytes, do not enlarge buffer */
    static final int EXT2FS_BLOCKSIZE = 1024;

    static Tools tools;

    public static void init(Naming naming, String [] args) {
	Debug.out = new DebugPrintStream(new DebugOutputStream((DebugChannel) naming.lookup("DebugChannel0")));

	
		
	FSImpl fs = new FSImpl();

	BlockIO bio = (BlockIO)LookupHelper.waitUntilPortalAvailable(naming, args[0]);

	jx.fs.FileSystem jfs=initFS(bio);


	/*
	fs.mountRoot(jfs, false); // 2. Parameter = read-only  //hda8

	try {
	    fs.mkdir("lost+found", InodeImpl.S_IWUSR|InodeImpl.S_IRUGO|InodeImpl.S_IXUGO);
	} catch(FSException e) {
	    Debug.out.println("ERROR creating lost+found");
	}
	*/	

	try {

	    
	    Inode ino = jfs.getRootInode();
	    ino.mkdir("lost+found", InodeImpl.S_IWUSR|InodeImpl.S_IRUGO|InodeImpl.S_IXUGO);
	    
	    ino.create("blubber0",InodeImpl.S_IWUSR|InodeImpl.S_IRUGO|InodeImpl.S_IXUGO);
	    ino.create("blubber1",InodeImpl.S_IWUSR|InodeImpl.S_IRUGO|InodeImpl.S_IXUGO);
	    ino.decUseCount();

	    tools.checkFS(new AnswerMachine() {
		    public boolean ask(String msg) {
			Debug.out.println(msg);
			throw new Error("NOT IMPLEMENTED");
		    }
		});
		    
	    CreateRemove.dotest(jfs);
	} catch(FSException e) {
	    Debug.out.println("ERROR while testing");
	}
	
	if (jfs != null)
	    jfs.release();
	
	

    }

    
    static jx.fs.FileSystem initFS(BlockIO bio) {
	javafs.FileSystem jfs = new javafs.FileSystem();
	Clock clock = new DummyClock();
	buffercache.BufferCache bufferCache = new buffercache.BufferCache(bio, clock, 
									  BUFFERCACHE_NUMBER_FSBLOCKS, 
									  BUFFERCACHE_MAXNUMBER_FSBLOCKS,
									  BUFFERCACHE_INCNUMBER_FSBLOCKS, EXT2FS_BLOCKSIZE);
	jfs.init(bio, bufferCache, clock);
	
	jfs.build("TestFS", 1024);

	tools = jfs.getTools();

	return jfs;
    }
    

}
