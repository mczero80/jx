package jx.devices.pci;

import jx.zero.Naming;
import jx.zero.CPUManager;
import jx.zero.Ports;
import jx.zero.Debug;
import jx.zero.debug.*;
import jx.zero.debug.DebugPrintStream;
import java.util.Vector;
import jx.zero.Service;
import jx.zero.InitialNaming;

/*
 * This class is *not* a PCIDevice, because it implements only the
 * "CPU-side" of the PCI-Bus. The "PCI side" is a PCIDevice which is
 * registered by this class as any other PCI device.
 * 
 * Note: The classname is a suggestion of Michael Golm. ;-]
 */
public class PCIGod implements PCIAccess, PCIHB, PCI, Service {
   Naming naming;
   Ports ports;
   Vector devices = new Vector();
   
   /********************************************************************/
   
   public static void main(String[] args){
       Naming naming =    InitialNaming.getInitialNaming();
 
       Debug.out.println("Domain PCI speaking.");
      
      // init PCI bus
      PCIGod instance = new PCIGod(naming);
      
      // promote as DEP
      final Naming dz = naming;
      final PCIAccess depHandle = instance;
      
      // register as DEP
      naming.registerPortal(depHandle, "PCIAccess");
      Debug.out.println("PCIAccess registered");
   }
   
   PCIGod(Naming naming){
      this.naming = naming;
      Debug.assert(naming != null, "naming must be valid");
      
      ports = (Ports)naming.lookup("Ports");
      Debug.assert(ports != null, "'Ports' portal not found");
      
      if( !probePCI() )
	throw new Error("no PCI Bus detected");
      
      scanBus();
//      dumpDevices();
   }
   
   /********************************************************************/
   /* initialisation & device searching                                */
   /********************************************************************/
   
   private boolean probePCI() {
      int old = ports.inl_p(CONFIG_ADDRESS);
      if ((old & CONFIG_ENABLE_MASK) != 0) 
	return false;
      
      Debug.out.println("Success reading PCI configuration port");
      
      ports.outl_p(CONFIG_ADDRESS, ECD_MASK);
      ports.outb_p(CONFIG_ADDRESS + 3, (byte)0);
      int mode1res = ports.inl_p(CONFIG_ADDRESS);
      ports.outl_p(CONFIG_ADDRESS, old);
      
      if (mode1res != 0) {
	 Debug.out.println("now checking pci bus");
	 if (lookForDevices()) return true;;
	 Debug.out.println("found no devices on this pci bus");
      }
      
      ports.outl_p(CONFIG_ADDRESS, CONF1_ENABLE_CHK1);
      mode1res = ports.inl_p(CONFIG_ADDRESS);
      ports.outl_p(CONFIG_ADDRESS, old);
      
      if ((mode1res & CONF1_ENABLE_MSK1) == CONF1_ENABLE_RES1) {
	 Debug.out.println("now checking pci bus #2");
	 if (lookForDevices())   return true;
      }
      
      return false;
   }
   
   /*
    * Try to find at least one device on the PCI bus.
    */
   private boolean lookForDevices () {
      for(byte device = 0; device < MAX_PCI_AGENTS; device++) {
	 int id = readDeviceConfig(createAddress(0, device, 0, 0));
	 if( id != INVALID_ID )
	   return true;
      }
      return false;
   }
   
   
   public void scanBus() {
      PCIDevice pcidev;
      int num_bus = 1;
      for (byte bus = 0; bus < num_bus; ++bus) {
	 for (byte device = 0; device < MAX_PCI_AGENTS; ++device) {
	    int num_func = MAX_PCI_FUNCTIONS;
	    for(int function = 0; function < num_func; ++function){
	       PCIAddress pciaddr = new PCIAddress(bus, device, function);
	       
	       int id = readDeviceConfig(pciaddr, REG_DEVVEND);
	       if ( id == INVALID_ID )
		 break;
	       
	       pcidev = new PCIDevice(this, pciaddr);
	       
	       int misc = pcidev.getHeaderType();
	       if (function == 0 && (misc & PCI.HEADER_MULTIFUNCTION) == 0)
		 num_func = 1;
	    
	       int classCode = pcidev.getClassCode();
	       int mainclass = (classCode & 0xff0000) >> 16;
	       if( mainclass == 0x00 || mainclass == 0xff )
		 continue;
	       
	       // if PCI-PCI bridge, increment bus count 
	       if ((classCode & ~CLASSCODE_PIF_MASK) == CLASSCODE_PCI_BRIDGE ){
		  Debug.out.println("PCI bridge found: bus="+ bus +", device="+device+", function="+function);
		  ++num_bus;
	       }
	       devices.addElement(pcidev);
	    }
	 }
      }
   } 
   
   public void dumpDevices() {
      Debug.out.println("Devices:");
      for(int i=0; i<devices.size(); ++i){
	 PCIDevice dev = (PCIDevice)devices.elementAt(i);
	 int irq = dev.getInterruptLine();
	 Debug.out.println(dev.getAddress().toString() + ": " +
			   " (INT " + irq + ")" +
			   " Class: " + PCICodes.lookupClass(dev.getClassCode())  );
	Debug.out.println("               "+PCICodes.lookup(dev.readConfig(REG_DEVVEND)) ); 

      }
   } 
   
   /********************************************************************/
   /* internal read/write operations with direct support methods */
   
   private static int createAddress(PCIAddress pciaddress, int register) {
      return createAddress(pciaddress.bus, pciaddress.device, pciaddress.function, register);
   }
   
   private static int createAddress(int bus, int device, int function, int register) {
      Debug.assert(device < MAX_PCI_AGENTS, "device number out of range");
      return ECD_MASK |
	((bus      << BUS_OFFSET_BIT) & BUS_MASK) |
	((device   << DEV_OFFSET_BIT) & DEV_MASK) |
	((function << FUN_OFFSET_BIT) & FUN_MASK) |
	((register << REG_OFFSET_BIT) & REG_MASK);
   }
   
   private int readDeviceConfig(int address){
      ports.outl_p(CONFIG_ADDRESS, address);
      int data = ports.inl_p(CONFIG_DATA);
      ports.outl_p (CONFIG_ADDRESS, 0);
      return data;
   }
   
   private void writeDeviceConfig(int address, int value){
      ports.outl(CONFIG_ADDRESS, address);
      ports.outl(CONFIG_DATA, value);
      ports.outl (CONFIG_ADDRESS, 0);
   }
   
   /********************************************************************/
   /* public interface for PCIAccess                                   */
   /********************************************************************/
   
   public int getNumberOfDevices() {
      return devices.size();
   }

   public PCIDevice getDeviceAt(int index) {
      return (PCIDevice)devices.elementAt(index);
   }
   
   public PCIDevice[] getDevicesByID(short vendorID, short deviceID){
      boolean compareVID = (vendorID == 0xffff || vendorID == 0x0000)? false : true;
      boolean compareDID = (deviceID == 0xffff || deviceID == 0x0000)? false : true;
      
      PCIDevice dev;
      Vector v = new Vector(devices.size());
      for(int i=0; i<devices.size(); ++i){
	 dev = (PCIDevice)devices.elementAt(i);
	 if( compareVID && vendorID != dev.getVendorID() )
	   continue;
	 if( compareDID && deviceID != dev.getDeviceID() )
	   continue;
	 v.addElement(dev);
      }
      return DevVecToArray(v);
   }
   
   public PCIDevice[] getDevicesByClass(int mask, int classcode){
      PCIDevice dev;
      Vector v = new Vector(devices.size());
      for(int i=0; i<devices.size(); ++i){
	 dev = (PCIDevice)devices.elementAt(i);
	 if( (dev.getClassCode() & mask) == classcode )
	   v.addElement(dev);
      }
      return DevVecToArray(v);
   }
   
   public int readDeviceConfig(PCIAddress devaddr, int reg){
      return readDeviceConfig(createAddress(devaddr, reg));
   }
   
   public void writeDeviceConfig(PCIAddress devaddr, int reg, int value){
      writeDeviceConfig(createAddress(devaddr, reg), value);
   }
   
   /********************************************************************/
   
   PCIDevice[] DevVecToArray(Vector v){
      // Note: for jdk >= 1.2 use this:
      // return (PCIDevice [])v.toArray();
      PCIDevice a[] = new PCIDevice[v.size()];
      for(int i=0; i<a.length; ++i)
	a[i] = (PCIDevice)v.elementAt(i);
      return a;
   }
}


/********************************************************************/

// define constants related to PCIGod
interface PCIHB {
    /*
    * The registers (ports) CONFIG_ADDRESS and CONFIG_DATA
    * are used to access the PCI configuration space.
    */
   int CONFIG_ADDRESS		= 0x0cf8;
   int CONFIG_DATA		= 0x0cfc;
   
   int CONFIG_ENABLE_CHECK	= 0x80000000;
   int CONFIG_ENABLE_MASK	= 0x7ff00000;
   
   int CONF1_ENABLE_CHK1	= 0xff000001;
   int CONF1_ENABLE_MSK1	= 0x80000001;
   int CONF1_ENABLE_RES1	= 0x80000000;
   
   int BUS_OFFSET_BIT		= 16;
   int DEV_OFFSET_BIT		= 11;
   int FUN_OFFSET_BIT		= 8;
   int REG_OFFSET_BIT		= 2;
   
   int ECD_MASK			= 0x80000000; // Enable CONFIG_DATA
   int BUS_MASK			= 0x00ff0000;
   int DEV_MASK			= 0x0000f800;
   int FUN_MASK			= 0x00000700;
   int REG_MASK			= 0x000000fc;

   int MAX_PCI_FUNCTIONS	= 8;
   int MAX_PCI_AGENTS		= 32;
   int MAX_PCI_BUSSES		= 256;
   
   int CLASSCODE_PCI_BRIDGE	= 0x060400;
   
   int INVALID_ID		= 0xffffffff;
}
