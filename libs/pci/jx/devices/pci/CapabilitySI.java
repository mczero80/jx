package jx.devices.pci;

public class CapabilitySI extends PCICapability {
   CapabilitySI(PCIDevice device, int baseRegister){
      super(PCICap.ID_SI, device, baseRegister);
   }
   
   public String toString(){
      return "SI(0x"+Integer.toHexString(baseRegister)+")";
   }
}

