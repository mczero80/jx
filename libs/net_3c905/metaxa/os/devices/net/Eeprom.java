package metaxa.os.devices.net;

/* Klasse mit den Offsets des EEPROMs und Zugriffsfunktionen auf das EEPROM */
/* Siehe auch 3COM-Manual ab Seite 80 */

import metaxa.os.*;
import metaxa.os.devices.*;
import jx.zero.*;
import jx.timer.*;

class Eeprom {

    /*
     * 3Com Node Address
     */

    final static short EEPROM_NODE_ADDRESS_WORD_0 = 0x00;
    final static short EEPROM_NODE_ADDRESS_WORD_1 = 0x01;
    final static short EEPROM_NODE_ADDRESS_WORD_2 = 0x02;
    
    final static short EEPROM_DEVICE_ID = 0x03;
    
    /*++
      
      Mögliche Werte für 3C90xB (siehe auch Seite 81) für die DEVICE_ID:
      
      0x9055 - PCI 10/100 Mbps; shared 10BASE-T/100BASE-TX connector.
      0x9056 - PCI 10/100 Mbps; shared 10BASE-T/100BASE-T4 connector.
      0x9004 - PCI 10BASE-T (TPO)
      0x9005 - PCI 10BASE-T/10BASE-2/AUI(COMBO)
      0x9006 - PCI 10BASE-T/10BASE-2/(TPC)
      
      Ergänzung nach Seite 81:
      0x900A - PCI 10BASE-FL
      0x905A - PCI 10BASE-FX
      
      --*/
    
    final static short EEPROM_MANUFACTURING_DATE = 0x04;
    final static short EEPROM_MANUFACTURING_DIVISION = 0x05;
    final static short EEPROM_MANUFACTURING_PRODUCT_CODE = 0x06;
    final static short EEPROM_MANUFACTURING_ID = 0x07;
    final static short EEPROM_PCI_PARAMETERS_1 = 0x08;
    final static short EEPROM_ROM_INFORMATION	= 0x09;
    
    /*
     * OEM Node address
     */
    
    final static short EEPROM_OEM_NODE_ADDRESS_WORD_0 = 0x0A;
    final static short EEPROM_OEM_NODE_ADDRESS_WORD_1	= 0x0B;
    final static short EEPROM_OEM_NODE_ADDRESS_WORD_2	= 0x0C;
    
    final static short EEPROM_SOFTWARE_INFORMATION_1 = 0x0D;
    
    final static short EEPROM_COMPATABILITY_WORD = 0x0E;
    final static short EEPROM_COMPATABILITY_LEVEL = 0x00;
    
    final static short EEPROM_SOFTWARE_INFORMATION_2 = 0x0F;
    final static short ENABLE_MWI_WORK = 0x0020;
    
    final static short EEPROM_CAPABILITIES_WORD = 0x10;
    final static short EEPROM_RESERVED_LOCATION = 0x11;
    final static short EEPROM_SHORTERNAL_CONFIG_WORD_0 = 0x12;
    final static short EEPROM_SHORTERNAL_CONFIG_WORD_1 = 0x13;
    final static short EEPROM_ANALOG_DIAGNOSTICS = 0x14;
    final static short EEPROM_SOFTWARE_INFORMATION_3 = 0x15;
    
    /*
     * Locations 0x1E - 0x1F are reserved.
     */
    
    final static short EEPROM_CHECKSUM_1 = 0x20;
    
    /*
     * Locations 0x21 - 0x2F are reserved.
     */
    
    final static short EEPROM_SOS_PINS_1_TO_4 = 0x21;
    final static short EEPROM_SOS_PINS_5_TO_7 = 0x22;
    
    /*
     * Locations 0x00 - 0xFD are flexible format locations (4kb EEPROMs)
     */
    
    final static short EEPROM_CHECKSUM_2_UPPER = 0xFE;
    final static short EEPROM_CHECKSUM_2_LOWER = 0xFF;
    
    /*
     * Locations 0x00 - 0x3FD are flexible format locations (16Kb EEPROMs)
     */
    
    final static int EEPROM_CHECKSUM_3_UPPER = 0x3FE;
    final static int EEPROM_CHECKSUM_3_LOWER = 0x3FF;
    final static int EEPROM_COMMAND_MASK = 0xE000;
    final static int EEPROM_COMMAND_AUTOINIT_DONE = 0xE000;
    final static int EEPROM_COMMAND_PCI_CONFIG_WRITE = 0xA000;
    final static int EEPROM_COMMAND_REGISTER_WRITE = 0x6000;
    final static int EEPROM_COMMAND_TX_FIFO_WRITE = 0x2000;
    final static int EEPROM_CURRENT_WINDOW_MASK = 0x7000;
    final static int EEPROM_ADDRESS_MASK = 0x00FF;
    final static int EEPROM_TX_BYTE_COUNT = 0x03FF;
    final static int EEPROM_FLEXIBLE_FORMAT_START = 0x40;
    final static int EEPROM_WORD_ACCESS = 0x1000;
    final static int MAX_FLEX_EEPROM_SIZE = 2048;
    
    final static short EEPROM_WINDOW_0 = (0x0 << 0x8);
    final static short EEPROM_WINDOW_1 = (0x1 << 0x8);
    final static short EEPROM_WINDOW_2 = (0x2 << 0x8);	
    final static short EEPROM_WINDOW_3 = (0x3 << 0x8);
    final static short EEPROM_WINDOW_4 = (0x4 << 0x8);
    final static short EEPROM_WINDOW_5 = (0x5 << 0x8);	
    final static short EEPROM_WINDOW_6 = (0x6 << 0x8);
    final static short EEPROM_WINDOW_7 = (0x7 << 0x8);
    
    // also in Register.java - but these handle access to the EEPROM, so they also appear here
    
    final static short BIOS_ROM_ADDRESS_REGISTER = 0x4;
    final static short BIOS_ROM_DATA_REGISTER = 0x8;
    
    final static short EEPROM_COMMAND_REGISTER = 0xA;
    final static int EEPROM_BUSY_BIT = BitPosition.bit_15();
    final static short EEPROM_COMMAND_READ = 0x0080;   
    final static short EEPROM_WRITE_ENABLE = 0x0030;
    final static short EEPROM_ERASE_REGISTER = 0x00C0;
    final static short EEPROM_WRITE_REGISTER = 0x0040;
    
    final static short EEPROM_DATA_REGISTER = 0xC;

    final static short EEPROM_INTERNAL_CONFIG_WORD_0 = 0x12;
    final static short EEPROM_INTERNAL_CONFIG_WORD_1 = 0x13;
        
    // some references to objects whose methods are needed by the EEPROM-methods
	
    // needed by the CheckIfEEPROMBusy method
    TimerManager timerManager;
    // for writing to the NICs registers
    Befehl befehle;
    // bitmanipulation stuff
    BitPosition bits;
    // offsets of the registers
    Register register;

    // constructor setting up the objects needed by the methods of Eeprom

    public Eeprom(Ports ports, TimerManager timerManager) {
	befehle = new Befehl(ports, timerManager);
	bits = new BitPosition();
	register = new Register();
    }

    private boolean CheckIfEEPROMBusy(NicInformation Adapter) {
	
	short command = 0;
	int count;
 
	int i=0;
	do {
	    command = befehle.NicReadPortShort(Adapter, EEPROM_COMMAND_REGISTER);
	    ComInit.sleep(0,10000);
	} while ( (( command & EEPROM_BUSY_BIT) != 0) &&  (i++ < 100) );
	
	if ((command & EEPROM_BUSY_BIT) != 0) {
	    Debug.out.println("CheckIfEEPROMBusy: command timeout");
	    return false;
	}
	return true; 
    }
    
    /**
     * read a value from the onboard Eeprom
     *
     * @param Adapter information class maintained by D3C905
     * @param EEPROMAdress address of the data to be read
     *
     * @exception NicStatusFailure is thrown if an error occures 
     */
    public short ReadEEPROM(NicInformation Adapter, short EEPROMAddress) throws NicStatusFailure {
	
	short lowerOffset = 0;
	short upperOffset = 0;
	short contents;
       
	// we just can address 64 different 16-bit words 

	if (EEPROMAddress > 0x003F) {
	    lowerOffset = (short)(EEPROMAddress & (short)0x003F);
	    upperOffset = (short)((EEPROMAddress & (short)0x03C0) << 2);
	    EEPROMAddress = (short)(upperOffset | lowerOffset);
	}
	
	befehle.NicCommand(Adapter, befehle.COMMAND_SELECT_REGISTER_WINDOW() | Register.REGISTER_WINDOW_0());
	
	//
	// Check if EEPROM is busy (true means the function didn´t quit with a failure)
	//
	
	if (CheckIfEEPROMBusy(Adapter) != true) {
	    
	    Debug.out.println("ReadEEPROM: EEPROM is busy\n");
	    throw new NicStatusFailure("ReadEEPROM: can´t access EEPROM, because it is busy");    
	}
	
	//
	// Issue the read eeprom data command
	//

	befehle.NicWritePortShort(Adapter, EEPROM_COMMAND_REGISTER, (short)(EEPROM_COMMAND_READ + EEPROMAddress));

	//
	// Check if EEPROM is busy
	//

	if (CheckIfEEPROMBusy(Adapter) != true) {
	    
	    Debug.out.println("ReadEEPROM: EEPROM is busy after command.");
	    throw new NicStatusFailure("ReadEEPROM: the EEPROM is busy after the read command!");
	}

	//
	// Save value read from eeprom
	//

	contents = befehle.NicReadPortShort(Adapter, EEPROM_DATA_REGISTER);
	return contents;
    }


    /**
     * write daa to the Eeprom
     *
     * @param Adapter information class maintained by D3C905
     * @param EEPROMAddress the target address
     * @param Data the data to be written
     *
     * @return true if everything is ok, otherwise the exception is thrown
     * @exception NicStatusFailure is thrown if a error occurs
     */
    public boolean WriteEEPROM(NicInformation Adapter, short EEPROMAddress, short Data) throws NicStatusFailure {
	
	short lowerOffset = 0;
	short upperOffset = 0;
	short saveAddress;
	
	saveAddress = EEPROMAddress;

	if (EEPROMAddress > 0x003F) {
	
		lowerOffset = (short)(EEPROMAddress & (short)0x003F);
		upperOffset = (short)((EEPROMAddress & 0x03C0) << 2);
		EEPROMAddress = (short)(upperOffset | lowerOffset);
	}

	befehle.NicCommand(Adapter, befehle.COMMAND_SELECT_REGISTER_WINDOW() | Register.REGISTER_WINDOW_0());

	//
	// Issue erase register command prior to writing
	//

	befehle.NicWritePortShort(Adapter, EEPROM_COMMAND_REGISTER, (short)EEPROM_WRITE_ENABLE);
	
	if (CheckIfEEPROMBusy(Adapter) != true) {
	        Debug.out.println("WriteEEPROM: Write enable, EEPROM is busy");
		throw new NicStatusFailure("WriteEEPROM: Write enable, EEPROM is busy");
	}
	
	befehle.NicWritePortShort(Adapter, EEPROM_COMMAND_REGISTER, (short)(EEPROM_ERASE_REGISTER + EEPROMAddress));

	if (CheckIfEEPROMBusy(Adapter) != true) {
	    Debug.out.println("WriteEEPROM: Erase Register, EEPROM is busy\n");
	    throw new NicStatusFailure("WriteEEPROM: Erase Register, EEPROM is busy");
	}	

	//
	// Load data to be written to the eeprom
	//

 	befehle.NicWritePortShort(Adapter, EEPROM_DATA_REGISTER, Data);


	if (CheckIfEEPROMBusy(Adapter) != true){

	      Debug.out.println("WriteEEPROM: Write data, EEPROM is busy");
	      throw new NicStatusFailure("WriteEEPROM: Write data, EEPROM is busy");
    	}
	

	//
	// Issue the write eeprom data command 
	//

	befehle.NicWritePortShort(Adapter, EEPROM_COMMAND_REGISTER, (short)EEPROM_WRITE_ENABLE);
	
	if (CheckIfEEPROMBusy(Adapter) != true){

        	Debug.out.println("WriteEEPROM: EEPROM is busy\n");
		throw new NicStatusFailure("WriteEEPROM: EEPROM is busy");
    	}
   	 	
	befehle.NicWritePortShort(Adapter, EEPROM_COMMAND_REGISTER,(short)(EEPROM_WRITE_REGISTER + EEPROMAddress));
	
	if (CheckIfEEPROMBusy(Adapter) != true){

	        Debug.out.println("WriteEEPROM: Write register, EEPROM is busy");
		throw new NicStatusFailure("WriteEEPROM: Write register, EEPROM is busy");
    	}
	return true;
    }

    /* Calculates the EEPROM checksum #1 from offset 0 to 0x1 */
    private short CalculateEEPROMChecksum1(NicInformation Adapter) throws NicStatusFailure {
      
    short checksum=0;
    short value=0;
    short index;
  
    befehle.NicCommand(Adapter, befehle.COMMAND_SELECT_REGISTER_WINDOW() | Register.REGISTER_WINDOW_0());

	for (index = EEPROM_NODE_ADDRESS_WORD_0; index < EEPROM_CHECKSUM_1; index++) {

	    try {
		value = ReadEEPROM(Adapter, index);
	    }
	    catch (NicStatusFailure e) {
		Debug.out.println("CalculateEEPROMChecksum1: Read from EEPROM failed\n");
		// rethrow the exception
		throw e;
	    }

	    checksum ^= (short)bits.lobyte(value);			 
	    checksum ^= (short)bits.lobyte(value);			 
	}
	return((short)checksum);
    }

}


