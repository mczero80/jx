package jx.devices.pci;

public class CapabilityVPD extends PCICapability {
   CapabilityVPD(PCIDevice device, int baseRegister){
      super(PCICap.ID_VPD, device, baseRegister);
   }
   
   public String toString(){
      return "VPD(0x"+Integer.toHexString(baseRegister)+")";
   }
}

