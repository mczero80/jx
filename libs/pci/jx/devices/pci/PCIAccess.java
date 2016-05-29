package jx.devices.pci;

import jx.zero.Portal;

public interface PCIAccess extends Portal {
   
   int getNumberOfDevices();
   PCIDevice getDeviceAt(int index);
   
   PCIDevice[] getDevicesByID(short vendorID, short deviceID);
   PCIDevice[] getDevicesByClass(int mask, int classcode);

   
   int readDeviceConfig(PCIAddress devaddr, int reg);
   void writeDeviceConfig(PCIAddress devaddr, int reg, int value);

    void dumpDevices(); /* debugging */
}
