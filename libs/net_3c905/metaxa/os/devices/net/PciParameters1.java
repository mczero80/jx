package metaxa.os.devices.net;

class PciParameters1 {
    
    // Bit 0 - fastBacktoBack oder d3Hot, je nach ASIC
    private byte Bit0;
    // Bit 1
    private byte Lower1Meg;
    // Bit 2
    private byte DisableMemoryBase;
    // Bit 3
    private byte D3SupportCold;
    // Bit 4
    private byte D1Support;
    // Bit 5
    private byte D2Support;
    // Bit [9:6]
    private byte MinimumGrant;
    // Bit [15:10]
    private byte MaximumLatency;

    PciParameters1(byte bit0, byte lower1meg, byte disablememorybase, byte d3supportcold, byte d1support, byte d2support,
                   byte minimumgrant, byte maximumlatency) {
	
	set_Bit0(bit0);
	set_Lower1Meg(lower1meg);
	set_DisableMemoryBase(disablememorybase);
	set_D3SupportCold(d3supportcold);
	set_D1Support(d1support);
	set_D2Support(d2support);
	set_MinimumGrant(minimumgrant);
	set_MaximumLatency(maximumlatency);
    }

    public void set_Bit0(byte bit0) {
	Bit0 = bit0;
    }
    public void set_Lower1Meg(byte lower1meg) {
	Lower1Meg = lower1meg;
    }
    public void set_DisableMemoryBase(byte disablememorybase) {
	DisableMemoryBase = disablememorybase;
    }
    public void set_D3SupportCold(byte d3supportcold) {
	D3SupportCold = d3supportcold;
    }
    public void set_D1Support(byte d1support) {
	D1Support = d1support;
    }
    public void set_D2Support(byte d2support) {
	D2Support = d2support;
    }
    public void set_MinimumGrant(byte minimumgrant) {
	MinimumGrant = minimumgrant;
    }
    public void set_MaximumLatency(byte maximumlatency) {
	MaximumLatency = maximumlatency;
    }

    public byte get_Bit0() {
	return Bit0;
    }
    public byte get_Lower1Meg() {
	return Lower1Meg;
    }
    public byte get_DisableMemoryBase() {
	return DisableMemoryBase;
    }
    public byte get_D1Support() {
	return D1Support;
    }
    public byte get_D2Support() {
	return D2Support;
    }
    public byte get_MinimumGrant() {
	return MinimumGrant;
    }
    public byte get_MaximumLatency() {
	return MaximumLatency;
    }

} 
