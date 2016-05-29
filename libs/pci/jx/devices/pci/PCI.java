package jx.devices.pci;

/** 
 * Interface which defines constants related to the PCI bus.
 */
public interface PCI {
   
   /********************************************************************/
   /* PCI Configuration Space Header                                   */
   /********************************************************************/
   /* Definition of the registers                                      */
   
   // Register 0
   int REG_DEVVEND		= 0;
   int DEVICE_MASK		= 0xffff0000;
   int DEVICE_SHIFT		= 16;
   int VENDOR_MASK		= 0x0000ffff;
   int VENDOR_SHIFT		= 0;
   
   // Register 1
   int REG_STATCMD		= 1;
   int COMMAND_MASK		= 0x0000ffff;
   int COMMAND_SHIFT		= 0;
   int STATUS_MASK		= 0xffff0000;
   int STATUS_SHIFT		= 16;
   
   // Register 2
   int REG_CLASSREV		= 2;
   int CLASSCODE_MASK		= 0xffffff00;
   int CLASSCODE_SHIFT		= 8;
   int REVISION_MASK		= 0x000000ff;
   int REVISION_SHIFT		= 0;
   
   // Register 3
   int REG_BHLC			= 3;
   int BIST_MASK		= 0xff000000;
   int BIST_SHIFT		= 24;
   int HEADERTYPE_MASK		= 0x00ff0000;
   int HEADERTYPE_SHIFT		= 16;
   int LATENCYTIMER_MASK	= 0x0000ff00;
   int LATENCYTIMER_SHIFT	= 8;
   int CACHELINE_MASK		= 0x000000ff;
   int CACHELINE_SHIFT		= 0;
   
   // Register 4-9
   int REG_BASEADDRESS_0	= 4;
   int REG_BASEADDRESS_1	= 5;
   int REG_BASEADDRESS_2	= 6;
   int REG_BASEADDRESS_3	= 7;
   int REG_BASEADDRESS_4	= 8;
   int REG_BASEADDRESS_5	= 9;
   int BASEADDRESS_MEM_MASK	= 0xfffffff0;
   
   // Register 11
   int REG_SUBDEVVEND		= 11;
   int SUBSYSTEM_MASK		= 0xffff0000;
   int SUBSYSTEM_SHIFT		= 16;
   int SUBVENDOR_MASK		= 0x0000ffff;
   int SUBVENDOR_SHIFT		= 0;
   
   // Register 12
   int REG_EXPANSIONROM		= 12;
   
   // Register 13
   int REG_CAP			= 13;
   int CAP_MASK			= 0x000000ff;
   int CAP_SHIFT		= 0;
   
   // Register 15
   int REG_LGII			= 15;
   int MAXLATENCY_MASK		= 0xff000000;
   int MAXLATENCY_SHIFT		= 24;
   int MINGNT_MASK		= 0x00ff0000;
   int MINGNT_SHIFT		= 16;
   int INTERRUPTPIN_MASK	= 0x0000ff00;
   int INTERRUPTPIN_SHIFT	= 8;
   int INTERRUPTLINE_MASK	= 0x000000ff;
   int INTERRUPTLINE_SHIFT	= 0;
   
   /********************************************************************/
   /* Definitions of various bits inside the registers                 */
   
   int STATUS_CAPABILITY	= 0x0010;
   
     
   int HEADER_MULTIFUNCTION	= 0x80;
   
   int CLASSCODE_CLASS_MASK	= 0xff0000;   /* Class */
   int CLASSCODE_SUBCLASS_MASK	= 0x00ff00;   /* Subclass */
   int CLASSCODE_PIF_MASK	= 0x0000ff;   /* Programming Interface */


   /********************************************************************/
   /* Definitions of well known classcodes and subclass codes          */

    int BASECLASS_LEGACY        = 0x000000;
    int BASECLASS_STORAGE       = 0x010000;
    int BASECLASS_NETWORK       = 0x020000;
    int BASECLASS_VIDEO         = 0x030000;
    int BASECLASS_MULTIMEDIA    = 0x040000;
    int BASECLASS_MEMORY        = 0x050000;
    int BASECLASS_BRIDGE        = 0x060000;
   
    int SUBCLASS_NETWORK_ETHERNET   = 0x020000;
    int SUBCLASS_NETWORK_TOKEN_RING = 0x020100;
    int SUBCLASS_NETWORK_FDDI       = 0x020200;
    int SUBCLASS_NETWORK_ATM        = 0x020300;
    int SUBCLASS_NETWORK_OTHER      = 0x028000;

    int SUBCLASS_MULTIMEDIA_VIDEO   = 0x040000;
    int SUBCLASS_MULTIMEDIA_AUDIO   = 0x040100;
    int SUBCLASS_MULTIMEDIA_OTHER   = 0x048000;
    
   
   
   /********************************************************************/
   /* The following bits&pieces are from the bt878 docu. They should   */
   /* be sorted out and sorted into the above namespace.               */
   /********************************************************************/
   
   /**  !!!!!!!!!!!!!!!! THEESE VALUES MAY CHANGE WITHOUT NOTICE !!!!!!!!!!!!!!!! */
      
   /* PCI Config Register */
   
   int CMD_STATUS_PARITY_ERROR	= 0x8000;
   int CMD_STATUS_SYSTEM_ERROR	= 0x4000;
   int CMD_STATUS_MASTER_ABORT	= 0x2000;
   int CMD_STATUS_TARGET_ABORT	= 0x1000;
   
   int CMD_STATUS_SIG_TARGET_ABORT	= 0x0800;
   int CMD_STATUS_DEVSEL_TIM_MASK	= 0x0600;
   int CMD_STATUS_DEVSEL_TIM_OFF	= 25;
   int CMD_STATUS_PARITY_ERROR_R	= 0x0100;
   int CMD_STATUS_FB2B_CAP		= 0x0080;
   
   int CMD_COMMAND_SERR_ENABLE	= 0x0100;
   int CMD_COMMAND_PERR_ENABLE	= 0x0040;
   int CMD_COMMAND_BM_ENABLE	= 0x0004;
   int CMD_COMMAND_MEM_SPACE	= 0x0002;
   
   
   /********************************************************************/
   /* Constants for PCI Vendors & PCI Device IDs                       */
   /********************************************************************/
   
   /* PCI Vendor Codes */
   final static short VENDOR_HAUPPAUGE = 0x0070;
   final static short VENDOR_MIRO      = 0x1031;
   final static short VENDOR_BROOKTREE = 0x109e;

   /* PCI Device IDs */
   final static short BROOKTREE_BT878 = 0x036e;
   final static short BROOKTREE_BT879 = 0x036f;
   final static short BROOKTREE_BT848 = 0x0350;
}
