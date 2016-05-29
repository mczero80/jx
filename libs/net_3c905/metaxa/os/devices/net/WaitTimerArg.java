package metaxa.os.devices.net;

class WaitTimerArg {

    private NicInformation Info;
    private DpdListEntry TestDPD;
    private int TimeOut;
      
    public WaitTimerArg(NicInformation info, DpdListEntry test, int timeout) {
	Info = info;
       	TestDPD = test;
	TimeOut = timeout;
    }
    
    public NicInformation get_NicInfo() {
	return Info;
    }
      public DpdListEntry get_TestDPD() {
	return TestDPD;
    }
    public int get_TimeOut() {
	return TimeOut;
    }
 
}
