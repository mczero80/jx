package jx.wintv;

import jx.zero.*;
import jx.zero.debug.*;
import jx.devices.pci.*;

/*
 * WinTV PCI FM
 * 
 * InputSources:
 * 
 *     The tuner (FM1216 compatible) is connected to MUX0. The SVideo input is
 *     connected to both MUX2 and MUX3.
 * 
 * Clock generator:
 * 
 *     A NTSC * 8 clock generator is connected to XT0.
 * 
 */

public class WinTV extends GenericBT878 implements/* CaptureDevice,*/ Service {
   // I2C stuff
   BT878I2CBus	i2cbus	= null;
   I2CEEPROM	eeprom	= null;
   IRReceiver	ir	= null;
   Tuner	tuner	= null;
   
   // other InputSources
   InputSource chinch	= null;
   SVideo svideo	= null;
   InputSource colorBars= null;
   
   // all InputSources
   InputSource inputSources[] = null;
   
   
   /* debugging options */
   boolean dumpEEPROM = false;
   
   
   /********************************************************************/
   /* Initialization & Setup                                           */
   /********************************************************************/
   
   /**
    * Search a Hauppauge WinTV board on the given PCI bus.
    */
   
   public static PCIDevice findDevice(PCIAccess pcibus){
      WinTV wintv = null;
      PCIDevice dev;
      int devc = pcibus.getNumberOfDevices();
      for(int devindex=0; devindex<devc; ++devindex){
	 dev = pcibus.getDeviceAt(devindex);
	 if( isWinTV(dev) )
	   return dev;
      }
      return null;
   }
   
   /** 
    * Check whether a PCI device is a Hauppauge WinTV board or not.
    */
   public static boolean isWinTV(PCIDevice pcidev){
      
      // check VendorID (Brooktree 0x109e) and DeviceID (0x036[ef])
      if( pcidev.getVendorID() != PCI.VENDOR_BROOKTREE )
	return false;
      
      switch( pcidev.getDeviceID() ){
       case PCI.BROOKTREE_BT878:
       case PCI.BROOKTREE_BT879:
	 break;
       default:
	 return false;
      }

      // check SubvendorID (Hauppauge)
      short subsystemID = pcidev.getSubsystemID();
      short subsystemVendorID = pcidev.getSubsystemVendorID();
      
      // Debug.out.println("isWinTV: subvendor: 0x"+Integer.toHexString(subsystemVendorID)+", subsystem: 0x"+Integer.toHexString(subsystemID));
      
      if( subsystemVendorID != PCI.VENDOR_HAUPPAUGE )
	return false;
      
      switch( subsystemID ){
       case 0x13eb:			// WinTV@dune; WinTV@inf4
	 break;
       default:
	 return false;
      }
      
      return true;
   }
   
   /**
    * Initialize internal stuff, setup PCI subsystem of the WinTV board.
    */
   public WinTV(PCIAccess pcibus, PCIDevice pcidevice){
      super(pcidevice);

      if( !isWinTV(pcidevice) ){
	 Debug.out.println("ERROR: Device is not a WinTV card!");
	 throw new Error("Device is not a WinTV card!");
      }
      Debug.out.println("WinTV: call super.initPCI");
      super.initPCI();
      
      
      /**********************/
      /* disable function 1 */
      /**********************/
      
      PCIAddress audioAddr = pcidevice.getAddress().getSubfunction(1);
      PCIDevice  audioDev  = null;
      
      for(int i=0; i < pcibus.getNumberOfDevices(); ++i){
	 audioDev = pcibus.getDeviceAt(i);
	 if( audioAddr.equals(audioDev.getAddress()) )
	   break;
      }
      
      if( audioDev != null ){
	 Debug.out.println("disabling audio device...");
	 audioDev.setCommand((short)0x0);
      }
      else 
	Debug.out.println("strange: could not find bt878 audio device...");
      
      
      /**************************/
      /* initialize other stuff */
      /**************************/
      
      // initialize I2C Bus
      BT878I2CBus tmpbus = new BT878I2CBus(bt);
      tmpbus.lock();
      
      i2cbus = tmpbus;
      Throwable e = null; // FIXME: finally crashes
      try {
	 
	 i2cbus.scanBus();
	 
	 int i2cid = 0xa0;
	 if( i2cbus.probe(i2cid) ){
	    Debug.out.println("found EEPROM at I2C("+hex((byte)i2cid)+")");
	    eeprom = new I2CEEPROM(i2cbus, i2cid);
	 }
	 
	 i2cid = 0x34;
	 if( i2cbus.probe(i2cid) ){
	    Debug.out.println("found external IR receiver at I2C("+hex((byte)i2cid)+")");
	    ir = new IRReceiverImpl(i2cbus, i2cid, RCKeys.keytable);
	 } 
	 else {
	    i2cid = 0x30;
	    if( i2cbus.probe(i2cid) ){
	       Debug.out.println("found internal IR receiver at I2C("+hex((byte)i2cid)+")");
	       ir = new IRReceiverImpl(i2cbus, i2cid, RCKeys.keytable);
	    }
	 }
	 
	 i2cid = 0xc2;
	 if( i2cbus.probe(i2cid) ){
	    Debug.out.println("found Tuner at I2C("+hex((byte)i2cid)+")");
	    tuner = (Tuner)makeInputSourcePortal(new FM1216(bt, bt.MUX0, i2cbus, i2cid), 
						 "jx/wintv/Tuner", "Tuner");
	 }
      } catch(Throwable t) {
	  e = t;
      }

      i2cbus.unlock();
      if (e!=null) throw new Error(e.toString());
      
      if( eeprom != null && dumpEEPROM )
	eeprom.dump(Debug.out, 256);

      // initialize other InputSources
      chinch = (InputSource)makeInputSourcePortal(new MUXPin(bt, bt.MUX2), 
						  "jx/wintv/InputSource", "Chinch");
      
      svideo = (SVideo)makeInputSourcePortal(new SVideoImpl(bt, bt.MUX3), 
					     "jx/wintv/SVideo", "SVideo");
      colorBars = (InputSource)makeInputSourcePortal(new ColorBars(bt), 
						     "jx/wintv/InputSource", "ColorBars");
      
      inputSources = new InputSource[] {
	 tuner,
	   chinch,
	   svideo,
	   colorBars
      };
      
      if( pciCheck() )
	Debug.out.println("PCI check: Ok");
      else
	Debug.out.println("PCI check: FAILED!");
   }
   
   
   /********************************************************************/
   /* Public Scratch Area                                              */
   /********************************************************************/
   
   public IRReceiver getRC(){
      return ir;
   }
   
   /********************************************************************/
   /* public interface                                                 */
   /********************************************************************/
   
   public InputSource[] getInputSources(){
      return inputSources;
   }
   
   public InputSource getCurrentInputSource(){
      for(int i=0; i<inputSources.length; ++i)
	if( inputSources[i].isActive() )
	  return inputSources[i];
      return getTuner();
   }
   
   public Tuner getTuner(){
      return tuner;
   }
   
   public InputSource getChinch(){
      return chinch;
   }
   
   public SVideo getSVideo(){
      return svideo;
   }
   
   public InputSource getColorBars(){
      return colorBars;
   }
   
   /********************************************************************/
   /* abstract methods from GenericBT878                               */
   /********************************************************************/
   
   public String getBoardName(){
      return "WinTV";
   }
   
   public int getXT0Frequ(){
      return BT878PLL.fsc8NTSC;
   }
}

