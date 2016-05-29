package metaxa.os.devices.net;

import jx.zero.Debug;

class NicHardwareInformation {

    final static byte HARDWARE_STATUS_WORKING = 0x0;
    final static byte HARDWARE_STATUS_HUNG = 0x1;    
    final static byte HARDWARE_STATUS_FAILURE = 0x2;
    
    final static int MOTHERBOARD_FEATURE_SET = 0x0;
    final static int LOW_COST_ADAPTER_FEATURE_SET = 0x1;
    final static int STANDARD_ADAPTER_FEATURE_SET = 0x2;
    final static int SERVER_ADAPTER_FEATURE_SET = 0x4;
    
    // mögliche Arten des Anschlusses

    final static byte CONNECTOR_10BASET = 0;
    final static byte CONNECTOR_10AUI = 1;
    final static byte CONNECTOR_10BASE2 = 3;
    final static byte CONNECTOR_100BASETX = 4;
    final static byte CONNECTOR_100BASEFX = 5;
    final static byte CONNECTOR_MII = 6;
    final static byte CONNECTOR_AUTONEGOTIATION = 8;
    final static byte CONNECTOR_EXTERNAL_MII = 9;
    final static byte CONNECTOR_UNKNOWN = (byte)0xFF;

    // mögliche Link-States 

    final static byte LINK_UP	= 0;             // Link established
    final static byte LINK_DOWN = 1;             // Link lost
    final static byte LINK_DOWN_AT_INIT = 2;	 // Link lost and needs notification to NDIS

    
    private byte CacheLineSize;
    private byte RevisionId;
    private byte Status;
    
    private short XcvrType;
    private short DeviceId;
    private int BitsInHashFilter;
    private int LinkSpeed;
    private int UpdateInterval;
    
    private byte Connector;			 
    private byte ConfigConnector;    
    
    private boolean HurricaneEarlyRevision;
    private byte FeatureSet;
    
    private boolean OptimizeForThroughput;
    private boolean OptimizeForCPU;
    private boolean OptimizeNormal;
    
    private boolean BroadcastErrDone;
    private boolean UDPChecksumErrDone;
    private boolean FullDuplexEnable;
    private boolean DuplexCommandOverride;
    
    private boolean MWIErrDone;
    private boolean FlowControlEnable;
    private boolean FlowControlSupported;
    private boolean LinkBeatDisable;
    
    private boolean SupportsPowerManagement;
    private boolean WOLConnectorPresent;
    private boolean AutoResetToD0;
    private boolean DontSleep;
    private boolean D3Work;
    
    private boolean WakeOnMagicPacket;
    private boolean WakeOnLinkChange;
    
    private boolean SQEDisable;
    private boolean AutoSelect;
    private boolean LightTen;
    private byte LinkState;
    
    //
    // TryMII sets these parameters.
    //
    
    private short MIIReadCommand;
    private short MIIWriteCommand;
    private short MIIPhyOui;
    private short MIIPhyModel;
    private short MIIPhyUsed;
    private short MediaOverride;
    
    private short phys;	/* MII device addr. - for Becker's diag */
    
    // Zugriffsfunktionen

    public void set_CacheLineSize(byte cachelinesize) {
	CacheLineSize = cachelinesize;
    }
    public void set_RevisionId(byte revisionid) {
	RevisionId = revisionid;
    }
    public void set_Status(byte status) {
	Status = status;
    }
    public void set_XcvrType(short xcvrtype) {
	XcvrType = xcvrtype;
    }
    public void set_DeviceId(short deviceid) {
	DeviceId = deviceid;
    }
    public void set_BitsInHashFilter(int bitsinhashfilter) {
	BitsInHashFilter = bitsinhashfilter;
    }
    public void set_LinkSpeed(int linkspeed) {
	LinkSpeed = linkspeed;
    }
    public void set_UpdateInterval(int updateinterval) {
	UpdateInterval = updateinterval;
    }
    public void set_Connector(byte connector) throws UnknownConnectorType {
	if (!((connector >= 0 && connector < 7) || connector == 8 || connector == 9 || connector == (byte)0xFF))
	    throw new UnknownConnectorType();
	Connector = connector;
    }
    public void set_ConfigConnector(byte configconnector) throws UnknownConnectorType {
	if (!((configconnector >= 0 && configconnector < 7) || configconnector == 8 || 
	      configconnector == 9 || configconnector == (byte)0xFF))
	    throw new UnknownConnectorType();
	ConfigConnector = configconnector;
    }
    public void set_HurricaneEarlyRevision(boolean hurricaneearlyrevision) {
	HurricaneEarlyRevision = hurricaneearlyrevision;
    }
    public void set_FeatureSet(byte featureset) {
	FeatureSet = featureset;
    }
    public void set_OptimizeForThroughput(boolean optimizeforthroughput) {
	OptimizeForThroughput = optimizeforthroughput;
    }
    public void set_OptimizeForCPU(boolean optimizeforcpu) {
	OptimizeForCPU = optimizeforcpu;
    }
    public void set_OptimizeNormal(boolean optimizenormal) {
	OptimizeNormal = optimizenormal;
    }
    public void set_BroadcastErrDone(boolean broadcasterrdone) {
      BroadcastErrDone = broadcasterrdone;
    }
    public void set_UDPChecksumErrDone(boolean udpchecksumerrdone) {
	UDPChecksumErrDone = udpchecksumerrdone;
    }  
    public void set_FullDuplexEnable(boolean fullduplexenable) {
	FullDuplexEnable = fullduplexenable;
    }
    public void set_DuplexCommandOverride(boolean duplexcommandoverride) {
	DuplexCommandOverride = duplexcommandoverride;
    }
    public void set_MWIErrDone(boolean mwierrdone) {
	MWIErrDone = mwierrdone;
    }
    public void set_FlowControlEnable(boolean flowcontrolenable) {
	FlowControlEnable = flowcontrolenable;
    }
    public void set_FlowControlSupported(boolean flowcontrolsupported) {
	FlowControlSupported = flowcontrolsupported;
    }
    public void set_LinkBeatDisable(boolean linkbeatdisable) {
	LinkBeatDisable = linkbeatdisable;
    }
    public void set_SupportsPowerManagement(boolean supportspowermanagement) {
	SupportsPowerManagement = supportspowermanagement;
    }
    public void set_WOLConnectorPresent(boolean wolconnectorpresent) {
	WOLConnectorPresent = wolconnectorpresent;
    } 
    public void set_AutoResetToD0(boolean autoresettod0) {
	AutoResetToD0 = autoresettod0;
    }
    public void set_DontSleep(boolean dontsleep) {
	DontSleep = dontsleep;
    }
    public void set_D3Work(boolean d3work) {
	D3Work = d3work;
    }
    public void set_WakeOnMagicPacket(boolean wakeonmagicpacket) {
	WakeOnMagicPacket = wakeonmagicpacket;
    }
    public void set_WakeOnLinkChange(boolean wakeonlinkchange) {
	WakeOnLinkChange = wakeonlinkchange;
    }
    public void set_SQEDisable(boolean sqedisable) {
	SQEDisable = sqedisable;
    }
    public void set_AutoSelect(boolean autoselect) {
	AutoSelect = autoselect;
    }
    public void set_LightTen(boolean lightten) {
	LightTen = lightten;
    }
    public void set_LinkState(byte linkstate) throws UnknownLinkState {
	if (!(linkstate == 0 || linkstate == 1 || linkstate == 2)) 
	    throw new UnknownLinkState();
	LinkState = linkstate;
    }
    public void set_MIIReadCommand(short miireadcommand) {
	MIIReadCommand = miireadcommand;
    }
    public void set_MIIWriteCommand(short miiwritecommand) {
	MIIWriteCommand = miiwritecommand;
    }
    public void set_MIIPhyOui(short miiphyoui) {
	MIIPhyOui = miiphyoui;
    }
    public void set_MIIPhyModel(short miiphymodel) {
	MIIPhyModel = miiphymodel;
    }
    public void set_MIIPhyUsed(short miiphyused) {
	MIIPhyUsed = miiphyused;
    }
    public void set_MediaOverride(short mediaoverride) {
	MediaOverride = mediaoverride;
    }
    public void set_phys(short phys_arg) {
	phys = phys_arg;
    }
							 

    public byte get_CacheLineSize() {
	return CacheLineSize;
    }
    public byte get_RevisionId() {
	return RevisionId;
    }
    public byte get_Status() {
	return Status;
    }
    public short get_XcvrType() {
	return XcvrType;
    }
    public short get_DeviceId() {
	return DeviceId;
    }
    public int get_BitsInHashFilter() {
	return BitsInHashFilter;
    }
    public int get_LinkSpeed() {
	return LinkSpeed;
    }
    public int get_UpdateInterval() {
	return UpdateInterval;
    }
    public byte get_Connector() {
	return Connector;
    }
    public byte get_ConfigConnector() {
	return ConfigConnector;
    }
    public boolean get_HurricaneEarlyRevision() {
	return HurricaneEarlyRevision;
    }
    public byte get_FeatureSet() {
	return FeatureSet;
    }
    public boolean get_OptimizeForThroughput() {
	return OptimizeForThroughput;
    }
    public boolean get_OptimizeForCPU() {
	return OptimizeForCPU;
    }
    public boolean get_OptimizeNormal() {
	return OptimizeNormal;
    }
    public boolean get_BroadcastErrDone() {
	return BroadcastErrDone;
    }
    public boolean get_UDPChecksumErrDone() {
	return UDPChecksumErrDone;
    }  
    public boolean get_FullDuplexEnable() {
	return FullDuplexEnable;
    }
    public boolean get_DuplexCommandOverride() {
	return DuplexCommandOverride;
    }
    public boolean get_MWIErrDone() {
	return MWIErrDone;
    }
    public boolean get_FlowControlEnable() {
	return FlowControlEnable;
    }
    public boolean get_FlowControlSupported() {
	return FlowControlSupported;
    }
    public boolean get_LinkBeatDisable() {
	return LinkBeatDisable;
    }
    public boolean get_SupportsPowerManagement() {
	return SupportsPowerManagement;
    }
    public boolean get_WOLConnectorPresent() {
	return WOLConnectorPresent;
    } 
    public boolean get_AutoResetToD0() {
	return AutoResetToD0;
    }
    public boolean get_DontSleep() {
	return DontSleep;
    }
    public boolean get_D3Work() {
	return D3Work;
    }
    public boolean get_WakeOnMagicPacket() {
	return WakeOnMagicPacket;
    }
    public boolean get_WakeOnLinkChange() {
	return WakeOnLinkChange;
    }
    public boolean get_SQEDisable() {
	return SQEDisable;
    }
    public boolean get_AutoSelect() {
	return AutoSelect;
    }
    public boolean get_LightTen() {
	return LightTen;
    }
    public byte get_LinkState() {
	return LinkState;
    }
    public short get_MIIReadCommand() {
	return MIIReadCommand;
    }
    public short get_MIIWriteCommand() {
	return MIIWriteCommand;
    }
    public short get_MIIPhyOui() {
	return MIIPhyOui;
    }
    public short get_MIIPhyModel() {
	return MIIPhyModel;
    }
    public short get_MIIPhyUsed() {
	return MIIPhyUsed;
    }
    public short get_MediaOverride() {
	return MediaOverride;
    }
    public short get_phys() {
	return phys;
    }
}


