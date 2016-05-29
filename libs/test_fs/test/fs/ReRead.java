package test.fs;

import java.io.IOException;
import jx.zero.Debug;
import jx.zero.Memory;
import jx.fs.*;
import jx.zero.*;


/**
 * Used to investigate re-read performance problems
 */
public class ReRead {
    private static final int MAXBUFFERSIZE   = 1*1024*1024; // 8*1024*1024

    public static void main(String [] args) {
	Naming naming = InitialNaming.getInitialNaming();
	FS fs = (FS)LookupHelper.waitUntilPortalAvailable(naming, args[0]);
	new ReRead(fs);
    }

    public ReRead(FS fs) {
	boolean checkRead = false;
	int reclen = 4 * 1024;
	int filesize = 128 * 1024;
	int starttime, endtime;
	MemoryManager memMgr = (MemoryManager)InitialNaming.getInitialNaming().lookup("MemoryManager");
	Profiler profiler = ((Profiler)InitialNaming.getInitialNaming().lookup("Profiler"));
	Clock clock = (Clock)InitialNaming.getInitialNaming().lookup("Clock");
	Memory buffer = memMgr.allocAligned(MAXBUFFERSIZE, 4);
	Memory buffer1 = memMgr.allocAligned(MAXBUFFERSIZE, 4);
	try {
	fs.create("iozone.tmp", InodeImpl.S_IWUSR|InodeImpl.S_IRUGO);
	InodeImpl inode = (InodeImpl)fs.lookup("iozone.tmp");
	for(int i=0; i<filesize>>2; i++) {
	  buffer1.set32(i, i);
	  }
	inode.write(buffer1, 0, filesize);
	inode.read(buffer, 0, reclen);
	if (checkRead) {
	  for(int i=0; i<reclen>>2; i++) {
	    if (buffer.get32(i) != i) 	throw new Error();
	  }
	}
	inode.read(buffer, 0, reclen);

	starttime = clock.getTicks_low();
	inode.read(buffer, 0, reclen);
	endtime = clock.getTicks_low();
	Debug.out.println("Readtime for one 4KB block="+(endtime-starttime)+" cycles");

	Debug.out.println("StartAddr=");
	buffer.copyToMemory(buffer1, 0, 0, reclen);
	starttime = clock.getTicks_low();
	for(int i=0; i<10; i++) {
	    buffer.copyToMemory(buffer1, 0, 0, reclen);
	}
	endtime = clock.getTicks_low();
	Debug.out.println("Copy time for one 4KB block="+((endtime-starttime)/10)+" cycles");

	int n = 1;
	for(int j=0; j<n; j++) {
	    inode.read(buffer, j*reclen, reclen);
	}
	
	n = filesize / reclen;
	//int RETRIES = 400000;
	int RETRIES = 10000;
	starttime = clock.getTimeInMillis();//System.currentTimeMillis();
	profiler.startSampling();	
	for(int i=0; i<RETRIES; i++) {
	    for(int j=0; j<n; j++) {
		inode.read(buffer, j*reclen, reclen);
	    }
	}
	profiler.stopSampling();
	
	endtime = clock.getTimeInMillis();//System.currentTimeMillis();
	int readtime = endtime-starttime;
	// readtime 
	// (x * 1000*1000) / 1000 = reclen / readtime
	Debug.out.println("Readtime="+readtime);
	int readrate = (reclen/1000) * n * RETRIES / readtime;
	Debug.out.println("ReRead-Rate Reclen="+reclen+": "+readrate + "MB/s");

	profiler.shell();

	} catch(FSException ex) {
	    Debug.out.println("EXCEPTION!");
	}
	
    }
}
