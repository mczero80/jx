package metaxa.os.devices.net;

import java.util.*;
import jx.zero.*;
import jx.zero.debug.*;
import jx.devices.pci.*;
import jx.devices.*;
import jx.timer.*;
import jx.buffer.separator.*;
import jx.zero.debug.DebugPrintStream;
import jx.zero.debug.DebugOutputStream;

class User {
    static DebugPrintStream out;
}

public class ComInit extends Softlimits implements DeviceFinder {
    CPUManager cpuManager;
    static SleepManager sleepManager;// = new timerpc.SleepManagerImpl();


    final static int ID_DEVICE_MASK = 0xffff0000; 
    final static int ID_DEVICE_OFFSET_BIT = 16; 
    final static int ID_VENDOR_MASK = 0x0000ffff; 
    final static int ID_VENDOR_OFFSET_BIT = 0; 
   
    //
    // Supported PCI device id's
    //
    
    final static int NIC_VENDOR_ID = 0x10B7;            // 3COM Vendor ID
    final static int NIC_PCI_DEVICE_ID_9055 = 0x9055;
    final static int NIC_PCI_DEVICE_ID_9056 = 0x9056;
    final static int NIC_PCI_DEVICE_ID_9058 = 0x9058;
    final static int NIC_PCI_DEVICE_ID_9004 = 0x9004;
    final static int NIC_PCI_DEVICE_ID_9005 = 0x9005;
    final static int NIC_PCI_DEVICE_ID_9006 = 0x9006;
    final static int NIC_PCI_DEVICE_ID_900A = 0x900A;
    final static int NIC_PCI_DEVICE_ID_905A = 0x905A;
    final static int NIC_PCI_DEVICE_ID_9200 = 0x9200;
    final static int NIC_PCI_DEVICE_ID_9800 = 0x9800;
    final static int NIC_PCI_DEVICE_ID_9805 = 0x9805;
    
    //
    // ASIC versions - currently not used
    //

    final static int NIC_ASIC_CYCLONE_KRAKATOA_LUCENT = 0x0;
    final static int NIC_ASIC_HURRICANE_TORNADO_LUCENT = 0x1;
    final static int NIC_ASIC_HURRICANE_NATIONAL = 0x2;
    final static int NIC_ASIC_HURRICANE_TORNADO_BROADCOM = 0x3;

    // 
    // from PCIBus.java - for method doBasicConfig for checking, wether busmastering is already set by the bios
    // because all these things are private in PCIBus.java, I have to put it here too :-)
    //

    static final int REG_CMD = 0x01; 
    static final int CMD_MASK_BM = 0x00000004;

    //
    // Registers to set the IOBase address or the MEMBase address
    //
    // IMPORTANT - THESE ARE THE NUMBERS OF THE PCI-REGISTERS, NOT THE ADDRESSES 

    static final int PCI_BASE_ADDRESS_0 = 4;   // IOBaseAddress  
    static final int PCI_BASE_ADDRESS_1 = 5;   // MemBaseAddress

    //
    // Registers for additional information
    //

    static final int PCI_REVISION_ID = 2;       // Revision ID - just for 3C90xB NICs - NOTE: number of the register
 
  // the same as above - these are the numbers of the register
    static final int PCI_CACHE_LINE_SIZE = 3;   // 8 bits 
    static final int PCI_LATENCY_TIMER = 3;     // 8 bits 
    static final int PCI_HEADER_TYPE = 3;       // 8 bits 
        
    //
    // for access to the power management
    //
    
  static final int PCI_POWER_CONTROL = 56;   // number of the register
    static final int PCI_PME_ENABLE = 0x0100;
    static final int PCI_PME_STATUS = 0x8000;
    
    static final int PCI_POWER_STATE_D0 = 0x00;
    static final int PCI_POWER_STATE_D1 = 0x01;
    static final int PCI_POWER_STATE_D2 = 0x02;
    static final int PCI_POWER_STATE_D3 = 0x03; 

    // from pci.h include - mask to get the corresponding addresses of I/O-Space or memory-space
    
    static final int PCI_BASE_ADDRESS_MEM_MASK = (~0x0f);
    static final int PCI_BASE_ADDRESS_IO_MASK = (~0x03);       


    Vector foundNics = new Vector();

    public static void udelay(int microseconds) {
	sleepManager.udelay(microseconds);
    }
    public static void sleep(int msec, int microseconds) {
	sleepManager.mdelay(msec);
	sleepManager.udelay(microseconds);
    }

    private Ports ports;
    private Clock clock;
    private IRQ irq;
    private MemoryManager memMgr;
    private TimerManager timerManager;
    private MemoryConsumer etherConsumer;

    public ComInit(TimerManager timerManager,  SleepManager sleepManager, MemoryConsumer etherConsumer) {
	Naming naming = InitialNaming.getInitialNaming();
	cpuManager = (CPUManager) InitialNaming.getInitialNaming().lookup("CPUManager");
	this.ports = (Ports)naming.lookup("Ports");
	this.clock = (Clock)naming.lookup("Clock");
	this.irq = (IRQ)naming.lookup("IRQ");
	this.memMgr = (MemoryManager)naming.lookup("MemoryManager");

	this.sleepManager = new timerpc.SleepManagerImpl();
	this.timerManager = timerManager;
	this.etherConsumer = etherConsumer;
	if (etherConsumer != null) throw new Error("not expected");
	User.out = Debug.out;
    }

    public Device[] find(String[] args) {
	Debug.out.println("lookup PCI Access Point...");
	PCIAccess bus;
	int counter=0;
	for(;;) {
	    bus = (PCIAccess)InitialNaming.getInitialNaming().lookup("PCIAccess");
	    if (bus == null) {
		if (counter % 20 == 0) { counter = 0; Debug.out.println("NetInit still waiting for PCI");}
		counter++;
		Thread.yield();
	    } else {
		break;
	    }
	}	
	return findDevice(bus);
    }

    public D3C905[] findDevice(PCIAccess pci) {
	int deviceID = -1;
	int vendorID = -1;
	/* query for network interface cards of any vendor */
	PCIDevice[] devInfo = pci.getDevicesByClass(PCI.CLASSCODE_CLASS_MASK, PCI.BASECLASS_NETWORK);
	if (devInfo == null || devInfo.length == 0) {
	    Debug.out.println("3COM probe(): no network devices of any vendor found! ");
	    return null;
	}
	// search for supported NICs
	for (int i = 0; i < devInfo.length; i++) {
	    deviceID = devInfo[i].getDeviceID() & 0xffff;
	    vendorID = devInfo[i].getVendorID() & 0xffff;

	    Debug.out.println("Vendor="+ Integer.toHexString(vendorID)+", Device="+ Integer.toHexString(deviceID));
	
	    if (vendorID ==  NIC_VENDOR_ID) {          // 3COM Vendor ID
		switch (deviceID) {

		case NIC_PCI_DEVICE_ID_9055:
		    User.out.println("10/100 Base-TX NIC found");
		    foundNics.addElement(devInfo[i]);
		    break;
		    
		case NIC_PCI_DEVICE_ID_9058:
		    User.out.println("10/100 COMBO Deluxe board found");
		    foundNics.addElement(devInfo[i]);	    
		    break;
		    
		case NIC_PCI_DEVICE_ID_9004:
		    User.out.println("10Base-T TPO NIC found");
		    foundNics.addElement(devInfo[i]);
		     break;
 
		case NIC_PCI_DEVICE_ID_9005:
		    User.out.println("10Base-T/10Base-2/AUI Combo found");
		    foundNics.addElement(devInfo[i]);
		    break;
 
		case NIC_PCI_DEVICE_ID_9006:
		    User.out.println("10Base-T/10Base-2/TPC found");
		    foundNics.addElement(devInfo[i]);
		    break;
 
		case NIC_PCI_DEVICE_ID_900A:
		    User.out.println("10Base-FL NIC found");
		    foundNics.addElement(devInfo[i]);
		    break;
 
		case NIC_PCI_DEVICE_ID_905A:
		    User.out.println("100Base-Fx NIC found");
		    foundNics.addElement(devInfo[i]);
		 break;
    
		case NIC_PCI_DEVICE_ID_9200:
		    User.out.println("Tornado NIC found");
		    foundNics.addElement(devInfo[i]);
		   break;
		    
		case NIC_PCI_DEVICE_ID_9800:
		    User.out.println("10/100 Base-TX NIC(Python-H) found");
		    foundNics.addElement(devInfo[i]);
		    break;
		    
 
		case NIC_PCI_DEVICE_ID_9805:
		    User.out.println("10/100 Base-TX NIC(Python-T) found");
		    foundNics.addElement(devInfo[i]);
		    break;
		    
		default:				
		    User.out.println("ERROR: Unsupported NIC found");
		    continue;
		}				
	    }
	}
	    
	//User.out.println("A total of " + foundNics.size() + " supported network interface cards found!");
	D3C905[] helper = new D3C905[foundNics.size()];
	for(int i=0; i<foundNics.size(); i++) {
	    try {
		Memory [] bufs = new Memory[10];
		for(int j=0; j<bufs.length; j++) {
		    bufs[j] = memMgr.alloc(AdapterLimits.ETHERNET_MAXIMUM_FRAME_SIZE);
		}
		helper[i] = new D3C905((PCIDevice)foundNics.elementAt(i), ports, clock, irq, memMgr, timerManager, cpuManager, null/*etherConsumer*/, bufs); 
	    /*
            // tell the PCI system that we manage all these devices
		// add to the drivers vector
		drivers.addElement(helper);
		if (! pci.manageDevice((PCIDevice)foundNics.elementAt(i), helper)) {
		    User.out.println("PCI rejects our registration :-((");
		}
	    }
	    */
	    } catch (D3C905Exception e) {
		Debug.out.println("ComInit: FATAL ERROR - couldn´t instantiate a driver!");
		throw new Error();
	    }

	    // now do for each NIC basic configuration stuff as enabling busmastering and checking the IRQ-line
	    // and the IO-base
	    // for convience reasons this is done by a separate method
	    if (doBasicConfig((PCIDevice)foundNics.elementAt(i), pci, helper[i]) == false) {
		User.out.println("Basic configuration of NIC " + (PCIDevice)foundNics.elementAt(i) + " failed!!");
		return null;
	    }

        }
	return helper;
    }                    

    // check for busmastering and enable it, read the IRQ-Line and the IO-base

    private boolean doBasicConfig(PCIDevice pcidev, PCIAccess pcibus, D3C905 driver) {
	
	int IRQLine= -1;
	int IOBase = -1;
	int MemBase = -1;
	boolean busMasterCapable = false;
	int cacheLineSize = -1;
	int latency = -1;
	DeviceMemory devicemem;
	// get access to the NicInformation-Object
	NicInformation nicinfo = driver.NicInfo();
	PCIAddress pciaddr = pcidev.getAddress();

	Debug.out.println("DOBasicConfig: " + pcidev);

	if (nicinfo == null)
	    Debug.out.println("ERROR - nicinfo ist null!");
       
	// read the command register of the PCI-Device
	int cmd = pcibus.readDeviceConfig(pciaddr, REG_CMD);

	// AM BESTEN VARIABLE SETZEN UND IM TREIBER ABPRUEFEN
	if ((cmd & 0x100) == 1)
	    Debug.out.println("SERRRRRRR");
	else 
	    Debug.out.println("NO NO NO NO SERRRRRRR");

	// check wether busmastering is already set (by the bios)
	// set it when device is capable
	if ((cmd & CMD_MASK_BM) != 0) {
	    User.out.println("Busmastering for NIC " + pcidev + " already enabled by the Bios!");
	}
	else if (pcidev.busmasterCapable() == true) {
	    User.out.println("Bustmastering for NIC " + pcidev + " not enabled, though device is capable of busmastering.");
	    User.out.println("Trying to enforce it!");
	    if (pcidev.enforceBusmaster() == true) {
		User.out.println("Busmastering enforced for NIC " + pcidev);
	    }
	    else {
		User.out.println("Enforcing Busmastering for NIC " + pcidev + " failed!!");
	    }
	}
	else {
	    User.out.println("NIC " + pcidev + " doesn´t support busmastering!");
	}

	IRQLine = pcidev.readIRQLine();
	User.out.println("NIC " + pcidev + " at IRQline " + IRQLine);
	IOBase = pcidev.getBaseAddress(0);
	// mask the the first two bits 
	IOBase &= PCI_BASE_ADDRESS_IO_MASK;
	MemBase = pcidev.getBaseAddress(1);
	MemBase &= PCI_BASE_ADDRESS_MEM_MASK;
	User.out.println("NIC " + pcidev + " has IOBase " + Integer.toHexString(IOBase));
	User.out.println("NIC " + pcidev + " has MemBase " + Integer.toHexString(MemBase));

	if (IOBase == 0) {
	    User.out.println("The system has not assigned an I/O address for the NIC. I/O Adress will be assigned!");
	    setupIOBase(PCI_BASE_ADDRESS_0, 0x5000, pcidev);
	}

	// some extra testing stuff
	// read PCI Register 1 (PCICommand | PCIStatus) and test for some bits
// 	int comm = pcibus.readConfig(pcidev, 0x01);
// 	comm &= 0x0000ffff;
// 	if ((comm & 0x01) != 0)
// 	    Debug.out.println("ioSpace enabled");
// 	else 
// 	    Debug.out.println("ioSpace disabled");
// 	if ((comm & 0x04) != 0)
// 	    Debug.out.println("busmastering enabled");
// 	else 
// 	    Debug.out.println("busmastering disabled"); 
// 	if ((comm & 0x02) != 0)
// 	    Debug.out.println("memSpace enabled");
// 	else 
// 	    Debug.out.println("memSpace disabled");
    
	// reading the revisionID of the NIC - this just applies to 3C90xBs

	int revision = pcidev.getRevisionID();
	// get the first byte
	byte revision2 = (byte)(revision & 0x000000ff);

	// stimmt das mit den UPPERBITS ODER MUSS ICH NOCH SCHIFTEN UM 8 NACH RECHTS????
	int upperbits = revision2 & 0xE0;      // determine the chip/vendor
	int lowerbits = revision2 & 0x1F;      // encodes the chip revision

	switch (upperbits) {

	case 0:
	    User.out.println("NIC " + pcidev + " has 40-0502-00x ASIC");
	     User.out.println("Revision is: " + (0x1C & lowerbits));
	    break;
	case 1:
	    User.out.println("NIC " + pcidev + " has 40-0483-00x ASIC");
	    User.out.println("Revision is: " + (0x1C & lowerbits));          // just bits [4:2] valid
	    break;
	case 3:
	    User.out.println("NIC " + pcidev + " has 40-0476-001 ASIC");      
	    User.out.println("Revision is: " + (0x1C & lowerbits));         // just bits [4:2] valid
	    break;
	default:
	    User.out.println("NIC " + pcidev + " uses unknown ASIC ?");
	    //bits.printBinary("Unknown Asic -> upperbits are: ", upperbits);
	    break;
	}
  
	// now read the cacheLineSize - may be needed later and applies to 3C90xB only

	cacheLineSize = pcidev.getCacheLineSize();
	
	/*	cacheLineSize = cacheLineSize & 0x000000ff;
	// get bits [6:2]
	cacheLineSize = ((cacheLineSize & 0x7C) >> 2);
	*/
	User.out.println("NIC " + pcidev + " has cacheLineSize of: " + cacheLineSize);
	
	// must be a power of 2
	if (cacheLineSize != 4 || cacheLineSize != 8 || cacheLineSize != 16 || cacheLineSize != 32 || cacheLineSize != 64)
	    User.out.println("Unsupported cacheLineSize - treated as 0!");

	latency = pcidev.getLatencyTimer();

	if (latency < 32) {
	    User.out.println("The latency is very low (" + latency +") - setting to 32!");
	    pcidev.setLatencyTimer((byte)32);
	}
	else {
	    User.out.println("The latency is " + latency);
	}

	// set the power-management to the approbiate state
 
	int pmanage = PCI_PME_STATUS | PCI_POWER_STATE_D0;

	pcidev.writeConfig(PCI_POWER_CONTROL, pmanage);

	// now we write again the iobase to the reg

	pcidev.setBaseAddress(0, IOBase); 

	// now save irq and IOBase in the NicInformation
	nicinfo.irq = IRQLine;
	//nicinfo.PCI.set_IoBaseAddress(IOBase);

	nicinfo.IoBaseAddress = IOBase;
	nicinfo.MemBaseAddress = MemBase;
	nicinfo.Hardware.set_CacheLineSize((byte)(cacheLineSize * 4));

	Befehl befehl = new Befehl(ports, timerManager);
	
	if ((nicinfo.Hardware.get_CacheLineSize() % 0x10) != 0) {
	  Debug.out.println("doBasicConfig: CachelineSize modulo 0x10 not null - setting to 0x20 !");
	  nicinfo.Hardware.set_CacheLineSize((byte)0x20);
	}
	nicinfo.Hardware.set_DeviceId(pcidev.getDeviceID());
	// the PCI_REVISION field - remember that for 3C90xB NICS the bits [4:2] are used to encode the revision
	nicinfo.Hardware.set_RevisionId((byte)revision);
       
	User.out.println("Enable FlowControl by default");
	nicinfo.Hardware.set_FlowControlSupported(true);
	nicinfo.Hardware.set_FlowControlEnable(true);

	// for downpollrate see Technical reference, p.109
	User.out.println("DownPollRate setting to 8 by default");
	nicinfo.Resources.set_DownPollRate(0x8);
	nicinfo.DelayStart = false;
	nicinfo.Hardware.set_MediaOverride(Mii.MEDIA_NONE);
	nicinfo.Resources.set_SendCount(AdapterLimits.NIC_DEFAULT_SEND_COUNT); 
	nicinfo.Resources.set_ReceiveCount(AdapterLimits.NIC_DEFAULT_RECEIVE_COUNT); 

	// we set the Media to autoselection
	nicinfo.Hardware.set_MediaOverride(Mii.MEDIA_AUTO_SELECT);

	return true;
    }


    /*
     * assign an I/O Address to the card - user is responsible for taking an address not already used by some other device
     */ 
    private void setupIOBase(int reg, int base, PCIDevice pcidev) {
    	int addr = pcidev.readConfig(reg);	 // remember old value
	int iobase = base & 0xffffff81;
	pcidev.writeConfig(reg, iobase);
	Debug.out.println("setupIOBase: changed I/O-Base from " + addr + " to " + iobase + "(" + base + ")");
	int a2 = pcidev.readConfig(reg);
	Debug.out.println("setupIOBase: CHECK REGISTERVALUE: " + a2); 
    }

}
