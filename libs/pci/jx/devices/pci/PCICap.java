package jx.devices.pci;

public interface PCICap {
   
   /* Capability IDs */
   
   byte ID_PM	= 0x01;			/* Power Management */
   byte ID_AGP	= 0x02;			/* Advanced Graphics Port */
   byte ID_VPD	= 0x03;			/* Vital Product Data */
   byte ID_SI	= 0x04;			/* Slot Identification */
   byte ID_MSI	= 0x05;			/* Message Signaled Interrupts */
   
   
   /* Bit definitions of Capability Header */
   
   int CAP_ID_MASK	= 0x000000ff;
   int CAP_ID_SHIFT	= 0;
   int CAP_NEXT_MASK	= 0x0000ff00;
   int CAP_NEXT_SHIFT	= 8;
}
