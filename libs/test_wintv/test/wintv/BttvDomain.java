package test.wintv;
import jx.zero.*;
import jx.zero.debug.*;
import jx.wintv.*;
import jx.devices.pci.*;

public class BttvDomain {
    static Naming naming = InitialNaming.getInitialNaming();
   public static void main(String [] args){
       String framegrabberName = args[0];
       Debug.out.println("Domain BTTV-Test speaking.");
      try{
	 Debug.out.println("lookup PCI Access Point...");
	 PCIAccess bus = (PCIAccess)naming.lookup("PCIAccess");
	 if( bus == null )
	   Debug.assert( bus != null, "found no PCI Access Point" );
	 
	 findFramegrabber(bus, framegrabberName);
      }
      catch(Throwable t){
	 Debug.out.println("test.wintv.Main: caught somthing: " + t);
	 Debug.out.println("message is: " + t.getMessage());
      }
      
      Debug.out.println("test.wintv.BttvDomain: exit initial thread");
   }
   
   static void findFramegrabber(PCIAccess bus, String framegrabberName){
      Debug.out.println("scanning PCIBus for Brooktree devices...");
      
      // Hauppauge WinTV 
      PCIDevice wintvDevice  = WinTV.findDevice(bus);
      if( wintvDevice != null ){
	 Debug.out.println("Found a Hauppauge WinTV device.");
	 WinTV wintv = new WinTV(bus, wintvDevice);
	 makePortal(wintv, framegrabberName);
	 
	 makePortal((IRReceiverImpl)wintv.getRC(), "WinTV-IRReceiver");
	 return;
      }
	 
      // Miro TV
      PCIDevice mirotvDevice = MiroTV.findDevice(bus);
      if( mirotvDevice != null ){
	 Debug.out.println("Found a Miro TV device.");
	 MiroTV mirotv = new MiroTV(bus, mirotvDevice);
	 makePortal(mirotv, framegrabberName);
	 return;
      }
      
      // FlyTV
      PCIDevice flytvDevice = FlyTV.findDevice(bus); 
      if( flytvDevice != null ){
	 Debug.out.println("Found a FlyTV device");
	 FlyTV flytv = new FlyTV(bus, flytvDevice);
	 makePortal(flytv, framegrabberName);
	 return;
      }
      
      throw new Error("Domain BTTV: found no framegrabber! PANIC!");
   }
   
   static Portal makePortal(Portal impl, String id){
      Debug.assert(impl != null, "makePortal: need DEP instance");
      Debug.out.println("Bttv: Register portal "+id);
      if( id != null )
	naming.registerPortal(impl, id);
      return impl;
   }
}

