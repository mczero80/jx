package metaxa.os.devices.net;

class RomInformation {
    
    private short Reserved;
    // Bit 22
    private byte RomPresent;
    
    // Mögliche Werte für RomSize
    // 0x00 - 64k * 8.
    // 0x01 - 128k * 8.
    // 0x1x - Reserved.
    //

    // Bit [13:12]
    private byte RomSize;

    RomInformation(byte rompresent, byte romsize) {
	set_RomPresent(rompresent);
	set_RomSize(romsize);
    }

    public void set_RomPresent(byte rompresent) {
	RomPresent = rompresent;
    }

    public void set_RomSize(byte romsize) {
	RomSize = romsize;
    }

    public byte get_RomPresent() {
	return RomPresent;
    }
    
    public byte get_RomSize() {
	return RomSize;
    }

} 
