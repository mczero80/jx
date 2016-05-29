/* Test and measure the DEP mechanism.
 */
package test.portal.perf;

import jx.zero.*;
import jx.zero.debug.*;
import jx.fs.*;
import jx.secmgr.*;


class Test{
    int a;
    public void m() {}
    public final void finalm() {}
    public static void staticm() {}
}

public class PortalPerf {
    public static int EMPTY_ITERATIONS = 5000000;
    public static int VCALL_ITERATIONS = 1000000;
    public static int EVENT_ITERATIONS = 100000;
    public static int PORTAL_ITERATIONS = 10000;
    //   public static int PORTAL_ITERATIONS = 1500000;
    public static int PORTAL_ONEINT_ITERATIONS = 150000;
    public static int NEW_ITERATIONS = 30000;
    public static int COPY_ITERATIONS = 1000;

    public static boolean doGC = false;
    public static boolean measureEventLogging = false;

    public static void main(String args[]) {
	if (args.length >0) {
	    new PortalPerf(true);
 	} else
	    new PortalPerf(false);
    }

    PortalPerf(boolean fstest) {
	Naming naming = InitialNaming.getInitialNaming();
	int startTime, endTime;
	Clock clock = (Clock) naming.lookup("Clock");
	Profiler profiler = ((Profiler)naming.lookup("Profiler"));
	CPUManager cpuManager = (CPUManager) naming.lookup("CPUManager");
	DomainManager domainManager = (DomainManager) naming.lookup("DomainManager");

	Debug.out.println("test.portal.perf.PortalPerf speaking.");

	    TargetDomain targetDomain = null;
	    FilesystemInterface FStargetDomain = null;
	if (fstest) {
	    DomainStarter.createDomain("BlockIO", "test_fs.jll", "test/fs/BioRAMDomain", 2000000, new String[] {"BlockIO"});
	    DomainStarter.createDomain("FS", "fs_user_impl.jll", "jx/fs/FSInterfaceDomain", 6000000, new String[] {"BlockIO"},naming ,"jx.secmgr.FileSystemSecurityPolicy");
	    CentralSecurityManager secmgr = (CentralSecurityManager)naming.lookup("SecurityManager");
	    if ( secmgr == null ) throw new Error("CentralSecurityManager not found");
	    secmgr.addDomainAndPrincipal(domainManager.getCurrentDomain(), new Principal_impl("User",1));
	    domainManager.getCurrentDomain().clearTCBflag();
	    do {
		Thread.yield();
		FStargetDomain = (FilesystemInterface)naming.lookup("FSInterface");
	    } while(FStargetDomain == null);
	    Debug.out.println("Got Test portal");
	} else {
	    DomainStarter.createDomain("TargetDomain","test_portal","test/portal/perf/TargetDomain_Impl",120*1024*1024,naming);
	    do {
		Thread.yield();
		targetDomain = (TargetDomain) naming.lookup("Target");
	    } while(targetDomain == null);
	    Debug.out.println("Got Test portal");
	}
	Test o = new Test();

	int iter = EMPTY_ITERATIONS;
	Debug.out.println("Iterations per test: "+iter);
	// measure null loop
	startTime = clock.getTimeInMillis();
	for(int i=0; i<iter; i++)
	    ;
	endTime = clock.getTimeInMillis();
	Debug.out.println("Operation null-loop (ns): "+((endTime-startTime))/(iter/1000000));

	// measure virtual call
	startTime = clock.getTimeInMillis();
	iter = VCALL_ITERATIONS;
	for(int i=0; i<iter; i++) 
	    o.m();
	endTime = clock.getTimeInMillis();
	Debug.out.println("Operation loop + virtual-call (ns): "+((endTime-startTime))*1000/(iter/1000));

	// measure non-virtual call
	startTime = clock.getTimeInMillis();
	iter = VCALL_ITERATIONS;
	for(int i=0; i<iter; i++) 
	    o.finalm();
	endTime = clock.getTimeInMillis();
	Debug.out.println("Operation loop + non-virtual-call (ns): "+((endTime-startTime))*1000/(iter/1000));

	// measure static call
	startTime = clock.getTimeInMillis();
	iter = VCALL_ITERATIONS;
	for(int i=0; i<iter; i++) 
	    o.staticm();
	endTime = clock.getTimeInMillis();
	Debug.out.println("Operation loop + static-call (ns): "+((endTime-startTime))*1000/(iter/1000));

	// measure assignment
	startTime = clock.getTimeInMillis();
	iter = VCALL_ITERATIONS;
	for(int i=0; i<iter; i++) 
	    o.a = 1;
	endTime = clock.getTimeInMillis();
	Debug.out.println("Operation loop + assignment (ns): "+((endTime-startTime))*1000/(iter/1000));

	// measure object creation
	startTime = clock.getTimeInMillis();
	iter = NEW_ITERATIONS;
	for(int i=0; i<iter; i++) 
	    new Test();
	endTime = clock.getTimeInMillis();
	Debug.out.println("Operation loop + new-object (ns): "+((endTime-startTime))*1000/(iter/1000));

	int event=cpuManager.createNewEvent("test_event");
	if (measureEventLogging) {
	    // measure event logging
	    startTime = clock.getTimeInMillis();
	    iter = EVENT_ITERATIONS;
	    for(int i=0; i<iter; i++) 
		cpuManager.recordEvent(event);
	    endTime = clock.getTimeInMillis();
	    Debug.out.println("Operation loop + event logging (ns): "+((endTime-startTime))*1000/(iter/1000));
	    Debug.out.println("Operation loop + event logging: "+(endTime-startTime));
	} else {
	    // use test_event only to mark beginning of portal test
		cpuManager.recordEvent(event);	    
	}
	Debug.out.println("Starting portal performance test ");
	// measure portal call
	if (fstest){
	    Debug.out.println("   FS access");
	    try {
	    Directory d = (Directory)FStargetDomain.openRootDirectoryRW();
	    iter = PORTAL_ITERATIONS;
	    startTime = clock.getTimeInMillis();
	    profiler.startSampling();
	    for(int i=0; i<iter; i++) 
		d.getAttribute();
	    profiler.stopSampling();
	    endTime = clock.getTimeInMillis();
	    } catch (Exception e){ Debug.out.println("Exception!"); }
	    Debug.out.println("Operation loop + portal call (repeated "+iter+" times) (ns): "+((endTime-startTime))*1000/(iter/1000));
	} else {
	    Debug.out.println("   No FS access");
	    TestObject o1 = new TestObject() {
		    public int m() {return 0;}
		};
	    iter = PORTAL_ITERATIONS;
	    if (doGC) {
		domainManager.gc(domainManager.getCurrentDomain());
		targetDomain.gc();
	    }
	    startTime = clock.getTimeInMillis();
//	    profiler.startSampling();
	    for(int i=0; i<iter; i++) {
		//targetDomain.test(o1, 42);
		targetDomain.noparam();
	    }
//	    profiler.stopSampling();
	    endTime = clock.getTimeInMillis();
	    Debug.out.println("Operation loop + portal call (repeated "+iter+" times) (ns): "+((endTime-startTime))*1000/(iter/1000));

	    iter = COPY_ITERATIONS;
	    NullObject nobj = new NullObject();
	    if (doGC) {
		domainManager.gc(domainManager.getCurrentDomain());
		targetDomain.gc();
	    }
	    startTime = clock.getTimeInMillis();
//	    profiler.startSampling();
	    for(int i=0; i<iter; i++) {
		targetDomain.objparam(nobj);
	    }
//	    profiler.stopSampling();
	    endTime = clock.getTimeInMillis();
	    Debug.out.println("Operation loop + portal call 1 param (object with no fields) (repeated "+iter+" times) (ns): "+((endTime-startTime))*1000/(iter/1000));

	    int result[] = new int[100];
	    int num[] = new int[100];
	    int nresult=0;
	    CycleTime starttimec = new CycleTime();
	    CycleTime endtimec = new CycleTime();
	    CycleTime diff = new CycleTime();
	    for(int listlen=1; listlen<=10000; listlen*=2) {
		NullObject[] lobj = new NullObject[listlen];
		for(int j=0; j<listlen; j++) {
		    lobj[j] = new NullObject();
		}
	    if (doGC) {
		domainManager.gc(domainManager.getCurrentDomain());
		targetDomain.gc();
	    }
		//startTime = clock.getTimeInMillis();
		clock.getCycles(starttimec);


		//	    profiler.startSampling();
		for(int i=0; i<iter; i++) {
		    targetDomain.arrparam(lobj);
		}
		//	    profiler.stopSampling();
		//endTime = clock.getTimeInMillis();
		clock.getCycles(endtimec);
		clock.subtract(diff, endtimec, starttimec);
		Debug.out.println("Operation loop + portal call 1 array param (array contains "+listlen+" null objects) (repeated "+iter+" times) (microsec): "+(clock.toMicroSec(diff)));

		//Debug.out.println("Operation loop + portal call 1 array param (array contains "+listlen+" null objects) (repeated "+iter+" times) (ns): "+((endTime-startTime))*1000/(iter/1000));
		num[nresult] = listlen;
		result[nresult++]=clock.toMicroSec(diff);
	    }

	    int result2[] = new int[100];
	    int num2[] = new int[100];
	    int nresult2=0;
	    for(int listlen=1; listlen<=10000; listlen*=2) {
		ListObject lobj = new ListObject(listlen);
	    if (doGC) {
		domainManager.gc(domainManager.getCurrentDomain());
		targetDomain.gc();
	    }
		//startTime = clock.getTimeInMillis();
		clock.getCycles(starttimec);
		//	    profiler.startSampling();
		for(int i=0; i<iter; i++) {
		    targetDomain.listparam(lobj);
		}
		//	    profiler.stopSampling();
		clock.getCycles(endtimec);
		clock.subtract(diff, endtimec, starttimec);
		//		endTime = clock.getTimeInMillis();
		//Debug.out.println("Operation loop + portal call 1 param (object list with "+listlen+" null objects) (repeated "+iter+" times) (ns): "+((endTime-startTime))*1000/(iter/1000));
		num2[nresult2] = listlen;
		//result2[nresult2++]=((endTime-startTime))*1000/(iter/1000);
		result2[nresult2++]=clock.toMicroSec(diff);
	    }
	    Debug.out.println("Array iterations "+iter);
	    for(int i=0; i<nresult; i++) {
		Debug.out.println(num[i]+" "+result[i]);
	    }
	    Debug.out.println("List iterations "+iter);
	    for(int i=0; i<nresult2; i++) {
		Debug.out.println(num2[i]+" "+result2[i]);
	    }
	}
	
	Debug.out.println("Success");
	return;
    }
}
