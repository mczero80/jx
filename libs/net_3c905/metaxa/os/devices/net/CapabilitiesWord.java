package metaxa.os.devices.net;

class CapabilitiesWord {

    private boolean SupportsPlugNPlay;
    private boolean SupportsFullDuplex;
    private boolean SupportsLargePackets;
    private boolean SupportsSlaveDMA;
    private boolean SupportsSecondDMA;
    private boolean SupportsFullBusMaster;
    private boolean SupportsFragBusMaster;
    private boolean SupportsCRCPassThrough;
    private boolean SupportsTxDone;
    private boolean SupportsNoTxLength;
    private boolean SupportsRxRepeat;
    private boolean SupportsSnooping;
    private boolean Supports100Mbps;
    private boolean SupportsPowerManagement;

    CapabilitiesWord(boolean supportsplugnplay, boolean supportsfullduplex, boolean supportslargepackets, 
		    boolean supportsslavedma, boolean supportsseconddma, boolean supportsfullbusmaster,
		    boolean supportsfragbusmaster, boolean supportscrcpassthrough, boolean supportstxdone,
		    boolean supportsnotxlength, boolean supportsrxrepeat, boolean supportssnooping,
		    boolean supports100mbps, boolean supportspowermanagement) {
	set_SupportsPlugNPlay(supportsplugnplay);
	set_SupportsFullDuplex(supportsfullduplex);
	set_SupportsLargePackets(supportslargepackets);
	set_SupportsSlaveDMA(supportsslavedma);
	set_SupportsSecondDMA(supportsseconddma);
	set_SupportsFullBusMaster(supportsfullbusmaster);
	set_SupportsFragBusMaster(supportsfragbusmaster);
	set_SupportsCRCPassThrough(supportscrcpassthrough);
	set_SupportsTxDone(supportstxdone);
	set_SupportsNoTxLength(supportsnotxlength);
	set_SupportsRxRepeat(supportsrxrepeat);
	set_SupportsSnooping(supportssnooping);
	set_Supports100Mbps(supports100mbps);
	set_SupportsPowerManagement(supportspowermanagement);
    }

    public void set_SupportsPlugNPlay(boolean supportsplugnplay) {
	SupportsPlugNPlay = supportsplugnplay;
    }
    public void	set_SupportsFullDuplex(boolean supportsfullduplex) {
	SupportsFullDuplex = supportsfullduplex;
    }
    public void	set_SupportsLargePackets(boolean supportslargepackets) {
	SupportsLargePackets = supportslargepackets;
    }
    public void	set_SupportsSlaveDMA(boolean supportsslavedma) {
	SupportsSlaveDMA = supportsslavedma;
    }
    public void	set_SupportsSecondDMA(boolean supportsseconddma) {
	SupportsSecondDMA = supportsseconddma;
    }
    public void	set_SupportsFullBusMaster(boolean supportsfullbusmaster) {
	SupportsFullBusMaster = supportsfullbusmaster;
    }
    public void	set_SupportsFragBusMaster(boolean supportsfragbusmaster) {
	SupportsFragBusMaster = supportsfragbusmaster;
    }
    public void	set_SupportsCRCPassThrough(boolean supportscrcpassthrough) {
	SupportsCRCPassThrough = supportscrcpassthrough;
    }
    public void	set_SupportsTxDone(boolean supportstxdone) {
	SupportsTxDone = supportstxdone;
    }
    public void	set_SupportsNoTxLength(boolean supportsnotxlength) {
	SupportsNoTxLength = supportsnotxlength;
    }
    public void	set_SupportsRxRepeat(boolean supportsrxrepeat) {
	SupportsRxRepeat = supportsrxrepeat;
    }
    public void set_SupportsSnooping(boolean supportssnooping) {
	SupportsSnooping = supportssnooping;
    }
    public void	set_Supports100Mbps(boolean supports100mbps) {
	Supports100Mbps = supports100mbps;
    }
    public void	set_SupportsPowerManagement(boolean supportspowermanagement) {
	SupportsPowerManagement = supportspowermanagement;
    }

    public boolean get_SupportsPlugNPlay() {
	return SupportsPlugNPlay;
    }
    public boolean get_SupportsFullDuplex() {
	return SupportsFullDuplex;
    }
    public boolean get_SupportsLargePackets() {
	return SupportsLargePackets;
    }
    public boolean get_SupportsSlaveDMA() {
	return SupportsSlaveDMA;
    }
    public boolean get_SupportsSecondDMA() {
	return SupportsSecondDMA;
    }
    public boolean get_SupportsFullBusMaster() {
	return SupportsFullBusMaster;
    }
    public boolean get_SupportsFragBusMaster() {
	return SupportsFragBusMaster;
    }
    public boolean get_SupportsCRCPassThrough() {
	return SupportsCRCPassThrough;
    }
    public boolean get_SupportsTxDone() {
	return SupportsTxDone;
    }
    public boolean get_SupportsNoTxLength() {
	return SupportsNoTxLength;
    }
    public boolean get_SupportsRxRepeat() {
	return SupportsRxRepeat;
    } 
    public boolean get_SupportsSnnoping() {
	return SupportsSnooping;
    }
    public boolean get_Supports100Mbps() {
	return Supports100Mbps;
    }
    public boolean get_SupportsPowerManagement() {
	return SupportsPowerManagement;
    }

} 
