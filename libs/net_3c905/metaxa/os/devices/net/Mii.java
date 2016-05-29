package metaxa.os.devices.net;
import metaxa.os.*;
import jx.zero.*;
import jx.timer.*;
/**
 * the class Mii offers methods to access the serial medium independent interface
 */ 
class Mii {

    final static short  MII_PHY_ADDRESS = 0x0C00;
    
    //
    //--------------------- MII register definitions --------------------
    //
    
    final static short  MII_PHY_CONTROL = 0;               // control reg address
    final static short  MII_PHY_STATUS = 1;                // status reg address
    final static short  MII_PHY_OUI = 2;                   // most of the OUI bits
    final static short  MII_PHY_MODEL = 3;                 // model/rev bits, and rest of OUI
    final static short  MII_PHY_ANAR = 4;                  // Auto negotiate advertisement reg
    final static short  MII_PHY_ANLPAR = 5;                // auto negotiate Link Partner
    final static short  MII_PHY_ANER = 0x6;
    final static short  MII_PAR = 0x19;
    final static short  MII_PCR = 0x17;                    // PCS Config register
    
    final static short  MII_PHY_REGISTER_24 = 24;          // Register 24 of the MII
    final static short  MII_PHY_REGISTER_24_PVCIRC = 0x01; // Process Variation Circuit bit
    
    //
    //--------------------- Bit definitions: Physical Management --------------------
    //
    
    final static short  PHY_WRITE = 0x0004;              // Write to PHY (drive MDIO)
    final static short  PHY_DATA1 = 0x0002;              // MDIO data bit
    final static short  PHY_CLOCK = 0x0001;              // MII clock signal
    
    //
    //--------------------- Bit definitions: MII Control --------------------
    //
    
    final static int  MII_CONTROL_RESET = 0x8000;        // reset bit in control reg
    final static int  MII_CONTROL_100MB = 0x2000;        // 100Mbit or 10 Mbit flag
    final static int  MII_CONTROL_ENABLE_AUTO = 0x1000;  // autonegotiate enable
    final static int  MII_CONTROL_ISOLATE = 0x0400;      // islolate bit
    final static int  MII_CONTROL_START_AUTO = 0x0200;   // restart autonegotiate
    final static int  MII_CONTROL_FULL_DUPLEX = 0x0100;
    
    
    //
    //--------------------- Bit definitions: MII Status --------------------
    //
    
    final static int  MII_STATUS_100MB_MASK = 0xE000;     // any of these indicate 100 Mbit
    final static int  MII_STATUS_10MB_MASK = 0x1800;      // either of these indicate 10 Mbit
    final static int  MII_STATUS_AUTO_DONE = 0x0020;      // auto negotiation complete
    final static int  MII_STATUS_AUTO = 0x0008;           // auto negotiation is available
    final static int  MII_STATUS_LINK_UP = 0x0004;        // link status bit
    final static int  MII_STATUS_EXTENDED = 0x0001;       // extended regs exist
    final static int  MII_STATUS_100T4 = 0x8000;          // capable of 100BT4
    final static int  MII_STATUS_100TXFD = 0x4000;        // capable of 100BTX full duplex
    final static int  MII_STATUS_100TX = 0x2000;          // capable of 100BTX
    final static int  MII_STATUS_10TFD = 0x1000;          // capable of 10BT full duplex
    final static int  MII_STATUS_10T = 0x0800;            // capable of 10BT
    
    
    //
    //----------- Bit definitions: Auto-Negotiation Link Partner Ability ----------
    //
    
    final static int  MII_ANLPAR_100T4 = 0x0200;          // support 100BT4
    final static int  MII_ANLPAR_100TXFD = 0x0100;        // support 100BTX full duplex
    final static int  MII_ANLPAR_100TX = 0x0080;          // support 100BTX half duplex
    final static int  MII_ANLPAR_10TFD = 0x0040;          // support 10BT full duplex
    final static int  MII_ANLPAR_10T = 0x0020;            // support 10BT half duplex
    
    //
    //----------- Bit definitions: Auto-Negotiation Advertisement ----------
    //
    
    final static int  MII_ANAR_100T4 = 0x0200;            // support 100BT4
    final static int  MII_ANAR_100TXFD = 0x0100;          // support 100BTX full duplex
    final static int  MII_ANAR_100TX = 0x0080;            // support 100BTX half duplex
    final static int  MII_ANAR_10TFD = 0x0040;            // support 10BT full duplex
    final static int  MII_ANAR_10T = 0x0020;              // support 10BT half duplex
    final static int  MII_ANAR_FLOWCONTROL = 0x0400;      // support Flow Control
    
    final static int  MII_ANAR_MEDIA_MASK = 0x07E0;       // Mask the media selection bits
    final static int  MII_ANAR_MEDIA_100_MASK = (MII_ANAR_100TXFD | MII_ANAR_100TX);
    final static int  MII_ANAR_MEDIA_10_MASK = (MII_ANAR_10TFD | MII_ANAR_10T);
    
    final static short  MII_100TXFD = 0x01;
    final static short  MII_100T4 = 0x02;
    final static short  MII_100TX = 0x03;
    final static short  MII_10TFD = 0x04;
    final static short  MII_10T = 0x05;
    
    //
    //----------- Bit definitions: Auto-Negotiation Expansion ----------
    //
    
    final static short  MII_ANER_LPANABLE = 0x0001;         // Link partner autonegotiatable ?
    final static short  MII_ANER_MLF = 0x0010;              // Multiple Link Fault bit
    
    //
    // MII Transceiver Type store in miiSelect
    //
    
    final static short  MIISELECT_GENERIC = 0x0000;
    final static short  MIISELECT_100BT4 = 0x0001;
    final static short  MIISELECT_10BT = 0x0002;
    final static short  MIISELECT_100BTX = 0x0003;
    final static short  MIISELECT_10BT_ANE = 0x0004;
    final static short  MIISELECT_100BTX_ANE = 0x0005;
    final static short  MIITXTYPE_MASK = 0x000F;


    final static short  MEDIA_NONE = 0;
    final static short  MEDIA_10BASE_TX = 1;
    final static short  MEDIA_10AUI = 2;
    final static short  MEDIA_10BASE_2 = 3;
    final static short  MEDIA_100BASE_TX = 4;
    final static short  MEDIA_100BASE_FX = 5;
    final static short  MEDIA_10BASE_FL = 6;
    final static short  MEDIA_AUTO_SELECT = 7;

    Befehl befehle;
    WaitTimerGlobal waitglobal;
    TimerManager timerManager;
    Ports ports;
    Eeprom eeprom;

    public Mii(Ports ports, TimerManager timerManager) {
	this.ports = ports;
	this.timerManager = timerManager;
	befehle = new Befehl(ports, timerManager);
	waitglobal = WaitTimerGlobal.instance();
	eeprom = new Eeprom(ports, timerManager);
    }

   
    /* Establishes the synchronization for each MII transaction - This is done by sending thirty-two "1" bits */
    private void SendMIIPhyPreamble(NicInformation Adapter) {

	//Debug.out.println("SendMIIPhyPreamble: IN");
	byte index;

	//
	// Set up and send the preamble, a sequence of 32 "1" bits
	//

	befehle.NicCommand(Adapter, Befehl.COMMAND_SELECT_REGISTER_WINDOW() | Register.REGISTER_WINDOW_4());
      
	// techn. reference p.205 - bits [2:0] control the MII - set bit2 to 1 (write data to PHY)
	befehle.NicWritePortShort(Adapter, Register.PHYSICAL_MANAGEMENT_REGISTER(), (short)PHY_WRITE);
      
	for (index = 0; index < 32; index++) {
	    // write a 1 into bit 1
	    befehle.NicWritePortShort(Adapter, Register.PHYSICAL_MANAGEMENT_REGISTER(), (short)(PHY_WRITE | PHY_DATA1));
	    // drive the management clock 
	    befehle.NicWritePortShort(Adapter, Register.PHYSICAL_MANAGEMENT_REGISTER(), (short)(PHY_WRITE | PHY_DATA1 | PHY_CLOCK));
	    ComInit.udelay(1);
	    befehle.NicWritePortShort(Adapter, Register.PHYSICAL_MANAGEMENT_REGISTER(), (short)PHY_WRITE);
	    ComInit.udelay(1);
	}
  
	//Debug.out.println("SendMIIPhyPreamble: OUT");
    }
  
    /**
     * Reads a particular MII PHY register given the proper offset. Also refer to technical reference, 
     * appendix B for information how to drive read or write cycles
     *
     * @param Adapter the most important information class maintained by D3C905
     * @param RegisterAddress the address of the register to be read
     *
     * @exception ReadMIIException is thrown if internal problem occurs
     */
    public short ReadMIIPhy(NicInformation Adapter, short RegisterAddress) throws ReadMIIException {
	
	short PhyManagement = 0;
    	short ReadCommand;
    	int index;
	short pInput = 0;

	//Debug.out.println("ReadMIIPhy: IN");

    	ReadCommand = Adapter.Hardware.get_MIIReadCommand();
	
    	SendMIIPhyPreamble(Adapter);

    	//
    	// Bits 2..6 of the command word specify the register
    	//

    	ReadCommand |= (RegisterAddress & 0x1F) << 2;
  
    	for (index = 0x8000; index > 2; index >>= 1) {
	    befehle.NicCommand(Adapter, Befehl.COMMAND_SELECT_REGISTER_WINDOW() | Register.REGISTER_WINDOW_4());
	    if ((ReadCommand & index) >= 1) {
		befehle.NicWritePortShort(Adapter, Register.PHYSICAL_MANAGEMENT_REGISTER(), (short)(PHY_WRITE | PHY_DATA1));
		befehle.NicWritePortShort(Adapter, Register.PHYSICAL_MANAGEMENT_REGISTER(), (short)(PHY_WRITE | PHY_DATA1 | PHY_CLOCK));
	    }
	    else {
		befehle.NicWritePortShort(Adapter, Register.PHYSICAL_MANAGEMENT_REGISTER(), (short)PHY_WRITE);
		befehle.NicWritePortShort(Adapter, Register.PHYSICAL_MANAGEMENT_REGISTER(), (short)(PHY_WRITE | PHY_CLOCK));
	    }
	    ComInit.udelay(1);
	    befehle.NicWritePortShort(Adapter, Register.PHYSICAL_MANAGEMENT_REGISTER(), (short)PHY_WRITE);
	    ComInit.udelay(1);
    	}

   	//
	// Now run one clock with nobody driving.
	//

	befehle.NicWritePortShort(Adapter, Register.PHYSICAL_MANAGEMENT_REGISTER(), (short)0);
	befehle.NicWritePortShort(Adapter, Register.PHYSICAL_MANAGEMENT_REGISTER(), (short)PHY_CLOCK);
	ComInit.udelay(1);
	befehle.NicWritePortShort(Adapter, Register.PHYSICAL_MANAGEMENT_REGISTER(), (short)0);
	ComInit.udelay(1);

	//
	// Now run one clock, expecting the PHY to be driving a 0 on the data
	// line. If we read a 1, it has to be just his pull-up, and he's not
	// responding.
	//

    	PhyManagement = befehle.NicReadPortShort(Adapter, Register.PHYSICAL_MANAGEMENT_REGISTER());

    	if ((PhyManagement & PHY_DATA1) != 0) {
	    throw new ReadMIIException();
    	}

    	//
    	// We think we are in sync.  Now we read 16 bits of data from the PHY.
    	// we start with bit 15, stop with bit 0
	//

    	for (index = 0x8000; index > 0; index >>= 1) {

	    //
	    // Shift input up one to make room
	    //

	    befehle.NicCommand(Adapter, Befehl.COMMAND_SELECT_REGISTER_WINDOW() | Register.REGISTER_WINDOW_4());
	    befehle.NicWritePortShort(Adapter, Register.PHYSICAL_MANAGEMENT_REGISTER(), (short)PHY_CLOCK);
	    ComInit.udelay(1);
	    befehle.NicWritePortShort(Adapter, Register.PHYSICAL_MANAGEMENT_REGISTER(), (short)0);
	    ComInit.udelay(1);
	    PhyManagement = befehle.NicReadPortShort(Adapter, Register.PHYSICAL_MANAGEMENT_REGISTER());
	    
	    // construct the result
	    if ((PhyManagement & PHY_DATA1) != 0)
		pInput |= index;
	    else
		pInput &= ~index;
	}

	//
	// OK now give it a couple of clocks with nobody driving.
	//

	befehle.NicWritePortShort(Adapter, Register.PHYSICAL_MANAGEMENT_REGISTER(), (short)0);
	
	for (index = 0; index < 2; index++) {
	    
	    befehle.NicWritePortShort(Adapter, Register.PHYSICAL_MANAGEMENT_REGISTER(), (short)PHY_CLOCK);
	    ComInit.udelay(1);
	    befehle.NicWritePortShort(Adapter, Register.PHYSICAL_MANAGEMENT_REGISTER(), (short)0);
	    ComInit.udelay(1);
	}
	//Debug.out.println("ReadMIIPhy: OUT");
	return pInput;
    }
    
    /**
     * Writes to a particular MII PHY register given the proper offset
     *
     * @param Adapter the most important information classed maintained by D3C905
     * @param RegAddr the register address to be written
     * @param Output data to be we written 
     */ 
    public void WriteMIIPhy(NicInformation Adapter, short RegAddr, short Output) {

	int index;
	int index2;
	short[] writecmd = new short[2];

	//Debug.out.println("WriteMIIPhy: IN");

	writecmd[0] = Adapter.Hardware.get_MIIWriteCommand();
	writecmd[1] = 0;
	
	SendMIIPhyPreamble(Adapter);
	
	//
	// Bits 2..6 of the command word specify the register
	//

	writecmd[0] |= (RegAddr & 0x1F) << 2;
	writecmd[1] = Output;
	
	befehle.NicCommand(Adapter, Befehl.COMMAND_SELECT_REGISTER_WINDOW() | Register.REGISTER_WINDOW_4());
	
	for (index2 = 0; index2 < 2; index2++) {
	    
	    for (index = 0x8000; index > 0; index >>= 1) {
		
		if ((writecmd[index2] & index) != 0) {
		    befehle.NicWritePortShort(Adapter, Register.PHYSICAL_MANAGEMENT_REGISTER(), (short)(PHY_WRITE | PHY_DATA1));
		    befehle.NicWritePortShort(Adapter, Register.PHYSICAL_MANAGEMENT_REGISTER(), (short)(PHY_WRITE | PHY_DATA1 | PHY_CLOCK));
		} 
		else {
		    befehle.NicWritePortShort(Adapter, Register.PHYSICAL_MANAGEMENT_REGISTER(), (short)PHY_WRITE);
		    befehle.NicWritePortShort(Adapter, Register.PHYSICAL_MANAGEMENT_REGISTER(), (short)(PHY_WRITE | PHY_CLOCK));
		}
		ComInit.udelay(1);
		befehle.NicWritePortShort(Adapter, Register.PHYSICAL_MANAGEMENT_REGISTER(), (short)PHY_WRITE);
		ComInit.udelay(1);
	    }
	}

	//
	// OK now give it a couple of clocks with nobody driving
	//

	befehle.NicWritePortShort(Adapter, Register.PHYSICAL_MANAGEMENT_REGISTER(), (short)0);
	
	for (index = 0; index < 2; index++) {
	    befehle.NicWritePortShort(Adapter, Register.PHYSICAL_MANAGEMENT_REGISTER(), (short)PHY_CLOCK);
	    ComInit.udelay(1);
	    befehle.NicWritePortShort(Adapter, Register.PHYSICAL_MANAGEMENT_REGISTER(), (short)0);
	    ComInit.udelay(1);
	}

    	//Debug.out.println("WriteMIIPhy: OUT");
    }
    

    /**
     * Since this function is called for forced xcvr configurations, it assumes that the xcvr type has
     * been verified as supported by the NIC
     *
     * @param Adapter the most important information class maintained by D3C905
     */
    public void CheckMIIAutoNegotiationStatus(NicInformation Adapter) {
	
	boolean PhyResponding;
	short PhyStatus = 0;
	short PhyAnar  = 0;
	short PhyAnlpar = 0;

	//Debug.out.println("CheckMIIAutoNegotiationStatus: IN");

	//
	// Check to see if auto-negotiation has completed. Check the results in the status registers
	//

	PhyResponding = true;
	// read the Phy-Status - see technical reference p.184
	try 
	    {
		PhyStatus = ReadMIIPhy(Adapter, (short)MII_PHY_STATUS);
	    }  
	catch (ReadMIIException e) {
	    PhyResponding = false;
	    
	}
	
	if (!PhyResponding) {		
	    Debug.out.println("CheckMIIAutoNegotiationStatus: Phy not responding (1) ");
	    Adapter.Hardware.set_Status(NicHardwareInformation.HARDWARE_STATUS_FAILURE);
	    return;
	}
	if ((PhyStatus & MII_STATUS_LINK_UP) != 0)
	    return; // We have a valid link, so get out!

	//
	// Check to see why auto-negotiation or parallel detection has failed. We'll do this
	// by comparing the advertisement registers between the NIC and the link partner
	//

	PhyResponding = true;
	// read the advertisment register
	try {
	    PhyAnar = ReadMIIPhy(Adapter, (short)MII_PHY_ANAR);
	}
	catch (ReadMIIException e) {
	    PhyResponding = false;
	}
	if (!PhyResponding) {		
	    Debug.out.println("CheckMIIAutoNegotiationStatus: Phy not responding (2) ");
	    Adapter.Hardware.set_Status(NicHardwareInformation.HARDWARE_STATUS_FAILURE);
	    return;
	}
	PhyResponding = true;
	// read the link partner advertisment register
	try {
	    PhyAnlpar = ReadMIIPhy(Adapter, (short)MII_PHY_ANLPAR);
	}
	catch (ReadMIIException e) {
	    PhyResponding = false;
	}
	if (!PhyResponding) {		
	    Debug.out.println("CheckMIIAutoNegotiationStatus: Phy not responding (3) ");		
	    Adapter.Hardware.set_Status(NicHardwareInformation.HARDWARE_STATUS_FAILURE);
	    return;
	}

	//
	// Now, compare what was advertised between the NIC and it's link partner
	//

	if ((PhyAnar & MII_ANAR_MEDIA_MASK) != (PhyAnlpar & MII_ANAR_MEDIA_MASK))
	    Debug.out.println("CheckMIIAutoNegotiationStatus: Incompatible configuration ");

	//Debug.out.println("CheckMIIAutoNegotiationStatus: OUT");
    }
    

    /**
     * Used to setup the configuration for 10Base-T and 100Base-TX
     *
     * @param Adapter the most important information class maintained by D3C905
     * @param MediaOptions the options to be configured
     */
    private boolean ConfigureMII(NicInformation Adapter, short MediaOptions) {

	boolean PhyResponding;
    	short PhyControl = 0;
	short PhyAnar = 0;
	int TimeOutCount;

	// timer-object implementing Timer Interface
	WaitTimer waittimer = new WaitTimer(ports, timerManager);
        
    	//Debug.out.println("ConfigureMII: IN");

    	//
    	// Nowhere is it written that the register must be latched, and since
    	// reset is the last bit out, the contents might not be valid. Read it one more time
    	//

    	PhyResponding = true;
	try {
	    PhyControl = ReadMIIPhy(Adapter, (short)MII_PHY_CONTROL);
	}
	catch (ReadMIIException e) {
	    PhyResponding = false;
	}
    	if (!PhyResponding) {
	    Debug.out.println("ConfigureMII: Phy not responding (1) ");
	    Adapter.Hardware.set_Status(NicHardwareInformation.HARDWARE_STATUS_FAILURE);
	    return false;
	}

    	//
    	// Also, read the ANAR register and clear out it's current settings.
    	//

    	PhyResponding = true;
	try {
	    PhyAnar = ReadMIIPhy(Adapter, (short)MII_PHY_ANAR);
	}
	catch (ReadMIIException e) {
	    PhyResponding = false;
	}
    	if (!PhyResponding) {	
	    Debug.out.println("ConfigureMII: Phy not responding (2) ");
	    Adapter.Hardware.set_Status(NicHardwareInformation.HARDWARE_STATUS_FAILURE);
	    return false;
	}

	// 
	// Set up speed and duplex settings in MII Control and ANAR register
	//

	PhyAnar &= ~(MII_ANAR_100TXFD | MII_ANAR_100TX | MII_ANAR_10TFD	| MII_ANAR_10T);

	//
	// Set up duplex
	//

	if (Adapter.Hardware.get_FullDuplexEnable())
	    PhyControl |= MII_CONTROL_FULL_DUPLEX;
	else
	    PhyControl &= ~(MII_CONTROL_FULL_DUPLEX);

	//
	// Set up flow control
	// NOTE: On some NICs, such as Tornado, this will be hardwired to be set.
	// Clearing it will have no effect.
	//

	if (Adapter.Hardware.get_FlowControlSupported()) 
	    PhyAnar |= MII_ANAR_FLOWCONTROL;
	else
	    PhyAnar &= ~(MII_ANAR_FLOWCONTROL);

	//
	// Set up the media options. For duplex settings, if we're set to auto-select 
	// then enable both half and full-duplex settings. Otherwise, go by what's 
	// been enabled for duplex mode.
	//

	if ((MediaOptions & Register.MEDIA_OPTIONS_100BASETX_AVAILABLE()) != 0) {
	    if (Adapter.Hardware.get_AutoSelect())
		PhyAnar |= (MII_ANAR_100TXFD | MII_ANAR_100TX);
	    else { 
		if (Adapter.Hardware.get_FullDuplexEnable())
		    PhyAnar |= MII_ANAR_100TXFD;
		else
		    PhyAnar |= MII_ANAR_100TX;
	    }
	}
	if ((MediaOptions & Register.MEDIA_OPTIONS_10BASET_AVAILABLE()) != 0) {
	    if (Adapter.Hardware.get_AutoSelect())
		PhyAnar |= (MII_ANAR_10TFD | MII_ANAR_10T);
	    else {
		if (Adapter.Hardware.get_FullDuplexEnable())
		    PhyAnar |= MII_ANAR_10TFD;
		else
		    PhyAnar |= MII_ANAR_10T;
	    }
	}

	//
	// Enable and start auto-negotiation
	//

	PhyControl |= (MII_CONTROL_ENABLE_AUTO | MII_CONTROL_START_AUTO);

	//
	// Write the MII registers back
	//

	WriteMIIPhy(Adapter, MII_PHY_ANAR, (short)PhyAnar);
	WriteMIIPhy(Adapter, MII_PHY_CONTROL, (short)PhyControl);

	//
	// Wait for auto-negotiation to finish
	//

	waitglobal.set_PhyResponding(true);
	try {
	    waitglobal.set_PhyStatus(ReadMIIPhy(Adapter, MII_PHY_STATUS));
	}
	catch (ReadMIIException e) {
	    waitglobal.set_PhyResponding(false);
	}

	ComInit.udelay(1000);
	if (!waitglobal.get_PhyResponding()) {
	    Debug.out.println("ConfigureMII: Phy not responding (3) ");
	    Adapter.Hardware.set_Status(NicHardwareInformation.HARDWARE_STATUS_FAILURE);
	    return false;
	}
	if ((waitglobal.get_PhyStatus() & MII_STATUS_AUTO_DONE) == 0)
	    {	Adapter.WaitCases = NicInformation.CHECK_PHY_STATUS;
	    TimeOutCount = timerManager.getCurrentTime() + 40; // run max = 4s
	    WaitTimerArg arg = new WaitTimerArg(Adapter, Adapter.TestDPD,  TimeOutCount);
	    timerManager.addMillisTimer(10, waittimer, arg);
	    while(TimeOutCount > timerManager.getCurrentTime());
	    if((waitglobal.get_PhyStatus() & MII_STATUS_AUTO_DONE) == 0)
		{	Debug.out.println("ConfigureMII: Autonegotiation not done ");
		try {
		    Adapter.Hardware.set_LinkState(NicHardwareInformation.LINK_DOWN_AT_INIT);
		}
		catch (UnknownLinkState e) {
		    Debug.out.println("ConfigureMII: wrong LinkState set! (1)");
		    return false;
		}
		return false;
		}
	    }
	try {
	    Adapter.Hardware.set_LinkState(NicHardwareInformation.LINK_UP);
	}
	catch (UnknownLinkState e) {
	    Debug.out.println("ConfigureMII: wrong Linkstate set! (2)");
	    return false;
	}
	//Debug.out.println("ConfigureMII: OUT");
	return true;
    }

    /**
     * Sets up the tranceiver through MII registers. This will first check on the current connection
     * state as shown by the MII registers. If the current state matches what the media options support,
     * then the link is kept. If not, the registers will be configured in the proper manner and auto-negotiation
     * will be restarted.
     *
     * Since this function is called for forced xcvr configurations, it assumes that the xcvr type has
     * been verified as supported by the NIC.
     *
     * @param Adapter the most important information class maintained by D3C905
     * @param MediaOptions the options to be checked again 
     */
    public boolean CheckMIIConfiguration(NicInformation Adapter, short MediaOptions) {
	
	boolean PhyResponding;
	short PhyControl;
	short PhyStatus;
	short PhyAnar;
	short tempAnar;

	//Debug.out.println("CheckMIIConfiguration: IN");

	//
	// Check to see if auto-negotiation has completed. Check the results in the control and status registers
	//
	
	try {
	    PhyControl = ReadMIIPhy(Adapter, MII_PHY_CONTROL);
	}
	catch (ReadMIIException e) {
	    Debug.out.println("CheckMIIConfiguration: Phy not responding (1) ");
	    Adapter.Hardware.set_Status(NicHardwareInformation.HARDWARE_STATUS_FAILURE);
	    return false;
	}
	try {
	    PhyStatus = ReadMIIPhy(Adapter, MII_PHY_STATUS);
	}
	catch (ReadMIIException e) {
	    Debug.out.println("CheckMIIConfiguration: Phy not responding (2) ");
	    Adapter.Hardware.set_Status(NicHardwareInformation.HARDWARE_STATUS_FAILURE);
	    return false;
	}
	// if autonegotiation is not enabled in the PHY Control or if the autonegotiation is not finished ...
	if (!(((PhyControl & MII_CONTROL_ENABLE_AUTO) > 0)  && ((PhyStatus & MII_STATUS_AUTO_DONE) > 0))) {
	    //
	    // Auto-negotiation did not complete, so start it over using the new settings.
	    //
	    if (!ConfigureMII(Adapter, MediaOptions))
		return false;
	}

	//
	// Auto-negotiation has completed. Check the results against the ANAR (Autonegotiation advertisment) and ANLPAR (link partner advertisment)
	// registers to see if we need to restart auto-neg
	// ANAR controls which capabilities our NIC is allowed to advertise
	// ANLPAR shows the capabilities that our link partner advertised and which we received during the autonegotiation process
	//

	try {
	    PhyAnar = ReadMIIPhy(Adapter, MII_PHY_ANAR);
	}
	catch (ReadMIIException e) {
	    Debug.out.println("CheckMIIConfiguration: Phy not responding (3) ");
	    Adapter.Hardware.set_Status(NicHardwareInformation.HARDWARE_STATUS_FAILURE);
	    return false;
	}

	//
	// Check to see what we negotiated with the link partner. First, let's make
	// sure that the ANAR is set properly based on the media options defined.
	//

	tempAnar = 0;

	// if the Media Options register indicates (passed as MediaOptions) that a 100BaseTX is available, we check wether autoselection is specified
	// and construct the tempAnar
	// otherwise we check wether full duplex is specified and construct the tempAnar depending of this value

	if ((MediaOptions & Register.MEDIA_OPTIONS_100BASETX_AVAILABLE()) > 0) {
	    if (Adapter.Hardware.get_AutoSelect()) {
		tempAnar |= MII_ANAR_100TXFD | MII_ANAR_100TX;
	    }
	    else {
		if (Adapter.Hardware.get_FullDuplexEnable())
		    tempAnar |= MII_ANAR_100TXFD;
		else
		    tempAnar |= MII_ANAR_100TX;
	    }
	}
	if ((MediaOptions & Register.MEDIA_OPTIONS_10BASET_AVAILABLE()) > 0) {
	    if (Adapter.Hardware.get_AutoSelect())
		tempAnar |= MII_ANAR_10TFD | MII_ANAR_10T;
	    else {
		if (Adapter.Hardware.get_FullDuplexEnable())
		    tempAnar |= MII_ANAR_10TFD;
		else
		    tempAnar |= MII_ANAR_10T;
	    }
	}
	if (Adapter.Hardware.get_FullDuplexEnable() && Adapter.Hardware.get_FlowControlSupported())
	    tempAnar |= MII_ANAR_FLOWCONTROL;

	if ((PhyAnar & MII_ANAR_MEDIA_MASK) == tempAnar) {

	    //
	    // The negotiated configuration hasn't changed. So, return and don't restart auto-negotiation
	    //

	    return true;
	}

	//
	// Check the media settings
	//

	if ((MediaOptions & Register.MEDIA_OPTIONS_100BASETX_AVAILABLE()) > 0) {
	 
	    //
	    // Check 100BaseTX settings
	    //

	    if ((PhyAnar & MII_ANAR_MEDIA_100_MASK) != (tempAnar & MII_ANAR_MEDIA_100_MASK)) {
		Debug.out.println("CheckMIIConfiguration: Re-Initiating autonegotiation..."); 
		return ConfigureMII(Adapter, MediaOptions);
	    }
	}
	if ((MediaOptions & Register.MEDIA_OPTIONS_10BASET_AVAILABLE()) > 0) {
	   
	    //
	    // Check 10BaseT settings
	    //
	    
	    if ((PhyAnar & MII_ANAR_MEDIA_10_MASK) != (tempAnar & MII_ANAR_MEDIA_10_MASK)) {
		Debug.out.println("CheckMIIConfiguration: Re-Initiating autonegotiation..."); 
		return ConfigureMII(Adapter, MediaOptions);
	    }
	}      
	//Debug.out.println("CheckMIIConfiguration: OUT");  
	return true;
    }

  
    /**
     * Search for any PHY that is not known
     *
     * @param Adapter the most important information class maintained by D3C905
     */
    public boolean FindMIIPhy(NicInformation Adapter) {

	short MediaOptions = 0;
	short PhyManagement = 0;
	byte index;
	//Debug.out.println("FindMIIPhy: IN");
	
	//
	// Read the MEDIA OPTIONS to see what connectors are available
	//

	befehle.NicCommand(Adapter, Befehl.COMMAND_SELECT_REGISTER_WINDOW() | Register.REGISTER_WINDOW_3());
    	MediaOptions = befehle.NicReadPortShort(Adapter, Register.MEDIA_OPTIONS_REGISTER());

	if (((MediaOptions & Register.MEDIA_OPTIONS_MII_AVAILABLE()) != 0) || ((MediaOptions & Register.MEDIA_OPTIONS_100BASET4_AVAILABLE()) != 0)) {
	    //
	    // Drop everything, so we are not driving the data, and run the
	    // clock through 32 cycles in case the PHY is trying to tell us
	    // something. Then read the data line, since the PHY's pull-up
	    // will read as a 1 if it's present.
	    //
	    befehle.NicCommand(Adapter, Befehl.COMMAND_SELECT_REGISTER_WINDOW() | Register.REGISTER_WINDOW_4());
	    befehle.NicWritePortShort(Adapter, Register.PHYSICAL_MANAGEMENT_REGISTER(), (short)0);
	
	    for (index = 0; index < 32; index++) {
		ComInit.udelay(1);
		befehle.NicWritePortShort(Adapter, Register.PHYSICAL_MANAGEMENT_REGISTER(), (short)PHY_CLOCK);
		ComInit.udelay(1);
		befehle.NicWritePortShort(Adapter, Register.PHYSICAL_MANAGEMENT_REGISTER(), (short)0);
	    }
		
	    PhyManagement = befehle.NicReadPortShort(Adapter, Register.PHYSICAL_MANAGEMENT_REGISTER());

	    if ((PhyManagement & PHY_DATA1) != 0) {
		return true;
	    } 
	    else {
		return false;
	    }
	} 
	//Debug.out.println("FindMIIPhy: OUT");
    	return true;
    }


    /* MII values need to be updated based on what was set in the registry */
    private short MIIMediaOverride(NicInformation Adapter, short PhyModes) throws MIIMediaOverrideException {

	//Debug.out.println("MIIMediaOverride: IN");
       	
	short MiiType = 0;

	switch(Adapter.Hardware.get_MediaOverride()) {
	    
	case MEDIA_10BASE_TX:
		    
	    if (((PhyModes & MII_STATUS_10TFD) != 0) && (Adapter.Hardware.get_FullDuplexEnable() == true)) 
		MiiType = MIISELECT_10BT;
	    else if ((PhyModes & MII_STATUS_10T) != 0) 
		MiiType = MIISELECT_10BT;
	    else 
		throw new MIIMediaOverrideException();
	    break;

	case MEDIA_100BASE_TX:

	    if (((PhyModes & MII_STATUS_100TXFD) != 0) && (Adapter.Hardware.get_FullDuplexEnable() == true)) 
		MiiType = MIISELECT_100BTX;
	    else if ((PhyModes & MII_STATUS_100TX) != 0) 
		MiiType = MIISELECT_100BTX;
	    else 
		throw new MIIMediaOverrideException();
	    break;
	}
	//Debug.out.println("MIIMediaOverride: OUT");
	return MiiType;

    }

    /**
     * Setup the necessary MII registers with values either read from the EEPROM or from command line
     *
     * @param Adapter the most important information class maintained by D3C905
     * @param NewConnector a new connector to be configured
     */
    public boolean ProgramMII(NicInformation Adapter, ConnectorType NewConnector) {

	boolean PhyResponding;
 	short PhyControl;
    	short PhyStatus;
	int status;
    	short MiiType=0, PhyModes;
	int TimeOutCount;

	//Debug.out.println("ProgrammMII: IN");
    	
	//
    	// First see if there's anything connected to the MII
    	//
    	
	if (!FindMIIPhy(Adapter))
	    return false;						 
    
	//
	// Nowhere is it written that the register must be latched, and since
    	// reset is the last bit out, the contents might not be valid.  read
    	// it one more time.
    	//
    	
	PhyResponding = true;
	try {
	    PhyControl = ReadMIIPhy(Adapter, MII_PHY_CONTROL);
    	}
	catch (ReadMIIException e) {
	    PhyResponding = false;
	}

	//
    	// Now we can read the status and try to figure out what's out there
    	//
	
	try {
	    PhyStatus = ReadMIIPhy(Adapter, MII_PHY_STATUS);
	}
	catch (ReadMIIException e) {
	    PhyResponding =false;
	}
	
	if (!PhyResponding)
	    return false;  	
	
	//
    	// Reads the miiSelect field in EEPROM. Program MII as the default
   	//

    	try {
	    MiiType = eeprom.ReadEEPROM(Adapter, (short)Eeprom.EEPROM_SOFTWARE_INFORMATION_3);				  	  
	}
	catch (NicStatusFailure e) {
	}

	//                                                     
	// If an override is present AND the transceiver type is available on the card, that type will be used  
	//

	try {
	    PhyModes = ReadMIIPhy(Adapter, MII_PHY_STATUS);
	}
	catch (ReadMIIException e) {
	    return false;
	}
	try {
	    PhyControl = ReadMIIPhy(Adapter, MII_PHY_CONTROL);
	}
	catch (ReadMIIException e) {
	    return false;
	}
	try {
	    MiiType = MIIMediaOverride(Adapter, PhyModes);
	}
	catch (MIIMediaOverrideException e) {
	    return false;
	}

  	//
  	// If full duplex selected, set it in PhyControl 
	//

    	if (Adapter.Hardware.get_FullDuplexEnable()) 
	    PhyControl |= MII_CONTROL_FULL_DUPLEX;
    	else   
	    PhyControl &= ~MII_CONTROL_FULL_DUPLEX;
    
    	PhyControl &= ~MII_CONTROL_ENABLE_AUTO;

    	if (((MiiType & MIITXTYPE_MASK) == MIISELECT_100BTX) || ((MiiType & MIITXTYPE_MASK) == MIISELECT_100BTX_ANE)) {
	    PhyControl |= MII_CONTROL_100MB;
	    WriteMIIPhy(Adapter, MII_PHY_CONTROL, PhyControl);
	    //delay 600 milliseconds
	    TimeOutCount = timerManager.getCurrentTime() + 60;
	    while(TimeOutCount > timerManager.getCurrentTime());
	    Adapter.Hardware.set_LinkSpeed(100000000);
	    return true;
    	}
    	else if (((MiiType & MIITXTYPE_MASK ) == MIISELECT_10BT) || ((MiiType & MIITXTYPE_MASK ) == MIISELECT_10BT_ANE)) {
	    PhyControl &= ~MII_CONTROL_100MB;
	    WriteMIIPhy(Adapter, MII_PHY_CONTROL, PhyControl);
	    //delay 600 milliseconds
	    TimeOutCount = timerManager.getCurrentTime() + 60;
	    while(TimeOutCount > timerManager.getCurrentTime()) ;
	    Adapter.Hardware.set_LinkSpeed(10000000);
	    return true;
    	}

    	PhyControl &= ~MII_CONTROL_100MB;
    	WriteMIIPhy(Adapter, MII_PHY_CONTROL, PhyControl);
	//delay 600 milliseconds
	TimeOutCount = timerManager.getCurrentTime() + 60;
	while(TimeOutCount > timerManager.getCurrentTime()) ;
  	Adapter.Hardware.set_LinkSpeed(10000000);
	//Debug.out.println("ProgrammMII: OUT");
    	return false;
  }

}
