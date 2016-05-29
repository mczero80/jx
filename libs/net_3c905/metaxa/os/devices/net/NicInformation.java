package metaxa.os.devices.net;
import java.util.*;
import metaxa.os.*;
import metaxa.os.devices.*;
import jx.devices.pci.PCIDevice;

class NicInformation extends WaitCases {

    static final int MAXIMUM_TEST_BUFFERS = 1;
    
    int IoBaseAddress;
    // for memory-mapped access to registers
    int MemBaseAddress;
    int irq;
  
    byte[] DeviceName;
    byte[] PermanentAddress;
    byte[] StationAddress;
    int ResourcesReserved;
    NicPciInformation PCI;
    
    // Upload list pointer 
    UpdListEntry HeadUPD;


    // Download list pointer
    DpdListEntry HeadDPD;
    DpdListEntry TailDPD;
    DpdListEntry TestDPD;
   
    Resourcen Resources;
    NicStatistics Statistics;
    NicHardwareInformation Hardware;
    int BytesInDPDQueue;
    //  PCIDevInfo pcidev;	 
    boolean InTimer;
    boolean DelayStart = true; // delay after init (needed by some switches)
 
    // MultiCast List - implemented by a Vector
    Vector mc_list = new Vector();
    
    // lock for NicTimer
    Object lock;
    boolean DPDRingFull;
    boolean DeviceGivenByOS;
    
    int keepForGlobalReset;	
    int WaitCases;

    public NicInformation(PCIDevice pcidevice) {
	DeviceName = new byte[8];
	PermanentAddress = new byte[6];
	StationAddress = new byte[6];
	Resources = new Resourcen();
	Statistics = new NicStatistics();
	Hardware = new NicHardwareInformation();
	PCI = new NicPciInformation(pcidevice);
	lock = new Object();
    }
}
