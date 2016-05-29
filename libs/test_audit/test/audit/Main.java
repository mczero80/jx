package test.audit;

import jx.zero.*;
import jx.bio.*;
import auditfs.*;

public class Main {
    public static void main(String [] args) {
	Naming naming = InitialNaming.getInitialNaming();
	String bioname = args[0];

	//BlockIO bio;
	//Debug.out.println("Looking for Block I/O device  "+bioname);
	//CPUManager cpuManager = (CPUManager) naming.lookup("CPUManager");
	//while((bio=(BlockIO)naming.lookup(bioname)) == null) cpuManager.yield();

	BlockIO bio = (BlockIO) LookupHelper.waitUntilPortalAvailable(naming, bioname);
	
	CycleTime starttimec = new CycleTime();
	CycleTime endtimec = new CycleTime();
	CycleTime diff = new CycleTime();

	AuditStore audits = new AuditStore(naming, bio, false, 0);
	/*
	  Debug.out.println("Erasing old audits.");
	  audits.erase();
	  Debug.out.println("Done.");
	*/

	int naudits = 200;
	Clock clock = (Clock)InitialNaming.getInitialNaming().lookup("Clock");
	clock.getCycles(starttimec);
	for(int i=0; i<naudits; i++) {
	    audits.writeAuditLog("pc1", "JXNET", "Hallo"+i);
	}
	clock.getCycles(endtimec);
	clock.subtract(diff, endtimec, starttimec);
	int audittime =  clock.toMicroSec(diff);
	int auditrate = ((naudits*1000*1000)/audittime); 

	Debug.out.println("Finished writing. Time for 1 audit = "+(audittime/naudits)+" microsec, Auditrate = "+auditrate +" audits/sec");
    }
}
