package jx.devices.pci;

public class CapabilityAGP extends PCICapability {
   final static int AGP_REV		= 0;
   final static int AGP_REV_MASK	= 0x00ff0000;
   final static int AGP_REV_SHIFT	= 16;
   final static int AGP_STATUS		= 1;
   final static int AGP_COMMAND		= 2;

   CapabilityAGP(PCIDevice device, int baseRegister){
      super(PCICap.ID_AGP, device, baseRegister);
   }
   
   public byte getRevision(){
      return (byte)device.readPackedConfig(baseRegister+AGP_REV, AGP_REV_MASK, AGP_REV_SHIFT);
   }
   
   public int getStatus(){
      return device.readConfig(baseRegister+AGP_STATUS);
   }
   
   public int getCommand(){
      return device.readConfig(baseRegister+AGP_COMMAND);
   }
   public void setCommand(int mask, int val){
      device.writePackedConfig(baseRegister+AGP_COMMAND, mask, val);
   }
   
   public String toString(){
      return "AGP(0x"+Integer.toHexString(baseRegister)+")";
   }
}

