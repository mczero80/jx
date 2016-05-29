package jx.rtc;

import jx.zero.*;
import jx.zero.debug.*;
import jx.zero.FirstLevelIrqHandler;

/**
 * Driver for the PC real-time clock.
 * Chip: MC146818
 */
public class RTCImpl implements RTC, /*IrqHandler,*/ Service, FirstLevelIrqHandler {
    static final boolean debug = false;
    public final static int IRQ_RTC = 8;

    static final int SELECT = 0x70; // ioport for selecting the register
    static final int RESULT = 0x71; // ioport for reading results

    static final byte REG_CLOCK_CURR_SEC  = (byte)0x00;
    static final byte REG_CLOCK_ALARM_SEC = (byte)0x01;
    static final byte REG_CLOCK_CURR_MIN  = (byte)0x02;
    static final byte REG_CLOCK_ALARM_MIN = (byte)0x03;
    static final byte REG_CLOCK_CURR_HOUR = (byte)0x04;
    static final byte REG_CLOCK_ALARM_HOUR= (byte)0x05;
    static final byte REG_CLOCK_CURR_DAY_OF_WEEK    = (byte)0x06;
    static final byte REG_CLOCK_CURR_DATE_OF_MOUNTH = (byte)0x07;
    static final byte REG_CLOCK_CURR_MONTH          = (byte)0x08;
    static final byte REG_CLOCK_CURR_YEAR           = (byte)0x09;
    static final byte REG_STATUS_A  = (byte)0x0a;
    static final byte REG_STATUS_B  = (byte)0x0b;
    static final byte REG_STATUS_C  = (byte)0x0c;
    static final byte REG_STATUS_D  = (byte)0x0d;
    static final byte REG_EQUIPMENT = (byte)0x14;

    static final byte B_SET = (byte)0x80;
    static final byte B_PIE = (byte)0x40;
    static final byte B_AIE = (byte)0x20;
    static final byte B_UIE = (byte)0x10;
    static final byte B_SQWE= (byte)0x08;
    static final byte B_DM  = (byte)0x04;
    static final byte B_24  = (byte)0x02;
    static final byte B_DSE = (byte)0x01;


    Naming naming;
    Ports ports;
    IRQ irq;
    jx.zero.debug.DebugPrintStream out;
    int interruptCounter = 0;
    CPUState state;
    CPUManager cpuManager;
    AtomicVariable atomic;

    private byte rdReg(byte register) {
	ports.outb_p(SELECT, register);
	return  ports.inb_p(RESULT);
    }
    private void wrReg(byte register, int value) {
	ports.outb_p(SELECT, register);
	ports.outb_p(RESULT, (byte)value);
    }
    private int fromBCD(int bcd) {
	int low = bcd & 0xf;
	int high = (bcd >> 4) & 0xf;
	return high * 10 + low;
    }
    public RTCImpl(final Naming naming) {
	this.naming = naming;
	ports = (Ports) naming.lookup("Ports");
	irq = (IRQ) naming.lookup("IRQ");

	out = Debug.out;

	cpuManager = (CPUManager)naming.lookup("CPUManager");
	
	if (debug) {
	    out.println("RTC: Equipment: "+rdReg(REG_EQUIPMENT));
	    out.println("RTC: Minute: "+fromBCD(rdReg(REG_CLOCK_CURR_MIN)));
	    out.println("RTC: Hour: "+fromBCD(rdReg(REG_CLOCK_CURR_HOUR)));
	}

	irq.installFirstLevelHandler(IRQ_RTC, this);
	if (debug)  Debug.out.println("RTC: installed interrupt handler");	  
	/*
	final IrqHandler irqHndl = (IrqHandler)naming.promoteDEP(this, "jx/zero/IrqHandler");
	naming.startThread(new ThreadEntry() {
	    public void run() {
		((CPUManager)naming.lookup("CPUManager")).receive(irqHndl);
	    }
	});
	irq.installHandler(IRQ_RTC, irqHndl);
	*/

	irq.disableIRQ(IRQ_RTC);
	
	wrReg(REG_STATUS_B, rdReg(REG_STATUS_B) & 0x8f); // disable all rtc interrupts
	

    }
    public int getTime() {
	return 0;
    }

    public void installIntervallTimer(AtomicVariable atomic, CPUState state, int frequency) {
	if (debug)  Debug.out.println("RTC install timer");	  
	if (this.state != null) throw new Error("RTC: timer already installed");
	this.state = state;
	this.atomic = atomic;

	//wrReg(REG_STATUS_B, rdReg(REG_STATUS_B) | B_UIE);
	
	
	wrReg(REG_STATUS_A, (rdReg(REG_STATUS_A) & 0xf0) | frequency);
	wrReg(REG_STATUS_B, rdReg(REG_STATUS_B) | B_PIE);
	
	irq.enableIRQ(IRQ_RTC);
    }

    public void interrupt() {
      if (debug) Debug.out.println("RTC Interrupt");	  
	rdReg(REG_STATUS_C);
	if (state!=null) {
	  // if (debug) Debug.out.println("RTC unblock thread");	  
	    atomic.atomicUpdateUnblock(null, state);
	    //Debug.out.println("RTC: thread is not blocked!!!");
	}
	//irq.enableIRQ(IRQ_RTC);
	/*
	rdReg(REG_STATUS_C);
	interruptCounter++;
	if (interruptCounter == 100) {
	    Debug.out.println("100 RTC Interrupts");	    
	    interruptCounter = 0;
	}
	*/
	/*
	Debug.out.print("RTC: ");
	Debug.out.print(rdReg(REG_STATUS_C));
	Debug.out.println("");
	*/
    }
}
