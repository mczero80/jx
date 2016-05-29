package metaxa.os.devices.net;

class SoftwareInformation3 {

    final static int EEPROM_GENERIC_MII = 0x00;
    final static int EEPROM_100BASE_T4_MII = 0x01;
    final static int EEPROM_10BASE_T_MII = 0x02;         
    final static int EEPROM_100BASE_TX_MII = 0x03;
    final static int EEPROM_10_BASE_T_AUTONEGOTIATION = 0x04;
    final static int EEPROM_100_BASE_TX_AUTONEGOTIATION = 0x04;                  
    
    private byte ForceXcvr;
    private short Reserved;

    SoftwareInformation3(byte forcexcvr) {
	set_ForceXcvr(forcexcvr);
    }

    public void set_ForceXcvr(byte forcexcvr) {
	ForceXcvr = forcexcvr;
    }
    
    public byte get_ForceXcvr() {
	return ForceXcvr;
    }

} 
