package jx.wintv;

import jx.zero.*;
import jx.zero.debug.*;
import jx.devices.pci.*;

/*
 * Terratec TerraTV+
 * 
 * I have not found a PCI vendor id for Terratec. The subvendor information
 * they have used is identified as "FlyTV".
 * 
 * InputSources:
 * 
 *     MUX0		Tuner (FM1216 compatible)
 *     MUX1		Chinch video input
 *     MUX2		SVideo input
 * 
 * Clock generator:
 * 
 *     Contrary to the recomendation in the BT878 manual a PAL * 8 clock
 *     generator is connected to XT0.
 * 
 */

public class FlyTV  extends GenericBT878 implements CaptureDevice, Service {
   
   // I2C stuff
   BT878I2CBus	i2cbus	= null;
   I2CEEPROM	eeprom	= null;
   Tuner	tuner	= null;
   
   // other InputSources
   InputSource	chinch	= null;
   SVideo	svideo	= null;
   InputSource colorBars = null;
   
   InputSource inputSources[] = null;
   
   /* debugging options */
   boolean dumpEEPROM = false;
   
   
   /**
    * Search board on the given PCI bus.
    */
   public static PCIDevice findDevice(PCIAccess pcibus){
      FlyTV card = null;
      PCIDevice dev;
      int devc = pcibus.getNumberOfDevices();
      for(int devindex=0; devindex<devc; ++devindex){
	 dev = pcibus.getDeviceAt(devindex);
	 if( isFlyTV(dev) )
	   return dev;
      }
      return null;
   }
   
   /** 
    * Check whether a PCI device is a Fly TV board or not.
    */
   public static boolean isFlyTV(PCIDevice pcidev){
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

      if(   pcidev.getSubsystemVendorID() == 0x1852 
	 && pcidev.getSubsystemID()       == 0x1852 )
	return true;
      
      return false;
   }
   
   /**
    * Initialize internal stuff, setup PCI subsystem of the TV board.
    */
   public FlyTV(PCIAccess pcibus, PCIDevice pcidevice){
      super(pcidevice);
      
      if( !isFlyTV(pcidevice) ){
	 Debug.out.println("ERROR: Device is not a FlyTV card!");
	 throw new Error("Device is not a FlyTV card!");
      }
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
	 
	 int eepromid = 0xa0;
	 if( i2cbus.probe(eepromid) ){
	    Debug.out.println("found EEPROM at I2C(0x"+hex((byte)eepromid)+")");
	    eeprom = new I2CEEPROM(i2cbus, eepromid);
	 }
	 
	 int tunerid = 0xc0;
	 if( i2cbus.probe(tunerid) ){
	    Debug.out.println("found Tuner at I2C(0x"+hex((byte)tunerid)+")");
	    tuner = (Tuner)makeInputSourcePortal(new FM1216(bt, bt.MUX0, i2cbus, tunerid), 
						 "jx/wintv/Tuner", "Tuner");
	 }
      
      } catch(Throwable t) {
	  e = t;
      }

      i2cbus.unlock();
      if (e!=null) throw new Error(e.toString());

      if( eeprom != null && dumpEEPROM )
	eeprom.dump(Debug.out, 256);

      // initialize InputSources
      chinch = (InputSource)makeInputSourcePortal(new MUXPin(bt, bt.MUX1), 
						  "jx/wintv/InputSource", "Chinch");
      svideo = (SVideo)makeInputSourcePortal(new SVideoImpl(bt, bt.MUX2), 
					     "jx/wintv/SVideo", "SVideo");
      colorBars = (InputSource)makeInputSourcePortal(new ColorBars(bt), 
						     "jx/wintv/InputSource", "ColorBars");
      
      inputSources = new InputSource[] {
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
   /* public interface                                                 */
   /********************************************************************/
   
   public InputSource[] getInputSources(){
      
      /*
      while(true){
	 Debug.out.println("testing all MUX pins:");
	 int muxids[] = new int[] { bt.MUX0, bt.MUX1, bt.MUX2, bt.MUX3 };
	 for(int i=0; i<muxids.length; ++i){
	    Debug.out.println("activating MUX "+i);
	    InputSource is = new MUXPin(bt, muxids[i]);
	    is.activate();
	    Debug.out.println("activatied MUX "+i);
	 }
	 Debug.out.println("all input sources activated");
      }
       */
      
      return new InputSource[] {
	 chinch,
	   svideo,
	   colorBars
      };
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
      return "FlyTV";
   }
   
   public int getXT0Frequ(){
      return BT878PLL.fsc8PAL;
   }
}
