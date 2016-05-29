package metaxa.os.devices.net;

class SoftwareInformation1 {

    final static int EEPROM_OPTIMIZE_NORMAL =  0x1;
    final static int EEPROM_OPTIMIZE_FOR_THROUGHPUT = 0x2;
    final static int EEPROM_OPTIMIZE_FOR_CPU = 0x3; 
    final static int EEPROM_DISABLE_FULL_DUPLEX = 0x00;
    final static int EEPROM_ENABLE_FULL_DUPLEX = 0x01;
    
    private byte Reserved1 = 0;
    private byte OptimizeFor;    
    private byte Reserved2 = 0;
    private boolean LinkBeatDisable;
    private boolean FullDuplexMode;

    SoftwareInformation1(byte optimizefor, boolean linkbeatdisable, boolean fullduplexmode) {
	set_OptimizeFor(optimizefor);
	set_LinkBeatDisable(linkbeatdisable);
	set_FullDuplexMode(fullduplexmode);
    }

    public void set_OptimizeFor(byte optimizefor) {
	OptimizeFor = optimizefor;
    }
    public void set_LinkBeatDisable(boolean linkbeatdisable) {
	LinkBeatDisable = linkbeatdisable;
    }
    public void set_FullDuplexMode(boolean fullduplexmode) {
	FullDuplexMode = fullduplexmode;
    }

    public byte get_OptimizeFor() {
	return OptimizeFor;
    }

    public boolean get_LinkBeatDisable() {
	return LinkBeatDisable;
    }
    public boolean get_FullDuplexMode() {
	return FullDuplexMode;
    }
} 
