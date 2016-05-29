package test.nfs;

import jx.zero.*;
import jx.zero.debug.*;

public class LoadGenerator implements Runnable {
    
    //final SleepManager sleepManager = new SleepManagerImpl();
    final CPUManager cpuManager = (CPUManager)InitialNaming.getInitialNaming().lookup("CPUManager");
    boolean ende = false;
    public static void init(Naming naming) {
	DebugChannel d = (DebugChannel) naming.lookup("DebugChannel0");
	CPUManager cpuManager = (CPUManager) naming.lookup("CPUManager");
	Debug.out = new DebugPrintStream(new DebugOutputStream(d));
	Debug.out.println("LoadGenerator Domain speaking.");
	new LoadGenerator();
   }


    public LoadGenerator(){
	Debug.out.println("new LoadGenerator");
	Thread allMyThreads[] = new Thread[100];
	int thr=0;
	
	cpuManager.sleep(15000,0);
	for (int i=0; i < 10; i++){
	    cpuManager.sleep(20000,0);
	    for (int j=0; j < 10; j++)
		(allMyThreads[thr++]=new Thread(this, "Load"+thr)).start();
	    Debug.out.println("Load:"+thr);
	}
	cpuManager.sleep(50000,0);
	//	for (thr = 0; thr <100; thr++)
	//	     allMyThreads[thr].destroy();
	ende = true;
    }
    
    public void run()
    {
	int j;
	while (!ende){
	    cpuManager.sleep(1000,0);
	    for (j=0; j<1000000;j++);
	}
    }
}
