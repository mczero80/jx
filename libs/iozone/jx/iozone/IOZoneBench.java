package jx.iozone;

import jx.zero.*;
import jx.zero.debug.*;
import jx.fs.FS;
import jx.fs.Inode;
import vfs.FSImpl;
import jx.fs.FSException;
import jx.zero.debug.*;
import java.util.*;

public class IOZoneBench {
    public static void main(String[]  args) throws Exception {
	if (args.length != 5) throw new Error("wrong args");
	
	int IOZONE_MIN_FILESIZE = Integer.parseInt(args[1]);
	int IOZONE_MAX_FILESIZE = Integer.parseInt(args[2]);

	int IOZONE_MIN_RECSIZE = Integer.parseInt(args[3]);
	int IOZONE_MAX_RECSIZE = Integer.parseInt(args[4]);

	Naming naming = InitialNaming.getInitialNaming();
	String fsname = args[0];
	jx.fs.FileSystem jfs=null;

	FS fs = (FS)LookupHelper.waitUntilPortalAvailable(naming, fsname);

	new IOZONE(fs, IOZONE_MIN_FILESIZE, IOZONE_MAX_FILESIZE, IOZONE_MIN_RECSIZE, IOZONE_MAX_RECSIZE);
		
	jfs = (jx.fs.FileSystem)naming.lookup("JavaFS");
	if (jfs != null)
	    jfs.release();
    }
}
