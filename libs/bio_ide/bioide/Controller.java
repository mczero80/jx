package bioide;

import jx.zero.*;
import jx.timer.*;

/**
 * Represents one IDE controller.
 * An IDE controller can contain up to two Drives.
 * @author Michael Golm
 * @author Andreas Weissel
 */
class Controller {

    /** drive information */
    public  Drive[]   drives;

    /** table of memory addresses of main memory that is accessed during a DMA */
    public  Memory    dmatable;

    /** base address of DMA registers (PIIX) */
    public  int       dma_base;

    /** controller irq */
    private int       irq_nr;

    /** interface name "ide0" */
    public  String    name;

    /** controller number (used by interrupt handler) */
    public  int       index;

    /** controller physically present? */
    private  boolean   present;

    /** base I/O port address */
    private int       io_base;

    /** normally io_base + 0x206 */
    private int       ctl_port;

    /** current command (queued) */
    private Operation operation;

    IDEIntrHandler       handlerObj;
    FirstLevelIrqHandler handler;

    private WaitTimer waittimer;

    private static final int ERROR_MAX          = 8;      // max. number read/write errors
    private static final int ERROR_RESET        = 3;      // reset controller after 4. error

    // Basisregister der Controller
    private static final int REG_BASE0          = 0x1F0;  // base register controller 0
    private static final int REG_BASE1          = 0x170;  // base register controller 1

    // Interrupt request lines
    private static final int AT_IRQ0            = 14;     // interrupt controller 0
    private static final int AT_IRQ1            = 15;     // interrupt controller 1

    // Read/Write registers
    private static final int  IDE_OFF_DATA      =  0;      // data register (as offset from base register)
    private static final int  IDE_OFF_FEATURE   =  1;      // set drive features
    private static final int  IDE_OFF_COUNT     =  2;      // number of sectors to transmit
    private static final int  IDE_OFF_SECTOR    =  3;      // number of sector
    private static final int  IDE_OFF_LO_CYL    =  4;      // cylinder number low byte 
    private static final int  IDE_OFF_HI_CYL    =  5;      // cylinder number high byte
    private static final int  IDE_OFF_LDH       =  6;      // LBA, drive, head

    public  static final byte LDH_DEFAULT       =  (byte)0xA0;   // 101xxxxx == ECC active, 512 bytes / sector

    // Read only registers
    private static final int  IDE_OFF_STATUS    =  7;      // status register
    public  static final byte   STATUS_BSY      =  (byte)0x80;   // controller busy
    public  static final byte   STATUS_RDY      =  0x40;   // drive ready
    public  static final byte   STATUS_WFT      =  0x20;   // write error
    public  static final byte   STATUS_SKC      =  0x10;   // seek complete (obsolete)
    public  static final byte   STATUS_DRQ      =  0x08;   // ready for transfer
    public  static final byte   STATUS_ERR      =  0x01;   // error (cf. IDE_OFF_ERROR)

    private static final int  IDE_OFF_ERROR     =  1;      // error register
    private static final byte   ERROR_BBK       =  (byte)0x80;   // sector marked as bad by host
    private static final byte   ERROR_UNC       =  0x40;   // non maskable data error

    // Write only registers
    private static final int  IDE_OFF_PRECOMP   =  1;      // precompensation
    private static final int  IDE_OFF_COMMAND   =  7;      // command

    private static final int  IDE_OFF_CTL_REG   = 0x206;   // control registrer
    public  static final byte   CTL_EIGHTHEADS  =  0x08;   // more than 8 heads
    public  static final byte   CTL_RESET       =  0x04;   // controlle reset
    public  static final byte   CTL_INTDISABLE  =  0x02;   // disable interrupts

    private static final int  PAGE_SIZE         = 4096;    // size of DMA table
    private static final int  PRD_BYTES         = 8;       // length of one entry in DMA table
    private static final int  PRD_ENTRIES       = PAGE_SIZE / PRD_BYTES;   // number of entries in DMA table

    public Controller(IDEDeviceImpl ide, int index) {
	this.index = index;
	if (index == 0) {
	    io_base = REG_BASE0;
	    irq_nr = AT_IRQ0;
	    name = "ide0";
	} else {
	    io_base = REG_BASE1;
	    irq_nr = AT_IRQ1;
	    name = "ide1";
	}
	ctl_port = io_base + IDE_OFF_CTL_REG;
	present = false;
	operation = null;
	drives = new Drive[IDEDeviceImpl.MAX_DRIVES];
	for (int unit = 0; unit < IDEDeviceImpl.MAX_DRIVES; unit++)
	    drives[unit] = new Drive(ide, this, unit);
	handlerObj = new IDEIntrHandler(this);
	handler = handlerObj;
	waittimer = new WaitTimer(this);
    }

    public void    setFeatureReg(byte value)  { Env.ports.outb(io_base + IDE_OFF_FEATURE, value); }
    public void    setCountReg(byte value)    { Env.ports.outb(io_base + IDE_OFF_COUNT, value); }
    public void    setSectorReg(byte value)   { Env.ports.outb(io_base + IDE_OFF_SECTOR, value); }
    public void    setLoCylReg(byte value)    { Env.ports.outb(io_base + IDE_OFF_LO_CYL, value); }
    public void    setHiCylReg(byte value)    { Env.ports.outb(io_base + IDE_OFF_HI_CYL, value); }
    public void    setLDHReg(byte value)      { Env.ports.outb(io_base + IDE_OFF_LDH, value); }
    public byte    getLDHReg()                { return (Env.ports.inb(io_base + IDE_OFF_LDH)); }
    public void    setCommandReg(byte value)  { Env.ports.outb(io_base + IDE_OFF_COMMAND, value); }
    public void    setCTLReg(byte value)      { Env.ports.outb(ctl_port, value); }
    public boolean statusBusy()               { return ((Env.ports.inb(io_base + IDE_OFF_STATUS) & STATUS_BSY) > 0); }
    public byte    getStatus()                { return (Env.ports.inb(io_base + IDE_OFF_STATUS)); }

    /** transfer data from controller to main memory using programmed I/O */
    public void    inputData(Memory buffer, int offset, int wcount) {
	inputData(io_base + IDE_OFF_DATA, buffer, offset, wcount); // TODO Env.ports.inputData
    }
    /** transfer data from main memory to controller using programmed I/O */
    public void    outputData(Memory buffer, int offset, int wcount) {
	outputData(io_base + IDE_OFF_DATA, buffer, offset, wcount); // TODO Env.ports.outputData
    }

    /**
     * Wait until controller status is good.
     *
     * @param good    state to wait for
     * @param bad     bad state (should not happen)
     * @param timeout timeout in millisec
     * @return <code>false</code>, if timeout or bad status, else <code>true</code>.
     */
    public boolean waitFor(int good, int bad, int timeoutMillis) {
	byte stat = getStatus();
	if ((stat & (good | bad)) == good) return true;
	if ((stat & STATUS_BSY) == 0) {
	    //ideError(drive, "status error", stat);
	    return false;
	}

	timeoutMillis += Env.clock.getTicks()*10;
	while (Env.clock.getTicks()*10 < timeoutMillis) {
	    if ((getStatus() & (good|bad)) == good)
		return true;
	    Env.sleepManager.mdelay(10); // may not be necessary
	}
	return false;
    }

    /**
     * Activate the timeout that terminates the operation after a certain time.
     */
    public void setTimeout(int sec, int msec) {
	//Env.timerManager.addMillisTimer(sec*1000+msec, waittimer, null); 
    }

    /**
     * Deactivate the timeout if operation was completed before timeout
     */
    public void stopTimeout() {
	//Env.timerManager.deleteTimer(waittimer);
    }

    /**
     * Enqueue operation.
     * If controller is ready, operation starts immediately.
     *
     * @param newOperation the operation
     * @param front         <code>true</code> prepend at front of queue
     *                      <code>false</code> append at end of queue
     */
    public void queueOperation(Operation newOperation, boolean front) {
	try {
	    if (operation == null) {
		operation = newOperation;
		if (Env.verboseCTRL) {
		    Env.cpuManager.dump("Controller.queueOperation this:", this);
		    Env.cpuManager.dump("Controller.queueOperation op:", operation);
		}
		operation.startOperation();
		if (Env.verboseCTRL) Debug.out.println("queueOperation: nach startOperation");
	    } else {
		if (Env.verboseCTRL) Debug.out.println("queueOperation: operation != null");
		/*
		// synchronous operation
		if (operation != null) Debug.out.println("WAITING FOR PREVIOUS OP TO COMPLETE!");
		while (operation != null)
		;
		if (Env.verboseCTRL) Debug.out.println("queueOperation: operation now == null");
		operation = newOperation;
		operation.startOperation();
		*/
		
		
		if (front) {
		    newOperation.next = operation;
		    operation = newOperation;
		} else {
		    Operation last = operation;
		    while (last.next != null)
			last = last.next;
		    last.next = newOperation;
		}
		Debug.out.println("operation queued");
	    } 
	}catch (IDEException e) {
	    e.printStackTrace();
	    throw new Error("IDEException");
	}
    }

    /**
     * Start next operation in queue. Return immediately if queue is empty.
     */
    public void nextOperation() {
	if (operation == null)
	    return;
	operation = operation.next;
	if (operation != null) {
	    Debug.out.println("nextOperation(): starting nextOperation");
	    try {
		operation.startOperation();
	    } catch(IDEException e) {
		Debug.out.println("IDEException in Controller!");
		throw new Error();
	    }
	}
    }

    /**
     * Test if controller exists and what drives are connected.
     */
    public boolean identify() {
	byte r = Env.ports.inb(io_base + IDE_OFF_LO_CYL);
	setLoCylReg((byte)~r);
	// contents of register does not change -> controller not available
	if (Env.ports.inb(io_base + IDE_OFF_LO_CYL) == r) {
	    present = false;
	} else {
	    Debug.out.println("Found " + name + " (" + Integer.toHexString(io_base) + "), use IRQ " + irq_nr);
	    present = true;
	}

	for (int unit = 0; unit < IDEDeviceImpl.MAX_DRIVES; unit++) {
	    Drive drive = drives[unit];
	    Debug.out.println("DRIVE: "+unit);
	}
	
	for (int unit = 0; unit < IDEDeviceImpl.MAX_DRIVES; unit++) {
	    Drive drive = drives[unit];
	    try {
		drive.identify();
		if (drive.present) {
		    drive.setup();
		    present = true;
		}
	    } catch(IDEException e) {
		Debug.out.println("DRIVE: "+unit+" could not be identified+initialized");
	    }
	}
	return true;
    }

    /**
     * Setup controller: install IRQ handler
     */
    public void setup() {
	Debug.out.println("setup: irq_no="+irq_nr);
	Env.cpuManager.waitUntilBlocked(handlerObj.irqCtx);
	Env.irq.installFirstLevelHandler(irq_nr, handler);
	Env.irq.enableIRQ(irq_nr);
    }

    /**
     * Error occured. Reset controller and/or terminate running operation.
     */
    public void ideError(Drive drive, String msg, byte stat) {
	int flags;

	Debug.out.println(drive.name + " error: " + msg);

	if (operation == null)
	    return;

	if ((stat & (STATUS_BSY | STATUS_ERR)) == STATUS_ERR) {
	    operation.errors |= ERROR_RESET;
	    /* falls Sektor defekt (ERROR_BBK) oder nicht korrigierbarer Datenfehler (ERROR_UNC), den
	       Fehlerstatus so setzen, dass der aktuelle Auftrag abgebrochen wird */
	    if ((Env.ports.inb(io_base + IDE_OFF_ERROR) & (ERROR_BBK | ERROR_UNC)) > 0)
		operation.errors |= ERROR_MAX;
	}
	
	if ((stat & (STATUS_BSY | STATUS_DRQ)) > 0)
	    operation.errors |= ERROR_RESET; // Timing-Problem
	
	if (operation.errors >= ERROR_MAX) {
	    operation.endOperation(false);  // Auftrag abbrechen
	} else {
	    if ((operation.errors & ERROR_RESET) == ERROR_RESET) {
		operation.errors++;
      
		/* Laufwerk-Reset ausfuehren
		   Ungluecklicherweise wird beim Laufwerksreset fuer alle Laufwerke am selben
		   Interface ein Reset ausgeloest.
		   Beim Reset wird kein Interrupt erzeugt, wenn die Operation beendet ist,
		   also muessen wir das Statusregister abfragen. Da ein Reset bis zu 30 Sekunden
		   dauern kann, verwenden wir kein busy-waiting, sondern einen Timer, der alle
		   50 msec aufgerufen wird.
		*/
		
		//flags = irq.clearIFlag();

		ResetOperation reset_operation = new ResetOperation(this, drive, 30*1000); // 30 sec
		queueOperation(reset_operation, true);
		setTimeout(0, 50); // 50 msec
		
		//irq.setFlags(flags);
		
		if (drive.using_dma) {
		    drive.using_dma = false;
		    Debug.out.println("" + drive.name + ": DMA deactivated");
		}

		return;
	    }
	    operation.errors++;
	}
    }

    /**
     * Initialize a DMA table.
     */
    public boolean buildDMATable(Memory buffer, int addrcount) {
	int addr, count = 0, size = addrcount, bcount, off = 0;

	addr = buffer.getStartAddress();
	if (Env.verboseCTRL) Debug.out.println("DMA startaddr "+Integer.toHexString(addr));

	while (size > 0) {
	    if (++count >= PRD_ENTRIES) {
		return false; // fuer diesen Auftrag PIO verwenden
	    } else {
 		bcount = 0x10000 - (addr & 0xffff);
		if (bcount > size)
		    bcount = size;
		
		dmatable.set32(off++, addr);
		dmatable.set32(off++, (bcount & 0xffff));
		addr += bcount;
		size -= bcount;
	    }
	}

	if (count > 0) {
	    off--;
	    dmatable.set32(off, dmatable.get32(off) | 0x80000000); // set End-Of-Table (EOT) bit 
	    return true;
	}
	throw new Error(); // better use PIO
    }

    /**
     * Reserver DMA registers and create DMA table
     */
    public void initTritonDma(int base) {
	if (Env.verboseCTRL) Debug.out.print("    " + name + ": BM-DMA (Basisadressen " + Integer.toHexString(base) + "-" + Integer.toHexString(base + 7) + ")");
	dma_base = base;
	if (dmatable == null) {
	    dmatable = Env.memoryManager.allocAligned(PAGE_SIZE,PAGE_SIZE);
	    if (Env.verboseCTRL) Debug.out.println("DMATABLE startaddr "+Integer.toHexString(dmatable.getStartAddress()));
	}
	/* The "Busmaster IDE Descriptor Table Pointer Register" must be dword-aligned and must not cross
	   a 4-Kbyte border -> align at PAGE_SIZE*/
	//}
	Debug.out.println("");
    }


    public void inputData(int port, Memory m, int offset, int wcount) {
        for(int i=0; i<wcount; i++) {
            int data = Env.ports.inl(port);
            m.set32(i+offset, data);
        }
    }

    public void outputData(int port, Memory m, int offset, int wcount) {
        for(int i=0; i<wcount; i++) {
            int data = m.get32(i+offset);
            Env.ports.outl(port, data);
        }
    }

    public boolean isPresent() { return present; }


    final public Operation getCurrentOperation() {
	if (Env.verboseCTRL) {
	    Env.cpuManager.dump("Controller:", this);
	    Env.cpuManager.dump("Controller op:", operation);
	}
	return operation;
    }


    final public String getName() { return name; }
}



class WaitTimer implements TimerHandler {

    Controller controller;

    WaitTimer(Controller controller) {
	this.controller = controller;
    }

    public void timer(Object arg) {	
	Debug.out.println("WaitTimer");
    }
}
