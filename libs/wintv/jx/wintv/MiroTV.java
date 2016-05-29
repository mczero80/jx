package jx.wintv;

import jx.zero.*;
import jx.zero.debug.*;
import jx.devices.pci.*;

/*
 * MiroTV
 * 
 * The Miro board is not based on the BT878 chip but on the older BT848A
 * chip. The differences are mostly on the audio part of the chip, but this
 * part is not supported anyway.
 * 
 * InputSources:
 * 
 *     - The tuner on this board is not supported.
 * 
 *     - The chinch video input is connected to MUX1.
 * 
 * Clock generator:
 * 
 *     A NTSC * 8 clock generator is connected to XT0.
 */

public class MiroTV  extends GenericBT878 implements CaptureDevice, Service {
   InputSource chinch = null;
   InputSource colorBars = null;
   
   /**
    * Search a Miro TV board on the given PCI bus.
    */
   public static PCIDevice findDevice(PCIAccess pcibus){
      MiroTV card = null;
      PCIDevice dev;
      int devc = pcibus.getNumberOfDevices();
      for(int devindex=0; devindex<devc; ++devindex){
	 dev = pcibus.getDeviceAt(devindex);
	 if( isMiroTV(dev) )
	   return dev;
      }
      return null;
   }
   
   /** 
    * Check whether a PCI device is a Miro TV board or not.
    */
   public static boolean isMiroTV(PCIDevice pcidev){
      
      // check VendorID (Brooktree)
      // check DeviceID (bt848) (how to check for bt848a?)
      if( pcidev.readConfig(PCI.REG_DEVVEND) != 0x0350109e )
	return false;
      
      // Sigh... no subvendor ID...
      return true;
   }
   
   /**
    * Initialize internal stuff, setup PCI subsystem of the TV board.
    */
   public MiroTV(PCIAccess pcibus, PCIDevice pcidevice){
      super(pcidevice);
      
      if( !isMiroTV(pcidevice) ){
	 Debug.out.println("ERROR: Device is not a MiroTV card!");
	 throw new Error("Device is not a MiroTV card!");
      }
      super.initPCI();
      
      
      /**************************/
      /* initialize other stuff */
      /**************************/
      
      // initialize InputSources
      chinch =  new MUXPin(bt, bt.MUX1);
      colorBars = new ColorBars(bt);
      
      
      if( pciCheck() )
	Debug.out.println("PCI check: Ok");
      else
	Debug.out.println("PCI check: FAILED!");
   }
   
   /********************************************************************/
   /* public interface                                                 */
   /********************************************************************/
   
   public InputSource[] getInputSources(){
      return new InputSource[] {
	 chinch,
	   colorBars
      };
   }
   
   public InputSource getCurrentInputSource(){
      if( chinch.isActive() )
	return chinch;
      if( colorBars.isActive() )
	return colorBars;
      return null;
   }

   public Tuner getTuner(){
      throw new NotImpl();
   }
   
   public InputSource getChinch(){
      return chinch;
   }
   
   public SVideo getSVideo(){
      throw new NotImpl();
   }
   
   public InputSource getColorBars(){
      return colorBars;
   }
   
   /********************************************************************/
   /* abstract methods from GenericBT878                               */
   /********************************************************************/
   
   public String getBoardName(){
      return "MiroTV";
   }
   
   public int getXT0Frequ(){
      return BT878PLL.fsc8NTSC;
   }
}
