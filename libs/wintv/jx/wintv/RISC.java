package jx.wintv;

/** This class simply holds constants related to the FIFO and RISC instructions. */

interface RISC {
   int BCOUNT_MSK	= 0x7F;
   int BENABL_MSK	= 0x0F << 12;
   int STATUS_MSK	= 0xFF << 16;
   int OPCODE_MSK	= 0x0F << 28;
   
   /* RISC opcodes */
   int WRITE		= 0x01 << 28;
   int WRITE123		= 0x09 << 28;
   int WRITE1S23	= 0x0b << 28;
   int WRITEC		= 0x05 << 28;
   int SKIP		= 0x02 << 28;
   int SKIP123		= 0x0a << 28;
   int JUMP		= 0x07 << 28;
   int SYNC		= 0x08 << 28;
   
   /* RISC options */
   int IRQ		= 1 << 24;
   int EOL		= 1 << 26;
   int SOL		= 1 << 27;
   
   // not strictly options
   int BENABL0		= 0x1 << 12;
   int BENABL1		= 0x2 << 12;
   int BENABL2		= 0x4 << 12;
   int BENABL3		= 0x8 << 12;
   
   /* only for sync */
   int RESYNC		= 1 << 15;
   
   /* FIFO status bits */
   int FIFO_FM1		= 0x06;
   int FIFO_FM3		= 0x0E;
   int FIFO_SOL		= 0x02;
   int FIFO_EOL4	= 0x01;
   int FIFO_EOL3	= 0x0d;
   int FIFO_EOL2	= 0x09;
   int FIFO_EOL1	= 0x05;
   int FIFO_VRE		= 0x04;
   int FIFO_VRO		= 0x0c;
   int FIFO_PXV		= 0x00;
}


