package jx.devices.pci;

public class CapabilityPM extends PCICapability {
   CapabilityPM(PCIDevice device, int baseRegister){
      super(PCICap.ID_PM, device, baseRegister);
   }
   
   public String toString(){
      return "PM(0x"+Integer.toHexString(baseRegister)+")";
   }
}

