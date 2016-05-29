package jx.devices.pci;

public class PCICapability  {
   PCIDevice device;
   int baseRegister;
   byte id;
   
   PCICapability(int id, PCIDevice device, int baseRegister){
      this((byte)id, device, baseRegister);
   }
   
   PCICapability(byte id, PCIDevice device, int baseRegister){
      this.id = id;
      this.device = device;
      this.baseRegister = baseRegister;
   }
   
   public byte getID(){
      return id;
   }
   public int getBaseRegister(){
      return baseRegister;
   }
   
   public String toString(){
      return "capability(0x"+Integer.toHexString(id)+", "+Integer.toHexString(baseRegister)+")";
   }
     
   static PCICapability createCapability(PCIDevice device, int baseRegister){
      int id = device.readPackedConfig(baseRegister, PCICap.CAP_ID_MASK, PCICap.CAP_ID_SHIFT);
      
      switch(id){
       case PCICap.ID_PM:
	 return new CapabilityPM(device, baseRegister);
       case PCICap.ID_AGP:
	 return new CapabilityAGP(device, baseRegister);
       case PCICap.ID_VPD:
	 return new CapabilityVPD(device, baseRegister);
       case PCICap.ID_SI:
	 return new CapabilitySI(device, baseRegister);
       case PCICap.ID_MSI:
	 return new CapabilityVPD(device, baseRegister);
       default:
	 return new PCICapability(id, device, baseRegister);
      }
   }
}

