package bioide;

import jx.zero.*;

/**
 * Interrupt handler.
 * @author Michael Golm
 * @author Andreas Weissel
 */
class IDEIntrHandler implements FirstLevelIrqHandler, Service {
    private Controller controller;
    CPUState irqCtx;
    private boolean missed;

    public IDEIntrHandler(Controller controller) {
	this.controller = controller;
	installIRQThread();
	while(irqCtx == null) {
	    Env.cpuManager.yield();
	}
    }

    /**
     * Calls the real handler (operation.handler)
     */
    
    public void interrupt() {
	if( Env.verboseIRQ )
	    Env.cpuManager.dump("ctx in irq:", irqCtx);
	if (! Env.cpuManager.unblock(irqCtx)) {
	    missed = true;
	    Debug.out.println("MISSED IRQ");
	}
    }
    
    private void installIRQThread() {
	Naming naming = InitialNaming.getInitialNaming();
	Env.cpuManager.start(Env.cpuManager.createCPUState(new ThreadEntry() {
	    public void run() {
	      Env.cpuManager.setThreadName("IDE-2nd-IRQ "+controller.getName());
	      irqCtx = Env.cpuManager.getCPUState();
		for(;;) {
		    if( Env.verboseIRQ ) {
		        Debug.out.println("blocking");
		        Env.cpuManager.dump("ctx in run:", irqCtx);
		    }
		    if (! missed) {
			Env.cpuManager.block();
		    } else {
			missed = false;
		    }
		    interruptHandler();
		}
	    }
	}));
    }
    
    public void interruptHandler() {
        if( Env.verboseIRQ )  Env.cpuManager.dump("IDEIntrHandler: Interrupt:", this);
	Operation operation = controller.getCurrentOperation();
	if (operation != null) {
	    controller.stopTimeout();
	    if (Env.verboseIRQ) {
		Debug.out.println("calling operation.handler");
		Env.cpuManager.dump("operation:", operation);
	    }
	    operation.handler();
	} else {
	    Debug.out.println("IDEIntrHandler: unexpected interrupt");
	}
    }
}
