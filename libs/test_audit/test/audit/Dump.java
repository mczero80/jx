package test.audit;

import jx.zero.*;
import jx.bio.*;
import auditfs.*;

public class Dump {
    public static void main(String [] args) {
	Naming naming = InitialNaming.getInitialNaming();
	String bioname = args[0];

	BlockIO bio;
	Debug.out.println("Looking for Block I/O device  "+bioname);
	CPUManager cpuManager = (CPUManager) naming.lookup("CPUManager");
	while((bio=(BlockIO)naming.lookup(bioname)) == null) cpuManager.yield();

	//	BlockIO bio = (BlockIO) LookupHelper.waitUntilPortalAvailable(naming, bioname);
	
	AuditStore audits = new AuditStore(naming, bio, false, 0);
	Debug.out.println("Old audits:");
	audits.dump();
    }
}
