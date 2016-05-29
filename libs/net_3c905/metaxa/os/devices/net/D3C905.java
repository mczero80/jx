package metaxa.os.devices.net;
import java.util.*;
import jx.zero.*;
import jx.zero.debug.*;
import jx.devices.net.NetworkDevice;
import jx.devices.pci.*;
import jx.timer.*;
import jx.buffer.separator.*;
import jx.devices.DeviceConfigurationTemplate;
import jx.devices.DeviceConfiguration;

import jx.buffer.multithread.MultiThreadBufferList;
import jx.buffer.multithread.Buffer;
import jx.buffer.multithread.MultiThreadBufferList2;
import jx.buffer.multithread.Buffer2;

/**
  * the class D3C905 is the main class for the network interface driver 
  * it has methods to handle the initialization (for several different cards with a different way to initialize them)
  * of course it does all the interrupt handling stuff and has methods for every kind of interrupt that can occur
  * the sending of data is also offered by some methods
  * the interfaces this class implements are PCIDevice as the supported NICs are PCI cards
  * IRQHandler indicates that the class deals with the interrupt handling of the device
  * NetworkDevice is the interface that defines the methods a network card at least has to offer (see description there)
  */
public class D3C905 implements FirstLevelIrqHandler, NetworkDevice, MemoryProducer {

    private final static boolean debug = false;
    private final static boolean debugSend = false;
    private final static boolean debugReceive = false;
    private final static boolean debugPacketNotice = false;
    private final static boolean debugLargeMemory = false; // message when getting a memory object that is too large
    private final static boolean assertions = true;

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
  
    final static int NIC_IO_PORT_REGISTERED = 0x00000001;
    final static int NIC_INTERRUPT_REGISTERED = 0x00000002;
    final static int NIC_SHARED_MEMORY_ALLOCATED = 0x00000004;
    final static int NIC_TIMER_REGISTERED = 0x00000010;
  
    final static int NIC_VENDOR_ID = 0x10B7;     
  
    /* for NICSetReceiveMode defining the various receive modes */
    final static int IFF_PROMISC = 0x100;            // receive all packets 
    final static int IFF_ALLMULTI = 0x200;           // receive all multicast packets                                    
    final static int IFF_MULTICAST = 0x1000;         // Supports multicast 
  
    /* starting from here follow the helper classes the driver needs in order to fullfill its tasks
       for example methods for reading the eeprom or the MII */
  
    /*
    private PCIDevInfo PCIInfo;
    private PCIBus bus;
    */
    private Befehl befehl;
    private Eeprom eeprom;
    private Register register;
    private BitPosition bits;
    private Clock clock;
    private WaitTimerGlobal waitglobal;
    private IRQ irq;
    private TimerManager timerManager;
    private MemoryManager memMgr;
    private CPUManager cpuManager;
    private Ports ports;
    private NicInformation Adapter;
    private Mii mii;
  
    /* handles to the beginning of the ring of download and upload managment objects */
    DpdListEntry firstdpd;
    UpdListEntry firstupd;
  
    DpdListEntry firsttestdpd;
  
    /* helper object handles for the management of the descriptor lists */
    DpdListEntry currentdpd;
    UpdListEntry currentupd;
    DpdListEntry previousdpd;
    UpdListEntry previousupd;
    DpdListEntry testhelpdpd;
    DpdListEntry previoustestdpd;
    int count;
    /* Storage needed by the UPDs */
    Memory storage;

    /* the consumer which consumes our received packets */
    NonBlockingMemoryConsumer etherConsumer;
  
    MultiThreadBufferList usableBufs /*, intransmitBufs*/;


    int event_interrupt;
    int event_snd;
    int event_rcv;

    /**
     * @param pcidevice contains information about the PCI NIC
     * @param etherConsumer the consumer of the ethernet packets that arrive at the nic
     * @exception a D3C905Exception is thrown if problems with the allocation of the memory objects needed by the descriptors occur
     */
    public D3C905(PCIDevice pcidevice, Ports ports, Clock clock, IRQ irq, MemoryManager memMgr, TimerManager timerManager, CPUManager cpuManager, NonBlockingMemoryConsumer etherConsumer, Memory[] bufs) throws D3C905Exception {

	this.clock = clock;
	this.irq = irq;
	this.memMgr = memMgr;
	this.timerManager = timerManager;
	this.ports = ports;
	this.etherConsumer = etherConsumer;
	this.cpuManager = cpuManager;

	this.usableBufs = new MultiThreadBufferList2(bufs);
	this.usableBufs.enableRecording("D3C905-available-queue");
	//this.intransmitBufs = new MultiThreadBufferList2();

	// event type
	event_interrupt = cpuManager.createNewEvent("3C905interrupt");
	event_snd = cpuManager.createNewEvent("3C905Snd");
	event_rcv = cpuManager.createNewEvent("3C905Rcv");
	// additionally the irq is also saved in Adapter
	int irqnum = pcidevice.getInterruptLine();

	befehl = new Befehl(ports, timerManager);
	eeprom = new Eeprom(ports, timerManager); 
	register = new Register();
	bits = new BitPosition();
	// for timer handling
	// parameter passing with the timers
	waitglobal = WaitTimerGlobal.instance();
	// the main information class 
	Adapter = new NicInformation(pcidevice);
	mii = new Mii(ports, timerManager);
    
	// register the interrupt-handler
	irq.installFirstLevelHandler(irqnum, this);
    	irq.enableIRQ(irqnum);

	/*
	 * now we construct the lists for sending and reception of data and allocate memory for the buffers
	 * the first one is the download list
	 */
    
	for (count = 0; count < AdapterLimits.NIC_DEFAULT_SEND_COUNT; count++) {
      
	    currentdpd = new DpdListEntry(memMgr);
      
	    if (count == 0) {
		firstdpd = currentdpd;
	    }
	    else {
		previousdpd.Next(currentdpd);
		currentdpd.Previous(previousdpd);
	    }
      
	    previousdpd = currentdpd;
	}
    
	// link first and last one
    
	firstdpd.Previous(currentdpd);
	currentdpd.Next(firstdpd);
	// save the first one in NicInformation
	Adapter.HeadDPD = firstdpd;
	Adapter.TailDPD = firstdpd;
    
	/*
	 * create the upload list 
	 */
    
	for (count = 0; count < AdapterLimits.NIC_DEFAULT_RECEIVE_COUNT; count++) {
      
	    currentupd = new UpdListEntry(memMgr);
      
	    if (count == 0) {
		firstupd = currentupd;
	    }
	    else {
		previousupd.Next(currentupd);
		previousupd.UpNextPointer(currentupd.UPDPhysicalAddress());
		currentupd.Previous(previousupd);
	    }
      
	    previousupd = currentupd;
	} 
	Debug.out.println("D3C905: Allocated buffers for upload:"+AdapterLimits.NIC_DEFAULT_RECEIVE_COUNT);
    
	// link first and last one
	currentupd.Next(firstupd);
	currentupd.UpNextPointer(firstupd.UPDPhysicalAddress());
	firstupd.Previous(currentupd);
    
	// save the first one in NicInformation
	Adapter.HeadUPD = firstupd;
    
	/*
	 * allocate the testbuffers - here we just have DPDs, but with a storage attached
	 */
    
	for (count = 0; count < NicInformation.MAXIMUM_TEST_BUFFERS; count++) {
      
	    testhelpdpd = new DpdListEntry(memMgr);
      
	    if (count == 0) {
		firsttestdpd = testhelpdpd;
	    }
	    else {
		previoustestdpd.Next(testhelpdpd);
		testhelpdpd.Previous(previoustestdpd);
	    }
      
	    storage = memMgr.alloc(AdapterLimits.ETHERNET_MAXIMUM_FRAME_SIZE);
      
	    if (storage == null) {
		Debug.out.println("D3C905: can´t allocate memory for TestDPDs! - Terminating");
		throw new D3C905Exception("Coudn´t allocate memory!");
	    }
      
	    // now store the information about the memory objects in our dpd-object
	    //testhelpdpd.StoreMem(storage, 0, AdapterLimits.ETHERNET_MAXIMUM_FRAME_SIZE);  FIXME!!
      	    testhelpdpd.StoreMem(storage);

	    previoustestdpd = testhelpdpd;
	} 
    
	// link first and last one
    
	firsttestdpd.Previous(testhelpdpd);
	testhelpdpd.Next(firsttestdpd);
    
	// store in NicInformation
	Adapter.TestDPD = firsttestdpd;
    
    }

  
    /**
     * to implement the interface PCIDevice
     * @return the Vendor ID of the NIC (of course it is the 3COM ID)
     */
    /*
    public int getVendorID() {
	return  NIC_VENDOR_ID;
    }
    */
    /**
     * to implement the interface PCIDevice
     * @return the Device ID of the NIC 
     */
    //public int getDeviceID() {
    //return (PCIInfo.id() & 0xffff);
    //}
    /**
     * to implement the interface PCIDevice
     * @return a DeviceInfo object containing some textual information about this PCI device
     */
    /*
    public DeviceInfo getInfo() {
	return new DeviceInfo("3COM 3C90x NIC", "Ethernet Network Interface", "3COM", "DUMMY");
    }
    */
    /** 
     * this is just a dummy mehtod
     * this method should not be called - instead the probe() of ComInit does the desired stuff
     * see the description on how to find and initialize the cards in the docu (Studienarbeit)
     */
    /* 
    public boolean probe(Bus bus) {
	return true;
    }
    */
    /** 
     * give access to the main information object (class NicInformation)
     * @return handle to the information object 
     */
    public NicInformation NicInfo() {
	return Adapter;
    }
    /**
     * method to get the hardware address of the interface card (this information is stored in the Eeprom)
     * the EtherLayer for example needs to know the address of the NIC to insert it into the EtherFormat Packet
     * @return a byte array containing the hardware address
     */
    public byte[] getMACAddress() {
	byte[] ret = new byte[6];
	for (int i=0; i<6; i++)
	    ret[i] = NicInfo().StationAddress[i];
	return ret;
    }

    /**
     * higher layers need to know how much data the underlying network can transport in one packet
     * this method returns the maximum size of an ethernet frame (which itself contains just information by definition;
     * the calling layers have to remember that also ethernet has some header which first has to be subtracted
     * @return the maximum data size of an ethernet frame
     */
    public int getMTU() {
	return metaxa.os.devices.net.AdapterLimits.ETHERNET_MAXIMUM_FRAME_SIZE;
    }
  
    /*
     *
     * starting from here the methods for the initialization of the network card follow
     *
     *
     */
  

    private boolean GetAdapterProperties() throws NicStatusFailure {
   
	short eepromValue;
	short intStatus;
	// helper for determining the compatability
	short compatabilityhelper;
	// helper for the softwareinformation stuff
	short information;
	CompatabilityWord compatability;
	SoftwareInformation1 information1;
	SoftwareInformation2 information2;
	CapabilitiesWord capabilities = null;
	byte value, index;
     
	if (debug) Debug.out.println("GetAdapterProperties: IN ");
	if (debug) Debug.out.println("I/O-BASE ist laut Adapter: " + Adapter.IoBaseAddress + " hex: " + Integer.toHexString(Adapter.IoBaseAddress));

	// at the beginning we issue a global reset to the card - this is needed because otherwise the testadapter method won't work
	// after this command we have a consistent starting point
	befehl.NicCommandWait(Adapter, (short)( 
					       befehl.COMMAND_GLOBAL_RESET() |
					       befehl.GLOBAL_RESET_MASK_TP_AUI_RESET() | 
					       befehl.GLOBAL_RESET_MASK_ENDEC_RESET()));
     
	//
	// Check the ASIC type. Udp checksum should not be used in
	// pre-Tornado cards-  JRR
	//
     
	if ((Adapter.Hardware.get_DeviceId() == NIC_PCI_DEVICE_ID_9055) ||
	    (Adapter.Hardware.get_DeviceId() == NIC_PCI_DEVICE_ID_9004) ||
	    (Adapter.Hardware.get_DeviceId() == NIC_PCI_DEVICE_ID_9005) ||
	    (Adapter.Hardware.get_DeviceId() == NIC_PCI_DEVICE_ID_9006) ||
	    (Adapter.Hardware.get_DeviceId() == NIC_PCI_DEVICE_ID_9058) ||
	    (Adapter.Hardware.get_DeviceId() == NIC_PCI_DEVICE_ID_900A) ||
	    (Adapter.Hardware.get_DeviceId() == NIC_PCI_DEVICE_ID_905A) || 
	    (Adapter.Hardware.get_DeviceId() == NIC_PCI_DEVICE_ID_9800)) { 
       
	    Adapter.Hardware.set_BitsInHashFilter(0x40);
	}
	else {
       
	    Adapter.Hardware.set_BitsInHashFilter(0x100);
	    Adapter.Hardware.set_UDPChecksumErrDone(true);
	}
     
	Adapter.Hardware.set_Status(NicHardwareInformation.HARDWARE_STATUS_WORKING);
	Adapter.Hardware.set_LinkSpeed(Softlimits.LINK_SPEED_100);
     
	//
	// Make sure we can see the adapter.  Set up for window 7, and make
	// sure the window number gets reflected in status. 
	//
	intStatus =0;
	int MAX_RETRIES = 10;
	Debug.out.println("GetAdapterProperties: Select window 7 ");
	for(int xx=0; xx<MAX_RETRIES; xx++) {
	    befehl.NicCommandWait(Adapter, (short)(Befehl.COMMAND_SELECT_REGISTER_WINDOW() | Register.REGISTER_WINDOW_7()));
	    
	    
	    intStatus = befehl.NicReadPortShort(Adapter, Register.INTSTATUS_COMMAND_REGISTER());
	    
	    if ((intStatus & Register.REGISTER_WINDOW_MASK()) != (Register.REGISTER_WINDOW_7() << 13)) {
		Debug.out.print(".");
		/*
		Debug.out.println("GetAdapterProperties: Window selection failure");
		Debug.out.println("GetAdapterProperties: Out with error");
		Debug.out.println("intStatus: "+intStatus);
		Debug.out.println("expected intstatus & "+Register.REGISTER_WINDOW_MASK+": "+(Register.REGISTER_WINDOW_7 << 13));
		throw new NicStatusFailure("GetAdapterProperties: error in setting for register window 7");
		*/
	    } else break;
	}
	if ((intStatus & Register.REGISTER_WINDOW_MASK()) != (Register.REGISTER_WINDOW_7() << 13)) {
	    
	    Debug.out.println("GetAdapterProperties: Window selection failure");
	    Debug.out.println("GetAdapterProperties: Out with error");
	    Debug.out.println("intStatus: "+intStatus);
	    Debug.out.println("expected intstatus & "+Register.REGISTER_WINDOW_MASK()+": "+(Register.REGISTER_WINDOW_7() << 13));
	    throw new NicStatusFailure("GetAdapterProperties: error in setting for register window 7");
	    
	}

     



	try {
	    information = eeprom.ReadEEPROM(Adapter, eeprom.EEPROM_DEVICE_ID);
	    if (debug) Debug.out.println("Software DeviceID: "+Integer.toHexString(information));
	}
	catch (NicStatusFailure e) 
	    {
		Debug.out.println("GetAdapterProperties: EEPROM read failed");
		throw new NicStatusFailure("GetAdapterProperties: EEPROM read failed");
	 
	    }





	//
	// ----------------- Read the compatability level ----------
	//
     
	// what we now do is driver version stuff
	// we check the level in the compatabilityword in the EEPROM of the NIC
	// if these values don´t compare with our predefined values, we quit
	// another kind of driver needs to be installed (well, I should develop this one :-))
     
	try {
	    compatabilityhelper = eeprom.ReadEEPROM(Adapter, eeprom.EEPROM_COMPATABILITY_WORD);
	}
	catch (NicStatusFailure e) 
	    {
		Debug.out.println("GetAdapterProperties: compatability read failed");
		throw e;
	    }
     
	// now set the compatability information
	// first the lower byte, then the upper byte
     
	compatability = new CompatabilityWord(((byte)(compatabilityhelper & 0x00FF)), ((byte)(compatabilityhelper & 0xFF00)));
     
	//
	// Check the Failure level - see above for explanation
	//
     
	if (compatability.get_failurelevel() > eeprom.EEPROM_COMPATABILITY_LEVEL) {
       
	    Debug.out.println("GetAdapterProperties: Incompatible failure level");
	    Debug.out.println("GetAdapterProperties: Out with error\n");
	    throw new NicStatusFailure("GetAdapterProperties: NICs failure level bigger than the allowed one");
	}
     
	//
	// Check the warning level.
	//
     
	if (compatability.get_warninglevel() > eeprom.EEPROM_COMPATABILITY_LEVEL) {  
	    Debug.out.println("GetAdapterProperties: Wrong down compatability level");
	}
	
	//
	// ----------------- Read the software information 1 -------
	//
     
	try {
	    information = eeprom.ReadEEPROM(Adapter, eeprom.EEPROM_SOFTWARE_INFORMATION_1);
	    if (debug) Debug.out.println("Software Information1: "+Integer.toHexString(information));
	}
	catch (NicStatusFailure e) 
	    {
		Debug.out.println("GetAdapterProperties: EEPROM s/w info1 read failed");
		throw new NicStatusFailure("GetAdapterProperties: EEPROM s/w info1 read failed");
	 
	    }
     
	// now lets set up the softwareinformation1
	// first get bits [5:4] for the optimizeFor information
	byte optimize = (byte)((information & 0x0030) >> 4);
	// then check for bit 14 and bit 15
	boolean link;
	boolean duplex;
	try {
	    if (bits.isSet(information,14) == true)
		link = true;
	    else 
		link = false;
	    if (bits.isSet(information,15) == true) 
		duplex = true;
	    else
		duplex = false;
	}
	catch (BitNotExistingException e) 
	    {
		Debug.out.println("GetAdapterProperties:Error while checking for bits in softwareinformation read from EEPROM");
		throw new NicStatusFailure("GetAdapterProperties: Error while checking for bits in softwareinformation read from EEPROM");
	    }
     
	// now we have the information to set up the softwareinformation1
	information1 = new SoftwareInformation1(optimize, link, duplex);
     
     
	if (information1.get_LinkBeatDisable()) {
	    if (debug) Debug.out.println("GetAdapterProperties: s/w information1 - Link beat disable");
	    Adapter.Hardware.set_LinkBeatDisable(true);
       
	}
	if (Adapter.Hardware.get_DuplexCommandOverride() == false) {
	    if (information1.get_FullDuplexMode()) {
		if (debug) Debug.out.println("GetAdapterProperties: s/w information1 - Full duplex enable");
		Adapter.Hardware.set_FullDuplexEnable(true);
	    }
	    else {
		if (debug) Debug.out.println("GetAdapterProperties: s/w information 1 - Full duplex disabled");
		Adapter.Hardware.set_FullDuplexEnable(false);
	    }
	}
     
	switch (information1.get_OptimizeFor()) {
       
	case SoftwareInformation1.EEPROM_OPTIMIZE_FOR_THROUGHPUT:
       
	    if (debug) Debug.out.println("GetAdapterProperties: sw info1 - optimize throughput");
	    Adapter.Hardware.set_OptimizeForThroughput(true);
	    break;
       
	case SoftwareInformation1.EEPROM_OPTIMIZE_FOR_CPU:
	    
	    //Debug.out.println("GetAdapterProperties: s/w info1 - optimize CPU");
	    Adapter.Hardware.set_OptimizeForCPU(true);
	    break;
       
	case SoftwareInformation1.EEPROM_OPTIMIZE_NORMAL:
       
	    //Debug.out.println("GetAdapterProperties: s/w info1 - optimize Normal");
	    Adapter.Hardware.set_OptimizeNormal(true);
	    break;
       
	default:
	    if (debug) Debug.out.println("GetAdapterProperties: Wrong optimization level: "+information1.get_OptimizeFor());
	    throw new NicStatusFailure("GetAdapterProperties: Wrong optimization level");
       
       
	}
     
	// ----------------- Read the capabilities information -----
	//
	// now we check for the capabilites of the NIC - see Technical Reference, p.85 for further information
     
	// reset information
	information = 0;
     
	try {
	    information = eeprom.ReadEEPROM(Adapter, eeprom.EEPROM_CAPABILITIES_WORD);
	}
	catch (NicStatusFailure e)
	    {
		Debug.out.println("GetAdapterProprties: EEPROM s/w capabilities read failed\n");
		throw new NicStatusFailure("GetAdapterProprties: EEPROM s/w capabilities read failed\n");
	    }
     
	// now we have to setup the capabilities 
     
	try {
	    capabilities = new CapabilitiesWord(bits.isSet(information,0),bits.isSet(information,1),bits.isSet(information,2),
						bits.isSet(information,3),bits.isSet(information,4),bits.isSet(information,5),
						bits.isSet(information,6),bits.isSet(information,7),bits.isSet(information,8),
						bits.isSet(information,9),bits.isSet(information,10),bits.isSet(information,11),
						bits.isSet(information,12),bits.isSet(information,13));
	}
	catch (BitNotExistingException e) 
	    {
		Debug.out.println("GetAdapterProperties: Error while checking for bits in capabilitesword read from EEPROM");
		throw new NicStatusFailure("GetAdapterProperties: Error while checking for bits in capabilitesword read from EEPROM");
	    }
     
	if (capabilities.get_SupportsPowerManagement()) {
       
	    Debug.out.println("GetAdapterProperties: Adapter supports power management");
	    Adapter.Hardware.set_SupportsPowerManagement(true);
	}
     
	//
	// ----------------- Read the software information 2 -------
	//
	// we obtain additional information, but its only useful for 90xB NICs
     
	// reset information
	information = 0;
     
	try {
	    information = eeprom.ReadEEPROM(Adapter, eeprom.EEPROM_SOFTWARE_INFORMATION_2);
	}
	catch (NicStatusFailure e) 
	    {
		Debug.out.println("GetAdapterProperties: ReadEEPROM , SWINFO2 failed");
		Debug.out.println("GetAdapterProperties: Out with error");
		throw new NicStatusFailure("GetAdapterProperties: ReadEEPROM , SWINFO2 failed");
	    }
     
	// set up the software information 2 
	// there seems to be more information in the EEPROM than the version of my Technical Reference offers
     
	try {
	    information2 = new SoftwareInformation2(bits.isSet(information,1),bits.isSet(information,2),
						    bits.isSet(information,3),bits.isSet(information,4),
						    bits.isSet(information,5),bits.isSet(information,6),
						    bits.isSet(information,7));
	}
	catch (BitNotExistingException e) 
	    {
		Debug.out.println("GetAdapterProperties:Error while checking for bits in softwareinformation2 read from EEPROM");
		throw new NicStatusFailure("GetAdapterProperties: Error while checking for bits in softwareinformation2 read from EEPROM");  
	    }
	if (information2.get_BroadcastRxErrDone()){
       
	    //Debug.out.println("GetAdapterProperties: Adapter has BroadcastRxErrDone");
	    Adapter.Hardware.set_BroadcastErrDone(true);
    	}
	
	if (information2.get_MWIErrDone()) {
       
	    //Debug.out.println("GetAdapterProperties: Adapter has MWIErrDone");
	    Adapter.Hardware.set_MWIErrDone(true);
	}
	
	if (information2.get_WOLConnectorPresent()){
       
	    //Debug.out.println("GetAdapterProperties: WOL is connected");
	    Adapter.Hardware.set_WOLConnectorPresent(true);
	}
     
	if (information2.get_AutoResetToD0()) {
       
	    //Debug.out.println("GetAdapterProperties: Auto reset to D0 bit on");
	    Adapter.Hardware.set_AutoResetToD0(true);
	}
     
	//
	// ----------------- Read the OEM station address ----------
	//
     
	// now get the address of the card - this value is the one to be programmed into the StationAddress register
	// for 3COM NICs it may contain the same value as in the 3COM node address, but OEMs may programm a different 
	// value - for further information, see Technical Reference p.83
     
	// reset information
	information = 0;
     
	try {
	    information = eeprom.ReadEEPROM(Adapter, eeprom.EEPROM_OEM_NODE_ADDRESS_WORD_0);
	}
	catch (NicStatusFailure e) 
	    {
	 
		//Debug.out.println("GetAdapterProperties: EEPROM read word 0 failed");
		//Debug.out.println("GetAdapterProperties: Out with error");
		throw new NicStatusFailure("GetAdapterProperties: EEPROM read word 0 failed");
	    }
     
	// set up the address field in NicInformation
	Adapter.PermanentAddress[0] = bits.hibyte(information);
	Adapter.PermanentAddress[1] = bits.lobyte(information);
     
	// reset information
	information = 0;
     
	try {
	    information = eeprom.ReadEEPROM(Adapter, eeprom.EEPROM_OEM_NODE_ADDRESS_WORD_1);
	}
	catch (NicStatusFailure e) 
	    {
		Debug.out.println("GetAdapterProperties: EEPROM read word 1 failed");
		Debug.out.println("GetAdapterProperties: Out with error");
		throw new NicStatusFailure("GetAdapterProperties: EEPROM read word 1 failed");	
	    }
     
	Adapter.PermanentAddress[2] = bits.hibyte(information);
	Adapter.PermanentAddress[3] = bits.lobyte(information);
     
	// reset information
	information = 0;
     
	try {
	    information = eeprom.ReadEEPROM(Adapter, eeprom.EEPROM_OEM_NODE_ADDRESS_WORD_2);
	}
	catch (NicStatusFailure e) 
	    {
		Debug.out.println("GetAdapterProperties: EEPROM read word 2 failed");
		Debug.out.println("GetAdapterProperties: Out with error");
		throw new NicStatusFailure("GetAdapterProperties: EEPROM read word 2 failed");
	    }
     
	Adapter.PermanentAddress[4] = bits.hibyte(information);
	Adapter.PermanentAddress[5] = bits.lobyte(information);
     
	//
	// If the station address has not been overriden, fill the permanent
	// address into it.
	//
     
	value = (byte)(Adapter.StationAddress[0] |
		       Adapter.StationAddress[1] |
		       Adapter.StationAddress[2] |
		       Adapter.StationAddress[3] |
		       Adapter.StationAddress[4] |
		       Adapter.StationAddress[5]);
     
	//
	// If the station address has not been overriden, set this value
	// in the station address.
	//
     
	if (value == 0x0) { 
       
	    for (index=0; index < 6; index++) 
		Adapter.StationAddress[index] = Adapter.PermanentAddress[index];
	}
     
	//Debug.out.println("GetAdapterProperties: OUT ");
	return true;
     
    }
    
    private boolean BasicInitializeAdapter() throws NicStatusFailure {
	
	short stationTemp, macControl;
	
	//Debug.out.println("BasicInitializeAdapter: In");

	//
	// ----------------- Tx Engine handling --------------------
	//

	if (!befehl.NicCommandWait(Adapter, (short)befehl.COMMAND_TX_DISABLE())) {
	    Debug.out.println("BasicInitializeAdapter: COMMAND_TX_DISABLE failed");
	    Debug.out.println("BasicInitializeAdapter: Out with error");
            throw new NicStatusFailure("BasicInitializeAdapter: COMMAND_TX_DISABLE failed");
    
    	}

	
	if (!befehl.NicCommandWait(Adapter, (short)(befehl.COMMAND_TX_RESET() | befehl.TX_RESET_MASK_NETWORK_RESET()))) {

	    Debug.out.println("BasicInitializeAdapter: COMMAND_TX_RESET failed");
	    Debug.out.println("GetAdapterProperties: Out with error");
	    throw new NicStatusFailure("BasicInitializeAdapter: COMMAND_TX_RESET failed");
    	}
	//
	// ----------------- Rx engine handling --------------------
	//

	if(!befehl.NicCommandWait(Adapter, (short)befehl.COMMAND_RX_DISABLE())) {

	    Debug.out.println("BasicInitializeAdapter: Rx disable failed");
	    Debug.out.println("BasicInitializeAdapter: Out with error");
	    throw new NicStatusFailure("BasicInitializeAdapter: Rx disable failed");	
	}

	
	
	if (!befehl.NicCommandWait(Adapter, (short)(befehl.COMMAND_RX_RESET() | befehl.RX_RESET_MASK_NETWORK_RESET()))) {

	    Debug.out.println("BasicInitializeAdapter: Rx reset failed");
	    Debug.out.println("BasicInitializeAdapter: Out with error");
	    throw new NicStatusFailure("BasicInitializeAdapter: Rx reset failed");
    	}
	//
	// Take care of the interrupts.
	//

	befehl.NicAcknowledgeAllInterrupt(Adapter);
	befehl.NicCommand(Adapter, befehl.COMMAND_STATISTICS_DISABLE());

    	befehl.NicCommand(Adapter, befehl.COMMAND_SELECT_REGISTER_WINDOW() | Register.REGISTER_WINDOW_6());
	
	//
	// Clear the statistics from the hardware - reading of those registers clears then
	// see Technical Reference p.141
	//
	
	befehl.NicReadPortByte(Adapter, Register.CARRIER_LOST_REGISTER());
	befehl.NicReadPortByte(Adapter, Register.SQE_ERRORS_REGISTER());
	befehl.NicReadPortByte(Adapter, Register.MULTIPLE_COLLISIONS_REGISTER());
	befehl.NicReadPortByte(Adapter, Register.SINGLE_COLLISIONS_REGISTER());
	befehl.NicReadPortByte(Adapter, Register.LATE_COLLISIONS_REGISTER());
	befehl.NicReadPortByte(Adapter, Register.RX_OVERRUNS_REGISTER());
	befehl.NicReadPortByte(Adapter, Register.FRAMES_TRANSMITTED_OK_REGISTER());
	befehl.NicReadPortByte(Adapter, Register.FRAMES_RECEIVED_OK_REGISTER());
	befehl.NicReadPortByte(Adapter, Register.FRAMES_DEFERRED_REGISTER());
	befehl.NicReadPortByte(Adapter, Register.UPPER_FRAMES_OK_REGISTER());
	befehl.NicReadPortShort(Adapter, Register.BYTES_RECEIVED_OK_REGISTER());
	befehl.NicReadPortShort(Adapter, Register.BYTES_TRANSMITTED_OK_REGISTER());

	befehl.NicCommand(Adapter, Befehl.COMMAND_SELECT_REGISTER_WINDOW() | Register.REGISTER_WINDOW_4());

	befehl.NicReadPortByte(Adapter, Register.BAD_SSD_REGISTER());
	befehl.NicReadPortByte(Adapter, Register.UPPER_BYTES_OK_REGISTER());
	
	//
	// Program the station address
	//
	
	befehl.NicCommand(Adapter, Befehl.COMMAND_SELECT_REGISTER_WINDOW() | Register.REGISTER_WINDOW_2());

	// we construct the words out of the bytes contained in Adapter.StationAddress
	// as described in technical reference, p.127, we must watch out for the following ->
	// if the address for example is 00:20:af:12:34:56 we have to write 0x2000 to offset 0, 0x12af to offset1 and 0x5643 to offset 3
	// another problem can arise with the "or" of the shifted value and the next value
	// if the value we want to "or" to the shifted value has its most significant bit set, the result is not the desired one, but the shifted
	// value becomes 0xff "or-ed" with the other value
	// for example: first byte: 0xe0, shift 8 bits -> 0xe000, now "or" 0xed to this -> we don't get 0xe0ed as expected, but 0xffed
	// so we have to "and" it with 0xff and after that we can "or" it otherwise we get the above described behaviour because of the promotion of the
	// value with its most significant bit set to int

	stationTemp = (short)(Adapter.StationAddress[1] << 8);
	stationTemp |= (Adapter.StationAddress[0] & 0xff);

	befehl.NicWritePortShort(Adapter, Register.STATION_ADDRESS_LOW_REGISTER(), stationTemp);

	stationTemp = (short)(Adapter.StationAddress[3] << 8);
	stationTemp |= (Adapter.StationAddress[2] & 0xff);

	befehl.NicWritePortShort(Adapter, Register.STATION_ADDRESS_MID_REGISTER(), stationTemp);

	stationTemp = (short)(Adapter.StationAddress[5] << 8);
	stationTemp |= (Adapter.StationAddress[4] & 0xff);

	befehl.NicWritePortShort(Adapter, Register.STATION_ADDRESS_HIGH_REGISTER(), stationTemp);

	Debug.out.println("NIC-Addresse (geschrieben ins StationAddressRegister): " + Integer.toHexString(Adapter.StationAddress[0] & 0xff) +":"+Integer.toHexString(Adapter.StationAddress[1] & 0xff)+":"+Integer.toHexString(Adapter.StationAddress[2] &0xff)+":"+Integer.toHexString(Adapter.StationAddress[3] & 0xff)+":"+Integer.toHexString(Adapter.StationAddress[4] & 0xff)+":"+Integer.toHexString(Adapter.StationAddress[5] &0xff ));
	
	befehl.NicWritePortShort(Adapter, 0x6, (short)0);
	befehl.NicWritePortShort(Adapter, 0x8, (short)0);
	befehl.NicWritePortShort(Adapter, 0xA, (short)0);
	befehl.NicCommand(Adapter, befehl.COMMAND_STATISTICS_ENABLE());
	
	//
	// Clear the mac control register
	//
	
	befehl.NicCommand(Adapter, befehl.COMMAND_SELECT_REGISTER_WINDOW() | Register.REGISTER_WINDOW_3());

	macControl = befehl.NicReadPortShort(Adapter, Register.MAC_CONTROL_REGISTER());
	macControl &= 0x1;
	befehl.NicWritePortShort(Adapter, Register.MAC_CONTROL_REGISTER(), macControl);

	Debug.out.println("BasicInitializeAdapter: Out with success");

	return true;

    }


    private static final int SLOWDOWN = 1;
    private boolean TestAdapter() throws NicStatusFailure {
	
	Memory mem, dpd, upd;
	int TimeOutCount;
	int count;
	short networkDiagnosticsValue;
	DpdListEntry testdpd;
	
	// timer-object implementing Timer Interface
	WaitTimer waittimer = new WaitTimer(ports, timerManager);

	//Debug.out.println("TestAdapter: IN ");

	//
	// Select the network diagnostics window.
	//

	befehl.NicCommand(Adapter, befehl.COMMAND_SELECT_REGISTER_WINDOW() | Register.REGISTER_WINDOW_4());

	//
	// Read the network diagnostics register - see Technical Reference p.203
	// 

	networkDiagnosticsValue = befehl.NicReadPortShort(Adapter, Register.NETWORK_DIAGNOSTICS_REGISTER());
	
	//
	// Enable loop back on the adapter
	// we switch the fifo loopback on - this means, that the data loopbacks directly from the transmit fifo
	// into the receive fifo
	// but we must not load more than one transmit packet into to the fifo at a time, otherwise we may loose data
	//

	networkDiagnosticsValue |= BitPosition.bit_12();
	befehl.NicWritePortShort(Adapter, Register.NETWORK_DIAGNOSTICS_REGISTER(), networkDiagnosticsValue);

	befehl.NicCommand(Adapter, befehl.COMMAND_TX_ENABLE());
	befehl.NicCommand(Adapter, befehl.COMMAND_RX_ENABLE());
	
	//
	// Write the address to the UpListPointer register
	//
	
	befehl.NicWritePortLong(Adapter, Register.UP_LIST_POINTER_REGISTER(), Adapter.HeadUPD.UPDPhysicalAddress());
	befehl.NicCommand(Adapter, befehl.COMMAND_UP_UNSTALL());
	
	testdpd = Adapter.TestDPD;
	// get the first buffer memory
	mem = testdpd.StoreMem();
	// now we generate a test packet - just writing increasing numbers into it 
	for (count=0; count < AdapterLimits.ETHERNET_MAXIMUM_FRAME_SIZE; count++) {
	    mem.set8((int)count,(byte)count);
	}

	// write the FrameStartHeader
	testdpd.FrameStartHeader(AdapterLimits.ETHERNET_MAXIMUM_FRAME_SIZE); 
	// just one entry - set the next pointer to 0
	testdpd.DownNextPointer(0);

	befehl.NicCommandWait(Adapter, (short)befehl.COMMAND_DOWN_STALL());
	
	//
	// Write the down list pointer register
	//

	befehl.NicWritePortLong(Adapter, Register.DOWN_LIST_POINTER_REGISTER(), testdpd.DPDPhysicalAddress());

	befehl.NicCommand(Adapter, befehl.COMMAND_DOWN_UNSTALL());

	//
	// Check that packet has been picked up by the hardware
	//
	waitglobal.set_PortV(befehl.NicReadPortLong(Adapter, Register.DOWN_LIST_POINTER_REGISTER()));

	ComInit.udelay(10);

	if(waitglobal.get_PortV() == testdpd.DPDPhysicalAddress()) {	
	    Adapter.WaitCases = WaitCases.CHECK_DOWNLOAD_STATUS;

	    // initialize a timer entry and set for 1 second
	    TimeOutCount = timerManager.getCurrentTimePlusMillis(1000*SLOWDOWN);
	    WaitTimerArg arg = new WaitTimerArg(Adapter, testdpd, TimeOutCount);
	    timerManager.addMillisTimer(10*SLOWDOWN, waittimer, arg); 

	    /* first variant: poll the timer manager */
	    /*
	    Debug.out.println("TIMEOUT:"+TimeOutCount);
	    Debug.out.println("CURRENT:"+timerManager.getCurrentTime());
	    while(TimeOutCount > timerManager.getCurrentTime()) {
		Debug.out.print(" "+TimeOutCount+"."+timerManager.getCurrentTime()+" ");
		cpuManager.yield();
	    }
	    */

	    /* second variant: use a timer to unblock this thread and block */
	    cpuManager.clearUnblockFlag();
	    Debug.out.println("waiting 3 seconds");
	    timerManager.unblockInMillis(cpuManager.getCPUState(), 3000*SLOWDOWN);
	    cpuManager.blockIfNotUnblocked();

	    if(waitglobal.get_PortV() == testdpd.DPDPhysicalAddress()) {	
		Debug.out.println("TestAdapter: Packet not picked up by the hardware");
		throw new NicStatusFailure();
	    }
	}
	//
	// Check the upload information
	//

	waitglobal.set_PortV(befehl.NicReadPortLong(Adapter, Register.UP_LIST_POINTER_REGISTER()));

	ComInit.udelay(10);

	if(waitglobal.get_PortV() == Adapter.HeadUPD.UPDPhysicalAddress())
	    {	Adapter.WaitCases = WaitCases.CHECK_UPLOAD_STATUS;
	    TimeOutCount = timerManager.getCurrentTimePlusMillis(4000);
	    WaitTimerArg arg = new WaitTimerArg(Adapter, testdpd, TimeOutCount);
	    timerManager.addMillisTimer(1000, waittimer, arg);
	    while(TimeOutCount > timerManager.getCurrentTime());
	    if(waitglobal.get_PortV() == Adapter.HeadUPD.UPDPhysicalAddress()) {
		Debug.out.println("TestAdapter: Packet not uploaded by adapter");
		throw new NicStatusFailure();
	    }
	    }
	
	//
	// Check the contents of the packet
	//
	
	upd = Adapter.HeadUPD.StoreMem();

   	for (count=0; count < AdapterLimits.ETHERNET_MAXIMUM_FRAME_SIZE; count++) {
	    //Debug.out.println("get8:"+ upd.get8((int)count) + ", (byte)count:"+ ((byte)count)); 
	    if (upd.get8((int)count) != (byte)count) 
		break;
	}
            
	if (AdapterLimits.ETHERNET_MAXIMUM_FRAME_SIZE != count) {
	    Debug.out.println("TestAdapter: Receive buffer contents not ok at count "+count);
	    throw new NicStatusFailure();

	}

	// clear the Up Packet Status of the first UPD
	Adapter.HeadUPD.UpPacketStatus(0);

	befehl.NicWritePortLong(Adapter, Register.UP_LIST_POINTER_REGISTER(), 0);
	befehl.NicAcknowledgeAllInterrupt(Adapter);

	//
	// Issue transmit and receive reset here
	//

	if (!befehl.NicCommandWait(Adapter, (short)(befehl.COMMAND_TX_RESET() | befehl.TX_RESET_MASK_NETWORK_RESET()))) {
	    Debug.out.println("TestAdapter: Transmit reset failed");
	    throw new NicStatusFailure();
    	}

	if (!befehl.NicCommandWait(Adapter, (short)(befehl.COMMAND_RX_RESET() | befehl.RX_RESET_MASK_NETWORK_RESET()))) {
	    Debug.out.println("TestAdapter: Receiver reset failed");
	    throw new NicStatusFailure();
	}       

	//
	// Clear the loop back bit in network diagnostics
	//

	networkDiagnosticsValue &= ~BitPosition.bit_12();
	befehl.NicWritePortShort(Adapter, Register.NETWORK_DIAGNOSTICS_REGISTER(), networkDiagnosticsValue);
	//Debug.out.println("TestAdapter: OUT");
	return true;
    }


    private boolean StartAdapter() throws NicStatusFailure {
	
	DpdListEntry headDPD;
	int TimeOutCount;
	short diagnostics;
	int dmaControl;

	// Debug.out.println("StartAdapter: In");

	//
	// Enable upper bytes counting in diagnostics register
	//

	befehl.NicCommand(Adapter, befehl.COMMAND_SELECT_REGISTER_WINDOW() | Register.REGISTER_WINDOW_4());

	diagnostics = befehl.NicReadPortShort(Adapter, Register.NETWORK_DIAGNOSTICS_REGISTER());

	diagnostics |= Register.NETWORK_DIAGNOSTICS_UPPER_BYTES_ENABLE();

	befehl.NicWritePortShort(Adapter, Register.NETWORK_DIAGNOSTICS_REGISTER(), diagnostics);
	
	//
	// Enable counter speed in DMA control
	//

	dmaControl = befehl.NicReadPortLong(Adapter, Register.DMA_CONTROL_REGISTER());
	
	if (100000000 == Adapter.Hardware.get_LinkSpeed())
	    dmaControl |= Register.DMA_CONTROL_COUNTER_SPEED();

	befehl.NicWritePortLong(Adapter, Register.DMA_CONTROL_REGISTER(), (int)dmaControl);        
	
	//
	// ------------ Give download structures to the adapter -----
	//
	// Stall the download engine
	//
	
	if (!befehl.NicCommandWait(Adapter, (short)befehl.COMMAND_DOWN_STALL())) {
	    Debug.out.println("StartAdapter: down stall failed");
	    throw new NicStatusFailure();
    	}

	//
	// Use the head DPD to mark it as dummy
	//

	headDPD = Adapter.HeadDPD;
	headDPD.FrameStartHeader(AdapterLimits.FSH_DPD_EMPTY());

	//
	// Now move head and tail to next one
	//

	Adapter.HeadDPD = headDPD.Next();
	Adapter.TailDPD = headDPD.Next();

	//
	// Write the first DPD address to the hardware
	//

	befehl.NicWritePortLong(Adapter, Register.DOWN_LIST_POINTER_REGISTER(), headDPD.DPDPhysicalAddress());

	//
	// Enable down polling on the hardware
	//

	befehl.NicWritePortByte(Adapter, Register.DOWN_POLL_REGISTER(), (byte)Adapter.Resources.get_DownPollRate());

	//
	// Unstall the download engine
	//

	befehl.NicCommand(Adapter, befehl.COMMAND_DOWN_UNSTALL());

	//
	// ------------ Give upload structures to the adapter -------
	//
	//
	// Stall the upload engine.
	//

	if (!befehl.NicCommandWait(Adapter, (short)befehl.COMMAND_UP_STALL())) {
	    Debug.out.println("StartAdapter: up stall failed");
	    throw new NicStatusFailure();
    	}

	//
	// Give the address of the first UPD to the adapter
	//

	befehl.NicWritePortLong(Adapter, Register.UP_LIST_POINTER_REGISTER(), Adapter.HeadUPD.UPDPhysicalAddress());

	//
	// Write the up poll register
	//

	befehl.NicWritePortByte(Adapter, Register.UP_POLL_REGISTER(), (byte)8);

	//
	// Unstall the download engine
	//

	befehl.NicCommand(Adapter, befehl.COMMAND_UP_UNSTALL());

	//
	// Enable the statistics back
	//

	befehl.NicCommand(Adapter, befehl.COMMAND_STATISTICS_ENABLE());

	//
	// Acknowledge any pending interrupts
	//

	befehl.NicAcknowledgeAllInterrupt(Adapter);

	//
	// Enable indication for all interrupts
	//

	befehl.NicEnableAllInterruptIndication(Adapter);

	//
	// Enable all interrupts to the host
	//

	befehl.NicUnmaskAllInterrupt(Adapter);

	//
	// Enable the transmit and receive engines.
	//

	befehl.NicCommand(Adapter, befehl.COMMAND_RX_ENABLE());
	befehl.NicCommand(Adapter, befehl.COMMAND_TX_ENABLE());

	//
	// Delay three seconds, only some switches need this,
	// default is no delay, user can enable this delay in command line
	//

	if(Adapter.DelayStart == true) {
	    Debug.out.println("D3C905: waiting 3 seconds ...");
	    ComInit.sleep(3000, 0);
	    Debug.out.println("D3C905: back again");
	} 

	befehl.NicCommand(Adapter, befehl.COMMAND_SELECT_REGISTER_WINDOW() | Register.REGISTER_WINDOW_5());
	byte rxfilter = befehl.NicReadPortByte(Adapter, Register.RX_FILTER_REGISTER());

	// for debugging purposes
	//Debug.out.println("STARTADAPTER: RxFilter -> : " + rxfilter);

	// now set the receive filter to receive just packets addressed to the card 
	// WATCH OUT: if broadcast should also be received, this has to be configured at this point

	int hardwareReceiveFilter = 0;
	hardwareReceiveFilter |= (byte)(1<<0);
	befehl.NicCommand(Adapter, (short)(befehl.COMMAND_SET_RX_FILTER() | hardwareReceiveFilter));

	rxfilter = 0;
	rxfilter = befehl.NicReadPortByte(Adapter, Register.RX_FILTER_REGISTER());

	// for debugging purposes
	// Debug.out.println("STARTADAPTER: RxFilter nach SETZEN VON INDIVIDUAL -> : " + rxfilter);

	//Debug.out.println("StartAdapter: OUT");
	return true;
    }

    private boolean SoftwareWork() {

	SoftwareInformation2 softinfo2 = new SoftwareInformation2();
	int DmaControl = 0;
	short NetDiag = 0;
	short Contents = 0;

	// Debug.out.println("SoftwareWork: IN");
	
	// read the software information 2 register, which contains additional information for the driver - it has no use for 3C90x NICs
	try {
	    Contents = eeprom.ReadEEPROM(Adapter, eeprom.EEPROM_SOFTWARE_INFORMATION_2);
	}
	catch (NicStatusFailure e) {
	    Debug.out.println("SoftwareWork: Error reading Eeprom");
	    return false;
	}
	// check wether the Memory write invalidate PCI command bug has been fixed
	// if not we can´t use this command
	if (!((Contents & eeprom.ENABLE_MWI_WORK) > 0)) {
	    DmaControl = befehl.NicReadPortLong(Adapter, Register.DMA_CONTROL_REGISTER());
	    befehl.NicWritePortLong(Adapter, Register.DMA_CONTROL_REGISTER(), (int)(Register.DMA_CONTROL_DEFEAT_MWI() | DmaControl));
	}

	// check for a special revision (early Hurricane)

	befehl.NicCommand(Adapter, befehl.COMMAND_SELECT_REGISTER_WINDOW() | Register.REGISTER_WINDOW_4());

	NetDiag = befehl.NicReadPortShort(Adapter, Register.NETWORK_DIAGNOSTICS_REGISTER());
    
	if ((((NetDiag & Register.NETWORK_DIAGNOSTICS_ASIC_REVISION()) >> 4) == 1) &&
       	    (((NetDiag & Register.NETWORK_DIAGNOSTICS_ASIC_REVISION_LOW()) >> 1) < 4)) {
	    Adapter.Hardware.set_HurricaneEarlyRevision(true);
	    // Debug.out.println("Hurricane Early board");
	    HurricaneEarlyRevision();
	}

	// SoftwareInformation2 not valid for 3C90x NICs

	// check for bit [7]
	if ((Contents & 0x80) > 1) {
	    // Debug.out.println("Enable D3 work");
	    Adapter.Hardware.set_D3Work(true);
	}
	
	// Additional work#3
	// check for bit [3] and bit [6]
	Adapter.Hardware.set_DontSleep(false);
	if (!((Contents & 0x48) > 1)) {
	    // Debug.out.println("Don't sleep is TRUE");
	    Adapter.Hardware.set_DontSleep(true);
	}

	// Debug.out.println("SoftwareWork: OUT");
	return true;
    }

    private void ReStartAdapter() {
	
	int internalConfig = Adapter.keepForGlobalReset;
	int TimeOutCount;

 	//Debug.out.println("ReStartAdapter after global reset: IN");
	
	// Delay for 1 second
	TimeOutCount = timerManager.getCurrentTime() + 100;
	while(TimeOutCount > timerManager.getCurrentTime()) 
	    ;

	//
	// Mask all interrupts
	//

	befehl.NicMaskAllInterrupt(Adapter);

	//
	// Enable indication for all interrupts
	//

	befehl.NicEnableAllInterruptIndication(Adapter);	

	//
	// Write the internal config back.
	//

	befehl.NicCommand(Adapter, befehl.COMMAND_SELECT_REGISTER_WINDOW() | Register.REGISTER_WINDOW_3());
	befehl.NicWritePortLong(Adapter, Register.INTERNAL_CONFIG_REGISTER(), (int)internalConfig);
	
	//
	// Set the adapter for operation
	//
	
	try {
	    if (!GetAdapterProperties())
		Debug.out.println("ReStartAdapter: GetAdapterProperties failed");	
	}
	catch (NicStatusFailure e) {
	    Debug.out.println("ReStartAdapter: GetAdapterProperties failed");
	}
	try {
	    if (!BasicInitializeAdapter())
		Debug.out.println("ReStartAdapter: BasicInitialize failed");
	}
	catch (NicStatusFailure e) {
	    Debug.out.println("ReStartAdapter: BasicInitializeAdapter failed");
	}
	if (!SetupMedia())
	    Debug.out.println("ReStartAdapter: SetupMedia failed");
	
	if (!SoftwareWork())
	    Debug.out.println("ReStartAdapter: SoftwareWork failed");

	//
	// Countdown timer is cleared by reset - therefore we have to write it back
	//

	SetCountDownTimer();
	
	// this is needed to indicate that some delay is needed because of the switches 
	// at this point we don´t need to delay so we can set it to false
	Adapter.DelayStart = false;

	try {
	    if (!StartAdapter())
		Debug.out.println("ReStartAdapter: StartAdapter failed");
	}
	catch (NicStatusFailure e) {
	    Debug.out.println("ReStartAdapter: StartAdapter failed");
	}
	return;
    }

    public DeviceConfigurationTemplate[] getSupportedConfigurations () {
	return null;
    }

     public void open(DeviceConfiguration conf) {
	 NicOpen() ;
     }
     public void close() {
	 NicClose() ;
     }

    /**
     * main initialization method - has to be called before any data can be send or received
     * this method internally uses 6 other helper method in order to fullfill the configuration tasks
     * for an exact description see the documentation ("Studienarbeit Ausarbeitung")
     * also see NICClose() - to call when network driver is shut down
     */
     public boolean NicOpen() {
	/*
	befehl.NicCommandWait(Adapter, (short)( 
					       befehl.COMMAND_GLOBAL_RESET | befehl.GLOBAL_RESET_MASK_NETWORK_RESET | 
					       befehl.GLOBAL_RESET_MASK_TP_AUI_RESET | befehl.GLOBAL_RESET_MASK_ENDEC_RESET |
					       befehl.GLOBAL_RESET_MASK_AISM_RESET | befehl.GLOBAL_RESET_MASK_SMB_RESET |
					       befehl.GLOBAL_RESET_MASK_VCO_RESET | befehl.GLOBAL_RESET_MASK_UP_DOWN_RESET));

	*/
	try {
	    if (!GetAdapterProperties()) {
		Debug.out.println("NICOpen: GetAdapterProperties failed");
		FreeAdapterResources();
		return false;
	    }	
	}
	catch (NicStatusFailure e) {
	    Debug.out.println("NICOpen: GetAdapterProperties failed");
	    FreeAdapterResources();
	    return false;
	}
	try {
	    if (!BasicInitializeAdapter()) {
		Debug.out.println("NICOpen: BasicInitializeAdapter failed");
		FreeAdapterResources();
		return false;
	    }
	}
	catch (NicStatusFailure e) {
	    Debug.out.println("NICOpen: BasicInitializeAdapter failed");
	    FreeAdapterResources();
	    return false;
	}
	try {
	    if (!TestAdapter()) {
		Debug.out.println("NICOpen: TestAdapter failed");
		FreeAdapterResources();
		return false;
	    }
	}
	catch (NicStatusFailure e) {
	    Debug.out.println("NICOpen: TestAdapter failed");
	    FreeAdapterResources();
	    return false;
	}
	if (!SetupMedia()) {
	    Debug.out.println("NICOpen: SetupMedia failed");
	    FreeAdapterResources();
	    return false;
	}
	if (!SoftwareWork()) {
	    Debug.out.println("NICOpen: EnableSoftwareWork failed");
	    FreeAdapterResources();
	    return false;
	}
	try {
	    if (!StartAdapter()) {
		Debug.out.println("NICOpen: StartAdapter failed");
		FreeAdapterResources();
		return false;
	    }
	}
	catch (NicStatusFailure e) {
	    Debug.out.println("NICOpen: StartAdapter failed");
	    FreeAdapterResources();
	    return false;
	}

       	//
	// Initialize the NICTimer - doing periodically some maintenance stuff (e.g. reading statistics registers)
	//

	Adapter.Resources.set_TimerInterval(100);
	NicTimer nictimer = new NicTimer(irq, timerManager);
	NicTimerArg arg = new NicTimerArg(this, Adapter);
	timerManager.addMillisTimer(1000, nictimer, arg);

	//
	// Timer has been registered.
	//

	Adapter.ResourcesReserved |= NIC_TIMER_REGISTERED;

       	return	true;
    }

    private void FreeAdapterResources() {

	if ((Adapter.ResourcesReserved & NIC_INTERRUPT_REGISTERED) > 0) {
	    // Debug.out.println("Releasing interrupt");
	    // WATCH OUT: if the JX-system has some way to unregister an interrupt this should be placed here!!!
	    Adapter.ResourcesReserved &= ~NIC_INTERRUPT_REGISTERED;	    
	}

	if ((Adapter.ResourcesReserved & NIC_TIMER_REGISTERED) > 0) {
	    Debug.out.println("Releasing Timers");
	    if (!timerManager.deleteTimer(new NicTimer(irq, timerManager))) 		
		Debug.out.println("NicTimer already dequeued");
	    Adapter.ResourcesReserved &= ~NIC_TIMER_REGISTERED;
	    if (!timerManager.deleteTimer(new WaitTimer(ports, timerManager)))					
		Debug.out.println("WaitTimer already queued");
	}

    }

    /**
     * this method is used to shut down the network driver 
     * it waits for ongoing transmits to finish and stops the network card
     * see NICOpen() which is the corresponding method to start the network card
     */
     public boolean NicClose() {

	int TimeOutCount;

	// Debug.out.println("NICClose: IN");

	//
	// Disable transmit and receive.
	//

	befehl.NicCommand(Adapter, befehl.COMMAND_TX_DISABLE());
	befehl.NicCommand(Adapter, befehl.COMMAND_RX_DISABLE());

	//
	// Wait for the transmit in progress to go off.
	//

	befehl.NicCommand(Adapter, befehl.COMMAND_SELECT_REGISTER_WINDOW() | Register.REGISTER_WINDOW_4());
	waitglobal.set_MediaStatus(befehl.NicReadPortShort(Adapter, Register.MEDIA_STATUS_REGISTER()));
	ComInit.udelay(10);
	
	if ((waitglobal.get_MediaStatus() & Register.MEDIA_STATUS_TX_IN_PROGRESS()) > 0) {	
	    Adapter.WaitCases = WaitCases.CHECK_TRANSMIT_IN_PROGRESS;
	    // maximum time to wait is one second
	    TimeOutCount = timerManager.getCurrentTime() + 100; 
	    WaitTimerArg arg = new WaitTimerArg(Adapter, Adapter.TestDPD, TimeOutCount);
	    timerManager.addMillisTimer(1000, new WaitTimer(ports, timerManager), arg);
	    while(TimeOutCount > timerManager.getCurrentTime());
	    if(!((waitglobal.get_MediaStatus() & Register.MEDIA_STATUS_TX_IN_PROGRESS()) > 0)) 	 
		Debug.out.println("NICClose: Adapter is not responding");
	}

	befehl.NicCommandWait(Adapter, (short)befehl.COMMAND_TX_RESET());
	befehl.NicCommandWait(Adapter, (short)befehl.COMMAND_RX_RESET());

    	//
	// Mask and acknowledge all interrupts
	//

	befehl.NicMaskAllInterrupt(Adapter);
	befehl.NicAcknowledgeAllInterrupt(Adapter);
	
	CleanupSendLogic();

	//
	// Unregister the interrupt handler
	//

	if ((Adapter.ResourcesReserved & NIC_INTERRUPT_REGISTERED) > 0) {
	    // WATCH OUT: if the JX-system has some way to unregister an interrupt this should be placed here!!!
	    Debug.out.println("Releasing interrupt");
	    Adapter.ResourcesReserved &= ~NIC_INTERRUPT_REGISTERED;
	}

	//
	// Unregister the NICTimer  
	//

	if ((Adapter.ResourcesReserved & NIC_TIMER_REGISTERED) > 0) {
	    Debug.out.println("Releasing Timer");
	    if (!timerManager.deleteTimer(new NicTimer(irq, timerManager))) {
		Debug.out.println("NICClose: NicTimer already dequeued");
	    }
	    Adapter.ResourcesReserved &= ~NIC_TIMER_REGISTERED;
	}

	// Debug.out.println("NICClose: OUT");
	return true;
    }

    private void CleanupSendLogic() {

	DpdListEntry headDPD;
	// Debug.out.println("CleanupSendLogic: IN");
	
	//
	// Now clean up the DPD ring
	//
	
	headDPD = Adapter.HeadDPD;
	
	//
	// This is to take care of hardware raise condition
	//
	
	Adapter.TailDPD.FrameStartHeader(0);

	while (true) {
	    if (headDPD == Adapter.TailDPD)
		break;
	    
	    //
	    // Complete all the packets.
	    //

	    Adapter.BytesInDPDQueue -= headDPD.PacketLength();
	    headDPD.FrameStartHeader(0);
	    headDPD = headDPD.Next();
	}

	//
	// Update the head to point to this DPD now
	//

	Adapter.HeadDPD = headDPD;
	
	//
	// Initialize all DPDs
	//
	
	headDPD = Adapter.HeadDPD;

	while (true ) {
	    headDPD.DownNextPointer(0);
	    headDPD.FrameStartHeader(0);
	    headDPD = headDPD.Next();
	    if (headDPD == Adapter.HeadDPD)
		break;
	}

	// Debug.out.println("CleanupSendLogic: OUT");
    }

    public Memory transmit1(Memory buf, int offset, int count) {
	DpdListEntry dpdhelper;
	int packetLength;
	int flags;
	cpuManager.recordEvent(event_snd);
	Memory sendmem = buf;
	
	// get usable exchange buffer and append a buffer skeleton to the intransmit queue
	//Debug.out.println("Free buffer space: "+usableBufs.size());
	Buffer2 h = (Buffer2)usableBufs.nonblockingUndockFirstElement();
	if (h==null) {
	    if (debugSend) Debug.out.println("no usable buffers");
	    return buf;
	}
	Memory buf2 = h.getRawData();
	if (! buf2.isValid()) {
	    throw new Error();
	}


	if (debugPacketNotice) {
	    Debug.out.println("D3C905.transmit: offset="+offset+", count=" + count);
	    //Dump.xdump(buf, offset, count);
	}

	// first check for the size to send - if its larger than a maximum ethernet frame, an error occured as
	// the splitting of data should be done at a higher level
	
	if (count > AdapterLimits.ETHERNET_MAXIMUM_FRAME_SIZE) {
	    Debug.out.println("NICSendPacket: sendbuffer larger than maximum ethernet frame size! "+count+">"+AdapterLimits.ETHERNET_MAXIMUM_FRAME_SIZE);
	    count = AdapterLimits.ETHERNET_MAXIMUM_FRAME_SIZE;
	    throw new Error();
	}

	if (Adapter.TailDPD.Next() == Adapter.HeadDPD) {
	    Debug.out.println("ERROR: download ring full");
	    Adapter.DPDRingFull = true;
	    throw new Error();
	} 

	// Get the free DPD from the DPD ring.
	dpdhelper = Adapter.TailDPD;

	// Zero out the frame start header.
	dpdhelper.FrameStartHeader(0);

	// remember bucket
	dpdhelper.buffer = h;

	// set dpd
	//Debug.out.println("D3C905.transmit1 offset="+offset+", count="+count);
	dpdhelper.StoreMem(sendmem, offset, count);

    	dpdhelper.FrameStartHeader((dpdhelper.FrameStartHeader() | AdapterLimits.FSH_ROUND_UP_DEFEAT())); 
    	dpdhelper.FrameStartHeader((dpdhelper.FrameStartHeader() | AdapterLimits.FSH_ADD_IP_CHECKSUM())); 
    	dpdhelper.FrameStartHeader((dpdhelper.FrameStartHeader() | AdapterLimits.FSH_ADD_UDP_CHECKSUM())); 

	// indicate that this was the last dpd (and the only one)
	dpdhelper.DownNextPointer(0);
    	
	Adapter.BytesInDPDQueue += count;
    	Adapter.TailDPD.Previous().DownNextPointer(dpdhelper.DPDPhysicalAddress());
	Adapter.TailDPD = dpdhelper.Next();

	// remember memory
	h.setData(sendmem);

	// start count down
    	SetCountDownTimer();

	return buf2;
    }

    /**
     * the send method is the one to call to transmit data after the NIC was initialized by NICOpen()
     * there would be a possibility to use several send methods, one method for example for data smaller than one packet
     * and one method for a greater amount of data 
     *
     * @param buf the memory containing the data to be sent
     */
    public Memory transmit(Memory buf) {
	DpdListEntry dpdhelper;
	int packetLength;
	int flags;
	cpuManager.recordEvent(event_snd);
	/*
	int offset = buf.getOffset();
	int count = buf.size();
	Memory sendmem = buf.extendAndRevoke();
	*/
	Memory sendmem = buf;//buf.revoke();
	int offset = 0;
	int count = sendmem.size();
	
	// get usable exchange buffer and append a buffer skeleton to the intransmit queue
	//Debug.out.println("Free buffer space: "+usableBufs.size());
	Buffer2 h = (Buffer2)usableBufs.nonblockingUndockFirstElement();
	if (h==null) {
	    if (debugSend) Debug.out.println("no usable buffers");
	    return buf;
	}
	Memory buf2 = h.getRawData();
	if (! buf2.isValid()) {
	    throw new Error();
	}


	if (debugPacketNotice) {
	    Debug.out.println("D3C905.transmit: " + count);
	}

	if (debugSend) {
	    Debug.out.println("transmit: IN with bufferlength: " + count);
	    Dump.xdump1(sendmem, 0, 64);
	}

	// first check for the size to send - if its larger than a maximum ethernet frame, an error occured as
	// the splitting of data should be done at a higher level
	
	if (count > AdapterLimits.ETHERNET_MAXIMUM_FRAME_SIZE) {
	    Debug.out.println("NICSendPacket: sendbuffer larger than maximum ethernet frame size! "+count+">"+AdapterLimits.ETHERNET_MAXIMUM_FRAME_SIZE);
	    count = AdapterLimits.ETHERNET_MAXIMUM_FRAME_SIZE;
	    throw new Error();
	}

	// disable interrupts
	//int iflags = irq.clearIFlag();
	
	if (Adapter.TailDPD.Next() == Adapter.HeadDPD) {
	    Debug.out.println("ERROR: download ring full");
	    Adapter.DPDRingFull = true;
	    // set interrupt-flags to saved state
	    //irq.setFlags(iflags);
	    throw new Error();
	} 

	//
	// Get the free DPD from the DPD ring.
	//
	
	dpdhelper = Adapter.TailDPD;

	//
	// Zero out the frame start header.
	//

	dpdhelper.FrameStartHeader(0);

	// enter first sendbuffer - the corresponding fields get automatically set (the fields in the dpd-memory and packetlength...)
	//dpdhelper.StoreMem(sendmem, offset, count); FIXME!!
	//dpdhelper.StoreMem(sendmem); // storemem must be executed before h.setData because setdata invalidates mem

	dpdhelper.buffer = h;

	dpdhelper.StoreMem(sendmem);

    	dpdhelper.FrameStartHeader((dpdhelper.FrameStartHeader() | AdapterLimits.FSH_ROUND_UP_DEFEAT())); 
    	dpdhelper.FrameStartHeader((dpdhelper.FrameStartHeader() | AdapterLimits.FSH_ADD_IP_CHECKSUM())); 
    	dpdhelper.FrameStartHeader((dpdhelper.FrameStartHeader() | AdapterLimits.FSH_ADD_UDP_CHECKSUM())); 
	// indicate that this was the last dpd (and the only one)
	dpdhelper.DownNextPointer(0);
    	
	Adapter.BytesInDPDQueue += count;
    	Adapter.TailDPD.Previous().DownNextPointer(dpdhelper.DPDPhysicalAddress());
	Adapter.TailDPD = dpdhelper.Next();

	h.setData(sendmem);

    	SetCountDownTimer();

	return buf2;
    }


    private void SetCountDownTimer() {

	int countDownValue;
	// Debug.out.println("SetCountDownTimer: IN");

	countDownValue = Adapter.BytesInDPDQueue / 4; 

	if (countDownValue < 10)
	    countDownValue = 10;
	
	befehl.NicWritePortShort(Adapter, Register.COUNTDOWN_REGISTER(), (short)countDownValue);
	
	//Debug.out.println("SetCountDownTimer: OUT");
    }

    /* for the HurricaneEarlyRevision we read the MII Auxiliary Control/Status and set MII_PHY_REGISTER_24_PVCIRC - the process variation bit */
    private void HurricaneEarlyRevision() {

	short PhyRegisterValue = 0;

	try {
	    PhyRegisterValue =  mii.ReadMIIPhy(Adapter, mii.MII_PHY_REGISTER_24);
	}
	catch (ReadMIIException e) {
	    Debug.out.println("CaneRev-ReadMIIPhy: Phy not responding");
	}
	PhyRegisterValue |= mii.MII_PHY_REGISTER_24_PVCIRC;
	mii.WriteMIIPhy(Adapter, mii.MII_PHY_REGISTER_24, PhyRegisterValue);
    }

    /* advertise flowcontrol - we set the corresponding bit and restart the autonegotiation process */
    private void FlowControl() {

	short PhyAnar = 0;
	short PhyControl = 0;
	
	try {
	    PhyAnar = mii.ReadMIIPhy(Adapter, mii.MII_PHY_ANAR);
	}
	catch (ReadMIIException e) {
	}
	// set the pause operation bit - see p.174
	PhyAnar |= mii.MII_ANAR_FLOWCONTROL;
	mii.WriteMIIPhy(Adapter, mii.MII_PHY_ANAR, PhyAnar);

	try {
	    PhyControl = mii.ReadMIIPhy(Adapter, mii.MII_PHY_CONTROL);
	}
	catch (ReadMIIException e) {
	}
	// now we restart the autonegotiation - see p.181
	// this bit resets to 0 after some cycles so it´s always 0 when read
	PhyControl |= mii.MII_CONTROL_START_AUTO;
	mii.WriteMIIPhy(Adapter, mii.MII_PHY_CONTROL, PhyControl);
    }

    /* This routine checks whether autoselection is specified.  If it is set, it calls MainAutoSelectionRoutine else for non-autoselect
       case, calls ProgramMII */
    private boolean SetupMedia() {

	short OptionAvailable = 0;
	int InternalConfig = 0;
	short InternalConfig0 = 0;
	short InternalConfig1 = 0;
	short MacControl = 0;
	ConnectorType NotUsed = new ConnectorType();
	boolean Handles100Mbit = false;

	// Debug.out.println("SetupMedia: IN");
	
	Adapter.Hardware.set_AutoSelect(false);

	try {
	    Adapter.Hardware.set_ConfigConnector(ConnectorType.CONNECTOR_UNKNOWN);
	    Adapter.Hardware.set_Connector(ConnectorType.CONNECTOR_UNKNOWN);
	}
	catch (UnknownConnectorType e) {
	    Debug.out.println("SetupMedia: tried to set an invalid connector");
	}

	//
	// Assumption made here for Cyclone, Hurricane, and Tornado
	// adapters have the same fixed PHY address.  For other PHY
	// address values, this needs to be changed.
	//

	Adapter.Hardware.set_phys(mii.MII_PHY_ADDRESS);
	Adapter.Hardware.set_MIIReadCommand((short)(mii.MII_PHY_ADDRESS | 0x6000));
	Adapter.Hardware.set_MIIWriteCommand((short)(mii.MII_PHY_ADDRESS | 0x5002)); 

    	// If this is a 10mb Lightning card, assume that the 10FL bit is
    	// set in the media options register
    	//

	if (Adapter.Hardware.get_DeviceId() == NIC_PCI_DEVICE_ID_900A) {	    
	    Debug.out.println("SetupMedia: 10BaseFL force Media Option ");
	    OptionAvailable = Register.MEDIA_OPTIONS_10BASEFL_AVAILABLE();
	}
	else {

	    //
	    // Read the MEDIA OPTIONS to see what connectors are available - see technical reference p.199
	    // this registers tells which physical connections area available on the NIC
	    //

	    befehl.NicCommand(Adapter, befehl.COMMAND_SELECT_REGISTER_WINDOW() | Register.REGISTER_WINDOW_3());
	    OptionAvailable = befehl.NicReadPortShort(Adapter, Register.MEDIA_OPTIONS_REGISTER());
	}

 	//
	// Read the internal config through EEPROM since reset invalidates the normal register value.
    	// the InternalConfig0 contains hardware properties of the adapter - see technical reference p.59
	//

	try {
	    InternalConfig0 = eeprom.ReadEEPROM(Adapter, eeprom.EEPROM_INTERNAL_CONFIG_WORD_0);
	}
	catch (NicStatusFailure e) {
	    Debug.out.println("SetupMedia: InternalConfig 0 read failed");
	    return false;
    	}

	// the InternalConfig 1 contains information that may be changed by the driver to tune the NIC to the system configuration
	try {
	    InternalConfig1 = eeprom.ReadEEPROM(Adapter, eeprom.EEPROM_INTERNAL_CONFIG_WORD_1);
	}
	catch (NicStatusFailure e) {
	    Debug.out.println("SetupMedia: InternalConfig 1 read failed");        
	    return false;
    	}

	// now construct the complete InternalConfig
	InternalConfig = InternalConfig0 | (InternalConfig1 <<16);

	befehl.NicCommand(Adapter, befehl.COMMAND_SELECT_REGISTER_WINDOW() | Register.REGISTER_WINDOW_3());

	//
	// Read the current value of the InternalConfig register. If it's different
	// from the EEPROM values, than write it out using the EEPROM values.
	// This is done since a global reset may invalidate the register value on
	// some ASICs. Also, writing to InternalConfig may reset the PHY on some ASICs
	//

	if (InternalConfig != befehl.NicReadPortLong(Adapter, Register.INTERNAL_CONFIG_REGISTER())) {
	    befehl.NicWritePortLong(Adapter, Register.INTERNAL_CONFIG_REGISTER(), InternalConfig);
	}

 	//
	// Get the connector to use.
	// ConfigConnector tells which Connector is set by the user, Connector is the result 
	//

	if (Adapter.Hardware.get_ConfigConnector() == ConnectorType.CONNECTOR_UNKNOWN) {
	    try {
		// get bits [23:20], the xcvrSelect field which indicates the selected transceiver - see technical reference p.62
		Adapter.Hardware.set_ConfigConnector((byte)((InternalConfig & Register.INTERNAL_CONFIG_TRANSCEIVER_MASK()) >> 20));
		Adapter.Hardware.set_Connector(Adapter.Hardware.get_ConfigConnector());
	    }
	    catch (UnknownConnectorType e) {
		Debug.out.println("SetupMedia: tried to set a wrong Connector!");
		return false;
	    }

	    // check for the autoselect bit [24] in InternalConfig
	    // this bit indicates when set that the driver should ignore the value set in xcvrSelect and instead autoselect the media port
	    // at load time through executing a autoselect sequence 

	    if ((InternalConfig & Register.INTERNAL_CONFIG_AUTO_SELECT()) > 0)
		Adapter.Hardware.set_AutoSelect(true);
	    // set the values Hardware depending on which connector to use or set autoselection
	    ProcessMediaOverrides(OptionAvailable);
    	}
	
	//
	// If auto selection of connector was specified, do it now...
	//
	
	if (Adapter.Hardware.get_AutoSelect()) {
	    Debug.out.println("SetupMedia: Autoselect set");	
	    befehl.NicCommand(Adapter, befehl.COMMAND_STATISTICS_DISABLE());
	    // this method tries the several available links 
	    MainAutoSelectionRoutine(OptionAvailable);
	}
	// otherwise if no autoselection is selected, we have to set the "forced" transceiver and programm the MII
    	else {
	
	    //
	    // MII connector needs to be initialized and the data rates
	    // set up even in the non-autoselect case
	    //
	    
	    Debug.out.println("SetupMedia: Adapter in forced-mode configuration!");
		
	    if ((Adapter.Hardware.get_Connector() == ConnectorType.CONNECTOR_MII) ||
		(Adapter.Hardware.get_Connector() == ConnectorType.CONNECTOR_AUTONEGOTIATION)) {
		try  {
		    mii.ProgramMII(Adapter, new ConnectorType(ConnectorType.CONNECTOR_MII));
		}
		catch (UndefinedConnectorException e) {
		    Debug.out.println("SetupMedia: wrong ConnectorType");
		}
	    }
	    else {			
		if ((Adapter.Hardware.get_Connector() == ConnectorType.CONNECTOR_100BASEFX) ||
		    (Adapter.Hardware.get_Connector() == ConnectorType.CONNECTOR_100BASETX)) {		    
		    Adapter.Hardware.set_LinkSpeed(Softlimits.LINK_SPEED_100);			
		}
		else {			
		    Adapter.Hardware.set_LinkSpeed(Softlimits.LINK_SPEED_10);			
		}
	    }	
	    try {
		SetupConnector(new ConnectorType(Adapter.Hardware.get_Connector()));
	    }
	    catch (UndefinedConnectorException e) {
		Debug.out.println("SetupMedia: unknown Connector in SetupConnector");
	    }
	}

	//
	// Check link speed and duplex settings before doing anything else.
	// If the call succeeds, we know the link is up, so we'll update the
	// link state
	//

	try {
	    Handles100Mbit = GetLinkSpeed();
	}
	catch (LinkSpeedException e) {
	    Debug.out.println("SetupMedia: Unable to determine link speed!");
	    try {
		Adapter.Hardware.set_LinkState(NicHardwareInformation.LINK_DOWN_AT_INIT);
	    }
	    catch (UnknownLinkState f) {
		Debug.out.println("SetupMedia: tried to set an unknown LinkState");
	    }
	}
	if (Handles100Mbit) {
	    Adapter.Hardware.set_LinkSpeed(Softlimits.LINK_SPEED_100);
	}
	else {
	    Adapter.Hardware.set_LinkSpeed(Softlimits.LINK_SPEED_10);
	}
	try {
	    Adapter.Hardware.set_LinkState(NicHardwareInformation.LINK_UP);
	}
	catch (UnknownLinkState e) {
	    Debug.out.println("SetupMedia: tried to set an unknown LinkState");
	} 
	
	//      
	// Set up duplex mode
	//

	befehl.NicCommand(Adapter, befehl.COMMAND_SELECT_REGISTER_WINDOW() | Register.REGISTER_WINDOW_3());

	MacControl = befehl.NicReadPortShort(Adapter, Register.MAC_CONTROL_REGISTER());
    	
	// if Full Duplex is selected we have to activate in the MacControl Register - see technical reference p.197
	if (Adapter.Hardware.get_FullDuplexEnable()) {

	    //
	    // Set Full duplex in MacControl register
	    //							

	    MacControl |= Register.MAC_CONTROL_FULL_DUPLEX_ENABLE();                      
	    Debug.out.println("Changed link to full duplex");

	    //
	    // Since we're switching to full duplex, enable flow control
	    //

	    if (Adapter.Hardware.get_FlowControlSupported()) {
		Debug.out.println("SetupMedia: flow Control support is on!");
		MacControl |=  Register.MAC_CONTROL_FLOW_CONTROL_ENABLE();	
		Adapter.Hardware.set_FlowControlEnable(true);
		SetMulticastAddresses();
	    }
	}
	else {

	    //
	    // Set Half duplex in MacControl register
	    //

	    MacControl &= ~(Register.MAC_CONTROL_FULL_DUPLEX_ENABLE());                            
	    Debug.out.println("Changed link to half duplex");		

	    //
	    // Since we're switching to half duplex, disable flow control
	    //                                    

	    if (Adapter.Hardware.get_FlowControlSupported()) {		
		MacControl &= ~(Register.MAC_CONTROL_FLOW_CONTROL_ENABLE());
		Adapter.Hardware.set_FlowControlEnable(false);                   
		SetMulticastAddresses();	
	    }         
	}
	// now write the Medium Access Control register - after changing the full duplex bit we also have to issue a TxReset and RxReset
	befehl.NicWritePortShort(Adapter, Register.MAC_CONTROL_REGISTER(), MacControl);
	
	if (!ResetAndEnableTransmitter()) { 
	    Debug.out.println("SetupMedia: Reset transmitter failed");
	    return false;
	}
	
	if (!ResetAndEnableReceiver()) {
	    Debug.out.println("SetupMedia: Reset receiver failed");
	    return false;
	}

    	//
    	// This is for advertisement of flow control.  We only need to
    	// call this if the adapter is using flow control, in Autoselect
    	// mode and not a Tornado board
    	//

	if ((Adapter.Hardware.get_AutoSelect() && Adapter.Hardware.get_FlowControlEnable()) &&
	    (Adapter.Hardware.get_DeviceId() != NIC_PCI_DEVICE_ID_9200) && 
	    (Adapter.Hardware.get_DeviceId() != NIC_PCI_DEVICE_ID_9805)) {		
	    FlowControl();
	}
    	// Debug.out.println("SetupMedia: Out");
    	return true;
    }
   
    /** Adjust linkspeed and duplex in tick handler */
    public void TickMediaHandler() {

	if ((Adapter.Hardware.get_Connector() == ConnectorType.CONNECTOR_AUTONEGOTIATION) ||
	    (Adapter.Hardware.get_Connector() == ConnectorType.CONNECTOR_10BASET) ||
	    (Adapter.Hardware.get_Connector() == ConnectorType.CONNECTOR_100BASETX)) {
	    
	    //
	    // TP case
	    //
	    
	    CheckTPLinkState();
	}
	else if (Adapter.Hardware.get_Connector() == ConnectorType.CONNECTOR_100BASEFX) {
	    
	    //
	    // FX case
	    //
	    
	    CheckFXLinkState();
 	}
    }



    /* Determine whether to notify the operating system if link is lost or restored. For autonegotiation case, it makes adjustments to duplex
       and speed if necessary */
    private void CheckTPLinkState() {

	boolean handles100Mbit = false;
	short PhyStatus = 0;
	boolean OldFullDuplex;
	int OldLinkSpeed;

	try {
	    PhyStatus = mii.ReadMIIPhy(Adapter, mii.MII_PHY_STATUS);
	}
	catch (ReadMIIException e) {
	    Debug.out.println("CheckTPLinkState: Couldn´t access MII");
	    return;
	}
	
	if (!((PhyStatus & mii.MII_STATUS_LINK_UP) > 0)) {					
	    
	    //
	    // If OS doesn't know link was lost, go       
	    // ahead and notify (currently the notify-method does not a lot as it is not supported by the OS)
	    //
	    
	    if (Adapter.Hardware.get_LinkState() == NicHardwareInformation.LINK_UP) 
		IndicateToOSLinkStateChange();
	}
	else {  	

	    //
	    // If OS doesn't know link was restored, go
	    // ahead and notify (same applies as in commentary above)
	    //

	    if (Adapter.Hardware.get_LinkState() != NicHardwareInformation.LINK_UP)
		IndicateToOSLinkStateChange();

	    if ((((PhyStatus & mii.MII_STATUS_AUTO) > 0) && 
		 ((PhyStatus & mii.MII_STATUS_EXTENDED) > 0)) &&
		(Adapter.Hardware.get_Connector() == ConnectorType.CONNECTOR_AUTONEGOTIATION)) {

		OldLinkSpeed = Adapter.Hardware.get_LinkSpeed();
		OldFullDuplex = Adapter.Hardware.get_FullDuplexEnable();
		
		//
		// Capable of autonegotiation 
		//
		
		try {
		    handles100Mbit = GetLinkSpeed();
		}
		catch (LinkSpeedException e) {
		    Debug.out.println("CheckTPLinkState: Unable to determine link speed!");
		    return;
		}

		if (handles100Mbit)
		    Adapter.Hardware.set_LinkSpeed(Softlimits.LINK_SPEED_100);
		else
		    Adapter.Hardware.set_LinkSpeed(Softlimits.LINK_SPEED_10);
		
		//
		// Set up for new speed if speed changed and set counterSpeed bit in DmaCtrl register
		//
		
		if (Adapter.Hardware.get_LinkSpeed() != OldLinkSpeed)
		    SetupNewSpeed();
		
		//
		// Set up for new duplex mode if duplex changed
		//
		
		if (Adapter.Hardware.get_FullDuplexEnable() != OldFullDuplex)
		    SetupNewDuplex();						
	    } 
	}
    }		                                     
    

    private void CheckFXLinkState() {

	short MediaStatus = 0;

	befehl.NicCommand(Adapter, befehl.COMMAND_SELECT_REGISTER_WINDOW() | Register.REGISTER_WINDOW_4());

	MediaStatus = befehl.NicReadPortShort(Adapter, Register.MEDIA_STATUS_REGISTER());

	if (!((MediaStatus & Register.MEDIA_STATUS_LINK_DETECT()) > 0)) {
	    if (Adapter.Hardware.get_LinkState() == NicHardwareInformation.LINK_UP)
		IndicateToOSLinkStateChange();
	}
	else {
	    if (Adapter.Hardware.get_LinkState() != NicHardwareInformation.LINK_UP)
		IndicateToOSLinkStateChange();
	}
    }


    /* with this mehtod there is a possibility to notify to the OS the change in link state
       currently there are just some variables set and a message is printed - maybe a OS version in the future
       has support to use this information so it has just to be added here */
    private void IndicateToOSLinkStateChange() {

	if (Adapter.Hardware.get_LinkState() != NicHardwareInformation.LINK_DOWN) { 
	    Debug.out.println("Link Lost...");
	    try {
		Adapter.Hardware.set_LinkState(NicHardwareInformation.LINK_DOWN);
	    }
	    catch (UnknownLinkState e) {
		Debug.out.println("IndicateToOSLinkStateChange: tried to set a unknown LinkState");
		return;
	    }
	}
    	else {					
	    Debug.out.println("Link Regained...");
	    try {
		Adapter.Hardware.set_LinkState(NicHardwareInformation.LINK_UP);
	    }
	    catch (UnknownLinkState e) {
		Debug.out.println("InidcateToOSLinkStateChange: tried to set an unknown link state!");
		return;
	    }
	}					
    }


    /* Determine from the MII AutoNegotiationAdvertisement and AutoNegotiationPartnerAbility registers whether the 
       current linkspeed is 10Mbits or 100Mbits */
    private boolean GetLinkSpeed() throws LinkSpeedException {

  	boolean PhyResponding;
	short PhyAnlpar;
    	short PhyAner;
    	short PhyAnar;
	short PhyStatus;
	boolean handles100Mbit;
      
	try {
	    PhyAner = mii.ReadMIIPhy(Adapter, mii.MII_PHY_ANER);
	}
	catch (ReadMIIException e) {
	    throw new LinkSpeedException();
	}  
	try {
	    PhyAnlpar = mii.ReadMIIPhy(Adapter, mii.MII_PHY_ANLPAR);
	}
	catch (ReadMIIException e) {
	    throw new LinkSpeedException();
	}  
	try {
	    PhyAnar = mii.ReadMIIPhy(Adapter, mii.MII_PHY_ANAR);
	}
    	catch (ReadMIIException e) {
	    throw new LinkSpeedException();
	}  
	try {
	    PhyStatus = mii.ReadMIIPhy(Adapter, mii.MII_PHY_STATUS);
	}
	catch (ReadMIIException e) {
	    throw new LinkSpeedException();
	}   
	
	//
	// Check to see if we've completed auto-negotiation
	//
	
	if (!((PhyStatus & mii.MII_STATUS_AUTO_DONE) >0))
	    throw new LinkSpeedException();
	
	if (((PhyAnar & mii.MII_ANAR_100TXFD) > 0) && ((PhyAnlpar & mii.MII_ANLPAR_100TXFD) > 0)) {
	    Adapter.Hardware.set_MIIPhyUsed(mii.MII_100TXFD);
	    handles100Mbit = true;
	    Adapter.Hardware.set_FullDuplexEnable(true);
	  
	} 
	else if (((PhyAnar & mii.MII_ANAR_100TX) > 0) && ((PhyAnlpar & mii.MII_ANLPAR_100TX) > 0)) {
	    Adapter.Hardware.set_MIIPhyUsed(mii.MII_100TX);
	    handles100Mbit = true;
	    Adapter.Hardware.set_FullDuplexEnable(false);
	} 
	else if (((PhyAnar & mii.MII_ANAR_10TFD) > 0) && ((PhyAnlpar & mii.MII_ANLPAR_10TFD) > 0)) {
	    Adapter.Hardware.set_MIIPhyUsed(mii.MII_10TFD);
	    Adapter.Hardware.set_FullDuplexEnable(true);
	    handles100Mbit = true;
	} 
   	else if (((PhyAnar & mii.MII_ANAR_10T) > 0) && ((PhyAnlpar & mii.MII_ANLPAR_10T) > 0)) {
	    Adapter.Hardware.set_MIIPhyUsed(mii.MII_10T);
	    Adapter.Hardware.set_FullDuplexEnable(false);
	    handles100Mbit = false;	
	}
	else if (!((PhyAner & mii.MII_ANER_LPANABLE) > 0)) {

	    //
	    // Link partner is not capable of auto-negotiation so we have to fall back to 10HD
	    //

	    Adapter.Hardware.set_MIIPhyUsed(mii.MII_10T);
	    Adapter.Hardware.set_FullDuplexEnable(false);
	    handles100Mbit = false;	
	}
	else 
	    throw new LinkSpeedException();
	
	return handles100Mbit;
    }


    /* Change the connector and duplex - this method offers the possibility to override predefined or set values and 
       may even be used to set values that it becomes from the command line at booting time (see kernel parameters in Linux for example) */
    private void ProcessMediaOverrides(short OptionAvailable) {

        int InternalConfig = 0;
	int OldInternalConfig = 0;
				
	befehl.NicCommand(Adapter, befehl.COMMAND_SELECT_REGISTER_WINDOW() | Register.REGISTER_WINDOW_3());

	// get the value of the InternalConfig register - see technical reference p.59
	InternalConfig = befehl.NicReadPortLong(Adapter, Register.INTERNAL_CONFIG_REGISTER());

	OldInternalConfig = InternalConfig;

	// if the MediaOverride tells us to autoselect, we switch on bit [24] in the InternalConfig - otherwise switch it off 
	if (Adapter.Hardware.get_MediaOverride() == Mii.MEDIA_AUTO_SELECT)
	    InternalConfig |= Register.INTERNAL_CONFIG_AUTO_SELECT();
	else 
	    InternalConfig &= ~(Register.INTERNAL_CONFIG_AUTO_SELECT());
	//
	// Write to the InternalConfig register only if it's changed
	//

	if (OldInternalConfig != InternalConfig) {
	    befehl.NicWritePortLong(Adapter, Register.INTERNAL_CONFIG_REGISTER(), InternalConfig);
	}

	// now set the corresponding values - if autoselection is specified, just set it Hardware, otherwise switch off autoselection and
	// set the corresponding Mediatype, depending on the value of the Media Options register (OptionAvailabe)
	try {
	    switch (Adapter.Hardware.get_MediaOverride()) {
		
	    case Mii.MEDIA_AUTO_SELECT:
		Adapter.Hardware.set_AutoSelect(true);
		break;
		
	    case Mii.MEDIA_10BASE_TX:			
		if ((OptionAvailable & Register.MEDIA_OPTIONS_10BASET_AVAILABLE()) > 0) {			    
		    Adapter.Hardware.set_Connector(ConnectorType.CONNECTOR_10BASET);
		    Adapter.Hardware.set_AutoSelect(false);
		}
		break;
		
	    case Mii.MEDIA_10AUI:			
		if ((OptionAvailable & Register.MEDIA_OPTIONS_10AUI_AVAILABLE()) > 0) {
		    Adapter.Hardware.set_Connector(ConnectorType.CONNECTOR_10AUI);
		    Adapter.Hardware.set_AutoSelect(false);
		}
		break;
		
	    case Mii.MEDIA_10BASE_2:			
		if ((OptionAvailable & Register.MEDIA_OPTIONS_10BASE2_AVAILABLE()) > 0) {	
		    Adapter.Hardware.set_Connector(ConnectorType.CONNECTOR_10BASE2);
		    Adapter.Hardware.set_AutoSelect(false);
		}
		break;
		
	    case Mii.MEDIA_100BASE_TX:
		if ((OptionAvailable & Register.MEDIA_OPTIONS_100BASETX_AVAILABLE()) > 0) {
		    Adapter.Hardware.set_Connector(ConnectorType.CONNECTOR_100BASETX);
		    Adapter.Hardware.set_AutoSelect(false);
		}
		break;
	    
	    case Mii.MEDIA_100BASE_FX:
		if ((OptionAvailable & Register.MEDIA_OPTIONS_100BASEFX_AVAILABLE()) > 0) {
		    Adapter.Hardware.set_Connector(ConnectorType.CONNECTOR_100BASEFX);
		    Adapter.Hardware.set_AutoSelect(true);
		}
		break;
	    
	    case Mii.MEDIA_10BASE_FL:
		if ((OptionAvailable & Register.MEDIA_OPTIONS_10BASEFL_AVAILABLE()) > 0) {
		    Adapter.Hardware.set_Connector(ConnectorType.CONNECTOR_10AUI);
		    Adapter.Hardware.set_AutoSelect(true);
		}
		break;

	    case Mii.MEDIA_NONE:			
		break;
	    }
	}
	catch (UnknownConnectorType e) {
	    Debug.out.println("ProcessMediaOverrides: tried to set an unknown connector");
	}
    }

    /* Setup new duplex in MacControl register */
    private boolean SetupNewDuplex() {

	short MacControl = 0;
	int TimeOutCount;
	WaitTimer waittimer = new WaitTimer(ports, timerManager);

	// for debugging purposes
	//Debug.out.println("SetupNewDuplex: IN");

	befehl.NicCommand(Adapter, befehl.COMMAND_RX_DISABLE());
	befehl.NicCommand(Adapter, befehl.COMMAND_TX_DISABLE());
    
	befehl.NicCommand(Adapter, befehl.COMMAND_SELECT_REGISTER_WINDOW() | Register.REGISTER_WINDOW_4());
	
	//
	// Wait for transmit to go quiet
	//
	
	waitglobal.set_MediaStatus((short)0);
	waitglobal.set_MediaStatus(befehl.NicReadPortShort(Adapter, Register.MEDIA_STATUS_REGISTER()));
	ComInit.udelay(10);

	if((waitglobal.get_MediaStatus() & Register.MEDIA_STATUS_TX_IN_PROGRESS()) > 0) {	
	    Adapter.WaitCases = WaitCases.CHECK_TRANSMIT_IN_PROGRESS;
	    TimeOutCount = timerManager.getCurrentTime() + 100;
	    WaitTimerArg arg = new WaitTimerArg(Adapter, Adapter.TestDPD, TimeOutCount);
	    timerManager.addMillisTimer(1000, waittimer, arg);
	    while(TimeOutCount > timerManager.getCurrentTime());
	    if(!((waitglobal.get_MediaStatus() & Register.MEDIA_STATUS_TX_IN_PROGRESS()) > 0)) {
		Debug.out.println("SetupNewDuplex: Packet was not picked up by hardware");
		return false;
	    }
	} 

	//
	// Wait for receive to go quiet
	//

	waitglobal.set_MediaStatus(befehl.NicReadPortShort(Adapter, Register.MEDIA_STATUS_REGISTER()));
	ComInit.udelay(10);
	
	if((waitglobal.get_MediaStatus() &  Register.MEDIA_STATUS_CARRIER_SENSE()) > 0) {
	    Adapter.WaitCases = WaitCases.CHECK_CARRIER_SENSE;
	    TimeOutCount = timerManager.getCurrentTime() + 100;
	    WaitTimerArg arg = new WaitTimerArg(Adapter, Adapter.TestDPD, TimeOutCount);
	    timerManager.addMillisTimer(10, waittimer, arg);
	    while(TimeOutCount > timerManager.getCurrentTime());
	    if(!((waitglobal.get_MediaStatus() &  Register.MEDIA_STATUS_CARRIER_SENSE()) > 0)) {
		Adapter.Hardware.set_Status(NicHardwareInformation.HARDWARE_STATUS_HUNG);
		Debug.out.println("SetupNewDuplex: Packet was not uploaded by hardware");
		return false;
	    }
	} 

	befehl.NicCommand(Adapter, befehl.COMMAND_SELECT_REGISTER_WINDOW() | Register.REGISTER_WINDOW_3());

	MacControl = befehl.NicReadPortShort(Adapter, Register.MAC_CONTROL_REGISTER());
				
	if (Adapter.Hardware.get_FullDuplexEnable()) {
	    
	    //
	    // Set Full duplex in MacControl register
	    //							
	    
	    MacControl |= Register.MAC_CONTROL_FULL_DUPLEX_ENABLE();                      
	    Debug.out.println("Changed link to full duplex");			
	    
	    //
	    // Since we're switching to full duplex, enable flow control
	    //                                                                      			      
	    
	    if (Adapter.Hardware.get_FlowControlSupported()) {                                                          
		MacControl |=  Register.MAC_CONTROL_FLOW_CONTROL_ENABLE();
		Adapter.Hardware.set_FlowControlEnable(true);
		SetMulticastAddresses();
	    }
	}
    	else {
	    
	    //
	    // Set Half duplex in MacControl register
	    //

	    MacControl &= ~(Register.MAC_CONTROL_FULL_DUPLEX_ENABLE());                            
	    Debug.out.println("Changed link to half duplex");
	    
	    //
	    // Since we're switching to half duplex, disable flow control
	    //                                    

	    if (Adapter.Hardware.get_FlowControlEnable() && Adapter.Hardware.get_FlowControlSupported()) {
		MacControl &= ~(Register.MAC_CONTROL_FLOW_CONTROL_ENABLE());
		Adapter.Hardware.set_FlowControlEnable(false);                   
		SetMulticastAddresses();	
	    }         
	}
	
	befehl.NicWritePortShort(Adapter, Register.MAC_CONTROL_REGISTER(), MacControl);
	ComInit.udelay(20);

	befehl.NicCommand(Adapter, befehl.COMMAND_RX_ENABLE());
	befehl.NicCommand(Adapter, befehl.COMMAND_TX_ENABLE());
	
	// Debug.out.println("SetupNewDuplex: OUT");
	
	return true;
    }

    /* Sets the counter speed in the DMA control register - Clear bit for 10Mbps or set the bit for 100Mbps */
    private void SetupNewSpeed() {

	int DmaControl = 0;
		
	// for debugging purposes
	// Debug.out.println("SetupNewSpeed: IN");

	DmaControl = befehl.NicReadPortLong(Adapter, Register.DMA_CONTROL_REGISTER());
	if (Adapter.Hardware.get_LinkSpeed() == Softlimits.LINK_SPEED_100) {                                    
	    DmaControl |= Register.DMA_CONTROL_COUNTER_SPEED();
	}
	else {
	    DmaControl &= ~(Register.DMA_CONTROL_COUNTER_SPEED());
	}
	befehl.NicWritePortLong(Adapter, Register.DMA_CONTROL_REGISTER(), DmaControl);
	  
	//Debug.out.println("SetupNewSpeed: OUT");
    }

    /**
     * This routine places nic into promiscous receive mode.
     */
    public void setPromiscousMode() {
	int hardwareReceiveFilter = 0;
	hardwareReceiveFilter |= (byte)(1<<0);
	hardwareReceiveFilter |= befehl.RX_FILTER_PROMISCUOUS();
	befehl.NicCommand(Adapter, (short)(befehl.COMMAND_SET_RX_FILTER() | hardwareReceiveFilter)); 
    }

    /**
     * This routine places nic into normal receive mode (individual and broadcast packets).
     */
    public void setNormalReceiveMode() {
	int hardwareReceiveFilter = 0;
	hardwareReceiveFilter |= (byte)(1<<0);
	hardwareReceiveFilter |= befehl.RX_FILTER_INDIVIDUAL();
	hardwareReceiveFilter |= befehl.RX_FILTER_BROADCAST();
	befehl.NicCommand(Adapter, (short)(befehl.COMMAND_SET_RX_FILTER() | hardwareReceiveFilter)); 
    }

    public void setReceiveMode(int mode) {
	switch(mode) {
	case NetworkDevice.RECEIVE_MODE_INDIVIDUAL:
	    NICSetReceiveMode(null);
	    break;
	case NetworkDevice.RECEIVE_MODE_PROMISCOUS:
	    NICSetReceiveMode(null);
	    break;
	case NetworkDevice.RECEIVE_MODE_MULTICAST:
	    NICSetReceiveMode(null);
	    break;
	default:
	    throw new Error("Wrong receive mode");
	}
	unmaskInterrupts();

    }

    /**
     * This routine sets receive mode (for example broadcast, multicast, ...)
     * @param multivec a Vector of MultiCast objects each describing a multicast address to be received
     */
     public void NICSetReceiveMode(Vector multivec) {

	int count;
	int hardwareReceiveFilter = 0;
	byte[] flowControlAddress = new byte[AdapterLimits.ETHERNET_ADDRESS_SIZE];
	byte[] broadcastAddress = new byte[AdapterLimits.ETHERNET_ADDRESS_SIZE];
	byte[] address = new byte[AdapterLimits.ETHERNET_ADDRESS_SIZE];
	int bitsInHashFilter;
	int index;

	// get first object - this must at least exist
	MultiCast multic=null;
	if(multivec!=null) multic= (MultiCast)multivec.elementAt(0);

	// for debugging purposes
	// Debug.out.println("NICSetReceiveMode: In");

	bitsInHashFilter = Adapter.Hardware.get_BitsInHashFilter();

	//
	// Check if promiscuous mode to be enabled
	//

	if ((multic!=null) && (multic.flags() & IFF_PROMISC) > 0) {

	    Debug.out.println("IFF_PROMISC mode ");
	    hardwareReceiveFilter |= befehl.RX_FILTER_PROMISCUOUS();
	}
	else if ((multic!=null) && (multic.flags() & IFF_ALLMULTI) > 0) {

	    //
	    // Check if ALL_MULTI mode to be enabled
	    //

	    Debug.out.println("IFF_ALLMULTI");
	    hardwareReceiveFilter |= befehl.RX_FILTER_INDIVIDUAL();
	    hardwareReceiveFilter |= befehl.RX_FILTER_BROADCAST();
	    hardwareReceiveFilter |= befehl.RX_FILTER_ALL_MULTICAST();
	}
	else if ((multic!=null) && (multic.flags() & IFF_MULTICAST) > 0) {
	   
	    //
	    // Check if hash multicast to be enabled
	    //
	    
	    Debug.out.println("IFF_MULTICAST");
	    hardwareReceiveFilter |= befehl.RX_FILTER_INDIVIDUAL();
	    hardwareReceiveFilter |= befehl.RX_FILTER_BROADCAST();
	    hardwareReceiveFilter |= befehl.RX_FILTER_MULTICAST_HASH();
	}
	else {

	    //
	    // OS does not want to enable multicast
	    //

	    Debug.out.println("Setting filter individual and broadcast");
	    hardwareReceiveFilter |= befehl.RX_FILTER_INDIVIDUAL();
	    hardwareReceiveFilter |= befehl.RX_FILTER_BROADCAST();
	}

	//
	// Write the Rx filter
	//

	befehl.NicCommand(Adapter, (short)(befehl.COMMAND_SET_RX_FILTER() | hardwareReceiveFilter));

	//
	// Clear the hash filter
	//

	for (count=0; count < bitsInHashFilter; count++) {

	    befehl.NicCommand(Adapter, (short)(befehl.COMMAND_SET_HASH_FILTER_BIT() | count));
	}

	//
	// Set the hash filter
	//
	if (multivec != null) {
	    for (count=0; count < multivec.size(); count++) {
		for (index=0; index < 6; index++)  {
		    address[index] = ((MultiCast)multivec.elementAt(count)).addr(index);
		    Debug.out.println("" + address[index]);
		}
		
		befehl.NicCommand(Adapter, (short)(befehl.COMMAND_SET_HASH_FILTER_BIT() | 0x400 | HashAddress(address)));
		
	    }
	}

	//
	// If receive filter is not promiscuos or multicast, enable hash multicast for receiving the flow control packets and
	// for the broadcast
	//

	if (!(((hardwareReceiveFilter & befehl.RX_FILTER_PROMISCUOUS()) > 0) ||
	      ((hardwareReceiveFilter & befehl.RX_FILTER_ALL_MULTICAST()) > 0))) {
	 
	    hardwareReceiveFilter |= befehl.RX_FILTER_MULTICAST_HASH();

	    //
	    // Set the flow control enable
	    //

	    if (Adapter.Hardware.get_FlowControlEnable() && Adapter.Hardware.get_FlowControlSupported() &&
		Adapter.Hardware.get_FullDuplexEnable()) {
		
		Debug.out.println("Setting flow control bit");

		//
		// Set the flow control address bit
		//

		flowControlAddress[0] = AdapterLimits.NIC_FLOW_CONTROL_ADDRESS_0;
		flowControlAddress[1] = AdapterLimits.NIC_FLOW_CONTROL_ADDRESS_1;
		flowControlAddress[2] = AdapterLimits.NIC_FLOW_CONTROL_ADDRESS_2;
		flowControlAddress[3] = AdapterLimits.NIC_FLOW_CONTROL_ADDRESS_3;
		flowControlAddress[4] = AdapterLimits.NIC_FLOW_CONTROL_ADDRESS_4;
		flowControlAddress[5] = AdapterLimits.NIC_FLOW_CONTROL_ADDRESS_5;
	
		befehl.NicCommand(Adapter, (short)(befehl.COMMAND_SET_HASH_FILTER_BIT() | 0x0400 |HashAddress(flowControlAddress)));
	    }

	    //
	    // If there is a broadcast error, write value for broadcast
	    //

	    if (Adapter.Hardware.get_BroadcastErrDone() == false) {
		Debug.out.println("Broadcast Err Done");
		for (count=0; count < AdapterLimits.ETHERNET_ADDRESS_SIZE; count++) 
		    broadcastAddress[count] = (byte)0xff;
		    
		befehl.NicCommand(Adapter, (short)(befehl.COMMAND_SET_HASH_FILTER_BIT() | 0x400 |
						   HashAddress(broadcastAddress)) );
	    }
	}

	// Debug.out.println("NICSetReceiveMode: Out");
    }


    /* This routine returns a bit corresponding to the hash address (in order to set the hash filter) */
    private short HashAddress(byte[] Address) {

	int crc, carry, bit;
	int count;
	byte thisByte;

	//
	// Intialize CRC
	//

	crc = 0xffffffff;
	
	for (count = 0; count < 6; count++) {
	    thisByte = Address[count];
	    for ( bit = 0; bit < 8; bit++) {
            	if ((crc & 0x80000000) > 0)
		    carry = 1 ^ (thisByte & 0x01);
		else 
		    carry = 0 ^ (thisByte & 0x01);
            	crc <<= 1;
            	thisByte >>= 1;
            	if (carry > 0)
		    crc  = (crc ^ 0x04c11db6) | carry;
	    }
	    
	}
	return (short)(crc & 0x000003FF) ;
    }
    
    /* This routine sets up the multicast list on the adapter */
     private boolean SetMulticastAddresses() {
    
	byte[] FlowControlAddress = new byte[AdapterLimits.ETHERNET_ADDRESS_SIZE];

	// for debugging purposes
	// Debug.out.println("SetMulticastAddresses: IN");
	
	//
	// Clear all bits in the hash filter, then write all multicast bits back
	//
	
	InitializeHashFilter();
   
	if ((Adapter.Hardware.get_FlowControlEnable() && Adapter.Hardware.get_FlowControlSupported()) &&
	    (Adapter.Hardware.get_FullDuplexEnable())) {
	    
	    //
	    // Set the flow control address bit
	    //
	    
	    FlowControlAddress[0] = AdapterLimits.NIC_FLOW_CONTROL_ADDRESS_0;
	    FlowControlAddress[1] = AdapterLimits.NIC_FLOW_CONTROL_ADDRESS_1;
	    FlowControlAddress[2] = AdapterLimits.NIC_FLOW_CONTROL_ADDRESS_2;
	    FlowControlAddress[3] = AdapterLimits.NIC_FLOW_CONTROL_ADDRESS_3;
	    FlowControlAddress[4] = AdapterLimits.NIC_FLOW_CONTROL_ADDRESS_4;
	    FlowControlAddress[5] = AdapterLimits.NIC_FLOW_CONTROL_ADDRESS_5;
	
	    befehl.NicCommand(Adapter, (short)(befehl.COMMAND_SET_HASH_FILTER_BIT() | 0x0400 | HashAddress(FlowControlAddress)) );
	}
	// Debug.out.println("SetMulticastAddresses: OUT");
    
	return true;
    }
    
    /* Clear all bits in hash filter and setup the multicast address bit */
    private void InitializeHashFilter() {
	
	int count;
        int bitsInHashFilter;
	int numberMulticast;
	byte[] address = new byte[AdapterLimits.ETHERNET_ADDRESS_SIZE];
	int index;
	
	//
	// Clear all bits in the hash filter, then write all multicast bits back
	//

	bitsInHashFilter = Adapter.Hardware.get_BitsInHashFilter();
	
	for (count = 0; count < bitsInHashFilter; count++ ) {    
	    befehl.NicCommand(Adapter, (short)(befehl.COMMAND_SET_HASH_FILTER_BIT() | count));
	}
	
	numberMulticast = Adapter.mc_list.size();
	Vector multicastList = Adapter.mc_list;

	for (count=0; count < numberMulticast; count++) {
	    for (index=0; index < 6; index++)  {
		address[index] = ((MultiCast)(multicastList.elementAt(count))).addr(index);
		Debug.out.println(" " + address[index]);

	    }
	       
	    befehl.NicCommand(Adapter, (short)(befehl.COMMAND_SET_HASH_FILTER_BIT() | 0x400 |
					       HashAddress(address)) );

	}
    }

    /* If autoselection is set, determine the connector and link speed
       by trying the various transceiver types - see technical reference p.64 and Appendix A */    
    private void MainAutoSelectionRoutine(short Options) {
	
	short index;
	
	// for debugging purposes
	// Debug.out.println("MainAutoSelectionRoutine: IN ");
	
	try {
	    Adapter.Hardware.set_Connector(ConnectorType.CONNECTOR_UNKNOWN);   
	}
	catch (UnknownConnectorType e) {
	    Debug.out.println("MainAutoSelectionRoutine: tried to set a wrong connector");
	}

        //
	// Try 100MB Connectors - check if advertised through the Media Options register 
	//

	if (((Options & Register.MEDIA_OPTIONS_100BASETX_AVAILABLE()) > 0) ||
   	    ((Options & Register.MEDIA_OPTIONS_10BASET_AVAILABLE()) > 0) ||
    	    ((Options & Register.MEDIA_OPTIONS_MII_AVAILABLE()) > 0)) {	       	  

	    //
	    // For 10Base-T and 100Base-TX, select autonegotiation instead of autoselect before calling trymii
	    // because autonegotiation is just possible with hubs or switches that also implement auto-negotiation
	    // or with pre-auto-negotiation 10BASE-T or 100BASE-TX
	    // watch out for the difference: 
	    // autoselect: selection of the media and link speed is done by the driver by selecting each available media port and doing
	    // a special test routine
	    // auto-negotiation: the NIC itself tries to determine the link and speed
	    //      
            
	    if (((Options & Register.MEDIA_OPTIONS_100BASETX_AVAILABLE()) > 0) ||
		((Options & Register.MEDIA_OPTIONS_10BASET_AVAILABLE()) > 0)) {
		try {
		    Adapter.Hardware.set_Connector(ConnectorType.CONNECTOR_AUTONEGOTIATION);
		}
		catch (UnknownConnectorType e) {
		    Debug.out.println("MainAutoSelectionRoutine: wrong connector");
		}
	    } 
	    else {
		try {
		    Adapter.Hardware.set_Connector(ConnectorType.CONNECTOR_MII);
		}
		catch (UnknownConnectorType e) {
		    Debug.out.println("MainAutoSelectionRoutine: wrong connector");
		}
	    }

	    Debug.out.println("MainAutoSelectionRoutine: Trying MII");

	    // now call TryMII
	    // this method checks the autonegotiation logic or an off-chip MII PHY, depending on the value of xcvrSelect set by the caller
	    // the method exits when found the first device with a good link

	    if (!TryMII(Options)) {
		try {
		    Adapter.Hardware.set_Connector(ConnectorType.CONNECTOR_UNKNOWN);
		}
		catch (UnknownConnectorType e) {
		    Debug.out.println("MainAutoSelectionRoutine: wrong connector");
		}
	    }
	}
	// if the connector is still unknown, we check the various available types

	//
	// Transceiver available is 100Base-FX
	//

	if (((Options & Register.MEDIA_OPTIONS_100BASEFX_AVAILABLE()) > 0) &&
	    (Adapter.Hardware.get_Connector() == ConnectorType.CONNECTOR_UNKNOWN)) {		
	    Debug.out.println("MainAutoSelectionRoutine: Trying 100BFX");	   		
	    try {
		// for FX we need to do a self directed download and do some statistics do decide wether the link is available
		if (TryLinkBeat(new ConnectorType(ConnectorType.CONNECTOR_100BASEFX))) {			
		    Adapter.Hardware.set_Connector(ConnectorType.CONNECTOR_100BASEFX);
		    Adapter.Hardware.set_LinkSpeed(Softlimits.LINK_SPEED_100);
   		}
	    }
	    catch (UnknownConnectorType e) {
		Debug.out.println("MainAutoSelectionRoutine: wrong connector");
	    }
	    catch (UndefinedConnectorException e) {
		Debug.out.println("MainAutoSelectionRoutine: wrong connector");
	    }
	}

	//
	// Transceiver available is 10AUI
	//

   	if (((Options & Register.MEDIA_OPTIONS_10AUI_AVAILABLE()) > 0) &&
	    (Adapter.Hardware.get_Connector() == ConnectorType.CONNECTOR_UNKNOWN)) {			
	    Debug.out.println("MainAutoSelectionRoutine: Trying 10AUI");
	    try {
		SetupConnector(new ConnectorType(ConnectorType.CONNECTOR_10AUI));
	    }
	    catch (UndefinedConnectorException e) {
		Debug.out.println("MainAutoSelectionRoutine: wrong connector");
	    }

	    //
	    // Try to loopback packet
	    //

	    for (index = 0; index < 3; index++) {
		if (TestPacket()) {
		    try {
			Adapter.Hardware.set_Connector(ConnectorType.CONNECTOR_10AUI);
			Adapter.Hardware.set_LinkSpeed(Softlimits.LINK_SPEED_10);
		    }
		    catch (UnknownConnectorType e) {
			Debug.out.println("MainAutoSelectionRoutine: wrong connector");
		    }
		    Debug.out.println("MainAutoSelectionRoutine: Found AUI");
		    break;
		}
	    }
	    if (index == 3)
		Debug.out.println("MainAutoSelectionRoutine: Unable to find AUI");
	}

	//
	// Transceiver available is 10Base-2
	//

	if (((Options & Register.MEDIA_OPTIONS_10BASE2_AVAILABLE()) > 0) &&
	    (Adapter.Hardware.get_Connector() == ConnectorType.CONNECTOR_UNKNOWN)) {			
	    Debug.out.println("MainAutoSelectionRoutine: Trying 10BASEB2");

	    //
	    // Set up the connector
	    //

	    try {
		SetupConnector(new ConnectorType(ConnectorType.CONNECTOR_10BASE2));
	    }
	    catch (UndefinedConnectorException e) {
		Debug.out.println("MainAutoSelectionRoutine: wrong connector");
	    }

	    //
	    // Try to loopback packet
	    //

	    for (index = 0; index < 3; index++) {
		if (TestPacket()) {
		    try {
			Adapter.Hardware.set_Connector(ConnectorType.CONNECTOR_10BASE2);
			Adapter.Hardware.set_LinkSpeed(Softlimits.LINK_SPEED_10);
		    }
		    catch (UnknownConnectorType e) {
			Debug.out.println("MainAutoSelectionRoutine: wrong connector");
		    }
		    Debug.out.println("MainAutoSelectionRoutine: Found 10Base2 ");
		    break;
		}
	    }
	    if (index == 3)
		Debug.out.println("MainAutoSelectionRoutine: Unable to find 10Base2");

	    //
	    // Disable DC converter - see technical reference p.159
	    //

	    befehl.NicCommand(Adapter, befehl.COMMAND_DISABLE_DC_CONVERTER());

	    //
	    // Check if DC convertor has been disabled
	    //

	    CheckDCConverter(false);
	}	

	//
	// Nothing left to try - set to default values
	//

	if (Adapter.Hardware.get_Connector() == ConnectorType.CONNECTOR_UNKNOWN) {
	    try {
		Adapter.Hardware.set_Connector(Adapter.Hardware.get_ConfigConnector());
		Adapter.Hardware.set_LinkSpeed(Softlimits.LINK_SPEED_10);
	    }
	    catch (UnknownConnectorType e) {
		Debug.out.println("MainAutoSelectionRoutine: wrong connector");
	    }

	    Debug.out.println("MainAutoSelectionRoutine: AutoSelection failed. Using default.");
	    Debug.out.println("MainAutoSelectionRoutine: Connector: " + Adapter.Hardware.get_Connector());
	    try {		
		Adapter.Hardware.set_LinkState(NicHardwareInformation.LINK_DOWN_AT_INIT);
	    }
	    catch (UnknownLinkState e) {
		Debug.out.println("MainAutoSelectionRoutine: wrong linkstate");
	    }
	}

	try {
	    SetupConnector(new ConnectorType(Adapter.Hardware.get_Connector()));
	}
	catch (UndefinedConnectorException e) {
	    Debug.out.println("MainAutoSelectionRoutine: wrong connector");
	}

	// Debug.out.println("MainAutoSelectionRoutine: OUT ");
    }

    private boolean CheckDCConverter(boolean EnabledState) {

	befehl.NicCommand(Adapter, befehl.COMMAND_SELECT_REGISTER_WINDOW() | Register.REGISTER_WINDOW_4());
	waitglobal.set_MediaStatus(befehl.NicReadPortShort(Adapter, Register.MEDIA_STATUS_REGISTER()));
	ComInit.udelay(10);

	if( ((EnabledState) && !((waitglobal.get_MediaStatus() & Register.MEDIA_STATUS_DC_CONVERTER_ENABLED()) > 0) ) ||
	    ((!EnabledState) && ((waitglobal.get_MediaStatus() & Register.MEDIA_STATUS_DC_CONVERTER_ENABLED()) > 0)) )
	    {	
		waitglobal.set_DCConverterEnabledState(EnabledState);  /* for waittimer */
		Adapter.WaitCases = WaitCases.CHECK_DC_CONVERTER;
		int TimeOutCount = timerManager.getCurrentTime() + 3; // 30ms
                WaitTimerArg arg = new WaitTimerArg(Adapter, Adapter.TestDPD, TimeOutCount);
		timerManager.addMillisTimer(10, new WaitTimer(ports, timerManager), arg); 
		while(TimeOutCount > timerManager.getCurrentTime());
		if( (EnabledState && !((waitglobal.get_MediaStatus() & Register.MEDIA_STATUS_DC_CONVERTER_ENABLED()) > 0)) || 
		    (!EnabledState && ((waitglobal.get_MediaStatus() & Register.MEDIA_STATUS_DC_CONVERTER_ENABLED()) > 0)))
		    {	Adapter.Hardware.set_Status(NicHardwareInformation.HARDWARE_STATUS_FAILURE);
		    Debug.out.println("ConfigureDCConverter: Timeout setting DC Converter ");
		    return false;
		    }
	    }
	return true;
    }

    /* Setup new transceiver type in InternalConfig. Determine whether to set JabberGuardEnable, enableSQEStats and linkBeatEnable in MediaStatus.
       Determine if the coax transceiver also needs to be enabled/disabled */
    private void SetupConnector(ConnectorType NewConnector) {

	int InternalConfig = 0;
	int OldInternalConfig = 0;
	short MediaStatus = 0;
	ConnectorType OldConnector = new ConnectorType();
	int arg;

	befehl.NicCommand(Adapter, befehl.COMMAND_SELECT_REGISTER_WINDOW() | Register.REGISTER_WINDOW_3());
	
	InternalConfig = befehl.NicReadPortLong(Adapter, Register.INTERNAL_CONFIG_REGISTER());
	OldInternalConfig = InternalConfig;

	//
	// Save old choice
	//

	try {
	    OldConnector.set_Connector((short)((InternalConfig & Register.INTERNAL_CONFIG_TRANSCEIVER_MASK()) >> 20));
	}      
	catch (UndefinedConnectorException e) {
	    Debug.out.println("SetupConnector: tried to save an unknown old connector!");
	}

	//
	// Program the MII registers if forcing the configuration to 10/100BaseT
	//

	if ((NewConnector.get_Connector() == ConnectorType.CONNECTOR_10BASET) ||
	    (NewConnector.get_Connector() == ConnectorType.CONNECTOR_100BASETX)){

	    //
	    // Clear transceiver type and change to new transceiver type
	    //

	    InternalConfig &= ~(Register.INTERNAL_CONFIG_TRANSCEIVER_MASK());
	    InternalConfig |= (ConnectorType.CONNECTOR_AUTONEGOTIATION << 20);

	    //
	    // Update the internal config register - Only do this if the value has changed to avoid dropping link
	    //

	    if (OldInternalConfig != InternalConfig) {
		befehl.NicWritePortLong(Adapter, Register.INTERNAL_CONFIG_REGISTER(), InternalConfig);
	    }

	    //
	    // Force the MII registers to the correct settings
	    //

	    if (NewConnector.get_Connector() == ConnectorType.CONNECTOR_100BASETX)
		arg = Register.MEDIA_OPTIONS_100BASETX_AVAILABLE();
	    else 
		arg = Register.MEDIA_OPTIONS_10BASET_AVAILABLE();
	    if ( !mii.CheckMIIConfiguration(Adapter, (short)arg)) {

		//
		// If the forced configuration didn't work, check the results and see why
		//

		mii.CheckMIIAutoNegotiationStatus(Adapter);
		return;
	    }
	}
	else {

	    //
	    // Clear transceiver type and change to new transceiver type
	    //

	    InternalConfig = (int)(InternalConfig & ~(Register.INTERNAL_CONFIG_TRANSCEIVER_MASK()));
	    InternalConfig |= (NewConnector.get_Connector() << 20);

	    //
	    // Update the internal config register. Only do this if the value has changed to avoid dropping link
	    //

	    if (OldInternalConfig != InternalConfig) {
		befehl.NicWritePortLong(Adapter, Register.INTERNAL_CONFIG_REGISTER(), InternalConfig);
	    }
	}

	//
	// Determine whether to set enableSQEStats and linkBeatEnable - Automatically set JabberGuardEnable in MediaStatus register	
	//

	befehl.NicCommand(Adapter, befehl.COMMAND_SELECT_REGISTER_WINDOW() | Register.REGISTER_WINDOW_4());
	MediaStatus = befehl.NicReadPortShort(Adapter, Register.MEDIA_STATUS_REGISTER());
	MediaStatus &= ~(Register.MEDIA_STATUS_SQE_STATISTICS_ENABLE() |
			 Register.MEDIA_STATUS_LINK_BEAT_ENABLE() |
			 Register.MEDIA_STATUS_JABBER_GUARD_ENABLE());
	MediaStatus |= Register.MEDIA_STATUS_JABBER_GUARD_ENABLE();

	if (NewConnector.get_Connector() == ConnectorType.CONNECTOR_10AUI)
	    MediaStatus |= Register.MEDIA_STATUS_SQE_STATISTICS_ENABLE();

   	if (NewConnector.get_Connector() == ConnectorType.CONNECTOR_AUTONEGOTIATION)
	    MediaStatus |= Register.MEDIA_STATUS_LINK_BEAT_ENABLE();
	else {
	    if ((NewConnector.get_Connector() == ConnectorType.CONNECTOR_10BASET) ||
		(NewConnector.get_Connector() == ConnectorType.CONNECTOR_100BASETX) ||
		(NewConnector.get_Connector() == ConnectorType.CONNECTOR_100BASEFX)) {
		if (!Adapter.Hardware.get_LinkBeatDisable())
		    MediaStatus |= Register.MEDIA_STATUS_LINK_BEAT_ENABLE();
	    }
	}
	befehl.NicWritePortShort(Adapter, Register.MEDIA_STATUS_REGISTER(), MediaStatus);

	//
	// If configured for coax we must start the internal transceiver. If not, we stop it (in case the configuration changed across a
	// warm boot)  
	//

	if (NewConnector.get_Connector() == ConnectorType.CONNECTOR_10BASE2) {
	    befehl.NicCommand(Adapter, befehl.COMMAND_ENABLE_DC_CONVERTER());
	   
	    //
	    // Check if DC convertor has been enabled
	    //

	    CheckDCConverter(true);
	}
	else {
	    befehl.NicCommand(Adapter, befehl.COMMAND_DISABLE_DC_CONVERTER());
	   
	    //
	    // Check if DC convertor has been disabled
	    //
	    
	    CheckDCConverter(false);
	}
    }


    /* Used to detect if 10Base-T, 100Base-TX, or external MII is available */ 
    private boolean TryMII(short MediaOptions) {

	boolean Handles100Mbit = false;
        
	// for debugging purposes
	// Debug.out.println("TryMII: IN ");
	
	//
	// First see if there's anything connected to the MII
	//

	if (!mii.FindMIIPhy(Adapter)) {
	    Debug.out.println("TryMII: FindMIIPhy failed ");
	    Adapter.Hardware.set_Status(NicHardwareInformation.HARDWARE_STATUS_FAILURE);
	    return false;
	}
	
	//
	// Now we can read the status and try to figure out what's out there - see technical reference p.184
	// contains, jabber detect, link status, autonegotiation complete, capabilities and some other indications
	//
   
	try {
	    waitglobal.set_PhyStatus(mii.ReadMIIPhy(Adapter, mii.MII_PHY_STATUS));
	}
	catch (ReadMIIException e) {
	    waitglobal.set_PhyResponding(false);
	    Debug.out.println("TryMII: Phy not responding ");
	    Adapter.Hardware.set_Status(NicHardwareInformation.HARDWARE_STATUS_FAILURE);
	    return false;
	}
	// no exception caught - the Phy is responding
	waitglobal.set_PhyResponding(true);
	// check for bit [0] (extended capability registers) and for bit [3] (autonegotiation is available)
	if (((waitglobal.get_PhyStatus() & mii.MII_STATUS_AUTO) > 0) && 
	    ((waitglobal.get_PhyStatus() & mii.MII_STATUS_EXTENDED) > 0)) {

	    //
	    // Check the current MII auto-negotiation state and see if we need to start auto-neg over
	    //

	    if (!mii.CheckMIIConfiguration(Adapter, MediaOptions))
		return false;
	    //
	    // See if link is up...
	    //
      
	    try {
		waitglobal.set_PhyStatus(mii.ReadMIIPhy(Adapter, mii.MII_PHY_STATUS));
	    }
	    catch (ReadMIIException e) {
		waitglobal.set_PhyResponding(true);
		Debug.out.println("TryMII: Phy not responding (2) ");
		Adapter.Hardware.set_Status(NicHardwareInformation.HARDWARE_STATUS_FAILURE);
		return false;
	    }
	    waitglobal.set_PhyResponding(true);
      
	    if ((waitglobal.get_PhyStatus() & mii.MII_STATUS_LINK_UP) > 0) {
		try {
		    Handles100Mbit = GetLinkSpeed();
		}
		catch (LinkSpeedException e) {
		    Debug.out.println("TryMII: Unknown link speed ");
		    Adapter.Hardware.set_Status(NicHardwareInformation.HARDWARE_STATUS_FAILURE);
		    return false;
		}
		if (Handles100Mbit) { 
		    Debug.out.println("TryMII: Link speed set to 100");
		    Adapter.Hardware.set_LinkSpeed(Softlimits.LINK_SPEED_100);
		} 
		else {
		    Debug.out.println("TryMII: Link speed set to 10");
		    Adapter.Hardware.set_LinkSpeed(Softlimits.LINK_SPEED_10);
		}			
		return true;
   	
	    } else {

		//
		// Assume 10Mbit if no link
		//

		if ((waitglobal.get_PhyStatus() & mii.MII_STATUS_100MB_MASK) > 0){
		    Debug.out.println("TryMII: Link speed defaulted to 100 ");
		    Adapter.Hardware.set_LinkSpeed(Softlimits.LINK_SPEED_100);		
		}
		else {
		    Debug.out.println("TryMII: Link speed defaulted to 10");
		    Adapter.Hardware.set_LinkSpeed(Softlimits.LINK_SPEED_10);			
		}
		return true;
	    } 
      
	}
	return false;
    }

    /* Download a self-directed packet. If successful, 100BASE-FX is available */
    private boolean TryLinkBeat(ConnectorType NewConnector) {

	short MediaStatus = 0;
	boolean retval = false;
  	
	// for debugging purposes
   	//Debug.out.println("TryLinkBeat: IN");
	
	//
    	// Go quiet for 1.5 seconds to get any N-Way hub into a receptive state to sense the new link speed. We go quiet by switching over
	// to 10BT and disabling Linkbeat.
	//

	Adapter.Hardware.set_LinkBeatDisable(true);
	try {
	    SetupConnector(new ConnectorType(ConnectorType.CONNECTOR_10BASET));
	}
	catch (UndefinedConnectorException e) {
	    Debug.out.println("TryLinkBeat: wrong connector");
	}

	//
	// Delay 1.5 seconds
	//

	int TimeOutCount = timerManager.getCurrentTime() + 150;
	while(TimeOutCount > timerManager.getCurrentTime()) ;

    	befehl.NicCommandWait(Adapter, (short)(befehl.COMMAND_TX_RESET() | befehl.TX_RESET_MASK_NETWORK_RESET()));
    	befehl.NicCommandWait(Adapter, (short)(befehl.COMMAND_RX_RESET() | befehl.RX_RESET_MASK_NETWORK_RESET()));

    	//
    	// Set up for TP transceiver
    	//

    	SetupConnector(NewConnector);

	// delay 5 milliseconds
	TimeOutCount = timerManager.getCurrentTime() + 1; //make it 10ms
	while(TimeOutCount > timerManager.getCurrentTime()) ;

	//
    	// We need to send a test packet to clear the  the Partition if for some reason it's partitioned (i.e., we were testing 100mb before.)
    	// Download a 20 byte packet into the TxFIFO, from us, to us

   	//
	try {
	    if (!DownloadSelfDirected()) {
		return false;
	    }
	}catch (D3C905Exception e) {
	    Debug.out.println("TryLinkBeat: error with DownloadSelfDirected");
	}

	//
	// Acknowledge the down complete
	//

	befehl.NicCommand(Adapter, befehl.COMMAND_ACKNOWLEDGE_INTERRUPT() + Register.INTSTATUS_DOWN_COMPLETE());

 	//
    	// Check MediaStatus for linkbeat indication
    	//

	befehl.NicCommand(Adapter, befehl.COMMAND_SELECT_REGISTER_WINDOW() | Register.REGISTER_WINDOW_4());
	MediaStatus = befehl.NicReadPortShort(Adapter, Register.MEDIA_STATUS_REGISTER());
    	if ((MediaStatus & Register.MEDIA_STATUS_LINK_DETECT()) > 0) {		
	    retval = true;
	}

	befehl.NicCommandWait(Adapter, (short)(befehl.COMMAND_TX_RESET() | befehl.TX_RESET_MASK_NETWORK_RESET()));
    	befehl.NicCommandWait(Adapter, (short)(befehl.COMMAND_RX_RESET() | befehl.RX_RESET_MASK_NETWORK_RESET()));

	// Debug.out.println("TryLinkBeat: OUT with success");
    	return retval;
    }

    /* Download a 20 byte packet into the TxFIFO, from us, to us */
    private boolean DownloadSelfDirected() throws D3C905Exception {
	final int TEST_SIZE = 20;
	DpdListEntry dpd = Adapter.TestDPD;
    
	Memory storage = memMgr.alloc(TEST_SIZE);
	if (storage == null) {
	    Debug.out.println("DownloadSelfDirected: could not allocate memory for storage!");
	    return false;
	}
	// fill data in the buffer
	for (int i=0; i<6; i++)
	    storage.set8(i, Adapter.StationAddress[i]);  // destination is me
	for (int i=0; i<6; i++)
	    storage.set8(i+6, Adapter.StationAddress[i]);  // as is the source
	storage.set8(12, (byte)0);
	storage.set8(16, (byte)0);
	
	// enter the storage and overwrite the already contained storage of the testbuffer
	//dpd.StoreMem(storage, 0, TEST_SIZE); FIXME!!
	dpd.StoreMem(storage);
	
	//
	// Create a single DPD  
	//
	
	dpd.DownNextPointer(0);
	dpd.FrameStartHeader(20 | AdapterLimits.FSH_ROUND_UP_DEFEAT());
	
	//
	// Download DPD
	//
	
	befehl.NicCommandWait(Adapter, (short)befehl.COMMAND_DOWN_STALL());
	befehl.NicWritePortLong(Adapter, Register.DOWN_LIST_POINTER_REGISTER(), dpd.DPDPhysicalAddress());
	befehl.NicCommand(Adapter, (short)befehl.COMMAND_DOWN_UNSTALL());

	//
	// Check if the DPD is done with
	//

	waitglobal.set_DownListPointer(befehl.NicReadPortLong(Adapter, Register.DOWN_LIST_POINTER_REGISTER()));
	ComInit.udelay(10);

	if(waitglobal.get_DownListPointer() == dpd.DPDPhysicalAddress())
	    {	
		Adapter.WaitCases = WaitCases.CHECK_DOWNLOAD_SELFDIRECTED;
		int TimeOutCount = timerManager.getCurrentTime() + 300;  //max = 3s
		WaitTimerArg arg = new WaitTimerArg(Adapter, Adapter.TestDPD, TimeOutCount);
		timerManager.addMillisTimer(100, new WaitTimer(ports, timerManager), arg); 
		while(TimeOutCount > timerManager.getCurrentTime());
		if(waitglobal.get_DownListPointer() != dpd.DPDPhysicalAddress())
		    {	Adapter.Hardware.set_Status(NicHardwareInformation.HARDWARE_STATUS_FAILURE);
		    Debug.out.println("DownloadSelfDirected: DPD not finished");
		    return false;
		    }
	    }
	return true;
    }

    private boolean CheckTransmitInProgress() {

	waitglobal.set_MediaStatus((short)0);
    
	befehl.NicCommand(Adapter, befehl.COMMAND_SELECT_REGISTER_WINDOW() | Register.REGISTER_WINDOW_4());
	waitglobal.set_MediaStatus(befehl.NicReadPortShort(Adapter, Register.MEDIA_STATUS_REGISTER()));
	ComInit.udelay(10);

	if ((waitglobal.get_MediaStatus() & Register.MEDIA_STATUS_TX_IN_PROGRESS()) > 0) {
	    Adapter.WaitCases = WaitCases.CHECK_TRANSMIT_IN_PROGRESS;
	    int TimeOutCount = timerManager.getCurrentTime() + 100;  //max = 1s
	    WaitTimerArg arg = new WaitTimerArg(Adapter, Adapter.TestDPD, TimeOutCount);
	    timerManager.addMillisTimer(10, new WaitTimer(ports, timerManager), arg); 
	    while(TimeOutCount > timerManager.getCurrentTime());
	    if(!((waitglobal.get_MediaStatus() & Register.MEDIA_STATUS_TX_IN_PROGRESS()) > 0))
		{	Adapter.Hardware.set_Status(NicHardwareInformation.HARDWARE_STATUS_FAILURE);
		Debug.out.println("CheckTransmitInProgress: Transmit still in progress");
		return false;
		}
	}
	return true;
    }
  
	
    /* This function is called by TryLoopback to determine if a packet can successfully be loopbacked for 10Base-2 and AUI */
    private boolean TestPacket() {
    
    	boolean ReturnValue = false;
	short MacControl = 0;
	int PacketStatus = 0;
	
	befehl.NicCommand(Adapter, befehl.COMMAND_SELECT_REGISTER_WINDOW() | Register.REGISTER_WINDOW_3());
	MacControl = befehl.NicReadPortShort(Adapter, Register.MAC_CONTROL_REGISTER());

	//
    	// Enable full duplex
    	//

	befehl.NicWritePortShort(Adapter, Register.MAC_CONTROL_REGISTER(), (short)(MacControl | Register.MAC_CONTROL_FULL_DUPLEX_ENABLE()));

	//
    	// Write UpListPointer to UpListPointer register and unstall
    	//

	befehl.NicCommandWait(Adapter, (short)befehl.COMMAND_UP_STALL());    
	befehl.NicWritePortLong(Adapter, Register.UP_LIST_POINTER_REGISTER(), Adapter.HeadUPD.UPDPhysicalAddress());
	befehl.NicCommand(Adapter, (short)befehl.COMMAND_UP_UNSTALL());

	//
    	// Enable receive and transmit and setup our packet filter
    	//

    	befehl.NicCommandWait(Adapter, (short)befehl.COMMAND_TX_ENABLE());
    	befehl.NicCommandWait(Adapter, (short)befehl.COMMAND_RX_ENABLE());
    	befehl.NicCommandWait(Adapter, (short)(befehl.COMMAND_SET_RX_FILTER() + befehl.RX_FILTER_INDIVIDUAL()));

	//
	// Create single DPD and download 
	//

	try {
	    if (!DownloadSelfDirected()) {
		return false;
	    }
	}
	catch (D3C905Exception e) {
	    Debug.out.println("TestPacket: error with DownloadSelfDirected");
	}

	//
	// Check if transmit is still in progress 
	//

	if (!CheckTransmitInProgress())
	    return false;
   
	//
    	// Reset the transmitter to get rid of any TxStatus we haven't seen yet
   	//

	befehl.NicCommandWait(Adapter, (short)(befehl.COMMAND_TX_RESET() | befehl.TX_RESET_MASK_NETWORK_RESET()));

	//
   	// Check UpListPtr to see if it has changed to see if upload complete
	//

	waitglobal.set_UpListPointer(befehl.NicReadPortLong(Adapter, Register.UP_LIST_POINTER_REGISTER()));
	ComInit.udelay(100);

	if(waitglobal.get_UpListPointer() == Adapter.HeadUPD.UPDPhysicalAddress()) {
	    Adapter.WaitCases = WaitCases.AUTONEG_TEST_PACKET;
	    int TimeOutCount = timerManager.getCurrentTime() + 100; //max =1s
	    WaitTimerArg arg = new WaitTimerArg(Adapter, Adapter.TestDPD, TimeOutCount);
	    timerManager.addMillisTimer(10, new WaitTimer(ports, timerManager), arg); 
	    while(TimeOutCount > timerManager.getCurrentTime());
	    if(waitglobal.get_UpListPointer() != Adapter.HeadUPD.UPDPhysicalAddress()) {	
		Debug.out.println("TestPacket: UPD not finished");
		return false;
	    }
	}

	//
	// Check RxStatus. If we've got a packet without any errors, this connector is okay
	//

    	PacketStatus = Adapter.HeadUPD.UpPacketStatus();   
	if (!((PacketStatus & Register.UP_PACKET_STATUS_ERROR()) > 0) && 
	    ((PacketStatus & Register.UP_PACKET_STATUS_COMPLETE()) > 0)) {
	    ReturnValue = true;		// Received a good packet
	}

    	//
    	// The following cleans up after the test we just ran
    	//

    	befehl.NicWritePortLong(Adapter, Register.UP_LIST_POINTER_REGISTER(), 0);
    	befehl.NicWritePortLong(Adapter, Register.DOWN_LIST_POINTER_REGISTER(), 0);
    	Adapter.HeadUPD.UpPacketStatus(0);

    	//
    	// Reset the receiver to wipe anything we haven't seen yet
    	//

    	befehl.NicCommandWait(Adapter, (short)(befehl.COMMAND_RX_RESET() | befehl.RX_RESET_MASK_NETWORK_RESET()));
    	befehl.NicCommand(Adapter, (short)(befehl.COMMAND_ACKNOWLEDGE_INTERRUPT() + Register.INTSTATUS_ACKNOWLEDGE_ALL()));

	//
	// Get out of loopback mode
	//

	befehl.NicCommand(Adapter, befehl.COMMAND_SELECT_REGISTER_WINDOW() | Register.REGISTER_WINDOW_3());
	MacControl = befehl.NicReadPortShort(Adapter, Register.MAC_CONTROL_REGISTER());
	befehl.NicWritePortShort(Adapter, Register.MAC_CONTROL_REGISTER(), (short)(MacControl & ~(Register.MAC_CONTROL_FULL_DUPLEX_ENABLE())));

    	return ReturnValue;
    }

   /* This routine resets the receiver */
    private boolean ResetAndEnableReceiver() {
    
	befehl.NicCommandWait(Adapter, (short)befehl.COMMAND_RX_DISABLE());
	befehl.NicCommandWait(Adapter, (short)(befehl.COMMAND_RX_RESET() | befehl.RX_RESET_MASK_NETWORK_RESET()));
	befehl.NicCommandWait(Adapter, (short)befehl.COMMAND_RX_ENABLE());

	return true;
    }

    /* This routine resets the transmitter */
    public boolean ResetAndEnableTransmitter() {

	befehl.NicCommand(Adapter, befehl.COMMAND_TX_DISABLE());

	//
	// Wait for the transmit to go quiet
	//

	befehl.NicCommand(Adapter, befehl.COMMAND_SELECT_REGISTER_WINDOW() | Register.REGISTER_WINDOW_4());
	waitglobal.set_MediaStatus(befehl.NicReadPortShort(Adapter, Register.MEDIA_STATUS_REGISTER()));
	ComInit.udelay(10);

	if ((waitglobal.get_MediaStatus() & Register.MEDIA_STATUS_TX_IN_PROGRESS()) > 0)
	    {	Adapter.WaitCases = WaitCases.CHECK_TRANSMIT_IN_PROGRESS;
	    int TimeOutCount = timerManager.getCurrentTime() + 100;
	    WaitTimerArg arg = new WaitTimerArg(Adapter, Adapter.TestDPD, TimeOutCount);
	    timerManager.addMillisTimer(10, new WaitTimer(ports, timerManager), arg); 
	    while(TimeOutCount > timerManager.getCurrentTime());
	    if (!((waitglobal.get_MediaStatus() & Register.MEDIA_STATUS_TX_IN_PROGRESS()) > 0)) 	 
		{	Debug.out.println("ResetAndEnableTransmitter: media status is hung");
		return false;
		}
	    } 
	
	//
	// Wait for download engine to stop
	//

	waitglobal.set_dmaControl(befehl.NicReadPortLong(Adapter, Register.DMA_CONTROL_REGISTER()));
	ComInit.udelay(10);

	if ((waitglobal.get_dmaControl() & Register.DMA_CONTROL_DOWN_IN_PROGRESS()) > 0)
	    {	Adapter.WaitCases = WaitCases.CHECK_DMA_CONTROL;
	    int TimeOutCount = timerManager.getCurrentTime() + 100;
	    WaitTimerArg arg = new WaitTimerArg(Adapter, Adapter.TestDPD, TimeOutCount);
	    timerManager.addMillisTimer(10, new WaitTimer(ports, timerManager), arg); 
	    while(TimeOutCount > timerManager.getCurrentTime());
	    if (!((waitglobal.get_dmaControl() & Register.DMA_CONTROL_DOWN_IN_PROGRESS()) > 0)) 	 
		{    Debug.out.println("ResetAndEnableTransmitter: DMAControl hung");
		return false;
		}
	    } 

	if (!befehl.NicCommandWait(Adapter, (short)(befehl.COMMAND_TX_RESET() | befehl.TX_RESET_MASK_DOWN_RESET() ))) {
	    Debug.out.println("ResetAndEnableTransmitter: Tx reset failed");
	    return false;
	}
	befehl.NicCommand(Adapter, (short)befehl.COMMAND_TX_ENABLE());
	return true;
    } 


    /*
     *
     *
     * starting from here up to the end come the interrupt related methods
     * first of all, the interruptHandler which is the main dispatching routine
     * following this come the methods each handling one specific interrrupt
     *
     *
     *
     */

    /**
     * this is the main interrupt dispatching routine 
     */
    public void interrupt() {	
	cpuManager.recordEvent(event_interrupt);
	short intStatus = 0;
	boolean countDownTimerEventCalled = false;

	// for debugging purposes
	//Debug.out.println("InterruptHandler: IN");
	intStatus = befehl.NicReadPortByte(Adapter, Register.INTSTATUS_COMMAND_REGISTER());

	// no interrupt latch? -> no interrupt occured -> just return
	if (!((intStatus & Register.INTSTATUS_INTERRUPT_LATCH()) > 0)) {	
	    Debug.out.println("interruptHandler: no interupt occured???");
	    return;
	}
	else {

	    //
	    // Mask all the interrupts via indication register
	    //

	    befehl.NicMaskAllInterrupt(Adapter);
	    // explicitly acknowledge interrupt latch bit
	    befehl.NicCommand(Adapter, (short)(befehl.COMMAND_ACKNOWLEDGE_INTERRUPT() | befehl.ACKNOWLEDGE_INTERRUPT_LATCH()));
	}
	boolean ok = true;
    	int loopCount = 2;
    	while (loopCount-- > 0) {

	    //
	    // Read the interrupt status register
	    //

	    intStatus = befehl.NicReadPortShort(Adapter, Register.INTSTATUS_COMMAND_REGISTER());
	    // get the interupt bits
	    intStatus &= Register.INTSTATUS_INTERRUPT_MASK();

	    // no interrupt bit(s) set
	    if (!(intStatus > 0)) break;

	    if ((intStatus & Register.INTSTATUS_HOST_ERROR()) > 0) {
		Debug.out.println("interruptHandler: HostError event happened");
		HostErrorEvent();
	    }
        
	    if ((intStatus & Register.INTSTATUS_UPDATE_STATISTICS()) > 0)  {
		// This interrupt will be cleared by reading statistics
		UpdateStatisticsEvent();
	    }

	    if ((intStatus & Register.INTSTATUS_UP_COMPLETE()) > 0) { 
		befehl.NicCommand(Adapter, (short)(befehl.COMMAND_ACKNOWLEDGE_INTERRUPT() | befehl.ACKNOWLEDGE_UP_COMPLETE()));
		//Debug.out.println("interruptHandler: UP_Complete Event!");
		ok = UpCompleteEvent();
		break; // TODO: why do we have a loop ?
	    }

	    if ((intStatus & Register.INTSTATUS_INTERRUPT_REQUESTED()) > 0) {
		befehl.NicCommand(Adapter, (short)(befehl.COMMAND_ACKNOWLEDGE_INTERRUPT() | befehl.ACKNOWLEDGE_INTERRUPT_REQUESTED()));
		CountDownTimerEvent();
		countDownTimerEventCalled = true;
	    }


	    if ((intStatus & Register.INTSTATUS_TX_COMPLETE()) > 0)  {
		TxCompleteEvent();
	    }

	    if (((intStatus & Register.INTSTATUS_RX_COMPLETE()) > 0) ||
		((intStatus & Register.INTSTATUS_LINK_EVENT()) > 0) ||
		((intStatus & Register.INTSTATUS_DOWN_COMPLETE()) > 0)) {
		Debug.out.println("interruptHandler: Unknown interrupt");
		Debug.out.println("interruptHandler: IntStatus = " + intStatus);
	    }

	    if (false == countDownTimerEventCalled) {
		CountDownTimerEvent();
		countDownTimerEventCalled = true;
	    }

    	}

	// re-enable nic interrupts if interrupt handler was sucessful
        if (ok) befehl.NicUnmaskAllInterrupt(Adapter);
	// Debug.out.println("InterruptHandler: OUT");
	return;
    }

    /* This routine handles the host error */
    private void HostErrorEvent() {

	// for debugging purposes
	// Debug.out.println("HostErrorEvent: IN");

	//
	// Read the internal config
	//

	befehl.NicCommand(Adapter, befehl.COMMAND_SELECT_REGISTER_WINDOW() | Register.REGISTER_WINDOW_3());
	Adapter.keepForGlobalReset = befehl.NicReadPortLong(Adapter, Register.INTERNAL_CONFIG_REGISTER());
	Debug.out.println("HostErrorEvent: Adapter does global reset and restart ");
	
	//
	// Issue Global reset. I will mask the updown reset so that  I don't have to set the UpPoll, DownPoll, UpListPointer
	// and DownListPointer.
	//

	/* THIS IS EXECUTED WITH INTERRUPTS OFF -> DONT BLOCK! */
	return;
	/*
	befehl.NicCommandWait(Adapter, (short)( 
					       befehl.COMMAND_GLOBAL_RESET | befehl.GLOBAL_RESET_MASK_NETWORK_RESET | 
					       befehl.GLOBAL_RESET_MASK_TP_AUI_RESET | befehl.GLOBAL_RESET_MASK_ENDEC_RESET |
					       befehl.GLOBAL_RESET_MASK_AISM_RESET | befehl.GLOBAL_RESET_MASK_SMB_RESET |
					       befehl.GLOBAL_RESET_MASK_VCO_RESET | befehl.GLOBAL_RESET_MASK_UP_DOWN_RESET));


	// now restart the adapter
	ReStartAdapter();

	// Debug.out.println("HostErrorEvent: OUT");
	return;
	*/
    }	

    /* This routine handles the interrupt requested event - see documentation */
    private void CountDownTimerEvent() {
	//Debug.out.println("*** CountDownTimerEvent");


	DpdListEntry headDPD;
	// Debug.out.println("CountDownTimerEvent: IN");
	headDPD = Adapter.HeadDPD;
	
	//
	// This clears the FSH_DPD_EMPTY and a raise condition of hardware
	//
	
	Adapter.TailDPD.FrameStartHeader(0);
	while (true) {
	    if (!((headDPD.FrameStartHeader() & AdapterLimits.FSH_DOWN_COMPLETE()) > 0))
		break;

	    // move buffer from intransmit to usable queue
	    //Buffer2 h = (Buffer2)intransmitBufs.nonblockingFindAndUndockElement(headDPD.StoreMem());
	    //Buffer2 h = (Buffer2)intransmitBufs.nonblockingUndockFirstElement();
	    //h.setData(headDPD.StoreMem());
	    Buffer2 h = headDPD.buffer;
	    headDPD.buffer = null;
	    usableBufs.appendElement(h);
	    if (assertions) {
		// if (headDPD.StoreMem().getStartAddress() != h.getData().getStartAddress()) throw new Error();
		// storemem already is invalidated
	    }



	    // we clear the memory
	    if (!headDPD.clear_StoreMem()) {
	      	Debug.out.println("CountDownTimerEvent: Couldn´t clear StoreMem");
	    }
	    // Clear the down complete bit in the frame start header
	    headDPD.FrameStartHeader(0);
	  
	    Adapter.BytesInDPDQueue -= headDPD.PacketLength();
	    headDPD = headDPD.Next();
    	}

	Adapter.HeadDPD = headDPD;

	if (Adapter.BytesInDPDQueue > 0) 
	    SetCountDownTimer();
    
	// Debug.out.println("CountDownTimerEvent: OUT");
    }


    /* This routine handles the receive event */
    private boolean UpCompleteEvent() {

	UpdListEntry currentUPD = Adapter.HeadUPD;
	int upPacketStatus;
	int frameLength;
	Memory received;
	int len;
    
	// for debugging purposes
	// Debug.out.println("UpCompleteEvent: IN ");
	//freelist.setConsumer(cpuManager.getCPUState());

   
	cpuManager.recordEvent(event_rcv);
	while (true) {	

	    //
	    // If done with all UPDs break
	    //

	    upPacketStatus = currentUPD.UpPacketStatus();
	    if (!((upPacketStatus & Register.UP_PACKET_STATUS_COMPLETE()) > 0)) {
		break;
	    }

	    //
	    // Get the frame length from the UPD.
	    //

	    frameLength = currentUPD.UpPacketStatus() & 0x1FFF; 
	    if (debugPacketNotice) Debug.out.println("D3C905 Upload: framelength: "+frameLength);

	    //
	    // Check if there is any error bit set.
	    //

	    if ((upPacketStatus & Register.UP_PACKET_STATUS_ERROR()) > 0) {
		
		if ((frameLength < AdapterLimits.ETHERNET_MINIMUM_FRAME_SIZE) ||
		    ((upPacketStatus &  Register.UP_PACKET_STATUS_OVERRUN()) > 0) ||
		    ((upPacketStatus &  Register.UP_PACKET_STATUS_ALIGNMENT_ERROR()) > 0) ||
		    ((upPacketStatus &  Register.UP_PACKET_STATUS_CRC_ERROR()) > 0) ||
		    ((upPacketStatus &  Register.UP_PACKET_STATUS_OVERSIZE_FRAME()) > 0)) { 
	  
		    if ((upPacketStatus &  Register.UP_PACKET_STATUS_RUNT_FRAME()) > 0) {
			Debug.out.println("UpCompleteEvent: Runt frame");
		    }                    
		    if ((upPacketStatus &  Register.UP_PACKET_STATUS_ALIGNMENT_ERROR()) > 0) {
			Debug.out.println("UpCompleteEvent: Alignment error");
			Adapter.Statistics.RxAlignmentError++;
		    }
		    if ((upPacketStatus &  Register.UP_PACKET_STATUS_CRC_ERROR()) > 0) {
			Debug.out.println("UpCompleteEvent: Crc error");
			Adapter.Statistics.RxBadCRCError++;
		    }
		    if ((upPacketStatus &  Register.UP_PACKET_STATUS_OVERSIZE_FRAME()) > 0) {
			Debug.out.println("UpCompleteEvent: Oversize error");
			Adapter.Statistics.RxOversizeError++;
		    }
		
		    //
		    // Discard this packet and move on
		    //

		    currentUPD.UpPacketStatus(0);
		    currentUPD = currentUPD.Next();
		    continue;
		}
		else {
		    Adapter.Statistics.RxFramesOk++; 
		    Adapter.Statistics.RxBytesOk += frameLength;
		}
	    }

	    //
	    // Try to allocate Memory for received data
	    //

	    /*
	    Memory storage = memMgr.alloc(frameLength);      
	    if (storage == null) {
		Debug.out.println("D3C905: can´t allocate memory for receive buffers!");
		return;
	    }
	    */

	    // get the received data
	    received = currentUPD.StoreMem();

	    if (debugPacketNotice) {
		Debug.out.println("D3C905.receive: " + frameLength);
	    }

	    if (debugReceive)
		Dump.xdump(received, 0, frameLength);

	    /*
	    storage.copyFromMemory(received, 0, 0, frameLength);
	    
	    // determine the amount uploaded
	    len = currentUPD.UpPacketStatus();
	    // get bits [12:0]
	    len &= 0x1FFF;

	    if (len != frameLength) {
		Debug.out.println("FRAMELENGTH "+frameLength+" != "+len);
		throw new Error();
	    }

	    // we received an ethernet packet - so hand it over to the etherqueue
	    */

	    // check wether we received a multicast (multicast bit and not a broadcast received)     
	    if (((received.get8(0) & Softlimits.ETH_MULTICAST_BIT) > 0) &&
		!(received.get8(0) == 0xff && received.get8(1) == 0xff && received.get8(2) == 0xff && received.get8(3) == 0xff && 
		  received.get8(4) == 0xff && received.get8(5) == 0xff)) {
		Adapter.Statistics.Rx_MulticastPkts++;
	    }
	    Memory newMem;
	    if (etherConsumer != null) {
	    //Memory rmem = received.getSubRange(0, frameLength);  Interrupthandler should not alloc mem
	    //Debug.out.println("FRAMELEN: "+frameLength);
	    
	    newMem = etherConsumer.processMemory(received, 0, frameLength); //rmem
	    if (newMem == null) {
		// no buffers available
		// -> stop receiving until buffers are again available
		Adapter.HeadUPD = currentUPD; // this is our current position; keep the frames
		// that have been already DMAed
		Debug.out.println("D3C905: Disabled all NIC interrupts");
		throw new Error(); //return false;
	    }
	    if (newMem.size() != AdapterLimits.ETHERNET_MAXIMUM_FRAME_SIZE) {
		if (debugLargeMemory) Debug.out.println("D3C905: warning: got="+newMem.size()+", need="+AdapterLimits.ETHERNET_MAXIMUM_FRAME_SIZE);
		//newMem = newMem.extendFullRange();
		if (newMem.size() < AdapterLimits.ETHERNET_MAXIMUM_FRAME_SIZE) {
		    Debug.out.println("D3C905: error: got="+newMem.size()+", need="+AdapterLimits.ETHERNET_MAXIMUM_FRAME_SIZE);
		    throw new Error("D3C905: NEED LARGER MEMORY");
		}
	    }
	    //newMem = newMem.revoke();
	    } else {
		newMem = received;
	    }

	    currentUPD.StoreMem(newMem);

	    currentUPD.UpPacketStatus(0);
	    currentUPD = currentUPD.Next();
	}
	Adapter.HeadUPD = currentUPD;
    
	//Debug.out.println("UpCompleteEvent: OUT ");
	return true;
    }
 
   /* This routine handles the Tx complete event */
    public void TxCompleteEvent() {
    	Debug.out.println("*** TxCompleteEvent");

	byte txStatus;
    
	// for debugging purposes
	// Debug.out.println("TxCompleteEvent: IN");
    
	txStatus = befehl.NicReadPortByte(Adapter, Register.TX_STATUS_REGISTER());
	befehl.NicWritePortByte(Adapter, Register.TX_STATUS_REGISTER(), txStatus);
    
	if ((txStatus & Register.TX_STATUS_HWERROR()) > 0) {
	   
	    //
	    // Transmit HWError recovery
	    //
	    
	    Debug.out.println("TxCompleteEvent: TxHWError");
	    Adapter.Statistics.TxHWErrors++;		
	    if (!ResetAndEnableTransmitter()) {
		Debug.out.println("TxCompleteEvent: TxReset failed");
		Adapter.Hardware.set_Status(NicHardwareInformation.HARDWARE_STATUS_HUNG);
		return;
	    
	    }

    	}
	else if ((txStatus & Register.TX_STATUS_JABBER()) > 0) {
	  
	    Debug.out.println("TxCompleteEvent: Jabber");
	    Adapter.Statistics.TxJabberError++;
	  
	    if (!ResetAndEnableTransmitter()) {
		Debug.out.println("TxCompleteEvent: TxReset failed");
		Adapter.Hardware.set_Status(NicHardwareInformation.HARDWARE_STATUS_HUNG);
		return;
	    }
	}
	else if ((txStatus & Register.TX_STATUS_MAXIMUM_COLLISION()) > 0) {
	    Debug.out.println("TxCompleteEvent: Maximum collision");
	    Adapter.Statistics.TxMaximumCollisions++;
	    befehl.NicCommand(Adapter, befehl.COMMAND_TX_ENABLE());
    	}
	else {
	    if (txStatus != 0 ) {
		Debug.out.println("TxCompleteEvent: Unknown error");
		Adapter.Statistics.TxUnknownError++;
		befehl.NicCommand(Adapter, befehl.COMMAND_TX_ENABLE());
	    }
	}
	Debug.out.println("TxCompleteEvent: OUT");
    }

    /** This method handles an updateStatistics event */
    public void UpdateStatisticsEvent() {

	NicStatistics statistics = Adapter.Statistics;
	short rxPackets, txPackets, highPackets;
	short rxBytes, txBytes, highBytes;

	// Debug.out.println("UpdateStatisticsEvent: IN");
	
	//
	// Change the window.
	//
	
	befehl.NicCommand(Adapter, befehl.COMMAND_SELECT_REGISTER_WINDOW() | Register.REGISTER_WINDOW_6());

	statistics.TxSQEErrors += befehl.NicReadPortByte(Adapter, Register.SQE_ERRORS_REGISTER());
	statistics.TxMultipleCollisions += befehl.NicReadPortByte(Adapter, Register.MULTIPLE_COLLISIONS_REGISTER());
	statistics.TxSingleCollisions += befehl.NicReadPortByte(Adapter, Register.SINGLE_COLLISIONS_REGISTER());
	statistics.RxOverruns += befehl.NicReadPortByte(Adapter, Register.RX_OVERRUNS_REGISTER());
	statistics.TxCarrierLost += befehl.NicReadPortByte(Adapter, Register.CARRIER_LOST_REGISTER());
	statistics.TxLateCollisions += befehl.NicReadPortByte(Adapter, Register.LATE_COLLISIONS_REGISTER());
	statistics.TxFramesDeferred += befehl.NicReadPortByte(Adapter, Register.FRAMES_DEFERRED_REGISTER());
	rxPackets = befehl.NicReadPortByte(Adapter, Register.FRAMES_RECEIVED_OK_REGISTER());
	txPackets = befehl.NicReadPortByte(Adapter, Register.FRAMES_TRANSMITTED_OK_REGISTER());
	highPackets = befehl.NicReadPortByte(Adapter, Register.UPPER_FRAMES_OK_REGISTER());
	rxPackets += ((highPackets & 0x03) << 8);
	txPackets += ((highPackets & 0x30) << 4);

	if (Adapter.Hardware.get_SQEDisable()) 
	    statistics.TxSQEErrors += txPackets;

	statistics.RxFramesOk += rxPackets;
	statistics.TxFramesOk += txPackets;
	rxBytes = befehl.NicReadPortShort(Adapter, Register.BYTES_RECEIVED_OK_REGISTER());
	txBytes = befehl.NicReadPortShort(Adapter, Register.BYTES_TRANSMITTED_OK_REGISTER());
	befehl.NicCommand(Adapter, befehl.COMMAND_SELECT_REGISTER_WINDOW() | Register.REGISTER_WINDOW_4());
	highBytes = befehl.NicReadPortByte(Adapter, Register.UPPER_BYTES_OK_REGISTER());
	rxBytes += ((highBytes & 0x0F) << 8);
	txBytes += ((highBytes & 0xF0) << 4);
	statistics.RxBytesOk += rxBytes;
	statistics.TxBytesOk += txBytes;
	statistics.RxBadSSD += befehl.NicReadPortByte(Adapter, Register.BAD_SSD_REGISTER());

	// Debug.out.println("UpdateStatisticsEvent: OUT");
    }

    
    public void unmaskInterrupts() {
	befehl.NicUnmaskAllInterrupt(Adapter);
    }


    public void TestReceive() throws NicStatusFailure {
	
	Memory mem, dpd, upd, xmem;
	int TimeOutCount;
	int count;
	short networkDiagnosticsValue;
	DpdListEntry testdpd;
	
	// timer-object implementing Timer Interface
	WaitTimer waittimer = new WaitTimer(ports, timerManager);

	//Debug.out.println("TestAdapter: IN ");

	
	int hardwareReceiveFilter = 0;
	hardwareReceiveFilter |= (byte)(1<<0);
	//hardwareReceiveFilter |= befehl.RX_FILTER_PROMISCUOUS;
	hardwareReceiveFilter |= befehl.RX_FILTER_INDIVIDUAL();
	hardwareReceiveFilter |= befehl.RX_FILTER_BROADCAST();
	befehl.NicCommand(Adapter, (short)(befehl.COMMAND_SET_RX_FILTER() | hardwareReceiveFilter)); 





	befehl.NicCommand(Adapter, befehl.COMMAND_RX_ENABLE());
	befehl.NicUnmaskAllInterrupt(Adapter);


	
	//
	// Write the address to the UpListPointer register
	//
	//	UpdListEntry e = new UpdListEntry(memMgr);
	UpdListEntry e = Adapter.HeadUPD;
	befehl.NicCommand(Adapter, befehl.COMMAND_UP_STALL());
	befehl.NicWritePortLong(Adapter, Register.UP_LIST_POINTER_REGISTER(), e.UPDPhysicalAddress());
	befehl.NicCommand(Adapter, befehl.COMMAND_UP_UNSTALL());



	/*

	for(;;) {
	    int PacketStatus = e.UpPacketStatus();   
	    if (!((PacketStatus & Register.UP_PACKET_STATUS_ERROR) > 0) && 
		((PacketStatus & Register.UP_PACKET_STATUS_COMPLETE) > 0)) {
		break;
	    }
	}
	
	Debug.out.println("TestReceiver: Packet uploaded");
	
	//
	// Check the contents of the packet
	//
	
	upd = e.StoreMem();
	
	Dump.xdump(upd, 0, 64);
	
	// clear the Up Packet Status of the first UPD
	Adapter.HeadUPD.UpPacketStatus(0);

	befehl.NicWritePortLong(Adapter, Register.UP_LIST_POINTER_REGISTER, 0);
	befehl.NicAcknowledgeAllInterrupt(Adapter);

	//
	// Issue transmit and receive reset here
	//

	if (!befehl.NicCommandWait(Adapter, (short)(befehl.COMMAND_TX_RESET | befehl.TX_RESET_MASK_NETWORK_RESET))) {
	    Debug.out.println("TestAdapter: Transmit reset failed");
	    throw new NicStatusFailure();
    	}

	if (!befehl.NicCommandWait(Adapter, (short)(befehl.COMMAND_RX_RESET | befehl.RX_RESET_MASK_NETWORK_RESET))) {
	    Debug.out.println("TestAdapter: Receiver reset failed");
	    throw new NicStatusFailure();
	}       
	*/

	Debug.out.println("TestReceive: OUT");
    }

    public boolean registerNonBlockingConsumer(NonBlockingMemoryConsumer consumer) {
	if (this.etherConsumer != null) {
	    throw new Error("Consumer already registered.");
	}
	this.etherConsumer = consumer;
	return true;
    }


    public void restartProduction() {
	// we did not get a memory object when the receive queue was full
	UpdListEntry currentUPD = Adapter.HeadUPD;
	currentUPD.UpPacketStatus(0);
	Adapter.HeadUPD = currentUPD.Next();
	// re-enable interrupts
	//Debug.out.println("Re-Enable all NIC interrupts");
        befehl.NicUnmaskAllInterrupt(Adapter);
    }

}
