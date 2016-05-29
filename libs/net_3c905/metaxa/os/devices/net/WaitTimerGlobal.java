package metaxa.os.devices.net;

// values set by various function all needed by the WaitTimer-Method
// singleton pattern

class WaitTimerGlobal {

    private int portValue;
    private boolean DCConverterEnabledState;
    private short MediaStatus;
    private boolean PhyResponding;
    private short PhyStatus;
    private int DownListPointer;
    private int UpListPointer;
    private int dmaControl;
  
    private static WaitTimerGlobal instance = new WaitTimerGlobal();            

    private WaitTimerGlobal() {
	MediaStatus = 0;
    }
    
    public static WaitTimerGlobal instance() {
	if (instance==null) instance = new WaitTimerGlobal();
        return instance;
    }        
    public boolean get_DCConverterEnabledState() {
	return DCConverterEnabledState;
    }
    public short get_MediaStatus() {
	return MediaStatus;
    }
    public boolean get_PhyResponding() {
	return PhyResponding;
    }
    public short get_PhyStatus() {
	return PhyStatus;
    }
    public int get_DownListPointer() {
	return DownListPointer;
    }
    public int get_UpListPointer() {
	return UpListPointer;
    }
    public int get_PortV() {
	return portValue;
    }
    public int get_dmaControl() {
        return dmaControl;
    }

    public void set_DCConverterEnabledState(boolean dc) {
	DCConverterEnabledState = dc;
    }
    public void set_MediaStatus(short media) {
       MediaStatus = media;
    }
    public void set_PhyResponding(boolean phyres) {
	PhyResponding = phyres;
    }
    public void set_PhyStatus(short phystat) {
	PhyStatus = phystat;
    }
    public void set_DownListPointer(int dlp) {
	DownListPointer = dlp;
    }
    public void set_UpListPointer(int ulp) {
	UpListPointer = ulp;
    }
    public void set_PortV(int port) {
	portValue = port;
    }
    public void set_dmaControl(int dma) {
        dmaControl = dma;
    }
}
