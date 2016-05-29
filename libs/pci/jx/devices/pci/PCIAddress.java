package jx.devices.pci;

public class PCIAddress {
   int bus;
   int device;
   int function;
   
   public PCIAddress(int bus, int device, int function){
      this.bus = bus;
      this.device = device;
      this.function = function;
   }
   
   public PCIAddress getSubfunction(int subfunction){
      return new PCIAddress(bus, device, subfunction);
   }
   
   public boolean equals(PCIAddress other){
      return (this == other) ||
	((this.bus == other.bus) &&
	 (this.device == other.device) &&
	 (this.function == other.function));
   }
     
   public String toString(){
      return "@PCI("+bus+","+device+","+function+")";
   }
}
