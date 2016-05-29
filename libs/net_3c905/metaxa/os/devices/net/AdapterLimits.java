package metaxa.os.devices.net;

/* Adapter Limits */

public class AdapterLimits {

    final static int TRANSMIT_FIFO_SIZE = 0x800;
    final static int RECEIVE_FIFO_SIZE = 0x800;
    
    //
    // Ethernet limits.
    // as these values are also needed outside this package, they are public
    
    public final static int ETHERNET_MAXIMUM_FRAME_SIZE = 1514;
    public final static int ETHERNET_MINIMUM_FRAME_SIZE = 60;
    public final static int ETHERNET_ADDRESS_SIZE = 6;
    public final static int ETHERNET_HEADER_SIZE = 14; 
    
    //
    // Flow Control Address that gets put into the hash filter for 
    // flow control enable
    //
    
    final static byte NIC_FLOW_CONTROL_ADDRESS_0 = (byte)0x01;
    final static byte NIC_FLOW_CONTROL_ADDRESS_1 = (byte)0x80;
    final static byte NIC_FLOW_CONTROL_ADDRESS_2 = (byte)0xC2;
    final static byte NIC_FLOW_CONTROL_ADDRESS_3 = (byte)0x00;
    final static byte NIC_FLOW_CONTROL_ADDRESS_4 = (byte)0x00;
    final static byte NIC_FLOW_CONTROL_ADDRESS_5 = (byte)0x01;
    
    //
    // DPD Frame Start header bit definitions.
    //
    /*
    final static int FSH_CRC_APPEND_DISABLE = BitPosition.bit_13();
    final static int FSH_TX_INDICATE = BitPosition.bit_15();
    final static int FSH_DOWN_COMPLETE = BitPosition.bit_16();
    final static int FSH_LAST_KEEP_ALIVE_PACKET = BitPosition.bit_24();
    final static int FSH_ADD_IP_CHECKSUM = BitPosition.bit_25();
    final static int FSH_ADD_TCP_CHECKSUM = BitPosition.bit_26();
    final static int FSH_ADD_UDP_CHECKSUM = BitPosition.bit_27();
    final static int FSH_ROUND_UP_DEFEAT = BitPosition.bit_28();
    final static int FSH_DPD_EMPTY = BitPosition.bit_29();
    final static int FSH_DOWN_INDICATE = BitPosition.bit_31();
    final static int MAXIMUM_SCATTER_GATHER_LIST = 0x10;
    */

    static int FSH_CRC_APPEND_DISABLE() { return BitPosition.bit_13(); }
    static int FSH_TX_INDICATE() { return BitPosition.bit_15(); }
    static int FSH_DOWN_COMPLETE() { return BitPosition.bit_16(); }
    static int FSH_LAST_KEEP_ALIVE_PACKET() { return BitPosition.bit_24(); }
    static int FSH_ADD_IP_CHECKSUM() { return BitPosition.bit_25(); }
    static int FSH_ADD_TCP_CHECKSUM() { return BitPosition.bit_26(); }
    static int FSH_ADD_UDP_CHECKSUM() { return BitPosition.bit_27(); }
    static int FSH_ROUND_UP_DEFEAT() { return BitPosition.bit_28(); }
    static int FSH_DPD_EMPTY() { return BitPosition.bit_29(); }
    static int FSH_DOWN_INDICATE() { return BitPosition.bit_31(); }
    final static int MAXIMUM_SCATTER_GATHER_LIST =  0x10;

    //
    // Software limits defined here.
    //
    final static int NIC_DEFAULT_SEND_COUNT = 0x40;
    final static int NIC_DEFAULT_RECEIVE_COUNT = 0x40;
    final static int NIC_MINIMUM_SEND_COUNT = 0x2;
    final static int NIC_MAXIMUM_SEND_COUNT = 0x80;
    final static int NIC_MINIMUM_RECEIVE_COUNT = 0x2;
    final static int NIC_MAXIMUM_RECEIVE_COUNT = 0x80;

   
}
