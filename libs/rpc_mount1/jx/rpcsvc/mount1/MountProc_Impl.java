package jx.rpcsvc.mount1;

import jx.rpcsvc.nfs2.*;
import jx.zero.*;
import jx.rpc.*;
import jx.fs.*;


public class MountProc_Impl implements MountProc {
    String name;
    byte[] handle;
    FS fs;

    public MountProc_Impl(FS fs) {
	this.fs = fs;
    }

    public void       nullproc() {}
    public FHStatus   mnt(jx.rpcsvc.nfs2.DirPath d) {
	Debug.out.println("MountProc.mnt called");
	Debug.out.println("   -> "+d.data);

	try {
	    jx.fs.Inode homeInode = fs.lookup(d.data);
	    MappedFHandle homeFh = 
		new MappedFHandle(homeInode.getFileSystem().getDeviceID().intValue(),
				  homeInode.getIdentifier(),
				  0);
	    return new FHStatusOK(homeFh.getFHandle());
	} catch (Exception e) {
	    //TODO
	    Debug.out.println("Fehler in mount!");
	    System.out.println("Fehler in mount!");
	    return new FHStatusOK();
	}
// 	if (d.data.equals(name)) {
// 	    FHandle fh = new FHandle(handle);
// 	    return new FHStatusOK(fh); 
// 	}
	//	throw new Error("MOUNT REQUEST FOR UNKNOWN DIR");
    }
    public MountList  dump() {return null; }
    public void       umnt(jx.rpcsvc.nfs2.DirPath d) {
	Debug.out.println("UNMOUNT AUFGERUFEN!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
	try {
	fs.cleanUp(); // TEST
	}catch(FSException ex){throw new Error();}
	
    }
    public void       umntall(){}
    public Exports    export() {return null; }
}
