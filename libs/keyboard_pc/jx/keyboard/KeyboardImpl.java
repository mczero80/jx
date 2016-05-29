package jx.keyboard;

import jx.zero.InitialNaming;
import jx.zero.Ports;
import jx.zero.IrqHandler;
import jx.zero.FirstLevelIrqHandler;
import jx.zero.ThreadEntry;
import jx.zero.debug.*;
import jx.zero.IRQ;
import jx.zero.CPUManager;
import jx.zero.CPUState;

import jx.devices.Keyboard;
import jx.devices.KeyListener;

import java.io.IOException;
import java.util.Vector;
import java.io.OutputStream;
import jx.zero.Service;
import jx.zero.Naming;
import jx.zero.Debug;

/**
 * PC Hardware, Seite 1032
 */
public class KeyboardImpl implements Keyboard, FirstLevelIrqHandler, Service
{

    public final static int IRQ_KEYBOARD = 1;
    public final static int AUX_IRQ = 12;

    /*
     * Keyboard I/O ports.
     */
    public static final byte REG_RDWR = 0x60;	/*
						 * keyboard data & cmds (read/write) 
						 */
    public static final byte REG_STAT = 0x64;	/*
						 * keybd status (read-only) 
						 */
    public static final byte REG_CMD = 0x64;	/*
						 * keybd ctlr command (write-only) 
						 */

    /*
     * Bit definitions for REG_STATUS
     */
    public static final byte STAT_OBUF_FUL = 0x01;	/*
							 * output (from keybd) buffer full 
							 */
    public static final byte STAT_IBUF_FUL = 0x02;	/*
							 * input (to keybd) buffer full 
							 */
    public static final byte STAT_SYSFLAG = 0x04;	/*
							 * "System Flag" 
							 */
    public static final byte STAT_CMD_DATA = 0x08;	/*
							 * 1 = input buf has cmd, 0 = data 
							 */
    public static final byte STAT_KBD_INHIBIT = 0x10;	/*
							 * 0 if keyboard inhibited 
							 */
    public static final byte STAT_AUX_OBUF_FUL = 0x20;	/*
							 * 1 = obuf holds aux device data 
							 */
    public static final byte STAT_TIMEOUT = 0x40;	/*
							 * timout error flag 
							 */
    public static final byte STAT_PARITY_ERROR = (byte) 0x80;	/*
								 * parity error flag 
								 */

    /*
     * Keyboard controller commands (sent to CMD port).
     */
    public static final byte CMD_READ = 0x20;	/*
						 * read controller command byte 
						 */
    public static final byte CMD_WRITE = 0x60;	/*
						 * write controller command byte 
						 */
    public static final byte CMD_DIS_AUX = (byte) 0xa7;	/*
							 * disable auxiliary device 
							 */
    public static final byte CMD_MOUSE_ENABLE = (byte) 0xa8;
    public static final byte CMD_MOUSE_DISABLE = (byte) 0xa7;
    public static final byte CMD_ENB_AUX = (byte) 0xa8;	/*
							 * enable auxiliary device 
							 */
    public static final byte CMD_TEST_AUX = (byte) 0xa9;	/*
								 * test auxiliary device interface 
								 */
    public static final byte CMD_SELFTEST = (byte) 0xaa;	/*
								 * keyboard controller self-test 
								 */
    public static final byte CMD_TEST = (byte) 0xab;	/*
							 * test keyboard interface 
							 */
    public static final byte CMD_DUMP = (byte) 0xac;	/*
							 * diagnostic dump 
							 */
    public static final byte CMD_DISABLE = (byte) 0xad;	/*
							 * disable keyboard 
							 */
    public static final byte CMD_ENABLE = (byte) 0xae;	/*
							 * enable keyboard 
							 */
    public static final byte CMD_RDKBD = (byte) 0xc4;	/*
							 * read keyboard ID 
							 */
    public static final byte CMD_WIN = (byte) 0xd0;	/*
							 * read  output port 
							 */
    public static final byte CMD_WOUT = (byte) 0xd1;	/*
							 * write output port 
							 */
    public static final byte CMD_ECHO = (byte) 0xee;	/*
							 * used for diagnostic testing 
							 */
    public static final byte CMD_PULSE = (byte) 0xff;	/*
							 * pulse bits 3-0 based on low nybble 
							 */
    public static final byte CMD_WRITE_AUX_OBUF = (byte) 0xd3;
    public static final byte CMD_WRITE_MOUSE = (byte)0xd4;
    /*
     * Keyboard commands (send to K_RDWR).
     */
    public static final byte K_CMD_LEDS = (byte) 0xed;	/*
							 * set status LEDs (caps lock, etc.) 
							 */
    public static final byte K_CMD_TYPEMATIC = (byte) 0xf3;	/*
								 * set key repeat and delay 
								 */

    /*
     * Bit definitions for controller command byte (sent following 
     * CMD_WRITE command).
     *
     * Bits 0x02 and 0x80 unused, always set to 0.
     */
    public static final byte CTRL_ENBLIRQ = 0x01;	/*
							 * enable data-ready intrpt 
							 */
    public static final byte CTRL_SETSYSF = 0x04;	/*
							 * Set System Flag 
							 */
    public static final byte CTRL_INHBOVR = 0x08;	/*
							 * Inhibit Override 
							 */
    public static final byte CTRL_DISBLE = 0x10;	/*
							 * disable keyboard 
							 */
    public static final byte CTRL_IGNPARITY = 0x20;	/*
							 * ignore parity from keyboard 
							 */
    public static final byte CTRL_SCAN = 0x40;	/*
						 * standard scan conversion 
						 */

    /*
     * Bit definitions for "Indicator Status Byte" (sent after a 
     * K_CMD_LEDS command).  If the bit is on, the LED is on.  Undefined 
     * bit positions must be 0.
     */
    public static final byte K_LED_SCRLLK = 0x1;	/*
							 * scroll lock 
							 */
    public static final byte K_LED_NUMLK = 0x2;	/*
						 * num lock 
						 */
    public static final byte K_LED_CAPSLK = 0x4;	/*
							 * caps lock 
							 */

    /*
     * Bit definitions for "Miscellaneous port B" (K_PORTB).
     */
    /*
     * read/write 
     */
    public static final byte K_ENABLETMR2 = 0x01;	/*
							 * enable output from timer 2 
							 */
    public static final byte K_SPKRDATA = 0x02;	/*
						 * direct input to speaker 
						 */
    public static final byte K_ENABLEPRTB = 0x04;	/*
							 * "enable" port B 
							 */
    public static final byte K_EIOPRTB = 0x08;	/*
						 * enable NMI on parity error 
						 */
    /*
     * read-only 
     */
    public static final byte K_REFRESHB = 0x10;	/*
						 * refresh flag from INLTCONT PAL 
						 */
    public static final byte K_OUT2B = 0x20;	/*
						 * timer 2 output 
						 */
    public static final byte K_ICKB = 0x40;	/*
						 * I/O channel check (parity error) 
						 */

    /*
     * Bit definitions for the keyboard controller's output port.
     */
    public static final byte KO_SYSRESET = 0x01;	/*
							 * processor reset 
							 */
    public static final byte KO_GATE20 = 0x02;	/*
						 * A20 address line enable 
						 */
    public static final byte KO_AUX_DATA_OUT = 0x04;	/*
							 * output data to auxiliary device 
							 */
    public static final byte KO_AUX_CLOCK = 0x08;	/*
							 * auxiliary device clock 
							 */
    public static final byte KO_OBUF_FUL = 0x10;	/*
							 * keyboard output buffer full 
							 */
    public static final byte KO_AUX_OBUF_FUL = 0x20;	/*
							 * aux device output buffer full 
							 */
    public static final byte KO_CLOCK = 0x40;	/*
						 * keyboard clock 
						 */
    public static final byte KO_DATA_OUT = (byte) 0x80;	/*
							 * output data to keyboard 
							 */

    /*
     * Keyboard return codes.
     */
    public static final byte K_RET_RESET_DONE = (byte) 0xaa;	/*
								 * BAT complete 
								 */
    public static final byte K_RET_ECHO = (byte) 0xee;	/*
							 * echo after echo command 
							 */
    public static final byte K_RET_ACK = (byte) 0xfa;	/*
							 * ack 
							 */
    public static final byte K_RET_RESET_FAIL = (byte) 0xfc;	/*
								 * BAT error 
								 */
    public static final byte K_RET_RESEND = (byte) 0xfe;	/*
								 * resend request 
								 */

    public static final int KBD_MODE_KBD_INT	= (byte)0x01;	/* Keyboard data generate IRQ1 */
    public static final int KBD_MODE_MOUSE_INT	= (byte)0x02;	/* Mouse data generate IRQ12 */
    public static final int KBD_MODE_SYS 		= (byte)0x04;	/* The system flag (?) */
    public static final int KBD_MODE_NO_KEYLOCK	= (byte)0x08;	/* The keylock doesn't affect the keyboard if set */
    public static final int KBD_MODE_DISABLE_KBD	= (byte)0x10;	/* Disable keyboard interface */
    public static final int KBD_MODE_DISABLE_MOUSE	= (byte)0x20;	/* Disable mouse interface */
    public static final int KBD_MODE_KCC 		= (byte)0x40;	/* Scan code conversion to PC format */
    public static final int KBD_MODE_RFU		= (byte)0x80;


    public static final int DELAY = 60;
    public static final int KBD_BUFSIZE = 64;
	
    public static final int AUX_RECONNECT 	= (byte)170;
    public static final int AUX_SET_RES	= (byte)0xe8;	/* Set resolution */
    public static final int AUX_SET_SCALE11	= (byte)0xe6;	/* Set 1:1 scaling */
    public static final int AUX_SET_SCALE21	= (byte)0xe7;	/* Set 2:1 scaling */
    public static final int AUX_GET_SCALE	= (byte)0xe9;	/* Get scaling factor */
    public static final int AUX_SET_STREAM	= (byte)0xea;	/* Set stream mode */
    public static final int AUX_SET_SAMPLE	= (byte)0xf3;	/* Set sample rate */
    public static final int AUX_ENABLE_DEV	= (byte)0xf4;
    public static final int AUX_DISABLE_DEV = (byte)0xf5;
    public static final int AUX_ACK		= (byte)0xfa;
    public static final int AUX_RESET	= (byte)0xff;

    public static final int AUX_INTS_OFF	= (byte) (KBD_MODE_KCC | KBD_MODE_DISABLE_MOUSE | KBD_MODE_SYS | KBD_MODE_KBD_INT);
    public static final int AUX_INTS_ON  	= (byte) (KBD_MODE_KCC | KBD_MODE_SYS | KBD_MODE_MOUSE_INT | KBD_MODE_KBD_INT);

    private Naming naming;
    private CPUManager cpuManager;
    private IRQ irq;
    private Scancodes scan;
    private KeyQueue queue = new KeyQueue (256);
    private KeyQueue auxQueue = new KeyQueue (256);

    private Vector keyListeners = new Vector ();
    private OutputStream localEcho;

    private Ports ports;

    private byte leds = (byte) 0;

    private CPUState waitingInGetcode = null;
    private CPUState waitingForMouse  = null;
    private int mouse_reply_expected = 0;
    private int aux_count = 0;
    private boolean hasAuxiliaryPort = false;

    private boolean hwAvailable; 
    private boolean hwAvailableUnknown = true; 

    public KeyboardImpl () {
	this(null);
    }

    public KeyboardImpl (OutputStream localEcho)
    {
	Debug.out.println ("KeyboardImpl::KeyboardImpl()");
	//if (instance != null) throw new Error("Keyboard already used");
	//instance = this;
	this.naming = InitialNaming.getInitialNaming();
	cpuManager = (CPUManager) naming.lookup ("CPUManager");
	ports = (Ports) naming.lookup ("Ports");
	irq = (IRQ) naming.lookup ("IRQ");
	
	scan = new Scancodes (Debug.out);
	this.localEcho = localEcho;
    }

    public void addKeyListener (KeyListener listener)
    {
	keyListeners.addElement (listener);
    }

    private void notifyKeyListeners (int scancode)
    {
	for (int i = 0; i < keyListeners.size (); i++)
	    {
		((KeyListener) keyListeners.elementAt (i)).keyPressed (scancode);
	    }
    }
	
    public void openAux ()
    {	
	if (hasAuxiliaryPort == false)
	    return;
	if (aux_count++ == 0)
	    {			
		// Enable the auxiliary port on controller. 
		kbdWrite (REG_CMD, CMD_MOUSE_ENABLE);
		kbdWriteCmd (AUX_INTS_ON);
		auxWriteAck (AUX_ENABLE_DEV);	// Enable aux device
	    }			
    }		
    public void releaseAux ()
    {
	if (hasAuxiliaryPort == false)
	    return;
	if (--aux_count != 0)
	    return;
	kbdWriteCmd (AUX_INTS_OFF);
	kbdWrite (REG_CMD, CMD_MOUSE_DISABLE);
    }
	public int readAuxUnblocked ()
	{
		if (hasAuxiliaryPort == false)
			return -1;
		while (true)
		{
			try
			{
				int scancode = -1;

				if (auxQueue.available() == 0)
					return -1;
				scancode = auxQueue.get ();
				return scancode;
			}
			catch (QueueEmptyException e)
		    {
				return -1;
		    }
		}
	}
    public int readAux ()
    {
	if (hasAuxiliaryPort == false)
	    return 0;
	for (;;)
	    {
		int c = -1;
		try
		    {
			int scancode = -1;
			do
			    {
				while (auxQueue.available () == 0)
				    {
					waitingForMouse = cpuManager.getCPUState ();
					cpuManager.block ();
				    }
				scancode = auxQueue.get ();
			    }
			while (scancode == -1);
			c = scancode;
		    }
		catch (QueueEmptyException e)
		    {
		    }
		return c;
	    }
    }
    public void writeAux (byte c)
    {		
	if (hasAuxiliaryPort == false)
	    return;
	waitForKeyboard ();
	ports.outb (REG_CMD, CMD_WRITE_MOUSE);
	waitForKeyboard ();
	ports.outb (REG_RDWR, c);
    } 
    /*
     * Achtung wichtig:
     *  Bevor auxWriteAck() aufgerufen wird muessen unbedingt die Interrupts fuer
     *  AUX enabled werden (mit kbdWriteCmd (AUX_INTS_ON), sonst funktioniert die
     *  Tastatur nicht mehr richtig!!!!
     */
    void auxWriteAck (int val)
    {
	/*
	 * we expect an ACK in response. 
	 */
	mouse_reply_expected++;
		
	waitForKeyboard ();
	ports.outb (REG_CMD, CMD_WRITE_MOUSE);
	waitForKeyboard ();
	ports.outb (REG_RDWR, (byte)val);
	waitForKeyboard ();
    }
    void handleMouseEvent (int scancode)
    {		
	if (mouse_reply_expected != 0)
	    {
		if (scancode == AUX_ACK)
		    {
			mouse_reply_expected--;
			return;
		    }
		mouse_reply_expected = 0;
	    }
	else if (scancode == AUX_RECONNECT)
	    {
		auxQueue.flush ();
		auxWriteAck (AUX_ENABLE_DEV); /* ping the mouse :) */
		return;
	    }
	if (aux_count != 0)
	    {
		auxQueue.append (scancode);
		if (waitingForMouse != null)
		    {
			cpuManager.unblock (waitingForMouse);
			waitingForMouse = null;
		    } else {
			//Debug.out.println("nobody interested in mouse event :-(");
		    }
	    }
    }

    public void interrupt ()
    {
	int scancode;
	int status;
		
	status = (int)ports.inb_p (REG_STAT) & 0xff;
		
	//Debug.out.println ("KeyboardImpl::interrupt() status: " + Integer.toHexString (status));
		
	while ((status & STAT_OBUF_FUL) != 0)
	{
		scancode = ports.inb_p (REG_RDWR) & 0xff;
			
		if ((status & STAT_AUX_OBUF_FUL) != 0)
		    {
			handleMouseEvent (scancode);
		    }
		else
		    {

			if (scan.isReset (scancode))
			    resetPC ();

			if (scan.isNumLock (scancode))
			    {
				switchNumLED ();
				return;
			    }
		
			int nCode = convertKeyCode (scancode);
	
			if (nCode != 0)
			    {
				if (!queue.append (nCode))
				    {
					//out.println("RING OVERFLOW ");
				    }
				queue.notifyWaiter ();
				if (waitingInGetcode != null)
				    {
					cpuManager.unblock (waitingInGetcode);
					waitingInGetcode = null;
				    }
			    }
		    }				
			status = (int)ports.inb_p (REG_STAT) & 0xff;
			//Debug.out.println ("KeyboardImpl::interrupt() status: " + Integer.toHexString (status));
	    }			
    }
    /*
     * public void interruptHandler() {
     * out.println("KEYBOARD IRQ");
     * int scancode;
     * 
     * // a scan code should be ready, since we got an interrupt
     * if ((ports.inb_p(REG_STAT) & STAT_OBUF_FUL) == 0) {
     * return; // no scancode ?
     * }
     * 
     * scancode = ports.inb_p(REG_RDWR);
     * //System.out.println("");
     * //out.println("Scancode "+scancode);
     * //System.out.println("");
     * if (scan.isReset(scancode)) resetPC();
     * 
     * if (scan.isNumLock(scancode)) { 
     * switchNumLED();
     * return;
     * }
     * 
     * notifyKeyListeners(scancode);
     * if (! queue.append(scancode)) {
     * out.println("RING OVERFLOW ");
     * }
     * }
     */
    private void sleep (int msec)
    {
	ports.outb (0x80, (byte) 0);
	//  try { Thread.sleep(msec,0); } catch(InterruptedException e) {}
    }
	
    boolean detectAuxiliaryPort ()
    {
	int loops = 10;
	boolean retval = false;

	/*
	 * Put the value 0x5A in the output buffer using the "Write
	 * * Auxiliary Device Output Buffer" command (0xD3). Poll the
	 * * Status Register for a while to see if the value really
	 * * turns up in the Data Register. If the KBD_STAT_MOUSE_OBF
	 * * bit is also set to 1 in the Status Register, we assume this
	 * * controller has an Auxiliary Port (a.k.a. Mouse Port).
	 */
	waitForKeyboard ();
	ports.outb (REG_CMD, CMD_WRITE_AUX_OBUF);
	
	waitForKeyboard ();
	ports.outb (REG_RDWR, (byte)0x5a);		/*
							 * 0x5a is a random dummy value. 
							 */

	do
	    {
		int status = ports.inb (REG_STAT) & 0xff;

		if ((status & STAT_OBUF_FUL) != 0)
		    {
			ports.inb (REG_RDWR);
			if ((status & STAT_AUX_OBUF_FUL) != 0)
			    {
				Debug.out.println ("Detected PS/2 Mouse Port.");
				retval = true;
			    }
			break;
		    }
		for (int i = 1000000; i >= 0; --i);
		/*
		  try 
		  { 
		  Thread.sleep(1,0); 
		  } 
		  catch(InterruptedException e) 
		  {
		  }
		*/
	    }
	while (--loops > 0);

	return retval;
    }

    public boolean keyboardHardwareAvailable ()
    {
	if (hwAvailableUnknown) {
	    hwAvailableUnknown=false;
	    for (int i = 0; i < 1000000; i++)
		{
		    if ((ports.inb_p (REG_STAT) & STAT_IBUF_FUL) == 0) {
			hwAvailable=true;
			return true;
		    }
		    sleep (DELAY);
		}
	    hwAvailable=false;
	    return false;
	} else {
	    return hwAvailable;
	}
    }

    private void waitForKeyboard ()
    {
	for (int i = 0; i < 1000000; i++)
	    {
		if ((ports.inb_p (REG_STAT) & STAT_IBUF_FUL) == 0)
		    return;
		sleep (DELAY);
	    }
	throw new Error ("No answer from keyboard.");
    }

    private void kbdWrite (int addr, int b)
    {
	waitForKeyboard ();
	ports.outb (addr, (byte)b);
    }
    private void kbdWriteCmd (int cb)
    {
	waitForKeyboard ();
	ports.outb_p (REG_CMD, CMD_WRITE);
	waitForKeyboard ();
	ports.outb_p (REG_RDWR, (byte) cb);
    }
    private void wrCommand (int cb)
    {
	Debug.out.println ("wrCommand :" + cb);
	waitForKeyboard ();
	ports.outb_p (REG_CMD, CMD_WRITE);
	waitForKeyboard ();
	ports.outb_p (REG_RDWR, (byte) cb);
    }

    private void clearKbdBuffer ()
    {
	while ((ports.inb_p (REG_STAT) & STAT_OBUF_FUL) != 0)
	    {
		sleep (DELAY);
		ports.inb_p (REG_RDWR);
	    }
    }

    private boolean resetKeyboard ()
    {
	for (int retires = 0; retires < 10; retires++)
	    {
		waitForKeyboard ();
		ports.outb_p (REG_RDWR, CMD_PULSE);
		int i = 10000;
		while (!((ports.inb_p (REG_STAT) & STAT_OBUF_FUL) != 0) && (--i > 0))
		    {
			sleep (DELAY);
		    }
		if (ports.inb_p (REG_RDWR) == K_RET_ACK)
		    return true;
	    }
	return false;
    }

    private boolean resetKeyboardFinish ()
    {
	for (int retires = 0; retires < 100; retires++)
	    {
		int i = 10000;
		while (!((ports.inb_p (REG_STAT) & STAT_OBUF_FUL) != 0) && (--i > 0))
		    {
			sleep (DELAY * 10);
		    }
		if (ports.inb_p (REG_RDWR) == K_RET_RESET_DONE)
		    return true;
	    }
	return false;
    }

    public void init ()
    {
	Debug.out.println ("KeyboardImpl.init()");
	int c, i;


	// Turn off irq generation
	wrCommand (CTRL_SCAN | CTRL_INHBOVR | CTRL_SETSYSF);

	Debug.out.println ("ClearBuffer:");
	clearKbdBuffer ();
	Debug.out.println ("  OK.");

	Debug.out.println ("Reset:");
	if (!resetKeyboard ())
	    throw new Error();
	Debug.out.println ("  OK.");

	Debug.out.println ("ResetFinish: ");
	if (!resetKeyboardFinish ())
	{
		Debug.out.println ("unable to reset keyboard");
	    throw new Error();
	}
	Debug.out.println ("  OK.");

	Debug.out.println ("Keyboard reset ok.");
		 
	hasAuxiliaryPort = detectAuxiliaryPort ();
	Debug.out.println ("PS/2 Mouse available: " + hasAuxiliaryPort);

	cpuManager.start (cpuManager.createCPUState (new ThreadEntry ()
	    {
		public void run ()
		{
		    cpuManager.
			setThreadName
			("KBD-2nd-IRQ"); for (;;)
			    {
				try
				    {
					queue.waitForCharacter ();
					notifyKeyListeners (queue.
							    get ());}
				catch (Exception e)
				    {
					Debug.out.println ("EXCEPTION");}
			    }
		}
	    }
						     ));
	Debug.out.println ("Started IRQ handler.");



	// Turn on IRQ generation
	irq.installFirstLevelHandler (IRQ_KEYBOARD, this);
	irq.enableIRQ (IRQ_KEYBOARD);
	wrCommand ((CTRL_SCAN | CTRL_INHBOVR | CTRL_SETSYSF | CTRL_ENBLIRQ));

	if (hasAuxiliaryPort == true)
	    {			
		irq.installFirstLevelHandler (AUX_IRQ, this);
		irq.enableIRQ (AUX_IRQ);
		kbdWrite (REG_CMD, CMD_MOUSE_ENABLE);
		kbdWriteCmd (AUX_INTS_ON);
		auxWriteAck (AUX_SET_SAMPLE);			
		auxWriteAck (100);		
		auxWriteAck (AUX_SET_RES);
		auxWriteAck (3);						
		auxWriteAck (AUX_SET_SCALE21);	
		kbdWrite (REG_CMD, CMD_MOUSE_DISABLE);	
		kbdWriteCmd (AUX_INTS_OFF);	
	    }
    }


    public void readKeys ()
    {
	ports.outb_p (0x21, (byte) 0x02);
	for (;;)
	    {
		for (;;)
		    {
			int status = ports.inb_p (0x64);
			if ((status & 0x01) == 0x01)
			    break;
		    }
		int scancode = ports.inb_p (0x60);

		System.out.println ("KB " + scancode);
		if (scancode == 0x01)
		    break;									// ESC key
	    }
	ports.outb_p (0x21, (byte) 0);
    }

    /**
     * Read a character from the keyboard.
     * Blocks until character is available.
     */
    public int getc ()
    {
	for (;;)
	    {
		int c = -1;
		do
		    {
			try
			    {
				int scancode = -1;
				do
				    {
					while (queue.available () == 0);	//cpuManager.yield(); // should sleep here !
					scancode = queue.get ();
				    }
				while (scancode == -1);

				if (scan.isReset (scancode))
				    {
					Debug.out.println ("Reset PC on users request.");
					resetPC ();
				    }
				c = scan.translate (scancode);
			    }
			catch (QueueEmptyException e)
			    {
			    }
		    }
		while (c == -1);
		try
		    {
			if (localEcho != null)
			    localEcho.write ((char) c);	// local echo
		    }
		catch (IOException e)
		    {
		    }
		return c;
	    }
    }

    /**
     * Read a scancode from the keyboard.
     * Blocks until character is available.
     */
    public int getcode ()
    {
	for (;;)
	    {
		int c = -1;
		try
		    {
			int scancode = -1;
			do
			    {
				while (queue.available () == 0)
				    {
					waitingInGetcode = cpuManager.getCPUState ();
					cpuManager.block ();
				    }
				scancode = queue.get ();
			    }
			while (scancode == -1);
			if (scan.isReset (scancode))
			    {
				Debug.out.println ("Reset PC on users request.");
				resetPC ();
			    }
			c = scancode;
		    }
		catch (QueueEmptyException e)
		    {
		    }
		return c;
	    }
    }

    public boolean getNumLock ()
    {
	return (leds & 2) != 0;
    }

    public void switchNumLED ()
    {
	switchNumLED (!getNumLock ());
    }

    public void switchNumLED (boolean on)
    {
	if (on)
	    {
		leds |= 2;
	    }
	else
	    {
		leds &= ~2;
	    }
	switchLEDs ();
    }

    private void switchLEDs ()
    {
	int status;
	ports.outb_p (REG_RDWR, K_CMD_LEDS);
	do
	    {
		status = ports.inb_p (REG_STAT);
	    }
	while (status == STAT_IBUF_FUL);

	ports.outb_p (REG_RDWR, leds);
    }

    public void resetPC ()
    {
	for (int i = 0; i < 100; i++) {
	    wrCommand(CMD_PULSE & ~KO_SYSRESET);
	}
    }
    static int nLastCode = 0;
    static int nLastKey = 0;
    static int nPauseKeyCount = 0;

    static int convertKeyCode (int nCode)
    {
	int nKey;
	int nFlg;

	if (nPauseKeyCount > 0)
	    {
		nPauseKeyCount++;

		if (nPauseKeyCount == 6)
		    {
			nPauseKeyCount = 0;
			return 0x10;
		    }
		else
		    {
			return 0;
		    }
	    }

	if (0xe1 == nCode)
	    {
		nPauseKeyCount = 1;
		return 0;
	    }

	nFlg = nCode & 0x80;

	if (0xe0 == nLastCode)
	    {
		nKey = s_anExtRawKeyTab[nCode & ~0x80];
	    }
	else
	    {
		if (0xe0 != nCode)
		    nKey = s_anRawKeyTab[nCode & ~0x80];
		else
		    nKey = 0;
	    }
	nLastCode = nCode;

	if ((int) (nKey | nFlg) == nLastKey || 0 == nKey)
	    return 0;

	nLastKey = nKey | nFlg;

	return (nKey | nFlg);
    }
    static int s_anRawKeyTab[] = {
	0x00,												/*
													 * * NO KEY 
													 */
	0x01,												/*
													 * 1   ESC  
													 */
	0x12,												/*
													 * 2   1  
													 */
	0x13,												/*
													 * 3   2  
													 */
	0x14,												/*
													 * 4   3  
													 */
	0x15,												/*
													 * 5   4  
													 */
	0x16,												/*
													 * 6   5  
													 */
	0x17,												/*
													 * 7   6  
													 */
	0x18,												/*
													 * 8   7  
													 */
	0x19,												/*
													 * 9   8  
													 */
	0x1a,												/*
													 * 10   9 
													 */
	0x1b,												/*
													 * 11   0 
													 */
	0x1c,												/*
													 * 12   - 
													 */
	0x1d,												/*
													 * 13   = 
													 */
	0x1e,												/*
													 * 14   BACKSPACE 
													 */
	0x26,												/*
													 * 15   TAB 
													 */
	0x27,												/*
													 * 16   Q 
													 */
	0x28,												/*
													 * 17   W 
													 */
	0x29,												/*
													 * 18   E 
													 */
	0x2a,												/*
													 * 19   R 
													 */
	0x2b,												/*
													 * 20   T 
													 */
	0x2c,												/*
													 * 21   Y 
													 */
	0x2d,												/*
													 * 22   U 
													 */
	0x2e,												/*
													 * 23   I 
													 */
	0x2f,												/*
													 * 24   O 
													 */
	0x30,												/*
													 * 25   P 
													 */
	0x31,												/*
													 * 26   [ { 
													 */
	0x32,												/*
													 * 27   ] } 
													 */
	0x47,												/*
													 * 28   ENTER (RETURN)  
													 */
	0x5c,												/*
													 * 29   LEFT CONTROL  
													 */
	0x3c,												/*
													 * 30   A 
													 */
	0x3d,												/*
													 * 31   S 
													 */
	0x3e,												/*
													 * 32   D 
													 */
	0x3f,												/*
													 * 33   F 
													 */
	0x40,												/*
													 * 34   G 
													 */
	0x41,												/*
													 * 35   H 
													 */
	0x42,												/*
													 * 36   J 
													 */
	0x43,												/*
													 * 37   K 
													 */
	0x44,												/*
													 * 38   L 
													 */
	0x45,												/*
													 * 39   ; : 
													 */
	0x46,												/*
													 * 40   ' " 
													 */
	0x11,												/*
													 * 41   ` ~ 
													 */
	0x4b,												/*
													 * 42   LEFT SHIFT  
													 */
	0x33,												/*
													 * 43   NOTE : This key code was not defined in the original table! (' *) 
													 */
	0x4c,												/*
													 * 44   Z 
													 */
	0x4d,												/*
													 * 45   X 
													 */
	0x4e,												/*
													 * 46   C 
													 */
	0x4f,												/*
													 * 47   V 
													 */
	0x50,												/*
													 * 48   B 
													 */
	0x51,												/*
													 * 49   N 
													 */
	0x52,												/*
													 * 50   M 
													 */
	0x53,												/*
													 * 51   , < 
													 */
	0x54,												/*
													 * 52   . > 
													 */
	0x55,												/*
													 * 53   / ? 
													 */
	0x56,												/*
													 * 54   RIGHT SHIFT 
													 */
	0x24,												/*
													 * 55   *            (KEYPAD) 
													 */
	0x5d,												/*
													 * 56   LEFT ALT  
													 */
	0x5e,												/*
													 * 57   SPACEBAR  
													 */
	0x3b,												/*
													 * 58   CAPSLOCK  
													 */
	0x02,												/*
													 * 59   F1  
													 */
	0x03,												/*
													 * 60   F2  
													 */
	0x04,												/*
													 * 61   F3  
													 */
	0x05,												/*
													 * 62   F4  
													 */
	0x06,												/*
													 * 63   F5  
													 */
	0x07,												/*
													 * 64   F6  
													 */
	0x08,												/*
													 * 65   F7  
													 */
	0x09,												/*
													 * 66   F8  
													 */
	0x0a,												/*
													 * 67   F9  
													 */
	0x0b,												/*
													 * 68   F10 
													 */
	0x22,												/*
													 * 69   NUMLOCK      (KEYPAD) 
													 */
	0x0f,												/*
													 * 70   SCROLL LOCK 
													 */
	0x37,												/*
													 * 71   7 HOME       (KEYPAD) 
													 */
	0x38,												/*
													 * 72   8 UP         (KEYPAD) 
													 */
	0x39,												/*
													 * 73   9 PGUP       (KEYPAD) 
													 */
	0x25,												/*
													 * 74   -            (KEYPAD) 
													 */
	0x48,												/*
													 * 75   4 LEFT       (KEYPAD) 
													 */
	0x49,												/*
													 * 76   5            (KEYPAD) 
													 */
	0x4a,												/*
													 * 77   6 RIGHT      (KEYPAD) 
													 */
	0x3a,												/*
													 * 78   +            (KEYPAD) 
													 */
	0x58,												/*
													 * 79   1 END        (KEYPAD) 
													 */
	0x59,												/*
													 * 80   2 DOWN       (KEYPAD) 
													 */
	0x5a,												/*
													 * 81   3 PGDN       (KEYPAD) 
													 */
	0x64,												/*
													 * 82   0 INSERT     (KEYPAD) 
													 */
	0x65,												/*
													 * 83   . DEL        (KEYPAD) 
													 */
	0x7e,												/*
													 * 84   SYSRQ 
													 */
	0x00,												/*
													 * 85 
													 */
	0x69,												/*
													 * 86   NOTE : This key code was not defined in the original table! (< >) 
													 */
	0x0c,												/*
													 * 87   F11 
													 */
	0x0d												/*
													 * 88   F12 
													 */
    };

    static int s_anExtRawKeyTab[] = {
	0x00,												/*
													 * 0  
													 */
	0x00,												/*
													 * 1  
													 */
	0x00,												/*
													 * 2  
													 */
	0x00,												/*
													 * 3  
													 */
	0x00,												/*
													 * 4  
													 */
	0x00,												/*
													 * 5  
													 */
	0x00,												/*
													 * 6  
													 */
	0x00,												/*
													 * 7  
													 */
	0x00,												/*
													 * 8  
													 */
	0x00,												/*
													 * 9  
													 */
	0x00,												/*
													 * 10 
													 */
	0x00,												/*
													 * 11 
													 */
	0x00,												/*
													 * 12 
													 */
	0x00,												/*
													 * 13 
													 */
	0x00,												/*
													 * 14 
													 */
	0x00,												/*
													 * 15 
													 */
	0x00,												/*
													 * 16 
													 */
	0x00,												/*
													 * 17 
													 */
	0x00,												/*
													 * 18 
													 */
	0x00,												/*
													 * 19 
													 */
	0x00,												/*
													 * 20 
													 */
	0x00,												/*
													 * 21 
													 */
	0x00,												/*
													 * 22 
													 */
	0x00,												/*
													 * 23 
													 */
	0x00,												/*
													 * 24 
													 */
	0x00,												/*
													 * 25 
													 */
	0x00,												/*
													 * 26 
													 */
	0x00,												/*
													 * 27 
													 */
	0x5b,												/*
													 * 28   ENTER        (KEYPAD) 
													 */
	0x60,												/*
													 * 29   RIGHT CONTROL 
													 */
	0x00,												/*
													 * 30 
													 */
	0x00,												/*
													 * 31 
													 */
	0x00,												/*
													 * 32 
													 */
	0x00,												/*
													 * 33 
													 */
	0x00,												/*
													 * 34 
													 */
	0x00,												/*
													 * 35 
													 */
	0x00,												/*
													 * 36 
													 */
	0x00,												/*
													 * 37 
													 */
	0x00,												/*
													 * 38 
													 */
	0x00,												/*
													 * 39 
													 */
	0x00,												/*
													 * 40 
													 */
	0x00,												/*
													 * 41 
													 */
	0x00,												/*
													 * 42   PRINT SCREEN (First code) 
													 */
	0x00,												/*
													 * 43 
													 */
	0x00,												/*
													 * 44 
													 */
	0x00,												/*
													 * 45 
													 */
	0x00,												/*
													 * 46 
													 */
	0x00,												/*
													 * 47 
													 */
	0x00,												/*
													 * 48 
													 */
	0x00,												/*
													 * 49 
													 */
	0x00,												/*
													 * 50 
													 */
	0x00,												/*
													 * 51 
													 */
	0x00,												/*
													 * 52 
													 */
	0x23,												/*
													 * 53   /            (KEYPAD) 
													 */
	0x00,												/*
													 * 54 
													 */
	0x0e,												/*
													 * 55   PRINT SCREEN (Second code)  
													 */
	0x5f,												/*
													 * 56   RIGHT ALT 
													 */
	0x00,												/*
													 * 57 
													 */
	0x00,												/*
													 * 58 
													 */
	0x00,												/*
													 * 59 
													 */
	0x00,												/*
													 * 60 
													 */
	0x00,												/*
													 * 61 
													 */
	0x00,												/*
													 * 62 
													 */
	0x00,												/*
													 * 63 
													 */
	0x00,												/*
													 * 64 
													 */
	0x00,												/*
													 * 65 
													 */
	0x00,												/*
													 * 66 
													 */
	0x00,												/*
													 * 67 
													 */
	0x00,												/*
													 * 68 
													 */
	0x00,												/*
													 * 69 
													 */
	0x7f,												/*
													 * 70  BREAK  
													 */
	0x20,												/*
													 * 71   HOME         (NOT KEYPAD) 
													 */
	0x57,												/*
													 * 72   UP           (NOT KEYPAD) 
													 */
	0x21,												/*
													 * 73   PAGE UP      (NOT KEYPAD) 
													 */
	0x00,												/*
													 * 74 
													 */
	0x61,												/*
													 * 75   LEFT         (NOT KEYPAD) 
													 */
	0x00,												/*
													 * 76 
													 */
	0x63,												/*
													 * 77   RIGHT        (NOT KEYPAD) 
													 */
	0x00,												/*
													 * 78 
													 */
	0x35,												/*
													 * 79   END          (NOT KEYPAD) 
													 */
	0x62,												/*
													 * 80   DOWN         (NOT KEYPAD) 
													 */
	0x36,												/*
													 * 81   PAGE DOWN    (NOT KEYPAD) 
													 */
	0x1f,												/*
													 * 82   INSERT       (NOT KEYPAD) 
													 */
	0x34,												/*
													 * 83   DELETE       (NOT KEYPAD) 
													 */
	0x00,												/*
													 * 84 
													 */
	0x00,												/*
													 * 85 
													 */
	0x00,												/*
													 * 86 
													 */
	0x00,												/*
													 * 87 
													 */
	0x00,												/*
													 * 88 
													 */
	0x00,												/*
													 * 89 
													 */
	0x00,												/*
													 * 90 
													 */
	0x00,												/*
													 * 91 
													 */
	0x00,												/*
													 * 92 
													 */
	0x00,												/*
													 * 93 
													 */
	0x00,												/*
													 * 94 
													 */
	0x00,												/*
													 * 95 
													 */
	0x00,												/*
													 * 96 
													 */
	0x00,												/*
													 * 97 
													 */
	0x00,												/*
													 * 98 
													 */
	0x00,												/*
													 * 99 
													 */
	0x00,												/*
													 * 100  
													 */
	0x00,												/*
													 * 101  
													 */
	0x00,												/*
													 * 102  
													 */
	0x00,												/*
													 * 103  
													 */
	0x00,												/*
													 * 104  
													 */
	0x00,												/*
													 * 105  
													 */
	0x00,												/*
													 * 106  
													 */
	0x00,												/*
													 * 107  
													 */
	0x00,												/*
													 * 108  
													 */
	0x00,												/*
													 * 109  
													 */
	0x00,												/*
													 * 110  
													 */
	0x00,												/*
													 * 111   MACRO  
													 */
	0x00,												/*
													 * 112  
													 */
	0x00,												/*
													 * 113  
													 */
	0x00,												/*
													 * 114  
													 */
	0x00,												/*
													 * 115  
													 */
	0x00,												/*
													 * 116  
													 */
	0x00,												/*
													 * 117  
													 */
	0x00,												/*
													 * 118  
													 */
	0x00,												/*
													 * 119  
													 */
	0x00,												/*
													 * 120  
													 */
	0x00,												/*
													 * 121  
													 */
	0x00,												/*
													 * 122  
													 */
	0x00,												/*
													 * 123  
													 */
	0x00,												/*
													 * 124  
													 */
	0x00,												/*
													 * 125  
													 */
	0x00,												/*
													 * 126  
													 */
	0x00,												/*
													 * 127  
													 */
	0x00,												/*
													 * 128  
													 */
	0x00,												/*
													 * 129  
													 */

	0x00,												/*
													 * 130  
													 */
	0x00,												/*
													 * 131  
													 */
	0x00,												/*
													 * 132  
													 */
	0x00,												/*
													 * 133  
													 */
	0x00,												/*
													 * 134  
													 */
	0x00,												/*
													 * 135  
													 */
	0x00,												/*
													 * 136  
													 */
	0x00,												/*
													 * 137  
													 */
	0x00,												/*
													 * 138  
													 */
	0x00,												/*
													 * 139  
													 */
	0x00,												/*
													 * 130  
													 */

	0x00,												/*
													 * 1  
													 */
	0x00,												/*
													 * 2  
													 */
	0x00,												/*
													 * 3  
													 */
	0x00,												/*
													 * 4  
													 */
	0x00,												/*
													 * 5  
													 */
	0x00,												/*
													 * 6  
													 */
	0x00,												/*
													 * 7  
													 */
	0x00,												/*
													 * 8  
													 */
	0x00,												/*
													 * 9  
													 */

	0x00,												/*
													 * 150  
													 */
	0x00,												/*
													 * 151  
													 */
	0x00,												/*
													 * 152  
													 */
	0x00,												/*
													 * 153  
													 */
	0x00,												/*
													 * 154  
													 */
	0x00,												/*
													 * 155  
													 */
	0x00,												/*
													 * 156  
													 */
	0x00,												/*
													 * 157  
													 */
	0x00,												/*
													 * 158  
													 */
	0x00,												/*
													 * 159  
													 */

	0x00,												/*
													 * 0  
													 */
	0x00,												/*
													 * 1  
													 */
	0x00,												/*
													 * 2  
													 */
	0x00,												/*
													 * 3  
													 */
	0x00,												/*
													 * 4  
													 */
	0x00,												/*
													 * 5  
													 */
	0x00,												/*
													 * 6  
													 */
	0x00,												/*
													 * 7  
													 */
	0x00,												/*
													 * 8  
													 */
	0x00,												/*
													 * 9  
													 */

	0x00,												/*
													 * 170  
													 */
	0x00,												/*
													 * 171  
													 */
	0x00,												/*
													 * 172  
													 */
	0x00,												/*
													 * 173  
													 */
	0x00,												/*
													 * 174  
													 */
	0x00,												/*
													 * 175  
													 */
	0x00,												/*
													 * 176  
													 */
	0x00,												/*
													 * 177  
													 */
	0x00,												/*
													 * 178  
													 */
	0x00,												/*
													 * 179  
													 */

	0x00,												/*
													 * 0  
													 */
	0x00,												/*
													 * 1  
													 */
	0x00,												/*
													 * 2  
													 */
	0x00,												/*
													 * 3  
													 */
	0x00,												/*
													 * 4  
													 */
	0x00,												/*
													 * 5  
													 */
	0x00,												/*
													 * 6  
													 */
	0x00,												/*
													 * 7  
													 */
	0x00,												/*
													 * 8  
													 */
	0x00,												/*
													 * 9  
													 */

	0x00,												/*
													 * 190  
													 */
	0x00,												/*
													 * 191  
													 */
	0x00,												/*
													 * 192  
													 */
	0x00,												/*
													 * 193  
													 */
	0x00,												/*
													 * 194  
													 */
	0x00,												/*
													 * 195  
													 */
	0x00,												/*
													 * 196  
													 */
	0x00,												/*
													 * 197  
													 */
	0x00,												/*
													 * 198  
													 */
	0x00,												/*
													 * 199  
													 */

	0x00,												/*
													 * 0  
													 */
	0x00,												/*
													 * 1  
													 */
	0x00,												/*
													 * 2  
													 */
	0x00,												/*
													 * 3  
													 */
	0x00,												/*
													 * 4  
													 */
	0x00,												/*
													 * 5  
													 */
	0x00,												/*
													 * 6  
													 */
	0x00,												/*
													 * 7  
													 */
	0x00,												/*
													 * 8  
													 */
	0x00,												/*
													 * 9  
													 */

	0x00,												/*
													 * 210  
													 */
	0x00,												/*
													 * 211  
													 */
	0x00,												/*
													 * 212  
													 */
	0x00,												/*
													 * 213  
													 */
	0x00,												/*
													 * 214  
													 */
	0x00,												/*
													 * 215  
													 */
	0x00,												/*
													 * 216  
													 */
	0x00,												/*
													 * 217  
													 */
	0x00,												/*
													 * 218  
													 */
	0x00,												/*
													 * 219  
													 */

	0x00,												/*
													 * 0  
													 */
	0x00,												/*
													 * 1  
													 */
	0x00,												/*
													 * 2  
													 */
	0x00,												/*
													 * 3  
													 */
	0x00,												/*
													 * 4  
													 */
	0x00,												/*
													 * 5  
													 */
	0x00,												/*
													 * 6  
													 */
	0x00,												/*
													 * 7  
													 */
	0x00,												/*
													 * 8  
													 */
	0x00,												/*
													 * 9  
													 */

	0x00,												/*
													 * 230  
													 */
	0x00,												/*
													 * 231  
													 */
	0x00,												/*
													 * 232  
													 */
	0x00,												/*
													 * 233  
													 */
	0x00,												/*
													 * 234  
													 */
	0x00,												/*
													 * 235  
													 */
	0x00,												/*
													 * 236  
													 */
	0x00,												/*
													 * 237  
													 */
	0x00,												/*
													 * 238  
													 */
	0x00,												/*
													 * 239  
													 */

	0x00,												/*
													 * 0  
													 */
	0x00,												/*
													 * 1  
													 */
	0x00,												/*
													 * 2  
													 */
	0x00,												/*
													 * 3  
													 */
	0x00,												/*
													 * 4  
													 */
	0x00,												/*
													 * 5  
													 */
	0x00,												/*
													 * 6  
													 */
	0x00,												/*
													 * 7  
													 */
	0x00,												/*
													 * 8  
													 */
	0x00,												/*
													 * 9  
													 */

	0x00,												/*
													 * 250  
													 */
	0x00,												/*
													 * 251  
													 */
	0x00,												/*
													 * 252  
													 */
	0x00,												/*
													 * 253  
													 */
	0x00,												/*
													 * 254  
													 */
	0x00,												/*
													 * 255  
													 */
	0x00,												/*
													 * 256  
													 */
    };
}

// FIFO queue of scancodes
// implemented as a ring buffer
// to be used in the interrupt handler
//  - no allocation of dynamic memory (objects)!
//  - no exceptions
// Notice: This class is not reentrant. 
//         Must be called in a single threaded environment.
//         However, this is not a really big problem since
//         append() and get() don't conflict.
class KeyQueue
{
    int length;
    int[] queue;
    int readIdx;
    int writeIdx;
    CPUManager cpuManager;
    CPUState waiting;

    public KeyQueue (int queueLength)
    {
	length = queueLength;
	queue = new int[length];
	readIdx = length - 1;
	writeIdx = 0;
		
	cpuManager =
	    (CPUManager) InitialNaming.getInitialNaming().lookup ("CPUManager");
    }

    // this method is called in an interrupt handler
    public boolean append (int k)
    {
	// check if there is space in the ring
	if (readIdx == writeIdx)
	    return false;
	queue[writeIdx] = k;
	writeIdx = (writeIdx + 1) % length;
	return true;
    }
    public void flush ()
    {
	readIdx = length - 1;
	writeIdx = 0;
    }

    /** 
     * @returns the number of characters that can be read from the queue
     */
    public int available ()
    {
	return (writeIdx - readIdx - 1) % length;
    }

    public int get () throws QueueEmptyException
    {
		int next = (readIdx + 1) % length;
		if (next == writeIdx)
	    	return -1;								//throw new QueueEmptyException();
		readIdx = next;
		return queue[readIdx];
    }

    public void waitForCharacter ()
    {
	while (((readIdx + 1) % length) == writeIdx)
	    {
		waiting = cpuManager.getCPUState ();
		cpuManager.block ();
	    }
    }

    public void notifyWaiter ()
    {
	// cpuManager.unblock(waiting);
    }

}

class QueueEmptyException extends Exception
{
    QueueEmptyException ()
    {
    }
    QueueEmptyException (String msg)
    {
	super (msg);
    }
}
