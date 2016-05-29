package metaxa.os.devices.net;
import jx.zero.*;
import jx.timer.*;

/**
 * This class holds the methods to send commands to the NIC 
 *
 * @author Marcus Meyerhoefer
 * @author Michael Golm
 */

class Befehl {

    //
    // Global reset command.
    //
    
     static int COMMAND_GLOBAL_RESET() { return  (0x0 << 0xB); }
     static int GLOBAL_RESET_MASK_TP_AUI_RESET() { return BitPosition.bit_0(); }
     static int GLOBAL_RESET_MASK_ENDEC_RESET() { return   BitPosition.bit_1(); }
     static int GLOBAL_RESET_MASK_NETWORK_RESET() { return  BitPosition.bit_2(); }
     static int GLOBAL_RESET_MASK_FIFO_RESET() { return  BitPosition.bit_3(); }
     static int GLOBAL_RESET_MASK_AISM_RESET() { return  BitPosition.bit_4(); }
     static int GLOBAL_RESET_MASK_HOST_RESET() { return 	BitPosition.bit_5(); }
     static int GLOBAL_RESET_MASK_SMB_RESET() { return  BitPosition.bit_6(); }
     static int GLOBAL_RESET_MASK_VCO_RESET() { return  BitPosition.bit_7(); }
     static int GLOBAL_RESET_MASK_UP_DOWN_RESET() { return  BitPosition.bit_8(); }
    
     static int COMMAND_SELECT_REGISTER_WINDOW() { return (0x1 << 0xB); }
     static int COMMAND_ENABLE_DC_CONVERTER() { return  (0x2 << 0xB); }
     static int COMMAND_RX_DISABLE() { return  (0x3 << 0xB); }
     static int COMMAND_RX_ENABLE() { return  (0x4 << 0xB); }
    
    //
    // Receiver reset command.
    //
    
     static int COMMAND_RX_RESET() { return  (0x5 << 0xB); }
     static int RX_RESET_MASK_TP_AUI_RESET() { return  BitPosition.bit_0(); }
     static int RX_RESET_MASK_ENDEC_RESET() { return  BitPosition.bit_1(); }
     static int RX_RESET_MASK_NETWORK_RESET() { return  BitPosition.bit_2(); }
     static int RX_RESET_MASK_FIFO_RESET() { return  BitPosition.bit_3(); }
     static int RX_RESET_MASK_UP_RESET() { return BitPosition.bit_8(); }
    
    static int COMMAND_UP_STALL() { return  ((0x6 << 0xB) | 0x0);} 
    static int COMMAND_UP_UNSTALL() { return   ((0x6 << 0xB) | 0x1);} 
    static int COMMAND_DOWN_STALL() { return   ((0x6 << 0xB) | 0x2);} 
    static int COMMAND_DOWN_UNSTALL() { return  ((0x6 << 0xB) | 0x3);} 
    static int COMMAND_TX_DONE() { return   (0x7 << 0xB);} 
    static int COMMAND_RX_DISCARD() { return   (0x8 << 0xB);} 
    static int COMMAND_TX_ENABLE() { return  (0x9 << 0xB);} 
    static int COMMAND_TX_DISABLE() { return  (0xA << 0xB);} 
    
    //
    // Transmitter reset command.
    //

     static int COMMAND_TX_RESET() { return  (0xB << 0xB); } 
     static int TX_RESET_MASK_TP_AUI_RESET() { return  BitPosition.bit_0(); }
     static int TX_RESET_MASK_ENDEC_RESET() { return  BitPosition.bit_1(); }
     static int TX_RESET_MASK_NETWORK_RESET() { return  BitPosition.bit_2(); }
     static int TX_RESET_MASK_FIFO_RESET() { return  BitPosition.bit_3(); }
     static int TX_RESET_MASK_DOWN_RESET() { return  BitPosition.bit_8(); }
     static int COMMAND_REQUEST_INTERRUPT() { return  (0xC << 0xB); }
    
    //
    // Interrupt acknowledge command.
    //
    
     static int COMMAND_ACKNOWLEDGE_INTERRUPT()   { return  (0xD << 0xB); }
     static int ACKNOWLEDGE_INTERRUPT_LATCH()     { return  BitPosition.bit_0(); }
     static int ACKNOWLEDGE_HOST_ERROR()          { return  BitPosition.bit_1(); }
     static int ACKNOWLEDGE_TX_COMPLETE()         { return  BitPosition.bit_2(); }
     static int ACKNOWLEDGE_RX_COMPLETE()         { return  BitPosition.bit_4(); }
     static int ACKNOWLEDGE_RX_EARLY()            { return  BitPosition.bit_5(); }
     static int ACKNOWLEDGE_INTERRUPT_REQUESTED() { return  BitPosition.bit_6(); }
     static int ACKNOWLEDGE_UPDATE_STATS()        { return  BitPosition.bit_7(); }
     static int ACKNOWLEDGE_LINK_EVENT()          { return  BitPosition.bit_8(); }
     static int ACKNOWLEDGE_DOWN_COMPLETE()       { return  BitPosition.bit_9(); }
     static int ACKNOWLEDGE_UP_COMPLETE()         { return  BitPosition.bit_10(); }
     static int ACKNOWLEDGE_CMD_IN_PROGRESS()     { return  BitPosition.bit_12(); }
     static int ACKNOWLEDGE_ALL_INTERRUPT()       { return  0x7FF; }   
     static int COMMAND_SET_INTERRUPT_ENABLE()    { return  (0xE << 0xB); }
     static int DISABLE_ALL_INTERRUPT()           { return  0x0; } 
     static int COMMAND_SET_INDICATION_ENABLE()   { return  (0xF << 0xB); }

    // static int ENABLE_ALL_INTERRUPT()            { return  0x6EE; } 
    
    static int ENABLE_ALL_INTERRUPT()            { return   ACKNOWLEDGE_HOST_ERROR() |
						       ACKNOWLEDGE_TX_COMPLETE() |
						       ACKNOWLEDGE_RX_COMPLETE() |
						       ACKNOWLEDGE_RX_EARLY()    |
						       ACKNOWLEDGE_INTERRUPT_REQUESTED()  |
						       ACKNOWLEDGE_UPDATE_STATS()      |   
						       ACKNOWLEDGE_LINK_EVENT()    |       
						       ACKNOWLEDGE_DOWN_COMPLETE()   |     
						       ACKNOWLEDGE_UP_COMPLETE()    |      
						       ACKNOWLEDGE_CMD_IN_PROGRESS()     ;
    
    }
    
    //
    // Receive filter command.
    //
    
     static int COMMAND_SET_RX_FILTER() { return (0x10 << 0xB); }
     static int RX_FILTER_INDIVIDUAL() { return  BitPosition.bit_0(); }
     static int RX_FILTER_ALL_MULTICAST() { return  BitPosition.bit_1(); }
     static int RX_FILTER_BROADCAST() { return  BitPosition.bit_2(); }
     static int RX_FILTER_PROMISCUOUS() { return  BitPosition.bit_3(); }
     static int RX_FILTER_MULTICAST_HASH() { return  BitPosition.bit_4(); }
     static int COMMAND_TX_AGAIN() { return  (0x13 << 0xB); }
     static int COMMAND_STATISTICS_ENABLE() { return  (0x15 << 0xB); }
     static int COMMAND_STATISTICS_DISABLE() { return  (0x16 << 0xB); }
     static int COMMAND_DISABLE_DC_CONVERTER() { return  (0x17 << 0xB); }
     static int COMMAND_SET_HASH_FILTER_BIT() { return  (0x19 << 0xB); }
     static int COMMAND_TX_FIFO_BISECT() { return (0x1B << 0xB); }
   
    // Window 0 - Commando Register
    final static int INTSTATUS_COMMAND_REGISTER = 0xE;

    // needed for access to registers
    Ports ports;
    // for the NicCommandWait-Method
    TimerManager timerManager;
    CPUManager cpuManager;

    public Befehl(Ports ports, TimerManager timerManager) {
	 this.ports = ports;
	 this.timerManager = timerManager;
	 cpuManager = (CPUManager) InitialNaming.getInitialNaming().lookup("CPUManager");
    }

    public byte NicReadPortByte(NicInformation Adapter, int Register) {
	return ports.inb(Adapter.IoBaseAddress + Register);
    }
    
    public short NicReadPortShort(NicInformation Adapter, int Register) {
	return ports.inw(Adapter.IoBaseAddress + Register);
    }
    
    public int NicReadPortLong(NicInformation Adapter, int Register) {
	return ports.inl(Adapter.IoBaseAddress + Register);
    }
    
    public void NicWritePortByte(NicInformation Adapter, int Register, byte Value) {
	ports.outb(Adapter.IoBaseAddress + Register, Value);
    }
    
    public void NicWritePortShort(NicInformation Adapter, int Register, short Value) {
	ports.outw(Adapter.IoBaseAddress + Register, Value);
    }
    
    public void NicWritePortLong(NicInformation Adapter, int Register, int Value) {
	ports.outl(Adapter.IoBaseAddress + Register, Value);
    }
    
    public void NicCommand(NicInformation Adapter, int Command) {
	NicWritePortShort(Adapter, INTSTATUS_COMMAND_REGISTER, (short)Command);
    }
 
    // false -> failure of command
    // true -> success of command

    public boolean NicCommandWait(NicInformation Adapter, short Command) {
	
	int count;
	short value;
	
	NicWritePortShort(Adapter, INTSTATUS_COMMAND_REGISTER, Command);
	
	count = timerManager.getCurrentTime() + 100;
	
	do {
	    value = NicReadPortShort(Adapter, INTSTATUS_COMMAND_REGISTER);
	    ComInit.udelay(10);	    
	} while ( ((value & Register.INTSTATUS_COMMAND_IN_PROGRESS()) == 0x01) && (count > timerManager.getCurrentTime()) );
	
	if (count < timerManager.getCurrentTime()) {
	    Debug.out.println("NIC_COMMAND_WAIT: timeout");
	    return false;
	}
	return true;
    }

    public short NicMaskAllInterrupt(NicInformation Adapter) { 
	NicCommand(Adapter, COMMAND_SET_INTERRUPT_ENABLE() | DISABLE_ALL_INTERRUPT() ); 
	return NicReadPortShort(Adapter, INTSTATUS_COMMAND_REGISTER); 
    }
    
    public short NicUnmaskAllInterrupt(NicInformation Adapter){ 
	//Debug.out.println("UNMASK!!");
	//cpuManager.printStackTrace();
	NicCommand(Adapter, COMMAND_SET_INTERRUPT_ENABLE() | ENABLE_ALL_INTERRUPT() ); 
	return NicReadPortShort(Adapter, INTSTATUS_COMMAND_REGISTER); 
    }

    public void NicAcknowledgeAllInterrupt(NicInformation Adapter) {
	NicCommand(Adapter, COMMAND_ACKNOWLEDGE_INTERRUPT() | ACKNOWLEDGE_ALL_INTERRUPT() );
    }
    
    public void NicEnableAllInterruptIndication(NicInformation Adapter) {
	NicCommand(Adapter, COMMAND_SET_INDICATION_ENABLE() | ENABLE_ALL_INTERRUPT() );
    }
    
    public void NicDisableAllInterruptIndication(NicInformation Adapter) {
	NicCommand(Adapter, COMMAND_SET_INDICATION_ENABLE() | DISABLE_ALL_INTERRUPT() );
    }
    
}
