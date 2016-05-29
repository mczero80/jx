package bioide;

import jx.zero.*;
import jx.timer.*;
import timerpc.*;

/**
 * Driver environment.
 * @author Michael Golm
 * @author Andreas Weissel
 */
class Env {
    static Naming               naming;
    static Clock                clock;
    static IRQ                  irq;
    static Ports                ports;
    static MemoryManager        memoryManager;
    static CPUManager           cpuManager;
    static SleepManager         sleepManager;

    // debugging
    final static boolean verboseIO   = false; // io operations
    final static boolean verbosePT   = false; // partition table
    final static boolean verboseDR   = false; // drive
    final static boolean verboseIRQ  = false; // interrupt
    final static boolean verboseCTRL = false; // controller
    final static boolean verboseID   = false; // identify
    
    static void init() {
	naming        = InitialNaming.getInitialNaming();
	clock         = (Clock) naming.lookup("Clock");
	irq           = (IRQ)naming.lookup("IRQ");
	ports         = (Ports)naming.lookup("Ports");
	memoryManager = (MemoryManager)naming.lookup("MemoryManager");
	cpuManager    = (CPUManager)naming.lookup("CPUManager");
	sleepManager  = new SleepManagerImpl();
    }
}

