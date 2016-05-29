package metaxa.os.devices.net;

/* Klasse, die die Registeraddressen kapselt */

class Register {
          
    //
    // Window definitions.
    //
    
     static int REGISTER_WINDOW_0() { return  0x0; }  // setup/configuration
     static int REGISTER_WINDOW_1() { return  0x1; }  // operating set
     static int REGISTER_WINDOW_2() { return  0x2; }  // station address setup/read
     static int REGISTER_WINDOW_3() { return  0x3; }  // FIFO management
     static int REGISTER_WINDOW_4() { return  0x4; }  // diagnostics
     static int REGISTER_WINDOW_5() { return  0x5; }  // registers set by commands
     static int REGISTER_WINDOW_6() { return  0x6; }  // statistics
     static int REGISTER_WINDOW_7() { return  0x7; }  // bus master control
     static int REGISTER_WINDOW_MASK() { return  0xE000; }
   
     static int INTSTATUS_INTERRUPT_MASK() { return  0x6EE; }  
    
    //
    // Window 0 registers.
    //
    
     static int  BIOS_ROM_ADDRESS_REGISTER() { return  0x4; }
     static int  BIOS_ROM_DATA_REGISTER() { return  0x8; }

     static int  EEPROM_COMMAND_REGISTER() { return 0xA; }
     static int  EEPROM_BUSY_BIT() { return BitPosition.bit_15(); }
     static int  EEPROM_COMMAND_READ() { return  0x0080; }   
     static int  EEPROM_WRITE_ENABLE() { return  0x0030; }
     static int  EEPROM_ERASE_REGISTER() { return  0x00C0; }
     static int  EEPROM_WRITE_REGISTER() { return  0x0040; }
    
     static int  EEPROM_DATA_REGISTER() { return  0xC; }

     static int  INTSTATUS_COMMAND_REGISTER() { return  0xE; }
     static int  INTSTATUS_INTERRUPT_LATCH() { return  BitPosition.bit_0(); }
     static int  INTSTATUS_HOST_ERROR() { return  BitPosition.bit_1(); }
     static int  INTSTATUS_TX_COMPLETE() { return  BitPosition.bit_2(); }
     static int  INTSTATUS_RX_COMPLETE() { return  BitPosition.bit_4(); }
     static int  INTSTATUS_INTERRUPT_REQUESTED() { return  BitPosition.bit_6(); }
     static int  INTSTATUS_UPDATE_STATISTICS() { return  BitPosition.bit_7(); }
     static int  INTSTATUS_LINK_EVENT() { return  BitPosition.bit_8(); }
     static int  INTSTATUS_DOWN_COMPLETE() { return  BitPosition.bit_9(); }
     static int  INTSTATUS_UP_COMPLETE() { return  BitPosition.bit_10(); }
     static int  INTSTATUS_COMMAND_IN_PROGRESS() { return  BitPosition.bit_12(); }
     static int  INTSTATUS_INTERRUPT_NONE() { return  0; }
     static int  INTSTATUS_INTERRUPT_ALL() { return  0x6EE; }  
     static int  INTSTATUS_ACKNOWLEDGE_ALL() { return  0x7FF; }
    
    //
    // Window 2 registers.
    //
    
     static int  STATION_ADDRESS_LOW_REGISTER() { return  0x0; }
     static int  STATION_ADDRESS_MID_REGISTER() { return  0x2; }
     static int  STATION_ADDRESS_HIGH_REGISTER() { return 0x4; }
    
    //
    // Window 3 registers.
    //
    
     static int  INTERNAL_CONFIG_REGISTER() { return  0x0; }  
     static int  INTERNAL_CONFIG_DISABLE_BAD_SSD() { return  BitPosition.bit_8(); }
     static int  INTERNAL_CONFIG_ENABLE_TX_LARGE() { return  BitPosition.bit_14(); }
     static int  INTERNAL_CONFIG_ENABLE_RX_LARGE() { return  BitPosition.bit_15(); }
     static int  INTERNAL_CONFIG_AUTO_SELECT() { return  BitPosition.bit_24(); }
     static int  INTERNAL_CONFIG_DISABLE_ROM() { return  BitPosition.bit_25(); }
     static int  INTERNAL_CONFIG_TRANSCEIVER_MASK() { return  0x00F00000; }
    
     static int  MAXIMUM_PACKET_SIZE_REGISTER() { return  0x4; }
    
     static short MAC_CONTROL_REGISTER() { return  0x6; }
     static short MAC_CONTROL_FULL_DUPLEX_ENABLE() { return  (short)BitPosition.bit_5(); }
     static short MAC_CONTROL_ALLOW_LARGE_PACKETS() { return  (short)BitPosition.bit_6(); }
     static short MAC_CONTROL_FLOW_CONTROL_ENABLE() { return  (short)BitPosition.bit_8(); }
     static short MEDIA_OPTIONS_REGISTER() { return  0x8; }
     static short MEDIA_OPTIONS_100BASET4_AVAILABLE() { return  (short)BitPosition.bit_0(); }
     static short MEDIA_OPTIONS_100BASETX_AVAILABLE() { return  (short)BitPosition.bit_1(); }
     static short MEDIA_OPTIONS_100BASEFX_AVAILABLE() { return  (short)BitPosition.bit_2(); }
     static short MEDIA_OPTIONS_10BASET_AVAILABLE() { return  (short)BitPosition.bit_3(); }
     static short MEDIA_OPTIONS_10BASE2_AVAILABLE() { return  (short)BitPosition.bit_4(); }
     static short MEDIA_OPTIONS_10AUI_AVAILABLE() { return  (short)BitPosition.bit_5(); }
     static short MEDIA_OPTIONS_MII_AVAILABLE() { return  (short)BitPosition.bit_6(); }
     static short MEDIA_OPTIONS_10BASEFL_AVAILABLE() { return  (short)BitPosition.bit_8(); }
    
     static int  RX_FREE_REGISTER() { return  0xA; }
     static int  TX_FREE_REGISTER() { return  0xC; }

    //
    // Window 4 registers.
    //

     static int  PHYSICAL_MANAGEMENT_REGISTER() { return  0x8; }
     static int  NETWORK_DIAGNOSTICS_REGISTER() { return  0x6; }
     static int  NETWORK_DIAGNOSTICS_ASIC_REVISION() { return  0x003E; }
     static int  NETWORK_DIAGNOSTICS_ASIC_REVISION_LOW() { return  0x000E; } 
     static int  NETWORK_DIAGNOSTICS_UPPER_BYTES_ENABLE() { return  BitPosition.bit_6(); }
     static int  MEDIA_STATUS_REGISTER() { return  0xA; }
     static int  MEDIA_STATUS_SQE_STATISTICS_ENABLE() { return  BitPosition.bit_3(); }
     static int  MEDIA_STATUS_CARRIER_SENSE() { return  BitPosition.bit_5(); }
     static int  MEDIA_STATUS_JABBER_GUARD_ENABLE() { return  BitPosition.bit_6(); }
     static int  MEDIA_STATUS_LINK_BEAT_ENABLE() { return  BitPosition.bit_7(); }
     static int  MEDIA_STATUS_LINK_DETECT() { return  BitPosition.bit_11(); }
     static int  MEDIA_STATUS_TX_IN_PROGRESS() { return  BitPosition.bit_12(); }
     static int  MEDIA_STATUS_DC_CONVERTER_ENABLED() { return  BitPosition.bit_14(); }
     static int  BAD_SSD_REGISTER() { return  0xC; }
     static int  UPPER_BYTES_OK_REGISTER() { return  0xD; }
    
    //
    // Window 5 registers.
    //
    
     static int  RX_FILTER_REGISTER() { return  0x8; }
     static int  INTERRUPT_ENABLE_REGISTER() { return  0xA; }
     static int  INDICATION_ENABLE_REGISTER() { return  0xC; }
    
    //
    // Window 6 registers.
    //

     static int  CARRIER_LOST_REGISTER() { return  0x0; }
     static int  SQE_ERRORS_REGISTER() { return  0x1; }
     static int  MULTIPLE_COLLISIONS_REGISTER() { return  0x2; }
     static int  SINGLE_COLLISIONS_REGISTER() { return  0x3; }
     static int  LATE_COLLISIONS_REGISTER() { return  0x4; }
     static int  RX_OVERRUNS_REGISTER() { return  0x5; }
     static int  FRAMES_TRANSMITTED_OK_REGISTER() { return  0x6; }
     static int  FRAMES_RECEIVED_OK_REGISTER() { return  0x7; }
     static int  FRAMES_DEFERRED_REGISTER() { return  0x8; }
     static int  UPPER_FRAMES_OK_REGISTER() { return  0x9; }
     static int  BYTES_RECEIVED_OK_REGISTER() { return  0xA; }
     static int  BYTES_TRANSMITTED_OK_REGISTER() { return  0xC; }
    
    //
    // Window 7 registers.
    //

     static int  TIMER_REGISTER() { return  0x1A; }
     static int  TX_STATUS_REGISTER() { return  0x1B; }
     static int  TX_STATUS_MAXIMUM_COLLISION() { return  BitPosition.bit_3(); }
     static int  TX_STATUS_HWERROR() { return  BitPosition.bit_4(); }
     static int  TX_STATUS_JABBER() { return  BitPosition.bit_5(); }
     static int  TX_STATUS_INTERRUPT_REQUESTED() { return  BitPosition.bit_6(); }
     static int  TX_STATUS_COMPLETE() { return  BitPosition.bit_7(); }
     static int  INT_STATUS_AUTO_REGISTER() { return  0x1E; }
     static int  DMA_CONTROL_REGISTER() { return  0x20; }
     static int  DMA_CONTROL_DOWN_STALLED() { return  BitPosition.bit_2(); }
     static int  DMA_CONTROL_UP_COMPLETE() { return  BitPosition.bit_3(); }
     static int  DMA_CONTROL_DOWN_COMPLETE() { return  BitPosition.bit_4(); }
    
     static int  DMA_CONTROL_ARM_COUNTDOWN() { return  BitPosition.bit_6(); }
     static int  DMA_CONTROL_DOWN_IN_PROGRESS() { return  BitPosition.bit_7(); }
     static int  DMA_CONTROL_COUNTER_SPEED() { return  BitPosition.bit_8(); }
     static int  DMA_CONTROL_COUNTDOWN_MODE() { return  BitPosition.bit_9(); }
     static int  DMA_CONTROL_DOWN_SEQ_DISABLE() { return  BitPosition.bit_17(); }
     static int  DMA_CONTROL_DEFEAT_MWI() { return  BitPosition.bit_20(); }
     static int  DMA_CONTROL_DEFEAT_MRL() { return  BitPosition.bit_21(); }
     static int  DMA_CONTROL_UPOVERDISC_DISABLE() { return  BitPosition.bit_22(); }
     static int  DMA_CONTROL_TARGET_ABORT() { return  BitPosition.bit_30(); }
     static int  DMA_CONTROL_MASTER_ABORT() { return  BitPosition.bit_31(); }
    
     static int  DOWN_LIST_POINTER_REGISTER() { return  0x24; }
     static int  DOWN_POLL_REGISTER() { return  0x2D; }
     static int  UP_PACKET_STATUS_REGISTER() { return  0x30; }
     static int  UP_PACKET_STATUS_ERROR() { return  BitPosition.bit_14(); }
     static int  UP_PACKET_STATUS_COMPLETE() { return  BitPosition.bit_15(); }
     static int  UP_PACKET_STATUS_OVERRUN() { return  BitPosition.bit_16(); }
     static int  UP_PACKET_STATUS_RUNT_FRAME() { return  BitPosition.bit_17(); }
     static int  UP_PACKET_STATUS_ALIGNMENT_ERROR() { return  BitPosition.bit_18(); }
     static int  UP_PACKET_STATUS_CRC_ERROR() { return  BitPosition.bit_19(); }
     static int  UP_PACKET_STATUS_OVERSIZE_FRAME() { return  BitPosition.bit_20(); }
     static int  UP_PACKET_STATUS_DRIBBLE_BITS() { return  BitPosition.bit_23(); }
     static int  UP_PACKET_STATUS_OVERFLOW() { return  BitPosition.bit_24(); }
     static int  UP_PACKET_STATUS_IP_CHECKSUM_ERROR() { return  BitPosition.bit_25(); }
     static int  UP_PACKET_STATUS_TCP_CHECKSUM_ERROR() { return  BitPosition.bit_26(); }
     static int  UP_PACKET_STATUS_UDP_CHECKSUM_ERROR() { return  BitPosition.bit_27(); }
     static int  UP_PACKET_STATUS_IMPLIED_BUFFER_ENABLE() { return  BitPosition.bit_28(); }
     static int  UP_PACKET_STATUS_IP_CHECKSUM_CHECKED() { return  BitPosition.bit_29(); }
     static int  UP_PACKET_STATUS_TCP_CHECKSUM_CHECKED() { return  BitPosition.bit_30(); }
     static int  UP_PACKET_STATUS_UDP_CHECKSUM_CHECKED() { return  BitPosition.bit_31(); }
     static int  UP_PACKET_STATUS_ERROR_MASK() { return  0x1F0000; }
     static int  FREE_TIMER_REGISTER() { return  0x34; }
     static int  COUNTDOWN_REGISTER() { return  0x36; }
     static int  UP_LIST_POINTER_REGISTER() { return  0x38; }
     static int  UP_POLL_REGISTER() { return  0x3D; }
     static int  REAL_TIME_COUNTER_REGISTER() { return  0x40; }
     static int  CONFIG_ADDRESS_REGISTER() { return  0x44; }
     static int  CONFIG_DATA_REGISTER() { return  0x48; }
     static int  DEBUG_DATA_REGISTER() { return  0x70; }
     static int  DEBUG_CONTROL_REGISTER() { return  0x74; }

}
