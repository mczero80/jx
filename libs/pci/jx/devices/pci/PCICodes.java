package jx.devices.pci;

public class PCICodes {
   public final static PCIClassCode baseclass_code[] = {
	   new PCIClassCode(0x00, "pre PCI 2.0 device"),
	   new PCIClassCode(0x01, "Mass storage controller"),
	   new PCIClassCode(0x02, "Network controller"),
	   new PCIClassCode(0x03, "Display controller"),
	   new PCIClassCode(0x04, "Multimedia device"),
	   new PCIClassCode(0x05, "Memory Controller"),
	   new PCIClassCode(0x06, "Bridge Device"),
	   new PCIClassCode(0x07, "Simple communications controllers"),
	   new PCIClassCode(0x08, "Base system peripherals"),
	   new PCIClassCode(0x09, "Inupt devices"),
	   new PCIClassCode(0x0a, "Docking Stations"),
	   new PCIClassCode(0x0B, "Processors"),
	   new PCIClassCode(0x0C, "Serial bus controllers"),
   };
   public final static PCIClassCode class_code[][] = {
       // 00 pre PCI 2.0 device
       {  new PCIClassCode(0x01, "VGA device"),
       }, // 01 Mass storage controller
       { new PCIClassCode(0x00, "SCSI Controller"),
	 new PCIClassCode(0x01, "IDE controller"),
	 new PCIClassCode(0x02, "Floppy disk controller"),
	 new PCIClassCode(0x03, "IPI controller"),
	 new PCIClassCode(0x04, "RAID controller"),
       }, // 02 Network controller
       { new PCIClassCode(0x00, "Ethernet controller"),
	 new PCIClassCode(0x01, "Token Ring network controller"),
	 new PCIClassCode(0x02, "FDDI network controller"),
	 new PCIClassCode(0x03, "ATM network controller"),
       }, // 03 Display controller
       { new PCIClassCode(0x00, "VGA compatable controller"),
	 new PCIClassCode(0x01, "XGA controller"),
       }, // 04 Multimedia device
       { new PCIClassCode(0x00, "Video device"),
	 new PCIClassCode(0x01, "Audio device"),
       }, // 05 Memory Controller
       { new PCIClassCode(0x00, "RAM controller"),
	 new PCIClassCode(0x01, "Flash memory controller"),
       }, // 06 Bridge Device
       { new PCIClassCode(0x00, "Host/PCI bridge"),
	 new PCIClassCode(0x01, "PCI/ISA bridge"),
	 new PCIClassCode(0x02, "PCI/EISA bridge"),
	 new PCIClassCode(0x03, "PCI/Micro Channel bridge"),
	 new PCIClassCode(0x04, "PCI/PCI bridge"),
	 new PCIClassCode(0x05, "PCI/PCMCIA bridge"),
	 new PCIClassCode(0x06, "PCI/NuBus bridge"),
	 new PCIClassCode(0x07, "PCI/CardBus bridge"),
       }, // 07 Simple communications controllers
       { new PCIClassCode(0x00, "Serial controller"),
	 new PCIClassCode(0x01, "Parallel port"),
       }, // 08 Base system peripherals
       { new PCIClassCode(0x00, "programmable interrupt controller (PIC)"),
	 new PCIClassCode(0x01, "DMA controller"),
	 new PCIClassCode(0x02, "system timer"),
	 new PCIClassCode(0x03, "RTC controller"),
       }, // 09 Inupt devices
       { new PCIClassCode(0x00, "Keyboard controller"),
	 new PCIClassCode(0x01, "Digitizer (pen)"),
	 new PCIClassCode(0x02, "Mouse controller"),
       }, // 0a Docking Stations
       { new PCIClassCode(0x00, "Generic docking station"),
       }, // 0B Processors
       { new PCIClassCode(0x0, ""),
	 new PCIClassCode(0x00, "386"),
	 new PCIClassCode(0x01, "486"),
	 new PCIClassCode(0x02, "Pentium"),
	 new PCIClassCode(0x10, "Alpha"),
	 new PCIClassCode(0x20, "PowerPC"),
	 new PCIClassCode(0x40, "Co-Processor"),
       }, // 0C Serial bus controllers
       { new PCIClassCode(0x00, "Firewire (IEEE 1394)"),
	 new PCIClassCode(0x01, "ACCESS bus"),
	 new PCIClassCode(0x02, "SSA (Serial Storage Architecture)"),
	 new PCIClassCode(0x03, "USB (Universal Serial Bus)"),
       },
   };
 

   public final static PCIVendorCode vendor_code[] = {
      new PCIVendorCode(0x0070, "Hauppauge Computer Works Inc."),
      new PCIVendorCode(0x8086, "Intel Corporation"),
      new PCIVendorCode(0x109e, "Brooktree"),
      new PCIVendorCode(0x10b7, "3Com"),
      new PCIVendorCode(0x102b, "Matrox"),
      new PCIVendorCode(0x1022, "AMD"),
      new PCIVendorCode(0x1106, "VIA Technologies Inc."),
      new PCIVendorCode(0x15ad, "VMWare"),
   };
   public final static PCIDeviceCode device_code[] = {
      new PCIDeviceCode(0x8086, 0x7100, "82439TX", 	"System Controller (MTXC)"),
      new PCIDeviceCode(0x8086, 0x7190, "82443BX/ZX", 	"440BX/ZX AGPset Host Bridge"),
      new PCIDeviceCode(0x8086, 0x7191, "82443BX/ZX", 	"440BX/ZX AGPset PCI-to-PCI bridge"),
      new PCIDeviceCode(0x8086, 0x7110, "82371AB",	"PIIX4 ISA Bridge"),
      new PCIDeviceCode(0x8086, 0x7111, "82371AB",	"PIIX4 IDE Controller"),
      new PCIDeviceCode(0x8086, 0x7112, "82371AB",	"PIIX4 USB Interface"),
      new PCIDeviceCode(0x8086, 0x7113, "82371AB",	"PIIX4 Power Management Controller"),
      new PCIDeviceCode(0x8086, 0x1229, "82557",	"Fast Ethernet LAN Controller (used on EEPRO/100B 10/100 Adapter)"),
      new PCIDeviceCode(0x10b7, 0x9055, "3C905B", 	"Fast Etherlink XL 10/100"),
      new PCIDeviceCode(0x102b, 0x0521, "MGA-G200", 	"Millennium/Mystique G200 AGP (Eclipse/Calao)"),
      new PCIDeviceCode(0x102b, 0x0525, "MGA-G400/450", "Millennium G400/450 (Toucan/Condor)"),
      new PCIDeviceCode(0x109e, 0x036e, "bt878",	"Brooktree 878 Framegrabber/TV Card"),
      new PCIDeviceCode(0x109e, 0x0878, "bt878 Audio",	"Brooktree 878 Framegrabber/TV Card, audio function"),
      new PCIDeviceCode(0x109e, 0x0350, "bt848",	"Brooktree 848 Framegrabber/TV Card"),
      new PCIDeviceCode(0x15ad, 0x0710, "VirtualSVGA",	"VirtualSVGA"),
      new PCIDeviceCode(0x1022, 0x2000, "Am79C970/1/3/5",	"PCnet PCI Ethernet Controller"),
      new PCIDeviceCode(0x1106, 0x3091, "VT8633",	"CPU to PCI Bridge (Apollo Pro 266 chipset)"),
      new PCIDeviceCode(0x1106, 0xB091, "VT8633",	"PCI-to-PCI Bridge (AGP) (Apollo Pro266 chipset)"),
      new PCIDeviceCode(0x1106, 0x3074, "VT8233",	"PCI to ISA Bridge"),
      new PCIDeviceCode(0x1106, 0x0571, "VT82C586/596/686",	"PCI IDE Controller"),
      new PCIDeviceCode(0x1106, 0x3038, "VT83C572",	"PCI USB Controller"),
      new PCIDeviceCode(0x1106, 0x3059, "VT8233",	"AC97 Enhanced Audio Controller"),
   };

   public static String lookup(int id){
      return lookup( (id & PCI.DEVICE_MASK) >> PCI.DEVICE_SHIFT,
		    (id & PCI.VENDOR_MASK) >> PCI.VENDOR_SHIFT);
   }
   
   public static String lookup(int deviceID, int vendorID){
      return lookup((short)deviceID, (short)vendorID);
   }
   
   public static String lookup(short deviceID, short vendorID){
      for(int i=0; i<device_code.length; ++i){
	 if(device_code[i].deviceID == deviceID &&
	    device_code[i].vendorID == vendorID )
	   return "Chip="+device_code[i].chipNumber + '(' +device_code[i].description + ')';
      }
      return "device 0x"+Integer.toHexString(deviceID) + " / " + lookupVendor(vendorID);
   }
   
   public static String lookupVendor(int vendorID){
      return lookupVendor((short)vendorID);
   }
   
   public static String lookupVendor(short vendorID){
      for(int i=0; i<vendor_code.length; ++i){
	 if( vendor_code[i].vendorID == vendorID )
	   return vendor_code[i].vendor;
      }
      return "vendor 0x"+Integer.toHexString(vendorID);
   }
    
    public static String lookupClass (int classID) {
	String result = "class 0x"+Integer.toHexString(classID);
	for(int j=0; j<baseclass_code.length; ++j)
	    if( baseclass_code[j].classID == (classID & 0xff0000) >> 16 ) {
		result =  baseclass_code[j].className;
		for(int i=0; i<class_code[j].length; ++i)
		    if( class_code[j][i].classID == (classID & 0xff00) >> 8 ) 
			result =  class_code[j][i].className;
	    }
	
	return result;
    }
}

class PCIClassCode {
   int classID;
   String className;
   
   PCIClassCode(int classID, String className){
      this.classID	= classID;
      this.className	= className;
   }
}

class PCIVendorCode {
   short vendorID;
   String vendor;
   
   PCIVendorCode(short vendorID, String vendor){
      this.vendorID	= vendorID;
      this.vendor	= vendor;
   }
   
   PCIVendorCode(int vendorID, String vendor){
      this((short)vendorID, vendor);
   }
   
}

class PCIDeviceCode {
   short vendorID;
   short deviceID;
   String chipNumber;
   String description;

   PCIDeviceCode(short vendorID, short deviceID, String chipNumber, String description){
      this.vendorID = vendorID;
      this.deviceID = deviceID;
      this.chipNumber = chipNumber;
      this.description = description;
   }
   PCIDeviceCode(int vendorID, int deviceID, String chipNumber, String description){
      this((short)vendorID, (short)deviceID, chipNumber, description);
   }
}

