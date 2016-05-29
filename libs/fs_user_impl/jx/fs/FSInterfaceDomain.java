package jx.fs;

import jx.zero.*;
import jx.zero.debug.*;
import jx.bio.BlockIO;
import bioram.BlockIORAM;
import javafs.*;
import jx.fs.FS;
//import vfs.FSImpl;
import jx.fs.FilesystemInterface;
import jx.fs.FilesystemImpl;
import jx.fs.Inode;
import jx.fs.FSException;
import java.util.*;

/* DummyClock */
import jx.zero.Clock;
import jx.zero.CycleTime;
final class MyDummyClock implements Clock {
    int t;
    public int getTimeInMillis() { return t++; }
    public long getTicks() {return 0;}
    public int getTicks_low(){return 0;}
    public int getTicks_high(){return 0;}
    public  void getCycles(CycleTime c){}
    public void subtract(CycleTime result, CycleTime a, CycleTime b){}
    public int toMicroSec(CycleTime c) {return 0;}
    public int toNanoSec(CycleTime c) {return 0;}
    public int toMilliSec(CycleTime c) {return 0;}
}
/*=============*/

public class FSInterfaceDomain {

    Naming naming;

    static final int EXT2FS_BLOCKSIZE = 1024;

    public static void init(final Naming naming, String[] args) {
	DebugChannel d = (DebugChannel) naming.lookup("DebugChannel0");
	CPUManager cpuManager = (CPUManager) naming.lookup("CPUManager");
	Debug.out = new DebugPrintStream(new DebugOutputStream(d));

	String bioname = args[0];

	Debug.out.println("Domain FSInterfaceDomain speaking.");
	cpuManager.setThreadName("FSDomain-Main");

	BlockIO bio = (BlockIO) LookupHelper.waitUntilPortalAvailable(naming, bioname);

	new FSInterfaceDomain(naming,bio);
    }

    FSInterfaceDomain(final Naming naming, BlockIO bio) {
	try {
	    this.naming = naming;

	    Debug.out.println("Create FileSystem on BlockIO");
	    Debug.out.println("Capacity: " + bio.getCapacity());

	    final javafs.FileSystem jfs = new javafs.FileSystem();
	    Clock clock = new MyDummyClock();
	    jfs.init(bio, new buffercache.BufferCache(bio, clock, 800, 1000, 100, EXT2FS_BLOCKSIZE), clock);	    	    
	    jfs.build("TestFS", 1024);

	    final FilesystemImpl ifs = new FilesystemImpl(jfs);

	    Debug.out.println("FileSystem is ready !!!");
	    naming.registerPortal(ifs, "FSInterface");

	} catch(Exception e) {
	    e.printStackTrace();
	    throw new Error();
	}
    }
}
