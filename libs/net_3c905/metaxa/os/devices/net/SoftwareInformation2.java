package metaxa.os.devices.net;

class SoftwareInformation2 {

    private boolean BroadcastRxErrDone;
    private boolean EncoderDecoderLoopBackErrDone;
    private boolean WOLConnectorPresent;
    private boolean PMEPulsed;
    private boolean MWIErrDone;
    private boolean AutoResetToD0;
    private boolean D3Work;

    public SoftwareInformation2(boolean broadcastrxerrdone, boolean encoderdecoderloopbackerrdone, boolean wolconnectorpresent, 
                         boolean pmepulsed, boolean mwierrdone, boolean autoresettod0, boolean d3work) {
	set_BroadcastRxErrDone(broadcastrxerrdone);
	set_EncoderDecoderLoopBackErrDone(encoderdecoderloopbackerrdone);
	set_WOLConnectorPresent(wolconnectorpresent);
	set_PMEPulsed(pmepulsed);
	set_MWIErrDone(mwierrdone);
	set_AutoResetToD0(autoresettod0);
	set_D3Work(d3work);
    }

    public SoftwareInformation2() {}

    public void set_BroadcastRxErrDone(boolean broadcastrxerrdone) {
	BroadcastRxErrDone =  broadcastrxerrdone;
    }
    public void set_EncoderDecoderLoopBackErrDone(boolean encoderdecoderloopbackerrdone) {
	 EncoderDecoderLoopBackErrDone = encoderdecoderloopbackerrdone;
    }
    public void set_WOLConnectorPresent(boolean wolconnectorpresent) {
	 WOLConnectorPresent = wolconnectorpresent;
    }
    public void set_PMEPulsed(boolean pmepulsed) {
	 PMEPulsed = pmepulsed;
    }	
    public void set_MWIErrDone(boolean mwierrdone) {
	 MWIErrDone = mwierrdone;
    }
    public void set_AutoResetToD0(boolean autoresettod0) {
	AutoResetToD0 = autoresettod0;
    }
    public void set_D3Work(boolean d3work) {
	D3Work = d3work;
    }
    
    public boolean get_BroadcastRxErrDone() {
	return BroadcastRxErrDone;
    }
    public boolean get_EncoderDecoderLoopBackErrDone() {
	return EncoderDecoderLoopBackErrDone;
    }
    public boolean get_WOLConnectorPresent() {
	return WOLConnectorPresent;
    }
    public boolean get_PMEPulsed() {
	 return PMEPulsed;
    }	
    public boolean get_MWIErrDone() {
	return MWIErrDone;
    }
    public boolean get_AutoResetToD0() {
	return AutoResetToD0;
    }
    public boolean get_D3Work() {
	return D3Work;
    }
    
} 
